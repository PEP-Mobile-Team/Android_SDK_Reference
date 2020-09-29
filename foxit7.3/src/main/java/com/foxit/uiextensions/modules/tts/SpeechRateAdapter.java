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
package com.foxit.uiextensions.modules.tts;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.browser.adapter.SuperAdapter;
import com.foxit.uiextensions.browser.adapter.viewholder.SuperViewHolder;
import com.foxit.uiextensions.modules.panel.bean.BaseBean;

import java.util.List;

import androidx.annotation.NonNull;

public class SpeechRateAdapter extends SuperAdapter {

    private Context mContext;
    private List<SpeechRateItemInfo> mRateItemInfos;
    private ISelectedSpeedRateCallback mSelectedCallback;

    private int mLastSelectedIndex;

    SpeechRateAdapter(Context context, List<SpeechRateItemInfo> itemInfos) {
        super(context);
        this.mContext = context;
        this.mRateItemInfos = itemInfos;
    }

    void setLastSelectedIndex(int index) {
        this.mLastSelectedIndex = index;
    }

    @Override
    public void notifyUpdateData() {
        notifyDataSetChanged();
    }

    @Override
    public BaseBean getDataItem(int position) {
        return mRateItemInfos.get(position);
    }

    @NonNull
    @Override
    public SuperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SuperViewHolder viewHolder = new ItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.rd_list_check_layout, parent, false));
        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return mRateItemInfos.size();
    }

    class ItemViewHolder extends SuperViewHolder {

        private TextView mTvRate;
        private ImageView mIvChecked;

        private View mContentView;

        ItemViewHolder(View itemView) {
            super(itemView);

            mContentView = itemView.findViewById(R.id.ll_check_container);
            mTvRate = itemView.findViewById(R.id.tv_type_name);
            mIvChecked = itemView.findViewById(R.id.iv_type_checked);

            mContentView.setOnClickListener(this);
        }

        @Override
        public void bind(BaseBean data, int position) {
            SpeechRateItemInfo itemInfo = (SpeechRateItemInfo) data;
            mTvRate.setText(itemInfo.speedRate);
            mTvRate.setSelected(itemInfo.isChecked);
            int visibility = itemInfo.isChecked ? View.VISIBLE : View.INVISIBLE;
            mIvChecked.setVisibility(visibility);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.ll_check_container) {
                int selectedPosition = getAdapterPosition();
                if (selectedPosition == mLastSelectedIndex)
                    return;

                if (mLastSelectedIndex != -1) {
                    SpeechRateItemInfo lastItemInfo = mRateItemInfos.get(mLastSelectedIndex);
                    lastItemInfo.isChecked = false;
                    notifyItemChanged(mLastSelectedIndex);
                }

                SpeechRateItemInfo curItemInfo = mRateItemInfos.get(selectedPosition);
                curItemInfo.isChecked = true;
                notifyItemChanged(selectedPosition);
                mLastSelectedIndex = selectedPosition;

                if (mSelectedCallback != null)
                    mSelectedCallback.onItemClick(selectedPosition, curItemInfo);
            }
        }
    }

    static class SpeechRateItemInfo extends BaseBean {
        String speedRate;
        boolean isChecked;

        SpeechRateItemInfo(String speedRate, boolean isChecked) {
            this.speedRate = speedRate;
            this.isChecked = isChecked;
        }
    }

    void setSelectedSpeedCallback(ISelectedSpeedRateCallback callback) {
        mSelectedCallback = callback;
    }

    interface ISelectedSpeedRateCallback {
        void onItemClick(int position, SpeechRateItemInfo itemInfo);
    }

}
