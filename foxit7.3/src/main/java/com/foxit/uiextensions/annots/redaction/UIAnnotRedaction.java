/**
 * Copyright (C) 2003-2020, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.annots.redaction;


import android.content.Context;
import android.graphics.RectF;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.addon.Redaction;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Redact;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;
import java.util.List;

public class UIAnnotRedaction {

    public static void apply(final PDFViewCtrl pdfViewCtrl, final Annot annot) {
        showTipsDlg(pdfViewCtrl, annot, null);
    }

    public static void apply(PDFViewCtrl pdfViewCtrl, final Annot annot, final Event.Callback callback) {
        showTipsDlg(pdfViewCtrl, annot, callback);
    }

    private static void showTipsDlg(final PDFViewCtrl pdfViewCtrl, final Annot annot, final Event.Callback callback) {
        if (annot == null || annot.isEmpty() || pdfViewCtrl.getUIExtensionsManager() == null) {
            return;
        }
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
        Context context = uiExtensionsManager.getAttachedActivity().getApplicationContext();

        final UITextEditDialog dialog = new UITextEditDialog(uiExtensionsManager.getAttachedActivity());
        dialog.getInputEditText().setVisibility(View.GONE);
        dialog.setTitle(AppResource.getString(context, R.string.fx_string_warning));
        dialog.getPromptTextView().setText(AppResource.getString(context, R.string.fx_string_redact_apply_toast));
        dialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyRedaction(pdfViewCtrl, annot, callback);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private static void applyRedaction(final PDFViewCtrl pdfViewCtrl, final Annot annot, final Event.Callback callback) {
        try {
            final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();
            uiExtensionsManager.getDocumentManager().onAnnotWillApply(page, annot);

            ApplySingleEvent event = new ApplySingleEvent(annot);
            Context context = uiExtensionsManager.getAttachedActivity();
            final FxProgressDialog progressDialog = new FxProgressDialog(context, AppResource.getString(context, R.string.fx_string_processing));
            progressDialog.show();
            ApplyTask task = new ApplyTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        uiExtensionsManager.getDocumentManager().setDocModified(true);
                        uiExtensionsManager.getDocumentManager().onAnnotApplied(page, annot);
                        uiExtensionsManager.getDocumentManager().clearUndoRedo();

                        if (pdfViewCtrl.isPageVisible(pageIndex)) {
                            int width = pdfViewCtrl.getPageViewWidth(pageIndex);
                            int height = pdfViewCtrl.getPageViewHeight(pageIndex);
                            RectF pageRectF = new RectF(0, 0, width, height);
                            pdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(pageRectF));
                        }
                    }
                    if (callback != null) {
                        callback.result(null, success);
                    }
                    progressDialog.dismiss();
                }
            });
            pdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public static void applyAll(final PDFViewCtrl pdfViewCtrl, List<Annot> annots, final Event.Callback callback) {
        try {
            final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();

            final List<Integer> pageIndexs = new ArrayList<>();
            for (Annot annot : annots) {
                pageIndexs.add(annot.getPage().getIndex());
            }

            Context context = uiExtensionsManager.getAttachedActivity();
            final FxProgressDialog progressDialog = new FxProgressDialog(context, AppResource.getString(context, R.string.fx_string_processing));
            progressDialog.show();
            ApplyAllEvent applyAllEvent = new ApplyAllEvent(pdfViewCtrl.getDoc());
            ApplyTask task = new ApplyTask(applyAllEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        for (Integer pageIndex : pageIndexs) {
                            if (pdfViewCtrl.isPageVisible(pageIndex)) {
                                int width = pdfViewCtrl.getPageViewWidth(pageIndex);
                                int height = pdfViewCtrl.getPageViewHeight(pageIndex);
                                RectF pageRectF = new RectF(0, 0, width, height);
                                pdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(pageRectF));
                            }
                        }
                        uiExtensionsManager.getDocumentManager().setDocModified(true);
                        uiExtensionsManager.getDocumentManager().clearUndoRedo();
                    }

                    if (callback != null) {
                        callback.result(null, success);
                    }
                    progressDialog.dismiss();
                }
            });
            pdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    static class ApplyAllEvent extends ApplyEvent {

        private PDFDoc mPDFDoc;

        public ApplyAllEvent(PDFDoc pdfDoc) {
            mPDFDoc = pdfDoc;
        }

        @Override
        public boolean apply() {
            if (mPDFDoc == null || mPDFDoc.isEmpty()) {
                return false;
            }

            try {
                Redaction redaction = new Redaction(mPDFDoc);
                if (redaction.isEmpty())
                    return false;

                redaction.apply();
                return true;
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    static class ApplySingleEvent extends ApplyEvent {

        private Annot mAnnot;

        public ApplySingleEvent(Annot annot) {
            mAnnot = annot;
        }

        @Override
        public boolean apply() {
            if (mAnnot == null || mAnnot.isEmpty() || !(mAnnot instanceof Redact)) {
                return false;
            }

            try {
                Redact redact = (Redact) mAnnot;
                redact.removeAllReplies();
                return redact.apply();
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    static class ApplyTask extends Task {

        private boolean ret;
        private ApplyEvent mEvent;

        public ApplyTask(ApplyEvent event, final Event.Callback callback) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    callback.result(null, ((ApplyTask) task).ret);
                }
            });
            mEvent = event;
        }

        @Override
        protected void execute() {
            ret = mEvent.apply();
        }
    }

    static abstract class ApplyEvent {
        public abstract boolean apply();
    }

}
