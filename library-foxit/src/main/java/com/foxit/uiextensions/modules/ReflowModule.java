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
package com.foxit.uiextensions.modules;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.ReflowPage;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.config.modules.ModulesConfig;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BottomBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.modules.panel.IPanelManager;
import com.foxit.uiextensions.modules.panel.annot.AnnotPanelModule;
import com.foxit.uiextensions.modules.panel.filespec.FileSpecPanelModule;
import com.foxit.uiextensions.modules.panel.signature.SignaturePanelModule;
import com.foxit.uiextensions.pdfreader.IMainFrame;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.OnPageEventListener;

/** Reflow the PDF page , so it will fit to the small screen such like cellphone.It is more convenient to read.*/
public class ReflowModule implements Module {
    private static final float MAX_ZOOM = 8.0f;
    private static final float MIN_ZOOM = 1.0f;

    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;

    private boolean mIsReflow;
    private IMultiLineBar mSettingBar;
    private int mPreViewMode;
    private int mPreReflowMode;

    private BaseBar mReflowTopBar;
    private BaseBar mReflowBottomBar;
    private IBaseItem mBackItem;
    private IBaseItem mTitleItem;
    private IBaseItem mBookmarkItem;
    private IBaseItem mPicItem;
    private IBaseItem mZoomOutItem;//out(-)
    private IBaseItem mZoomInItem;//in(+)
    private IBaseItem mPrePageItem;
    private IBaseItem mNextPageItem;
    private IBaseItem mListItem;
    private float mScale = 1.0f;

    private ModulesConfig mModuleConfig;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public ReflowModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;

            mModuleConfig = uiExtensionsManager.getConfig().modules;
            mSettingBar = uiExtensionsManager.getMainFrame().getSettingBar();
            uiExtensionsManager.registerStateChangeListener(mStatusChangeListener);
            uiExtensionsManager.registerModule(this);
        }

        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        return true;
    }


    class ReflowBottomBar extends BottomBarImpl {
        public ReflowBottomBar(Context context) {
            super(context);
        }
    }

    private void addBar() {
        RelativeLayout.LayoutParams reflowTopLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        reflowTopLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mParent.addView(mReflowTopBar.getContentView(), reflowTopLp);
        RelativeLayout.LayoutParams reflowBottomLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        reflowBottomLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mParent.addView(mReflowBottomBar.getContentView(), reflowBottomLp);
        mReflowTopBar.getContentView().setVisibility(View.INVISIBLE);
        mReflowBottomBar.getContentView().setVisibility(View.INVISIBLE);
    }

    private void removeBar() {
        mParent.removeView(mReflowBottomBar.getContentView());
        mParent.removeView(mReflowTopBar.getContentView());
    }

    private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener() {
        @Override
        public void onPageChanged(int oldPageIndex, int curPageIndex) {
            resetNextPageItem();
            resetPrePageItem();
        }
    };

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
            initReflowBar();
        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != Constants.e_ErrSuccess)
                return;
            if (!mPdfViewCtrl.isDynamicXFA()) {
                addBar();
            }
            initValue();
            if (!initMLBarValue()) return;
            applyValue();
            registerMLListener();
            mPdfViewCtrl.registerPageEventListener(mPageEventListener);
            Module bookmarkModule = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_BOOKMARK);
            if ((bookmarkModule instanceof ReadingBookmarkModule)) {
                boolean canAssemble = ((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().canAssemble();
                mBookmarkItem.setEnable(canAssemble);
                ((ReadingBookmarkModule) bookmarkModule).addMarkedItem(mBookmarkItem);
                ((ReadingBookmarkModule) bookmarkModule).remarkItemState(mPdfViewCtrl.getCurrentPage());
            }
            onStatusChanged();

            String pageMode = ((UIExtensionsManager)mUiExtensionsManager).getConfig().uiSettings.pageMode;
            if ("Reflow".equals(pageMode)){
                setReflowPageMode(true);
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {

        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            removeBar();
            mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
            Module bookmarkModule = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_BOOKMARK);
            if (bookmarkModule != null && (bookmarkModule instanceof ReadingBookmarkModule)) {
                ((ReadingBookmarkModule) bookmarkModule).removeMarkedItem(mBookmarkItem);
            }
            unRegisterMLListener();
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private boolean isLoadPanel() {
        return (mModuleConfig == null) || mModuleConfig.isLoadReadingBookmark()
                || mModuleConfig.isLoadOutline() || mModuleConfig.isLoadAttachment();
    }

    private void initReflowBar() {
        mReflowTopBar = new TopBarImpl(mContext);
        mReflowBottomBar = new ReflowBottomBar(mContext);
        mReflowBottomBar.setInterval(true);

        mBackItem = new BaseItemImpl(mContext);
        mTitleItem = new BaseItemImpl(mContext);
        mBookmarkItem = new BaseItemImpl(mContext);
        mPicItem = new BaseItemImpl(mContext);
        mZoomOutItem = new BaseItemImpl(mContext);
        mZoomInItem = new BaseItemImpl(mContext);
        mPrePageItem = new BaseItemImpl(mContext);
        mNextPageItem = new BaseItemImpl(mContext);
        mListItem = new BaseItemImpl(mContext);

        initItemsImgRes();
        initItemsOnClickListener();

        mModuleConfig = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getConfig().modules;
        if (mModuleConfig == null || isLoadPanel()) {
            mReflowBottomBar.addView(mListItem, BaseBar.TB_Position.Position_CENTER);
        }

        mReflowBottomBar.addView(mZoomInItem, BaseBar.TB_Position.Position_CENTER);
        mReflowBottomBar.addView(mZoomOutItem, BaseBar.TB_Position.Position_CENTER);
        mReflowBottomBar.addView(mPicItem, BaseBar.TB_Position.Position_CENTER);
        mReflowBottomBar.addView(mPrePageItem, BaseBar.TB_Position.Position_CENTER);
        mReflowBottomBar.addView(mNextPageItem, BaseBar.TB_Position.Position_CENTER);

        mReflowTopBar.addView(mBackItem, BaseBar.TB_Position.Position_LT);

        mReflowTopBar.addView(mTitleItem, BaseBar.TB_Position.Position_LT);
        if (mModuleConfig == null || mModuleConfig.isLoadReadingBookmark()) {
            mReflowTopBar.addView(mBookmarkItem, BaseBar.TB_Position.Position_RB);
        }
        mReflowTopBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_bg_color_toolbar_colour));
        mReflowBottomBar.setBackgroundColor(mContext.getApplicationContext().getResources().getColor(R.color.ux_bg_color_toolbar_light));
    }

    private void initItemsImgRes() {
        mPicItem.setImageResource(R.drawable.rd_reflow_no_picture_selector);
        mZoomOutItem.setImageResource(R.drawable.rd_reflow_zoomout_selecter);
        mZoomInItem.setImageResource(R.drawable.rd_reflow_zoomin_selecter);
        mPrePageItem.setImageResource(R.drawable.rd_reflow_previous_selecter);
        mNextPageItem.setImageResource(R.drawable.rd_reflow_next_selecter);
        mListItem.setImageResource(R.drawable.rd_reflow_list_selecter);

        mBackItem.setImageResource(R.drawable.cloud_back);
        mBackItem.setTag(ToolbarItemConfig.ITEM_TOPBAR_BACK);
        mTitleItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rd_reflow_topbar_title));
        mTitleItem.setTag(ToolbarItemConfig.ITEM_TOPBAR_BACK + 1);
        mTitleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimensionPixelOffset(R.dimen.ux_text_height_title)));
        mTitleItem.setTextColorResource(R.color.ux_text_color_title_light);

        mBookmarkItem.setImageResource(R.drawable.bookmark_topbar_blue_add_selector);
    }

    private void resetPicItem() {
        if ((mPdfViewCtrl.getReflowMode()& ReflowPage.e_WithImage) == 1) {
            mPicItem.setImageResource(R.drawable.rd_reflow_picture_selector);
        } else {
            mPicItem.setImageResource(R.drawable.rd_reflow_no_picture_selector);
        }
    }


    private void resetZoomOutItem() {
        if (isMinZoomScale()) {
            mZoomOutItem.setEnable(false);
            mZoomOutItem.setImageResource(R.drawable.rd_reflow_zoomout_pressed);
        } else {
            mZoomOutItem.setEnable(true);
            mZoomOutItem.setImageResource(R.drawable.rd_reflow_zoomout_selecter);
        }
    }

    private void resetZoomInItem() {
        if (isMaxZoomScale()) {
            mZoomInItem.setEnable(false);
            mZoomInItem.setImageResource(R.drawable.rd_reflow_zoomin_pressed);
        } else {
            mZoomInItem.setEnable(true);
            mZoomInItem.setImageResource(R.drawable.rd_reflow_zoomin_selecter);
        }
    }

    private void resetPrePageItem() {
        if (mPdfViewCtrl.getCurrentPage() == 0) {
            mPrePageItem.setImageResource(R.drawable.rd_reflow_left_pressed);
        } else {
            mPrePageItem.setImageResource(R.drawable.rd_reflow_previous_selecter);
        }
    }

    private void resetNextPageItem() {
        if (mPdfViewCtrl.getCurrentPage() + 1 == mPdfViewCtrl.getPageCount()) {
            mNextPageItem.setImageResource(R.drawable.rd_reflow_right_pressed);
        } else {
            mNextPageItem.setImageResource(R.drawable.rd_reflow_next_selecter);
        }
    }

    private void initItemsOnClickListener() {
        mPicItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager)mUiExtensionsManager).resetHideToolbarsTimer();
                if ((mPdfViewCtrl.getReflowMode()& ReflowPage.e_WithImage) == 1) {
                    mPdfViewCtrl.setReflowMode(ReflowPage.e_Normal);
                    mPicItem.setImageResource(R.drawable.rd_reflow_no_picture_selector);
                } else {
                    mPdfViewCtrl.setReflowMode(ReflowPage.e_WithImage|ReflowPage.e_Normal);
                    mPicItem.setImageResource(R.drawable.rd_reflow_picture_selector);
                }
            }
        });
        mZoomOutItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager)mUiExtensionsManager).resetHideToolbarsTimer();
                if (isMinZoomScale()) {
                    mZoomOutItem.setEnable(false);
                    mZoomOutItem.setImageResource(R.drawable.rd_reflow_zoomout_pressed);
                } else {
                    mScale = Math.max(MIN_ZOOM, mScale*0.8f);
                    mPdfViewCtrl.setZoom(mScale);
                    resetZoomInItem();
                    resetZoomOutItem();
                }

            }
        });
        mZoomInItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager)mUiExtensionsManager).resetHideToolbarsTimer();
                if (isMaxZoomScale()) {
                    mZoomInItem.setEnable(false);
                    mZoomInItem.setImageResource(R.drawable.rd_reflow_zoomin_pressed);
                } else {
                    mScale = Math.min(MAX_ZOOM, mScale*1.25f);
                    mPdfViewCtrl.setZoom(mScale);
                    resetZoomInItem();
                    resetZoomOutItem();
                }
            }
        });
        mPrePageItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager)mUiExtensionsManager).resetHideToolbarsTimer();
                if ((mPdfViewCtrl.getCurrentPage() - 1) >= 0) {
                    mPdfViewCtrl.gotoPrevPage();
                }
            }
        });
        mNextPageItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager)mUiExtensionsManager).resetHideToolbarsTimer();
                if ((mPdfViewCtrl.getCurrentPage() + 1) < mPdfViewCtrl.getPageCount()) {
                    mPdfViewCtrl.gotoNextPage();
                }
            }
        });
        mBackItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager)mUiExtensionsManager).changeState(ReadStateConfig.STATE_NORMAL);
            }
        });
        mListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).triggerDismissMenuEvent();
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getPanelManager().showPanel();
            }
        });
    }

    private void initValue() {
        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_REFLOW) {
            mIsReflow = true;
        } else {
            mIsReflow = false;
        }
        mPreViewMode = mPdfViewCtrl.getPageLayoutMode();
        mPreReflowMode = mPdfViewCtrl.getReflowMode();
    }

    private boolean initMLBarValue() {
        mSettingBar = ((UIExtensionsManager) mUiExtensionsManager).getMainFrame().getSettingBar();
        if (mPdfViewCtrl.isDynamicXFA()) {
            mSettingBar.enableBar(IMultiLineBar.TYPE_REFLOW, false);
            return false;
        }

        mSettingBar.setProperty(IMultiLineBar.TYPE_REFLOW, mIsReflow);
        return true;
    }

    private void applyValue() {
        if (mIsReflow) {
            ((UIExtensionsManager) mUiExtensionsManager).changeState(ReadStateConfig.STATE_REFLOW);
        }
    }

    private boolean isMaxZoomScale() {
        return mScale >= MAX_ZOOM;
    }

    private boolean isMinZoomScale(){
        return mScale <= MIN_ZOOM;
    }


    private void resetAnnotPanelView(boolean showAnnotPanel) {
        if (((UIExtensionsManager) mUiExtensionsManager).isHiddenPanel(PanelSpec.PanelType.Annotations) &&
                ((UIExtensionsManager) mUiExtensionsManager).isHiddenPanel(PanelSpec.PanelType.Signatures)) {
            return;
        }
        AnnotPanelModule annotPanelModule = (AnnotPanelModule) ((UIExtensionsManager) mUiExtensionsManager).getModuleByName(Module.MODULE_NAME_ANNOTPANEL);
        FileSpecPanelModule attachmentPanelModule = (FileSpecPanelModule)((UIExtensionsManager) mUiExtensionsManager).getModuleByName(Module.MODULE_NAME_FILE_PANEL);
        IPanelManager panelManager = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager();
        if (panelManager.getPanel() == null) return;

        if (mModuleConfig.isLoadAttachment() && attachmentPanelModule != null) {
            panelManager.getPanel().removeSpec(attachmentPanelModule);
        }

        if (mModuleConfig.isLoadAnnotations() && annotPanelModule != null) {
            if (showAnnotPanel) {
                panelManager.getPanel().removeSpec(annotPanelModule);
                panelManager.getPanel().addSpec(annotPanelModule);
            } else {
                panelManager.getPanel().removeSpec(annotPanelModule);
            }
        }

        if (mModuleConfig.isLoadAttachment() && attachmentPanelModule != null) {
            panelManager.getPanel().addSpec(attachmentPanelModule);
        }

        SignaturePanelModule signaturePanelModule = (SignaturePanelModule) ((UIExtensionsManager) mUiExtensionsManager).getModuleByName(Module.MODULE_NAME_SIGNATUREPANEL);
        if (mModuleConfig.isLoadSignature() && signaturePanelModule != null) {
            if (showAnnotPanel) {
                panelManager.getPanel().removeSpec(signaturePanelModule);
                panelManager.getPanel().addSpec(signaturePanelModule);
            } else {
                panelManager.getPanel().removeSpec(signaturePanelModule);
            }
        }
    }

    private void registerMLListener() {
        mSettingBar.registerListener(mReflowChangeListener);
    }

    private void unRegisterMLListener() {
        mSettingBar.unRegisterListener(mReflowChangeListener);
    }

    private IMultiLineBar.IValueChangeListener mReflowChangeListener = new IMultiLineBar.IValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (type == IMultiLineBar.TYPE_REFLOW) {
                setReflowPageMode((boolean)value);
            }
        }

        @Override
        public void onDismiss() {
        }

        @Override
        public int getType() {
            return IMultiLineBar.TYPE_REFLOW;
        }
    };

    private void setReflowPageMode(boolean isReflow){
        mIsReflow = isReflow;
        int curLayout = mPdfViewCtrl.getPageLayoutMode();
        int curReflowMode = mPdfViewCtrl.getReflowMode();
        if (curLayout != PDFViewCtrl.PAGELAYOUTMODE_REFLOW) {
            //hide annot menu.
            if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() != null) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
            }
            if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
                ((UIExtensionsManager) mUiExtensionsManager).triggerDismissMenuEvent();
            }
            mPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_REFLOW);
            mPdfViewCtrl.setReflowMode(mPreReflowMode);
            ((UIExtensionsManager) mUiExtensionsManager).changeState(ReadStateConfig.STATE_REFLOW);
            IMainFrame mainFrame = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getMainFrame();
            mainFrame.hideSettingBar();
            if (!mainFrame.isToolbarsVisible()){
                mainFrame.showToolbars();
            }
            resetAnnotPanelView(false);
        } else {
            if (mPreViewMode == PDFViewCtrl.PAGELAYOUTMODE_REFLOW) {
                mPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_SINGLE);
            } else {
                mPdfViewCtrl.setPageLayoutMode(mPreViewMode);
            }
            ((UIExtensionsManager) mUiExtensionsManager).changeState(ReadStateConfig.STATE_NORMAL);
        }
        mPreViewMode = curLayout;
        mPreReflowMode = curReflowMode;
        PageNavigationModule module = (PageNavigationModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PAGENAV);
        if(module != null) {
            module.resetJumpView();
        }
    }

    private void onStatusChanged() {
        if (mPdfViewCtrl.getDoc() == null) {
            return;
        }
        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_REFLOW) {

            if (((UIExtensionsManager) mUiExtensionsManager).getMainFrame().isToolbarsVisible()) {
                mReflowBottomBar.getContentView().setVisibility(View.VISIBLE);
                mReflowTopBar.getContentView().setVisibility(View.VISIBLE);
            } else {
                mReflowBottomBar.getContentView().setVisibility(View.INVISIBLE);
                mReflowTopBar.getContentView().setVisibility(View.INVISIBLE);
            }

            mScale = mPdfViewCtrl.getZoom();
            resetPicItem();
            resetZoomInItem();
            resetZoomOutItem();
            resetNextPageItem();
            resetPrePageItem();
        } else {
            mReflowBottomBar.getContentView().setVisibility(View.INVISIBLE);
            mReflowTopBar.getContentView().setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mDocEventListener = null;
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterStateChangeListener(mStatusChangeListener);
        }
        return true;
    }

    @Override
    public String getName() {
        return MODULE_NAME_REFLOW;
    }

    private IStateChangeListener mStatusChangeListener = new IStateChangeListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            if (newState == ReadStateConfig.STATE_PAGENAVIGATION) return;
            int curLayout = mPdfViewCtrl.getPageLayoutMode();
            int curReflowMode = mPdfViewCtrl.getReflowMode();

            try {
                if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getState() == ReadStateConfig.STATE_REFLOW) {
                    if (curLayout != PDFViewCtrl.PAGELAYOUTMODE_REFLOW) {
                        mPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_REFLOW);
                        mPdfViewCtrl.setReflowMode(mPreReflowMode);
                        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() != null) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        }
                        IMainFrame mainFrame = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getMainFrame();
                        mainFrame.hideSettingBar();
                        if (!mainFrame.isToolbarsVisible()){
                            mainFrame.showToolbars();
                        }
                        mPreViewMode = curLayout;
                        mPreReflowMode = curReflowMode;
                        PageNavigationModule pageNumberJump = (PageNavigationModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PAGENAV);
                        if(pageNumberJump != null)
                            pageNumberJump.resetJumpView();
                    }
                } else {
                    if (curLayout == PDFViewCtrl.PAGELAYOUTMODE_REFLOW) {
                        if (mPreViewMode == PDFViewCtrl.PAGELAYOUTMODE_REFLOW) {
                            mPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_SINGLE);
                        } else {
                            mPdfViewCtrl.setPageLayoutMode(mPreViewMode);
                            resetAnnotPanelView(true);
                        }
                        mPreViewMode = curLayout;
                        mPreReflowMode = curReflowMode;
                        PageNavigationModule pageNumberJump = (PageNavigationModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PAGENAV);
                        if(pageNumberJump != null)
                            pageNumberJump.resetJumpView();
                    }
                }
            } catch (Exception e) {
            }
            if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_REFLOW) {
                mSettingBar.setProperty(IMultiLineBar.TYPE_REFLOW, true);
                mIsReflow = true;
            } else {

                mSettingBar.setProperty(IMultiLineBar.TYPE_REFLOW, false);
                mIsReflow = false;
            }
            onStatusChanged();
        }
    };
}
