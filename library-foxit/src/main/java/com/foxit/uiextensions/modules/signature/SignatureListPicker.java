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
package com.foxit.uiextensions.modules.signature;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import androidx.fragment.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SignatureListPicker {
    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;
    private View mRootView;
    private BaseBar mSignPickerTopBar;
    private IBaseItem mSignPickerTitleItem;
    private IBaseItem mSignPickerCloseItem;
    private IBaseItem mSignPickerCreateItem;
    private ExpandableListView mSignListView;
    private SignatureFragment.SignatureInkCallback mInkCallback;
    private ISignListPickerDismissCallback mSignListPickerDismissCallback;
    private SingListAdapter mAdapter;
    private int mLeftSideInterval = 16;
    private int mRightSideInterval = 9;
    private boolean mIsPad;
    private AppDisplay mDisplay;
    private ArrayList<SignListPickerGroupItem> mSignListPickerGroupItems = new ArrayList<SignListPickerGroupItem>();
    private ArrayList<SignatureInkItem> mInkBaseItems = new ArrayList<SignatureInkItem>();
    private ArrayList<SignatureInkItem> mDsgInkItems = new ArrayList<SignatureInkItem>();
    private ArrayList<SignatureInkItem> mHandwritingInkItems = new ArrayList<SignatureInkItem>();
    private Map<String, WeakReference<Bitmap>> mCacheMap = new HashMap<String, WeakReference<Bitmap>>();
    private boolean mIsFromSignatureField = false;

    public SignatureListPicker(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, SignatureFragment.SignatureInkCallback inkCallback, boolean isFromSignatureField) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mDisplay = AppDisplay.getInstance(mContext);
        mIsPad = mDisplay.isPad();
        mInkCallback = inkCallback;
        if (mIsPad) {
            mRootView = View.inflate(mContext, R.layout.sign_list_layout_pad, null);
        } else {
            mRootView = View.inflate(mContext, R.layout.sign_list_layout_phone, null);
        }
        mIsFromSignatureField = isFromSignatureField;
        initDimens();
        initTopBar();
    }

    public void loadData(){
        initData();
        initList();
    }

    /**
     * must use
     */
    public void init(ISignListPickerDismissCallback signListPickerDismissCallback) {
        mSignListPickerDismissCallback = signListPickerDismissCallback;
    }

    private void initDimens() {
        if (mIsPad) {
            mLeftSideInterval = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
        } else {
            mLeftSideInterval = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
        }

        if (mIsPad) {
            mRightSideInterval = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_pad);
        } else {
            mRightSideInterval = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_phone);
        }
    }

    private void initTopBar() {
        mSignPickerTopBar = new SignatureSignListBar(mContext);
        if (mIsPad) {
            mSignPickerTopBar.setBackgroundResource(R.drawable.dlg_title_bg_circle_corner_blue);
        } else {
            mSignPickerTopBar.setBackgroundResource(R.color.ux_bg_color_toolbar_colour);
        }

        mSignPickerTitleItem = new BaseItemImpl(mContext);
        mSignPickerTitleItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_sign_model));
        mSignPickerTitleItem.setTextColorResource(R.color.ux_text_color_title_light);
        mSignPickerTitleItem.setTextSize(mDisplay.px2dp(mContext.getResources().getDimensionPixelOffset(R.dimen.ux_text_height_title)));

        mSignPickerCreateItem = new BaseItemImpl(mContext);
        mSignPickerCreateItem.setImageResource(R.drawable.sg_list_create_selector);
        mSignPickerCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignListPickerDismissCallback.onDismiss(false);
                if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
                Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                if (context == null) return;
                FragmentActivity act = ((FragmentActivity) context);
                SignatureFragment fragment;
                fragment = (SignatureFragment) act.getSupportFragmentManager().findFragmentByTag("InkSignFragment");
                if (fragment == null) {
                    fragment = new SignatureFragment();
                    fragment.init(mContext, mParent, mPdfViewCtrl, mIsFromSignatureField);
                }
                fragment.setInkCallback(mInkCallback);
//                if (fragment.isAdded()) {
//                    act.getSupportFragmentManager().beginTransaction().attach(fragment);
//                } else {
//                    act.getSupportFragmentManager().beginTransaction().add(R.id.rd_main_id, fragment, "InkSignFragment").addToBackStack(null).commitAllowingStateLoss();
//                }
                AppDialogManager.getInstance().showAllowManager(fragment, act.getSupportFragmentManager(), "InkSignFragment", null);
            }
        });

        mSignPickerCloseItem = new BaseItemImpl(mContext);
        mSignPickerCloseItem.setImageResource(R.drawable.cloud_back);
        mSignPickerCloseItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignListPickerDismissCallback.onDismiss(true);
            }
        });

        if (!mIsPad) {
            mSignPickerTopBar.addView(mSignPickerCloseItem, BaseBar.TB_Position.Position_LT);
        }
        mSignPickerTopBar.addView(mSignPickerTitleItem, BaseBar.TB_Position.Position_LT);
        mSignPickerTopBar.addView(mSignPickerCreateItem, BaseBar.TB_Position.Position_RB);

        ((RelativeLayout) mRootView.findViewById(R.id.sign_list_top_bar)).addView(mSignPickerTopBar.getContentView());
    }

    private void initData() {
        List<String> recent = SignatureDataUtil.getRecentKeys(mContext);
        String recentKey = recent == null ? null : recent.get(0);
        List<String> keys = SignatureDataUtil.getModelKeys(mContext);
        if (keys == null) return;
        for (String key : keys) {
            SignatureInkItem item = new SignatureInkItem();
            if (key.equals(recentKey)) {
                item.selected = true;
            }
            item.key = key;
            initBaseItemInfo(item);
            mInkBaseItems.add(item);
        }
        initDsgAndHandwritingItems();
        initGroupItems();
    }

    private void initGroupItems() {
        mSignListPickerGroupItems.clear();

        // Normal signature
        if (mIsFromSignatureField == false){
            SignListPickerGroupItem handwritingInkGroupItem = new SignListPickerGroupItem();
            handwritingInkGroupItem.name = AppResource.getString(mContext.getApplicationContext(), R.string.sg_signer_handwriting_group_title);
            handwritingInkGroupItem.inkItems = mHandwritingInkItems;
            mSignListPickerGroupItems.add(handwritingInkGroupItem);
        }
        // Certificate signature
        if(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE) != null) {
            SignListPickerGroupItem dsgInkGroupItem = new SignListPickerGroupItem();
            dsgInkGroupItem.name = AppResource.getString(mContext.getApplicationContext(), R.string.sg_signer_dsg_group_title);
            dsgInkGroupItem.inkItems = mDsgInkItems;
            mSignListPickerGroupItems.add(dsgInkGroupItem);
        }
    }

    private void initItem(SignatureInkItem item) {
        HashMap map = SignatureDataUtil.getBitmapByKey(mContext, item.key);
        item.bitmap = (Bitmap) map.get("bitmap");
        item.rect = (Rect) map.get("rect");
        item.color = (int) map.get("color");
        item.diameter = (float) map.get("diameter");
        Object dsgPathObj = map.get("dsgPath");
        if (dsgPathObj != null && !AppUtil.isEmpty((String) dsgPathObj)) {
            item.dsgPath = (String) dsgPathObj;
        } else {
            item.dsgPath = null;
        }
    }

    private void initBaseItemInfo(SignatureInkItem item) {
        HashMap map = SignatureDataUtil.getBitmapByKey(mContext, item.key);
        Object dsgPathObj = map.get("dsgPath");
        if (dsgPathObj != null && !AppUtil.isEmpty((String) dsgPathObj)) {
            item.dsgPath = (String) dsgPathObj;
        } else {
            item.dsgPath = null;
        }
    }

    private void initDsgAndHandwritingItems() {
        mDsgInkItems.clear();
        mHandwritingInkItems.clear();
        for (SignatureInkItem inkItem : mInkBaseItems) {
            if (!AppUtil.isEmpty(inkItem.dsgPath)) {
                mDsgInkItems.add(inkItem);
            } else {
                mHandwritingInkItems.add(inkItem);
            }
        }
    }

    private void initList() {
        mSignListView = (ExpandableListView) mRootView.findViewById(R.id.sign_list_listview);
        mSignListView.setGroupIndicator(null);
        mAdapter = new SingListAdapter();
        mSignListView.setAdapter(mAdapter);
        for (int i = 0; i < mAdapter.getGroupCount(); i++) {
            mSignListView.expandGroup(i);
        }
        addListListener();

    }

    private void addListListener() {
        mSignListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true;
            }
        });
        mSignListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                boolean hasOpened = false;
                for (SignatureInkItem inkItem : mInkBaseItems) {
                    if (inkItem.isOpened) {
                        hasOpened = true;
                        inkItem.isOpened = false;
                    }
                }
                if (hasOpened) {
                    mAdapter.notifyDataSetChanged();
                    return true;
                } else {
                    SignatureInkItem item = mSignListPickerGroupItems.get(groupPosition).inkItems.get(childPosition);
                    if (item == null) return false;
                    applySign(item);
                    return true;
                }
            }
        });
    }

    private void applySign(SignatureInkItem item) {
        initItem(item);
        mInkCallback.onSuccess(false, item.bitmap, item.rect, item.color, item.dsgPath);
        SignatureDataUtil.insertRecent(mContext, item.key);
        mSignListPickerDismissCallback.onDismiss(true);
    }

    static class GroupViewHolder {
        TextView name;
        View cuttingLine;
    }

    static class ChildViewHolder {
        ImageView selectedImg;
        ImageView bitmap;
        ImageView menu;
        LinearLayout menu_layout;
        LinearLayout edit;
        LinearLayout delete;
    }

    class SingListAdapter extends BaseExpandableListAdapter {
        @Override
        public int getGroupCount() {
            return mSignListPickerGroupItems.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if (mSignListPickerGroupItems.get(groupPosition).inkItems != null) {
                return mSignListPickerGroupItems.get(groupPosition).inkItems.size();
            } else {
                return 0;
            }
        }

        @Override
        public SignListPickerGroupItem getGroup(int groupPosition) {
            return mSignListPickerGroupItems.get(groupPosition);
        }

        @Override
        public SignatureInkItem getChild(int groupPosition, int childPosition) {
            if (mSignListPickerGroupItems.get(groupPosition).inkItems != null && mSignListPickerGroupItems.get(groupPosition).inkItems.size() != 0) {
                return mSignListPickerGroupItems.get(groupPosition).inkItems.get(childPosition);
            } else {
                return null;
            }
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            final GroupViewHolder gViewHolder;
            if (convertView == null) {
                gViewHolder = new GroupViewHolder();
                convertView = View.inflate(mContext, R.layout.sign_list_group_item, null);
                gViewHolder.name = (TextView) convertView.findViewById(R.id.sign_list_group_name);
                gViewHolder.cuttingLine = convertView.findViewById(R.id.sign_list_group_item_cutting_line);
                gViewHolder.name.setPadding(mLeftSideInterval, 0, mRightSideInterval, 0);
                convertView.setTag(gViewHolder);
            } else {
                gViewHolder = (GroupViewHolder) convertView.getTag();
            }
            gViewHolder.name.setText(getGroup(groupPosition).name);
            if (getGroup(groupPosition).inkItems.size() == 0) {
                gViewHolder.cuttingLine.setVisibility(View.VISIBLE);
            } else {
                gViewHolder.cuttingLine.setVisibility(View.INVISIBLE);
            }
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            final ChildViewHolder cViewHolder;
            if (convertView == null) {
                cViewHolder = new ChildViewHolder();
                convertView = View.inflate(mContext, R.layout.sign_list_listview_child_item, null);
                cViewHolder.selectedImg = (ImageView) convertView.findViewById(R.id.sign_list_item_selected);
                cViewHolder.bitmap = (ImageView) convertView.findViewById(R.id.sign_list_child_item_bitmap);
                cViewHolder.menu = (ImageView) convertView.findViewById(R.id.sign_list_child_menu_item);
                cViewHolder.menu_layout = (LinearLayout) convertView.findViewById(R.id.sign_list_child_menu_layout);
                cViewHolder.edit = (LinearLayout) convertView.findViewById(R.id.sign_list_child_edit_layout);
                cViewHolder.delete = (LinearLayout) convertView.findViewById(R.id.sign_list_child_item_delete_layout);
                cViewHolder.selectedImg.setPadding(mLeftSideInterval, 0, 0, 0);
                cViewHolder.menu.setPadding(0, 0, mRightSideInterval, 0);
                convertView.setTag(cViewHolder);
            } else {
                cViewHolder = (ChildViewHolder) convertView.getTag();
            }
            final SignatureInkItem inkItem = getChild(groupPosition, childPosition);
            if (inkItem == null) {
                return null;
            }
            if (inkItem.selected) {
                cViewHolder.selectedImg.setVisibility(View.VISIBLE);
            } else {
                cViewHolder.selectedImg.setVisibility(View.INVISIBLE);
            }
            if (inkItem.isOpened) {
                cViewHolder.menu_layout.setVisibility(View.VISIBLE);
            } else {
                cViewHolder.menu_layout.setVisibility(View.GONE);
            }
            WeakReference<Bitmap> reference = mCacheMap.get(inkItem.key);
            Bitmap bitmap = null;
            if (reference != null) {
                bitmap = reference.get();
            }
            if (bitmap == null) {
                bitmap = getBitmap(inkItem);
            }
            if (bitmap != null) {
                cViewHolder.bitmap.setImageBitmap(bitmap);
            }
            cViewHolder.menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AppUtil.isFastDoubleClick()) {
                        return;
                    }
                    updateMenuLayoutState(inkItem);
                }
            });
            cViewHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SignatureDataUtil.deleteByKey(mContext, SignatureConstants.getModelTableName(), inkItem.key);
                    mInkBaseItems.remove(inkItem);
                    mHandwritingInkItems.remove(inkItem);
                    mDsgInkItems.remove(inkItem);
                    if (mInkBaseItems.size() == 0) {

                    } else {
                        if (true) {
                            List<String> recent = SignatureDataUtil.getRecentKeys(mContext);
                            String recentKey = recent == null ? null : recent.get(0);
                            for (SignatureInkItem item : mInkBaseItems) {
                                if (item.key.equals(recentKey)) {
                                    item.selected = true;
                                } else {
                                    item.selected = false;
                                }
                            }
                        } else {
                            HashMap<String, Object> map = SignatureDataUtil.getRecentNormalSignData(mContext);
                            if (map != null && map.get("rect") != null && map.get("bitmap") != null) {
                                for (SignatureInkItem item : mInkBaseItems) {
                                    if (item.key.equals(map.get("key"))) {
                                        item.selected = true;
                                    } else {
                                        item.selected = false;
                                    }
                                }
                            }
                        }
                    }
                    notifyDataSetChanged();
                }
            });
            cViewHolder.edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AppUtil.isFastDoubleClick()) {
                        return;
                    }
                    initItem(inkItem);
                    mSignListPickerDismissCallback.onDismiss(false);
                    if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
                    Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                    if (context == null) return;
                    FragmentActivity act = ((FragmentActivity) context);
                    SignatureFragment fragment;
                    fragment = (SignatureFragment) act.getSupportFragmentManager().findFragmentByTag("InkSignFragment");
                    if (fragment == null) {
                        fragment = new SignatureFragment();
                        fragment.init(mContext, mParent, mPdfViewCtrl, mIsFromSignatureField);
                    }
                    fragment.setInkCallback(mInkCallback, inkItem);
//                    if (fragment.isAdded()) {
//                        act.getSupportFragmentManager().beginTransaction().attach(fragment);
//                    } else {
//                        act.getSupportFragmentManager().beginTransaction().add(R.id.rd_main_id, fragment, "InkSignFragment").addToBackStack(null).commitAllowingStateLoss();
//                    }
                    AppDialogManager.getInstance().showAllowManager(fragment, act.getSupportFragmentManager(), "InkSignFragment", null);
                }
            });
            convertView.setMinimumHeight(mDisplay.dp2px(100));
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        private void updateMenuLayoutState(SignatureInkItem item) {
            for (int i = 0; i < mInkBaseItems.size(); i++) {
                mInkBaseItems.get(i).isOpened = false;
            }
            item.isOpened = true;
            notifyDataSetChanged();
        }
    }

    public int getBaseItemsSize() {
        if (mInkBaseItems != null) {
            return mInkBaseItems.size();
        }
        return 0;
    }

    public int getHandwritingItemsSize() {
        if (mHandwritingInkItems != null) {
            return mHandwritingInkItems.size();
        }
        return 0;
    }

    public void dismiss() {//// TODO: 2017/3/8
        Iterator iterator = mCacheMap.values().iterator();
        while (iterator.hasNext()) {
            ((WeakReference) iterator.next()).clear();
        }
        mCacheMap.clear();
        mInkBaseItems.clear();
        mHandwritingInkItems.clear();
        mDsgInkItems.clear();
    }

    private Bitmap getBitmap(SignatureInkItem item) {
        Bitmap bmp = null;
        try {
            bmp = SignatureDataUtil.getScaleBmpByKey(mContext, item.key, mDisplay.dp2px(120), mDisplay.dp2px(100));
            WeakReference<Bitmap> reference = new WeakReference<Bitmap>(bmp);
            mCacheMap.put(item.key, reference);
        } catch (OutOfMemoryError error) {
            error.printStackTrace();
        }
        return bmp;
    }

    public View getRootView() {
        return mRootView;
    }

    class SignListPickerGroupItem {
        String name;
        ArrayList<SignatureInkItem> inkItems;
    }

    public interface ISignListPickerDismissCallback {
        void onDismiss(boolean isShowAnnotMenu);
    }
}
