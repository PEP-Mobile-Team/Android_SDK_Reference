/**
 * Copyright (C) 2003-2019, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.annots.common;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.sdk.pdf.annots.Note;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class UIAnnotFlatten {

    public static void flattenAnnot(final PDFViewCtrl pdfViewCtrl, final Annot annot) {
        showTipsDlg(pdfViewCtrl, annot, null);
    }

    public static void flattenAnnot(PDFViewCtrl pdfViewCtrl, final Annot annot, final Event.Callback callback) {
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
        dialog.setTitle(AppResource.getString(context, R.string.fx_string_flatten));
        dialog.getPromptTextView().setText(AppResource.getString(context, R.string.fx_flatten_toast));
        dialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flatten(pdfViewCtrl, annot, callback);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public static void flatten(final PDFViewCtrl pdfViewCtrl, final Annot annot, final Event.Callback callback) {
        try {
            final PDFPage page = annot.getPage();
            final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
            final int pageIndex = page.getIndex();
            Matrix matrix = pdfViewCtrl.getDisplayMatrix(pageIndex);
            final RectF pvRect = new RectF();
            if (matrix != null) {
                pvRect.set(AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix))));
                if (AppAnnotUtil.isReplaceCaret(annot)) {
                    StrikeOut strikeOut = AppAnnotUtil.getStrikeOutFromCaret((Caret) annot);
                    if (strikeOut != null) {
                        RectF sto_Rect = AppUtil.toRectF(strikeOut.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                        pvRect.union(sto_Rect);
                    }
                }
            }

            uiExtensionsManager.getDocumentManager().onAnnotWillFlatten(page, annot);
            FlattenEvent event = new FlattenEvent(EditAnnotEvent.EVENTTYPE_FLATTEN, annot, pdfViewCtrl);
            if (uiExtensionsManager.getDocumentManager().isMultipleSelectAnnots()) {
                if (callback != null) {
                    callback.result(event, true);
                }
                return;
            }
            final EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        uiExtensionsManager.getDocumentManager().onAnnotFlattened(page, annot);
                        uiExtensionsManager.getDocumentManager().setDocModified(true);
                        if (pdfViewCtrl.isPageVisible(pageIndex)) {
                            pdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(pvRect));
                        }
                    }

                    if (callback != null) {
                        callback.result(null, success);
                    }
                }
            });
            pdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private static boolean flattenAnnot(Annot annot) {
        if (annot == null || annot.isEmpty()) return false;
        try {
            PDFPage page = annot.getPage();
            //flatten strikeout
            if (AppAnnotUtil.isReplaceCaret(annot)) {
                Caret caret = (Caret) annot;
                MarkupArray markupArray = caret.getGroupElements();
                int nCount = (int) markupArray.getSize();
                for (int i = nCount - 1; i >= 0; i --) {
                    Markup groupAnnot = markupArray.getAt(i);
                    if (groupAnnot.getType() == Annot.e_StrikeOut) {
                        page.flattenAnnot(groupAnnot);
                        break;
                    }
                }
            }
            if (!flatten(page, annot)) return false;
            return page.flattenAnnot(annot);
        } catch (PDFException e) {
        }
        return false;
    }

    private static boolean flatten(PDFPage page, Annot annot) {
        try {
            if (annot.isMarkup()) {
                Markup markup = new Markup(annot);
                int count = markup.getReplyCount();
                while (count > 0) {
                    Note note = markup.getReply(count - 1);
                    if (note.getReplyCount() > 0) {
                        flatten(note.getPage(), note);
                    }
                    page.flattenAnnot(note);
                    count = markup.getReplyCount();
                }
            }
            return true;
        } catch (PDFException e) {
        }
        return false;
    }

    static class FlattenEvent extends EditAnnotEvent {
        FlattenEvent(int eventType, Annot annot, PDFViewCtrl pdfViewCtrl) {
            mType = eventType;
            mAnnot = annot;
            mPdfViewCtrl = pdfViewCtrl;
        }

        @Override
        public boolean add() {
            return false;
        }

        @Override
        public boolean modify() {
            return false;
        }

        @Override
        public boolean delete() {
            return false;
        }

        @Override
        public boolean flatten() {
            if (mAnnot == null || mAnnot.isEmpty()) return false;
            return flattenAnnot(mAnnot);
        }
    }
}
