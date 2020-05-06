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
package com.foxit.uiextensions.modules.panel.signature;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.browser.adapter.SuperAdapter;
import com.foxit.uiextensions.browser.adapter.viewholder.PageFlagViewHolder;
import com.foxit.uiextensions.browser.adapter.viewholder.SuperViewHolder;
import com.foxit.uiextensions.modules.panel.ILoadPanelItemListener;
import com.foxit.uiextensions.modules.panel.bean.BaseBean;
import com.foxit.uiextensions.modules.panel.bean.SignatureBean;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.IResult;

import java.util.ArrayList;

import androidx.annotation.NonNull;


public class SignatureAdapter extends SuperAdapter {
    public final static int FLAG_SIGNATURE = 0;
    public final static int FLAG_PAGE_TAG = 1;
    private int mIndex = -1;
    private int mSignedIndex = 0;
    private SignaturePresenter mSigPresenter;
    private ArrayList<SignatureBean> mItems = new ArrayList<>();
    private ILoadPanelItemListener mLoadPanelItemListener = null;
    private PDFViewCtrl mPdfViewCtrl;
    public SignatureAdapter(Context context, PDFViewCtrl pdfViewCtrl, SignaturePresenter.ISignedVersionCallBack callBack) {
        super(context);
        mPdfViewCtrl = pdfViewCtrl;
        mSigPresenter = new SignaturePresenter(context, pdfViewCtrl, callBack);
    }

    public void load(final ILoadPanelItemListener listener) {
        mLoadPanelItemListener = listener;
        mSigPresenter.loadSignatures(new IResult<ArrayList<SignatureBean>, Object, Object>() {
            @Override
            public void onResult(boolean success, ArrayList<SignatureBean> p1, Object p2, Object p3) {
                mIndex = -1;
                mSignedIndex = 0;
                mItems.clear();
                boolean hasSignatures = success && p1.size() > 0;
                if (hasSignatures) {
                    mItems.addAll(p1);
                }
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onResult(hasSignatures);
                }
            }
        });
    }

    @Override
    public void notifyUpdateData() {
        notifyDataSetChanged();
    }

    @Override
    public BaseBean getDataItem(int position) {
        return mItems.get(position);
    }

    @NonNull
    @Override
    public SuperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SuperViewHolder viewHolder;
        switch (viewType) {
            case FLAG_SIGNATURE:
                viewHolder = new SignatureAdapter.ItemViewHolder(inflateLayout(getContext(), parent, R.layout.panel_item_signature));
                break;
            case FLAG_PAGE_TAG:
                viewHolder = new PageFlagViewHolder(inflateLayout(getContext(), parent, R.layout.panel_item_fileattachment_flag));
                break;
            default:
                viewHolder = new SignatureAdapter.ItemViewHolder(inflateLayout(getContext(), parent, R.layout.panel_item_signature));
                break;
        }

        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getFlag();
    }

    private View inflateLayout(Context context, ViewGroup parent, int layoutId) {
        return LayoutInflater.from(context).inflate(layoutId, parent, false);
    }

    public void reset(){
        mIndex =-1;
    }

    public int getIndex() {
        return mIndex;
    }

    private void view(int position) {
        mSigPresenter.view(position, mItems.get(position).getPageIndex(), mItems.get(position).getUuid());
    }

    private void verify(int position) {
        mSigPresenter.verify(position, mItems.get(position).getPageIndex(), mItems.get(position).getUuid());
    }

    private void sign(int position) {
        mSigPresenter.gotoPage(position, mItems.get(position).getPageIndex(), mItems.get(position).getUuid(), true);
    }

    private void gotoPage(int position) {
        mSigPresenter.gotoPage(position, mItems.get(position).getPageIndex(), mItems.get(position).getUuid(), false);
    }

    public void delete(String uuid) {
        for (SignatureBean bean : mItems) {
            if (bean.getUuid().equals(uuid)) {
                mItems.remove(bean);
                break;
            }
        }

        mIndex = -1;
        boolean hasSignatures = mItems.size() > 0;
        notifyDataSetChanged();
        if (mLoadPanelItemListener != null) {
            mLoadPanelItemListener.onResult(hasSignatures);
        }
    }

    public void clearItems() {
        mItems.clear();
        notifyDataSetChanged();
    }

    class ItemViewHolder extends SuperViewHolder {
        private ImageView mIcon;
        private ImageView mMore;
        private TextView mTitle;
        private TextView mDate;
        private View mMoreLayoutView;
        private View mSigContainer;

        private TextView mMoreView;
        private TextView mMoreVerify;
        private TextView mMoreSign;

        public ItemViewHolder(View viewHolder) {
            super(viewHolder);
            mSigContainer = viewHolder.findViewById(R.id.panel_signature_container);
            mIcon = viewHolder.findViewById(R.id.panel_item_signature_icon);
            mMore = (ImageView) viewHolder.findViewById(R.id.panel_item_signature_more);
            mTitle = (TextView) viewHolder.findViewById(R.id.panel_item_signature_title);
            mDate = (TextView) viewHolder.findViewById(R.id.panel_item_signature_date);
            mMore.setOnClickListener(this);
            mMoreLayoutView = viewHolder.findViewById(R.id.signature_more_view);

            mMoreView = (TextView) viewHolder.findViewById(R.id.panel_more_tv_signature_view);
            mMoreVerify = (TextView) viewHolder.findViewById(R.id.panel_more_tv_signature_verify);
            mMoreSign = (TextView) viewHolder.findViewById(R.id.panel_more_tv_signature_sign);

            mMoreView.setOnClickListener(this);
            mMoreVerify.setOnClickListener(this);
            mSigContainer.setOnClickListener(this);
            mMoreSign.setOnClickListener(this);
        }

        @Override
        public void bind(BaseBean data) {
            SignatureBean item = (SignatureBean) data;
            String title = AppResource.getString(context.getApplicationContext(), R.string.rv_panel_signature_unsigned);
            String dateOrName = item.getSigner();
            if (item.isSigned()) {
//                title = context.getApplicationContext().getString(R.string.rv_panel_signature_revocation, String.valueOf(item.getSignedIndex()))
//                        + context.getApplicationContext().getString(R.string.rv_panel_signature_signed_by, item.getSigner());
                title = context.getApplicationContext().getString(R.string.rv_panel_signature_signed_by, item.getSigner());
                dateOrName = item.getDate();

                mIcon.setImageDrawable(AppResource.getDrawable(context.getApplicationContext(), R.drawable.rv_panel_signature_type, null));
                mMoreSign.setVisibility(View.GONE);
                mMore.setVisibility(View.VISIBLE);
                mMoreView.setVisibility(View.VISIBLE);
                mMoreVerify.setVisibility(View.VISIBLE);
            } else {
                mIcon.setImageDrawable(AppResource.getDrawable(context.getApplicationContext(), R.drawable.rv_panel_signature_unsigned_type, null));
                boolean isVisible = (mPdfViewCtrl.isDynamicXFA() && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyXFAForm())
                        || (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canSigning()
                        && !item.isReadOnly());
                mMoreSign.setVisibility(isVisible ? View.VISIBLE : View.GONE);
                mMore.setVisibility(isVisible ? View.VISIBLE : View.GONE);
                mMoreView.setVisibility(View.GONE);
                mMoreVerify.setVisibility(View.GONE);
            }
            mTitle.setText(title);
            mDate.setText(dateOrName);

            if(getAdapterPosition() != mIndex) {
                mMoreLayoutView.setVisibility(View.GONE);
            }else {
                mMoreLayoutView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.panel_item_signature_more) {
                int temp = mIndex;
                mIndex = getAdapterPosition();
                notifyItemChanged(temp);
                notifyItemChanged(mIndex);
            } else if (v.getId() == R.id.panel_more_tv_signature_view){
                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                view(getAdapterPosition());
            } else if (v.getId() == R.id.panel_more_tv_signature_verify){
                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                verify(getAdapterPosition());
            } else if (v.getId() == R.id.panel_more_tv_signature_sign) {
                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                sign(getAdapterPosition());
            } else if (v.getId() == R.id.panel_signature_container){
                if (mIndex != -1){
                    int temp = mIndex;
                    reset();
                    notifyItemChanged(temp);
                    return;
                }

                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                gotoPage(getAdapterPosition());
            }
        }
    }
}
