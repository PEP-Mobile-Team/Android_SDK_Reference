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
package com.foxit.uiextensions.annots.screen.multimedia;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Screen;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.screen.MultimediaSupport;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppIntentUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.UIToast;

import java.util.List;

public class MultimediaToolHandler implements ToolHandler {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiExtensionsManager;
    private MultimediaUtil mMultimediaUtil;

    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    private List<String> mSupportMultimediaList;

    private boolean mIsContinue;
    private String mIntent;

    public MultimediaToolHandler(Context context, PDFViewCtrl pdfViewCtrl, final String intent) {
        mContext = context;
        mIntent = intent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
        mMultimediaUtil = new MultimediaUtil(mContext);

        mUiExtensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                mUiExtensionsManager.setCurrentToolHandler(MultimediaToolHandler.this);
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
            }

            @Override
            public int getType() {
                return getToolType(intent);
            }
        });
    }

    private int getToolType(String intent) {
        int toolType;
        if (ToolHandler.TH_TYPE_SCREEN_AUDIO.equals(intent)) {
            toolType = MoreTools.MT_TYPE_AUDIO;
        } else {
            toolType = MoreTools.MT_TYPE_VIDEO;
        }
        return toolType;
    }

    @Override
    public String getType() {
        return mIntent;
    }

    @Override
    public void onActivate() {
        resetAnnotBar();
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

        mContinuousCreateItem = new BaseItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_CONTINUE);
        mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinue));

        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }

                mIsContinue = !mIsContinue;
                mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinue));
                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinue);
            }
        });

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
        int action = motionEvent.getActionMasked();
        PointF point = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        mPdfViewCtrl.convertPageViewPtToPdfPt(point, point, pageIndex);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                showSelectDialog(pageIndex, point);
                break;
            default:
                break;
        }
        return true;
    }

    private void showSelectDialog(final int pageIndex, final PointF pdfPoint) {
        mMultimediaUtil.showPickDialog(mUiExtensionsManager, mIntent, new MultimediaSupport.IPickResultListener() {
            @Override
            public void onResult(boolean isSuccess, String path) {

                if (isSuccess) {
                    String mimeType = mMultimediaUtil.getMimeType(path);
                    int lastIndex = path.lastIndexOf('.');
                    String exp = lastIndex >= 0 ? path.substring(lastIndex).toLowerCase() : "";
                    if (mimeType == null) {
                        mimeType = AppIntentUtil.getMIMEType(exp);
                    }

                    if (ToolHandler.TH_TYPE_SCREEN_AUDIO.equals(mIntent)) {
                        mSupportMultimediaList = mMultimediaUtil.getAudioSupportMimeList();
                    } else {
                        // In some phones, this type will return to video/ext-mpeg.
                        if (exp.equals(".mpeg")){
                            mimeType = "video/mpeg";
                        }
                        mSupportMultimediaList = mMultimediaUtil.getVideoSupportMimeList();
                    }

                    if (mSupportMultimediaList.contains(mimeType)) {
                        createAnnot(pdfPoint, pageIndex, path, mimeType);
                    } else {
                        UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.multimedia_type_not_support));
                    }
                }
            }
        });
    }

    private void createAnnot(PointF pdfPointF, int pageIndex, String path, String mimeType) {
        RectF viewRectF;
        Bitmap bmp;
        if (ToolHandler.TH_TYPE_SCREEN_AUDIO.equals(mIntent)) {
            bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.audio_play);

            if (PDFViewCtrl.PAGELAYOUTMODE_FACING == mPdfViewCtrl.getPageLayoutMode()
                    || PDFViewCtrl.PAGELAYOUTMODE_COVER == mPdfViewCtrl.getPageLayoutMode()) {
                viewRectF = getImageRectOnPageView(pdfPointF, pageIndex, bmp.getWidth() / 4.0f, bmp.getHeight() / 4.0f);
            } else {
                viewRectF = getImageRectOnPageView(pdfPointF, pageIndex, bmp.getWidth() / 2.0f, bmp.getHeight() / 2.0f);
            }
        } else {
            bmp = mMultimediaUtil.getVideoThumbnail(mPdfViewCtrl, path);
            viewRectF = getImageRectOnPageView(pdfPointF, pageIndex, bmp.getWidth(), bmp.getHeight());
        }

        RectF pdfRect = new RectF();
        mPdfViewCtrl.convertPageViewRectToPdfRect(viewRectF, pdfRect, pageIndex);
        createAnnot(pageIndex, pdfRect, path, mimeType, bmp);
    }

    private void createAnnot(final int pageIndex, final RectF annotRectF, String path, String mimeType, Bitmap bmp) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final Screen annot = (Screen) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Screen, AppUtil.toFxRectF(annotRectF)), Annot.e_Screen);

            final MultimediaAddUndoItem undoItem = new MultimediaAddUndoItem(mPdfViewCtrl);
            undoItem.mPageIndex = pageIndex;
            undoItem.mNM = AppDmUtil.randomUUID(null);
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mFlags = Annot.e_FlagPrint;
            undoItem.mFileName = AppFileUtil.getFileName(path);
            undoItem.mFilePath = path;
            undoItem.mMediaClipContentType = mimeType;
            undoItem.mPreviewBitmap = bmp;
            undoItem.mContents = AppFileUtil.getFileName(path);
            undoItem.mBBox = new RectF(annotRectF);
            undoItem.mRotation = (page.getRotation() + mPdfViewCtrl.getViewRotation()) % 4;

            MultimediaEvent event = new MultimediaEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF rectF = new RectF();
                            rectF.set(annotRectF);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                            Rect rect = new Rect();
                            rectF.roundOut(rect);
                            mPdfViewCtrl.refresh(pageIndex, rect);

                            if (!mIsContinue) {
                                mUiExtensionsManager.setCurrentToolHandler(null);
                            }
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
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
        return mIsContinue;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
        mIsContinue = continueAddAnnot;
    }

    private RectF getImageRectOnPageView(PointF pdfPoint, int pageIndex, float offsetX, float offsetY) {
        PointF pageViewPt = new PointF(pdfPoint.x, pdfPoint.y);
        mPdfViewCtrl.convertPdfPtToPageViewPt(pageViewPt, pageViewPt, pageIndex);

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

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
    }

}
