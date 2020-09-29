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
package com.foxit.uiextensions.security.certificate;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.security.KeyUsageUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.UIMarqueeTextView;

import java.util.ArrayList;
import java.util.List;

public class CertificateDetailDialog extends UIMatchDialog {
    public static final int PERMDLG_TYPE_ENCRYPT = 0;
    public static final int PERMDLG_TYPE_DECRYPT = 1;
    private int mPermDialogType;
    private CertificateFileInfo mCertInfo;
    private Context mContext;
    private InfoAdapter mCertDetailAdapter;

    public CertificateDetailDialog(Context context, boolean showTopBarShadow) {
        super(context, showTopBarShadow);
        mContext = context;
    }

    public void init(int DlgType, final CertificateFileInfo info) {
        mPermDialogType = DlgType;
        mCertInfo = info;
        initPermissions(info.permCode);
        createView();
    }

    private View mView;

    private View createView() {
        mView = View.inflate(mContext, R.layout.rv_security_information, null);
        final RelativeLayout permly = (RelativeLayout) mView.findViewById(R.id.rv_security_information_prm_ly);
        final LinearLayout tably = (LinearLayout) mView.findViewById(R.id.rv_security_information_tab_ly);
        final TextView permLabel = (TextView) mView.findViewById(R.id.rv_security_information_prmtitle);
        final TextView certLabel = (TextView) mView.findViewById(R.id.rv_security_information_detailtitle);
        final ListView permListView = (ListView) mView.findViewById(R.id.rv_security_information_listp);
        final ListView certListView = (ListView) mView.findViewById(R.id.rv_security_information_listc);

        final View permLine = mView.findViewById(R.id.rv_security_information_prmline);
        final View detailLine = mView.findViewById(R.id.rv_security_information_detailline);

        mCertDetailAdapter = new InfoAdapter(mContext, getCertInfos());
        certListView.setAdapter(mCertDetailAdapter);
        if (mPermDialogType == PERMDLG_TYPE_DECRYPT) {
            permly.setVisibility(View.GONE);
            tably.setVisibility(View.GONE);
            permListView.setVisibility(View.GONE);
            certListView.setVisibility(View.VISIBLE);

        } else {
            permly.setVisibility(View.VISIBLE);
            tably.setVisibility(View.VISIBLE);
            certListView.setVisibility(View.GONE);
            detailLine.setVisibility(View.INVISIBLE);
        }

        if (AppDisplay.getInstance(mContext).isPad()) {
            permLabel.setTextColor(mView.getResources().getColor(R.color.ux_bg_color_toolbar_colour));
            certLabel.setTextColor(mView.getResources().getColor(R.color.ux_bg_color_toolbar_colour));
            permLine.setBackgroundColor(mView.getResources().getColor(R.color.ux_bg_color_toolbar_colour));
            detailLine.setBackgroundColor(mView.getResources().getColor(R.color.ux_bg_color_toolbar_colour));
            tably.setBackgroundColor(mView.getResources().getColor(R.color.ux_color_white));
        } else {
            permLabel.setTextColor(mView.getResources().getColor(R.color.ux_bg_color_toolbar_light));
            certLabel.setTextColor(mView.getResources().getColor(R.color.ux_bg_color_toolbar_light));
            permLine.setBackgroundColor(mView.getResources().getColor(R.color.ux_bg_color_toolbar_light));
            detailLine.setBackgroundColor(mView.getResources().getColor(R.color.ux_bg_color_toolbar_light));
            tably.setBackgroundColor(mView.getResources().getColor(R.color.ux_bg_color_toolbar_colour));
        }


        permLabel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (permListView.isShown()) return;
                certListView.setVisibility(View.GONE);
                permListView.setVisibility(View.VISIBLE);
                detailLine.setVisibility(View.INVISIBLE);
                permLine.setVisibility(View.VISIBLE);
            }
        });
        certLabel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (certListView.isShown()) return;
                permListView.setVisibility(View.GONE);
                certListView.setVisibility(View.VISIBLE);
                permLine.setVisibility(View.INVISIBLE);
                detailLine.setVisibility(View.VISIBLE);
            }
        });

        setContentView(mView);
        if (mPermDialogType == PERMDLG_TYPE_DECRYPT) {
            setTitle(mContext.getApplicationContext().getString(R.string.rv_security_information_certlist_title));
        } else {
            setTitle(mContext.getApplicationContext().getString(R.string.rv_certlist_note));
        }
        setBackButtonVisible(View.VISIBLE);
        setTitlePosition(BaseBar.TB_Position.Position_CENTER);
//        setButton(MatchDialog.DIALOG_OK);
        setListener(new DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == MatchDialog.DIALOG_OK) {
                    if (mPermDialogType == PERMDLG_TYPE_ENCRYPT) {
                        int code = getCustomPermission();
                        mCertInfo.permCode = code;
                    }
                    AppDialogManager.getInstance().dismiss(CertificateDetailDialog.this);
                }
            }

            @Override
            public void onBackClick() {

            }
        });

        return mView;
    }

    private List<InfoAdapter.CertInfo> getCertInfos() {
        List<InfoAdapter.CertInfo> infos = new ArrayList<>();

        InfoAdapter.CertInfo info = new InfoAdapter.CertInfo();
        info.name = AppResource.getString(mContext, R.string.rv_security_information_certlist_serialnumber);
        info.value = mCertInfo.certificateInfo.serialNumber;
        infos.add(info);

        info = new InfoAdapter.CertInfo();
        info.name = AppResource.getString(mContext, R.string.fx_string_name);
        info.value = mCertInfo.certificateInfo.name;
        infos.add(info);

        info = new InfoAdapter.CertInfo();
        info.name = AppResource.getString(mContext, R.string.rv_security_information_certlist_publisher);
        info.value = mCertInfo.certificateInfo.issuer;
        infos.add(info);

        info = new InfoAdapter.CertInfo();
        info.name = AppResource.getString(mContext, R.string.rv_security_information_certlist_usage);
        info.value = KeyUsageUtil.getInstance(mContext).getUsage(mCertInfo.certificateInfo.usageCode);
        infos.add(info);

        info = new InfoAdapter.CertInfo();
        info.name = AppResource.getString(mContext, R.string.rv_security_information_certlist_startdate);
        info.value = mCertInfo.certificateInfo.startDate;
        infos.add(info);

        info = new InfoAdapter.CertInfo();
        info.name = AppResource.getString(mContext, R.string.rv_security_information_certlist_expiringdate);
        info.value = mCertInfo.certificateInfo.expiringDate;
        infos.add(info);

        info = new InfoAdapter.CertInfo();
        info.name = AppResource.getString(mContext, R.string.rv_security_information_certlist_email);
        info.value = mCertInfo.certificateInfo.emailAddress;
        infos.add(info);
        return infos;
    }

    static class InfoAdapter extends BaseAdapter {

        private Context mContext;
        private List<CertInfo> mCertInfos = new ArrayList<>();

        InfoAdapter(Context context, List<CertInfo> certInfos) {
            mCertInfos = certInfos;
            mContext = context;
        }

        class TextViewHolder {
            TextView name;
            UIMarqueeTextView value;
        }

        static class CertInfo {
            String name;
            String value;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextViewHolder holder = null;
            if (convertView == null) {
                holder = new TextViewHolder();
                convertView = View.inflate(mContext, R.layout.rv_security_information_certlist_item, null);
                holder.name = convertView.findViewById(R.id.rv_security_information_certlist_name);
                holder.value = convertView.findViewById(R.id.rv_security_information_certlist_value);
                convertView.setTag(holder);

                if (AppDisplay.getInstance(mContext).isPad()) {
                    ListView.LayoutParams LP = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.MATCH_PARENT);
                    int height = mContext.getResources().getDimensionPixelSize(R.dimen.ux_list_item_height_1l_pad);
                    LP.height = height;
                    convertView.setLayoutParams(LP);
                }
            } else {
                holder = (TextViewHolder) convertView.getTag();
            }

            CertInfo certInfo = mCertInfos.get(position);
            holder.name.setText(certInfo.name);
            holder.value.setText(certInfo.value);
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return mCertInfos.get(position);
        }

        @Override
        public int getCount() {
            return mCertInfos.size();
        }
    }

    private void initPermissions(int permission) {

    }

    private int getCustomPermission() {
        int code = 0;
        return code;
    }

}
