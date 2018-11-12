package net.zedge.photoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import ja.burhanrashid52.photoeditor.BitmapUtil;
import ja.burhanrashid52.photoeditor.CustomEffect;
import ja.burhanrashid52.photoeditor.GLToolbox;
import ja.burhanrashid52.photoeditor.ImageFilterView;
import ja.burhanrashid52.photoeditor.PhotoFilter;

public class ZedgeImageFilterView extends ImageFilterView {

    private static final String TAG = "ZedgeImageFilterView";
    private boolean mShouldReloadTexture = false;
    private boolean mShouldReloadEffect = false;

    public interface RenderJobCallback {
        void onRenderJobDone(String jobName, Bitmap bitmap);
    }

    public static class RenderJob {
        String jobId;
        CustomEffect customEffect;
        float scale;
        RenderJobCallback callback;
        public RenderJob(String jobId, CustomEffect customEffect, float scale, RenderJobCallback callback) {
            this.jobId = jobId;
            this.customEffect = customEffect;
            this.scale = scale;
            this.callback = callback;
        }
    }

    private List<RenderJob> mRenderJobs = Collections.synchronizedList(new ArrayList<RenderJob>());

    public ZedgeImageFilterView(Context context) {
        super(context);
    }

    public ZedgeImageFilterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void setSourceBitmap(Bitmap sourceBitmap) {
        if (sourceBitmap == mSourceBitmap) {
           return;
        }
        mSourceBitmap = sourceBitmap;
        mShouldReloadTexture = true;
    }

    private void prepareToDraw() {
        if (mEffectContext == null) {
            mEffectContext = EffectContext.createWithCurrentGlContext();
        }
        if (!mTexRenderer.isInitialized()) {
            mTexRenderer.init();
            createTextures();
            reloadTextures();
        }
        if (mShouldReloadTexture) {
            mShouldReloadTexture = false;
            reloadTextures();
        }
    }

    private void drawJobFrame(RenderJob renderJob) {
        initEffect(renderJob.customEffect);
        int viewPortWidth = Math.round(mImageWidth * renderJob.scale);
        int viewPortHeight = Math.round(mImageHeight * renderJob.scale);
        applyEffect(viewPortWidth, viewPortHeight);
        renderJob.callback.onRenderJobDone(
                renderJob.jobId,
                BitmapUtil.createBitmapFromGlFrameBuffer(
                        0, 0, viewPortWidth, viewPortHeight));
    }

    private void drawNormalFrame(GL10 gl) {
        if (mCustomEffect != null) {
            if (mShouldReloadEffect) {
                mShouldReloadEffect = false;
                //if an effect is chosen initialize it and apply it to the texture
                initEffect(mCustomEffect);
            }
            applyEffect(mImageWidth, mImageHeight);
        }
        renderResult();
        if (isSaveImage) {
            final Bitmap mFilterBitmap = BitmapUtil.createBitmapFromGLSurface(this, gl);
            Log.e(TAG, "onDrawFrame: " + mFilterBitmap);
            isSaveImage = false;
            if (mOnSaveBitmap != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mOnSaveBitmap.onBitmapReady(mFilterBitmap);
                    }
                });
            }
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d("ZIZI", "onDrawFrame");
        prepareToDraw();
        if (!mRenderJobs.isEmpty()) {
            drawJobFrame(mRenderJobs.remove(0));

            // Reload the effect because the job might have replaced it.
            mShouldReloadEffect = true;
            drawNormalFrame(gl);

            if (!mRenderJobs.isEmpty()) {
                // Trigger another render iteration to handle the next job.
                requestRender();
            }
        } else {
            drawNormalFrame(gl);
        }
    }

    @Override
    protected void setFilterEffect(PhotoFilter effect) {
        mShouldReloadEffect = true;
        super.setFilterEffect(effect);
    }

    @Override
    protected void setFilterEffect(CustomEffect customEffect) {
        mShouldReloadEffect = true;
        super.setFilterEffect(customEffect);
    }

    void submitRenderJob(RenderJob renderJob) {
        Log.d("ZIZI", "submitRenderJob");
        mRenderJobs.add(renderJob);
        requestRender();
    }

    void cancelRenderJob(String jobId) {
        synchronized (mRenderJobs) {
            for (RenderJob renderJob : mRenderJobs) {
                if (renderJob.jobId.equals(jobId)) {
                    mRenderJobs.remove(renderJob);
                }
            }
        }
    }


    protected void removeEffect() {
        mCurrentEffect = null;
        mCustomEffect = null;
        requestRender();
    }

    private void reloadTextures() {
        // Load input bitmap
        if (mSourceBitmap != null) {
            mImageWidth = mSourceBitmap.getWidth();
            mImageHeight = mSourceBitmap.getHeight();
            mTexRenderer.updateTextureSize(mImageWidth, mImageHeight);

            // Upload to texture
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
            GLToolbox.checkGlError("glBindTexture");
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mSourceBitmap, 0);
            GLToolbox.checkGlError("texImage2D");

            // Set texture parameters
            GLToolbox.initTexParams();
        }
    }

    private void createTextures() {
        Log.d("ZIZI", "LOAD TEXTURE");
        // Generate textures
        GLES20.glGenTextures(2, mTextures, 0);
    }

    private void initEffect(@Nullable CustomEffect customEffect) {
        EffectFactory effectFactory = mEffectContext.getFactory();
        if (mEffect != null) {
            mEffect.release();
            mEffect = null;
        }
        if (customEffect != null) {
            mEffect = effectFactory.createEffect(customEffect.getEffectName());
            Map<String, Object> parameters = customEffect.getParameters();
            for (Map.Entry<String, Object> param : parameters.entrySet()) {
                mEffect.setParameter(param.getKey(), param.getValue());
            }
        }
    }

    private void applyEffect(int viewPortWidth, int viewPortHeight) {
        if (mEffect != null) {
            mEffect.apply(mTextures[0], viewPortWidth, viewPortHeight, mTextures[1]);
        }
    }

}
