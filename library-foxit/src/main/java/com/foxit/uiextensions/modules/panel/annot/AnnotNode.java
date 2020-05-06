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
package com.foxit.uiextensions.modules.panel.annot;

import com.foxit.uiextensions.utils.AppDmUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AnnotNode implements Comparable<AnnotNode> {
    private int index;
    private final String uid;
    private final String replyTo;
    protected int counter;

    private final boolean pageDivider;

    private String type;
    private String author;
    private CharSequence contents;
    private String modifiedDate;
    private String creationDate;
    private String intent;
    private boolean canDelete;
    private boolean canReply;
    private boolean canComment;
    private AnnotNode parent;
    private List<AnnotNode> children;
    private boolean checked;
    private boolean isLocked;
    private boolean isReadOnly;
    private boolean canEdit;
    private boolean canApplyRedaction;

    public AnnotNode(int index, String uid, String replyTo) {
        this.index = index;
        this.uid = uid;
        this.replyTo = replyTo;//annotation uid
        this.pageDivider = false;
    }

    public AnnotNode(int index) {
        this.index = index;
        this.uid = null;
        this.replyTo = null;
        this.pageDivider = true;
    }

    public boolean isPageDivider() {
        return this.pageDivider;
    }

    public int getPageIndex() {
        return index;
    }

    public void setPageIndex(int index) {
        this.index = index;
    }

    public String getUID() {
        return this.uid == null ? "" : this.uid;
    }

    public String getReplyTo() {
        return this.replyTo == null ? "" : this.replyTo;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAuthor() {
        return this.author == null ? "" : this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setContent(CharSequence contents) {
        this.contents = contents;
    }

    public CharSequence getContent() {
        return this.contents == null ? "" : this.contents;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getModifiedDate() {
        return this.modifiedDate == null ? AppDmUtil.dateOriValue : this.modifiedDate;
    }

    public String getCreationDate() {
        return this.creationDate == null ? AppDmUtil.dateOriValue : this.creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public void setParent(AnnotNode parent) {
        this.parent = parent;
    }

    public AnnotNode getParent() {
        return this.parent;
    }

    public boolean isRootNode() {
        return this.parent == null;
    }

    public boolean isLeafNode() {
        return this.children == null || this.children.size() == 0;
    }

    public void addChildNode(AnnotNode note) {
        if (this.children == null) {
            this.children = new ArrayList<AnnotNode>();
        }
        if (!this.children.contains(note)) {
            this.children.add(note);
        }
    }

    // Recursively delete all child nodes.
    public void removeChildren() {
        if (this.children != null) {
            for (int i = 0; i < this.children.size(); i++) {
                this.children.get(i).removeChildren();
                this.children.get(i).setParent(null);
            }
            this.children.clear();
        }
    }

    // Remove the node from the current node and will remove all the child nodes of the node.
    public void removeChild(AnnotNode node) {
        if (this.children != null && this.children.contains(node)) {
            node.removeChildren();
            node.setParent(null);
            this.children.remove(node);
        }
    }

    public List<AnnotNode> getChildren() {
        return this.children;
    }

    public int getLevel() {
        if (pageDivider) return -1;
        return this.parent == null ? 0 : parent.getLevel() + 1;
    }

    public void setChecked(boolean isChecked) {
        if (!this.pageDivider) {
            this.checked = isChecked;
        }
    }

    public boolean isChecked() {
        return this.checked;
    }

    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    public boolean isLocked() {
        return this.isLocked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AnnotNode)) return false;
        AnnotNode another = (AnnotNode) o;
        return this.pageDivider == another.pageDivider && this.getPageIndex() == another.getPageIndex() && this.getUID().equals(another.getUID());
    }

    @Override
    public int compareTo(AnnotNode another) {
        if (another == null) return 0;
        if (getPageIndex() != another.getPageIndex())
            return getPageIndex() - another.getPageIndex();
        if (getLevel() != another.getLevel()) {
            return getLevel() - another.getLevel();
        }
        try {
            Date lDate = AppDmUtil.documentDateToJavaDate(AppDmUtil.parseDocumentDate(getCreationDate()));
            Date rDate = AppDmUtil.documentDateToJavaDate(AppDmUtil.parseDocumentDate(another.getCreationDate()));
            if (lDate == null || rDate == null)
                return 0;

            return lDate.before(rDate) ? -1 : (lDate.after(rDate) ? 1 : 0);
        } catch (Exception e) {
        }
        return 0;
    }

    public boolean isRedundant() {
        return !(this.getReplyTo().equals("") || (this.parent != null && !this.parent.isRedundant()));

    }

    public boolean canDelete() {
        return canDelete;
    }

    public void setDeletable(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public boolean canReply() {
        return canReply;
    }

    public void setCanReply(boolean canReply) {
        this.canReply = canReply;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnlyFlag(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public void setEditable(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean canApplyRedaction(){
        return canApplyRedaction;
    }

    public void setApplyRedaction(boolean canApplyRedaction) {
        this.canApplyRedaction = canApplyRedaction;
    }

    public boolean canComment() {
        return !(isLocked || isReadOnly);
    }
}
