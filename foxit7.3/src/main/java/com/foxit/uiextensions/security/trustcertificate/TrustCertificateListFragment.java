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


import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.BaseDialogFragment;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

public class TrustCertificateListFragment extends BaseDialogFragment {

    private PDFViewCtrl mPDFViewCtrl;
    private TrustCertificateListAdapter mAdapter;

    public void init(PDFViewCtrl pdfViewCtrl) {
        mPDFViewCtrl = pdfViewCtrl;
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.panel_trust_certificate_layout, container, false);
        initTopBar(view);
        initListView(view);
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
        LinearLayout topBarLayout = parentView.findViewById(R.id.panel_trust_certificate_title);
        TopBarImpl topBar = new TopBarImpl(mContext);
        topBar.setBackgroundResource(R.color.ux_bg_color_toolbar_colour);
        IBaseItem backItem = new BaseItemImpl(mContext);
        backItem.setImageResource(R.drawable.cloud_back);
        backItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TrustCertificateListFragment.this.dismiss();
            }
        });
        topBar.addView(backItem, BaseBar.TB_Position.Position_LT);

        IBaseItem titleItem = new BaseItemImpl(mContext);
        titleItem.setText(AppResource.getString(mContext, R.string.menu_more_item_trust_certificate));
        titleItem.setTextColorResource(R.color.ux_text_color_title_light);
        titleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimension(R.dimen.ux_text_height_title)));
        topBar.addView(titleItem, BaseBar.TB_Position.Position_CENTER);

        IBaseItem addItem = new BaseItemImpl(mContext);
        addItem.setText(AppResource.getString(mContext, R.string.fx_string_add));
        addItem.setTextColorResource(R.color.ux_text_color_title_light);
        addItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimension(R.dimen.ux_text_size_16sp)));
        addItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = mAdapter.getIndex();
                if (index != -1) {
                    mAdapter.reset();
                    mAdapter. notifyItemChanged(index);
                }
                mAdapter.addCert();
            }
        });
        topBar.addView(addItem, BaseBar.TB_Position.Position_RB);

        topBarLayout.removeAllViews();
        topBarLayout.addView(topBar.getContentView());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListView(View parentView) {
        RecyclerView recyclerView = parentView.findViewById(R.id.panel_trust_certificate_list);
        mAdapter = new TrustCertificateListAdapter(mContext, mPDFViewCtrl);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int index = mAdapter.getIndex();
                if (index != -1) {
                    mAdapter.reset();
                    mAdapter. notifyItemChanged(index);
                }
                return false;
            }
        });

        mAdapter.loadData();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        showToolbars();
    }

    private void showToolbars() {
        MainFrame mainFrame = (MainFrame) ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getMainFrame();
        mainFrame.setHideSystemUI(true);
        mainFrame.showToolbars();
    }

}
