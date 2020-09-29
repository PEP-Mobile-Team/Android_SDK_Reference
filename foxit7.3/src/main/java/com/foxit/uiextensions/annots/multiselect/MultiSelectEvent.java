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

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class MultiSelectEvent extends EditAnnotEvent {
    private ArrayList<Annot> mAnnots;
    private ArrayList<EditAnnotEvent> mAnnotList;

    public MultiSelectEvent(int eventType, MultiSelectUndoItem undoItem, ArrayList<Annot> annots, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnots = annots;
        mPdfViewCtrl = pdfViewCtrl;
    }

    public MultiSelectEvent(int eventType, ArrayList<EditAnnotEvent> annotList, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mAnnotList = annotList;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnotList == null || mAnnotList.size() == 0) return false;
        for (EditAnnotEvent event : mAnnotList) {
            event.add();
        }
        return true;
    }

    @Override
    public boolean modify() {
        if (mAnnots == null || mAnnots.size() == 0) return false;
        try {
            MultiSelectModifyUndoItem undoItem = (MultiSelectModifyUndoItem) mUndoItem;
            if (undoItem.mOperType == MultiSelectConstants.OPER_MOVE_ANNOTS) {
                for (Annot annot : mAnnots) {
                    if (mUndoItem.mModifiedDate != null) {
                        annot.setModifiedDateTime(mUndoItem.mModifiedDate);
                    }
                    String nm = AppAnnotUtil.getAnnotUniqueID(annot);
                    if (undoItem.mCurrentAnnots.get(nm) != null) {
                        if (annot.getType() == Annot.e_FreeText)
                            ((FreeText)annot).setDefaultAppearance(((FreeText) annot).getDefaultAppearance());
                        annot.move(AppUtil.toFxRectF(undoItem.mCurrentAnnots.get(nm)));
//                        annot.resetAppearanceStream();
                    }
                }
            } else if (undoItem.mOperType == MultiSelectConstants.OPER_GROUP_ANNOTS) {
                MarkupArray groups = new MarkupArray();
                for (Annot annot : mAnnots) {
                    if (mUndoItem.mModifiedDate != null) {
                        annot.setModifiedDateTime(mUndoItem.mModifiedDate);
                    }
                    String nm = AppAnnotUtil.getAnnotUniqueID(annot);
                    if (undoItem.mNMList.contains(nm) && annot.isMarkup()) {
                        groups.add((Markup) annot);
                    }
                }
                if (groups.getSize() > 0)
                    GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, mAnnots.get(0).getPage(), groups, 0);
            } else if (undoItem.mOperType == MultiSelectConstants.OPER_UNGROUP_ANNOTS) {
                Annot annot = mAnnots.get(0);
                if (AppAnnotUtil.isGrouped(annot))
                    GroupManager.getInstance().unGroup(((Markup) annot).getGroupHeader());

                DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
                PDFPage page = mPdfViewCtrl.getDoc().getPage(undoItem.mPageIndex);
                HashMap<String, ArrayList<String>> groups = undoItem.mGroups;
                for (ArrayList<String> arrayList : groups.values()) {
                    int size = arrayList.size();
                    MarkupArray markupArray = new MarkupArray();
                    for (int i = 0; i < size; i++) {
                        Annot groupAnnot = documentManager.getAnnot(page, arrayList.get(i));
                        if (groupAnnot == null || groupAnnot.isEmpty() || !groupAnnot.isMarkup())
                            continue;

                        if (mUndoItem.mModifiedDate != null) {
                            groupAnnot.setModifiedDateTime(mUndoItem.mModifiedDate);
                        }
                        markupArray.add((Markup) groupAnnot);
                    }
                    if (markupArray.getSize() > 0)
                        GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, markupArray, 0);
                }
            } else if (undoItem.mOperType == MultiSelectConstants.OPER_MODIFY_PROPERTY) {
                for (Annot annot : mAnnots) {
                    if (mUndoItem.mModifiedDate != null) {
                        annot.setModifiedDateTime(mUndoItem.mModifiedDate);
                    }
                    String nm = AppAnnotUtil.getAnnotUniqueID(annot);
                    if (undoItem.mContents.get(nm) != null) {
                        annot.setContent(undoItem.mContents.get(nm));
                        annot.resetAppearanceStream();
                    }
                }
            }
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete() {
        if (mAnnotList == null || mAnnotList.size() == 0) return false;
        for (EditAnnotEvent event : mAnnotList) {
            event.delete();
        }
        return true;
    }

    @Override
    public boolean flatten() {
        if (mAnnotList == null || mAnnotList.size() == 0) return false;
        for (EditAnnotEvent event : mAnnotList) {
            if (!event.flatten()) return false;
        }
        return true;
    }
}
