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
package com.foxit.uiextensions.modules.thumbnail.createpage.colorpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.foxit.uiextensions.utils.AppDisplay;

public class ColorSlideView extends View implements ColorObserver{

	private final Paint borderPaint;
	private final Rect viewRect = new Rect();
	private int w;
	private int h;
	private final Path borderPath;
	private Bitmap bitmap;
	private final Path pointerPath;
	private final Paint pointerPaint;

	private float currentPos;
	private ObservableColor observableColor = new ObservableColor(0);

	public void observeColor(ObservableColor observableColor) {
		this.observableColor = observableColor;
		observableColor.addObserver(this);
	}

	public ColorSlideView(Context context, AttributeSet attrs) {
		super(context, attrs);
		borderPaint = new Paint();
		borderPaint.setColor(0xff000000);
		borderPaint.setStrokeWidth(AppDisplay.getInstance(context).dp2px(0.5f));
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setAntiAlias(true);

		pointerPaint = new Paint();
		pointerPaint.setColor(0xff000000);
		pointerPaint.setStrokeWidth(AppDisplay.getInstance(context).dp2px(1f));
		pointerPaint.setStyle(Paint.Style.STROKE);
		pointerPaint.setAntiAlias(true);

		pointerPath = new Path();
		final float radiusPx = AppDisplay.getInstance(context).dp2px(6);
		pointerPath.addCircle(0, 0, radiusPx, Path.Direction.CW);
		borderPath = new Path();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		this.w = w;
		this.h = h;
		viewRect.set(0, 0, w, h);
		float inset = borderPaint.getStrokeWidth() / 2;
		borderPath.reset();
		borderPath.addRect(new RectF(inset, inset, w - inset, h - inset), Path.Direction.CW);
		updateBitmap();
	}

	protected void setPos(float pos) {
		currentPos = pos;
		optimisePointerColor();
	}

	protected void updateBitmap() {
		if (w > 0 && h > 0) {
			bitmap = makeBitmap(w, h);
			optimisePointerColor();
		}
	}

	private Bitmap makeBitmap(int w, int h) {
		final boolean isWide = w > h;
		final int n = Math.max(w, h);
		int[] colors = new int[n];

		float[] hsv = new float[]{0, 0, 0};
		observableColor.getHsv(hsv);

		for (int i = 0; i < n; ++i) {
			hsv[2] = isWide ? (float)i / n : 1 - (float)i / n;
			colors[i] = Color.HSVToColor(hsv);
		}
		final int bmpWidth = isWide ? w : 1;
		final int bmpHeight = isWide ? 1 : h;
		return Bitmap.createBitmap(colors, bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		switch (action) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				currentPos = valueForTouchPos(event.getX(), event.getY());
				optimisePointerColor();
				observableColor.updateValue(currentPos, this);
				invalidate();
				getParent().requestDisallowInterceptTouchEvent(true);
				return true;
		}
		return super.onTouchEvent(event);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawBitmap(bitmap, null, viewRect, null);
		canvas.drawPath(borderPath, borderPaint);

		canvas.save();
		if (isWide()) {
			canvas.translate(w * currentPos, h / 2);
		} else {
			canvas.translate(w / 2, h * (1 - currentPos));
		}
		canvas.drawPath(pointerPath, pointerPaint);
		canvas.restore();
	}

	private boolean isWide() {
		return w > h;
	}

	private float valueForTouchPos(float x, float y) {
		final float val = isWide() ? x / w : 1 - y / h;
		return Math.max(0, Math.min(1, val));
	}

	private void optimisePointerColor() {
		pointerPaint.setColor(getPointerColor(currentPos));
	}

	private int getPointerColor(float currentPos) {
		float brightColorLightness = observableColor.getLightness();
		float posLightness = currentPos * brightColorLightness;
		return posLightness > 0.5f ? 0xff000000 : 0xffffffff;
	}

	@Override
	public void updateColor(ObservableColor observableColor) {
		setPos(observableColor.getValue());
		updateBitmap();
		invalidate();
	}

}
