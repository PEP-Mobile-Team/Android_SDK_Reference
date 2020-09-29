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
package com.foxit.uiextensions.annots.freetext.typewriter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.config.uisettings.annotations.annots.TypewriterConfig;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.OnPageEventListener;


public class TypewriterModule implements Module, PropertyBar.PropertyChangeListener {

    private TypewriterToolHandler mToolHandler;
    private TypewriterAnnotHandler mAnnotHandler;

    private int mCurrentColor;
    private int mCurrentOpacity;
    private String mCurrentFontName;
    private float mCurrentFontSize;

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public TypewriterModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    public ToolHandler getToolHandler() {
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

    @Override
    public String getName() {
        return Module.MODULE_NAME_TYPEWRITER;
    }

    @Override
    public boolean loadModule() {
        mToolHandler = new TypewriterToolHandler(mContext, mPdfViewCtrl);
        mToolHandler.setPropertyChangeListener(this);

        mAnnotHandler = new TypewriterAnnotHandler(mContext, mPdfViewCtrl);
        mAnnotHandler.setPropertyChangeListener(this);
        mAnnotHandler.setAnnotMenu(new AnnotMenuImpl(mContext, mPdfViewCtrl));
        mAnnotHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl));

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);
        initCurrentValue();
        return true;
    }

    @Override
    public boolean unloadModule() {
        mAnnotHandler.removePropertyBarListener();
        mToolHandler.removeProbarListener();

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
        }
        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        return true;
    }

    private void initCurrentValue() {
        if (mCurrentColor == 0) mCurrentColor = PropertyBar.PB_COLORS_TYPEWRITER[10];
        if (mCurrentOpacity == 0) mCurrentOpacity = 100;
        if (mCurrentFontName == null) mCurrentFontName = "Courier";
        if (mCurrentFontSize == 0) mCurrentFontSize = 24;

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            TypewriterConfig config = ((UIExtensionsManager) mUiExtensionsManager).getConfig()
                    .uiSettings.annotations.typewriter;
            mCurrentColor = config.textColor;
            mCurrentOpacity = (int) (config.opacity * 100);
            String[] fontNames = new String[]{mContext.getApplicationContext().getString(R.string.fx_font_courier),
                    mContext.getApplicationContext().getString(R.string.fx_font_helvetica),
                    mContext.getApplicationContext().getString(R.string.fx_font_times)};
            for (String fontName: fontNames){
                if (fontName.equals(config.textFace)){
                    mCurrentFontName = config.textFace;
                    break;
                }
            }
            mCurrentFontSize = config.textSize;
        }

        mToolHandler.onColorValueChanged(mCurrentColor);
        mToolHandler.onOpacityValueChanged(mCurrentOpacity);
        mToolHandler.onFontValueChanged(mCurrentFontName);
        mToolHandler.onFontSizeValueChanged(mCurrentFontSize);
    }

    @Override
    public void onValueChanged(long property, int value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentColor = value;
                mToolHandler.onColorValueChanged(value);
            }
            if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onColorValueChanged(value);
            }
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentOpacity = value;
                mToolHandler.onOpacityValueChanged(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onOpacityValueChanged(value);
            }
        }
    }

    @Override
    public void onValueChanged(long property, float value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
        if (property == PropertyBar.PROPERTY_FONTSIZE) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentFontSize = value;
                mToolHandler.onFontSizeValueChanged(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onFontSizeValueChanged(value);
            }
        }
    }

    @Override
    public void onValueChanged(long property, String value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
        if (property == PropertyBar.PROPERTY_FONTNAME) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentFontName = value;
                mToolHandler.onFontValueChanged(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onFontValueChanged(value);
            }
        }
    }

    private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener() {
        @Override
        public void onPageChanged(int oldPageIndex, int curPageIndex) {
            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
            AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
            if (uiExtensionsManager.getCurrentToolHandler() != null && uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                if (mToolHandler.mLastPageIndex != -1 && mToolHandler.mLastPageIndex != curPageIndex && curPageIndex != -1) {
                    if ((mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING  && curPageIndex % 2 == 0)
                            || (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER  && curPageIndex % 2 == 1)) {
                        return;
                    }
                    uiExtensionsManager.setCurrentToolHandler(null);
                }
            }
            Annot curAnnot = ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().getCurrentAnnot();
            if ( curAnnot != null && currentAnnotHandler == mAnnotHandler) {
                try {
                    if (curAnnot.getPage().getIndex() == curPageIndex) {
                        if (mPdfViewCtrl.isPageVisible(curPageIndex)) {
                            RectF viewRect = AppUtil.toRectF(curAnnot.getRect());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, curPageIndex);
                            viewRect.inset(-40, -40);
                            mPdfViewCtrl.refresh(curPageIndex, AppDmUtil.rectFToRect(viewRect));
                        }
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
