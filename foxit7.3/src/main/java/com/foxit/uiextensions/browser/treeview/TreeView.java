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


public class TreeView {
    private TreeNode mRoot;
    private Context mContext;
    private TreeNode.TreeNodeClickListener nodeClickListener;

    public TreeView(Context context, TreeNode root) {
        mRoot = root;
        mContext = context;
    }

    public void setNodeClickListener(TreeNode.TreeNodeClickListener listener) {
        nodeClickListener = listener;
    }

    public View getView() {
        final ViewGroup view = new HVScrollView(mContext);
        final LinearLayout viewTreeItems = new LinearLayout(mContext);
        viewTreeItems.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(layoutParams);
        view.addView(viewTreeItems);

        mRoot.setViewHolder(new TreeNode.BaseNodeViewHolder(mContext) {
            @Override
            public View createNodeView(TreeNode node, Object value) {
                return null;
            }

            @Override
            public void toggle(boolean active) {
            }

            @Override
            public ViewGroup getNodeItemsView() {
                return viewTreeItems;
            }
        });
        expandNode(mRoot);
        return view;
    }

    public void toggleNode(TreeNode node) {
        if (node.isExpanded()) {
            collapseNode(node);
        } else {
            expandNode(node);
        }
    }

    private void collapseNode(TreeNode node) {
        node.setExpanded(false);
        TreeNode.BaseNodeViewHolder nodeViewHolder = getNodeViewHolder(node);
        nodeViewHolder.getNodeItemsView().setVisibility(View.GONE);
        nodeViewHolder.toggle(false);
    }

    private void expandNode(final TreeNode node) {
        node.setExpanded(true);
        final TreeNode.BaseNodeViewHolder parentViewHolder = getNodeViewHolder(node);
        parentViewHolder.getNodeItemsView().removeAllViews();
        parentViewHolder.toggle(true);

        for (TreeNode n : node.getChildrenNodes()) {
            addNode(parentViewHolder.getNodeItemsView(), n);
            if (n.isExpanded()) {
                expandNode(n);
            }
        }
        parentViewHolder.getNodeItemsView().setVisibility(View.VISIBLE);
        parentViewHolder.getNodeItemsView().getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
    }

    private void addNode(ViewGroup container, final TreeNode n) {
        final TreeNode.BaseNodeViewHolder viewHolder = getNodeViewHolder(n);
        final View nodeView = viewHolder.getView();
        container.addView(nodeView);

        nodeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeClickListener != null) {
                    nodeClickListener.onClick(n, n.getValue());
                }
                toggleNode(n);
            }
        });
    }

    private TreeNode.BaseNodeViewHolder getNodeViewHolder(TreeNode node) {
        TreeNode.BaseNodeViewHolder viewHolder = node.getViewHolder();
        if (viewHolder.getTreeView() == null)
            viewHolder.setTreeView(this);
        return viewHolder;
    }

    private ILastNodeChangeListener mLastNodeChangeListener;

    public void setLastNodeChangeLisener(ILastNodeChangeListener lastChangeListener) {
        this.mLastNodeChangeListener = lastChangeListener;
    }

    public interface ILastNodeChangeListener {
        void onNodeChanged(TreeNode node);
    }

    public ILastNodeChangeListener getLastNodeChangeListener() {
        return mLastNodeChangeListener;
    }

}
