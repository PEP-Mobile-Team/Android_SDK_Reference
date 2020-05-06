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
package com.foxit.uiextensions.modules.panzoom;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.common.fxcrt.Matrix2D;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.common.Renderer;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;

public class PanZoomView extends RelativeLayout {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;

    private RelativeLayout mPanZoomView = null;
    private LinearLayout mPanZoom_ll_center;

    private float mTouchStartX;
    private float mTouchStartY;
    private final WindowManager mWindowManager;
    private WindowManager.LayoutParams mWmParams;
    private OverlayView mOverlayView;
    private int mMaxWidth;
    private int mMaxHeight;

    public PanZoomView(Context context, PDFViewCtrl pdfViewCtrl) {
        super(context);
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mPanZoomView = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.pan_zoom_layout, this);

        initView();
        bindEvent();
    }

    private void initView() {
        float dpi = mContext.getResources().getDisplayMetrics().densityDpi;
        if (dpi == 0) {
            dpi = 240;
        }
        mMaxWidth = (int) (dpi / 4 * 3.5f);
        mMaxHeight = (int) (dpi / 4 * 5);

        int pageIndex = mPdfViewCtrl.getCurrentPage();
        if (mPdfViewCtrl.getPageViewWidth(pageIndex) > mPdfViewCtrl.getPageViewHeight(pageIndex)) {
            int tmp = mMaxHeight;
            mMaxHeight = mMaxWidth;
            mMaxWidth = tmp;
        }

        mPanZoom_ll_center = (LinearLayout) mPanZoomView.findViewById(R.id.rd_panzoom_ll_center);
        mOverlayView = new OverlayView(mContext, mPdfViewCtrl);
        mPanZoom_ll_center.removeAllViews();
        mPanZoom_ll_center.addView(mOverlayView);
        mPanZoomView.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams centerParams = (LayoutParams) mPanZoom_ll_center.getLayoutParams();
        centerParams.width = mMaxWidth;
        centerParams.height = mMaxHeight;
        mPanZoom_ll_center.setLayoutParams(centerParams);
    }

    private void bindEvent() {
        mOverlayView.setPanZoomRectEvent(mPanZoomRectEventListener);
    }

    PanZoomView.IPanZoomRectEventListener mPanZoomRectEventListener = new PanZoomView.IPanZoomRectEventListener() {
        @Override
        public void onPanZoomRectMove(float offsetX, float offsetY) {
            mPdfViewCtrl.scrollView(offsetX, offsetY);
        }
    };

    protected void onBack() {
        if (mOverlayView != null) {
            mOverlayView.setPanZoomRectEvent(null);
            mOverlayView = null;
        }

        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() != null) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
        }
        return false;
    }

    protected boolean exit() {
        if (mPanZoomView.getVisibility() == View.VISIBLE) {
            onBack();
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mWmParams = (WindowManager.LayoutParams) getLayoutParams();
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchStartX = event.getX();
                mTouchStartY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float mMoveStartX = event.getX();
                float mMoveStartY = event.getY();

                if (Math.abs(mTouchStartX - mMoveStartX) > 3
                        && Math.abs(mTouchStartY - mMoveStartY) > 3) {
                    mWmParams.x = (int) (x - mTouchStartX);
                    mWmParams.y = (int) (y - mTouchStartY);
                    mWindowManager.updateViewLayout(this, mWmParams);
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
        }
        return true;
    }

    protected void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight){
        mWmParams = (WindowManager.LayoutParams) getLayoutParams();

        View rootView = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView();
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        int width = rootView.getWidth();
        int height = rootView.getHeight();
        if (AppDisplay.getInstance(mContext).isLandscape()) {
            mWmParams.x = x + width / 2;
            mWmParams.y = y + height / 4;
        } else {
            mWmParams.x = x + width / 4;
            mWmParams.y = y + height / 2;
        }
        mWindowManager.updateViewLayout(this, mWmParams);

        if (mOverlayView == null) return;
        mOverlayView.calcPdfViewerRect();
        reCalculatePanZoomRect(mOverlayView.curPageIndex);
    }

    public void reDrawPanZoomView(int pageIndex) {
        if (mOverlayView == null) return;
        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING ||
                mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER) {
            Rect pageRect = mPdfViewCtrl.getPageViewRect(pageIndex);
            if (!pageRect.intersect(mOverlayView.mPdfViewerRect)) {
                pageIndex = pageIndex + 1;
            }
        }

        float pageW = mPdfViewCtrl.getPageViewWidth(pageIndex);
        float pageH = mPdfViewCtrl.getPageViewHeight(pageIndex);

        if ((pageW > pageH && mMaxWidth < mMaxHeight) ||
                (pageW < pageH && mMaxWidth > mMaxHeight)) {
            int tmp = mMaxHeight;
            mMaxHeight = mMaxWidth;
            mMaxWidth = tmp;

            RelativeLayout.LayoutParams centerParams = (LayoutParams) mPanZoom_ll_center.getLayoutParams();
            centerParams.width = mMaxWidth;
            centerParams.height = mMaxHeight;
            mPanZoom_ll_center.setLayoutParams(centerParams);
        }

        mOverlayView.setCurPageIndex(pageIndex);
        mOverlayView.calcCurPageViewRect();
        mOverlayView.calcPanZoomRect();
        post(mOverlayView);
    }

    public int getCurPageIndex(){
        if (mOverlayView == null)
            return -1;
        return mOverlayView.getCurPageIndex();
    }

    public void reCalculatePanZoomRect(int pageIndex) {
        if (mOverlayView == null) return;

        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING ||
                mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER) {
            int tmpIndex = mOverlayView.curPageIndex;
            int curPageIndex = mPdfViewCtrl.getCurrentPage();
            Rect pageRect = mPdfViewCtrl.getPageViewRect(curPageIndex);
            if (pageRect.intersect(mOverlayView.mPdfViewerRect)) {
                tmpIndex = curPageIndex;
            } else if (pageIndex != mOverlayView.curPageIndex ) {
                tmpIndex = pageIndex;
            }
            reDrawPanZoomView(tmpIndex);
            return;
        }

        RectF dirtyRect = mOverlayView.mPanZoomRect;
        mOverlayView.calcCurPageViewRect();
        mOverlayView.calcPanZoomRect();

        dirtyRect.union(mOverlayView.mPanZoomRect);
        mOverlayView.invalidate(AppDmUtil.rectFToRect(dirtyRect));
    }

    public boolean isPanZoomRectMoving() {
        if (mOverlayView == null) return false;
        return mOverlayView.mTouchCaptured;
    }

    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        if (mOverlayView == null) return false;
        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING ||
                mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER) {
            Rect pageRect = mPdfViewCtrl.getPageViewRect(mOverlayView.curPageIndex);
            if (pageRect.intersect(mOverlayView.mPdfViewerRect)) return false;
            if (pageIndex != mOverlayView.curPageIndex) {
                reDrawPanZoomView(pageIndex);
            }
        }
        return false;
    }

    class OverlayView extends View implements Runnable {
        private PDFViewCtrl mViewCtrl;
        private Bitmap mBitmap = null;
        private Paint mBitmapPaint;
        private int curPageIndex;

        private RectF mPanZoomRect = new RectF(0, 0 ,0, 0);
        private Paint mRectPaint;
        private int mRectColor;
        private int mLineWith = 4;

        private Rect mPdfViewerRect = new Rect(0, 0, 0, 0);
        private Rect mCurPageViewRect = new Rect(0, 0, 0, 0);

        public OverlayView(Context context, PDFViewCtrl pdfViewCtrl) {
            super(context);
            mViewCtrl = pdfViewCtrl;
            curPageIndex = mViewCtrl.getCurrentPage();

            mBitmapPaint = new Paint();
            mBitmapPaint.setAntiAlias(true);
            mBitmapPaint.setFilterBitmap(true);

            mRectPaint = new Paint();
            mRectPaint.setStyle(Paint.Style.STROKE);
            mRectPaint.setAntiAlias(true);
            mRectPaint.setDither(true);
            mRectPaint.setStrokeWidth(mLineWith);
            mRectColor = Color.RED;

            setBackgroundColor(Color.argb(0xff, 0xe1, 0xe1, 0xe1));

            initialize();

            if (Build.VERSION.SDK_INT < 24) {
                post(this);
            }
        }

        private void initialize() {
            calcPdfViewerRect();
            calcCurPageViewRect();
            calcPanZoomRect();
        }

        private void calcPdfViewerRect() {
            int viewerLeft = mViewCtrl.getLeft();
            int viewerRight = mViewCtrl.getRight();
            int viewerTop = mViewCtrl.getTop();
            int viewerBottom = mViewCtrl.getBottom();
            mPdfViewerRect.set(viewerLeft, viewerTop, viewerRight, viewerBottom);
        }

        private void calcCurPageViewRect() {
            mCurPageViewRect.set(mViewCtrl.getPageViewRect(curPageIndex));
        }

        private void calcPanZoomRect() {
            Rect rect = new Rect();
            rect.set(mCurPageViewRect);
            rect.intersect(mPdfViewerRect);

            RectF pvRect = new RectF();
            mViewCtrl.convertDisplayViewRectToPageViewRect(AppDmUtil.rectToRectF(rect), pvRect, curPageIndex);

            float left = pvRect.left / mViewCtrl.getPageViewWidth(curPageIndex) * mMaxWidth;
            float top = pvRect.top / mViewCtrl.getPageViewHeight(curPageIndex) * mMaxHeight;
            float right = pvRect.right / mViewCtrl.getPageViewWidth(curPageIndex) * mMaxWidth;
            float bottom = pvRect.bottom / mViewCtrl.getPageViewHeight(curPageIndex) * mMaxHeight;

            mPanZoomRect.set(left, top, right, bottom);
        }

        Rect mClipRect = new Rect();
        RectF mDrawRect = new RectF();
        @Override
        protected void onDraw(Canvas canvas) {
            if (mBitmap == null) {
                if (Build.VERSION.SDK_INT >= 24) {
                    post(this);
                }
                return;
            }
            canvas.getClipBounds(mClipRect);
            canvas.drawBitmap(mBitmap, mClipRect.left, mClipRect.top, mBitmapPaint);

            DocumentManager documentManager = ((UIExtensionsManager) mViewCtrl.getUIExtensionsManager()).getDocumentManager();
            if (!documentManager.isCDRMDoc() ||
                    documentManager.isAccessPage(curPageIndex)) {
                mRectPaint.setColor(mRectColor);
                canvas.save();
                mDrawRect.set(mPanZoomRect);
                float dx = mLastPoint.x - mDownPoint.x;
                float dy = mLastPoint.y - mDownPoint.y;
                mDrawRect.offset(dx, dy);
                mDrawRect.inset(mLineWith / 2.0f, mLineWith / 2.0f);
                canvas.drawRect(mDrawRect, mRectPaint);
                canvas.restore();
            }

            drawWrapperData(canvas, curPageIndex, mBitmap.getWidth(), mBitmap.getHeight());
        }

        private PointF mAdjustPointF = new PointF(0, 0);

        private PointF adjustScalePointF(RectF rectF, float dxy) {
            float adjustx = 0;
            float adjusty = 0;

            if ((int) rectF.left < dxy) {
                adjustx = -rectF.left + dxy;
                rectF.left = dxy;
            }
            if ((int) rectF.top < dxy) {
                adjusty = -rectF.top + dxy;
                rectF.top = dxy;
            }

            if ((int) rectF.right > getWidth() - dxy) {
                adjustx = getWidth() - rectF.right - dxy;
                rectF.right = getWidth() - dxy;
            }
            if ((int) rectF.bottom > getHeight() - dxy) {
                adjusty = getHeight() - rectF.bottom - dxy;
                rectF.bottom = getHeight() - dxy;
            }
            mAdjustPointF.set(adjustx, adjusty);
            return mAdjustPointF;
        }

        private PointF mDownPoint = new PointF();
        private PointF mLastPoint = new PointF();
        private RectF mInvalidateRect = new RectF(0, 0, 0, 0);
        private RectF mAdjustRect = new RectF(0, 0, 0, 0);
        private boolean mTouchCaptured = false;

        private PointF mLastDownPoint = new PointF();

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mDownPoint.set(x, y);
                    mLastPoint.set(x, y);

                    mLastDownPoint.set(x, y);
                    boolean isMaxRect = mPanZoomRect.width() == mMaxWidth && mPanZoomRect.height() == mMaxHeight;
                    if (!isMaxRect && isInPanZoomRect(x, y)) {
                        mTouchCaptured = true;
                    } else {
                        mTouchCaptured = false;
                        return false;
                    }

                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (mTouchCaptured) {
                        if (x != mLastPoint.x && y != mLastPoint.y) {
                            RectF drawBBox = new RectF(mPanZoomRect);
                            mInvalidateRect.set(drawBBox);
                            mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                            mAdjustRect.set(drawBBox);
                            mAdjustRect.offset(x - mDownPoint.x, y - mDownPoint.y);
                            float deltaXY = 0;// Judging border value
                            PointF adjustXY = adjustScalePointF(mAdjustRect, deltaXY);
                            mInvalidateRect.union(mAdjustRect);

                            mInvalidateRect.inset(-deltaXY, -deltaXY);
                            invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                            mLastPoint.set(x, y);
                            mLastPoint.offset(adjustXY.x, adjustXY.y);

                            float offsetX = (mLastPoint.x - mLastDownPoint.x) * mViewCtrl.getPageViewWidth(curPageIndex) / mMaxWidth;
                            float offsetY = (mLastPoint.y - mLastDownPoint.y) * mViewCtrl.getPageViewHeight(curPageIndex) / mMaxHeight;
                            if (mPanZoomRectListener != null) {
                                mPanZoomRectListener.onPanZoomRectMove(offsetX, offsetY);
                            }

                            mLastDownPoint.set(mLastPoint);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mTouchCaptured) {
                        RectF drawBox = new RectF(mPanZoomRect);
                        drawBox.inset(mLineWith / 2.0f, mLineWith / 2.0f);
                        drawBox.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                        mPanZoomRect.set(drawBox);
                        mPanZoomRect.sort();
                        mPanZoomRect.inset(-mLineWith / 2.0f, -mLineWith / 2.0f);

                        mTouchCaptured = false;
                        mDownPoint.set(0, 0);
                        mLastPoint.set(0, 0);
                        mLastDownPoint.set(0, 0);
                        return true;
                    }

                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mLastDownPoint.set(0, 0);
                    return true;
                default:
            }

            return true;
        }

        @Override
        public void run() {
            drawPage(curPageIndex);
        }

        public void setCurPageIndex(int pageIndex) {
            curPageIndex = pageIndex;
        }

        public int getCurPageIndex() {
            return curPageIndex;
        }

        public void drawPage(int pageIndex) {
            PDFViewCtrl.lock();
            try {
                DocumentManager documentManager = ((UIExtensionsManager) mViewCtrl.getUIExtensionsManager()).getDocumentManager();
                if (documentManager.isCDRMDoc() &&
                        !documentManager.isAccessPage(pageIndex)) {
                    mBitmap = Bitmap.createBitmap(mMaxWidth, mMaxHeight, Bitmap.Config.RGB_565);
                    mBitmap.eraseColor(Color.WHITE);
                    invalidate();
                    return;
                }
                PDFPage pdfPage = documentManager.getPage(pageIndex, false);
                if (pdfPage == null) {
                    return;
                }

//                Matrix2D page2device = pdfPage.getDisplayMatrix(0, 0, mMaxWidth, mMaxHeight, mViewCtrl.getViewRotation());
//                mBitmap = Bitmap.createBitmap(mMaxWidth, mMaxHeight, Bitmap.Config.RGB_565);
//                mBitmap.eraseColor(Color.WHITE);

                float width = pdfPage.getWidth();
                float height = pdfPage.getHeight();

                if (isRotationVertical(mViewCtrl.getViewRotation())) {
                    float tmp = width;
                    width = height;
                    height = tmp;
                }

                Matrix2D page2device = pdfPage.getDisplayMatrix(0, 0, (int) width, (int) height, mViewCtrl.getViewRotation());
                Bitmap bitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.RGB_565);
                bitmap.eraseColor(Color.WHITE);

                Renderer renderer = new Renderer(bitmap, true);
                Progressive progressive = renderer.startRender(pdfPage, page2device, null);
                int state = Progressive.e_ToBeContinued;
                while (state == Progressive.e_ToBeContinued) {
                    state = progressive.resume();
                }

                mBitmap = Bitmap.createScaledBitmap(bitmap, mMaxWidth, mMaxHeight, true);
                invalidate();
            } catch (PDFException e) {
                e.printStackTrace();
            } finally {
                PDFViewCtrl.unlock();
            }
        }


        public boolean isInPanZoomRect(float x, float y) {
            if (mPanZoomRect.contains(x, y)) {
                return true;
            }

            return false;
        }

        IPanZoomRectEventListener mPanZoomRectListener;
        public void setPanZoomRectEvent(IPanZoomRectEventListener listener) {
            mPanZoomRectListener = listener;
        }

        private void drawWrapperData(Canvas canvas, int pageIndex, int width, int height) {
            DocumentManager documentManager = ((UIExtensionsManager) mViewCtrl.getUIExtensionsManager()).getDocumentManager();
            if (documentManager.isCDRMDoc() &&
                    !documentManager.isAccessPage(pageIndex)) {
                TextPaint textPaint = new TextPaint();
                textPaint.setColor(Color.RED);
                textPaint.setTextSize(14);

                StaticLayout layout = new StaticLayout(documentManager.getWrapperContent(), textPaint,
                        width - 10, Layout.Alignment.ALIGN_CENTER,
                        1.5f, 0, false);

                canvas.save();
                canvas.translate(10, height / 2);
                layout.draw(canvas);
                canvas.restore();
            }
        }
    }


    public interface IPanZoomRectEventListener {
        void onPanZoomRectMove(float offsetX, float offsetY);
    }

    private boolean isRotationVertical(int rotation) {
        if (rotation == com.foxit.sdk.common.Constants.e_Rotation90
                || rotation == com.foxit.sdk.common.Constants.e_Rotation270) {
            return true;
        }
        return false;
    }
}

