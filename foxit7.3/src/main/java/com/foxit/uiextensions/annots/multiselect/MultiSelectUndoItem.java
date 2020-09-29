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

import android.graphics.Matrix;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import androidx.annotation.NonNull;

public abstract class MultiSelectUndoItem extends AnnotUndoItem {

    int mOperType;
    ArrayList<String> mNMList = new ArrayList<>();

    HashMap<String, String> mContents = new HashMap<>();
    HashMap<String, ArrayList<String>> mGroups = new HashMap<>();

    public MultiSelectUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    public boolean isSelected(@NonNull String nm) {
        if (mNMList.size() == 0) return false;
        return mNMList.contains(nm);
    }
}

class MultiSelectModifyUndoItem extends MultiSelectUndoItem {

    int mUndoOperType;
    int mRedoOperType;

    HashMap<String, ArrayList<String>> mRedoGroups;
    HashMap<String, ArrayList<String>> mUndoGroups;

    HashMap<String, RectF> mLastAnnots = new HashMap<String, RectF>();
    HashMap<String, RectF> mCurrentAnnots = new HashMap<String, RectF>();

    HashMap<String, String> mUndoContents = new HashMap<>();
    HashMap<String, String> mRedoContents = new HashMap<>();

    MultiSelectModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        MultiSelectModifyUndoItem undoItem = new MultiSelectModifyUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = mPageIndex;
        undoItem.mOperType = mUndoOperType;
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

        if (MultiSelectConstants.OPER_MOVE_ANNOTS == mUndoOperType) {
            return moveAnnots(undoItem, mLastAnnots);
        } else if (MultiSelectConstants.OPER_GROUP_ANNOTS == mUndoOperType
                || MultiSelectConstants.OPER_UNGROUP_ANNOTS == mUndoOperType) {
            return groupAnnots(undoItem, mUndoOperType, mUndoGroups);
        } else if (MultiSelectConstants.OPER_MODIFY_PROPERTY == mUndoOperType) {
            return modifyProperty(undoItem, mUndoContents);
        }
        return false;
    }

    @Override
    public boolean redo() {
        MultiSelectModifyUndoItem undoItem = new MultiSelectModifyUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = mPageIndex;
        undoItem.mOperType = mRedoOperType;
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

        if (MultiSelectConstants.OPER_MOVE_ANNOTS == mRedoOperType) {
            return moveAnnots(undoItem, mCurrentAnnots);
        } else if (MultiSelectConstants.OPER_GROUP_ANNOTS == mRedoOperType
                || MultiSelectConstants.OPER_UNGROUP_ANNOTS == mRedoOperType) {
            return groupAnnots(undoItem, mRedoOperType, mRedoGroups);
        } else if (MultiSelectConstants.OPER_MODIFY_PROPERTY == mRedoOperType) {
            return modifyProperty(undoItem, mRedoContents);
        }
        return false;
    }

    private boolean moveAnnots(MultiSelectModifyUndoItem undoItem, final HashMap<String, RectF> annots) {
        if (annots == null || annots.size() == 0) return false;
        try {
            final DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Iterator it = annots.entrySet().iterator();
            final ArrayList<Annot> annotArray = new ArrayList<>();
            int count = 0;
            final RectF newRects = new RectF();
            final RectF oldRects = new RectF();
            RectF _new = new RectF();
            RectF _old = new RectF();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String nm = (String) entry.getKey();
                Annot annot = documentManager.getAnnot(page, nm);
                if (annot == null || annot.isEmpty()) return false;
                annotArray.add(annot);
                RectF oldRect = AppUtil.toRectF(annot.getRect());
                undoItem.mLastAnnots.put(nm, oldRect);
                RectF newRect = (RectF) entry.getValue();
                undoItem.mCurrentAnnots.put(nm, newRect);
                undoItem.mNMList.add(nm);

                _new.set(newRect);
                _old.set(oldRect);
                mPdfViewCtrl.convertPdfRectToPageViewRect(_new, _new, mPageIndex);
                mPdfViewCtrl.convertPdfRectToPageViewRect(_old, _old, mPageIndex);
                if (count == 0) {
                    newRects.set(_new);
                    oldRects.set(_old);
                } else {
                    newRects.union(_new);
                    oldRects.union(_old);
                }
                count++;
            }
            if (count != annots.size()) return false;
            EditAnnotEvent modifyEvent = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, annotArray, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        try {
                            for (int i = 0; i < annotArray.size(); i++) {
                                documentManager.onAnnotModified(mPdfViewCtrl.getDoc().getPage(mPageIndex), annotArray.get(i));
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF invalidateRect = new RectF(newRects);
                            invalidateRect.union(oldRects);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(invalidateRect));
                        }
                    }
                }
            });

            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean groupAnnots(MultiSelectModifyUndoItem undoItem, final int operType, final HashMap<String, ArrayList<String>> groups) {
        try {
            final DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);

            undoItem.mNMList = mNMList;
            undoItem.mGroups = groups;

            final ArrayList<Annot> annotArray = new ArrayList<>();
            final RectF tempRectF = new RectF();
            for (int i = 0; i < undoItem.mNMList.size(); i++) {
                Annot annot = documentManager.getAnnot(page, undoItem.mNMList.get(i));
                if (annot == null || annot.isEmpty()) continue;
                annotArray.add(annot);

                RectF annotRectF = AppUtil.toRectF(annot.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, mPageIndex);
                if (i == 0) {
                    tempRectF.set(annotRectF);
                } else {
                    tempRectF.union(annotRectF);
                }
            }

            EditAnnotEvent modifyEvent = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, annotArray, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (MultiSelectConstants.OPER_GROUP_ANNOTS == operType)
                            documentManager.onAnnotGrouped(page, annotArray);
                        else
                            documentManager.onAnnotUnGrouped(page, annotArray);

                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF invalidateRect = new RectF(tempRectF);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(invalidateRect));
                        }
                    }
                }
            });

            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean modifyProperty(MultiSelectModifyUndoItem undoItem, HashMap<String, String> contents) {
        try {
            final DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            undoItem.mNMList = mNMList;
            undoItem.mContents = contents;

            final ArrayList<Annot> annotArray = new ArrayList<>();
            final RectF tempRectF = new RectF();
            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
            for (int i = 0; i < undoItem.mNMList.size(); i++) {
                Annot annot = documentManager.getAnnot(page, undoItem.mNMList.get(i));
                if (annot == null || annot.isEmpty()) continue;
                annotArray.add(annot);

                RectF pvRect = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                if (i == 0)
                    tempRectF.set(pvRect);
                else
                    tempRectF.union(pvRect);
            }

            EditAnnotEvent modifyEvent = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, annotArray, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        try {
                            for (int i = 0; i < annotArray.size(); i++) {
                                documentManager.onAnnotModified(mPdfViewCtrl.getDoc().getPage(mPageIndex), annotArray.get(i));
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF invalidateRect = new RectF(tempRectF);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(invalidateRect));
                        }
                    }
                }
            });

            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class MultiSelectDeleteUndoItem extends MultiSelectUndoItem {
    ArrayList<AnnotUndoItem> mUndoItemList;
    MultiSelectModule multiSelectModule;
    UIExtensionsManager uiExtensionsManager;

    public MultiSelectDeleteUndoItem(@NonNull PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
        uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        multiSelectModule = (MultiSelectModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_SELECT_ANNOTATIONS);
    }

    @Override
    public boolean undo() {
        uiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(true);
        final ArrayList<EditAnnotEvent> eventList = new ArrayList<>();
        for (AnnotUndoItem undoItem : mUndoItemList) {
            undoItem.mPageIndex = mPageIndex;
            undoItem.undo(new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success && event instanceof EditAnnotEvent) {
                        eventList.add((EditAnnotEvent) event);
                    }
                }
            });
        }
        MultiSelectEvent addEvent = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_ADD, eventList, mPdfViewCtrl);
        EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                uiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(false);
                if (success) {
                    try {
                        for (int i = 0; i < eventList.size(); i++) {
                            uiExtensionsManager.getDocumentManager().onAnnotAdded(mPdfViewCtrl.getDoc().getPage(mPageIndex), eventList.get(i).mAnnot);
                        }

                        DocumentManager documentManager = uiExtensionsManager.getDocumentManager();
                        PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
                        HashMap<String, ArrayList<String>> groups = mGroups;
                        for (ArrayList<String> arrayList : groups.values()) {
                            int size = arrayList.size();
                            MarkupArray markupArray = new MarkupArray();
                            ArrayList<Annot> groupAnnots = new ArrayList<>();
                            for (int i = 0; i < size; i++) {
                                Annot groupAnnot = documentManager.getAnnot(page, arrayList.get(i));
                                if (groupAnnot == null || groupAnnot.isEmpty() || !groupAnnot.isMarkup())
                                    continue;
                                markupArray.add((Markup) groupAnnot);
                                groupAnnots.add(groupAnnot);
                            }
                            if (markupArray.getSize() > 0)
                                GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, markupArray, 0);
                            documentManager.onAnnotGrouped(page, groupAnnots);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                    if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                        RectF rectF = calculateInvalidateRect(eventList);
                        mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(rectF));
                    }
                }
            }
        });
        mPdfViewCtrl.addTask(task);
        return true;
    }

    @Override
    public boolean redo() {
        uiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(true);
        final ArrayList<EditAnnotEvent> eventList = new ArrayList<>();
        for (AnnotUndoItem undoItem : mUndoItemList) {
            undoItem.mPageIndex = mPageIndex;
            undoItem.redo(new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success && event instanceof EditAnnotEvent) {
                        eventList.add((EditAnnotEvent) event);
                    }
                }
            });
        }
        final RectF rectF = calculateInvalidateRect(eventList);
        MultiSelectEvent delEvent = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_DELETE, eventList, mPdfViewCtrl);
        EditAnnotTask task = new EditAnnotTask(delEvent, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                uiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(false);
                if (success) {
                    try {
                        for (int i = 0; i < eventList.size(); i++) {
                            uiExtensionsManager.getDocumentManager().onAnnotDeleted(mPdfViewCtrl.getDoc().getPage(mPageIndex), eventList.get(i).mAnnot);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                    if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                        mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(rectF));
                    }
                }
            }
        });
        mPdfViewCtrl.addTask(task);
        return true;
    }

    private RectF calculateInvalidateRect(ArrayList<EditAnnotEvent> eventList) {
        int count = 0;
        RectF rect = new RectF();
        try {
            for (EditAnnotEvent event : eventList) {
                RectF tmpRect = AppUtil.toRectF(event.mAnnot.getRect());
                if (event.mAnnot.getType() == Annot.e_Caret) {
                    if (AppAnnotUtil.isReplaceCaret(event.mAnnot)) {
                        StrikeOut strikeOut = AppAnnotUtil.getStrikeOutFromCaret((Caret) event.mAnnot);
                        if (strikeOut != null) {
                            RectF sto_Rect = AppUtil.toRectF(strikeOut.getRect());
                            sto_Rect.union(tmpRect);
                            tmpRect.set(sto_Rect);
                        }
                    }
                }
                mPdfViewCtrl.convertPdfRectToPageViewRect(tmpRect, tmpRect, mPageIndex);
                if (count == 0) {
                    rect.set(tmpRect);
                } else {
                    rect.union(tmpRect);
                }
                count++;
            }
            return rect;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }
}
