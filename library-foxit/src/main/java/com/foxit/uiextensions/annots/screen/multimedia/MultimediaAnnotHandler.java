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


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.fxcrt.FileReaderCallback;
import com.foxit.sdk.pdf.FileSpec;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.Rendition;
import com.foxit.sdk.pdf.actions.Action;
import com.foxit.sdk.pdf.actions.RenditionAction;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Screen;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppIntentUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.UIToast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/*
 *   1-----------2
 *   |	         |
 *   |	         |
 *   |           |
 *   |           |
 *   |           |
 *   4-----------3
 *   */
public class MultimediaAnnotHandler implements AnnotHandler {

    private static final int CTR_NONE = -1;
    private static final int CTR_LT = 1;
    private static final int CTR_RT = 2;
    private static final int CTR_RB = 3;
    private static final int CTR_LB = 4;
    private int mCurrentCtr = CTR_NONE;

    private static final int OPER_DEFAULT = -1;
    private static final int OPER_SCALE_LT = 1;// old:start at 0
    private static final int OPER_SCALE_RT = 2;
    private static final int OPER_SCALE_RB = 3;
    private static final int OPER_SCALE_LB = 4;
    private static final int OPER_TRANSLATE = 5;
    private int mLastOper = OPER_DEFAULT;

    private float mCtlPtLineWidth = 2;
    private float mCtlPtRadius = 5;
    private float mCtlPtTouchExt = 20;
    private float mCtlPtDeltyXY = 20;// Additional refresh range

    private Paint mFrmPaint;// outline
    private Paint mCtlPtPaint;

    private Annot mCurrentAnnot;
    private MultimediaUtil mMultimediaUtil;

    private ArrayList<Integer> mMenuText;
    private AnnotMenu mAnnotMenu;

    private RectF mTempLastBBox = new RectF();
    private int mTempLastBorderColor;
    private float mTempLastBorderWidth;

    private RectF mThicknessRectF = new RectF();
    private RectF mPageViewRect = new RectF(0, 0, 0, 0);
    private RectF mPageDrawRect = new RectF();
    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);
    private RectF mAnnotMenuRect = new RectF(0, 0, 0, 0);
    private RectF mViewDrawRect = new RectF(0, 0, 0, 0);
    private RectF mDocViewerBBox = new RectF(0, 0, 0, 0);

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ProgressDialog mProgressDlg;

    private PointF mDownPoint;
    private PointF mLastPoint;

    private Map<String, String> mMultimediaTempPathMap = new HashMap<>();

    private boolean mTouchCaptured = false;
    private boolean mIsModify;
    private float mThickness = 0f;

    public MultimediaAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Paint.Style.STROKE);
        mFrmPaint.setAntiAlias(true);

        mCtlPtPaint = new Paint();

        mAnnotMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);
        mMenuText = new ArrayList<Integer>();
        mMultimediaUtil = new MultimediaUtil(mContext);

        ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).registerLifecycleListener(mLifecycleEventListener);
    }

    @Override
    public int getType() {
        return AnnotHandler.TYPE_SCREEN_MULTIMEDIA;
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
        mCtlPtRadius = AppDisplay.getInstance(mContext).dp2px(mCtlPtRadius);
        mCtlPtDeltyXY = AppDisplay.getInstance(mContext).dp2px(mCtlPtDeltyXY);
        try {
            mTempLastBBox = AppUtil.toRectF(annot.getRect());
            mTempLastBorderColor = annot.getBorderColor();
            mTempLastBorderWidth = annot.getBorderInfo().getWidth();

            RectF _rect = AppUtil.toRectF(annot.getRect());
            mPageViewRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);

            prepareAnnotMenu(annot);
            RectF menuRect = new RectF(mPageViewRect);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
            mAnnotMenu.show(menuRect);

            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(mPageViewRect));
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

    @Override
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
        mCtlPtRadius = 5;
        mCtlPtDeltyXY = 20;

        mAnnotMenu.setListener(null);
        mAnnotMenu.dismiss();

        try {
            PDFPage page = annot.getPage();

            if (needInvalid && mIsModify) {
                if (mTempLastBorderWidth == annot.getBorderInfo().getWidth()
                        && mTempLastBorderColor == annot.getBorderColor()
                        && mTempLastBBox.equals(AppUtil.toRectF(annot.getRect()))) {
                    modifyAnnot(page.getIndex(), annot, AppUtil.toRectF(annot.getRect()), false, true,
                            Module.MODULE_NAME_MEDIA, null);
                } else {
                    modifyAnnot(page.getIndex(), annot, AppUtil.toRectF(annot.getRect()), true, true,
                            Module.MODULE_NAME_MEDIA, null);
                }
            } else if (mIsModify) {
                annot.setBorderColor(mTempLastBorderColor);
                BorderInfo borderInfo = new BorderInfo();
                borderInfo.setWidth(mTempLastBorderWidth);
                annot.setBorderInfo(borderInfo);
                annot.move(AppUtil.toFxRectF(mTempLastBBox));
                annot.resetAppearanceStream();
            }

            if (mPdfViewCtrl.isPageVisible(page.getIndex()) && needInvalid) {
                RectF pdfRect = AppUtil.toRectF(annot.getRect());
                RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, page.getIndex());
                mPdfViewCtrl.refresh(page.getIndex(), AppDmUtil.rectFToRect(viewRect));
            }

            mCurrentAnnot = null;
            mIsModify = false;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void modifyAnnot(final int pageIndex, final Annot annot, RectF bbox, boolean isModifyJni, final boolean addUndo, final String fromType, final Event.Callback result) {
        try {
            final MultimediaModifyUndoItem undoItem = new MultimediaModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.mBBox = new RectF(bbox);
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

            undoItem.mOldBBox = new RectF(mTempLastBBox);
            undoItem.mOldColor = mTempLastBorderColor;
            undoItem.mOldLineWidth = mTempLastBorderWidth;

            if (isModifyJni) {
                final RectF tempRectF = AppUtil.toRectF(annot.getRect());

                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(addUndo);
                MultimediaEvent event = new MultimediaEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Screen) annot, mPdfViewCtrl);

                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            if (addUndo) {
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                            }

                            if (fromType.equals("")) {
                                mIsModify = true;
                            }

                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                try {
                                    RectF annotRectF = AppUtil.toRectF(annot.getRect());
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                                    annotRectF.union(tempRectF);
                                    annotRectF.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                                } catch (PDFException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
                mPdfViewCtrl.addTask(task);
            }
            if (!fromType.equals("")) {
                mIsModify = true;
                if (isModifyJni) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);
                }

                if (!isModifyJni) {
                    Screen screenAnnot = (Screen) annot;
                    RectF tempRectF = AppUtil.toRectF(annot.getRect());
                    screenAnnot.move(AppUtil.toFxRectF(bbox));
                    screenAnnot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                    screenAnnot.resetAppearanceStream();

                    RectF annotRectF = AppUtil.toRectF(annot.getRect());

                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());

                        mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                        annotRectF.union(tempRectF);
                        annotRectF.inset(-thickness - mCtlPtRadius - mCtlPtDeltyXY, -thickness - mCtlPtRadius - mCtlPtDeltyXY);
                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                    }
                }
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    private void deleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
        }

        try {
            final PDFPage page = annot.getPage();
            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            final int pageIndex = page.getIndex();

            final MultimediaDeleteUndoItem undoItem = new MultimediaDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;

            undoItem.mContents = annot.getContent();
//            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mBBox = AppUtil.toRectF(annot.getRect());
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mRotation = ((Screen) annot).getRotation();

            Action action = ((Screen) annot).getAction();
            RenditionAction renditionAction = new RenditionAction(action);
            final Rendition rendition = renditionAction.getRendition(0);

            undoItem.mPDFDictionary = ((Screen) annot).getMKDict();
            undoItem.mFileName = rendition.getMediaClipName();
            undoItem.mMediaClipContentType = rendition.getMediaClipContentType();

            String filePath = mMultimediaTempPathMap.get(annot.getUniqueID());

            if (filePath == null || filePath.isEmpty()) {
                FileSpec fileSpec = rendition.getMediaClipFile();
                String fileName = fileSpec.getFileName();
                final String tmpPath = getTempPath(annot, fileName);

                saveMultimediaAttachment(mPdfViewCtrl, tmpPath, fileSpec, new Event.Callback() {

                    @Override
                    public void result(Event event, boolean success) {
                        saveTempPath(annot, tmpPath);

                        undoItem.mFilePath = tmpPath;
                        deleteAnnot(undoItem, annot, viewRect, addUndo, result);
                    }
                });
            } else {
                File file = new File(filePath);
                if (file.exists()) {
                    undoItem.mFilePath = filePath;
                    deleteAnnot(undoItem, annot, viewRect, addUndo, result);
                } else {
                    FileSpec fileSpec = rendition.getMediaClipFile();
                    String fileName = fileSpec.getFileName();
                    final String tmpPath = getTempPath(annot, fileName);

                    saveMultimediaAttachment(mPdfViewCtrl, tmpPath, fileSpec, new Event.Callback() {

                        @Override
                        public void result(Event event, boolean success) {
                            saveTempPath(annot, tmpPath);

                            undoItem.mFilePath = tmpPath;
                            deleteAnnot(undoItem, annot, viewRect, addUndo, result);
                        }
                    });
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void deleteAnnot(final MultimediaDeleteUndoItem undoItem, final Annot annot, final RectF viewRect, final boolean addUndo, final Event.Callback result) {
        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            MultimediaEvent deleteEvent = new MultimediaEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Screen) annot, mPdfViewCtrl);
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(deleteEvent, true);
                }
                return;
            }
            EditAnnotTask task = new EditAnnotTask(deleteEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (addUndo) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }

                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                        }
                    }

                    if (result != null) {
                        result.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void prepareAnnotMenu(final Annot annot) {
        resetAnnotationMenuResource(annot);

        mAnnotMenu.dismiss();
        mAnnotMenu.setMenuItems(mMenuText);

        mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
            @Override
            public void onAMClick(int btType) {
                if (btType == AnnotMenu.AM_BT_DELETE) {
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        deleteAnnot(annot, true, null);
                    }
                } else if (btType == AnnotMenu.AM_BT_PALY) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    openMultimedia(annot);
                } else if (btType == AnnotMenu.AM_BT_FLATTEN) {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                }
            }
        });
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        try {
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            RectF bbox = AppUtil.toRectF(annot.getRect());
            if (content.getBBox() != null)
                bbox = content.getBBox();

            mTempLastBorderColor = annot.getBorderColor();
            mTempLastBorderWidth = annot.getBorderInfo().getWidth();
            mTempLastBBox = bbox;

            modifyAnnot(pageIndex, annot, bbox, true, addUndo, "", result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        deleteAnnot(annot, addUndo, result);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (null == getToolHandler(annot)
                || !((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
            return false;
        }

        PointF point = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        float evX = point.x;
        float evY = point.y;

        int action = motionEvent.getActionMasked();
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        mThickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
                        RectF pageViewBBox = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                        RectF pdfRect = AppUtil.toRectF(annot.getRect());
                        mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
                        mPageViewRect.inset(mThickness / 2f, mThickness / 2f);

                        mCurrentCtr = isTouchControlPoint(pageViewBBox, evX, evY);

                        mDownPoint.set(evX, evY);
                        mLastPoint.set(evX, evY);

                        if (mCurrentCtr == CTR_LT) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_LT;
                            return true;
                        } else if (mCurrentCtr == CTR_RT) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_RT;
                            return true;
                        } else if (mCurrentCtr == CTR_RB) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_RB;
                            return true;
                        } else if (mCurrentCtr == CTR_LB) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_LB;
                            return true;
                        } else if (isHitAnnot(annot, point)) {
                            mTouchCaptured = true;
                            mLastOper = OPER_TRANSLATE;
                            return true;
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (pageIndex == annot.getPage().getIndex()
                            && mTouchCaptured
                            && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        if (evX != mLastPoint.x && evY != mLastPoint.y) {
                            RectF pageViewBBox = AppUtil.toRectF(annot.getRect());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                            float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                            switch (mLastOper) {

                                case OPER_TRANSLATE: {
                                    mInvalidateRect.set(pageViewBBox);
                                    mAnnotMenuRect.set(pageViewBBox);
                                    mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    mAnnotMenuRect.offset(evX - mDownPoint.x, evY - mDownPoint.y);
                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                    mInvalidateRect.union(mAnnotMenuRect);

                                    mInvalidateRect.inset(-deltaXY - mCtlPtDeltyXY, -deltaXY - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
                                        mAnnotMenu.update(mAnnotMenuRect);
                                    }

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    break;
                                }
                                case OPER_SCALE_LT: {

                                    float viewLeft = mPageViewRect.left;
                                    float viewTop = mPageViewRect.top;
                                    float viewRight = mPageViewRect.right;
                                    float viewBottom = mPageViewRect.bottom;

                                    float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                                    float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);
                                    float y = k * evX + b;

                                    if (evX != mLastPoint.x && evY != mLastPoint.y && y > deltaXY) {
                                        mInvalidateRect.set(mLastPoint.x, mLastPoint.x * k + b, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(evX, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RT: {
                                    float viewLeft = mPageViewRect.left;
                                    float viewTop = mPageViewRect.top;
                                    float viewRight = mPageViewRect.right;
                                    float viewBottom = mPageViewRect.bottom;

                                    float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                                    float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);
                                    float y = k * evX + b;

                                    if (evX != mLastPoint.x && evY != mLastPoint.y && y > deltaXY) {
                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.x * k + b, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, evY, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RB: {
                                    float viewLeft = mPageViewRect.left;
                                    float viewTop = mPageViewRect.top;
                                    float viewRight = mPageViewRect.right;
                                    float viewBottom = mPageViewRect.bottom;

                                    float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                                    float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);
                                    float y = k * evX + b;

                                    if (evX != mLastPoint.x && evY != mLastPoint.y && (y + deltaXY) < mPdfViewCtrl.getPageViewHeight(pageIndex)) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.x * k + b);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_LB: {
                                    float viewLeft = mPageViewRect.left;
                                    float viewTop = mPageViewRect.top;
                                    float viewRight = mPageViewRect.right;
                                    float viewBottom = mPageViewRect.bottom;

                                    float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                                    float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);
                                    float y = k * evX + b;

                                    if (evX != mLastPoint.x && evY != mLastPoint.y && (y + deltaXY) < mPdfViewCtrl.getPageViewHeight(pageIndex)) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.x * k + b);
                                        mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_DEFAULT:
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mTouchCaptured
                            && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                        pageViewRect.inset(mThickness / 2, mThickness / 2);

                        switch (mLastOper) {
                            case OPER_TRANSLATE: {
                                mPageDrawRect.set(pageViewRect);
                                mPageDrawRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                break;
                            }
                            case OPER_SCALE_LT: {
                                float viewLeft = mPageViewRect.left;
                                float viewTop = mPageViewRect.top;
                                float viewRight = mPageViewRect.right;
                                float viewBottom = mPageViewRect.bottom;

                                float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                                float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, k * mLastPoint.x + b, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RT: {
                                float viewLeft = mPageViewRect.left;
                                float viewTop = mPageViewRect.top;
                                float viewRight = mPageViewRect.right;
                                float viewBottom = mPageViewRect.bottom;

                                float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                                float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, k * mLastPoint.x + b, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RB: {
                                float viewLeft = mPageViewRect.left;
                                float viewTop = mPageViewRect.top;
                                float viewRight = mPageViewRect.right;
                                float viewBottom = mPageViewRect.bottom;

                                float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                                float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, mLastPoint.x * k + b);
                                }
                                break;
                            }
                            case OPER_SCALE_LB: {
                                float viewLeft = mPageViewRect.left;
                                float viewTop = mPageViewRect.top;
                                float viewRight = mPageViewRect.right;
                                float viewBottom = mPageViewRect.bottom;

                                float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                                float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, mLastPoint.x * k + b);
                                }
                                break;
                            }
                            default:
                                break;
                        }
                        if (mLastOper != OPER_DEFAULT && !mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                            RectF bboxRect = new RectF(viewDrawBox);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(bboxRect, bboxRect, pageIndex);

                            modifyAnnot(pageIndex, annot, bboxRect, false, false,
                                    Module.MODULE_NAME_MEDIA, null);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);

                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(viewDrawBox);
                            } else {
                                mAnnotMenu.show(viewDrawBox);
                            }

                        } else {
                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                            float _lineWidth = annot.getBorderInfo().getWidth();
                            viewDrawBox.inset(-thicknessOnPageView(pageIndex, _lineWidth) / 2, -thicknessOnPageView(pageIndex, _lineWidth) / 2);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(viewDrawBox);
                            } else {
                                mAnnotMenu.show(viewDrawBox);
                            }
                        }

                        mTouchCaptured = false;
                        mDownPoint.set(0, 0);
                        mLastPoint.set(0, 0);
                        mLastOper = OPER_DEFAULT;
                        mCurrentCtr = CTR_NONE;
                        return true;
                    }

                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mLastOper = OPER_DEFAULT;
                    mCurrentCtr = CTR_NONE;
                    mTouchCaptured = false;
                    return false;
                default:
            }
            return false;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (null == getToolHandler(annot)) {
            return false;
        }

        PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            try {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                } else {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(annot);
        }
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (null == getToolHandler(annot)) {
            return false;
        }

        if (AppUtil.isFastDoubleClick()) {
            return true;
        }
        try {
            PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
            if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {

                if ((pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt))) {
                    return true;
                } else {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    return true;
                }
            } else {
                openMultimedia(annot);
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private int isTouchControlPoint(RectF rect, float x, float y) {
        PointF[] ctlPts = calculateControlPoints(rect);
        RectF area = new RectF();
        int ret = -1;
        for (int i = 0; i < ctlPts.length; i++) {
            area.set(ctlPts[i].x, ctlPts[i].y, ctlPts[i].x, ctlPts[i].y);
            area.inset(-mCtlPtTouchExt, -mCtlPtTouchExt);
            if (area.contains(x, y)) {
                ret = i + 1;
            }
        }
        return ret;
    }

    /*
     *   1-----------2
     *   |	         |
     *   |	         |
     *   |          |
     *   |           |
     *   |           |
     *   4-----------3
     *   */
    private RectF mMapBounds = new RectF();

    private PointF[] calculateControlPoints(RectF rect) {
        rect.sort();
        mMapBounds.set(rect);
        mMapBounds.inset(-mCtlPtRadius - mCtlPtLineWidth / 2f, -mCtlPtRadius - mCtlPtLineWidth / 2f);// control rect
        PointF p1 = new PointF(mMapBounds.left, mMapBounds.top);
        PointF p2 = new PointF(mMapBounds.right, mMapBounds.top);
        PointF p3 = new PointF(mMapBounds.right, mMapBounds.bottom);
        PointF p4 = new PointF(mMapBounds.left, mMapBounds.bottom);

        return new PointF[]{p1, p2, p3, p4};
    }

    private PointF mAdjustPointF = new PointF(0, 0);

    private PointF adjustScalePointF(int pageIndex, RectF rectF, float dxy) {
        float adjustx = 0;
        float adjusty = 0;
        if (mLastOper != OPER_TRANSLATE) {
            rectF.inset(-mThickness / 2f, -mThickness / 2f);
        }

        if ((int) rectF.left < dxy) {
            adjustx = -rectF.left + dxy;
            rectF.left = dxy;
        }
        if ((int) rectF.top < dxy) {
            adjusty = -rectF.top + dxy;
            rectF.top = dxy;
        }

        if ((int) rectF.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - dxy) {
            adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectF.right - dxy;
            rectF.right = mPdfViewCtrl.getPageViewWidth(pageIndex) - dxy;
        }
        if ((int) rectF.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - dxy) {
            adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectF.bottom - dxy;
            rectF.bottom = mPdfViewCtrl.getPageViewHeight(pageIndex) - dxy;
        }
        mAdjustPointF.set(adjustx, adjusty);
        return mAdjustPointF;
    }

    private void openMultimedia(final Annot annot) {
        try {
            showProgressDlg();
            Screen screen = (Screen) annot;
            Action action = screen.getAction();
            if (action.isEmpty()) {
                dismissProgressDlg();
                UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.rv_document_open_failed));
            } else {
                RenditionAction renditionAction = new RenditionAction(action);
                if (renditionAction.isEmpty() || renditionAction.getRenditionCount() == 0) {
                    dismissProgressDlg();
                    UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.rv_document_open_failed));
                } else {
                    String filePath = mMultimediaTempPathMap.get(annot.getUniqueID());
                    Rendition rendition = renditionAction.getRendition(0);
                    final String mimeType = rendition.getMediaClipContentType();

                    if (filePath == null || filePath.isEmpty()) {
                        FileSpec fileSpec = rendition.getMediaClipFile();
                        String fileName = fileSpec.getFileName();
                        final String newFilePath = getTempPath(annot, fileName);

                        saveMultimediaAttachment(mPdfViewCtrl, newFilePath, fileSpec, new Event.Callback() {
                            @Override
                            public void result(Event event, boolean success) {
                                dismissProgressDlg();
                                if (success) {
                                    saveTempPath(annot, newFilePath);
                                    openFile(newFilePath, mimeType);
                                }
                            }
                        });
                    } else {
                        File file = new File(filePath);
                        if (file.exists()) {
                            dismissProgressDlg();
                            openFile(filePath, mimeType);
                        } else {
                            FileSpec fileSpec = rendition.getMediaClipFile();
                            String fileName = fileSpec.getFileName();
                            final String newFilePath = getTempPath(annot, fileName);

                            saveMultimediaAttachment(mPdfViewCtrl, newFilePath, fileSpec, new Event.Callback() {
                                @Override
                                public void result(Event event, boolean success) {
                                    dismissProgressDlg();
                                    if (success) {
                                        saveTempPath(annot, newFilePath);
                                        openFile(newFilePath, mimeType);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
            dismissProgressDlg();
            UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.rv_document_open_failed));
        }
    }

    private void openFile(String path, String mimeType) {
        if (mMultimediaUtil.canPlaySimpleAudio(path)) {
            mPlayFilePath = path;
            if (isBindService) {
                mAudioPlayer.stop();
                mediaPlayer_view.setVisibility(View.GONE);
                startPlayAudio();
            } else {
                mHandler.sendEmptyMessage(START_SERVICE);
            }
        } else {
            exitPlay();
            Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            if (context == null) return;
            if (mimeType != null && !mimeType.isEmpty()) {
                if (mimeType.contains("video")) {
                    mimeType = "video/*";
                } else {
                    mimeType = "audio/*";
                }
            }
            AppIntentUtil.openFile((Activity) context, path, mimeType);
        }
    }

    private void saveTempPath(Annot annot, String tmpPath) {
        String uuid = null;
        try {
            uuid = annot.getUniqueID();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        if (uuid == null || uuid.isEmpty()) {
            uuid = AppDmUtil.randomUUID("");
        }
        mMultimediaTempPathMap.put(uuid, tmpPath);
    }

    private String getTempPath(Annot annot, String filename) {
        String tempPath = "";
        String uuid = null;
        try {
            tempPath = Environment.getExternalStorageDirectory() + "/FoxitSDK/AttaTmp/";
            uuid = annot.getUniqueID();
        } catch (PDFException e) {
            e.printStackTrace();
        }

        if (uuid == null || uuid.isEmpty()) {
            uuid = AppDmUtil.randomUUID("");
        }
        tempPath = tempPath + uuid + "/";
        File file = new File(tempPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return tempPath + filename;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }

    private RectF mBBoxInOnDraw = new RectF();
    private RectF mViewDrawRectInOnDraw = new RectF();
    private DrawFilter mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null || !(annot instanceof Screen)) {
            return;
        }

        try {
            int annotPageIndex = annot.getPage().getIndex();
            if (AppAnnotUtil.equals(mCurrentAnnot, annot) && annotPageIndex == pageIndex) {
                canvas.save();
                canvas.setDrawFilter(mDrawFilter);
                RectF rect2 = AppUtil.toRectF(annot.getRect());
                float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
                mPdfViewCtrl.convertPdfRectToPageViewRect(rect2, rect2, pageIndex);
                rect2.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                RectF _rect = AppUtil.toRectF(annot.getRect());
                mViewDrawRectInOnDraw.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRectInOnDraw, mViewDrawRectInOnDraw, pageIndex);
                mViewDrawRectInOnDraw.inset(thickness / 2f, thickness / 2f);

                if (mLastOper == OPER_SCALE_LT) {// SCALE
                    float viewLeft = mViewDrawRectInOnDraw.left;
                    float viewTop = mViewDrawRectInOnDraw.top;
                    float viewRight = mViewDrawRectInOnDraw.right;
                    float viewBottom = mViewDrawRectInOnDraw.bottom;

                    float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                    float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                    mBBoxInOnDraw.set(mLastPoint.x, k * mLastPoint.x + b, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_RT) {
                    float viewLeft = mViewDrawRectInOnDraw.left;
                    float viewTop = mViewDrawRectInOnDraw.top;
                    float viewRight = mViewDrawRectInOnDraw.right;
                    float viewBottom = mViewDrawRectInOnDraw.bottom;

                    float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                    float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.x * k + b, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_RB) {
                    float viewLeft = mViewDrawRectInOnDraw.left;
                    float viewTop = mViewDrawRectInOnDraw.top;
                    float viewRight = mViewDrawRectInOnDraw.right;
                    float viewBottom = mViewDrawRectInOnDraw.bottom;

                    float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                    float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mLastPoint.x * k + b);
                } else if (mLastOper == OPER_SCALE_LB) {
                    float viewLeft = mViewDrawRectInOnDraw.left;
                    float viewTop = mViewDrawRectInOnDraw.top;
                    float viewRight = mViewDrawRectInOnDraw.right;
                    float viewBottom = mViewDrawRectInOnDraw.bottom;

                    float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                    float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                    mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.x * k + b);
                }
                mBBoxInOnDraw.inset(-thickness / 2f, -thickness / 2f);
                if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {// TRANSLATE or DEFAULT
                    mBBoxInOnDraw = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxInOnDraw, mBBoxInOnDraw, pageIndex);
                    float dx = mLastPoint.x - mDownPoint.x;
                    float dy = mLastPoint.y - mDownPoint.y;

                    mBBoxInOnDraw.offset(dx, dy);
                }
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    drawControlPoints(canvas, mBBoxInOnDraw);
                    // add Control Imaginary
                    drawControlImaginary(canvas, mBBoxInOnDraw);
                }

                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (curAnnot != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this) {

            try {
                int annotPageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                    float thickness = thicknessOnPageView(annotPageIndex, curAnnot.getBorderInfo().getWidth());
                    RectF _rect = AppUtil.toRectF(curAnnot.getRect());
                    mViewDrawRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRect, mViewDrawRect, annotPageIndex);
                    mViewDrawRect.inset(thickness / 2f, thickness / 2f);

                    if (mLastOper == OPER_SCALE_LT) {
                        float viewLeft = mViewDrawRect.left;
                        float viewTop = mViewDrawRect.top;
                        float viewRight = mViewDrawRect.right;
                        float viewBottom = mViewDrawRect.bottom;

                        float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                        float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mLastPoint.x * k + b;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_RT) {
                        float viewLeft = mViewDrawRectInOnDraw.left;
                        float viewTop = mViewDrawRectInOnDraw.top;
                        float viewRight = mViewDrawRectInOnDraw.right;
                        float viewBottom = mViewDrawRectInOnDraw.bottom;

                        float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                        float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mLastPoint.x * k + b;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_RB) {
                        float viewLeft = mViewDrawRect.left;
                        float viewTop = mViewDrawRect.top;
                        float viewRight = mViewDrawRect.right;
                        float viewBottom = mViewDrawRect.bottom;

                        float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                        float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mLastPoint.x * k + b;
                    } else if (mLastOper == OPER_SCALE_LB) {
                        float viewLeft = mViewDrawRectInOnDraw.left;
                        float viewTop = mViewDrawRectInOnDraw.top;
                        float viewRight = mViewDrawRectInOnDraw.right;
                        float viewBottom = mViewDrawRectInOnDraw.bottom;

                        float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                        float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mLastPoint.x * k + b;
                    }
                    mDocViewerBBox.inset(-thickness / 2f, -thickness / 2f);
                    if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {
                        mDocViewerBBox = AppUtil.toRectF(curAnnot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mDocViewerBBox, mDocViewerBBox, annotPageIndex);

                        float dx = mLastPoint.x - mDownPoint.x;
                        float dy = mLastPoint.y - mDownPoint.y;

                        mDocViewerBBox.offset(dx, dy);
                    }

                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewerBBox, mDocViewerBBox, annotPageIndex);
                    mAnnotMenu.update(mDocViewerBBox);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    private void drawControlPoints(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
        for (PointF ctlPt : ctlPts) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            int color = Color.parseColor("#179CD8");
            mCtlPtPaint.setColor(color);
            mCtlPtPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    private Path mImaginaryPath = new Path();

    private void drawControlImaginary(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mFrmPaint.setStrokeWidth(mCtlPtLineWidth);
        int color = Color.parseColor("#179CD8");
        mFrmPaint.setColor(color);
        mImaginaryPath.reset();
        // set path
        pathAddLine(mImaginaryPath, ctlPts[0].x + mCtlPtRadius, ctlPts[0].y, ctlPts[1].x - mCtlPtRadius, ctlPts[1].y);
        pathAddLine(mImaginaryPath, ctlPts[1].x, ctlPts[1].y + mCtlPtRadius, ctlPts[2].x, ctlPts[2].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[2].x - mCtlPtRadius, ctlPts[2].y, ctlPts[3].x + mCtlPtRadius, ctlPts[3].y);
        pathAddLine(mImaginaryPath, ctlPts[3].x, ctlPts[3].y - mCtlPtRadius, ctlPts[0].x, ctlPts[0].y + mCtlPtRadius);

        canvas.drawPath(mImaginaryPath, mFrmPaint);
    }

    private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);
    }

    private void saveMultimediaAttachment(PDFViewCtrl pdfViewCtrl, final String newFile, final FileSpec fileSpec, final Event.Callback callback) {
        Task.CallBack callBack = new Task.CallBack() {
            @Override
            public void result(Task task) {
                if (callback != null) {
                    callback.result(null, true);
                }
            }
        };
        Task task = new Task(callBack) {
            @Override
            protected void execute() {
                try {
                    if (fileSpec == null)
                        return;
                    FileReaderCallback fileRead = fileSpec.getFileData();
                    if (fileRead == null)
                        return;
                    FileOutputStream fileOutputStream = new FileOutputStream(newFile);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                    int offset = 0;
                    int bufSize = 4 * 1024;
                    long fileSize = fileRead.getSize();
                    byte[] buf;

                    while (true) {
                        if (fileSize < bufSize + offset) {
                            buf = new byte[(int) (fileSize - offset)];
                            fileRead.readBlock(buf, offset, fileSize - offset);
                        } else {
                            buf = new byte[bufSize];
                            fileRead.readBlock(buf, offset, bufSize);
                        }
                        if (buf.length != bufSize) {
                            bufferedOutputStream.write(buf, 0, buf.length);
                            break;
                        } else {
                            bufferedOutputStream.write(buf, 0, bufSize);
                        }
                        offset += bufSize;
                    }
                    bufferedOutputStream.flush();

                    bufferedOutputStream.close();
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        pdfViewCtrl.addTask(task);
    }

    private void resetAnnotationMenuResource(Annot annot) {
        mMenuText.clear();

        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
            mMenuText.add(AnnotMenu.AM_BT_PALY);
            mMenuText.add(AnnotMenu.AM_BT_FLATTEN);
            if (!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot))) {
                mMenuText.add(AnnotMenu.AM_BT_DELETE);
            }
        } else {
            mMenuText.add(AnnotMenu.AM_BT_PALY);
        }
    }

    private float thicknessOnPageView(int pageIndex, float thickness) {
        mThicknessRectF.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mThicknessRectF, mThicknessRectF, pageIndex);
        return Math.abs(mThicknessRectF.width());
    }

    private void showProgressDlg() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();

        if (mProgressDlg == null && uiExtensionsManager.getAttachedActivity() != null) {
            mProgressDlg = new ProgressDialog(uiExtensionsManager.getAttachedActivity());
            mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDlg.setCancelable(false);
            mProgressDlg.setIndeterminate(false);
        }

        if (mProgressDlg != null && !mProgressDlg.isShowing()) {
            mProgressDlg.setMessage(mContext.getApplicationContext().getString(R.string.fx_string_opening));
            AppDialogManager.getInstance().showAllowManager(mProgressDlg, null);
        }
    }

    private void dismissProgressDlg() {
        if (mProgressDlg != null && mProgressDlg.isShowing()) {
            AppDialogManager.getInstance().dismiss(mProgressDlg);
            mProgressDlg = null;
        }
    }

    private ToolHandler getToolHandler(Annot annot) {
        ToolHandler toolHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getToolHandlerByType(AppAnnotUtil.getTypeToolName(annot));
        return toolHandler;
    }

    private SeekBar mediaPlayer_seekbar;
    private TextView mediaPlayer_pastTime;
    private TextView mediaPlayer_totalTime;
    private ImageView mediaPlayer_playbtn;
    private ImageView mediaPlayer_slowbtn;
    private ImageView mediaPlayer_speedbtn;
    private ImageView mediaPlayer_stopbtn;
    private View mediaPlayer_view;

    private AudioPlayService mAudioPlayer;
    private boolean isBindService = false;

    private void _initMediaPlayer() {
        //init View
        ViewGroup readerView = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getMainFrame().getContentView();

        mediaPlayer_view = View.inflate(mContext, R.layout.audio_play_layout, null);
        int width = AppDisplay.getInstance(mContext).dp2px(260);
        int height = AppDisplay.getInstance(mContext).dp2px(80);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, height);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        if (AppDisplay.getInstance(mContext).isPad()) {
            lp.setMargins(0, 0, 0, (int) AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_pad) + AppDisplay.getInstance(mContext).dp2px(16));
        } else {
            lp.setMargins(0, 0, 0, AppDisplay.getInstance(mContext).dp2px(110));
        }
        readerView.addView(mediaPlayer_view, lp);
        mediaPlayer_view.setVisibility(View.GONE);

        mediaPlayer_pastTime = (TextView) mediaPlayer_view.findViewById(R.id.audio_play_pasttime);
        mediaPlayer_totalTime = (TextView) mediaPlayer_view.findViewById(R.id.audio_play_totaltime);
        mediaPlayer_playbtn = (ImageView) mediaPlayer_view.findViewById(R.id.audio_play_pause);
        mediaPlayer_slowbtn = (ImageView) mediaPlayer_view.findViewById(R.id.audio_play_slow);
        mediaPlayer_speedbtn = (ImageView) mediaPlayer_view.findViewById(R.id.audio_play_speed);
        mediaPlayer_stopbtn = (ImageView) mediaPlayer_view.findViewById(R.id.audio_play_stop);

        //init play status
        mediaPlayer_seekbar = (SeekBar) mediaPlayer_view.findViewById(R.id.audio_play_seekbar);
        mediaPlayer_seekbar.setOnSeekBarChangeListener(mSeekbarChangedListener);

        mediaPlayer_playbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAudioPlayer.isPlaying()) {
                    mediaPlayer_playbtn.setImageResource(R.drawable.audio_player_play_selector);
                    mAudioPlayer.pause();
                } else {
                    mediaPlayer_playbtn.setImageResource(R.drawable.audio_player_pause_selector);
                    mAudioPlayer.seekTo(mAudioPlayer.getCurrentPosition());
                    try {
                        mAudioPlayer.start();
                        mHandler.sendEmptyMessage(CHANGE_UI_STATE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mediaPlayer_stopbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitPlay();
            }
        });

        mediaPlayer_slowbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = mAudioPlayer.getCurrentPosition();
                pos -= 5000;

                if (pos < 0) {
                    pos = 0;
                }
                mAudioPlayer.seekTo(pos);
                mediaPlayer_seekbar.setProgress(pos);
                mediaPlayer_pastTime.setText(timeParse(pos));
            }
        });

        mediaPlayer_speedbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int pos = mAudioPlayer.getCurrentPosition();
                pos += 5000;

                if (pos > mAudioPlayer.getDuration()) {
                    pos = mAudioPlayer.getDuration();
                }
                mAudioPlayer.seekTo(pos);
                mediaPlayer_seekbar.setProgress(pos);
                mediaPlayer_pastTime.setText(timeParse(pos));
            }
        });
    }

    private boolean fromUser;
    private SeekBar.OnSeekBarChangeListener mSeekbarChangedListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (fromUser) {
                mAudioPlayer.seekTo(mediaPlayer_seekbar.getProgress());
                mediaPlayer_pastTime.setText(timeParse(mAudioPlayer.getCurrentPosition()));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            fromUser = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            fromUser = false;
        }
    };

    private void startAudioService() {
        if (isBindService) {
            stopAudioService();
        }
        Intent intent = new Intent(mContext, AudioPlayService.class);
        mContext.startService(intent);
        isBindService = true;
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopAudioService() {
        Intent intent = new Intent(mContext, AudioPlayService.class);
        if (isBindService) {
            mContext.unbindService(mServiceConnection);
            isBindService = false;
        }
        mContext.stopService(intent);
        mAudioPlayer = null;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mAudioPlayer = ((AudioPlayService.AudioPlayBinder) service).getService();
            mAudioPlayer.setAudioStatusChangeListener(new AudioPlayService.IAudioStatusChangeListener() {
                @Override
                public void replay() {
                    if (View.VISIBLE == mediaPlayer_view.getVisibility()) {
                        mHandler.sendEmptyMessage(CHANGE_UI_STATE);
                    }
                }

                @Override
                public void pause() {
                    if (View.VISIBLE == mediaPlayer_view.getVisibility()) {
                        mHandler.sendEmptyMessageDelayed(CHANGE_UI_STATE, 100);
                    }
                }
            });
            mHandler.sendEmptyMessage(SERVICE_CONNECTED);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mHandler.sendEmptyMessage(SERVICE_DISCONNECTED);
        }
    };

    protected void exitPlay() {
        mediaPlayer_view.setVisibility(View.GONE);
        if (mAudioPlayer != null) {
            mAudioPlayer.stop();
            mHandler.sendEmptyMessage(STOP_SERVICE);
        }
    }

    private ILifecycleEventListener mLifecycleEventListener = new ILifecycleEventListener() {
        @Override
        public void onCreate(Activity act, Bundle savedInstanceState) {
            _initMediaPlayer();
        }

        @Override
        public void onStart(Activity act) {
        }

        @Override
        public void onPause(Activity act) {
        }

        @Override
        public void onResume(Activity act) {
            if (View.VISIBLE == mediaPlayer_view.getVisibility()) {
                mHandler.sendEmptyMessageDelayed(CHANGE_UI_STATE, 100);
            }
        }

        @Override
        public void onStop(Activity act) {
        }

        @Override
        public void onDestroy(Activity act) {
        }

        @Override
        public void onSaveInstanceState(Activity act, Bundle bundle) {
        }

        @Override
        public void onHiddenChanged(boolean hidden) {
            if (View.VISIBLE == mediaPlayer_view.getVisibility() && !hidden) {
                mHandler.sendEmptyMessageDelayed(CHANGE_UI_STATE, 100);
            }
        }

        @Override
        public void onActivityResult(Activity act, int requestCode, int resultCode, Intent data) {

        }
    };

    private static final int START_SERVICE = 111;
    private static final int STOP_SERVICE = 222;
    private static final int SERVICE_CONNECTED = 333;
    private static final int SERVICE_DISCONNECTED = 444;
    private static final int CHANGE_UI_STATE = 555;
    private String mPlayFilePath;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case START_SERVICE:
                    startAudioService();
                    break;
                case STOP_SERVICE:
                    stopAudioService();
                    break;
                case SERVICE_CONNECTED:
                    startPlayAudio();
                    break;
                case SERVICE_DISCONNECTED:
                    break;
                case CHANGE_UI_STATE:
                    changeUIState();
                    break;
                default:
                    break;
            }
        }
    };

    private void startPlayAudio() {
        try {
            showProgressDlg();
            mAudioPlayer.prepare(mPlayFilePath, new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    dismissProgressDlg();
                    mediaPlayer_view.setVisibility(View.VISIBLE);
                    mediaPlayer_seekbar.setMax(mAudioPlayer.getDuration());
                    mediaPlayer_seekbar.setProgress(0);

                    //set totalTime
                    mediaPlayer_totalTime.setText(timeParse(mAudioPlayer.getDuration()));
                    //play file
                    mAudioPlayer.start();
                    mHandler.sendEmptyMessage(CHANGE_UI_STATE);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            mediaPlayer_view.setVisibility(View.GONE);
        }
    }

    private void changeUIState() {
        if (!mediaPlayer_view.isShown()) return;

        if (!mAudioPlayer.isPlaying()) {
            mediaPlayer_playbtn.setImageResource(R.drawable.audio_player_play_selector);
            if (mAudioPlayer.getCurrentPosition() + 1000 > mAudioPlayer.getDuration()) {
                //end
                mediaPlayer_pastTime.setText(timeParse(mAudioPlayer.getDuration()));
                try {
                    mAudioPlayer.pause();
                    mAudioPlayer.seekTo(0);
                    mediaPlayer_pastTime.setText(timeParse(0));
                    mediaPlayer_seekbar.setProgress(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            mediaPlayer_playbtn.setImageResource(R.drawable.audio_player_pause_selector);
            mediaPlayer_pastTime.setText(timeParse(mAudioPlayer.getCurrentPosition()));
            mediaPlayer_seekbar.setProgress(mAudioPlayer.getCurrentPosition());

            mHandler.sendEmptyMessageDelayed(CHANGE_UI_STATE, 100);
        }
    }

    private String timeParse(long duration) {
        String time = "";
        long minute = duration / 60000;
        long seconds = duration % 60000;
        long second = Math.round((float) seconds / 1000);
        if (minute < 10) {
            time += "0";
        }
        time += minute + ":";
        if (second < 10) {
            time += "0";
        }
        time += second;
        return time;
    }

    protected void unregisterLifecycleListener() {
        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).unregisterLifecycleListener(mLifecycleEventListener);
    }

}
