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
package com.foxit.uiextensions.security.trustcertificate;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.browser.treeview.TreeNode;
import com.foxit.uiextensions.browser.treeview.TreeView;
import com.foxit.uiextensions.controls.dialog.BaseDialogFragment;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

import java.util.ArrayList;
import java.util.List;

public class CertificateViewerFragment extends BaseDialogFragment {

    private PDFViewCtrl mPDFViewCtrl;
    private List<CertificateFileInfo> mCertFileInfos = new ArrayList<>();

    public void init(PDFViewCtrl pdfViewCtrl, List<CertificateFileInfo> certificateFileInfos) {
        mPDFViewCtrl = pdfViewCtrl;
        mCertFileInfos = certificateFileInfos;
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.cert_viewer_layout, container, false);
        initTopBar(view);
        initTreeView(view);
        return view;
    }

    @NonNull
    @Override
    protected PDFViewCtrl getPDFViewCtrl() {
        return mPDFViewCtrl;
    }

    @Override
    protected void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
    }

    private void initTopBar(View parentView) {
        LinearLayout topBarLayout = parentView.findViewById(R.id.panel_verify_cert_list_title);
        TopBarImpl topBar = new TopBarImpl(mContext);
        topBar.setBackgroundResource(R.color.ux_bg_color_toolbar_colour);
        IBaseItem backItem = new BaseItemImpl(mContext);
        backItem.setImageResource(R.drawable.panel_topbar_close_selector);
        backItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CertificateViewerFragment.this.dismiss();
            }
        });
        topBar.addView(backItem, BaseBar.TB_Position.Position_LT);

        IBaseItem titleItem = new BaseItemImpl(mContext);
        titleItem.setText(AppResource.getString(mContext, R.string.rv_view_certificate_info));
        titleItem.setTextColorResource(R.color.ux_text_color_title_light);
        titleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimension(R.dimen.ux_text_height_title)));
        topBar.addView(titleItem, BaseBar.TB_Position.Position_CENTER);

        topBarLayout.removeAllViews();
        topBarLayout.addView(topBar.getContentView());
    }

    private void initTreeView(View parentView) {
        TreeNode root = new TreeNode(null);
        TreeNode parentNode = new TreeNode(mCertFileInfos.get(0));
        parentNode.setViewHolder(new ItemCertViewerHolder(getContext(), mPDFViewCtrl));
        TreeNode currentNode = parentNode;
        for (int i = 1; i < mCertFileInfos.size(); i++) {
            TreeNode certInfo = new TreeNode(mCertFileInfos.get(i));
            certInfo.setViewHolder(new ItemCertViewerHolder(getContext(), mPDFViewCtrl));
            currentNode.addChild(certInfo);
            currentNode = certInfo;
        }
        root.addChild(parentNode);

        TreeView treeView = new TreeView(mContext, root);
        treeView.setNodeClickListener(mNodeClickListener);
        treeView.setLastNodeChangeLisener(mLastNodeChangeListener);

        ViewGroup containerView = (ViewGroup) parentView.findViewById(R.id.treeview_container);
        containerView.addView(treeView.getView());
    }

    private ItemCertViewerHolder mLastItemViewHolder;
    private TreeView.ILastNodeChangeListener mLastNodeChangeListener = new TreeView.ILastNodeChangeListener() {
        @Override
        public void onNodeChanged(TreeNode node) {
            ItemCertViewerHolder holder = (ItemCertViewerHolder) node.getViewHolder();
            if (mLastItemViewHolder != null && holder != mLastItemViewHolder)
                mLastItemViewHolder.onLastNodeChanged();
            mLastItemViewHolder = holder;
        }
    };

    private TreeNode.TreeNodeClickListener mNodeClickListener = new TreeNode.TreeNodeClickListener() {
        @Override
        public void onClick(TreeNode node, Object value) {
            ItemCertViewerHolder holder = (ItemCertViewerHolder) node.getViewHolder();
            holder.onNodeClikCallback();
        }
    };

}
