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
package com.foxit.uiextensions.annots.polygon;

import android.content.Context;
import android.graphics.Canvas;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.config.modules.annotations.AnnotationsConfig;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;

public class PolygonModule implements Module, PropertyBar.PropertyChangeListener {

    private PolygonAnnotHandler mAnnotHandler;
    private PolygonToolHandler mToolHandler;

    private PolygonCloudToolHandler mCloudToolHandler;

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public PolygonModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public void onValueChanged(long property, int value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            if (mToolHandler != null && uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mToolHandler.changeCurrentColor(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onColorValueChanged(value);
            } else if (mCloudToolHandler != null && uiExtensionsManager.getCurrentToolHandler() == mCloudToolHandler) { // polygon cloud
                mCloudToolHandler.changeCurrentColor(value);
            }
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            if (mToolHandler != null && uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mToolHandler.changeCurrentOpacity(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onOpacityValueChanged(value);
            } else if (mCloudToolHandler != null && uiExtensionsManager.getCurrentToolHandler() == mCloudToolHandler) { // polygon cloud
                mCloudToolHandler.changeCurrentOpacity(value);
            }
        }
    }

    @Override
    public void onValueChanged(long property, float value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
        if (property == PropertyBar.PROPERTY_LINEWIDTH) {
            if (mToolHandler != null && uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mToolHandler.changeCurrentThickness(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onLineWidthValueChanged(value);
            } else if (mCloudToolHandler != null && uiExtensionsManager.getCurrentToolHandler() == mCloudToolHandler) { // polygon cloud
                mCloudToolHandler.changeCurrentThickness(value);
            }
        }
    }

    @Override
    public void onValueChanged(long property, String value) {

    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_POLYGON;
    }

    @Override
    public boolean loadModule() {
        mAnnotHandler = new PolygonAnnotHandler(mContext, mPdfViewCtrl);
        mAnnotHandler.setPropertyChangeListener(this);
        mAnnotHandler.setAnnotMenu(new AnnotMenuImpl(mContext, mPdfViewCtrl));
        mAnnotHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl));

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            Config config = ((UIExtensionsManager) mUiExtensionsManager).getConfig();
            AnnotationsConfig annotConfig = config.modules.getAnnotConfig();

            //polygon
            if (annotConfig.isLoadDrawPolygon()) {
                mToolHandler = new PolygonToolHandler(mContext, mPdfViewCtrl);
                mToolHandler.setPropertyChangeListener(this);
                ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);
                mToolHandler.init();
            }

            //polygon cloud
            if (annotConfig.isLoadDrawCloud()) {
                mCloudToolHandler = new PolygonCloudToolHandler(mContext, mPdfViewCtrl);
                mCloudToolHandler.setPropertyChangeListener(this);
                ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mCloudToolHandler);
                mCloudToolHandler.init();
            }

            ((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mAnnotHandler.removePropertyListener();

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {

            //polygon
            if (mToolHandler != null) {
                mToolHandler.removePropertyListener();
                ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);
            }

            //polygon cloud
            if (mCloudToolHandler != null) {
                mCloudToolHandler.removePropertyListener();
                ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mCloudToolHandler);
            }

            ((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
        }

        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        return true;
    }

    public ToolHandler getToolHandler(int tag) {
        if (tag == MoreTools.MT_TYPE_CLOUD) {
            return mCloudToolHandler;
        }
        return mToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
        @Override
        public void onWillRecover() {
            if (mAnnotHandler.getAnnotMenu() != null && mAnnotHandler.getAnnotMenu().isShowing()) {
                mAnnotHandler.getAnnotMenu().dismiss();
            }

            if (mAnnotHandler.getPropertyBar() != null && mAnnotHandler.getPropertyBar().isShowing()) {
                mAnnotHandler.getPropertyBar().dismiss();
            }
        }

        @Override
        public void onRecovered() {
        }
    };
}
