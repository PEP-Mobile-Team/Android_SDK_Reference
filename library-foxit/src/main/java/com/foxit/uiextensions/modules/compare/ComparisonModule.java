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
package com.foxit.uiextensions.modules.compare;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import androidx.core.widget.TextViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.objects.PDFDictionary;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.modules.ScreenLockModule;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.thread.AppThreadManager;


public class ComparisonModule implements Module {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private ViewGroup mParent = null;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    private BaseBar mTopBar;
    private IBaseItem mBackItem;
    private IBaseItem mTitleItem;
    private IBaseItem mShowLegendItem;

    private boolean mFirstShowLegend = true;
    private LegendPopupWindow mLegendWindow;
    private CompareHandler mCompareHandler;

    public ComparisonModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return MODULE_NAME_COMPARISON;
    }

    @Override
    public boolean loadModule() {
        mCompareHandler = new CompareHandler(mContext, mParent, mPdfViewCtrl);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;

            uiExtensionsManager.registerStateChangeListener(mStateListener);
            uiExtensionsManager.registerModule(this);
            uiExtensionsManager.registerConfigurationChangedListener(mConfigurationChangedListener);
            uiExtensionsManager.registerLifecycleListener(mLifecycleEventListener);
        }
        mPdfViewCtrl.registerDocEventListener(docEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterStateChangeListener(mStateListener);;
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLifecycleListener(mLifecycleEventListener);
        }
        mPdfViewCtrl.unregisterDocEventListener(docEventListener);
        return true;
    }

    private void removeBar() {
        mParent.removeView(mTopBar.getContentView());
    }

    private void initComparisonBar() {
        mTopBar = new TopBarImpl(mContext);

        mBackItem = new BaseItemImpl(mContext);
        mBackItem.setId(R.id.compare_back);

        mTitleItem = new BaseItemImpl(mContext);

        mShowLegendItem = new BaseItemImpl(mContext);
        mShowLegendItem.setId(R.id.compare_show_legend);

        initItemsImgRes();
        initItemsOnClickListener();

        mTopBar.addView(mBackItem, BaseBar.TB_Position.Position_LT);
        mTopBar.addView(mTitleItem, BaseBar.TB_Position.Position_CENTER);
        mTopBar.addView(mShowLegendItem, BaseBar.TB_Position.Position_RB);
        mTopBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_bg_color_toolbar_light));

        mLegendWindow = new LegendPopupWindow(mContext, mParent);

        RelativeLayout.LayoutParams pzTopLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pzTopLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mParent.addView(mTopBar.getContentView(), pzTopLp);
        mTopBar.getContentView().setVisibility(View.INVISIBLE);
    }

    private void initItemsImgRes() {
        mBackItem.setImageResource(R.drawable.rd_reflow_back_selector);
        mTitleItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.hm_comparison_title));
        mTitleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimensionPixelOffset(R.dimen.ux_text_height_title)));
        mTitleItem.setTextColorResource(R.color.ux_text_color_title_dark);

        mShowLegendItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.show_legend));
        mShowLegendItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimensionPixelOffset(R.dimen.ux_text_height_subhead)));
        mShowLegendItem.setTextColorResource(R.color.ux_text_color_title_dark);
    }

    private void initItemsOnClickListener() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.compare_back) {
                    if (mUiExtensionsManager instanceof UIExtensionsManager) {
                        onKeyBack();
                        ((UIExtensionsManager)mUiExtensionsManager).triggerDismissMenuEvent();
                        if (((UIExtensionsManager)mUiExtensionsManager).getBackEventListener() != null
                                && ((UIExtensionsManager)mUiExtensionsManager).getBackEventListener().onBack()) {
                            return;
                        }
                        ((UIExtensionsManager)mUiExtensionsManager).backToPrevActivity();
                    }
                } else if (id == R.id.compare_show_legend) {
                    mShowLegendItem.setId(R.id.compare_hide_legend);
                    mShowLegendItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.hide_legend));

                    if (mLegendWindow != null) {
                        int height = mTopBar.getContentView().getMeasuredHeight();
                        mLegendWindow.show(10, height + 10);
                    }
                } else if (id == R.id.compare_hide_legend) {
                    mShowLegendItem.setId(R.id.compare_show_legend);
                    mShowLegendItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.show_legend));

                    if (mLegendWindow != null) {
                        mLegendWindow.dismiss();
                    }
                }
            }
        };
        mBackItem.setOnClickListener(clickListener);
        mShowLegendItem.setOnClickListener(clickListener);
    }

    PDFViewCtrl.IDocEventListener docEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
            initComparisonBar();
        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != Constants.e_ErrSuccess || !isCompareDoc()) {
                return;
            }

            mCompareHandler.fillDocDiffMap();

        }

        @Override
        public void onDocWillClose(PDFDoc document) {
            mCompareHandler.reset();
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            removeBar();
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private IStateChangeListener mStateListener = new IStateChangeListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            onStatusChanged();
        }
    };

    private void onStatusChanged() {
        if (ReadStateConfig.STATE_COMPARE == ((UIExtensionsManager)mUiExtensionsManager).getState()) {
            ((UIExtensionsManager)mUiExtensionsManager).getMainFrame().hideSettingBar();

            if (mPdfViewCtrl.getDoc() == null) {
                return;
            }

            Activity activity = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getMainFrame().isToolbarsVisible()) {
                mTopBar.getContentView().setVisibility(View.VISIBLE);
            } else {
                mTopBar.getContentView().setVisibility(View.INVISIBLE);
            }

            if (!mLegendWindow.isShowing() && mFirstShowLegend) {
                AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        mShowLegendItem.setId(R.id.compare_hide_legend);
                        mShowLegendItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.hide_legend));

                        int height = mTopBar.getContentView().getMeasuredHeight();
                        mLegendWindow.show(10, height + 10);
                        mFirstShowLegend = false;
                    }
                });
            }
        }
    }

    private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            if (mLegendWindow != null && mLegendWindow.isShowing()) {
                mLegendWindow.update();
            }
        }
    };

    private ILifecycleEventListener mLifecycleEventListener = new LifecycleEventListener(){

        @Override
        public void onResume(Activity act) {
            onStatusChanged();
        }
    };

    public boolean onKeyBack() {
        if (ReadStateConfig.STATE_COMPARE == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getState()){
            if (mLegendWindow != null && mLegendWindow.isShowing()) {
                mShowLegendItem.setId(R.id.compare_show_legend);
                mShowLegendItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.show_legend));

                mLegendWindow.dismiss();
            }

            ScreenLockModule lockModule = (ScreenLockModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_SCREENLOCK);
            int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_USER;
            if (lockModule != null){
                screenOrientation = lockModule.getRequestedOrientation();
            }
            Activity activity = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            activity.setRequestedOrientation(screenOrientation);

            mFirstShowLegend = true;
            return true;
        }
        return false;
    }

    class LegendPopupWindow extends PopupWindow {
        private Context mContext;
        private ViewGroup mParent;
        private AppDisplay display;
        private View mLegendView;

        public LegendPopupWindow(Context context, ViewGroup parent) {
            this(context, null, parent);
        }

        public LegendPopupWindow(Context context, AttributeSet attrs, ViewGroup parent) {
            this(context, attrs, 0, parent);
        }

        public LegendPopupWindow(Context context, AttributeSet attrs, int defStyleAttr, ViewGroup parent) {
            super(context, attrs, defStyleAttr);
            this.mContext = context;
            this.mParent = parent;

            display = AppDisplay.getInstance(context);

            mLegendView = LayoutInflater.from(mContext).inflate(R.layout.compare_legend_layout, null, false);
            mLegendView.getBackground().setAlpha((int)(255 * 0.7f));
            float w = display.dp2px(180.0f);
            if (w > mParent.getWidth() * 0.5f) {
                w = mParent.getWidth() * 0.5f;
            }
            setWidth((int) w);
            setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

            TextView tvTitle = mLegendView.findViewById(R.id.legend_title);
            TextView tvReplaced = mLegendView.findViewById(R.id.legend_replaced);
            TextView tvInserted = mLegendView.findViewById(R.id.legend_inserted);
            TextView tvDeleted = mLegendView.findViewById(R.id.legend_deleted);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tvTitle, display.dp2px(8), display.dp2px(15), 2, TypedValue.COMPLEX_UNIT_PX);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tvReplaced, display.dp2px(8), display.dp2px(15), 2, TypedValue.COMPLEX_UNIT_PX);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tvInserted, display.dp2px(8), display.dp2px(15), 2, TypedValue.COMPLEX_UNIT_PX);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tvDeleted, display.dp2px(8), display.dp2px(15), 2, TypedValue.COMPLEX_UNIT_PX);
            setContentView(mLegendView);

            setTouchable(false);
            setOutsideTouchable(false);
            setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }

        public void show(int x, int y) {
            showAtLocation(mParent, Gravity.RIGHT | Gravity.TOP, x, y);
        }
    }

    private boolean isCompareDoc() {
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) return false;
        try {
            PDFDictionary root = mPdfViewCtrl.getDoc().getCatalog();
            if (root != null) {
                boolean bExistPieceInfo = root.hasKey("PieceInfo");
                if (!bExistPieceInfo) return false;
                PDFDictionary pieceInfo = root.getElement("PieceInfo").getDict();
                if (pieceInfo == null) return false;
                return pieceInfo.hasKey("ComparePDF");
            }
        } catch (PDFException e) {

        }
        return false;
    }

    public CompareHandler getCompareHandler() {
        return mCompareHandler;
    }

}
