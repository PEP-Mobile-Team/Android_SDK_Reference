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
package com.foxit.uiextensions.annots.multimedia.sound;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.FileSpec;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Sound;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.multimedia.AudioPlayView;
import com.foxit.uiextensions.annots.multimedia.MultimediaUtil;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppIntentUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SoundAnnotHandler implements AnnotHandler {
    private FxProgressDialog mProgressDlg;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private MultimediaUtil mMultimediaUtil;

    private ArrayList<Integer> mMenuText;
    private AnnotMenu mAnnotMenu;
    private Map<String, String> mSoundTempPathMap = new HashMap<>();
    private Annot mCurrentAnnot;
    private Paint mFrmPaint;// outline

    private String mDefaultCache;
    private int mBBoxSpace;

    public SoundAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        this.mContext = context;
        this.mPdfViewCtrl = pdfViewCtrl;

        mDefaultCache = getDefaultCachePath(context);

        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mMultimediaUtil = new MultimediaUtil(mContext);
        mAnnotMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);
        mMenuText = new ArrayList<Integer>();

        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Paint.Style.STROKE);
        mFrmPaint.setAntiAlias(true);
        mFrmPaint.setStrokeWidth(2);
        int color = Color.parseColor("#179CD8");
        mFrmPaint.setColor(color);
    }

    @Override
    public int getType() {
        return Annot.e_Sound;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        RectF rectF = null;
        try {
            rectF = AppUtil.toRectF(annot.getRect());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rectF;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        RectF bbox = getAnnotBBox(annot);
        if (bbox == null) return false;
        try {
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, annot.getPage().getIndex());
        } catch (PDFException e) {
            return false;
        }
        return bbox.contains(point.x, point.y);
    }

    @Override
    public void onAnnotSelected(Annot annot, boolean reRender) {
        try {
            prepareAnnotMenu(annot);

            int pageIndex = annot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF _rect = AppUtil.toRectF(annot.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(_rect, _rect, pageIndex);
                RectF menuRect = new RectF(_rect);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
                mAnnotMenu.show(menuRect);

                RectF pageViewRect = new RectF(_rect);
                pageViewRect.inset(-10, -10);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(pageViewRect));
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mCurrentAnnot = annot;
                }
            } else {
                mCurrentAnnot = annot;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void prepareAnnotMenu(final Annot annot) {
        mMenuText.clear();
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
            mMenuText.add(AnnotMenu.AM_BT_PALY);
            mMenuText.add(AnnotMenu.AM_BT_FLATTEN);
        } else {
            mMenuText.add(AnnotMenu.AM_BT_PALY);
        }
        mAnnotMenu.dismiss();
        mAnnotMenu.setMenuItems(mMenuText);

        mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
            @Override
            public void onAMClick(int btType) {
                if (btType == AnnotMenu.AM_BT_PALY) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    playSound(annot);
                } else if (btType == AnnotMenu.AM_BT_FLATTEN) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                }
            }
        });
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
        if (mAnnotMenu.isShowing())
            mAnnotMenu.dismiss();

        try {

            int pageIndex = annot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex) && needInvalid) {
                RectF pdfRect = AppUtil.toRectF(annot.getRect());
                RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
            }
            mCurrentAnnot = null;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return onPressedEvent(true, pageIndex, motionEvent, annot);
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return onPressedEvent(false, pageIndex, motionEvent, annot);
    }

    private boolean onPressedEvent(boolean isSingleTap, int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (AppUtil.isFastDoubleClick()) {
            return true;
        }
        try {
            PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
            DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
            if (annot == documentManager.getCurrentAnnot()) {
                if ((pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt))) {
                    return true;
                } else {
                    documentManager.setCurrentAnnot(null);
                    return true;
                }
            } else {
                if (isSingleTap) {
                    playSound(annot);
                } else {
                    documentManager.setCurrentAnnot(annot);
                }
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void playSound(final Annot annot) {
        try {
            showProgressDlg();
            Sound sound = (Sound) annot;
            if (sound.isEmpty()) {
                dismissProgressDlg();
            } else {
                String filePath = mSoundTempPathMap.get(AppAnnotUtil.getAnnotUniqueID(annot));

                if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
                    FileSpec fileSpec = sound.getFileSpec();
                    String fileName = fileSpec.isEmpty() ? AppDmUtil.randomUUID("") + ".wav" : fileSpec.getFileName();
                    final String tempSavePath = getTempPath(annot, fileName);
                    OpenSoundTask openSoundTask = new OpenSoundTask(sound, tempSavePath, new Event.Callback() {
                        @Override
                        public void result(Event event, boolean success) {
                            dismissProgressDlg();
                            if (success) {
                                saveTempPath(annot, tempSavePath);
                                openFile(tempSavePath);
                            } else {
                                UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.rv_document_open_failed));
                            }
                        }
                    });
                    mPdfViewCtrl.addTask(openSoundTask);
                } else {
                    dismissProgressDlg();
                    openFile(filePath);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
            dismissProgressDlg();
            UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.rv_document_open_failed));
        }
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (curAnnot != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this) {
            try {
                RectF annotRect = AppUtil.toRectF(curAnnot.getRect());
                int pageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRect, annotRect, pageIndex);
                    mAnnotMenu.update(annotRect);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null)
            return;
        try {
            int index = annot.getPage().getIndex();
            if (AppAnnotUtil.equals(mCurrentAnnot, annot) && index == pageIndex) {
                canvas.save();

                RectF frameRectF = new RectF();
                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
                RectF rect = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                frameRectF.set(rect.left - mBBoxSpace, rect.top - mBBoxSpace, rect.right + mBBoxSpace, rect.bottom + mBBoxSpace);
                canvas.drawRect(frameRectF, mFrmPaint);
                canvas.restore();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showProgressDlg() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();

        if (mProgressDlg == null && uiExtensionsManager.getAttachedActivity() != null) {
            mProgressDlg = new FxProgressDialog(uiExtensionsManager.getAttachedActivity(), mContext.getApplicationContext().getString(R.string.fx_string_opening));
        }

        if (mProgressDlg != null && !mProgressDlg.isShowing()) {
            mProgressDlg.show();
        }
    }

    private void dismissProgressDlg() {
        if (mProgressDlg != null && mProgressDlg.isShowing()) {
            mProgressDlg.dismiss();
            mProgressDlg = null;
        }
    }

    private String getTempPath(Annot annot, String filename) {
        String tempPath = "";
        String uuid = null;
        try {
            uuid = annot.getUniqueID();
        } catch (PDFException e) {
            e.printStackTrace();
        }

        if (uuid == null || uuid.isEmpty()) {
            uuid = AppDmUtil.randomUUID("");
        }
        tempPath = mDefaultCache + uuid + "/";
        File file = new File(tempPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return tempPath + filename;
    }

    private void saveTempPath(Annot annot, String tmpPath) {
        String uuid = AppAnnotUtil.getAnnotUniqueID(annot);
        mSoundTempPathMap.put(uuid, tmpPath);
    }

    private AudioPlayView mAudioPlayView;

    private void openFile(String path) {
        if (mMultimediaUtil.canPlaySimpleAudio(path)) {
            if (mAudioPlayView == null) {
                mAudioPlayView = new AudioPlayView(mContext);

                ViewGroup readerView = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getMainFrame().getContentView();
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lp.addRule(RelativeLayout.CENTER_IN_PARENT);
                lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                if (AppDisplay.getInstance(mContext).isPad()) {
                    lp.setMargins(0, 0, 0, (int) AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_pad) + AppDisplay.getInstance(mContext).dp2px(16));
                } else {
                    lp.setMargins(0, 0, 0, AppDisplay.getInstance(mContext).dp2px(110));
                }
                readerView.addView(mAudioPlayView, lp);
            }

            showProgressDlg();
            mAudioPlayView.startPlayAudio(path, new AudioPlayView.OnPreparedListener() {
                @Override
                public void onPrepared(boolean success, MediaPlayer mp) {
                    dismissProgressDlg();
                    if (!success){
                        UIToast.getInstance(mContext).show(AppResource.getString(mContext,R.string.rv_document_open_failed));
                    }
                }
            });
        } else {
            if (mAudioPlayView != null)
                mAudioPlayView.release();
            AppIntentUtil.openFile(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity(), path, "audio/*");
        }
    }

    protected void changeUIState() {
        if (mAudioPlayView == null) return;
        if (View.VISIBLE == mAudioPlayView.getContentView().getVisibility()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAudioPlayView.changeUIState();
                }
            }, 100);
        }
    }

    protected void release() {
        if (mAudioPlayView != null)
            mAudioPlayView.release();

        if (mDefaultCache == null) return;
        // delete cache files
        File tempFile = new File(mDefaultCache);
        if (tempFile.exists()) {
            AppFileUtil.deleteFolder(tempFile, false);
        }
    }

    private static String getDefaultCachePath(Context context) {
        return AppFileUtil.getDiskCachePath(context) + File.separatorChar + "sound" + File.separator;
    }
}
