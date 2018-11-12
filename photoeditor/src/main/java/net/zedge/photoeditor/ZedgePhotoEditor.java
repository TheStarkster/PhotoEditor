package net.zedge.photoeditor;

import android.content.Context;

import ja.burhanrashid52.photoeditor.PhotoEditor;

public class ZedgePhotoEditor extends PhotoEditor {

    private ZedgePhotoEditorView mZedgePhotoEditorView;

    private ZedgePhotoEditor(Builder builder) {
        super(builder);
        mZedgePhotoEditorView = builder.zedgePhotoEditorView;
    }

    public void createFilterPreview(ZedgeImageFilterView.RenderJob renderJob) {
        mZedgePhotoEditorView.submitRenderJob(renderJob);
    }

    public void cancelFilterPreview(String jobId) {
        mZedgePhotoEditorView.cancelRenderJob(jobId);
    }


    public void removeFilter() {
        mZedgePhotoEditorView.removeFilter();
    }

    public static class Builder extends PhotoEditor.Builder {
        ZedgePhotoEditorView zedgePhotoEditorView;

        public Builder(Context context, ZedgePhotoEditorView photoEditorView) {
            super(context, photoEditorView);
            zedgePhotoEditorView = photoEditorView;
        }

        @Override
        public ZedgePhotoEditor build() {
            return new ZedgePhotoEditor(this);
        }
    }
}