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
package com.foxit.uiextensions.annots.multiselect;

import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
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
    ArrayList<String> mNMList = new ArrayList<>();
    public MultiSelectUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    public boolean isSelected(@NonNull String nm) {
        if (mNMList.size() == 0) return false;
        return mNMList.contains(nm);
    }
}

class MultiSelectModifyUndoItem extends MultiSelectUndoItem {

    public HashMap<String, RectF> mLastAnnots = new HashMap<String, RectF>();
    public HashMap<String, RectF> mCurrentAnnots = new HashMap<String, RectF>();
    public MultiSelectModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        return modifyAnnots(mLastAnnots);
    }

    @Override
    public boolean redo() {
        return modifyAnnots(mCurrentAnnots);
    }

    private boolean modifyAnnots(final HashMap<String, RectF> annots) {
        if (annots == null || annots.size() == 0) return false;
        final DocumentManager documentManager = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        try {
            MultiSelectModifyUndoItem undoItem = new MultiSelectModifyUndoItem(mPdfViewCtrl);
            undoItem.mPageIndex = mPageIndex;
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Iterator it = annots.entrySet().iterator();
            final ArrayList<Annot> annotArray = new ArrayList<>();
            int count = 0;
            final RectF newRects = new RectF();
            final RectF oldRects = new RectF();
            RectF _new = new RectF();
            RectF _old = new RectF();
            while (it.hasNext()){
                Map.Entry entry= (Map.Entry) it.next();
                String nm = (String)entry.getKey();
                Annot annot = documentManager.getAnnot(page, nm);
                if (annot == null || annot.isEmpty()) return false;
                annotArray.add(annot);
                RectF oldRect = AppUtil.toRectF(annot.getRect());
                undoItem.mLastAnnots.put(nm, oldRect);
                RectF newRect = (RectF)entry.getValue();
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
                count ++;
            }
            if (count != annots.size()) return false;
            EditAnnotEvent modifyEvent = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, annotArray, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        try {
                            for (int i = 0; i < annotArray.size(); i ++) {
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
                        for (int i = 0; i < eventList.size(); i ++) {
                            uiExtensionsManager.getDocumentManager().onAnnotAdded(mPdfViewCtrl.getDoc().getPage(mPageIndex), eventList.get(i).mAnnot);
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
                        for (int i = 0; i < eventList.size(); i ++) {
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

    private RectF calculateInvalidateRect(ArrayList<EditAnnotEvent> eventList){
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
                count ++;
            }
            return rect;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }
}
