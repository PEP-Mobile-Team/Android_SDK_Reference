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
package com.foxit.uiextensions.annots;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.utils.AppDisplay;

/**
 * Class that defines common behaviour for annotation creation. The annotation handler is mainly responsible for creating a
 * new annotation on the PDF page.
 */
public abstract class AbstractToolHandler implements ToolHandler, PropertyBar.PropertyChangeListener {
	protected Context mContext;

	protected PropertyBar mPropertyBar;
	protected String mToolName;

	protected PropertyCircleItem    mPropertyBtn;
	protected boolean				mIsContinuousCreate;
    
	protected int					mColor;
	protected int					mCustomColor;
	protected int					mOpacity;
	protected float					mThickness;

	protected PDFViewCtrl mPdfViewCtrl;
	protected UIExtensionsManager  mUiExtensionsManager;

	public AbstractToolHandler(Context context, PDFViewCtrl pdfViewCtrl, String name, String propKey) {
		mContext = context;
		mPdfViewCtrl = pdfViewCtrl;
		mToolName = name;
		mColor = Color.RED;
		mCustomColor = Color.RED;
		mOpacity = 100;
		mThickness = 5.0f;
		
		mUiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
		mPropertyBar = mUiExtensionsManager.getMainFrame().getPropertyBar();
	}


	public void setPropertyBar(PropertyBar propertyBar) {
		mPropertyBar = propertyBar;
	}

	public PropertyBar getPropertyBar() {
		return mPropertyBar;
	}

	protected void removeToolButton() {
	}
	
	public void updateToolButtonStatus() {

	}

	public int getCustomColor() {
		return mCustomColor;
	}

	public void setCustomColor(int color) {
		mCustomColor = color;
	}

	public int getColor() {
		return mColor;
	}

	public void setColor(int color) {
		if (mColor == color) return;
		mColor = color;
	}

	public int getOpacity() {
		return mOpacity;
	}

	public void setOpacity(int opacity) {
		if (mOpacity == opacity) return;
		mOpacity = opacity;
	}

	public float getThickness() {
		return mThickness;
	}

	public void setThickness(float thickness) {
		if (mThickness == thickness) return;
		mThickness = thickness;
	}

	public String getFontName() {
		return null;
	}

	public void setFontName(String name) {
	}

	public float getFontSize() {
		return 0;
	}

	public void setFontSize(float size) {
	}

	private ColorChangeListener mColorChangeListener = null;

	public void setColorChangeListener(ColorChangeListener listener) {
		mColorChangeListener = listener;
	}

	public interface ColorChangeListener {
		void onColorChange(int color);
	}

	@Override
    public void onValueChanged(long property, int value) {
		if (property == PropertyBar.PROPERTY_COLOR) {
			setColor(value);
			if (mColorChangeListener != null) {
				mColorChangeListener.onColorChange(value);
			}
		} else if (property == PropertyBar.PROPERTY_SELF_COLOR) {
			setCustomColor(value);
			setColor(value);
			if (mColorChangeListener != null) {
				mColorChangeListener.onColorChange(value);
			}
		} else if (property == PropertyBar.PROPERTY_OPACITY) {
			setOpacity(value);
		}
	}
	
	@Override
    public void onValueChanged(long property, float value) {
		if (property == PropertyBar.PROPERTY_LINEWIDTH) {
			setThickness(value);
		}
	}
	
	@Override
    public void onValueChanged(long property, String value) {
	}

	@Override
	public String getType() {
		return mToolName;
	}

	@Override
	public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
		return false;
	}
	

	public boolean onPrepareOptionsMenu() {
		if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
			return false;
		}
		return true;
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
				return true;
			}
		}
		return false;
	}
	
	public void onConfigurationChanged(Configuration newConfig) {

	}
	
	public void onStatusChanged(int oldState, int newState) {
		updateToolButtonStatus();
	}

	protected void showPropertyBar(long curProperty) {
		setPropertyBarProperties(mPropertyBar);
		mPropertyBar.setPropertyChangeListener(this);
		mPropertyBar.reset(getSupportedProperties());
		Rect rect = new Rect();
        mPropertyBtn.getContentView().getGlobalVisibleRect(rect);
		mPropertyBar.show(new RectF(rect), true);
	}
	
	protected void hidePropertyBar() {
		mPropertyBar.setPropertyChangeListener(null);
		if (mPropertyBar.isShowing())
			mPropertyBar.dismiss();
	}
	
	protected void setPropertyBarProperties(PropertyBar propertyBar) {
		propertyBar.setProperty(PropertyBar.PROPERTY_COLOR, getColor());
		propertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, getOpacity());
		propertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, getThickness());
		if (AppDisplay.getInstance(mContext).isPad()) {
			propertyBar.setArrowVisible(true);
		} else {
			propertyBar.setArrowVisible(false);
		}
	}

	@Override
	public boolean isContinueAddAnnot() {
		return mIsContinuousCreate;
	}

	@Override
	public void setContinueAddAnnot(boolean continueAddAnnot) {
		mIsContinuousCreate = continueAddAnnot;
	}

	protected abstract void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint);
	public abstract long getSupportedProperties();
}
