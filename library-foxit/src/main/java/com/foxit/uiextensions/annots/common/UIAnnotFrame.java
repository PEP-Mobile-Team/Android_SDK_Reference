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
package com.foxit.uiextensions.annots.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;


public class UIAnnotFrame {
	/**
     * 0---1---2 
     * |       | 
     * 7       3 
     * |       | 
     * 6---5---4  
     */
    public static final int CTL_NONE 			= -1;
    public static final int CTL_LEFT_TOP 		= 0;
    public static final int CTL_MID_TOP 		= 1;
    public static final int CTL_RIGHT_TOP 		= 2;
    public static final int CTL_RIGHT_MID 		= 3;
    public static final int CTL_RIGHT_BOTTOM 	= 4;
    public static final int CTL_MID_BOTTOM 		= 5;
    public static final int CTL_LEFT_BOTTOM 	= 6;
    public static final int CTL_LEFT_MID 		= 7;

    public static final int OP_DEFAULT 			= -1;
    public static final int OP_TRANSLATE		= 0;
    public static final int OP_SCALE 			= 1;

    private float mFrmLineWidth			= 1;
    private float mCtlLineWidth			= 2;
    private float mCtlRadius				= 5;
    private float mCtlTouchExt			= 20;
    private Paint mFrmPaint				= new Paint();
    private Paint mCtlPaint				= new Paint();

	private static UIAnnotFrame mInstance = null;
	public static UIAnnotFrame getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new UIAnnotFrame(context);
		}

		return mInstance;
	}

    private UIAnnotFrame(Context context) {
    	float d2pFactor = AppDisplay.getInstance(context).dp2px(1.0f);
    	mFrmLineWidth *= d2pFactor;
    	mCtlLineWidth *= d2pFactor;
    	mCtlRadius *= d2pFactor;
    	mCtlTouchExt *= d2pFactor;
		mFrmPaint.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());
		mFrmPaint.setStyle(Style.STROKE);
		mFrmPaint.setAntiAlias(true);
		mFrmPaint.setStrokeWidth(mFrmLineWidth);
		mCtlPaint.setStrokeWidth(mCtlLineWidth);
    }

    public static RectF calculateBounds(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot) {
		try {
			RectF bbox = AppUtil.toRectF(annot.getRect());
			float thickness = annot.getBorderInfo().getWidth();
			return calculateBounds(pdfViewCtrl, pageIndex, bbox, thickness);
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return  null;
	}

	public static float getPageViewThickness(PDFViewCtrl pdfViewCtrl, int pageIndex, float thickness) {
		RectF rectF = new RectF(0, 0, thickness, thickness);
		if (pdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex)) {
			return rectF.width();
		}
		return thickness;
	}

    public static RectF calculateBounds(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF docBBox, float docThickness) {
		pdfViewCtrl.convertPdfRectToPageViewRect(docBBox, docBBox, pageIndex);
		float thickness = getPageViewThickness(pdfViewCtrl, pageIndex, docThickness);
		docBBox.inset(-thickness / 2 - AppAnnotUtil.getAnnotBBoxSpace(),
				-thickness / 2 - AppAnnotUtil.getAnnotBBoxSpace());
		return docBBox;
	}

    public static RectF mapBounds(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, int op, int ctl, float dx, float dy) {
		try {
			RectF bbox = AppUtil.toRectF(annot.getRect());
			pdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
			Matrix matrix = calculateOperateMatrix(bbox, op, ctl, dx, dy);
			matrix.mapRect(bbox);

			float thickness = getPageViewThickness(pdfViewCtrl, pageIndex, annot.getBorderInfo().getWidth());
			bbox.inset(-thickness / 2 - AppAnnotUtil.getAnnotBBoxSpace(),
					-thickness / 2 - AppAnnotUtil.getAnnotBBoxSpace());
			return bbox;
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static float[] calculateControls(RectF bounds) {
		float l = bounds.left;
		float t = bounds.top;
		float r = bounds.right;
		float b = bounds.bottom;
		float[] ctlPts = new float[] {
				l, t,
				(l + r) / 2, t,
				r, t,
				r, (t + b) / 2,
				r, b,
				(l + r) / 2, b,
				l, b,
				l, (t + b) / 2,
			};
		return ctlPts;
	}

	public int hitControlTest(RectF bounds, PointF point) {
		float[] ctlPts = calculateControls(bounds);
		RectF area = new RectF();
		for (int i = 0; i < ctlPts.length / 2; i ++) {
			area.set(ctlPts[i * 2], ctlPts[i * 2 + 1], ctlPts[i * 2], ctlPts[i * 2 + 1]);
			area.inset(-mCtlTouchExt, -mCtlTouchExt);
			if (area.contains(point.x, point.y)) {
				return i;
			}
		}
		return CTL_NONE;
	}

	private static Matrix calculateScaleMatrix(RectF bounds, int ctl, float dx, float dy) {
		Matrix matrix = new Matrix();
		float ctlPts[] = calculateControls(bounds);
		float px = ctlPts[ctl * 2];
		float py = ctlPts[ctl * 2 + 1];

		float oppositeX = 0 ;
		float oppositeY = 0 ;
		if (ctl < 4 && ctl >= 0) {
			oppositeX = ctlPts[ctl * 2 + 8];
			oppositeY = ctlPts[ctl * 2 + 9];
		} else if (ctl >= 4) {
			oppositeX = ctlPts[ctl * 2 - 8];
			oppositeY = ctlPts[ctl * 2 - 7];
		}

		float scaleh = (px + dx - oppositeX) / (px - oppositeX);
		float scalev = (py + dy - oppositeY) / (py - oppositeY);

		switch (ctl) {
		case CTL_LEFT_TOP:
		case CTL_RIGHT_TOP:
		case CTL_RIGHT_BOTTOM:
		case CTL_LEFT_BOTTOM:
			matrix.postScale(scaleh, scalev, oppositeX, oppositeY);
			break;
		case CTL_RIGHT_MID:
		case CTL_LEFT_MID:
			matrix.postScale(scaleh, 1.0f, oppositeX, oppositeY);
			break;
		case CTL_MID_TOP:
		case CTL_MID_BOTTOM:
			matrix.postScale(1.0f, scalev, oppositeX, oppositeY);
			break;
		case CTL_NONE:
		default:
			break;
		}
		return matrix;
	}

	public static Matrix calculateOperateMatrix(RectF bounds, int op, int ctl, float dx, float dy) {
		Matrix matrix = new Matrix();
		if (op == OP_SCALE) {
			matrix = UIAnnotFrame.calculateScaleMatrix(bounds, ctl, dx, dy);
		} else {
			matrix.preTranslate(dx, dy);
		}
		return matrix;
	}

	public float getControlExtent() {
		return mCtlLineWidth + mCtlRadius;
	}

	public void extentBoundsToContainControl(RectF bounds) {
		bounds.inset(-getControlExtent(), -getControlExtent());
	}

	private PointF calculateTranslateCorrection(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF bounds) {
		PointF adjust = new PointF();
		float extent = getControlExtent();
		if (bounds.left < extent) {
			adjust.x = extent - bounds.left;
		}
		if (bounds.top < extent) {
			adjust.y = extent - bounds.top;
		}
		if (bounds.right > pdfViewCtrl.getPageViewWidth(pageIndex) - extent) {
			adjust.x = pdfViewCtrl.getPageViewWidth(pageIndex) - bounds.right - extent;
		}
		if (bounds.bottom > pdfViewCtrl.getPageViewHeight(pageIndex) - extent) {
			adjust.y = pdfViewCtrl.getPageViewHeight(pageIndex) - bounds.bottom - extent;
		}
		return adjust;
	}

	private PointF calculateScaleCorrection(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF bounds, int ctl) {
		PointF adjust = new PointF();
		float extent = getControlExtent();
		switch (ctl) {
		case CTL_LEFT_TOP:
			if (bounds.left < extent) {
				adjust.x = extent - bounds.left ;
			}
			if (bounds.top < extent) {
				adjust.y = extent - bounds.top;
			}
			break;
		case CTL_MID_TOP:
			if (bounds.top < extent) {
				adjust.y = extent - bounds.top;
			}
			break;
		case CTL_RIGHT_TOP:
			if (bounds.right > pdfViewCtrl.getPageViewWidth(pageIndex) - extent) {
				adjust.x = pdfViewCtrl.getPageViewWidth(pageIndex) - bounds.right - extent;
			}
			if (bounds.top < extent) {
				adjust.y = extent - bounds.top;
			}
			break;
		case CTL_RIGHT_MID:
			if (bounds.right > pdfViewCtrl.getPageViewWidth(pageIndex) - extent) {
				adjust.x = pdfViewCtrl.getPageViewWidth(pageIndex) - bounds.right - extent;
			}
			break;
		case CTL_RIGHT_BOTTOM:
			if (bounds.right > pdfViewCtrl.getPageViewWidth(pageIndex) - extent) {
				adjust.x = pdfViewCtrl.getPageViewWidth(pageIndex) - bounds.right - extent;
			}
			if (bounds.bottom > pdfViewCtrl.getPageViewHeight(pageIndex) - extent) {
				adjust.y = pdfViewCtrl.getPageViewHeight(pageIndex) - bounds.bottom - extent;
			}
			break;
		case CTL_MID_BOTTOM:
			if (bounds.bottom > pdfViewCtrl.getPageViewHeight(pageIndex) - extent) {
				adjust.y = pdfViewCtrl.getPageViewHeight(pageIndex) - bounds.bottom - extent;
			}
			break;
		case CTL_LEFT_BOTTOM:
			if (bounds.left < extent) {
				adjust.x = extent - bounds.left ;
			}
			if (bounds.bottom > pdfViewCtrl.getPageViewHeight(pageIndex) - extent) {
				adjust.y = pdfViewCtrl.getPageViewHeight(pageIndex) - bounds.bottom - extent;
			}
			break;
		case CTL_LEFT_MID:
			if (bounds.left < extent) {
				adjust.x = extent - bounds.left ;
			}
			break;
        default:
                break;
		}
		return adjust;
	}

	public PointF calculateCorrection(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF bounds, int op, int ctl) {
		switch (op) {
		case UIAnnotFrame.OP_TRANSLATE:
			return calculateTranslateCorrection(pdfViewCtrl, pageIndex, bounds);
		case UIAnnotFrame.OP_SCALE:
			return calculateScaleCorrection(pdfViewCtrl, pageIndex, bounds, ctl);
		default:
		}
		return new PointF();
	}

	public static void adjustBounds(RectF bounds, int op, int ctl, PointF adjust) {
		switch (op) {
		case UIAnnotFrame.OP_TRANSLATE:
			bounds.offset(adjust.x, adjust.y);
			break;
		case UIAnnotFrame.OP_SCALE:
			UIAnnotFrame.adjustControl(bounds, ctl, adjust);
			break;
		default:
			break;
		}
	}

	public static void adjustControl(RectF bounds, int ctl, PointF adjust) {
		switch (ctl) {
		case CTL_LEFT_TOP:
			bounds.left += adjust.x;
			bounds.top += adjust.y;
			break;
		case CTL_MID_TOP:
			bounds.top += adjust.y;
			break;
		case CTL_RIGHT_TOP:
			bounds.right += adjust.x;
			bounds.top += adjust.y;
			break;
		case CTL_RIGHT_MID:
			bounds.right += adjust.x;
			break;
		case CTL_RIGHT_BOTTOM:
			bounds.right += adjust.x;
			bounds.bottom += adjust.y;
			break;
		case CTL_MID_BOTTOM:
			bounds.bottom += adjust.y;
			break;
		case CTL_LEFT_BOTTOM:
			bounds.left += adjust.x;
			bounds.bottom += adjust.y;
			break;
		case CTL_LEFT_MID:
			bounds.left += adjust.x;
			break;
		default:
            break;
		}

	}

	public void drawFrame(Canvas canvas, RectF bounds, int color, int opacity) {
		mFrmPaint.setColor(color);
		mFrmPaint.setAlpha(opacity);
		canvas.drawRect(bounds, mFrmPaint);
	}

	public void drawControls(Canvas canvas, RectF bounds, int color, int opacity) {
		float[] ctlPts = calculateControls(bounds);
		for (int i = 0; i < ctlPts.length; i += 2) {
			mCtlPaint.setColor(Color.WHITE);
			mCtlPaint.setAlpha(255);
			mCtlPaint.setStyle(Paint.Style.FILL);
			canvas.drawCircle(ctlPts[i], ctlPts[i + 1], mCtlRadius, mCtlPaint);
			mCtlPaint.setColor(color);
			mCtlPaint.setAlpha(opacity);
			mCtlPaint.setStyle(Paint.Style.STROKE);
			canvas.drawCircle(ctlPts[i], ctlPts[i + 1], mCtlRadius, mCtlPaint);
		}
	}

	public void draw(Canvas canvas, RectF bounds, int color, int opacity) {
		drawFrame(canvas, bounds, color, opacity);
		drawControls(canvas, bounds, color, opacity);
	}
}
