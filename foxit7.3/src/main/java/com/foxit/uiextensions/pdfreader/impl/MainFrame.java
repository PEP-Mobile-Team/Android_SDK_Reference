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
package com.foxit.uiextensions.pdfreader.impl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.UIAnnotToolbar;
import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.controls.menu.MoreMenuModule;
import com.foxit.uiextensions.controls.menu.MoreMenuView;
import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.MultiLineBarImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBarsHandler;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BottomBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.modules.panel.IPanelManager;
import com.foxit.uiextensions.modules.panel.PanelManager;
import com.foxit.uiextensions.modules.signature.SignatureModule;
import com.foxit.uiextensions.modules.signature.SignatureToolHandler;
import com.foxit.uiextensions.modules.thumbnail.ThumbnailModule;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IMainFrame;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;


public class MainFrame implements IMainFrame {
    private int SHOW_ANIMATION_TAG = 100;
    private int HIDE_ANIMATION_TAG = 101;

    private int mPageLayoutMode = PDFViewCtrl.PAGELAYOUTMODE_SINGLE;
    private int mZoomMode = PDFViewCtrl.ZOOMMODE_FITPAGE;
    private Activity mAttachedActivity;
    private Context mContext;
    private Config mConfig;
    private int mThirdMaskCounter;

    private UIExtensionsManager mUiExtensionsManager;
    private ViewGroup mRootView;

    private ViewGroup mDocViewerLayout;
    private ViewGroup mTopBarLayout;
    private ViewGroup mBottomBarLayout;
    private ViewGroup mFormBarLayout;
    private ViewGroup mEditDoneBarLayout;
    private ViewGroup mToolSetBarLayout;
    private ViewGroup mAnnotCustomTopBarLayout;
    private ViewGroup mAnnotCustomBottomBarLayout;
    private ViewGroup mMaskView;
    private ImageView mToolIconView;
    private TextView mToolNameTv;

    private TopBarImpl mTopBar;
    private BaseBarImpl mBottomBar;
    private BaseBarImpl mEditDoneBar;
    private BaseBarImpl mFormBar;
    private MultiLineBarImpl mSettingBar;
    private BaseBarImpl mToolSetBar;
    private BaseBarImpl mAnnotCustomTopBar;
    private BaseBarImpl mAnnotCustomBottomBar;

    private ViewGroup mAnnotBarLayout;
    private UIAnnotToolbar mAnnotBar;

    private AnimationSet mTopBarShowAnim;
    private AnimationSet mTopBarHideAnim;
    private AnimationSet mBottomBarShowAnim;
    private AnimationSet mBottomBarHideAnim;
    private AnimationSet mMaskShowAnim;
    private AnimationSet mMaskHideAnim;

    private PropertyBar mPropertyBar;

    private MoreMenuModule mMoreMenuModule;
    private PopupWindow mSettingPopupWindow;

    private ArrayList<View> mStateLayoutList;
    private boolean mIsShowTopToolbar = true;
    private boolean mIsShowBottomToolbar = true;
    private boolean mIsHideSystemUI = true;
    private boolean mIsFullScreen = true;

    private IPanelManager mPanelManager;

    public MainFrame(Context context, Config config) {
        mContext = context;
        mConfig = config;
        mRootView = (ViewGroup) View.inflate(mContext, R.layout.rd_main_frame, null);

        mDocViewerLayout = (ViewGroup) mRootView.findViewById(R.id.read_docview_ly);

        mTopBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_top_bar_ly);
        mBottomBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_bottom_bar_ly);
        mFormBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_form_bar_ly);
        mEditDoneBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_annot_done_bar_ly);
        mToolSetBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_tool_set_bar_ly);
        mAnnotCustomTopBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_annot_custom_top_bar_ly);
        mAnnotCustomBottomBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_annot_custom_bottom_bar_ly);

        mMaskView = (ViewGroup) mRootView.findViewById(R.id.read_mask_ly);
        mToolIconView = (ImageView) mRootView.findViewById(R.id.read_tool_icon);
        mToolNameTv = (TextView) mRootView.findViewById(R.id.read_tool_name_tv);

        mAnnotBarLayout = mRootView.findViewById(R.id.read_new_annot_bar_rl);

        mStateLayoutList = new ArrayList<View>();
        mStateLayoutList.add(mTopBarLayout);
        mStateLayoutList.add(mBottomBarLayout);
        mStateLayoutList.add(mEditDoneBarLayout);
        mStateLayoutList.add(mFormBarLayout);
        mStateLayoutList.add(mToolSetBarLayout);
        mStateLayoutList.add(mAnnotCustomTopBarLayout);
        mStateLayoutList.add(mAnnotCustomBottomBarLayout);
        mStateLayoutList.add(mAnnotBarLayout);
    }

    public void init(UIExtensionsManager uiExtensionsManager) {
        mUiExtensionsManager = uiExtensionsManager;
        mUiExtensionsManager.registerStateChangeListener(mStateChangeListener);
        mUiExtensionsManager.registerLayoutChangeListener(mLayoutChangeListener);
        mUiExtensionsManager.registerLifecycleListener(mLifecycleEventListener);

        mTopBar = new TopBarImpl(mContext);
        mBottomBar = new BottomBarImpl(mContext);
        mFormBar = new BottomBarImpl(mContext);
        mEditDoneBar = new TopBarImpl(mContext);
        mToolSetBar = new BottomBarImpl(mContext);
        mAnnotCustomTopBar = new TopBarImpl(mContext);
        mAnnotCustomBottomBar = new BottomBarImpl(mContext);

        mSettingBar = new MultiLineBarImpl(mContext, mUiExtensionsManager.getPDFViewCtrl());
        mSettingBar.init(mRootView, mConfig);
        mAnnotBar = new UIAnnotToolbar(mContext, mUiExtensionsManager.getPDFViewCtrl(), mConfig);

        mPropertyBar = new PropertyBarImpl(mContext, mUiExtensionsManager.getPDFViewCtrl());

        mBottomBar.setOrientation(BaseBar.HORIZONTAL, RelativeLayout.LayoutParams.MATCH_PARENT, AppResource.getDimensionPixelSize(mContext, R.dimen.ux_bottombar_height));
        mBottomBar.setItemInterval(mContext.getResources().getDimensionPixelSize(R.dimen.rd_bottombar_button_space));

        mFormBar.setOrientation(BaseBar.HORIZONTAL, RelativeLayout.LayoutParams.MATCH_PARENT, AppResource.getDimensionPixelSize(mContext, R.dimen.ux_bottombar_height));
        mFormBar.setInterceptTouch(false);

        mEditDoneBar.setOrientation(BaseBar.HORIZONTAL);
        mEditDoneBar.setInterceptTouch(false);

        mToolSetBar.setOrientation(BaseBar.HORIZONTAL, RelativeLayout.LayoutParams.MATCH_PARENT, AppResource.getDimensionPixelSize(mContext, R.dimen.ux_bottombar_height));
        mToolSetBar.setInterceptTouch(false);

        mAnnotCustomTopBar.setOrientation(BaseBar.HORIZONTAL);
        mAnnotCustomTopBar.setInterceptTouch(false);
        mAnnotCustomBottomBar.setOrientation(BaseBar.HORIZONTAL, RelativeLayout.LayoutParams.MATCH_PARENT, AppResource.getDimensionPixelSize(mContext, R.dimen.ux_bottombar_height));
        mAnnotCustomBottomBar.setInterceptTouch(false);

//        mTopBarLayout.addView(mTopBar.getContentView(), 0);
//        mBottomBarLayout.addView(mBottomBar.getContentView());
//        mFormBarLayout.addView(mFormBar.getContentView());
//        mEditDoneBarLayout.addView(mEditDoneBar.getContentView());
//        mToolSetBarLayout.addView(mToolSetBar.getContentView());
//        mAnnotCustomTopBarLayout.addView(mAnnotCustomTopBar.getContentView());
//        mAnnotCustomBottomBarLayout.addView(mAnnotCustomBottomBar.getContentView());
//        mAnnotBarLayout.addView(mAnnotBar);

        setSettingView(mSettingBar.getRootView());


        mPanelManager = new PanelManager(mContext, mUiExtensionsManager, mRootView, new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mIsHideSystemUI = true;
                showToolbars();
            }
        });

        mPanelManager.setOnShowPanelListener(new IPanelManager.OnShowPanelListener() {
            @Override
            public void onShow() {
                if (isToolbarsVisible()) {
                    mIsHideSystemUI = false;
                } else {
                    AppUtil.showSystemUI(mUiExtensionsManager.getAttachedActivity());
                }
            }
        });
        initBottomBarBtns();
        initOtherView();
        initAnimations();
        mStateChangeListener.onStateChanged(mUiExtensionsManager.getState(), mUiExtensionsManager.getState());
    }

    public void release() {
        mPanelManager = null;

        mTopBarShowAnim = null;
        mTopBarHideAnim = null;
        mBottomBarShowAnim = null;
        mBottomBarHideAnim = null;
        mMaskShowAnim = null;
        mMaskHideAnim = null;

        mMaskView = null;
        mToolIconView = null;

        mDocViewerLayout.removeAllViews();
        mDocViewerLayout = null;
        mRootView.removeAllViews();
        mRootView = null;

        mStateLayoutList.clear();
        mStateLayoutList = null;

        mUiExtensionsManager.unregisterStateChangeListener(mStateChangeListener);
        mStateChangeListener = null;
        mUiExtensionsManager.unregisterLayoutChangeListener(mLayoutChangeListener);
        mLayoutChangeListener = null;
        mUiExtensionsManager.unregisterLifecycleListener(mLifecycleEventListener);
        mLifecycleEventListener = null;
    }

    private IBaseItem mBackItem;
    private IBaseItem mPanelBtn = null;
    private IBaseItem mSettingBtn = null;
    private IBaseItem mEditBtn = null;
    private IBaseItem mReadSignItem = null;

    private boolean isLoadPanel() {
        return mConfig.modules.isLoadAnnotations() || mConfig.modules.isLoadReadingBookmark()
                || mConfig.modules.isLoadOutline() || mConfig.modules.isLoadAttachment()
                || mConfig.modules.isLoadSignature();
    }

    private void initBottomBarBtns() {
        mPanelBtn = new BaseItemImpl(mContext);
        mSettingBtn = new BaseItemImpl(mContext);

        int textSize = mContext.getResources().getDimensionPixelSize(R.dimen.ux_text_height_toolbar);
        int textColorResId = R.color.ux_text_color_body2_dark;
        int interval = mContext.getResources().getDimensionPixelSize(R.dimen.ux_toolbar_button_icon_text_vert_interval);

        mPanelBtn.setImageResource(R.drawable.rd_bar_panel_selector);
        mPanelBtn.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rd_bar_panel));
        mPanelBtn.setRelation(IBaseItem.RELATION_BELOW);
        mPanelBtn.setInterval(interval);
        mPanelBtn.setTextSize(AppDisplay.getInstance(mContext).px2dp(textSize));
        mPanelBtn.setTextColorResource(textColorResId);
        mPanelBtn.setTag(ToolbarItemConfig.ITEM_BOTTOMBAR_LIST);
        mPanelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPanelManager != null) {
                    mUiExtensionsManager.triggerDismissMenuEvent();
                    mPanelManager.showPanel();
                }
            }
        });
        if (isLoadPanel()) {
            mBottomBar.addView(mPanelBtn, BaseBar.TB_Position.Position_CENTER);
        }

        mSettingBtn.setImageResource(R.drawable.rd_bar_setting_selector);
        mSettingBtn.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rd_bar_setting));
        mSettingBtn.setRelation(BaseItemImpl.RELATION_BELOW);
        mSettingBtn.setInterval(interval);
        mSettingBtn.setTextSize(AppDisplay.getInstance(mContext).px2dp(textSize));
        mSettingBtn.setTextColorResource(textColorResId);
        mSettingBtn.setTag(ToolbarItemConfig.ITEM_BOTTOMBAR_VIEW);
        mBottomBar.addView(mSettingBtn, BaseBar.TB_Position.Position_CENTER);
        mSettingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUiExtensionsManager.stopHideToolbarsTimer();
                showSettingBar();
            }
        });

        if (mConfig.modules.isLoadAnnotations()) {
            mEditBtn = new BaseItemImpl(mContext);
            mEditBtn.setImageResource(R.drawable.rd_bar_edit_selector);
            mEditBtn.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rd_bar_edit));
            mEditBtn.setRelation(BaseItemImpl.RELATION_BELOW);
            mEditBtn.setInterval(interval);
            mEditBtn.setTextSize(AppDisplay.getInstance(mContext).px2dp(textSize));
            mEditBtn.setTextColorResource(textColorResId);
            mEditBtn.setTag(ToolbarItemConfig.ITEM_BOTTOMBAR_COMMENT);
            mEditBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mUiExtensionsManager.triggerDismissMenuEvent();
                    mUiExtensionsManager.changeState(ReadStateConfig.STATE_EDIT);
                }
            });
            mBottomBar.addView(mEditBtn, BaseBar.TB_Position.Position_CENTER);
        }

        if (mConfig.modules.isLoadSignature()) {
            mReadSignItem = new BaseItemImpl(mContext);
            mReadSignItem.setImageResource(R.drawable.sg_selector);
            mReadSignItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rd_bar_sign));
            mReadSignItem.setRelation(IBaseItem.RELATION_BELOW);
            mReadSignItem.setInterval(interval);
            mReadSignItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(textSize));
            mReadSignItem.setTextColorResource(textColorResId);
            mReadSignItem.setTag(ToolbarItemConfig.ITEM_BOTTOMBAR_SIGN);
            mBottomBar.addView(mReadSignItem, BaseBar.TB_Position.Position_CENTER);
            mReadSignItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AppUtil.isFastDoubleClick()) return;
                    mUiExtensionsManager.triggerDismissMenuEvent();
                    Module module = mUiExtensionsManager.getModuleByName(Module.MODULE_NAME_PSISIGNATURE);
                    if (module instanceof SignatureModule)
                        mUiExtensionsManager.setCurrentToolHandler(((SignatureModule) module).getToolHandler());
                    resetAnnotCustomBottomBar();
                    resetAnnotCustomTopBar();
                    mUiExtensionsManager.changeState(ReadStateConfig.STATE_SIGNATURE);
                }
            });

        }

    }


    private IBaseItem mSignListItem;

    public void resetAnnotCustomBottomBar() {
        mAnnotCustomBottomBar.removeAllItems();
        mAnnotCustomBottomBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_bg_color_toolbar_light));
        mAnnotCustomBottomBar.setItemInterval(mContext.getResources().getDimensionPixelSize(com.foxit.uiextensions.R.dimen.rd_bottombar_button_space));

        mSignListItem = new BaseItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (AppDisplay.getInstance(mContext).isPad()) {
                    Module module = mUiExtensionsManager.getModuleByName(Module.MODULE_NAME_PSISIGNATURE);
                    if (module != null) {
                        SignatureToolHandler signature_toolHandler = (SignatureToolHandler) ((SignatureModule) module).getToolHandler();
                        if (signature_toolHandler.getPropertyBar().isShowing()) {
                            Rect rect = new Rect();
                            mSignListItem.getContentView().getGlobalVisibleRect(rect);
                            signature_toolHandler.getPropertyBar().update(new RectF(rect));
                        }
                    }
                }
            }
        };
        mSignListItem.setImageResource(R.drawable.sg_list_selector);
        mSignListItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_sign_model));
        mSignListItem.setRelation(IBaseItem.RELATION_BELOW);
        mSignListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mSignListItem.getContentView().getGlobalVisibleRect(rect);
                Module module = mUiExtensionsManager.getModuleByName(Module.MODULE_NAME_PSISIGNATURE);
                if (module != null) {
                    SignatureToolHandler signature_toolHandler = (SignatureToolHandler) ((SignatureModule) module).getToolHandler();
                    signature_toolHandler.showSignList(new RectF(rect));
                }
            }
        });
        mAnnotCustomBottomBar.addView(mSignListItem, BaseBar.TB_Position.Position_CENTER);
    }

    public void resetAnnotCustomTopBar() {
        mAnnotCustomTopBar.removeAllItems();
        mAnnotCustomTopBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_bg_color_toolbar_light));

        IBaseItem closeItem = new BaseItemImpl(mContext);
        closeItem.setImageResource(R.drawable.rd_reflow_back_selector);
        closeItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) return;
                mUiExtensionsManager.setCurrentToolHandler(null);
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_NORMAL);
                mUiExtensionsManager.getPDFViewCtrl().invalidate();
            }
        });
        IBaseItem titleItem = new BaseItemImpl(mContext);
        titleItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.sg_signer_title));
        titleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimension(R.dimen.ux_text_height_subhead)));
        titleItem.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_dark));

        mAnnotCustomTopBar.addView(closeItem, BaseBar.TB_Position.Position_LT);
        mAnnotCustomTopBar.addView(titleItem, BaseBar.TB_Position.Position_LT);
    }


    BaseItemImpl mMenuBtn = null;
    private IBaseItem mAnnotDoneBtn = null;

    private void initOtherView() {
        //Topbar backButton
        mBackItem = new BaseItemImpl(mContext);
        mBackItem.setImageResource(R.drawable.rd_reflow_back_selector);
        mBackItem.setTag(ToolbarItemConfig.ITEM_TOPBAR_BACK);
        mTopBar.addView(mBackItem, BaseBar.TB_Position.Position_LT);
        mBackItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUiExtensionsManager.triggerDismissMenuEvent();
                if (mUiExtensionsManager.getBackEventListener() != null && mUiExtensionsManager.getBackEventListener().onBack()) {
                    return;
                }
                mUiExtensionsManager.backToPrevActivity();
            }
        });

        // Topbar Menu Button
        mMenuBtn = new BaseItemImpl(mContext);
        mMenuBtn.setImageResource(R.drawable.rd_bar_more_selector);
        mMenuBtn.setTag(ToolbarItemConfig.ITEM_TOPBAR_MORE);
        mMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreMenu();
            }
        });
        mTopBar.addView(mMenuBtn, BaseBar.TB_Position.Position_RB);

        //Annot Done Button
        mAnnotDoneBtn = new BaseItemImpl(mContext);
        mAnnotDoneBtn.setImageResource(R.drawable.rd_reflow_back_selector);
        mEditDoneBar.addView(mAnnotDoneBtn, BaseBar.TB_Position.Position_LT);
        mAnnotDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUiExtensionsManager.triggerDismissMenuEvent();
                if (mUiExtensionsManager.getCurrentToolHandler() != null) {
                    mUiExtensionsManager.setCurrentToolHandler(null);
                }

                mUiExtensionsManager.changeState(ReadStateConfig.STATE_NORMAL);

                if (!isToolbarsVisible()) {
                    showToolbars();
                }
            }
        });
    }

    void initAnimations() {
        if (mTopBarShowAnim == null) {
            mTopBarShowAnim = new AnimationSet(true);
            mTopBarHideAnim = new AnimationSet(true);
            mBottomBarShowAnim = new AnimationSet(true);
            mBottomBarHideAnim = new AnimationSet(true);
            mMaskShowAnim = new AnimationSet(true);
            mMaskHideAnim = new AnimationSet(true);
        }
        if (mTopBarShowAnim.getAnimations() != null && mTopBarShowAnim.getAnimations().size() > 0) {
            return;
        }
        if (mTopBarLayout.getHeight() == 0) {
            return;
        }
        SHOW_ANIMATION_TAG = R.id.rd_show_animation_tag;
        HIDE_ANIMATION_TAG = R.id.rd_hide_animation_tag;
        // top bar
        TranslateAnimation anim = new TranslateAnimation(0, 0, -mTopBarLayout.getHeight(), 0);
        anim.setDuration(300);
        mTopBarShowAnim.addAnimation(anim);
        anim = new TranslateAnimation(0, 0, 0, -mTopBarLayout.getHeight());
        anim.setDuration(300);
        mTopBarHideAnim.addAnimation(anim);
        // bottom bar
        anim = new TranslateAnimation(0, 0, mBottomBarLayout.getHeight(), 0);
        anim.setDuration(300);
        mBottomBarShowAnim.addAnimation(anim);
        anim = new TranslateAnimation(0, 0, 0, mTopBarLayout.getHeight());
        anim.setDuration(300);
        mBottomBarHideAnim.addAnimation(anim);
        // mask view
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(300);
        mMaskShowAnim.addAnimation(alphaAnimation);
        alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(300);
        mMaskHideAnim.addAnimation(alphaAnimation);

        mTopBarLayout.setTag(SHOW_ANIMATION_TAG, mTopBarShowAnim);
        mTopBarLayout.setTag(HIDE_ANIMATION_TAG, mTopBarHideAnim);
        mBottomBarLayout.setTag(SHOW_ANIMATION_TAG, mBottomBarShowAnim);
        mBottomBarLayout.setTag(HIDE_ANIMATION_TAG, mBottomBarHideAnim);
        mEditDoneBarLayout.setTag(SHOW_ANIMATION_TAG, mTopBarShowAnim);
        mEditDoneBarLayout.setTag(HIDE_ANIMATION_TAG, mTopBarHideAnim);
        mAnnotBarLayout.setTag(SHOW_ANIMATION_TAG, mBottomBarShowAnim);
        mAnnotBarLayout.setTag(HIDE_ANIMATION_TAG, mBottomBarHideAnim);
        mFormBarLayout.setTag(SHOW_ANIMATION_TAG, mBottomBarShowAnim);
        mFormBarLayout.setTag(HIDE_ANIMATION_TAG, mBottomBarHideAnim);
        mToolSetBarLayout.setTag(SHOW_ANIMATION_TAG, mBottomBarShowAnim);
        mToolSetBarLayout.setTag(HIDE_ANIMATION_TAG, mBottomBarHideAnim);
        mAnnotCustomTopBarLayout.setTag(SHOW_ANIMATION_TAG, mTopBarShowAnim);
        mAnnotCustomTopBarLayout.setTag(HIDE_ANIMATION_TAG, mTopBarHideAnim);
        mAnnotCustomBottomBarLayout.setTag(SHOW_ANIMATION_TAG, mBottomBarShowAnim);
        mAnnotCustomBottomBarLayout.setTag(HIDE_ANIMATION_TAG, mBottomBarHideAnim);
        mMaskView.setTag(SHOW_ANIMATION_TAG, mMaskShowAnim);
        mMaskView.setTag(HIDE_ANIMATION_TAG, mMaskHideAnim);
    }

    public void addDocView(View docView) {
        ViewParent parent = docView.getParent();
        if (parent != null) {
            ((ViewGroup) parent).removeView(docView);
        }
        mDocViewerLayout.addView(docView);
    }

    @Override
    public RelativeLayout getContentView() {
        return (RelativeLayout) mRootView;
    }

    @Override
    public BaseBar getTopToolbar() {
        return mTopBar;
    }

    @Override
    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    @Override
    public BaseBar getBottomToolbar() {
        return mBottomBar;
    }

    @Override
    public IPanelManager getPanelManager() {
        return mPanelManager;
    }

    @Override
    public void showToolbars() {
        mIsFullScreen = false;
        mUiExtensionsManager.changeState(mUiExtensionsManager.getState());
        AppUtil.showSystemUI(mUiExtensionsManager.getAttachedActivity());
    }

    @Override
    public void hideToolbars() {
        mIsFullScreen = true;
        mUiExtensionsManager.changeState(mUiExtensionsManager.getState());
        if (isSupportFullScreen() && mIsHideSystemUI) {
            AppUtil.hideSystemUI(mUiExtensionsManager.getAttachedActivity());
        }
    }

    private boolean isSupportFullScreen() {
        int state = mUiExtensionsManager.getState();
        return state == ReadStateConfig.STATE_NORMAL ||
                state == ReadStateConfig.STATE_REFLOW ||
                state == ReadStateConfig.STATE_PANZOOM;
    }

    @Override
    public boolean isToolbarsVisible() {
        return !mIsFullScreen;
    }

    private void showMoreMenu() {
        mUiExtensionsManager.triggerDismissMenuEvent();
        mMoreMenuModule = (MoreMenuModule) mUiExtensionsManager.getModuleByName(Module.MODULE_MORE_MENU);
        if (mMoreMenuModule == null) return;
        MoreMenuView view = mMoreMenuModule.getView();
        if (view != null)
            view.show();
    }

    private void hideMoreMenu() {
        mMoreMenuModule = (MoreMenuModule) mUiExtensionsManager.getModuleByName(Module.MODULE_MORE_MENU);
        if (mMoreMenuModule == null) return;
        MoreMenuView view = mMoreMenuModule.getView();
        if (view != null)
            view.hide();
    }

    @Override
    public IMultiLineBar getSettingBar() {
        return mSettingBar;
    }

    private void setSettingView(View view) {
        mSettingPopupWindow = new PopupWindow(view,
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);
        mSettingPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
        mSettingPopupWindow.setAnimationStyle(R.style.View_Animation_BtoT);
        mSettingPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mStateChangeListener.onStateChanged(mUiExtensionsManager.getState(), mUiExtensionsManager.getState());
                mUiExtensionsManager.startHideToolbarsTimer();
            }
        });
    }

    private void showThumbnailDialog() {
        ThumbnailModule thumbnailModule = (ThumbnailModule) mUiExtensionsManager.getModuleByName(Module.MODULE_NAME_THUMBNAIL);
        if (thumbnailModule != null)
            thumbnailModule.show();
    }

    private void rotateView() {
        int rotation = mUiExtensionsManager.getPDFViewCtrl().getViewRotation();
        rotation = (rotation + 1) % 4;

        mUiExtensionsManager.getPDFViewCtrl().rotateView(rotation);
        mUiExtensionsManager.getPDFViewCtrl().updatePagesLayout();
    }

    class SettingValueChangeListener implements IMultiLineBar.IValueChangeListener {
        //should be one of IMultiLineBar.TYPE_XXX
        private int type;

        public SettingValueChangeListener(int type) {
            this.type = type;
        }

        @Override
        public void onValueChanged(int type, Object value) {
            switch (type) {
                case IMultiLineBar.TYPE_THUMBNAIL:
                    showThumbnailDialog();
                    mUiExtensionsManager.getMainFrame().hideSettingBar();
                    break;
                case IMultiLineBar.TYPE_SINGLEPAGE:
                    mPageLayoutMode = (Integer) value;
                    mSettingBar.setProperty(IMultiLineBar.TYPE_SINGLEPAGE, mPageLayoutMode);
                    mUiExtensionsManager.getPDFViewCtrl().setPageLayoutMode(mPageLayoutMode);
                    break;
                case IMultiLineBar.TYPE_FITPAGE:
                case IMultiLineBar.TYPE_FITWIDTH:
                    mZoomMode = (Integer) value;
                    mSettingBar.setProperty(type, mZoomMode);
                    mUiExtensionsManager.getPDFViewCtrl().setZoomMode(mZoomMode);
                    break;
                case IMultiLineBar.TYPE_ROTATEVIEW:
                    rotateView();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onDismiss() {

        }

        @Override
        public int getType() {
            return this.type;
        }
    }

    private Point clacSettingPopupWindowSize() {
        int height = mSettingBar.getContentView().getMeasuredHeight();
        int screenWidth = mRootView.getWidth();
        int screenHeight = mRootView.getHeight();

        int width = screenWidth;

        if (AppDisplay.getInstance(mContext).isPad()) {
            width = (int) (Math.max(screenWidth, screenHeight) * 0.35f);
            width = Math.min(screenWidth, width);
        }

        if (height >= screenHeight) {
            height = (int) (screenHeight * 0.7f);
        }

        return new Point(width, height);
    }

    @SuppressLint("WrongConstant")
    void showSettingBar() {
        if (mSettingBar == null) return;
        mUiExtensionsManager.triggerDismissMenuEvent();
        mSettingBar.getContentView().measure(0, 0);
        mSettingBar.registerListener(new SettingValueChangeListener(IMultiLineBar.TYPE_SINGLEPAGE));
        mSettingBar.registerListener(new SettingValueChangeListener(IMultiLineBar.TYPE_THUMBNAIL));
        mSettingBar.registerListener(new SettingValueChangeListener(IMultiLineBar.TYPE_FITPAGE));
        mSettingBar.registerListener(new SettingValueChangeListener(IMultiLineBar.TYPE_FITWIDTH));
        mSettingBar.registerListener(new SettingValueChangeListener(IMultiLineBar.TYPE_ROTATEVIEW));

        Point point = clacSettingPopupWindowSize();
        mSettingPopupWindow.setWidth(point.x);
        mSettingPopupWindow.setHeight(point.y);
        mSettingPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mSettingPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        int x = 0;
        int y = 0;
        int[] location = new int[2];
        mRootView.getLocationOnScreen(location);
        int rootX = location[0];
        Rect viewbtnRect = new Rect();
        mSettingBtn.getContentView().getGlobalVisibleRect(viewbtnRect);
        y = viewbtnRect.bottom - point.y;
        if (AppDisplay.getInstance(mContext).isPad()) {
            x = viewbtnRect.centerX() - point.x / 2;
            x = Math.max(x, rootX);
            y = viewbtnRect.top - point.y;
        }
        mSettingPopupWindow.showAtLocation(mRootView, Gravity.LEFT | Gravity.TOP, x, y);
        mStateChangeListener.onStateChanged(mUiExtensionsManager.getState(), mUiExtensionsManager.getState());
        mPageLayoutMode = mUiExtensionsManager.getPDFViewCtrl().getPageLayoutMode();
        mSettingBar.setProperty(IMultiLineBar.TYPE_SINGLEPAGE, mPageLayoutMode);
        mZoomMode = mUiExtensionsManager.getPDFViewCtrl().getZoomMode();
        if (mZoomMode == PDFViewCtrl.ZOOMMODE_FITPAGE) {
            mSettingBar.setProperty(IMultiLineBar.TYPE_FITPAGE, mZoomMode);
        } else if (mZoomMode == PDFViewCtrl.ZOOMMODE_FITWIDTH) {
            mSettingBar.setProperty(IMultiLineBar.TYPE_FITWIDTH, mZoomMode);
        } else {
            mSettingBar.setProperty(IMultiLineBar.TYPE_FITPAGE, mZoomMode);
            mSettingBar.enableBar(IMultiLineBar.TYPE_FITPAGE, true);
            mSettingBar.enableBar(IMultiLineBar.TYPE_FITWIDTH, true);
        }
    }

    private void updateSettingBar() {
        if (mSettingBar == null || !mSettingPopupWindow.isShowing()) return;
        mSettingBar.getContentView().measure(0, 0);
        int barHeight = mSettingBar.getContentView().getMeasuredHeight();
        int rootWidth = mRootView.getWidth();
        int rootHeight = mRootView.getHeight();

        int x = 0;
        int y = 0;
        int[] location = new int[2];
        mRootView.getLocationOnScreen(location);
        int rootX = location[0];
        int rootY = location[1];

        if (barHeight >= rootHeight) {
            barHeight = (int) (rootHeight * 0.7f);
        }

        int barWidth = rootWidth;
        Rect viewbtnRect = new Rect();
        mSettingBtn.getContentView().getGlobalVisibleRect(viewbtnRect);
        if (AppDisplay.getInstance(mContext).isPad()) {
            barWidth = (int) (Math.max(rootWidth, rootHeight) * 0.35f);
            barWidth = Math.min(rootWidth, barWidth);
            x = viewbtnRect.centerX() - barWidth / 2;
            x = Math.max(x, rootX);
            y = viewbtnRect.top - barHeight;
        } else {
            if (Build.VERSION.SDK_INT == 24) {
                y = rootHeight - barHeight + rootY;
            } else {
                y = viewbtnRect.bottom - barHeight;
            }
        }
        mSettingPopupWindow.update(x, y, barWidth, barHeight);
    }

    @Override
    public void hideSettingBar() {
        if (mSettingPopupWindow != null && mSettingPopupWindow.isShowing()) {
            mSettingPopupWindow.dismiss();
        }
    }

    @Override
    public MoreTools getMoreToolsBar() {
        return mAnnotBar;
    }

    @Override
    public void showMaskView() {
        mThirdMaskCounter++;
        mStateChangeListener.onStateChanged(mUiExtensionsManager.getState(), mUiExtensionsManager.getState());
    }

    @Override
    public void hideMaskView() {
        mThirdMaskCounter--;
        if (mThirdMaskCounter < 0)
            mThirdMaskCounter = 0;
        mStateChangeListener.onStateChanged(mUiExtensionsManager.getState(), mUiExtensionsManager.getState());
    }

    public void resetMaskView() {
        if (mPanelManager != null && mPanelManager.getPanelWindow().isShowing()) {
            mPanelManager.hidePanel();
        }

        if (mMoreMenuModule != null) {
            hideMoreMenu();
        }

        if (mSettingPopupWindow.isShowing()) {
            hideSettingBar();
        }

        if (isMaskViewShowing()) {
            hideMaskView();
        }
        mThirdMaskCounter = 0;
    }

    @Override
    public boolean isMaskViewShowing() {
        return mMaskView.getVisibility() == View.VISIBLE
                || mThirdMaskCounter > 0;
    }

    @Override
    public void enableTopToolbar(boolean isEnabled) {
        if (mTopBarLayout != null) {
            if (isEnabled) {
                mIsShowTopToolbar = true;
                mTopBarLayout.setVisibility(View.VISIBLE);
            } else {
                mIsShowTopToolbar = false;
                mTopBarLayout.setVisibility(View.GONE);
            }

        }
    }

    @Override
    public void enableBottomToolbar(boolean isEnabled) {
        if (mBottomBarLayout != null) {
            if (isEnabled) {
                mIsShowBottomToolbar = true;
                mBottomBarLayout.setVisibility(View.VISIBLE);
            } else {
                mIsShowBottomToolbar = false;
                mBottomBarLayout.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public BaseBar getEditDoneBar() {
        return mEditDoneBar;
    }

    @Override
    public BaseBar getFormBar() {
        return mFormBar;
    }

    @Override
    public BaseBar getToolSetBar() {
        return mToolSetBar;
    }

    @Override
    public Activity getAttachedActivity() {
        return mAttachedActivity;
    }

    @Override
    public void setAttachedActivity(Activity act) {
        mAttachedActivity = act;
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    private IStateChangeListener mStateChangeListener = new IStateChangeListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            initAnimations();

            if (mConfig.modules.isLoadAnnotations()) {
                if (mUiExtensionsManager.getDocumentManager().canAddAnnot()) {
                    mEditBtn.setEnable(true);
                } else {
                    mEditBtn.setEnable(false);
                }
            }
            if (mConfig.modules.isLoadSignature()) {
                if (mUiExtensionsManager.getDocumentManager().canAddSignature()) {
                    mReadSignItem.setEnable(true);
                } else {
                    mReadSignItem.setEnable(false);
                }
            }

            ArrayList<View> currentShowViews = new ArrayList<View>();
            ArrayList<View> willShowViews = new ArrayList<View>();
            for (View view : mStateLayoutList) {
                if (view.getVisibility() == View.VISIBLE) {
                    currentShowViews.add(view);
                }
            }
            switch (newState) {
                case ReadStateConfig.STATE_NORMAL:
                    if (isToolbarsVisible()) {
                        if (mIsShowTopToolbar) {
                            willShowViews.add(mTopBarLayout);
                        }

                        if (mIsShowBottomToolbar) {
                            willShowViews.add(mBottomBarLayout);
                        }
                    }
                    break;
                case ReadStateConfig.STATE_EDIT:
                    if (isToolbarsVisible()) {
                        willShowViews.add(mEditDoneBarLayout);
                        willShowViews.add(mAnnotBarLayout);
                    }
                    break;
                case ReadStateConfig.STATE_SIGNATURE:
                    if (isToolbarsVisible()) {
                        willShowViews.add(mAnnotCustomBottomBarLayout);
                        willShowViews.add(mAnnotCustomTopBarLayout);
                    }
                    break;
                case ReadStateConfig.STATE_ANNOTTOOL:
                    willShowViews.add(mEditDoneBarLayout);
                    willShowViews.add(mToolSetBarLayout);
                    ToolHandler toolHandler = mUiExtensionsManager.getCurrentToolHandler();
                    if (toolHandler != null) {
                        mToolIconView.setImageResource(getToolIcon(toolHandler));
                        mToolNameTv.setText(getToolName(toolHandler));
                    }
                    break;
                case ReadStateConfig.STATE_CREATE_FORM:
                    if (isToolbarsVisible()) {
                        willShowViews.add(mEditDoneBarLayout);
                        willShowViews.add(mFormBarLayout);
                    }
                    break;
                case ReadStateConfig.STATE_REFLOW:
                case ReadStateConfig.STATE_SEARCH:
                case ReadStateConfig.STATE_PANZOOM:
                case ReadStateConfig.STATE_COMPARE:
                case ReadStateConfig.STATE_PAGENAVIGATION:
                case ReadStateConfig.STATE_TTS:
                default:
                    break;
            }
            for (View view : currentShowViews) {
                if (willShowViews.contains(view))
                    continue;
                if (newState == oldState && view.getTag(HIDE_ANIMATION_TAG) != null) {
                    view.startAnimation((AnimationSet) view.getTag(HIDE_ANIMATION_TAG));
                }
                view.setVisibility(View.INVISIBLE);
            }
            for (View view : willShowViews) {
                if (currentShowViews.contains(view))
                    continue;
                if (view.getTag(SHOW_ANIMATION_TAG) != null) {
                    view.startAnimation((Animation) view.getTag(SHOW_ANIMATION_TAG));
                }
                view.setVisibility(View.VISIBLE);
            }
            if ((mPanelManager != null && mPanelManager.getPanelWindow().isShowing())
                    || mSettingPopupWindow.isShowing()
                    || mThirdMaskCounter > 0) {
                if (mMaskView.getVisibility() != View.VISIBLE) {
                    mRootView.removeView(mMaskView);
                    mRootView.addView(mMaskView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    mMaskView.setVisibility(View.VISIBLE);
                    if (mMaskView.getTag(SHOW_ANIMATION_TAG) != null) {
                        mMaskView.startAnimation((AnimationSet) mMaskView.getTag(SHOW_ANIMATION_TAG));
                    }
                }
            } else {
                if (mMaskView.getVisibility() != View.GONE) {
                    mMaskView.setVisibility(View.GONE);
                    if (mMaskView.getTag(HIDE_ANIMATION_TAG) != null) {
                        mMaskView.startAnimation((AnimationSet) mMaskView.getTag(HIDE_ANIMATION_TAG));
                    }
                }
            }
        }
    };

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (newWidth != oldWidth || newHeight != oldHeight) {
                if (mSettingBar == null || mSettingPopupWindow.isShowing()) {
                    updateSettingBar();
                }
            }
        }
    };

    private ILifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {

        @Override
        public void onHiddenChanged(boolean hidden) {
            if (hidden) {
                mUiExtensionsManager.stopHideToolbarsTimer();
            } else {
                showToolbars();
                mUiExtensionsManager.resetHideToolbarsTimer();
            }
        }
    };

    private int getToolIcon(ToolHandler toolHandler) {
        int toolIcon = R.drawable.fx_item_detail;
        String type = toolHandler.getType();
        switch (type) {
            case ToolHandler.TH_TYPE_HIGHLIGHT:
                toolIcon = R.drawable.annot_tool_prompt_highlight;
                break;
            case ToolHandler.TH_TYPE_SQUIGGLY:
                toolIcon = R.drawable.annot_tool_prompt_squiggly;
                break;
            case ToolHandler.TH_TYPE_STRIKEOUT:
                toolIcon = R.drawable.annot_tool_prompt_strikeout;
                break;
            case ToolHandler.TH_TYPE_UNDERLINE:
                toolIcon = R.drawable.annot_tool_prompt_underline;
                break;
            case ToolHandler.TH_TYPE_NOTE:
                toolIcon = R.drawable.annot_tool_prompt_text;
                break;
            case ToolHandler.TH_TYPE_CIRCLE:
                toolIcon = R.drawable.annot_tool_prompt_circle;
                break;
            case ToolHandler.TH_TYPE_SQUARE:
                toolIcon = R.drawable.annot_tool_prompt_square;
                break;
            case ToolHandler.TH_TYPE_TYPEWRITER:
                toolIcon = R.drawable.annot_tool_prompt_typwriter;
                break;
            case ToolHandler.TH_TYPE_CALLOUT:
                toolIcon = R.drawable.annot_toll_prompt_callout;
                break;
            case ToolHandler.TH_TYPE_TEXTBOX:
                toolIcon = R.drawable.annot_textbox_push;
                break;
            case ToolHandler.TH_TYPR_INSERTTEXT:
                toolIcon = R.drawable.annot_tool_prompt_insert;
                break;
            case ToolHandler.TH_TYPE_REPLACE:
                toolIcon = R.drawable.annot_tool_prompt_replace;
                break;
            case ToolHandler.TH_TYPE_STAMP:
                toolIcon = R.drawable.annot_tool_prompt_stamp;
                break;
            case ToolHandler.TH_TYPE_ERASER:
                toolIcon = R.drawable.annot_tool_prompt_eraser;
                break;
            case ToolHandler.TH_TYPE_INK:
                toolIcon = R.drawable.annot_tool_prompt_pencil;
                break;
            case ToolHandler.TH_TYPE_ARROW:
                toolIcon = R.drawable.annot_tool_prompt_arrow;
                break;
            case ToolHandler.TH_TYPE_LINE:
                toolIcon = R.drawable.annot_tool_prompt_line;
                break;
            case ToolHandler.TH_TYPE_FILEATTACHMENT:
                toolIcon = R.drawable.annot_tool_prompt_fileattachment;
                break;
            case ToolHandler.TH_TYPE_DISTANCE:
                toolIcon = R.drawable.icon_annot_distance_tips;
                break;
            case ToolHandler.TH_TYPE_PDFIMAGE:
                toolIcon = R.drawable.annot_tool_prompt_image;
                break;
            case ToolHandler.TH_TYPE_SCREEN_AUDIO:
                toolIcon = R.drawable.annot_tool_prompt_audio;
                break;
            case ToolHandler.TH_TYPE_SCREEN_VIDEO:
                toolIcon = R.drawable.annot_tool_prompt_video;
                break;
            case ToolHandler.TH_TYPE_POLYGON:
                toolIcon = R.drawable.annot_tool_prompt_polygon;
                break;
            case ToolHandler.TH_TYPE_POLYGONCLOUD:
                toolIcon = R.drawable.annot_tool_prompt_polygoncloud;
                break;
            case ToolHandler.TH_TYPE_POLYLINE:
                toolIcon = R.drawable.annot_tool_prompt_polyline;
                break;
            case ToolHandler.TH_TYPE_SELECT_ANNOTATIONS:
                toolIcon = R.drawable.annot_tool_prompt_multi_select;
                break;
            default:
                break;
        }

        return toolIcon;
    }

    String getToolName(ToolHandler toolHandler) {
        String toolName = "-";
        String type = toolHandler.getType();
        switch (type) {
            case ToolHandler.TH_TYPE_HIGHLIGHT:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_highlight);
                break;
            case ToolHandler.TH_TYPE_SQUIGGLY:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_squiggly);
                break;
            case ToolHandler.TH_TYPE_STRIKEOUT:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_strikeout);
                break;
            case ToolHandler.TH_TYPE_UNDERLINE:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_underline);
                break;
            case ToolHandler.TH_TYPE_NOTE:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_note);
                break;
            case ToolHandler.TH_TYPE_CIRCLE:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_oval);
                break;
            case ToolHandler.TH_TYPE_SQUARE:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_rectangle);
                break;
            case ToolHandler.TH_TYPE_TYPEWRITER:
                toolName = mContext.getApplicationContext().getString(R.string.annot_tool_display_name_typewrite);
                break;
            case ToolHandler.TH_TYPE_CALLOUT:
                toolName = mContext.getApplicationContext().getString(R.string.annot_tool_display_name_callout);
                break;
            case ToolHandler.TH_TYPE_TEXTBOX:
                toolName = mContext.getApplicationContext().getString(R.string.annot_tool_display_name_textbox);
                break;
            case ToolHandler.TH_TYPR_INSERTTEXT:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_insert_text);
                break;
            case ToolHandler.TH_TYPE_REPLACE:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_replace_text);
                break;
            case ToolHandler.TH_TYPE_STAMP:
                toolName = mContext.getApplicationContext().getString(R.string.annot_tool_display_name_stamp);
                break;
            case ToolHandler.TH_TYPE_ERASER:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_eraser);
                break;
            case ToolHandler.TH_TYPE_INK:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_pencil);
                break;
            case ToolHandler.TH_TYPE_ARROW:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_arrow);
                break;
            case ToolHandler.TH_TYPE_LINE:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_line);
                break;
            case ToolHandler.TH_TYPE_FILEATTACHMENT:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_fileattachment);
                break;
            case ToolHandler.TH_TYPE_DISTANCE:
                toolName = mContext.getApplicationContext().getString(R.string.fx_distance);
                break;
            case ToolHandler.TH_TYPE_PDFIMAGE:
                toolName = mContext.getApplicationContext().getString(R.string.screen_annot_image);
                break;
            case ToolHandler.TH_TYPE_SCREEN_AUDIO:
                toolName = mContext.getApplicationContext().getString(R.string.screen_annot_audio);
                break;
            case ToolHandler.TH_TYPE_SCREEN_VIDEO:
                toolName = mContext.getApplicationContext().getString(R.string.screen_annot_video);
                break;
            case ToolHandler.TH_TYPE_POLYGON:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_polygon);
                break;
            case ToolHandler.TH_TYPE_POLYGONCLOUD:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_polygon_cloud);
                break;
            case ToolHandler.TH_TYPE_POLYLINE:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_polyLine);
                break;
            case ToolHandler.TH_TYPE_SELECT_ANNOTATIONS:
                toolName = mContext.getApplicationContext().getString(R.string.fx_string_multi_select);
                break;
            default:
                break;
        }
        return toolName;
    }

    public boolean removeBottomBar(IBarsHandler.BarName barName) {
        if (IBarsHandler.BarName.TOP_BAR.equals(barName)) {
            mTopBarLayout.removeAllViews();
            mIsShowTopToolbar = false;
            mTopBarLayout.setVisibility(View.GONE);
            return true;
        } else if (IBarsHandler.BarName.BOTTOM_BAR.equals(barName)) {
            mBottomBarLayout.removeAllViews();
            mIsShowBottomToolbar = false;
            mBottomBarLayout.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    public boolean addCustomToolBar(IBarsHandler.BarName barName, View view) {
        if (view == null) {
            return false;
        }

        int height;
        if (AppDisplay.getInstance(mContext).isPad()) {
            height = mContext.getResources().getDimensionPixelOffset(R.dimen.ux_toolbar_height_pad);
        } else {
            height = mContext.getResources().getDimensionPixelOffset(R.dimen.ux_toolbar_height_phone);
        }
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        view.setLayoutParams(params);

        if (IBarsHandler.BarName.TOP_BAR.equals(barName)) {
            mTopBarLayout.removeAllViews();
            mIsShowTopToolbar = true;
            mTopBarLayout.setVisibility(View.VISIBLE);
            mTopBarLayout.addView(view, 0);
            return true;
        } else if (IBarsHandler.BarName.BOTTOM_BAR.equals(barName)) {
            mBottomBarLayout.removeAllViews();
            mIsShowBottomToolbar = true;
            mBottomBarLayout.setVisibility(View.VISIBLE);
            mBottomBarLayout.addView(view);
            return true;
        }
        return false;
    }

    public boolean addSubViewToTopBar(View subView, int index, LinearLayout.LayoutParams params) {
        if (index <= 0 || subView == null) return false;
        mTopBarLayout.addView(subView, index, params);
        return true;
    }

    public boolean removeSubViewFromTopBar(View subView) {
        if (subView == null) return false;
        mTopBarLayout.removeView(subView);
        return true;
    }

    public void setHideSystemUI(boolean isHide) {
        mIsHideSystemUI = isHide;
    }

    public boolean isHideSystemUI() {
        return mIsHideSystemUI;
    }

    public Animation getTopbarShowAnimation() {
        return mTopBarShowAnim;
    }

    public Animation getBottombarShowAnimation() {
        return mBottomBarShowAnim;
    }

    public Animation getTopbarHideAnimation() {
        return mTopBarHideAnim;
    }

    public Animation getBottombarHideAnimation() {
        return mBottomBarHideAnim;
    }
}
