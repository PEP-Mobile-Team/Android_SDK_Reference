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
package com.foxit.uiextensions.annots.note;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;

import java.util.ArrayList;

public class NoteModule implements Module, PropertyBar.PropertyChangeListener {
    private Context mContext;
    private AppDisplay mDisplay;
    private PDFViewCtrl mPdfViewCtrl;
    private NoteAnnotHandler mAnnotHandler;
    private NoteToolHandler mToolHandler;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private ViewGroup mParentView;

    private int mCurrentColor;
    private int mCurrentOpacity;
    private int mCurrentIconType;

    private ArrayList<BitmapDrawable> mBitmapDrawables;

    private Paint mPaint;

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    public NoteModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mParentView = parent;
        mDisplay = new AppDisplay(context);
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return MODULE_NAME_NOTE;
    }

    @Override
    public boolean loadModule() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.STROKE);
        mPaint.setDither(true);

        mAnnotHandler = new NoteAnnotHandler(mContext, mParentView, mPdfViewCtrl, this);
        mToolHandler = new NoteToolHandler(mContext, mPdfViewCtrl);
        mToolHandler.setPropertyChangeListener(this);
        mAnnotHandler.setToolHandler(mToolHandler);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler( mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }

        initVariable();

        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        return true;
    }

    private void initVariable() {
        mCurrentColor = PropertyBar.PB_COLORS_NOTE[0];
        mCurrentOpacity = 100;
        mCurrentIconType = NoteConstants.TA_ICON_COMMENT;
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            com.foxit.uiextensions.config.Config config = ((UIExtensionsManager) mUiExtensionsManager).getConfig();
            mCurrentColor = config.uiSettings.annotations.note.color;
            mCurrentOpacity = (int) (config.uiSettings.annotations.note.opacity * 100);

            String typeName = config.uiSettings.annotations.note.icon;
            mCurrentIconType = NoteUtil.getIconByIconName(typeName);
        }

        mToolHandler.setColor(mCurrentColor);
        mToolHandler.setOpacity(mCurrentOpacity);
        mToolHandler.setIconType(mCurrentIconType);

        Rect rect = new Rect(0, 0, dp2px(32), dp2px(32));
        mBitmapDrawables = new ArrayList<BitmapDrawable>();
        for (int i = 1; i < NoteConstants.TA_ICON_COUNT + 1; i++) {
            Bitmap mBitmap = Bitmap.createBitmap(dp2px(32), dp2px(32), Config.RGB_565);
            Canvas canvas = new Canvas(mBitmap);
            @SuppressWarnings("deprecation")
            BitmapDrawable bd = new BitmapDrawable(mBitmap);
            mPaint.setStyle(Style.FILL);
            mPaint.setColor(Color.YELLOW);
            String iconName = NoteUtil.getIconNameByType(i);
            canvas.drawPath(NoteUtil.getPathStringByType(iconName, AppDmUtil.rectToRectF(rect)), mPaint);
            mPaint.setStyle(Style.STROKE);
            mPaint.setStrokeWidth(dp2px(1));
            mPaint.setARGB(255, (int) (255 * 0.36f), (int) (255 * 0.36f), (int) (255 * 0.64f));
            canvas.drawPath(NoteUtil.getPathStringByType(iconName, AppDmUtil.rectToRectF(rect)), mPaint);
            canvas.save();
            canvas.restore();
            mBitmapDrawables.add(bd);
        }

    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
        }
        mToolHandler.removePropertyBarListener();
        return true;
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    @Override
    public void onValueChanged(long property, int value) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;
        AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentColor = value;
                mToolHandler.setColor(value);
            }
            if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onColorValueChanged(value);
            }
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentOpacity = value;
                mToolHandler.setOpacity(value);
            }
            if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onOpacityValueChanged(value);
            }
        } else if (property == PropertyBar.PROPERTY_ANNOT_TYPE) {
            if (uiExtensionsManager.getCurrentToolHandler() == mToolHandler) {
                mCurrentIconType = value;
                mToolHandler.setIconType(value);
            }
            if (currentAnnotHandler == mAnnotHandler) {
                String iconName = NoteUtil.getIconNameByType(value);
                mAnnotHandler.onIconTypeChanged(iconName);
            }
        }
    }

    @Override
    public void onValueChanged(long property, float value) {
    }

    @Override
    public void onValueChanged(long property, String iconName) {
    }

    private int dp2px(int dip) {
        return mDisplay.dp2px(dip);
    }

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
