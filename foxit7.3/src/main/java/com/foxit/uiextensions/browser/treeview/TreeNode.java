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
package com.foxit.uiextensions.browser.treeview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TreeNode {
    private TreeNode mParent;
    private List<TreeNode> childrenNodes;
    private BaseNodeViewHolder mViewHolder;
    private Object mValue;

    private int mId;
    private int mChildId;
    private boolean mExpanded;

    public TreeNode(Object value) {
        childrenNodes = new ArrayList<>();
        mValue = value;
    }

    public boolean addChild(TreeNode childNode) {
        childNode.mParent = this;
        childNode.mId = ++mChildId;
        return childrenNodes.add(childNode);
    }

    public List<TreeNode> getChildrenNodes() {
        return Collections.unmodifiableList(childrenNodes);
    }

    public int size() {
        return childrenNodes.size();
    }

    public TreeNode getParent() {
        return mParent;
    }

    public int getId() {
        return mId;
    }

    public boolean isLeaf() {
        return size() == 0;
    }

    public Object getValue() {
        return mValue;
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
    }

    public void setViewHolder(BaseNodeViewHolder viewHolder) {
        mViewHolder = viewHolder;
        if (viewHolder != null)
            viewHolder.mNode = this;
    }

    public BaseNodeViewHolder getViewHolder() {
        return mViewHolder;
    }

    public static abstract class BaseNodeViewHolder<T> {
        private TreeView tView;
        private TreeNode mNode;
        private View mView;

        protected Context mContext;

        public BaseNodeViewHolder(Context context) {
            this.mContext = context;
        }

        public View getView() {
            if (mView != null) {
                return mView;
            }

            LinearLayout linearLayout = new LinearLayout(mContext);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            ViewGroup nodeHeader = new RelativeLayout(mContext);
            nodeHeader.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            nodeHeader.setId(R.id.node_header);

            LinearLayout nodeItems = new LinearLayout(mContext);
            nodeItems.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            nodeItems.setPadding(20, 0, 0, 0);
            nodeItems.setId(R.id.node_items);
            nodeItems.setOrientation(LinearLayout.VERTICAL);
            nodeItems.setVisibility(View.GONE);

            linearLayout.addView(nodeHeader);
            linearLayout.addView(nodeItems);

            final View nodeView = getNodeView();
            nodeHeader.addView(nodeView);
            mView = linearLayout;
            return mView;
        }

        public void setTreeView(TreeView treeView) {
            this.tView = treeView;
        }

        public TreeView getTreeView() {
            return tView;
        }

        public View getNodeView() {
            return createNodeView(mNode, (T) mNode.getValue());
        }

        public ViewGroup getNodeItemsView() {
            return (ViewGroup) getView().findViewById(R.id.node_items);
        }

        public abstract View createNodeView(TreeNode node, T value);

        public abstract void toggle(boolean active);
    }

    public interface TreeNodeClickListener {
        void onClick(TreeNode node, Object value);
    }

}
