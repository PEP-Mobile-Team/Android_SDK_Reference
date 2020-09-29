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


import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.IResult;

import java.io.Serializable;
import java.util.ArrayList;

public class ChoiceOptionsAdapter extends SuperAdapter {
    private static final int ADD_CHOICE_OPTION = 1;
    private static final int RENAME_CHOICE_OPTION = 2;

    private PDFViewCtrl mPDFViewCtrl;
    private ArrayList<ChoiceItemInfo> mChoiceInfos = new ArrayList<>();

    private int mMenuShowIndex = -1;
    private int mLastSelectedIndex = -1;
    private ArrayList<Integer> mMultiSelectedIndexs = new ArrayList<>();
    private SelectMode mSelectMode = SelectMode.SINGLE_SELECT;

    public ChoiceOptionsAdapter(Context context, PDFViewCtrl pdfViewCtrl, ArrayList<ChoiceItemInfo> itemInfos) {
        super(context);
        mPDFViewCtrl = pdfViewCtrl;
        mChoiceInfos = itemInfos;
    }

    public ArrayList<ChoiceItemInfo> getChoiceInfos() {
        return mChoiceInfos;
    }

    public enum SelectMode implements Serializable {
        SINGLE_SELECT, MULTI_SELECT
    }

    public void setSelectMode(SelectMode selectMode) {
        this.mSelectMode = selectMode;

        if (mSelectMode == SelectMode.SINGLE_SELECT) {
            mLastSelectedIndex = -1;
            for (int i = 0; i < mChoiceInfos.size(); i++) {
                if (mChoiceInfos.get(i).selected) {
                    mLastSelectedIndex = i;
                    break;
                }
            }
        } else if (mSelectMode == SelectMode.MULTI_SELECT) {
            mMultiSelectedIndexs.clear();

            for (int i = 0; i < mChoiceInfos.size(); i++) {
                if (mChoiceInfos.get(i).selected) {
                    mMultiSelectedIndexs.add(i);
                }
            }
        }
    }

    public void resetMoreMenuIndex() {
        mMenuShowIndex = -1;
    }

    public int getMoreMenuIndex() {
        return mMenuShowIndex;
    }

    @Override
    public void notifyUpdateData() {
        notifyDataSetChanged();
    }

    @Override
    public BaseBean getDataItem(int position) {
        return mChoiceInfos.get(position);
    }

    @NonNull
    @Override
    public SuperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SuperViewHolder viewHolder = new ItemViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.form_choice_options_item, parent, false));
        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return mChoiceInfos.size();
    }

    private void deleteOption(int position) {
        if (SelectMode.SINGLE_SELECT == mSelectMode) {
            if (mLastSelectedIndex == position)
                mLastSelectedIndex = -1;
            else if (mLastSelectedIndex > position)
                mLastSelectedIndex -= 1;
        } else if (SelectMode.MULTI_SELECT == mSelectMode) {
            for (Integer index : mMultiSelectedIndexs) {
                if (index == position) {
                    mMultiSelectedIndexs.remove((Integer) position);
                    break;
                }
            }
        }
        mMenuShowIndex = -1;
        mChoiceInfos.remove(position);
        notifyItemRemoved(position);
    }

    private void renameOption(final int position) {
        showInputDialog(RENAME_CHOICE_OPTION, mChoiceInfos.get(position).optionValue, new IResult<String, Object, Object>() {
            @Override
            public void onResult(boolean success, String value, Object p2, Object p3) {
                if (success) {
                    ChoiceItemInfo itemInfo = mChoiceInfos.get(position);
                    if (itemInfo.optionLabel.equals(itemInfo.optionValue)) {
                        itemInfo.optionLabel = value;
                    }
                    itemInfo.optionValue = value;
                }
                mMenuShowIndex = -1;
                notifyItemChanged(position);
            }
        });
    }

    public void addNewOption() {
        String optionName = "New Item-" + AppDmUtil.randomUUID("").substring(0, 6);
        showInputDialog(ADD_CHOICE_OPTION, optionName, new IResult<String, Object, Object>() {
            @Override
            public void onResult(boolean success, String value, Object p2, Object p3) {
                if (success) {
                    int size = mChoiceInfos.size();
                    ChoiceItemInfo itemInfo = new ChoiceItemInfo();
                    itemInfo.selected = true;
                    itemInfo.defaultSelected = false;
                    itemInfo.optionLabel = value;
                    itemInfo.optionValue = value;

                    if (SelectMode.SINGLE_SELECT == mSelectMode) {
                        if (mLastSelectedIndex != -1) {
                            ChoiceItemInfo lastItemInfo = mChoiceInfos.get(mLastSelectedIndex);
                            lastItemInfo.selected = false;
                            notifyItemChanged(mLastSelectedIndex);
                        }
                        mChoiceInfos.add(size, itemInfo);
                        notifyItemInserted(size);

                        mLastSelectedIndex = size;
                    } else if (SelectMode.MULTI_SELECT == mSelectMode) {
                        mMultiSelectedIndexs.clear();
                        for (ChoiceItemInfo choice : mChoiceInfos) {
                            choice.selected = false;
                        }
                        mMultiSelectedIndexs.add(size);
                        mChoiceInfos.add(size, itemInfo);
                        notifyItemInserted(size);
                        notifyUpdateData();
                    }
                }
            }
        });
    }

    private void showInputDialog(int operateType, String defaultInput, final IResult<String, Object, Object> resultCallback) {
        final UITextEditDialog textDialog = new UITextEditDialog(((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getAttachedActivity());
        String title;
        if (ADD_CHOICE_OPTION == operateType)
            title = AppResource.getString(getContext(), R.string.fx_string_add);
        else
            title = AppResource.getString(getContext(), R.string.fx_string_rename);
        textDialog.setTitle(title);
        textDialog.getPromptTextView().setVisibility(View.GONE);
        textDialog.getInputEditText().setText(defaultInput);
        textDialog.getInputEditText().selectAll();
        textDialog.show();
        AppUtil.showSoftInput(textDialog.getInputEditText());

        textDialog.getInputEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean enabled = true;
                if (TextUtils.isEmpty(s.toString())) {
                    enabled = false;
                } else {
                    for (ChoiceItemInfo itemInfo : mChoiceInfos) {
                        if (itemInfo.optionValue.equals(s.toString())) {
                            enabled = false;
                            break;
                        }
                    }
                }
                textDialog.getOKButton().setEnabled(enabled);
            }
        });

        textDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AppUtil.dismissInputSoft(textDialog.getInputEditText());
                textDialog.dismiss();

                if (resultCallback != null) {
                    resultCallback.onResult(true, textDialog.getInputEditText().getText().toString(), null, null);
                }
            }
        });

        textDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textDialog.dismiss();
                AppUtil.dismissInputSoft(textDialog.getInputEditText());
                if (resultCallback != null) {
                    resultCallback.onResult(false, null, null, null);
                }
            }
        });
    }

    class ItemViewHolder extends SuperViewHolder {
        private TextView mTvChoiceValue;
        private ImageView mIvChoiceSelected;
        private ImageView mIvMore;
        private View mRenameView;
        private View mDeleteView;
        private View mMoreContainerView;
        private View mContainer;

        public ItemViewHolder(View viewHolder) {
            super(viewHolder);
            mTvChoiceValue = viewHolder.findViewById(R.id.fx_tv_choice_item_value);
            mIvChoiceSelected = viewHolder.findViewById(R.id.fx_iv_choice_item_select);
            mIvMore = viewHolder.findViewById(R.id.fx_choice_item_more);
            mMoreContainerView = viewHolder.findViewById(R.id.fx_choice_more_view);
            mRenameView = viewHolder.findViewById(R.id.fx_ll_rename_choice_item);
            mDeleteView = viewHolder.findViewById(R.id.fx_delete_choice_item);
            mContainer = viewHolder.findViewById(R.id.fx_choice_item_container);

            mIvMore.setOnClickListener(this);
            mDeleteView.setOnClickListener(this);
            mRenameView.setOnClickListener(this);
            mContainer.setOnClickListener(this);
        }

        @Override
        public void bind(BaseBean data, int position) {
            ChoiceItemInfo itemInfo = (ChoiceItemInfo) data;

            mContainer.setTag(position);
            mTvChoiceValue.setText(itemInfo.optionValue);
            mIvChoiceSelected.setSelected(itemInfo.selected);
            int visibility = getAdapterPosition() == mMenuShowIndex ? View.VISIBLE : View.GONE;
            mMoreContainerView.setVisibility(visibility);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.fx_choice_item_more) {
                int temp = mMenuShowIndex;
                mMenuShowIndex = getAdapterPosition();
                notifyItemChanged(temp);
                notifyItemChanged(mMenuShowIndex);
            } else if (id == R.id.fx_ll_rename_choice_item) {
                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                renameOption(getAdapterPosition());
            } else if (id == R.id.fx_delete_choice_item) {
                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                deleteOption(getAdapterPosition());
            } else if (id == R.id.fx_choice_item_container) {
                if (mMenuShowIndex != -1) {
                    int temp = mMenuShowIndex;
                    mMenuShowIndex = -1;
                    notifyItemChanged(temp);
                    return;
                }

                int itemPosition = getAdapterPosition();
                if (SelectMode.SINGLE_SELECT == mSelectMode) {
                    if (mLastSelectedIndex == itemPosition) {
                        ChoiceItemInfo itemInfo = mChoiceInfos.get(itemPosition);
                        itemInfo.selected = !itemInfo.selected;
                        notifyItemChanged(itemPosition);
                    } else {
                        if (mLastSelectedIndex != -1) {
                            ChoiceItemInfo lastItemInfo = mChoiceInfos.get(mLastSelectedIndex);
                            lastItemInfo.selected = false;
                            notifyItemChanged(mLastSelectedIndex);
                        }

                        ChoiceItemInfo curItemInfo = mChoiceInfos.get(itemPosition);
                        curItemInfo.selected = true;
                        notifyItemChanged(itemPosition);
                        mLastSelectedIndex = itemPosition;
                    }
                } else if (SelectMode.MULTI_SELECT == mSelectMode) {
                    ChoiceItemInfo itemInfo = mChoiceInfos.get(itemPosition);
                    if (mMultiSelectedIndexs.contains(itemPosition)) {
                        mMultiSelectedIndexs.remove((Integer) itemPosition);
                        itemInfo.selected = false;
                    } else {
                        mMultiSelectedIndexs.add(itemPosition);
                        itemInfo.selected = true;
                    }
                    notifyItemChanged(itemPosition);
                }
            }
        }
    }

}
