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
package com.foxit.uiextensions.controls.dialog.saveas;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.addon.optimization.ImageSettings;
import com.foxit.sdk.addon.optimization.MonoImageSettings;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Library;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.annots.common.UIBtnImageView;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.UIMarqueeTextView;

import java.util.ArrayList;
import java.util.List;

public class UIDocSaveAsDialog extends UIMatchDialog {

    public final static int FORMAT_UNKNOWN = -1;
    public final static int FORMAT_ORIGINAL = 0x01;
    public final static int FORMAT_FLATTEN = 0x02;
    public final static int FORMAT_OPTIMIZE = 0x04;

    private UIMarqueeTextView mTvSaveName;
    private FormatItemAdapter mAdapter;

    private List<SaveAsBean> mFormatItems = new ArrayList<>();
    private int mLastFormatPosition;
    private String mFileExt = "pdf";
    private int mFormats;
    private Activity mActivity;

    public UIDocSaveAsDialog(Context context) {
        super(context);
        mActivity = (Activity) context;
        createView(context);
    }

    public int getFormat() {
        if (mFormatItems == null || mFormatItems.size() == 0)
            return -1;
        return mFormatItems.get(mLastFormatPosition).format;
    }

    public void setFormatItems(int formats) {
        mFormats = formats;
        if (mFormats == 0)
            mFormats = FORMAT_ORIGINAL | FORMAT_FLATTEN | FORMAT_OPTIMIZE;

        resetItems(formats);
    }

    private void resetItems(int formats) {
        mFormatItems.clear();

        if ((formats & FORMAT_ORIGINAL) == FORMAT_ORIGINAL) {
            SaveAsBean originDocumentItem = new SaveAsBean(AppResource
                    .getString(mContext, R.string.rv_saveas_original), FORMAT_ORIGINAL, false);
            mFormatItems.add(originDocumentItem);
        }
        if ((formats & FORMAT_FLATTEN) == FORMAT_FLATTEN) {
            SaveAsBean flattenDocumentItem = new SaveAsBean(AppResource
                    .getString(mContext, R.string.rv_saveas_flatten), FORMAT_FLATTEN, false);
            mFormatItems.add(flattenDocumentItem);
        }

        boolean hasOptimezerLicense = AppAnnotUtil.hasModuleLicenseRight(Constants.e_ModuleNameOptimizer);
        if (hasOptimezerLicense && (formats & FORMAT_OPTIMIZE) == FORMAT_OPTIMIZE) {
            SaveAsBean optimizeDocumentItem = new SaveAsBean(AppResource
                    .getString(mContext, R.string.rv_saveas_optimize), FORMAT_OPTIMIZE, false);
            optimizeDocumentItem.haveSecondOptions = true;

            SaveAsBean.OptimizerImageSettings imageSettings = new SaveAsBean.OptimizerImageSettings();
            imageSettings.quality = ImageSettings.e_ImageCompressQualityMedium;
            optimizeDocumentItem.imageSettings = imageSettings;

            SaveAsBean.OptimizerMonoSettings monoSettings = new SaveAsBean.OptimizerMonoSettings();
            monoSettings.quality = MonoImageSettings.e_ImageCompressQualityLossy;
            optimizeDocumentItem.monoSettings = monoSettings;
            mFormatItems.add(optimizeDocumentItem);
        }
        mLastFormatPosition = 0;
        mFormatItems.get(0).checked = mFormatItems.size() > 1;
        mAdapter.notifyDataSetChanged();
    }

    public String getFileName() {
        return mTvSaveName.getText().toString();
    }

    public void setFileName(String name) {
        mTvSaveName.setText(name);
    }

    public String getFileExt() {
        return mFileExt;
    }

    public void setFileExt(String fileExt) {
        mFileExt = fileExt;
    }

    public SaveAsBean getSaveAsBean() {
        if (mFormatItems == null || mFormatItems.size() == 0)
            return null;
        return mFormatItems.get(mLastFormatPosition);
    }

    private void createView(Context context) {
        View view = View.inflate(mContext, R.layout.rv_save_as_layout, null);
        setContentView(view);
        setTitle(mContext.getApplicationContext().getString(R.string.fx_string_saveas));
        setButton(MatchDialog.DIALOG_OK | MatchDialog.DIALOG_CANCEL);
        setBackButtonVisible(View.GONE);

        mTvSaveName = view.findViewById(R.id.rv_save_as_name);
        mTvSaveName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //rename
                showRenameDialog(mTvSaveName.getText().toString());
            }
        });

        mAdapter = new FormatItemAdapter(context, mFormatItems);
        final ListView listView = view.findViewById(R.id.rv_save_format_list);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mLastFormatPosition == position) {
                    return;
                }
                SaveAsBean lastItem = (SaveAsBean) parent.getItemAtPosition(mLastFormatPosition);
                lastItem.checked = false;
                SaveAsBean currenItem = (SaveAsBean) parent.getItemAtPosition(position);
                currenItem.checked = true;

                mLastFormatPosition = position;
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showRenameDialog(final String name) {
        final UITextEditDialog dialog = new UITextEditDialog(mActivity);
        dialog.setPattern("[/\\:*?<>|\"\n\t]");
        dialog.setTitle(R.string.fx_string_rename);
        dialog.getPromptTextView().setVisibility(View.GONE);

        final EditText editText = dialog.getInputEditText();
        editText.setText(name);
        editText.setSelectAllOnFocus(true);
        dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTvSaveName.setText(editText.getText());
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private static class FormatItemAdapter extends BaseAdapter {

        private Context mContext;
        private UIMatchDialog mChoicesDialog;
        private List<SaveAsBean> mItems = new ArrayList<>();

        private QualityAdapter.ItemBean mLastImageSettings;
        private QualityAdapter.ItemBean mLastMonoSettings;

        private FormatItemAdapter(Context context, List<SaveAsBean> itemInfos) {
            this.mContext = context;
            this.mItems = itemInfos;
        }

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
        public View getView(int position, View convertView, ViewGroup parent) {
            final FormatItemAdapter.ViewHolder holder;
            if (convertView == null) {
                holder = new FormatItemAdapter.ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.rv_save_as_format_item, null, false);
                holder.tvTitle = convertView.findViewById(R.id.tv_format_title);
                holder.tvSecondTitle = convertView.findViewById(R.id.tv_format_second_title);
                holder.ivChecked = convertView.findViewById(R.id.iv_format_checked);
                holder.ivRightArrow = convertView.findViewById(R.id.iv_format_right_arrow);
                convertView.setTag(holder);
            } else {
                holder = (FormatItemAdapter.ViewHolder) convertView.getTag();
            }
            final SaveAsBean itemInfo = mItems.get(position);

            holder.tvTitle.setText(itemInfo.title);
            int checkedVisibility = mItems.size() == 1 ? View.GONE : (itemInfo.checked ? View.VISIBLE : View.INVISIBLE);
            holder.ivChecked.setVisibility(checkedVisibility);

            int rightArrowVisibility = itemInfo.haveSecondOptions ? View.VISIBLE : View.INVISIBLE;
            holder.ivRightArrow.setVisibility(rightArrowVisibility);
            holder.ivRightArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mChoicesDialog = new UIMatchDialog(mContext);
                    mChoicesDialog.setContentView(getChoicesView(itemInfo));
                    mChoicesDialog.setBackButtonVisible(View.VISIBLE);
                    mChoicesDialog.setTitle(AppResource.getString(mContext, R.string.fx_string_saveas_quality));
                    mChoicesDialog.setBackgroundColor(mContext.getResources().getColor(R.color.ux_color_toolbar_grey));
                    mChoicesDialog.setTitlePosition(BaseBar.TB_Position.Position_CENTER);
                    mChoicesDialog.setOnDLDismissListener(new MatchDialog.DismissListener() {
                        @Override
                        public void onDismiss() {
                            if (mLastImageSettings != null) {
                                if (itemInfo.imageSettings != null) {
                                    itemInfo.imageSettings.quality = mLastImageSettings.itemType;
                                }
                            }

                            if (mLastMonoSettings != null) {
                                if (itemInfo.monoSettings != null) {
                                    itemInfo.monoSettings.quality = mLastMonoSettings.itemType;
                                }
                            }
                            notifyDataSetChanged();
                        }
                    });
                    mChoicesDialog.showDialog();
                }
            });
            return convertView;
        }

        private static class ViewHolder {
            private TextView tvTitle;
            private TextView tvSecondTitle;
            private ImageView ivChecked;
            private UIBtnImageView ivRightArrow;
        }

        private View getChoicesView(SaveAsBean saveBean) {
            View view = View.inflate(mContext, R.layout.rd_quality_listview, null);
            //ImageSettins
            NestedListView lvImageSettings = view.findViewById(R.id.rd_image_settings_list);
            List<QualityAdapter.ItemBean> imageList = new ArrayList<>();
            imageList.add(new QualityAdapter.ItemBean(AppResource.getString(mContext, R.string.fx_string_minimum),
                    ImageSettings.e_ImageCompressQualityMinimum, false));
            imageList.add(new QualityAdapter.ItemBean(AppResource.getString(mContext, R.string.fx_string_low),
                    ImageSettings.e_ImageCompressQualityLow, false));
            imageList.add(new QualityAdapter.ItemBean(AppResource.getString(mContext, R.string.fx_string_medium),
                    ImageSettings.e_ImageCompressQualityMedium, false));
            imageList.add(new QualityAdapter.ItemBean(AppResource.getString(mContext, R.string.fx_string_high),
                    ImageSettings.e_ImageCompressQualityHigh, false));
            imageList.add(new QualityAdapter.ItemBean(AppResource.getString(mContext, R.string.fx_string_maximum),
                    ImageSettings.e_ImageCompressQualityMaximum, false));
            for (QualityAdapter.ItemBean bean : imageList) {
                if (bean.itemType == saveBean.imageSettings.quality) {
                    mLastImageSettings = bean;
                    bean.isChecked = true;
                } else {
                    bean.isChecked = false;
                }
            }
            final QualityAdapter imageAdapter = new QualityAdapter(mContext, imageList);
            lvImageSettings.setAdapter(imageAdapter);
            lvImageSettings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    QualityAdapter.ItemBean itemBean = (QualityAdapter.ItemBean) parent.getItemAtPosition(position);
                    if (itemBean.isChecked)
                        return;
                    mLastImageSettings.isChecked = false;
                    itemBean.isChecked = true;
                    mLastImageSettings = itemBean;
                    imageAdapter.notifyDataSetChanged();
                }
            });

            //MonoImageSettins
            NestedListView monoSettingsListView = view.findViewById(R.id.rd_mono_settings_list);
            List<QualityAdapter.ItemBean> monoList = new ArrayList<>();
            monoList.add(new QualityAdapter.ItemBean(AppResource.getString(mContext, R.string.rv_saveas_mono_settings_lossy),
                    MonoImageSettings.e_ImageCompressQualityLossy, false));
            monoList.add(new QualityAdapter.ItemBean(AppResource.getString(mContext, R.string.rv_saveas_mono_settings_lossless),
                    MonoImageSettings.e_ImageCompressQualityLossless, false));

            for (QualityAdapter.ItemBean bean : monoList) {
                if (bean.itemType == saveBean.monoSettings.quality) {
                    mLastMonoSettings = bean;
                    bean.isChecked = true;
                } else {
                    bean.isChecked = false;
                }
            }

            final QualityAdapter monoAdapter = new QualityAdapter(mContext, monoList);
            monoSettingsListView.setAdapter(monoAdapter);
            monoSettingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    QualityAdapter.ItemBean itemBean = (QualityAdapter.ItemBean) parent.getItemAtPosition(position);
                    if (itemBean.isChecked)
                        return;
                    mLastMonoSettings.isChecked = false;
                    itemBean.isChecked = true;
                    mLastMonoSettings = itemBean;
                    monoAdapter.notifyDataSetChanged();
                }
            });
            return view;
        }

        private static class QualityAdapter extends BaseAdapter {
            private Context mContext;
            private List<ItemBean> mItemBeans;

            public QualityAdapter(Context context, List<ItemBean> list) {
                mContext = context;
                mItemBeans = list;
            }

            public List<ItemBean> getList() {
                return mItemBeans;
            }

            @Override
            public int getCount() {
                return mItemBeans.size();
            }

            @Override
            public Object getItem(int position) {
                return mItemBeans.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (convertView == null) {
                    holder = new ViewHolder();
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.rd_list_check_layout, null, false);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.tvName = convertView.findViewById(R.id.tv_type_name);
                holder.ivChecked = convertView.findViewById(R.id.iv_type_checked);

                ItemBean itemBean = mItemBeans.get(position);
                holder.tvName.setText(itemBean.itemName);
                holder.ivChecked.setVisibility(itemBean.isChecked ? View.VISIBLE : View.INVISIBLE);
                return convertView;
            }

            private static class ViewHolder {
                private ImageView ivChecked;
                private TextView tvName;
            }

            public static class ItemBean {
                public String itemName;
                public int itemType;
                public boolean isChecked;

                public ItemBean(String itemName, int itemType, boolean isChecked) {
                    this.itemName = itemName;
                    this.itemType = itemType;
                    this.isChecked = isChecked;
                }
            }
        }

    }

}
