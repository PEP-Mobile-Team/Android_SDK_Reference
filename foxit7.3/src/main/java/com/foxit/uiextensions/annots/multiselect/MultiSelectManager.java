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
package com.foxit.uiextensions.annots.multiselect;


import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class MultiSelectManager {

    private UIExtensionsManager mUiExtensionsManager;
    private PDFViewCtrl mPdfViewCtrl;

    MultiSelectManager(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
    }

    void moveAnnots(final int pageIndex,
                    final ArrayList<Annot> selectAnnots,
                    final RectF oldRect,
                    final RectF newRect,
                    final boolean addUndo,
                    final Event.Callback callback) {
        if (selectAnnots == null || selectAnnots.size() == 0) return;

        RectF oldSelectRect = new RectF();
        RectF newSelectRect = new RectF();

        mPdfViewCtrl.convertPageViewRectToPdfRect(oldRect, oldSelectRect, pageIndex);
        mPdfViewCtrl.convertPageViewRectToPdfRect(newRect, newSelectRect, pageIndex);

        float offsetX = newSelectRect.left;
        float offsetY = newSelectRect.bottom;
        float scaleX = (newSelectRect.right - newSelectRect.left) / (oldSelectRect.right - oldSelectRect.left);
        float scaleY = (newSelectRect.bottom - newSelectRect.top) / (oldSelectRect.bottom - oldSelectRect.top);

        final MultiSelectModifyUndoItem modifyUndoItem = new MultiSelectModifyUndoItem(mPdfViewCtrl);
        modifyUndoItem.mOperType = MultiSelectConstants.OPER_MOVE_ANNOTS;
        modifyUndoItem.mPageIndex = pageIndex;
        modifyUndoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        RectF tmpRect = new RectF();

        modifyUndoItem.mUndoOperType = MultiSelectConstants.OPER_MOVE_ANNOTS;
        modifyUndoItem.mRedoOperType = MultiSelectConstants.OPER_MOVE_ANNOTS;
        try {
            for (int i = 0; i < selectAnnots.size(); i++) {
                Annot annot = selectAnnots.get(i);
                RectF annontRect = AppUtil.toRectF(annot.getRect());
                AppUtil.normalizePDFRect(annontRect);
                String nm = AppAnnotUtil.getAnnotUniqueID(annot);
                modifyUndoItem.mNMList.add(nm);
                modifyUndoItem.mLastAnnots.put(nm, annontRect);
                tmpRect.set(annontRect);
                tmpRect.offset(-oldSelectRect.left, -oldSelectRect.bottom);
                RectF newAnnotRect = new RectF(
                        tmpRect.left * scaleX + offsetX,
                        tmpRect.top * scaleY + offsetY,
                        tmpRect.right * scaleX + offsetX,
                        tmpRect.bottom * scaleY + offsetY);

                modifyUndoItem.mCurrentAnnots.put(nm, newAnnotRect);
            }

            mUiExtensionsManager.getDocumentManager().setHasModifyTask(addUndo);
            EditAnnotEvent modifyEvent = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_MODIFY, modifyUndoItem, selectAnnots, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        try {
                            for (int i = 0; i < selectAnnots.size(); i++) {
                                mUiExtensionsManager.getDocumentManager().onAnnotModified(selectAnnots.get(i).getPage(), selectAnnots.get(i));
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }

                        if (addUndo) {
                            mUiExtensionsManager.getDocumentManager().addUndoItem(modifyUndoItem);
                            mUiExtensionsManager.getDocumentManager().setHasModifyTask(false);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF invalidateRect = new RectF(newRect);
                            invalidateRect.union(oldRect);
                            invalidateRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(invalidateRect));
                        }
                    }

                    if (callback != null)
                        callback.result(null, success);
                }
            });

            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    void groupAnnots(final int pageIndex,
                     final int operType,
                     final ArrayList<Annot> annots,
                     HashMap<String, ArrayList<String>> selectGroups,
                     final boolean addUndo,
                     final Event.Callback callback) {
        if (annots == null || annots.size() == 0) return;

        final PDFPage page = mUiExtensionsManager.getDocumentManager().getPage(pageIndex, false);
        final MultiSelectModifyUndoItem modifyUndoItem = new MultiSelectModifyUndoItem(mPdfViewCtrl);
        modifyUndoItem.mPageIndex = pageIndex;
        modifyUndoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

        modifyUndoItem.mOperType = operType;
        modifyUndoItem.mRedoOperType = operType;
        modifyUndoItem.mUndoOperType = operType == MultiSelectConstants.OPER_GROUP_ANNOTS ? MultiSelectConstants.OPER_UNGROUP_ANNOTS : MultiSelectConstants.OPER_GROUP_ANNOTS;
        try {
            final RectF tmpRect = new RectF();
            ArrayList<String> uniqueIDs = new ArrayList<>();
            for (int i = 0; i < annots.size(); i++) {
                Annot annot = annots.get(i);
                String nm = AppAnnotUtil.getAnnotUniqueID(annot);
                uniqueIDs.add(nm);
                RectF annotRectF = AppUtil.toRectF(annot.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                if (i == 0) {
                    tmpRect.set(annotRectF);
                } else {
                    tmpRect.union(annotRectF);
                }
            }
            modifyUndoItem.mNMList = uniqueIDs;
            if (MultiSelectConstants.OPER_GROUP_ANNOTS == operType) {
                HashMap<String, ArrayList<String>> groups = new HashMap<>();
                groups.put(uniqueIDs.get(0), uniqueIDs);
                modifyUndoItem.mGroups = groups;
                modifyUndoItem.mRedoGroups = groups;
                HashMap<String, ArrayList<String>> undoGroups = new HashMap<>();
                for (Map.Entry<String, ArrayList<String>> entry : selectGroups.entrySet()) {
                    undoGroups.put(entry.getKey(), entry.getValue());
                }
                modifyUndoItem.mUndoGroups = undoGroups;
            } else if (MultiSelectConstants.OPER_UNGROUP_ANNOTS == operType) {
                modifyUndoItem.mGroups = new HashMap<>();

                modifyUndoItem.mRedoGroups = new HashMap<>();
                HashMap<String, ArrayList<String>> groups = new HashMap<>();
                groups.put(uniqueIDs.get(0), uniqueIDs);
                modifyUndoItem.mUndoGroups = groups;
            }

            mUiExtensionsManager.getDocumentManager().setHasModifyTask(addUndo);
            EditAnnotEvent modifyEvent = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_MODIFY, modifyUndoItem, annots, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (MultiSelectConstants.OPER_GROUP_ANNOTS == operType)
                            mUiExtensionsManager.getDocumentManager().onAnnotGrouped(page, annots);
                        else if (MultiSelectConstants.OPER_UNGROUP_ANNOTS == operType)
                            mUiExtensionsManager.getDocumentManager().onAnnotUnGrouped(page, annots);

                        if (addUndo) {
                            mUiExtensionsManager.getDocumentManager().addUndoItem(modifyUndoItem);
                            mUiExtensionsManager.getDocumentManager().setHasModifyTask(false);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF invalidateRect = new RectF(tmpRect);
                            invalidateRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(invalidateRect));
                        }
                    }

                    if (callback != null)
                        callback.result(null, success);
                }
            });

            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    void deleteAnnots(final int pageIndex,
                      final ArrayList<Annot> annots,
                      HashMap<String, ArrayList<String>> selectGroups,
                      final RectF annotsRectF,
                      final boolean addUndo,
                      final Event.Callback callback) {
        if (annots == null || annots.size() == 0) return;

        final ArrayList<EditAnnotEvent> eventList = new ArrayList<>();
        final ArrayList<AnnotUndoItem> undoItems = new ArrayList<>();
        final MultiSelectDeleteUndoItem undoItem = new MultiSelectDeleteUndoItem(mPdfViewCtrl);
        mUiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(true);
        for (Annot annot : annots) {
            if (mUiExtensionsManager.isLoadAnnotModule(annot)) {
                removeAnnot(annot, false, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success && event instanceof EditAnnotEvent) {
                            eventList.add((EditAnnotEvent) event);
                            undoItems.add(((EditAnnotEvent) event).mUndoItem);
                            undoItem.mNMList.add(((EditAnnotEvent) event).mUndoItem.mNM);
                        }
                    }
                });
            }
        }

        HashMap<String, ArrayList<String>> groups = new HashMap<>();
        for (Map.Entry<String, ArrayList<String>> entry : selectGroups.entrySet()) {
            groups.put(entry.getKey(), entry.getValue());
        }
        undoItem.mGroups = groups;
        undoItem.mUndoItemList = undoItems;
        undoItem.mPageIndex = pageIndex;
        mUiExtensionsManager.getDocumentManager().setHasModifyTask(addUndo);
        MultiSelectEvent event = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_DELETE, eventList, mPdfViewCtrl);
        EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                mUiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(false);
                if (success) {
                    try {
                        for (int i = 0; i < annots.size(); i++) {
                            mUiExtensionsManager.getDocumentManager().onAnnotDeleted(mPdfViewCtrl.getDoc().getPage(pageIndex), annots.get(i));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                    if (addUndo) {
                        mUiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                        mUiExtensionsManager.getDocumentManager().setHasModifyTask(false);
                    }

                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        RectF invalidateRect = new RectF(annotsRectF);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(invalidateRect, invalidateRect, pageIndex);
                        invalidateRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(invalidateRect));
                    }
                }

                if (callback != null) {
                    callback.result(null, success);
                }
            }
        });
        mPdfViewCtrl.addTask(task);
    }

    private void removeAnnot(final Annot annot, boolean addUndo, final Event.Callback result) {
        if (AppAnnotUtil.isSameAnnot(annot, mUiExtensionsManager.getDocumentManager().getCurrentAnnot())) {
            mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
        }

        AnnotHandler annotHandler = mUiExtensionsManager.getAnnotHandlerByType(AppAnnotUtil.getAnnotHandlerType(annot));
        if (annotHandler != null) {
            annotHandler.removeAnnot(annot, addUndo, result);
        }
    }

    void flattenAnnots(final int pageIndex,
                       final ArrayList<Annot> annots,
                       final RectF annotsRectF,
                       final Event.Callback callback) {
        if (annots == null || annots.size() == 0) return;
        final Context context = mUiExtensionsManager.getAttachedActivity().getApplicationContext();

        final UITextEditDialog dialog = new UITextEditDialog(mUiExtensionsManager.getAttachedActivity());
        dialog.getInputEditText().setVisibility(View.GONE);
        dialog.setTitle(AppResource.getString(context, R.string.fx_string_flatten));
        dialog.getPromptTextView().setText(AppResource.getString(context, R.string.fx_flatten_toast));
        dialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (callback != null)
                    callback.result(new Event(MultiSelectConstants.FLATTEN_CANCEL), true);
            }
        });
        dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                final FxProgressDialog progressDialog = new FxProgressDialog(mUiExtensionsManager.getAttachedActivity(),
                        AppResource.getString(context, R.string.fx_string_processing));
                progressDialog.show();
                doAnnotsFlatten(annots, annotsRectF, pageIndex, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        progressDialog.dismiss();
                        if (callback != null) {
                            callback.result(new Event(MultiSelectConstants.FLATTEN_OK), success);
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private void doAnnotsFlatten(final ArrayList<Annot> annots,
                                 final RectF annotsRectF,
                                 final int pageIndex,
                                 final Event.Callback callback) {
        final ArrayList<EditAnnotEvent> eventList = new ArrayList<>();
        mUiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(true);
        for (Annot annot : annots) {
            if (mUiExtensionsManager.isLoadAnnotModule(annot)) {
                UIAnnotFlatten.flatten(mPdfViewCtrl, annot, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success && event instanceof EditAnnotEvent) {
                            eventList.add((EditAnnotEvent) event);
                        }
                    }
                });
            }
        }

        MultiSelectEvent event = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_FLATTEN, eventList, mPdfViewCtrl);
        EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                mUiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(false);
                if (success) {
                    try {
                        for (int i = 0; i < annots.size(); i++) {
                            mUiExtensionsManager.getDocumentManager().onAnnotFlattened(mPdfViewCtrl.getDoc().getPage(pageIndex), annots.get(i));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                    mUiExtensionsManager.getDocumentManager().setDocModified(true);
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        RectF invalidateRect = new RectF(annotsRectF);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(invalidateRect, invalidateRect, pageIndex);
                        invalidateRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(invalidateRect));
                    }
                }
                if (callback != null) {
                    callback.result(null, success);
                }
            }
        });
        mPdfViewCtrl.addTask(task);
    }

    void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        try {
            if (annot.getModifiedDateTime() != null && content.getModifiedDate() != null
                    && annot.getModifiedDateTime().equals(content.getModifiedDate())) {
                if (result != null) {
                    result.result(null, true);
                }
                return;
            }

            AnnotHandler annotHandler = mUiExtensionsManager.getAnnotHandlerByType(AppAnnotUtil.getAnnotHandlerType(annot));
            if (annotHandler != null) {
                annotHandler.modifyAnnot(annot, content, addUndo, result);
            } else {
                if (result != null) {
                    result.result(null, false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    void modifyProperty(Annot annot, AnnotContent annotContent, final boolean addUndo, final Event.Callback callback) {
        try {
            final int pageIndex = annot.getPage().getIndex();
            final MultiSelectModifyUndoItem undoItem = new MultiSelectModifyUndoItem(mPdfViewCtrl);
            ArrayList<String> uniqueIDs = new ArrayList<>();
            final ArrayList<Annot> annots = new ArrayList<>();
            HashMap<String, String> redoContents = new HashMap<>();
            HashMap<String, String> undoContents = new HashMap<>();
            final RectF tmpRect = new RectF();

            String content = annotContent.getContents() == null ? "" : annotContent.getContents();
            MarkupArray markupArray = ((Markup) annot).getGroupElements();
            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
            for (long i = 0; i < markupArray.getSize(); i++) {
                Annot groupAnnot = AppAnnotUtil.createAnnot(markupArray.getAt(i));
                if (groupAnnot == null || groupAnnot.isEmpty())
                    continue;

                RectF pvRect = AppUtil.toRectF(groupAnnot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                if (i == 0)
                    tmpRect.set(pvRect);
                else
                    tmpRect.union(pvRect);

                String nm = AppAnnotUtil.getAnnotUniqueID(groupAnnot);
                annots.add(groupAnnot);
                uniqueIDs.add(nm);
                redoContents.put(nm, content);
                undoContents.put(nm, groupAnnot.getContent());
            }

            undoItem.mPageIndex = pageIndex;
            undoItem.mNMList = uniqueIDs;
            undoItem.mContents = new HashMap<>(redoContents);
            undoItem.mRedoContents = redoContents;
            undoItem.mUndoContents = undoContents;
            undoItem.mOperType = MultiSelectConstants.OPER_MODIFY_PROPERTY;
            undoItem.mRedoOperType = MultiSelectConstants.OPER_MODIFY_PROPERTY;
            undoItem.mUndoOperType = MultiSelectConstants.OPER_MODIFY_PROPERTY;

            mUiExtensionsManager.getDocumentManager().setHasModifyTask(addUndo);
            EditAnnotEvent modifyEvent = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, annots, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        try {
                            for (int i = 0; i < annots.size(); i++) {
                                mUiExtensionsManager.getDocumentManager().onAnnotModified(annots.get(i).getPage(), annots.get(i));
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }

                        if (addUndo) {
                            mUiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                            mUiExtensionsManager.getDocumentManager().setHasModifyTask(false);
                        }

                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF invalidateRect = new RectF(tmpRect);
                            invalidateRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(invalidateRect));
                        }
                    }

                    if (callback != null)
                        callback.result(null, success);
                }
            });

            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

}
