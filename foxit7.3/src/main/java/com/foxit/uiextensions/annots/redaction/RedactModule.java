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
package com.foxit.uiextensions.annots.redaction;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;

public class RedactModule implements Module, PropertyBar.PropertyChangeListener {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private RedactAnnotHandler mAnnotHandler;
    private RedactToolHandler mToolHandler;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public RedactModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return MODULE_NAME_REDACT;
    }

    @Override
    public boolean loadModule() {
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        mToolHandler = new RedactToolHandler(mContext, mPdfViewCtrl);
        mAnnotHandler = new RedactAnnotHandler(mContext, mPdfViewCtrl, this);
        mAnnotHandler.setToolHandler(mToolHandler);
        mAnnotHandler.setPropertyChangeListener(this);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigurationChangedListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
        }
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        mAnnotHandler.removePropertyBarListener();
        return true;
    }

    @Override
    public void onValueChanged(long property, int value) {
        AnnotHandler currentAnnotHandler = ((UIExtensionsManager) mUiExtensionsManager).getCurrentAnnotHandler();
        if (currentAnnotHandler == mAnnotHandler) {
            if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
                mAnnotHandler.onFontColorValueChanged(value);
            }
        }
    }

    @Override
    public void onValueChanged(long property, float value) {
        AnnotHandler currentAnnotHandler = ((UIExtensionsManager) mUiExtensionsManager).getCurrentAnnotHandler();
        if (currentAnnotHandler == mAnnotHandler && property == PropertyBar.PROPERTY_FONTSIZE) {
            mAnnotHandler.onFontSizeValueChanged(value);
        }
    }

    @Override
    public void onValueChanged(long property, String value) {
        AnnotHandler currentAnnotHandler = ((UIExtensionsManager) mUiExtensionsManager).getCurrentAnnotHandler();
        if (currentAnnotHandler == mAnnotHandler) {
            if (property == PropertyBar.PROPERTY_FONTNAME) {
                mAnnotHandler.onFontValueChanged(value);
            } else if (property == PropertyBar.PROPERTY_EDIT_TEXT) {
                mAnnotHandler.onOverlayTextChanged(value);
            }
        }
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            mAnnotHandler.onConfigurationChanged(newConfig);
        }
    };

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

}
