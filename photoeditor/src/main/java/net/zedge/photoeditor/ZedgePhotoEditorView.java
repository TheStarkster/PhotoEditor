package net.zedge.photoeditor;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;

import ja.burhanrashid52.photoeditor.ImageFilterView;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

public class ZedgePhotoEditorView extends PhotoEditorView {

    private ZedgeImageFilterView mZedgeImageFilterView;

    public ZedgePhotoEditorView(Context context) {
        super(context);
    }

    public ZedgePhotoEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ZedgePhotoEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ZedgePhotoEditorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected ImageFilterView createImageFilterView() {
        mZedgeImageFilterView = new ZedgeImageFilterView(getContext());
        return mZedgeImageFilterView;
    }

    void submitRenderJob(ZedgeImageFilterView.RenderJob renderJob) {
        mZedgeImageFilterView.setVisibility(VISIBLE);
        mZedgeImageFilterView.submitRenderJob(renderJob);
    }

    void cancelRenderJob(String jobId) {
        mZedgeImageFilterView.cancelRenderJob(jobId);
    }

    void removeFilter() {
        mZedgeImageFilterView.setVisibility(GONE);
        mZedgeImageFilterView.removeEffect();
    }

}