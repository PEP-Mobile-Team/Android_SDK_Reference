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
package com.foxit.uiextensions.annots.ink;

import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class EraserUndoItem extends AnnotUndoItem {
    private ArrayList<InkUndoItem> undoItems;
    private ArrayList<EditAnnotEvent> mEvents = new ArrayList<>();

    public EraserUndoItem(PDFViewCtrl pdfViewCtrl){
        mPdfViewCtrl = pdfViewCtrl;
        undoItems = new ArrayList<InkUndoItem>();
    }

    public void addUndoItem(InkUndoItem undoItem){
        undoItems.add(undoItem);
    }

    public ArrayList<InkUndoItem> getUndoItems() {
        return undoItems;
    }

    @Override
    public boolean undo() {
        mEvents.clear();
        int eventType = -1;
        int size = undoItems.size();
        for (int i = size - 1; i >= 0; i--) {
            InkUndoItem undoItem = undoItems.get(i);
            if (undoItem instanceof InkModifyUndoItem) {
                eventType = EditAnnotEvent.EVENTTYPE_MODIFY;
                InkModifyUndoItem item = (InkModifyUndoItem) undoItem;
                item.undo(callback);
            } else if (undoItem instanceof InkDeleteUndoItem){
                eventType = EditAnnotEvent.EVENTTYPE_ADD;
                InkDeleteUndoItem item = (InkDeleteUndoItem) undoItem;
                item.undo(callback);
            }
        }
        doTask(eventType);
        return true;
    }

    @Override
    public boolean redo() {
        mEvents.clear();
        int eventType = -1;
        for (InkUndoItem undoItem : undoItems) {
            if (undoItem instanceof InkModifyUndoItem) {
                eventType = EditAnnotEvent.EVENTTYPE_MODIFY;
                InkModifyUndoItem item = (InkModifyUndoItem) undoItem;
                item.redo(callback);
            } else if (undoItem instanceof InkDeleteUndoItem){
                eventType = EditAnnotEvent.EVENTTYPE_DELETE;
                InkDeleteUndoItem item = (InkDeleteUndoItem) undoItem;
                item.redo(callback);
            }
        }

        doTask(eventType);
        return true;
    }

    Event.Callback callback = new Event.Callback() {
        @Override
        public void result(Event event, boolean success) {
            if (success && event instanceof EditAnnotEvent) {
                mEvents.add((EditAnnotEvent) event);
            }
        }
    };

    private RectF calculateInvalidateRect(ArrayList<EditAnnotEvent> eventList){
        int count = 0;
        RectF rect = new RectF();
        try {
            for (EditAnnotEvent event : eventList) {
                RectF tmpRect = AppUtil.toRectF(event.mAnnot.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(tmpRect, tmpRect, event.mUndoItem.mPageIndex);
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

    private void doTask(final int eventType) {
        final RectF rectF = calculateInvalidateRect(mEvents);
        EraserEvent eraserEvent = new EraserEvent(eventType, mEvents);
        EditAnnotTask task = new EditAnnotTask(eraserEvent, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success) {
                    doEvents(eventType);
                    if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                        mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(rectF));
                    }
                }
            }
        });
        mPdfViewCtrl.addTask(task);
    }

    private void doEvents(int eventType) {
        DocumentManager documentManager = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        for (int i = 0; i < mEvents.size(); i ++) {
            try {
                PDFPage pdfPage = mPdfViewCtrl.getDoc().getPage(mEvents.get(i).mUndoItem.mPageIndex);
                switch (eventType) {
                    case EditAnnotEvent.EVENTTYPE_ADD:
                        documentManager.onAnnotAdded(pdfPage, mEvents.get(i).mAnnot);
                        break;
                    case EditAnnotEvent.EVENTTYPE_MODIFY:
                        documentManager.onAnnotModified(pdfPage, mEvents.get(i).mAnnot);
                        break;
                    case EditAnnotEvent.EVENTTYPE_DELETE:
                        documentManager.onAnnotDeleted(pdfPage, mEvents.get(i).mAnnot);
                        break;
                    default:
                        break;
                }
            } catch (PDFException e) {
            }
        }

    }
}
