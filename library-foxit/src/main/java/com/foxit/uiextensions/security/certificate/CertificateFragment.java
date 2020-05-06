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
package com.foxit.uiextensions.security.certificate;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CertificateFragment extends UIMatchDialog {
    public CertificateFragment(Context context) {

        super(context);
        mContext = context.getApplicationContext();
        mSearchRunnable = new CertificateSearchRunnable(mContext);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        cleanup();
        if (mCertCallback != null) {
            mCertCallback.result(false, null, null);
        }
        return false;
    }

    private boolean mDoEncrypt;
    private Context mContext;
    public static final int	CERLIST_TYPE_ENCRYPT = 1;
    public static final int	CERLIST_TYPE_DECRYPT = 2;
    public static final int	CERLIST_TYPE_SIGNATURE = 3;

    private static final int TEMPLATE = 0;
    private static final int BITMAP_1 = 1;
    private static final int BITMAP_2 = 2;

    private int sigPostion = -1;

    public interface ICertDialogCallback {
        public void result(boolean succeed, Object result, Bitmap forSign);
    }

    public void init(CertificateViewSupport support, ICertDialogCallback callback, int type) {
        mViewSupport = support;
        mCertCallback = callback;
        mSignature = false;

        if (type == CERLIST_TYPE_ENCRYPT) {
            mDoEncrypt = true;
        } else if (type == CERLIST_TYPE_DECRYPT) {
            mDoEncrypt = false;
        } else if (type == CERLIST_TYPE_SIGNATURE) {
            mDoEncrypt = false;
            mSignature = true;
        }
        createView();

        searchCertificateFile(!mDoEncrypt);
    }

    public static final int			MESSAGE_UPDATE = 0x11;
    public static final int			MESSAGE_FINISH = 0x12;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == MESSAGE_UPDATE) {
                File file = (File) msg.obj;
                if (file != null && file.exists() && file.canRead()) {
                    if (addInfo(file.getName(), file.getPath(), file.getName().endsWith(".cer"))) {
                        mAdapter.notifyDataSetChanged();
                    }
                }
            } else if (msg.what == MESSAGE_FINISH) {
                TextView note = (TextView) mView.findViewById(R.id.rv_security_certlist_listtitle_tv);
                if (mItems.size() <= 0) {
                    note.setText(AppResource.getString(mContext, R.string.rv_security_certlist_nocerificatefile));
                } else {
                    note.setText(AppResource.getString(mContext, R.string.rv_certlist_note));
                }
            }
        }
    };

    private void searchCertificateFile(boolean isOnlyFindPfx) {

        mItems.clear();
        mSelectedItems.clear();
        getDataBySQLite(isOnlyFindPfx);
        TextView note = (TextView) mView.findViewById(R.id.rv_security_certlist_listtitle_tv);
        note.setText(AppResource.getString(mContext, R.string.rv_certlist_note_searching));
        mSearchRunnable.init(mHandler, isOnlyFindPfx);

        new Thread(mSearchRunnable).start();
    }

    private boolean addInfo(String fileName, String filePath, boolean isCert) {
        CertificateFileInfo info = new CertificateFileInfo();
        info.permCode = CertificateDataSupport.FULLPERMCODE;
        info.isCertFile = isCert;
        info.fileName = fileName;
        info.filePath = filePath;

        if (!mItems.contains(info)) {
            if (isCert) {
                updateInfo(info);
            }
            return mItems.add(info);
        }
        return false;
    }

    private void getDataBySQLite(boolean isOnlyFindPfx) {
        if (isOnlyFindPfx) {
            mViewSupport.getDataSupport().getAllPfxs(mItems);
            int size = mItems.size();
            for (int i = size - 1; i >= 0; i--) {
                CertificateFileInfo info = mItems.get(i);
                File file = new File(info.filePath);
                if (AppFileUtil.isSDAvailable() && !file.exists()) {
                    mViewSupport.getDataSupport().removePfx(info.filePath);
                    mItems.remove(i);
                    continue;
                }
                info.certificateInfo = mViewSupport.getCertSupport().verifyPassword(info.filePath, info.password);
                if (info.certificateInfo == null) {
                    info.password = null;
                }
            }
            if (mItems.size() > 0) {
                mHandler.obtainMessage(MESSAGE_UPDATE).sendToTarget();
            }
            return;
        }

        mViewSupport.getDataSupport().getAllPfxs(mItems);
        mViewSupport.getDataSupport().getAllCerts(mItems);
        int size = mItems.size();
        for (int i = size - 1; i >= 0; i--) {
            CertificateFileInfo info = mItems.get(i);
            File file = new File(info.filePath);
            if (AppFileUtil.isSDAvailable() && !file.exists()) {
                if (info.isCertFile) {
                    mViewSupport.getDataSupport().removeCert(info.filePath);
                } else {
                    mViewSupport.getDataSupport().removePfx(info.filePath);
                }
                mItems.remove(i);
                continue;
            }
            if (!info.isCertFile) {
                info.certificateInfo = mViewSupport.getCertSupport().verifyPassword(info.filePath, info.password);
                if (info.certificateInfo == null) {
                    info.password = null;
                }
            }
        }
        if (mItems.size() > 0) {
            mHandler.obtainMessage(MESSAGE_UPDATE).sendToTarget();
        }
    }

    private void updateInfo(CertificateFileInfo info) {
        if (info.isCertFile) {
            mViewSupport.getDataSupport().insertCert(info.issuer, info.publisher, info.serialNumber, info.filePath, info.fileName);
        } else {
            mViewSupport.getDataSupport().insertPfx(info.issuer, info.publisher, info.serialNumber, info.filePath, info.fileName, info.password);
        }
    }

    private ICertDialogCallback mCertCallback;
    private CertificateViewSupport mViewSupport;
    private Bitmap 				mTemplateBmp;
    private Bitmap 				mInkSignBmp1;
    private Bitmap 				mInkSignBmp2;
    private boolean				mSignature;

    private SparseArray<CertificateFileInfo> mSelectedItems = new SparseArray<CertificateFileInfo>();
    private List<CertificateFileInfo> mItems = new ArrayList<CertificateFileInfo>();
    private CertificateSearchRunnable mSearchRunnable;

    private View mView;
    private View createView() {
        if (mSignature) {
            int width = AppDisplay.getInstance(mContext).getDialogWidth() * 4 / 7;
            int height = width * 10 / 16;
            recycleBmp();
            createBitmap(width, height);
        }
        mView = View.inflate(mContext, R.layout.rv_security_certlist, null);
        ListView listView = (ListView) mView.findViewById(R.id.rv_security_certlist_lv);
        listView.setAdapter(mAdapter);

        setContentView(mView);
        setTitle(mContext.getString(
                 R.string.rv_certlist_note
        ));
        setButton(MatchDialog.DIALOG_OK | MatchDialog.DIALOG_CANCEL);
        setBackButtonVisible(View.GONE);
        setButtonEnable(false, UIMatchDialog.DIALOG_OK);
        setListener(new DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == MatchDialog.DIALOG_OK) {
                    AppDialogManager.getInstance().dismiss(CertificateFragment.this);
                    if (mCertCallback == null) {
                        return;
                    }
                    if (mSignature) {
                        cleanup();
                        recycleBmp();
                        CertificateFileInfo info = mSelectedItems.valueAt(0);
                        switch (info.radioButtonID) {
                            case TEMPLATE:
                            default:
                                mCertCallback.result(true, info, null);
                                return;
                            case BITMAP_1:
                                mCertCallback.result(true, info, getBmpByInkSignName(getInkSignNames().get(0)));
                                return;
                            case BITMAP_2:
                                mCertCallback.result(true, info, getBmpByInkSignName(getInkSignNames().get(1)));
                                return;
                        }
                    } else {
                        cleanup();
                        SparseArray<CertificateFileInfo> array = mSelectedItems;
                        int size = array.size();
                        List<CertificateFileInfo> infos = new ArrayList<CertificateFileInfo>(size);
                        for (int i = 0; i < size; i++) {
                            CertificateFileInfo info = array.get(array.keyAt(i));
                            if (info.permCode == 0xf3c) {
                                //full permissions
                                // phantom & adobe will check 0x2
                                info.permCode |= 0x2;
                            }
                            infos.add(info);
                        }
                        array.clear();
                        mCertCallback.result(true, infos, null);
                    }
                } else if (btType == MatchDialog.DIALOG_CANCEL) {
                    cleanup();
                    if (mCertCallback != null) {
                        mCertCallback.result(false, null, null);
                    }
                }
            }

            @Override
            public void onBackClick() {

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ViewHolder holder = (ViewHolder) view.getTag();
                CertificateFileInfo info = mItems.get(position);
                holder.checkBox.setChecked(!info.selected);
                _onCheckboxClicked(info, position, holder);
            }
        });

        return mView;
    }


    private void createBitmap(int width, int height) {
        mTemplateBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        List<String> signNames = getInkSignNames();
        if (signNames == null) {
            mInkSignBmp2 = null;
            mInkSignBmp1 = null;
            return;
        }
        int size = signNames.size();
        if (size > 1) {
            mInkSignBmp2 = getBmpByInkSignName(signNames.get(1), width, height);
        }
        if (size > 0) {
            mInkSignBmp1 = getBmpByInkSignName(signNames.get(0), width, height);
        }
    }

    private void cleanup() {
        if (!mSearchRunnable.isStoped()) {
            mSearchRunnable.stopSearch();
        }
    }

    private void recycleBmp() {
        if (mTemplateBmp != null) {
            if (!mTemplateBmp.isRecycled()) {
                mTemplateBmp.recycle();
            }
            mTemplateBmp = null;
        }
        if (mInkSignBmp1 != null) {
            if (!mInkSignBmp1.isRecycled()) {
                mInkSignBmp1.recycle();
            }
            mInkSignBmp1 = null;
        }
        if (mInkSignBmp2 != null) {
            if (!mInkSignBmp2.isRecycled()) {
                mInkSignBmp2.recycle();
            }
            mInkSignBmp2 = null;
        }
    }

    private List<String> getInkSignNames() {
        List<String> list = null;
        //TODO:SQ_Contants


        return list;
    }

    private Bitmap getBmpByInkSignName(String name, int width, int height) {
        //TODO: SQ_Contants

        return null;
    }

    private Bitmap getBmpByInkSignName(String name) {
        //TODO: SQ_Contants

        return null;
    }


    final class ViewHolder {
        public TextView		nameTextView;
        public CheckBox checkBox;
        public ImageView infoBtn;
        public LinearLayout templateLayout;
        public ImageView	templateImageView;
        public ImageView	inkSignBmp1ImageView;
        public ImageView	inkSignBmp2ImageView;
        public RadioButton templateRadioButton;
        public RadioButton 	inkSignBmp1RadioButton;
        public RadioButton	inkSignBmp2RadioButton;
    }

    private BaseAdapter mAdapter = new BaseAdapter() {


        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(mContext, R.layout.rv_security_certlist_item, null);
                holder.nameTextView = (TextView) convertView.findViewById(R.id.rv_security_certlist_item_tv);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.rv_security_certlist_item_cb);
                holder.infoBtn = (ImageView) convertView.findViewById(R.id.rv_security_certlist_item_info_iv);
                holder.templateLayout = (LinearLayout) convertView.findViewById(R.id.rv_security_certlist_item_sigshape);
                holder.templateImageView = (ImageView) convertView.findViewById(R.id.rv_security_certlist_item_sigshape_info_iv);
                holder.inkSignBmp1ImageView = (ImageView) convertView.findViewById(R.id.rv_security_certlist_item_sigshape_last1_iv);
                holder.inkSignBmp2ImageView = (ImageView) convertView.findViewById(R.id.rv_security_certlist_item_sigshape_last2_iv);
                holder.templateRadioButton = (RadioButton) convertView.findViewById(R.id.rv_security_certlist_item_sigshape_info_rb);
                holder.inkSignBmp1RadioButton = (RadioButton) convertView.findViewById(R.id.rv_security_certlist_item_sigshape_last1_rb);
                holder.inkSignBmp2RadioButton = (RadioButton) convertView.findViewById(R.id.rv_security_certlist_item_sigshape_last2_rb);
                convertView.setTag(holder);
                if (AppDisplay.getInstance(mContext).isPad()) {
                    LinearLayout.LayoutParams LP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    int height = mContext.getResources().getDimensionPixelSize(
                            R.dimen.ux_list_item_height_1l_pad);
                    LP.height = height;
                    RelativeLayout layout = (RelativeLayout) convertView.findViewById(R.id.rv_security_certlist_item_ly);
                    layout.setLayoutParams(LP);
                }
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final CertificateFileInfo info = mItems.get(position);
            holder.nameTextView.setText(info.fileName);
            if (mSignature) {
                if (position == sigPostion && info.selected) {
                    holder.checkBox.setChecked(true);
                } else {
                    holder.checkBox.setChecked(false);
                }
            } else {
                holder.checkBox.setChecked(info.selected);
            }

            final ViewHolder fHolder = holder;
            if (mSignature) {
                if (holder.checkBox.isChecked()) {

                    fHolder.templateImageView.setImageBitmap(mTemplateBmp);
                    if (mInkSignBmp1 != null) {
                        fHolder.inkSignBmp1ImageView.setImageBitmap(mInkSignBmp1);
                        fHolder.inkSignBmp1ImageView.setVisibility(View.VISIBLE);
                        fHolder.inkSignBmp1RadioButton.setVisibility(View.VISIBLE);
                    } else {
                        fHolder.inkSignBmp1ImageView.setVisibility(View.GONE);
                        fHolder.inkSignBmp1RadioButton.setVisibility(View.GONE);
                    }
                    if (mInkSignBmp2 != null) {
                        fHolder.inkSignBmp2ImageView.setImageBitmap(mInkSignBmp2);
                        fHolder.inkSignBmp2ImageView.setVisibility(View.VISIBLE);
                        fHolder.inkSignBmp2RadioButton.setVisibility(View.VISIBLE);
                    } else {
                        fHolder.inkSignBmp2ImageView.setVisibility(View.GONE);
                        fHolder.inkSignBmp2RadioButton.setVisibility(View.GONE);
                    }

                } else {
                    holder.templateLayout.setVisibility(View.GONE);
                }
            } else {
                holder.templateLayout.setVisibility(View.GONE);
            }

            holder.infoBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (info != null) {
                        if (!info.isCertFile) {
                            if (AppUtil.isEmpty(info.password) || info.certificateInfo == null) {
                                if (mViewSupport != null) {
                                    mViewSupport.showPasswordDialog(info, null);
                                }
                            } else {
                                if (mViewSupport != null) {
                                    mViewSupport.showPermissionDialog(info);
                                }
                            }
                        } else {
                            if (info.certificateInfo == null) {
                                if (mViewSupport != null) {
                                    info.certificateInfo = mViewSupport.getCertSupport().getCertificateInfo(info.filePath);
                                }
                            }
                            if (info.certificateInfo == null) {
                                UIToast.getInstance(mContext).show(AppResource.getString(mContext, R.string.rv_security_certfrompfx_failed));
                                return ;
                            }
                            if (mViewSupport != null) {
                                mViewSupport.showPermissionDialog(info);
                            }
                        }
                    }
                }
            });

            holder.checkBox.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    _onCheckboxClicked(info, position, fHolder);
                }
            });
            holder.templateRadioButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    setRadioButtonStatus(fHolder, TEMPLATE);
                    info.radioButtonID = TEMPLATE;
                }
            });
            holder.inkSignBmp1RadioButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    setRadioButtonStatus(fHolder, BITMAP_1);
                    info.radioButtonID = BITMAP_1;
                }
            });
            holder.inkSignBmp2RadioButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    setRadioButtonStatus(fHolder, BITMAP_2);
                    info.radioButtonID = BITMAP_2;
                }
            });
            return convertView;
        }


        private void setRadioButtonStatus(ViewHolder holder, int state) {
            holder.templateRadioButton.setChecked(state == TEMPLATE);
            holder.inkSignBmp1RadioButton.setChecked(state == BITMAP_1);
            holder.inkSignBmp2RadioButton.setChecked(state == BITMAP_2);
        }
    };

    private void _onCheckboxClicked(final CertificateFileInfo info, final  int position, ViewHolder fHolder) {
        sigPostion = position;
        CertificateFileInfo item = mSelectedItems.get(position);
        if (item == null) {
            //no cer selected
            info.selected = true;
            if (!info.isCertFile) {
                //pfx
                if (AppUtil.isEmpty(info.password)) {
                    //get info and passwd
                    if (mViewSupport != null) {
                        mViewSupport.showPasswordDialog(info, new ICertDialogCallback() {
                            @Override
                            public void result(boolean succeed, Object result, Bitmap forSign) {
                                if (mSignature) {
                                    info.selected =  false;
                                }
                                if (!succeed) {
                                    info.selected = false;
                                    mAdapter.notifyDataSetChanged();
                                    if (mSignature) {
                                        setButtonEnable(false, UIMatchDialog.DIALOG_OK);
                                    }
                                } else {
                                    if (info.certificateInfo.keyUsage != null && info.certificateInfo.keyUsage[3]) {
                                        //use to dataEncipherment
                                        if (mDoEncrypt && info.certificateInfo.expired) {
                                            // expired
                                            UIToast.getInstance(mContext).show(AppResource.getString(
                                                    mContext, R.string.rv_security_certlist_outdate), Toast.LENGTH_SHORT);
                                            info.selected = false;
                                            mAdapter.notifyDataSetChanged();
                                            setButtonEnable(false, UIMatchDialog.DIALOG_OK);
                                        } else {
                                            mSelectedItems.put(position, info);
                                            if (mSignature) {
                                                if (info.certificateInfo.expired) {
                                                    UIToast.getInstance(mContext).show(AppResource.getString(mContext,
                                                            R.string.rv_security_certlist_outdate), Toast.LENGTH_SHORT);
                                                    info.selected = false;
                                                    mAdapter.notifyDataSetChanged();
                                                    setButtonEnable(false, UIMatchDialog.DIALOG_OK);
                                                } else {
                                                    mSelectedItems.clear();
                                                    info.selected = true;
                                                    mSelectedItems.put(position, info);
                                                    setButtonEnable(true, UIMatchDialog.DIALOG_OK);
                                                }
                                            }
                                        }
                                    } else {
                                        if(!mSignature) {
                                            UIToast.getInstance(mContext).show(AppResource.getString(mContext,
                                                    R.string.rv_security_certlist_pubkey_invalidtype), Toast.LENGTH_SHORT);
                                            info.selected = false;
                                            mAdapter.notifyDataSetChanged();
                                        } else {
                                            if (info.certificateInfo.expired) {
                                                UIToast.getInstance(mContext).show(AppResource.getString(mContext,
                                                        R.string.rv_security_certlist_outdate), Toast.LENGTH_SHORT);
                                                info.selected = false;
                                                mAdapter.notifyDataSetChanged();
                                                setButtonEnable(false, UIMatchDialog.DIALOG_OK);
                                            } else {mSelectedItems.clear();
                                                info.selected = true;
                                                mSelectedItems.put(position, info);
                                                setButtonEnable(true, UIMatchDialog.DIALOG_OK);
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                    mAdapter.notifyDataSetChanged();
                    return ;
                } else {
                    //password is not Empty
                    if (info.certificateInfo.keyUsage == null || !info.certificateInfo.keyUsage[3]) {
                        //not use to dataEncipherment
                        if (!mSignature) {
                            UIToast.getInstance(mContext).show(AppResource.getString(mContext,
                                    R.string.rv_security_certlist_pubkey_invalidtype), Toast.LENGTH_SHORT);
                            info.selected = false;
                            mAdapter.notifyDataSetChanged();
                            return;
                        }
                    }
                    if (mDoEncrypt && info.certificateInfo.expired) {
                        // expired
                        UIToast.getInstance(mContext).show(AppResource.getString(mContext,
                                R.string.rv_security_certlist_outdate), Toast.LENGTH_SHORT);
                        info.selected = false;
                        mAdapter.notifyDataSetChanged();
                        setButtonEnable(false, UIMatchDialog.DIALOG_OK);
                        return;
                    }
                }
            }
            if (mSignature) {
                if (info.certificateInfo.expired) {
                    // expired
                    UIToast.getInstance(mContext).show(AppResource.getString(mContext,
                            R.string.rv_security_certlist_outdate), Toast.LENGTH_SHORT);
                    info.selected = false;
                    mAdapter.notifyDataSetChanged();
                    setButtonEnable(false, UIMatchDialog.DIALOG_OK);
                    return;
                } else {
                    info.selected = true;
                    if (mSelectedItems.size() > 0) {
                        CertificateFileInfo info_tmp = mSelectedItems.valueAt(0);
                        if (info_tmp != null) {
                            info_tmp.selected = false;
                            mSelectedItems.clear();
                        }
                    }
                }
            }
            //cer file
            if (info.certificateInfo == null) {
                if (mViewSupport != null) {
                    info.certificateInfo = mViewSupport.getCertSupport().getCertificateInfo(info.filePath);
                }
            }
            if (info.certificateInfo.keyUsage == null || !info.certificateInfo.keyUsage[3]) {
                //not use to dataEncipherment
                if (!mSignature) {
                    UIToast.getInstance(mContext).show(AppResource.getString(mContext,
                            R.string.rv_security_certlist_pubkey_invalidtype), Toast.LENGTH_SHORT);
                    info.selected = false;
                    mAdapter.notifyDataSetChanged();
                    return;
                }
            }
            if (mDoEncrypt && info.certificateInfo.expired) {
                // expired
                UIToast.getInstance(mContext).show(AppResource.getString(mContext,
                        R.string.rv_security_certlist_outdate), Toast.LENGTH_SHORT);
                info.selected = false;
                mAdapter.notifyDataSetChanged();
                setButtonEnable(false, UIMatchDialog.DIALOG_OK);
                return;
            }
            mSelectedItems.put(position, info);
        } else {
            //items not NULL
            fHolder.templateLayout.setVisibility(View.GONE);
            mSelectedItems.remove(position);
            info.selected = false;
        }
        if (mSelectedItems.size() == 0) {
            setButtonEnable(false, UIMatchDialog.DIALOG_OK);
        } else {
            setButtonEnable(true, UIMatchDialog.DIALOG_OK);
        }
        if (mSignature) {
            if (info.certificateInfo.expired) {
                UIToast.getInstance(mContext).show(AppResource.getString(mContext,
                        R.string.rv_security_certlist_outdate), Toast.LENGTH_SHORT);
                info.selected = false;
                mAdapter.notifyDataSetChanged();
            } else {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

}

