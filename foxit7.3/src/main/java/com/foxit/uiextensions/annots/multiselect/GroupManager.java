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
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class GroupManager {

    private static GroupManager instance;
    private HashMap<String, String> mGroupMembers = new HashMap<>();
    private HashMap<String, GroupInfo> mGroupMaps = new HashMap<>();
    private boolean isLoadGroupModule = false;

    private GroupManager() {
    }

    public static GroupManager getInstance() {
        if (instance == null)
            instance = new GroupManager();
        return instance;
    }

    public void setLoadGroupModule(boolean isLoadGroupModule) {
        this.isLoadGroupModule = isLoadGroupModule;
    }

    public synchronized boolean isGrouped(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (annot == null || annot.isEmpty())
            return false;

        if (isLoadGroupModule
                && AppAnnotUtil.isGrouped(annot)
                && AppAnnotUtil.isSupportAnnotGroup(annot)) {
            String headerUID = mGroupMembers.get(AppAnnotUtil.getAnnotUniqueID(annot));
            if (AppUtil.isEmpty(headerUID) || mGroupMaps.get(headerUID) == null) {
                GroupInfo groupInfo = updateGroupInfo(pdfViewCtrl, annot);
                return groupInfo != null && groupInfo.canSelect;
            } else {
                GroupInfo groupInfo = mGroupMaps.get(headerUID);
                return groupInfo != null && groupInfo.canSelect;
            }
        }
        return false;
    }

    public synchronized boolean canReply(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (annot == null || annot.isEmpty())
            return false;

        String headerUID = mGroupMembers.get(AppAnnotUtil.getAnnotUniqueID(annot));
        if (AppUtil.isEmpty(headerUID) || mGroupMaps.get(headerUID) == null) {
            GroupInfo groupInfo = updateGroupInfo(pdfViewCtrl, annot);
            return groupInfo != null && groupInfo.canReply;
        } else {
            GroupInfo groupInfo = mGroupMaps.get(headerUID);
            return groupInfo != null && groupInfo.canReply;
        }
    }

    public synchronized boolean canDelete(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (annot == null || annot.isEmpty())
            return false;

        String headerUID = mGroupMembers.get(AppAnnotUtil.getAnnotUniqueID(annot));
        if (AppUtil.isEmpty(headerUID) || mGroupMaps.get(headerUID) == null) {
            GroupInfo groupInfo = updateGroupInfo(pdfViewCtrl, annot);
            return groupInfo != null && groupInfo.canDelete;
        } else {
            GroupInfo groupInfo = mGroupMaps.get(headerUID);
            return groupInfo != null && groupInfo.canDelete;
        }
    }

    public synchronized String getHeaderUniqueID(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (annot == null || annot.isEmpty())
            return "";

        String headerUID = mGroupMembers.get(AppAnnotUtil.getAnnotUniqueID(annot));
        if (AppUtil.isEmpty(headerUID) || mGroupMaps.get(headerUID) == null) {
            GroupInfo groupInfo = updateGroupInfo(pdfViewCtrl, annot);
            return groupInfo != null ? groupInfo.groupHeaderUniqueID : "";
        } else {
            GroupInfo groupInfo = mGroupMaps.get(headerUID);
            return groupInfo != null ? groupInfo.groupHeaderUniqueID : "";
        }
    }

    public synchronized boolean contentsModifiable(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (annot == null || annot.isEmpty())
            return false;

        String headerUID = mGroupMembers.get(AppAnnotUtil.getAnnotUniqueID(annot));
        if (AppUtil.isEmpty(headerUID) || mGroupMaps.get(headerUID) == null) {
            GroupInfo groupInfo = updateGroupInfo(pdfViewCtrl, annot);
            return groupInfo != null && groupInfo.contentsModifiable;
        } else {
            GroupInfo groupInfo = mGroupMaps.get(headerUID);
            return groupInfo != null && groupInfo.contentsModifiable;
        }
    }

    public boolean removeGroupInfo(Annot annot) {
        if (isLoadGroupModule
                && AppAnnotUtil.isGrouped(annot)
                && AppAnnotUtil.isSupportAnnotGroup(annot)) {

            String headerUID = mGroupMembers.get(AppAnnotUtil.getAnnotUniqueID(annot));
            if (!AppUtil.isEmpty(headerUID)) {
                GroupInfo groupInfo = mGroupMaps.get(headerUID);
                if (groupInfo != null)
                    mGroupMaps.remove(headerUID);

                for (Iterator<String> iterator = mGroupMembers.values().iterator(); iterator.hasNext(); ) {
                    String value = iterator.next();
                    if (headerUID.equals(value)) {
                        iterator.remove();
                    }
                }
            }
        }
        return true;
    }

    public ArrayList<String> getGroupUniqueIDs(PDFViewCtrl pdfViewCtrl, Annot annot) {
        ArrayList<String> groupUniqueIDs = new ArrayList<>();

        if (AppAnnotUtil.isSupportAnnotGroup(annot)
                && AppAnnotUtil.isGrouped(annot)) {
            String headerUID = mGroupMembers.get(AppAnnotUtil.getAnnotUniqueID(annot));
            if (AppUtil.isEmpty(headerUID) || mGroupMaps.get(headerUID) == null) {
                GroupInfo groupInfo = updateGroupInfo(pdfViewCtrl, annot);
                return groupInfo != null ? groupInfo.groupUniqueIDs : groupUniqueIDs;
            } else {
                GroupInfo groupInfo = mGroupMaps.get(headerUID);
                return groupInfo != null ? groupInfo.groupUniqueIDs : groupUniqueIDs;
            }
        }
        return groupUniqueIDs;
    }

    public void setAnnotGroup(PDFViewCtrl pdfViewCtrl, PDFPage page, ArrayList<String> groupList) {
        if (page == null || page.isEmpty() || groupList.size() < 2)
            return;

        try {
            MarkupArray markupArray = new MarkupArray();
            int groupSize = groupList.size();
            for (int i = 0; i < groupSize; i++) {
                Annot groupAnnot = AppAnnotUtil.getAnnot(page, groupList.get(i));
                if (groupAnnot == null || groupAnnot.isEmpty() || !groupAnnot.isMarkup())
                    continue;
                markupArray.add((Markup) groupAnnot);
            }

            if (markupArray.getSize() > 0) {
                page.setAnnotGroup(markupArray, 0);
                updateGroupInfo(pdfViewCtrl, markupArray.getAt(0));
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void setAnnotGroup(PDFViewCtrl pdfViewCtrl, PDFPage page, MarkupArray markupArray, int headerIndex) {
        if (page == null || page.isEmpty() || markupArray.getSize() < 2)
            return;

        try {
            page.setAnnotGroup(markupArray, headerIndex);
            updateGroupInfo(pdfViewCtrl, markupArray.getAt(0));
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public boolean unGroup(PDFPage page, String uniqueID) {
        if (AppUtil.isEmpty(uniqueID))
            return false;
        try {
            Annot annot = AppAnnotUtil.getAnnot(page, uniqueID);
            if (!AppAnnotUtil.isGrouped(annot))
                return false;

            removeGroupInfo(annot);
            return ((Markup) annot).ungroup();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean unGroup(Annot annot) {
        if (annot == null || annot.isEmpty())
            return false;

        try {
            removeGroupInfo(annot);
            return ((Markup) annot).ungroup();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public GroupInfo updateGroupInfo(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (isLoadGroupModule
                && AppAnnotUtil.isGrouped(annot)
                && AppAnnotUtil.isSupportAnnotGroup(annot)) {
            try {
                UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
                ArrayList<Annot> loadAnnots = new ArrayList<>();
                ArrayList<String> groupNMs = new ArrayList<>();
                MarkupArray markupArray = ((Markup) annot).getGroupElements();

                Annot headerAnnot = ((Markup) annot).getGroupHeader();
                if (headerAnnot.isEmpty()) {
                    annot.getPage().setAnnotGroup(markupArray, 0);
                    headerAnnot = ((Markup) annot).getGroupHeader();
                }

                String groupUniqueID = AppAnnotUtil.getAnnotUniqueID(headerAnnot);
                boolean canReply = true;
                boolean canDelete = true;
                boolean contentsModifiable = true;
                for (int i = 0; i < markupArray.getSize(); i++) {
                    Annot groupAnnot = AppAnnotUtil.createAnnot(markupArray.getAt(i));
                    if (groupAnnot == null || groupAnnot.isEmpty())
                        continue;

                    if (canReply)
                        canReply = AppAnnotUtil.isAnnotSupportReply(groupAnnot) && !AppAnnotUtil.isReadOnly(groupAnnot);
                    if (contentsModifiable)
                        contentsModifiable = AppAnnotUtil.contentsModifiable(AppAnnotUtil.getTypeString(groupAnnot));
                    if (uiExtensionsManager.isLoadAnnotModule(groupAnnot)) {
                        loadAnnots.add(groupAnnot);
                        if (canDelete)
                            canDelete = AppAnnotUtil.isSupportDeleteAnnot(groupAnnot) && !(AppAnnotUtil.isLocked(groupAnnot) || AppAnnotUtil.isReadOnly(groupAnnot));
                    }

                    String uid = AppAnnotUtil.getAnnotUniqueID(groupAnnot);
                    groupNMs.add(uid);
                    mGroupMembers.put(uid, groupUniqueID);
                }

                GroupInfo groupInfo = new GroupInfo();
                groupInfo.canSelect = loadAnnots.size() > 1;
                groupInfo.canReply = canReply;
                groupInfo.canDelete = canDelete;
                groupInfo.canComment = false;
                groupInfo.contentsModifiable = contentsModifiable;
                groupInfo.groupHeaderUniqueID = groupUniqueID;
                groupInfo.groupUniqueIDs = groupNMs;
                mGroupMaps.put(groupUniqueID, groupInfo);
                return groupInfo;
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void clear() {
        mGroupMaps.clear();
        mGroupMembers.clear();
    }

}
