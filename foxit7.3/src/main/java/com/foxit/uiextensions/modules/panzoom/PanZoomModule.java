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
package com.foxit.uiextensions.modules.panzoom;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotEventListener;
import com.foxit.uiextensions.annots.IRedactionEventListener;
import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.modules.panzoom.floatwindow.FloatWindowController;
import com.foxit.uiextensions.modules.panzoom.floatwindow.FloatWindowUtil;
import com.foxit.uiextensions.modules.panzoom.floatwindow.receiver.HomeKeyReceiver;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.pdfreader.IMainFrame;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

/**  This module provides a tool to help to magnify and move the PDF page, it's very useful for PDF page with large size. */
public class PanZoomModule implements Module {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private ViewGroup mParent = null;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    private IMultiLineBar mSettingBar;

    private boolean mIsPanZoomModule;

    private BaseBar mPZTopBar;
    private View mPZBottomBar;
    private IBaseItem mBackItem;
    private IBaseItem mTitleItem;
    private SeekBar mSeekBarItem;
    private ImageView mZoomOutItem;//out(-)
    private ImageView mZoomInItem;//in(+)
    private ImageView mPrePageItem;
    private ImageView mNextPageItem;
    private float mScale = 1.0f;
    private boolean mIsConfigurationChanged = false;

    private FloatWindowController mFloatWindowController;

    public PanZoomModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;

        mFloatWindowController = new FloatWindowController(context);
    }

    @Override
    public String getName() {
        return MODULE_NAME_PANZOOM;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;

            mSettingBar = uiExtensionsManager.getMainFrame().getSettingBar();
            uiExtensionsManager.registerModule(this);
            uiExtensionsManager.registerStateChangeListener(mStatusChangeListener);
            uiExtensionsManager.getDocumentManager().registerAnnotEventListener(mAnnotEventListener);
            uiExtensionsManager.getDocumentManager().registerRedactionEventListener(mRedactionEventListener);
            uiExtensionsManager.registerLayoutChangeListener(mLayoutChangeListener);
            uiExtensionsManager.registerConfigurationChangedListener(mConfigurationChangedListener);
        }
        mPdfViewCtrl.registerDocEventListener(docEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.registerScaleGestureEventListener(mScaleListener);

        mFloatWindowController.registerDefaultHomeKeyReceiver();
        mFloatWindowController.registerHomeKeyEventListener(mHomeKeyEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterStateChangeListener(mStatusChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterAnnotEventListener(mAnnotEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterRedactionEventListener(mRedactionEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLayoutChangeListener(mLayoutChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
        }
        mPdfViewCtrl.unregisterDocEventListener(docEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.unregisterScaleGestureEventListener(mScaleListener);

        mFloatWindowController.unregisterHomeKeyEventListener(mHomeKeyEventListener);
        mFloatWindowController.unregisterDefaultHomeKeyReceiver();
        return true;
    }

    public boolean isInPanZoomMode() {
        return mIsPanZoomModule;
    }

    private IStateChangeListener mStatusChangeListener = new IStateChangeListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {

            if (ReadStateConfig.STATE_PANZOOM == ((UIExtensionsManager)mUiExtensionsManager).getState()) {
                if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() != null) {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
                ((UIExtensionsManager)mUiExtensionsManager).getMainFrame().hideSettingBar();
                mIsPanZoomModule = true;
            } else if (ReadStateConfig.STATE_PANZOOM == oldState && ReadStateConfig.STATE_PANZOOM != newState ){
                if (mFloatWindowController.getFloatWindow() != null) {
                    ((PanZoomView) mFloatWindowController.getFloatWindow()).exit();
                    mFloatWindowController.stopFloatWindowServer();
                    FloatWindowUtil.getInstance().setParent(null);
                }
                mIsPanZoomModule = false;
            }

            mSettingBar.setProperty(IMultiLineBar.TYPE_PANZOOM, mIsPanZoomModule);
            onStatusChanged();
        }
    };

    private void onStatusChanged() {
        if (mPdfViewCtrl.getDoc() == null) {
            return;
        }

        if (mIsPanZoomModule) {
            if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getMainFrame().isToolbarsVisible()) {
                mPZBottomBar.setVisibility(View.VISIBLE);
                mPZTopBar.getContentView().setVisibility(View.VISIBLE);
            } else {
                mPZBottomBar.setVisibility(View.INVISIBLE);
                mPZTopBar.getContentView().setVisibility(View.INVISIBLE);
            }

            mScale = mPdfViewCtrl.getZoom();
            mSeekBarItem.setProgress((int) (mScale - 1 + 0.5f));
            resetNextPageItem();
            resetPrePageItem();
        } else {
            mPZBottomBar.setVisibility(View.INVISIBLE);
            mPZTopBar.getContentView().setVisibility(View.INVISIBLE);
        }
    }

    private void addBar() {
        RelativeLayout.LayoutParams pzTopLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pzTopLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mParent.addView(mPZTopBar.getContentView(), pzTopLp);
        RelativeLayout.LayoutParams pzBottomLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pzBottomLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mParent.addView(mPZBottomBar, pzBottomLp);
        mPZTopBar.getContentView().setVisibility(View.INVISIBLE);
        mPZBottomBar.setVisibility(View.INVISIBLE);
    }

    private void removeBar() {
        mParent.removeView(mPZBottomBar);
        mParent.removeView(mPZTopBar.getContentView());
    }

    private void initPanZoomBar() {
        mPZTopBar = new TopBarImpl(mContext);
        mPZBottomBar = LayoutInflater.from(mContext).inflate(R.layout.pan_zoom_bottom_layout, null, false);

        mBackItem = new BaseItemImpl(mContext);
        mTitleItem = new BaseItemImpl(mContext);

        mZoomOutItem = (ImageView) mPZBottomBar.findViewById(R.id.rd_panzoom_ll_zoomout);
        mZoomInItem = (ImageView) mPZBottomBar.findViewById(R.id.rd_panzoom_ll_zoomin);
        mSeekBarItem = (SeekBar) mPZBottomBar.findViewById(R.id.rd_panzoom_ll_zoom);
        mPrePageItem = (ImageView) mPZBottomBar.findViewById(R.id.rd_panzoom_ll_prevpage);
        mNextPageItem = (ImageView) mPZBottomBar.findViewById(R.id.rd_panzoom_ll_nextpage);

        mZoomOutItem.setEnabled(false);
        mZoomInItem.setEnabled(false);

        initItemsImgRes();
        initItemsOnClickListener();

        mPZTopBar.addView(mBackItem, BaseBar.TB_Position.Position_LT);

        mPZTopBar.addView(mTitleItem, BaseBar.TB_Position.Position_LT);
        mPZTopBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_bg_color_toolbar_colour));
    }

    private void initItemsImgRes() {
        mBackItem.setImageResource(R.drawable.cloud_back);
        mTitleItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_pan_zoom_title));
        mTitleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimensionPixelOffset(R.dimen.ux_text_height_title)));
        mTitleItem.setTextColorResource(R.color.ux_text_color_title_light);
    }

    private void resetPrePageItem() {
        if (mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING){
            int currentPageIndex = mPdfViewCtrl.getCurrentPage();
            if (currentPageIndex > 2) {
                mPrePageItem.setEnabled(true);
            } else if (currentPageIndex == 2) {
                Rect preLeftPageRect = mPdfViewCtrl.getPageViewRect(currentPageIndex - 2);
                Rect preRightPageRect = mPdfViewCtrl.getPageViewRect(currentPageIndex - 1);
                int height = Math.max(preLeftPageRect.height(), preRightPageRect.height());
                if (height > mParent.getHeight() / 2) {
                    mPrePageItem.setEnabled(true);
                } else {
                    mPrePageItem.setEnabled(false);
                }
            } else {
                mPrePageItem.setEnabled(false);
            }
        } else if (mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER){
            int currentPageIndex = mPdfViewCtrl.getCurrentPage();
            if (currentPageIndex > 1) {
                mPrePageItem.setEnabled(true);
            } else if (currentPageIndex == 1) {
                Rect preRightPageRect = mPdfViewCtrl.getPageViewRect(currentPageIndex - 1);
                if (preRightPageRect.height() > mParent.getHeight() / 2) {
                    mPrePageItem.setEnabled(true);
                } else {
                    mPrePageItem.setEnabled(false);
                }
            } else {
                mPrePageItem.setEnabled(false);
            }
        } else {
            if (mPdfViewCtrl.getCurrentPage() == 0) {
                mPrePageItem.setEnabled(false);
            } else {
                mPrePageItem.setEnabled(true);
            }
        }
    }

    private void resetNextPageItem() {
        if (mPdfViewCtrl.isContinuous() && (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING
                || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER)) {

            if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER && mPdfViewCtrl.getCurrentPage() == 0){

                if (mPdfViewCtrl.getPageCount() >= 4) {
                    mNextPageItem.setEnabled(true);
                } else if (mPdfViewCtrl.getPageCount() >= 2){
                    Rect nextLeftPageRect = mPdfViewCtrl.getPageViewRect(mPdfViewCtrl.getCurrentPage() + 1);
                    Rect nextRightPageRect = mPdfViewCtrl.getPageViewRect(mPdfViewCtrl.getCurrentPage() + 2);
                    int height = Math.max(nextLeftPageRect.height(), nextRightPageRect.height());
                    if (height > mParent.getHeight() / 2) {
                        mNextPageItem.setEnabled(true);
                    } else {
                        mNextPageItem.setEnabled(false);
                    }
                } else {
                    mNextPageItem.setEnabled(false);
                }
            } else {
                if (mPdfViewCtrl.getCurrentPage() + 4 >= mPdfViewCtrl.getPageCount()) {
                    Rect nextLeftPageRect = mPdfViewCtrl.getPageViewRect(mPdfViewCtrl.getCurrentPage() + 2);
                    Rect nextRightPageRect = mPdfViewCtrl.getPageViewRect(mPdfViewCtrl.getCurrentPage() + 3);
                    int height = Math.max(nextLeftPageRect.height(), nextRightPageRect.height());
                    if (height > mParent.getHeight() / 2) {
                        mNextPageItem.setEnabled(true);
                    } else {
                        mNextPageItem.setEnabled(false);
                    }
                } else {
                    mNextPageItem.setEnabled(true);
                }
            }
        } else if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING
                || (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER && mPdfViewCtrl.getPageCount() > 2)) {
            if (mPdfViewCtrl.getCurrentPage() + 2 >= mPdfViewCtrl.getPageCount()) {
                mNextPageItem.setEnabled(false);
            } else {
                mNextPageItem.setEnabled(true);
            }
        } else {
            if (mPdfViewCtrl.getCurrentPage() + 1 == mPdfViewCtrl.getPageCount()) {
                mNextPageItem.setEnabled(false);
            } else {
                mNextPageItem.setEnabled(true);
            }
        }
    }

    private void initItemsOnClickListener() {
        mPrePageItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING
                        || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER) {

                    if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER && mPdfViewCtrl.getCurrentPage() == 1) {
                        int pageIndex = 0;
                        if (mPdfViewCtrl.isContinuous()) {
                            float devOffsetX = 0;
                            Rect pageRect = mPdfViewCtrl.getPageViewRect(pageIndex);
                            float devOffsetY = (float) (mParent.getHeight() - pageRect.height()) / 2;
                            mPdfViewCtrl.gotoPage(pageIndex, devOffsetX, -devOffsetY);
                        } else {
                            mPdfViewCtrl.gotoPage(pageIndex);
                        }
                    } else if (mPdfViewCtrl.getCurrentPage() - 2 >= 0) {
                        int pageIndex = mPdfViewCtrl.getCurrentPage() - 2;
                        if (mPdfViewCtrl.isContinuous()) {
                            gotoPage(pageIndex);
                        } else {
                            mPdfViewCtrl.gotoPage(pageIndex);
                        }
                    }
                } else if ((mPdfViewCtrl.getCurrentPage() - 1) >= 0) {
                    mPdfViewCtrl.gotoPrevPage();
                }
                ((UIExtensionsManager)mUiExtensionsManager).resetHideToolbarsTimer();
            }
        });
        mNextPageItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING
                        || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER) {

                    if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER &&
                            mPdfViewCtrl.getCurrentPage() == 0 &&
                            mPdfViewCtrl.getPageCount() >= 2) {

                        int pageIndex = mPdfViewCtrl.getCurrentPage() + 1;
                        if (mPdfViewCtrl.isContinuous()) {
                            gotoPage(pageIndex);
                        } else {
                            mPdfViewCtrl.gotoPage(pageIndex);
                        }
                    } else if (mPdfViewCtrl.getCurrentPage() + 2 < mPdfViewCtrl.getPageCount()) {
                        int pageIndex = mPdfViewCtrl.getCurrentPage() + 2;
                        if (mPdfViewCtrl.isContinuous()) {
                           gotoPage(pageIndex);
                        } else {
                            mPdfViewCtrl.gotoPage(pageIndex);
                        }
                    }
                } else if ((mPdfViewCtrl.getCurrentPage() + 1) < mPdfViewCtrl.getPageCount()) {
                    mPdfViewCtrl.gotoNextPage();
                }
                ((UIExtensionsManager)mUiExtensionsManager).resetHideToolbarsTimer();
            }
        });
        mBackItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFloatWindowController.getFloatWindow() != null) {
                    ((PanZoomView) mFloatWindowController.getFloatWindow()).onBack();
                    mFloatWindowController.stopFloatWindowServer();
                    FloatWindowUtil.getInstance().setParent(null);
                    mIsPanZoomModule = false;

                    ((UIExtensionsManager)mUiExtensionsManager).changeState(ReadStateConfig.STATE_NORMAL);
                }
            }
        });

        mSeekBarItem.setProgress((int)(mScale - 1 + 0.5f));
        mSeekBarItem.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 0 && progress < 12 && fromUser) {
                    mScale = progress + 1;
                    mPdfViewCtrl.setZoom(mScale);
                    ((UIExtensionsManager)mUiExtensionsManager).resetHideToolbarsTimer();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mPZBottomBar.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    private void gotoPage(int pageIndex) {
        Rect pageLeftRect = mPdfViewCtrl.getPageViewRect(pageIndex);
        Rect pageRightRect = mPdfViewCtrl.getPageViewRect(pageIndex + 1);
        int height = Math.max(pageLeftRect.height(), pageRightRect.height());

        float devOffsetX = 0;
        float devOffsetY = (float) (mParent.getHeight() - height) / 2;
        mPdfViewCtrl.gotoPage(pageIndex, devOffsetX, -devOffsetY);
    }

    private PDFViewCtrl.IDocEventListener docEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
            initPanZoomBar();
        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != Constants.e_ErrSuccess) return;
            if (!mPdfViewCtrl.isDynamicXFA()) {
                addBar();
            }
            initValue();
            if (!initMLBarValue()) return;
            registerMLListener();
        }

        @Override
        public void onDocWillClose(PDFDoc document) {

        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            removeBar();
            unRegisterMLListener();
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private void initValue() {
        // set value with the value of automatic pan zoom setting from system.
        mIsPanZoomModule = false;
    }

    private boolean initMLBarValue() {
        mSettingBar = ((UIExtensionsManager)mUiExtensionsManager).getMainFrame().getSettingBar();
        if (mPdfViewCtrl.isDynamicXFA()) {
            mSettingBar.enableBar(IMultiLineBar.TYPE_PANZOOM, false);
            return false;
        }
        mSettingBar.setProperty(IMultiLineBar.TYPE_PANZOOM, mIsPanZoomModule);
        if (mIsPanZoomModule) {
            ((UIExtensionsManager)mUiExtensionsManager).changeState(ReadStateConfig.STATE_PANZOOM);
        }
        return true;
    }

    private void registerMLListener() {
        mSettingBar.registerListener(mPanZoomChangeListener);
    }

    private void unRegisterMLListener() {
        mSettingBar.unRegisterListener(mPanZoomChangeListener);
    }

    private void applyPermission(Context context) {
        String property = FloatWindowUtil.getSystemProperty("ro.build.display.id");
        if (!TextUtils.isEmpty(property) && (property.contains("flyme") || property.toLowerCase().contains("flyme"))){
            try {
                Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
//              intent.setClassName("com.meizu.safe", "com.meizu.safe.security.AppSecActivity");
                intent.putExtra("packageName", context.getPackageName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }catch (Exception e){
                gotoOverlayermissionActivity(context);
            }
        } else {
            gotoOverlayermissionActivity(context);
        }
    }

    private void gotoOverlayermissionActivity(Context context){
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private IMultiLineBar.IValueChangeListener mPanZoomChangeListener = new IMultiLineBar.IValueChangeListener() {

        @Override
        public void onValueChanged(int type, Object value) {
            if (type == IMultiLineBar.TYPE_PANZOOM) {
                onStatusChanged();

                mIsPanZoomModule = (boolean) value;
                FloatWindowUtil.getInstance().setContext(mContext);
                FloatWindowUtil.getInstance().setParent(mParent);
                FloatWindowUtil.getInstance().setPdfViewCtrl(mPdfViewCtrl);
                final Activity activity = ((UIExtensionsManager) mUiExtensionsManager).getAttachedActivity();
                if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(activity)) {
                    String message = mContext.getApplicationContext().getString(R.string.not_window_permissions_tips);
                    String title = "";
                    Dialog dialog = new AlertDialog.Builder(activity).setCancelable(true).setTitle(title)
                            .setMessage(message)
                            .setPositiveButton(mContext.getApplicationContext().getString(R.string.fx_turn_on),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            applyPermission(activity);
                                            FloatWindowUtil.getInstance().setParent(null);
                                            dialog.dismiss();
                                        }
                                    }).setNegativeButton(mContext.getApplicationContext().getString(R.string.fx_turn_later),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            FloatWindowUtil.getInstance().setParent(null);
                                            dialog.dismiss();
                                        }
                                    }).create();
                    dialog.show();

                    mIsPanZoomModule = false;
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).changeState(ReadStateConfig.STATE_NORMAL);
                    return;
                }

                mFloatWindowController.startFloatWindowServer();
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).changeState(ReadStateConfig.STATE_PANZOOM);
                IMainFrame mainFrame = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getMainFrame();
                mainFrame.hideSettingBar();
                if (!mainFrame.isToolbarsVisible()){
                    mainFrame.showToolbars();
                }
                if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() != null) {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
            }
        }

        @Override
        public void onDismiss() {

        }

        @Override
        public int getType() {
            return IMultiLineBar.TYPE_PANZOOM;
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return exit();
        }

        return false;
    }

    public boolean exit() {
        if (mFloatWindowController.getFloatWindow() != null) {
            boolean ret = ((PanZoomView)mFloatWindowController.getFloatWindow()).exit();
            mFloatWindowController.stopFloatWindowServer();
            FloatWindowUtil.getInstance().setParent(null);
            mIsPanZoomModule = false;
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).changeState(ReadStateConfig.STATE_NORMAL);
            return ret;
        }
        return false;
    }

    private void reDrawPanZoomView(int pageIndex) {
        if (mFloatWindowController.getFloatWindow() != null) {
            PanZoomView panZoomView = (PanZoomView) mFloatWindowController.getFloatWindow();
            if (panZoomView.isPanZoomRectMoving()) return;
            panZoomView.reDrawPanZoomView(pageIndex);
        }
    }

    private PDFViewCtrl.IPageEventListener mPageListener = new PDFViewCtrl.IPageEventListener() {
        @Override
        public void onPageVisible(int index) {
        }

        @Override
        public void onPageInvisible(int index) {

        }

        @Override
        public void onPageChanged(int oldPageIndex, int curPageIndex) {
            resetPrePageItem();
            resetNextPageItem();
            reDrawPanZoomView(curPageIndex);
        }

        @Override
        public void onPageJumped() {
            resetNextPageItem();
            resetPrePageItem();
            reDrawPanZoomView(mPdfViewCtrl.getCurrentPage());
        }

        @Override
        public void onPagesWillRemove(int[] pageIndexes) {

        }

        @Override
        public void onPageWillMove(int index, int dstIndex) {

        }

        @Override
        public void onPagesWillRotate(int[] pageIndexes, int rotation) {

        }

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {

        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {

        }

        @Override
        public void onPagesRotated(boolean success, int[] pageIndexes, int rotation) {

        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] pageRanges) {

        }

        @Override
        public void onPagesWillInsert(int dstIndex, int[] pageRanges) {

        }
    };

    private AnnotEventListener mAnnotEventListener = new AnnotEventListener() {
        @Override
        public void onAnnotAdded(PDFPage page, Annot annot) {
            try {
                PanZoomView panZoomView = (PanZoomView) mFloatWindowController.getFloatWindow();
                if (null != panZoomView && page.getIndex() == panZoomView.getCurPageIndex()){
                    reDrawPanZoomView(page.getIndex());
                }
            }catch (PDFException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotWillDelete(PDFPage page, Annot annot) {

        }

        @Override
        public void onAnnotDeleted(PDFPage page, Annot annot) {
            try {
                PanZoomView panZoomView = (PanZoomView) mFloatWindowController.getFloatWindow();
                if (null != panZoomView && page.getIndex() == panZoomView.getCurPageIndex()){
                    reDrawPanZoomView(page.getIndex());
                }
            }catch (PDFException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotModified(PDFPage page, Annot annot) {
            try {
                PanZoomView panZoomView = (PanZoomView) mFloatWindowController.getFloatWindow();
                if (null != panZoomView && page.getIndex() == panZoomView.getCurPageIndex()){
                    reDrawPanZoomView(page.getIndex());
                }
            }catch (PDFException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {

        }
    };

    private IRedactionEventListener mRedactionEventListener = new IRedactionEventListener() {
        @Override
        public void onAnnotWillApply(PDFPage page, Annot annot) {

        }

        @Override
        public void onAnnotApplied(PDFPage page, Annot annot) {
            try {
                reDrawPanZoomView(page.getIndex());
            }catch (PDFException e){
                e.printStackTrace();
            }
        }
    };

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {
        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            if (mFloatWindowController.getFloatWindow() != null) {
                PanZoomView panZoomView = (PanZoomView) mFloatWindowController.getFloatWindow();
                if (panZoomView.isPanZoomRectMoving()) return;
                panZoomView.reCalculatePanZoomRect(pageIndex);
            }
        }
    };

    private PDFViewCtrl.IScaleGestureEventListener mScaleListener = new PDFViewCtrl.IScaleGestureEventListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mScale = mPdfViewCtrl.getZoom();
            mSeekBarItem.setProgress((int) (mScale - 1 + 0.5f));

            if (mPdfViewCtrl.isContinuous()){

                if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING) {
                    if (mPdfViewCtrl.getCurrentPage() == 2)
                        resetPrePageItem();

                    if (mPdfViewCtrl.getPageCount() > 2 &&
                            mPdfViewCtrl.getCurrentPage() + 2 < mPdfViewCtrl.getPageCount() &&
                            mPdfViewCtrl.getCurrentPage() + 4 >= mPdfViewCtrl.getPageCount())
                        resetNextPageItem();
                } else if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER && mPdfViewCtrl.getCurrentPage() == 1) {
                    if (mPdfViewCtrl.getCurrentPage() == 1)
                        resetPrePageItem();

                    if (mPdfViewCtrl.getPageCount() >= 2 && mPdfViewCtrl.getCurrentPage() + 4 >= mPdfViewCtrl.getPageCount()) {
                        if (mPdfViewCtrl.getCurrentPage() == 0 || mPdfViewCtrl.getCurrentPage() + 2 < mPdfViewCtrl.getPageCount()) {
                            resetNextPageItem();
                        }
                    }

                }
            }
        }
    };

    private HomeKeyReceiver.IHomeKeyEventListener mHomeKeyEventListener = new HomeKeyReceiver.IHomeKeyEventListener() {
        @Override
        public void onHomeKeyPressed() {
            exit();
        }
    };

    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        if (mIsPanZoomModule) {
            PanZoomView panZoomView = (PanZoomView) mFloatWindowController.getFloatWindow();
            if (panZoomView != null) {
                panZoomView.onTouchEvent(pageIndex, motionEvent);
            }
        }
        return false;
    }

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (mFloatWindowController.getFloatWindow() != null) {
                if (mIsConfigurationChanged && (newWidth != oldWidth || newHeight != oldHeight)) {
                    mIsConfigurationChanged = false;
                    PanZoomView panZoomView = (PanZoomView) mFloatWindowController.getFloatWindow();
                    panZoomView.onLayoutChange(v, newWidth, newHeight, oldWidth, oldHeight);
                }
            }
        }
    };

    private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            mIsConfigurationChanged = true;
        }
    };

}
