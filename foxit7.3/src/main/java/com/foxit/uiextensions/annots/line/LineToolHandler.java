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
package com.foxit.uiextensions.annots.line;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.common.UIAnnotFrame;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.PropertyCircleItemImp;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

public class LineToolHandler extends AbstractToolHandler {

	protected LineRealAnnotHandler mAnnotHandler;
	protected LineUtil mUtil;
	protected String mIntent;

	protected boolean				mTouchCaptured = false;
	protected int					mCapturedPage = -1;
	protected PointF mStartPt = new PointF();
	protected PointF mStopPt = new PointF();
	protected Paint mPaint;
	private TextPaint mTextPaint;
	private int scaleFromUnitIndex;
	private int scaleToUnitIndex;
	private int scaleFromValue = 1;
	private int scaleToValue = 1;

	private PointF dStart,dEnd;

	private PropertyCircleItem mPropertyItem;
	private IBaseItem mOKItem;
	private IBaseItem mContinuousCreateItem;

	public LineToolHandler(Context context, PDFViewCtrl pdfViewCtrl, LineUtil util, String intent) {
		super(context, pdfViewCtrl, util.getToolName(intent), util.getToolPropertyKey(intent));
		if (intent.equals(LineConstants.INTENT_LINE_ARROW)) {
			mColor = PropertyBar.PB_COLORS_ARROW[6];
		} else if(LineConstants.INTENT_LINE_DIMENSION.equals(intent)) {
			mColor = PropertyBar.PB_COLORS_ARROW[6];
			mThickness = 3.0f;
			mTextPaint = new TextPaint();
			mTextPaint.setStyle(Style.FILL);
			mTextPaint.setTextSize(40f);
			dStart = new PointF();
			dEnd = new PointF();
		} else {
			mColor = PropertyBar.PB_COLORS_LINE[6];
		}

		mUtil = util;
		mIntent = intent;
		scaleFromUnitIndex = 0;//default:0=="pt"

		mPaint = new Paint();
		mPaint.setStyle(Style.STROKE);
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);

		mUiExtensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
			@Override
			public void onMTClick(int type) {
				mUiExtensionsManager.setCurrentToolHandler(LineToolHandler.this);
				mUiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
			}

			@Override
			public int getType() {
				return getToolType();
			}
		});

	}

	protected void initUiElements() {
	}
	
	protected void uninitUiElements() {
		removeToolButton();
	}
	
	protected String getIntent() {
		return mIntent;
	}

	@Override
	public void onActivate() {
		mCapturedPage = -1;
		resetPropertyBar();
		resetAnnotBar();
	}

	private void resetPropertyBar() {
		if (mIntent.equals(LineConstants.INTENT_LINE_ARROW) || mIntent.equals(LineConstants.INTENT_LINE_DIMENSION)) {
			int[] colors = new int[PropertyBar.PB_COLORS_ARROW.length];
			System.arraycopy(PropertyBar.PB_COLORS_ARROW, 0, colors, 0, colors.length);
			colors[0] = mPropertyBar.PB_COLORS_ARROW[0];
			mPropertyBar.setColors(colors);
			if (mIntent.equals(LineConstants.INTENT_LINE_DIMENSION)){
				mPropertyBar.scaleFromUnit(scaleFromUnitIndex);
				mPropertyBar.scaleToUnit(scaleToUnitIndex);
				mPropertyBar.scaleFromValue(scaleFromValue);
				mPropertyBar.scaleToValue(scaleToValue);
			}
		} else {
			int[] colors = new int[PropertyBar.PB_COLORS_LINE.length];
			System.arraycopy(PropertyBar.PB_COLORS_LINE, 0, colors, 0, colors.length);
			colors[0] = PropertyBar.PB_COLORS_LINE[0];
			mPropertyBar.setColors(colors);
		}

		mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
		mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, mOpacity);
		mPropertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, mThickness);

		mPropertyBar.reset(getSupportedProperties());
		mPropertyBar.setPropertyChangeListener(this);
	}

	private void resetAnnotBar(){
		mUiExtensionsManager.getMainFrame().getToolSetBar().removeAllItems();

		mOKItem = new BaseItemImpl(mContext);
		mOKItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_OK);
		mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
		mOKItem.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mUiExtensionsManager.changeState(ReadStateConfig.STATE_EDIT);
				mUiExtensionsManager.setCurrentToolHandler(null);
			}
		});

		mPropertyItem = new PropertyCircleItemImp(mContext) {

			@Override
			public void onItemLayout(int l, int t, int r, int b) {

				if (LineToolHandler.this == mUiExtensionsManager.getCurrentToolHandler()) {
					if (mPropertyBar.isShowing()) {
						Rect rect = new Rect();
						mPropertyItem.getContentView().getGlobalVisibleRect(rect);
						mPropertyBar.update(new RectF(rect));
					}
				}
			}
		};
		mPropertyItem.setTag(ToolbarItemConfig.ITEM_ANNOT_PROPERTY);
		mPropertyItem.setCentreCircleColor(mColor);

		final Rect rect = new Rect();
		mPropertyItem.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				mPropertyBar.setArrowVisible(AppDisplay.getInstance(mContext).isPad());
				mPropertyItem.getContentView().getGlobalVisibleRect(rect);
				mPropertyBar.show(new RectF(rect), true);
			}
		});

		setColorChangeListener(new ColorChangeListener() {
			@Override
			public void onColorChange(int color) {
				mPropertyItem.setCentreCircleColor(color);
			}
		});

		mContinuousCreateItem = new BaseItemImpl(mContext);
		mContinuousCreateItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_CONTINUE);
		mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinuousCreate));

		mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AppUtil.isFastDoubleClick()) {
					return;
				}

				mIsContinuousCreate = !mIsContinuousCreate;
				mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinuousCreate));
				AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
			}
		});

		mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
		mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
		mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
	}

	private int getContinuousIcon(boolean isContinuous){
		int iconId;
		if (isContinuous) {
			iconId = R.drawable.rd_annot_create_continuously_true_selector;
		} else {
			iconId = R.drawable.rd_annot_create_continuously_false_selector;
		}
		return iconId;
	}

	@Override
	public void onDeactivate() {
		if (mTouchCaptured) {
			if (mPdfViewCtrl.isPageVisible(mCapturedPage)) {
				addAnnot(mCapturedPage);
			}
			mTouchCaptured = false;
			mCapturedPage = -1;
		}
	}

	@Override
	public boolean onTouchEvent(int pageIndex, MotionEvent e) {
		PointF pt = new PointF(e.getX(), e.getY());
		mPdfViewCtrl.convertDisplayViewPtToPageViewPt(pt, pt, pageIndex);
		int action = e.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (!mTouchCaptured || mCapturedPage == pageIndex) {
				mTouchCaptured = true;
				mStartPt.set(pt);
				mStopPt.set(pt);
				if (mCapturedPage == -1) {
					mCapturedPage = pageIndex;
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mTouchCaptured) {
				PointF point = new PointF(pt.x, pt.y);
				mUtil.correctPvPoint(mPdfViewCtrl, pageIndex, point, mThickness);
				if (mCapturedPage == pageIndex && !point.equals(mStopPt.x, mStopPt.y)) {
					float thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, mThickness);
					RectF rect1 = mUtil.getArrowBBox(mStartPt, mStopPt, thickness);
					RectF rect2 = mUtil.getArrowBBox(mStartPt, point, thickness);
					rect2.union(rect1);
					mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect2, rect2, pageIndex);
					mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect2));
					mStopPt.set(point);
				}
				if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
					addAnnot(pageIndex);
					mTouchCaptured = false;
					if (!mIsContinuousCreate) {
						((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
					}
				}
			}
			break;
			default:
				break;
		}
		return true;
	}

	@Override
	public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
		return false;
	}

	@Override
	public void onDraw(int pageIndex, Canvas canvas) {

		if (mCapturedPage == pageIndex) {
			float distance = AppDmUtil.distanceOfTwoPoints(mStartPt, mStopPt);
			float thickness = mThickness;
			thickness = thickness < 1.0f?1.0f:thickness;
			thickness = (thickness + 3)*15.0f/8.0f;
			thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl,pageIndex,thickness);
			if (distance > thickness * LineUtil.ARROW_WIDTH_SCALE / 2) {
				setPaintProperty(mPdfViewCtrl, pageIndex, mPaint);
				Path path = mUtil.getLinePath(getIntent(), mStartPt, mStopPt, thickness);
				canvas.drawPath(path, mPaint);
				if (getIntent()!=null && getIntent().equals(LineConstants.INTENT_LINE_DIMENSION)){
					mTextPaint.setTextAlign(Paint.Align.CENTER);
					mTextPaint.setSubpixelText(true);
					mPdfViewCtrl.convertPageViewPtToPdfPt(mStartPt,dStart,pageIndex);
					mPdfViewCtrl.convertPageViewPtToPdfPt(mStopPt,dEnd,pageIndex);
					distance = AppDmUtil.distanceOfTwoPoints(dStart, dEnd);
					float factor = DistanceMeasurement.valueOf(scaleFromUnitIndex).getScaleWithDefault() * scaleToValue/scaleFromValue;
					if (scaleFromValue==0){
						factor = 0;
					}

					String text = String.valueOf((float) (Math.round(distance * 100 * factor))/100) + DistanceMeasurement.valueOf(scaleToUnitIndex).getName();
					PointF pointF = mAnnotHandler.calculateTextPosition(mStartPt,mTextPaint,pageIndex,text);
					canvas.drawText(text, pointF.x, pointF.y ,mTextPaint);
				}
			}
		}
	}

	@Override
	protected void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint) {
		paint.setColor(mColor);
		paint.setAlpha(AppDmUtil.opacity100To255(mOpacity));
		paint.setStrokeWidth(UIAnnotFrame.getPageViewThickness(pdfViewCtrl, pageIndex, mThickness));
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	}

	@Override
	public void onValueChanged(long property, int value) {
		super.onValueChanged(property, value);
		if (property == PropertyBar.PROPERTY_DISTANCE) {
			setScaleFromUnitIndex(value);
		}else if (property == PropertyBar.PROPERTY_DISTANCE_TIP) {
			setScaleToUnitIndex(value);
		}else if (property == PropertyBar.PROPERTY_DISTANCE_VALUE) {
			setScaleFromValue(value);
		}else if (property == PropertyBar.PROPERTY_DISTANCE_TIP_VALUE) {
			setScaleToValue(value);
		}
	}

	@Override
	public long getSupportedProperties() {
		if (LineConstants.INTENT_LINE_DIMENSION.equals(getIntent())){
			return mUtil.getDistanceSupportedProperties();
		}
		return mUtil.getSupportedProperties();
	}

	private int getToolType() {
		int toolType;
		if (LineConstants.INTENT_LINE_ARROW.equals(getIntent())) {
			toolType = MoreTools.MT_TYPE_ARROW;
		}else if (LineConstants.INTENT_LINE_DIMENSION.equals(getIntent())){
			toolType = MoreTools.MT_TYPE_DISTANCE;
		}else {
			toolType = MoreTools.MT_TYPE_LINE;
		}
		return toolType;
	}

	@Override
	protected void setPropertyBarProperties(PropertyBar propertyBar) {
		if (mIntent.equals(LineConstants.INTENT_LINE_ARROW) || mIntent.equals(LineConstants.INTENT_LINE_DIMENSION)) {
			int[] colors = new int[PropertyBar.PB_COLORS_ARROW.length];
			System.arraycopy(PropertyBar.PB_COLORS_ARROW, 0, colors, 0, colors.length);
			colors[0] = PropertyBar.PB_COLORS_ARROW[0];
			propertyBar.setColors(colors);
			if (mIntent.equals(LineConstants.INTENT_LINE_DIMENSION)){
				propertyBar.scaleFromUnit(scaleFromUnitIndex);
				propertyBar.scaleToUnit(scaleToUnitIndex);
				propertyBar.scaleFromValue(scaleFromValue);
				propertyBar.scaleToValue(scaleToValue);
			}
		} else {
			int[] colors = new int[PropertyBar.PB_COLORS_LINE.length];
			System.arraycopy(PropertyBar.PB_COLORS_LINE, 0, colors, 0, colors.length);
			colors[0] = PropertyBar.PB_COLORS_LINE[0];
			propertyBar.setColors(colors);
		}
		super.setPropertyBarProperties(propertyBar);
	}
	
	private void addAnnot(int pageIndex) {
		if (mTouchCaptured && mCapturedPage >= 0) {
			float distance = AppDmUtil.distanceOfTwoPoints(mStartPt, mStopPt);
			float thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, mThickness);
			if (distance > thickness * LineUtil.ARROW_WIDTH_SCALE / 2) {
				RectF bbox = mUtil.getArrowBBox(mStartPt, mStopPt, thickness);
				PointF startPt = new PointF(mStartPt.x, mStartPt.y);
				PointF stopPt = new PointF(mStopPt.x, mStopPt.y);
				mPdfViewCtrl.convertPageViewRectToPdfRect(bbox, bbox, pageIndex);
				mPdfViewCtrl.convertPageViewPtToPdfPt(startPt, startPt, pageIndex);
				mPdfViewCtrl.convertPageViewPtToPdfPt(stopPt, stopPt, pageIndex);

				LineAddUndoItem undoItem = new LineAddUndoItem(mAnnotHandler, mPdfViewCtrl);

				if (getIntent().equals(LineConstants.INTENT_LINE_DIMENSION)){
					float factor = DistanceMeasurement.valueOf(scaleFromUnitIndex).getScaleWithDefault() * scaleToValue/scaleFromValue;
					if (scaleFromValue==0){
						factor = 0;
					}
					String ratio = ""+scaleFromValue+" " + DistanceMeasurement.valueOf(scaleFromUnitIndex).getName()+
							" = " + scaleToValue+" " + DistanceMeasurement.valueOf(scaleToUnitIndex).getName();
					String unit = "" + DistanceMeasurement.valueOf(scaleToUnitIndex).getName();
					mAnnotHandler.addAnnot(pageIndex,
							undoItem,
							new RectF(bbox),
							mColor,
							AppDmUtil.opacity100To255(mOpacity),
							mThickness, startPt, stopPt, getIntent(),
							factor,unit,ratio,
							new Event.Callback() {
								@Override
								public void result(Event event, boolean success) {
									mCapturedPage = -1;
								}
							});
				} else{
					mAnnotHandler.addAnnot(pageIndex,
							undoItem,
							new RectF(bbox),
							mColor,
							AppDmUtil.opacity100To255(mOpacity),
							mThickness, startPt, stopPt, getIntent(),
							new Event.Callback() {
								@Override
								public void result(Event event, boolean success) {
									mCapturedPage = -1;
								}
							});
				}

			} else {
				RectF bbox = mUtil.getArrowBBox(mStartPt, mStopPt, thickness);
				mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
				mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(bbox));
			}
		}
	}

	public int getScaleFromUnitIndex() {
		return scaleFromUnitIndex;
	}

	public void setScaleFromUnitIndex(int index) {
		this.scaleFromUnitIndex = index;
	}

	public int getScaleToUnitIndex() {
		return scaleToUnitIndex;
	}

	public void setScaleToUnitIndex(int index) {
		this.scaleToUnitIndex = index;
	}

	public int getScaleFromValue() {
		return scaleFromValue;
	}

	public void setScaleFromValue(int value) {
		this.scaleFromValue = value;
	}

	public int getScaleToValue() {
		return scaleToValue;
	}

	public void setScaleToValue(int value) {
		this.scaleToValue = value;
	}
}
