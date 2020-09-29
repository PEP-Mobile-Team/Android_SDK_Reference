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
package com.foxit.uiextensions.annots.multimedia.screen.image;


import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Screen;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.multimedia.MultimediaUtil;
import com.foxit.uiextensions.annots.multimedia.screen.MultimediaSupport;
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
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

public class PDFImageToolHandler implements ToolHandler {

    private PDFImageInfo mImageInfo;

    private RectF mLastImageRect = new RectF(0, 0, 0, 0);
    private RectF mImageRect = new RectF(0, 0, 0, 0);
    private int mLastPageIndex = -1;

    private boolean mTouchCaptured = false;
    private boolean mIsContinuousCreate = false;

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiExtensionsManager;

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    private PropertyBar mPropertyBar;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;
    private MultimediaUtil mMultimediaUtil;


    public PDFImageToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mImageInfo = new PDFImageInfo();
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
        mUiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        mPropertyBar = mUiExtensionsManager.getMainFrame().getPropertyBar();
        mMultimediaUtil = new MultimediaUtil(mContext);

        mUiExtensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {

                mMultimediaUtil.showPickDialog(mUiExtensionsManager, ToolHandler.TH_TYPE_PDFIMAGE, new MultimediaSupport.IPickResultListener() {
                    @Override
                    public void onResult(boolean isSuccess, String path) {
                        if (isSuccess) {
                            mUiExtensionsManager.setCurrentToolHandler(PDFImageToolHandler.this);
                            mUiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                            setImageInfo(path, mPdfViewCtrl.getCurrentPage());

                            resetPropertyBar();
                            resetAnnotBar();
                        } else {
                            mUiExtensionsManager.setCurrentToolHandler(null);
                            mUiExtensionsManager.changeState(ReadStateConfig.STATE_EDIT);
                        }
                    }
                });
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_IMAGE;
            }
        });
    }

    @Override
    public String getType() {
        return TH_TYPE_PDFIMAGE;
    }

    @Override
    public void onActivate() {
        mLastPageIndex = -1;
    }

    protected void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    protected void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

    private void resetPropertyBar() {
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, getImageInfo().getOpacity());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_ROTATION, getImageInfo().getRotation());
        mPropertyBar.setArrowVisible(true);
        mPropertyBar.reset(getSupportedProperties());
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_ROTATION;
    }

    private void resetAnnotBar() {
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
                if (PDFImageToolHandler.this == mUiExtensionsManager.getCurrentToolHandler()) {
                    if (mPropertyBar.isShowing()) {
                        Rect rect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                        mPropertyBar.update(new RectF(rect));
                    }
                }
            }
        };
        mPropertyItem.setTag(ToolbarItemConfig.ITEM_ANNOT_PROPERTY);
        mPropertyItem.setCentreCircleColor(Color.parseColor("#179CD8"));

        final Rect rect = new Rect();
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPropertyBar.reset(getSupportedProperties());
                mPropertyBar.setArrowVisible(true);

                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                mPropertyBar.show(new RectF(rect), true);
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
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        if (mImageInfo.getPageIndex() != pageIndex) {
            setImageInfo(mImageInfo.getPath(), pageIndex);
        }

        PointF disPoint = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF pvPoint = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(disPoint, pvPoint, pageIndex);
        float x = pvPoint.x;
        float y = pvPoint.y;

        mImageRect = getImageRectOnPageView(pvPoint, pageIndex);

        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mTouchCaptured && mLastPageIndex == -1 || mLastPageIndex == pageIndex) {
                    mTouchCaptured = true;
                    mLastImageRect = new RectF(mImageRect);
                    if (mLastPageIndex == -1) {
                        mLastPageIndex = pageIndex;
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!mTouchCaptured || mLastPageIndex != pageIndex) break;
                RectF rect = new RectF(mLastImageRect);
                rect.union(mImageRect);
                rect.inset(-10, -10);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, pageIndex);
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect));
                mLastImageRect = new RectF(mImageRect);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mTouchCaptured || mLastPageIndex != pageIndex) break;
                if (!mIsContinuousCreate) {
                    mUiExtensionsManager.setCurrentToolHandler(null);
                }
                RectF pdfRect = new RectF();
                mPdfViewCtrl.convertPageViewRectToPdfRect(mImageRect, pdfRect, pageIndex);
                createAnnot(pdfRect, pageIndex);
                return true;
            default:
                return true;
        }
        return true;
    }

    private RectF getImageRectOnPageView(PointF point, int pageIndex) {
        PointF pageViewPt = new PointF(point.x, point.y);
        float offsetX = mImageInfo.getWidth() * mImageInfo.getScale();
        float offsetY = mImageInfo.getHeight() * mImageInfo.getScale();

        RectF pageViewRect = new RectF(pageViewPt.x - offsetX, pageViewPt.y - offsetY, pageViewPt.x + offsetX, pageViewPt.y + offsetY);
        if (pageViewRect.left < 0) {
            pageViewRect.offset(-pageViewRect.left, 0);
        }
        if (pageViewRect.right > mPdfViewCtrl.getPageViewWidth(pageIndex)) {
            pageViewRect.offset(mPdfViewCtrl.getPageViewWidth(pageIndex) - pageViewRect.right, 0);
        }
        if (pageViewRect.top < 0) {
            pageViewRect.offset(0, -pageViewRect.top);
        }
        if (pageViewRect.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex)) {
            pageViewRect.offset(0, mPdfViewCtrl.getPageViewHeight(pageIndex) - pageViewRect.bottom);
        }
        return pageViewRect;
    }

    private void createAnnot(final RectF rectF, final int pageIndex) {
        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
            try {
                final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                final Screen newAnnot = (Screen) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Screen, AppUtil.toFxRectF(rectF)), Annot.e_Screen);

                final PDFImageAddUndoItem undoItem = new PDFImageAddUndoItem(mPdfViewCtrl);
                undoItem.mPageIndex = pageIndex;
                undoItem.mNM = AppDmUtil.randomUUID(null);
                undoItem.mOpacity = AppDmUtil.opacity100To255(mImageInfo.getOpacity()) / 255f;
                undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
                undoItem.mRotation = (mImageInfo.getRotation() + page.getRotation() + mPdfViewCtrl.getViewRotation()) % 4;
                undoItem.mImgPath = mImageInfo.getPath();
                undoItem.mFlags = Annot.e_FlagPrint;
                undoItem.mContents = AppFileUtil.getFileName(mImageInfo.getPath());
                undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
                undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
                undoItem.mBBox = new RectF(rectF);

                PDFImageEvent event = new PDFImageEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, newAnnot, mPdfViewCtrl);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            mUiExtensionsManager.getDocumentManager().onAnnotAdded(page, newAnnot);
                            mUiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {

                                try {
                                    RectF viewRect = AppUtil.toRectF(newAnnot.getRect());
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);

                                    Rect rect = new Rect();
                                    viewRect.roundOut(rect);
                                    viewRect.union(mLastImageRect);
                                    rect.inset(-10, -10);
                                    mPdfViewCtrl.refresh(pageIndex, rect);
                                    mLastImageRect.setEmpty();

                                    mTouchCaptured = false;
                                    mLastPageIndex = -1;
                                    if (!mIsContinuousCreate) {
                                        mUiExtensionsManager.setCurrentToolHandler(null);
                                    }
                                } catch (PDFException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
                mPdfViewCtrl.addTask(task);

            } catch (PDFException e) {
                if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                    mPdfViewCtrl.recoverForOOM();
                }
            }
        }
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
    public boolean isContinueAddAnnot() {
        return mIsContinuousCreate;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
        mIsContinuousCreate = continueAddAnnot;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (!mTouchCaptured || pageIndex != mLastPageIndex)
            return;

        Paint paint = new Paint();
        paint.setAlpha(100);

        ImageView view = new ImageView(mContext);
        Drawable drawable = Drawable.createFromPath(mImageInfo.getPath());
        view.setImageDrawable(drawable);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        canvas.drawRect(mImageRect, paint);
        view.draw(canvas);
        canvas.save();
        canvas.restore();
    }


    protected PDFImageInfo getImageInfo() {
        return mImageInfo;
    }

    private void setImageInfo(String picPath, int pageIndex) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picPath, options);

        int picWidth = options.outWidth;
        int picHeight = options.outHeight;

        mImageInfo.setWidth(picWidth);
        mImageInfo.setHeight(picHeight);
        mImageInfo.setPath(picPath);
        mImageInfo.setScale(getImageScale(picWidth, picHeight, pageIndex));
        mImageInfo.setPageIndex(pageIndex);
    }

    private float getImageScale(int picWidth, int picHeight, int pageIndex) {
        int pageWidth = mPdfViewCtrl.getPageViewWidth(pageIndex);
        int pageHeight = mPdfViewCtrl.getPageViewHeight(pageIndex);

        float widthScale = (float) picWidth / pageWidth;
        float heightScale = (float) picHeight / pageHeight;
        float scale = widthScale > heightScale ? 1 / (5 * widthScale) : 1 / (5 * heightScale);
        scale = (float) (Math.round(scale * 100)) / 100;
        return scale;
    }

}
