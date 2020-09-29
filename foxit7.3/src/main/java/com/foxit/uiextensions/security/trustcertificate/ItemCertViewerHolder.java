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


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.browser.treeview.TreeNode;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.Event;

public class ItemCertViewerHolder extends TreeNode.BaseNodeViewHolder<CertificateFileInfo> implements View.OnClickListener {

    private ImageView toggleView;
    private ViewGroup mMoreView;
    private ViewGroup mTrustCertView;

    private TreeNode mNode;
    private CertificateFileInfo mFileInfo;
    private TrustCertificatePresenter mPresenter;
    private PDFViewCtrl mPDFViewCtrl;

    public ItemCertViewerHolder(Context context, PDFViewCtrl pdfViewCtrl) {
        super(context);
        mPresenter = new TrustCertificatePresenter(context, pdfViewCtrl);
        mPDFViewCtrl = pdfViewCtrl;
    }

    @Override
    public View createNodeView(TreeNode node, CertificateFileInfo value) {
        mNode = node;
        mFileInfo = value;
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.cert_viewer_item, null, false);

        TextView tvTitle = view.findViewById(R.id.tv_certificate_title);
        TextView tvDate = view.findViewById(R.id.tv_certificate_date);
        ImageView ivMore = view.findViewById(R.id.iv_cert_more);
        toggleView = view.findViewById(R.id.iv_toggle_icon);
        mMoreView = view.findViewById(R.id.ll_certificate_more_view);
        View detailView = view.findViewById(R.id.rd_certificate_detail);
        mTrustCertView = view.findViewById(R.id.rd_add_trust_ceatificate);

        tvTitle.setText(value.subject);
        tvDate.setText(value.validTo);
        refreshTrustState();
        if (node.isLeaf())
            toggleView.setVisibility(View.INVISIBLE);
        ivMore.setOnClickListener(this);
        detailView.setOnClickListener(this);
        mTrustCertView.setOnClickListener(this);
        return view;
    }

    @Override
    public void toggle(boolean active) {
        toggleView.setImageResource(active ? R.drawable.rd_collapse_arrow : R.drawable.rd_expand_arrow);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_cert_more) {
            mMoreView.setVisibility(View.VISIBLE);

            if (getTreeView().getLastNodeChangeListener() != null)
                getTreeView().getLastNodeChangeListener().onNodeChanged(mNode);
        } else if (id == R.id.rd_certificate_detail) {
            mMoreView.setVisibility(View.GONE);
            mPresenter.viewCertInfo(mFileInfo);
        } else if (id == R.id.rd_add_trust_ceatificate) {
            mMoreView.setVisibility(View.GONE);
            final UITextEditDialog dialog = new UITextEditDialog(((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getAttachedActivity());
            dialog.getInputEditText().setVisibility(View.GONE);
            dialog.setTitle(AppResource.getString(mContext, R.string.menu_more_item_trust_certificate));
            dialog.getPromptTextView().setText(AppResource.getString(mContext, R.string.rv_add_trust_certificate_prompt));
            dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPresenter.addTrustCert(mFileInfo, new Event.Callback() {
                        @Override
                        public void result(Event event, boolean success) {
                            dialog.dismiss();
                            if (success) {
                                mFileInfo.isTrustCert = true;
                                refreshTrustState();
                            }
                        }
                    });
                }
            });
            dialog.show();
        }
    }

    private void refreshTrustState() {
        boolean enabled = !mFileInfo.isTrustCert;
        mTrustCertView.setEnabled(enabled);
        int childCount = mTrustCertView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = mTrustCertView.getChildAt(i);
            childView.setEnabled(enabled);
        }
    }

    public void onLastNodeChanged() {
        if (mMoreView != null && mMoreView.getVisibility() == View.VISIBLE)
            mMoreView.setVisibility(View.GONE);
    }

    public void onNodeClikCallback() {
        if (mMoreView != null && mMoreView.getVisibility() == View.VISIBLE)
            mMoreView.setVisibility(View.GONE);

        if (getTreeView().getLastNodeChangeListener() != null)
            getTreeView().getLastNodeChangeListener().onNodeChanged(mNode);
    }

}
