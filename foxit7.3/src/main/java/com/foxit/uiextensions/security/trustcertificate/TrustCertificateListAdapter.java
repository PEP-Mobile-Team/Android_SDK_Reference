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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.browser.adapter.SuperAdapter;
import com.foxit.uiextensions.browser.adapter.viewholder.SuperViewHolder;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.modules.panel.bean.BaseBean;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.IResult;

import java.util.ArrayList;
import java.util.List;

public class TrustCertificateListAdapter extends SuperAdapter {

    private PDFViewCtrl mPDFViewCtrl;
    private Context mContext;
    private TrustCertificatePresenter mPresenter;
    private ArrayList<CertificateFileInfo> mTrustCertInfos = new ArrayList<>();

    private int mIndex = -1;

    public TrustCertificateListAdapter(Context context, PDFViewCtrl pdfViewCtrl) {
        super(context);
        this.mContext = context;
        this.mPDFViewCtrl = pdfViewCtrl;
        mPresenter = new TrustCertificatePresenter(context, pdfViewCtrl);
    }

    public void reset() {
        mIndex = -1;
    }

    public int getIndex() {
        return mIndex;
    }

    @Override
    public void notifyUpdateData() {
        notifyDataSetChanged();
    }

    @Override
    public BaseBean getDataItem(int position) {
        return mTrustCertInfos.get(position);
    }

    @NonNull
    @Override
    public SuperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SuperViewHolder viewHolder = new ItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.trust_certificate_list_item, parent, false));
        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return mTrustCertInfos.size();
    }

    class ItemViewHolder extends SuperViewHolder {
        private TextView mTvCertTitle;
        private TextView mTvCertDate;

        private ImageView mMore;
        private View mCertInfoView;
        private View mDeleteCertView;

        private View mMoreLayoutView;
        private View mCertContainer;

        public ItemViewHolder(View viewHolder) {
            super(viewHolder);
            mTvCertTitle = viewHolder.findViewById(R.id.panel_item_trust_certificate_title);
            mTvCertDate = viewHolder.findViewById(R.id.panel_item_trust_certificate_date);
            mMore = viewHolder.findViewById(R.id.panel_item_trust_cert_more);
            mMoreLayoutView = viewHolder.findViewById(R.id.trust_certificate_more_view);
            mCertInfoView = viewHolder.findViewById(R.id.rd_trust_certificate_detail);
            mDeleteCertView = viewHolder.findViewById(R.id.rd_delete_trust_certificate);
            mCertContainer = viewHolder.findViewById(R.id.rela_trust_cert);

            mMore.setOnClickListener(this);
            mDeleteCertView.setOnClickListener(this);
            mCertInfoView.setOnClickListener(this);
            mMoreLayoutView.setOnClickListener(this);
            mCertContainer.setOnClickListener(this);
        }

        @Override
        public void bind(BaseBean data, int position) {
            CertificateFileInfo certInfo = (CertificateFileInfo) data;

            mTvCertTitle.setText(certInfo.subject);
            mTvCertDate.setText(certInfo.validTo + "");
            int visibility = getAdapterPosition() == mIndex ? View.VISIBLE : View.GONE;
            mMoreLayoutView.setVisibility(visibility);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.rd_trust_certificate_detail) {
                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                viewCertInfo(getAdapterPosition());
            } else if (id == R.id.rd_delete_trust_certificate) {
                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                deleteTrustCert(getAdapterPosition());
            } else if (id == R.id.panel_item_trust_cert_more) {
                int temp = mIndex;
                mIndex = getAdapterPosition();
                notifyItemChanged(temp);
                notifyItemChanged(mIndex);
            } else if (id == R.id.rela_trust_cert) {
                if (mIndex != -1) {
                    int temp = mIndex;
                    reset();
                    notifyItemChanged(temp);
                    return;
                }
            }
        }
    }

    public void loadData() {
        mPresenter.loadTrustCert(new IResult<List<CertificateFileInfo>, Object, Object>() {
            @Override
            public void onResult(boolean success, List<CertificateFileInfo> p1, Object p2, Object p3) {
                mIndex = -1;
                mTrustCertInfos.clear();
                boolean hasTrustCert = success && p1.size() > 0;
                if (hasTrustCert) {
                    mTrustCertInfos.addAll(p1);
                }
                notifyDataSetChanged();
            }
        });
    }

    private void viewCertInfo(int position) {
        mPresenter.viewCertInfo(mTrustCertInfos.get(position));
    }

    private void deleteTrustCert(final int position) {
        final UITextEditDialog dialog = new UITextEditDialog(((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getAttachedActivity());
        dialog.getInputEditText().setVisibility(View.GONE);
        dialog.setTitle(AppResource.getString(mContext, R.string.fx_string_delete));
        dialog.getPromptTextView().setText(AppResource.getString(mContext, R.string.rv_remove_trust_certificate_prompt));
        dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.deleteTrustCert(mTrustCertInfos.get(position), new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            mIndex = -1;
                            mTrustCertInfos.remove(position);
                            notifyItemRemoved(position);
                        }
                    }
                });
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void addCert() {
        mPresenter.addTrustCert(new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success) {
                    loadData();
                } else {
                    if (event != null) {
                        int errorCode = event.mType;
                        if (errorCode == TrustCertificatePresenter.e_ErrTrustCertExisted) {
                           //
                        }
                    }
                }
            }
        });
    }

}
