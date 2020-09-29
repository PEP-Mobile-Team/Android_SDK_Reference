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
package com.foxit.uiextensions.annots.form;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.annots.common.UIBtnImageView;
import com.foxit.uiextensions.controls.dialog.BaseDialogFragment;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.IResult;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChoiceOptionsFragment extends BaseDialogFragment {
    private static final String KEY_SELECT_MODE = "select mode";
    private static final String KEY_CHOICE_ITEM_INFOS = "item infos";

    private PDFViewCtrl mPDFViewCtrl;
    private IResult<ArrayList<ChoiceItemInfo>, Object, Object> mPickCallback;
    private ArrayList<ChoiceItemInfo> mItemInfos = new ArrayList<>();
    private ChoiceOptionsAdapter mOptionsAdapter;
    private RecyclerView mOptionsListView;
    private ChoiceOptionsAdapter.SelectMode mSelectMode = ChoiceOptionsAdapter.SelectMode.SINGLE_SELECT;

    public static ChoiceOptionsFragment newInstance(ChoiceOptionsAdapter.SelectMode selectMode, ArrayList<ChoiceItemInfo> infos) {
        ChoiceOptionsFragment fragment = new ChoiceOptionsFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_SELECT_MODE, selectMode);
        bundle.putParcelableArrayList(KEY_CHOICE_ITEM_INFOS, infos);
        fragment.setArguments(bundle);
        return fragment;
    }

    public void init(PDFViewCtrl pdfViewCtrl, IResult<ArrayList<ChoiceItemInfo>, Object, Object> pickOptionsCallback) {
        mPDFViewCtrl = pdfViewCtrl;
        mPickCallback = pickOptionsCallback;
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
        initData();

        View view = inflater.inflate(R.layout.form_choice_options_layout, container, false);
        initTopBar(view);
        initListView(view);
        return view;
    }

    private void initData() {
        Bundle data = this.getArguments();
        if (data != null) {
            mSelectMode = (ChoiceOptionsAdapter.SelectMode) data.getSerializable(KEY_SELECT_MODE);

            mItemInfos.clear();
            ArrayList<ChoiceItemInfo> infos = data.getParcelableArrayList(KEY_CHOICE_ITEM_INFOS);
            mItemInfos = FormFillerUtil.cloneChoiceOptions(infos);
        }
    }

    private void initTopBar(View parentView) {
        LinearLayout topBarLayout = parentView.findViewById(R.id.form_choice_options_title);
        TopBarImpl topBar = new TopBarImpl(mContext);
        topBar.setBackgroundResource(R.color.ux_bg_color_toolbar_colour);
        IBaseItem backItem = new BaseItemImpl(mContext);
        backItem.setImageResource(R.drawable.panel_topbar_close_selector);
        backItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChoiceOptionsFragment.this.dismiss();
            }
        });
        topBar.addView(backItem, BaseBar.TB_Position.Position_LT);

        IBaseItem titleItem = new BaseItemImpl(mContext);
        titleItem.setText(AppResource.getString(mContext, R.string.fx_form_item_list));
        titleItem.setTextColorResource(R.color.ux_text_color_title_light);
        titleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimension(R.dimen.ux_text_height_title)));
        topBar.addView(titleItem, BaseBar.TB_Position.Position_CENTER);

        IBaseItem addItem = new BaseItemImpl(mContext);
        addItem.setText(AppResource.getString(mContext, R.string.fx_string_done));
        addItem.setTextColorResource(R.color.ux_text_color_title_light);
        addItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimension(R.dimen.ux_text_size_16sp)));
        addItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChoiceOptionsFragment.this.dismiss();
                if (mPickCallback != null)
                    mPickCallback.onResult(true, mItemInfos, null, null);
            }
        });
        topBar.addView(addItem, BaseBar.TB_Position.Position_RB);

        topBarLayout.removeAllViews();
        topBarLayout.addView(topBar.getContentView());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListView(View parentView) {
        mOptionsListView = parentView.findViewById(R.id.form_choice_options_list);
        mOptionsAdapter = new ChoiceOptionsAdapter(mContext, mPDFViewCtrl, mItemInfos);
        mOptionsAdapter.setSelectMode(mSelectMode);
        mOptionsAdapter.registerAdapterDataObserver(mAdapterDataObserver);
        mOptionsListView.setAdapter(mOptionsAdapter);
        mOptionsListView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        mOptionsListView.setItemAnimator(new DefaultItemAnimator());
        mOptionsListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int index = mOptionsAdapter.getMoreMenuIndex();
                if (index != -1) {
                    mOptionsAdapter.resetMoreMenuIndex();
                    mOptionsAdapter.notifyItemChanged(index);
                }
                return false;
            }
        });

        UIBtnImageView addOptionButton = parentView.findViewById(R.id.fb_add_form_choice);
        addOptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = mOptionsAdapter.getMoreMenuIndex();
                if (index != -1) {
                    mOptionsAdapter.resetMoreMenuIndex();
                    mOptionsAdapter.notifyItemChanged(index);
                    return;
                }

                mOptionsAdapter.addNewOption();
            }
        });
    }

    private RecyclerView.AdapterDataObserver mAdapterDataObserver = new RecyclerView.AdapterDataObserver() {

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mOptionsListView.smoothScrollToPosition(positionStart + itemCount);
        }
    };

    @NonNull
    @Override
    protected PDFViewCtrl getPDFViewCtrl() {
        return mPDFViewCtrl;
    }

    @Override
    protected void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOptionsAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
    }
}
