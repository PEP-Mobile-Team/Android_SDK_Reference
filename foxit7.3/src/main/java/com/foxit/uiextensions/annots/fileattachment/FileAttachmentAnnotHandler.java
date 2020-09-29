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
package com.foxit.uiextensions.annots.fileattachment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.FileSpec;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FileAttachment;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.modules.PageNavigationModule;
import com.foxit.uiextensions.modules.panel.filespec.FileSpecOpenView;
import com.foxit.uiextensions.modules.panel.filespec.IAttachmentDocEvent;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppIntentUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class FileAttachmentAnnotHandler implements AnnotHandler {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private Paint mPaintBbox;
    private Paint mPaintAnnot;
    private ArrayList<Integer> mMenuItems;
    private Annot mBitmapAnnot;
    private PropertyBar mAnnotPropertyBar;
    private int mModifyColor;
    private float mModifyOpacity;
    private String mModifyIconName;
    private int mBBoxSpace;
    private boolean mIsAnnotModified;
    private FileAttachmentToolHandler mFileAttachmentToolHandler;
    private boolean mIsEditProperty;
    private AnnotMenu mAnnotMenu;
    private FileAttachmentModule mFileAttachmentModule;
    private PointF mDownPoint;
    private PointF mLastPoint;
    private boolean mTouchCaptured = false;
    private String mTmpPath;

    private FileSpecOpenView mOpenView;
    private ProgressDialog mProgressDlg;

    private HashMap<String, String> mAttachmentPath = new HashMap<>();

    public FileAttachmentAnnotHandler(Context context, PDFViewCtrl pdfViewer, FileAttachmentModule fileAttachmentModule) {
        mContext = context;
        mPdfViewCtrl = pdfViewer;
        mFileAttachmentModule = fileAttachmentModule;
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        AppAnnotUtil annotUtil = new AppAnnotUtil(mContext);
        mPaintBbox.setStrokeWidth(annotUtil.getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());

        mPaintAnnot = new Paint();
        mPaintAnnot.setStyle(Paint.Style.STROKE);
        mPaintAnnot.setAntiAlias(true);
        mDrawLocalTmpF = new RectF();

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mMenuItems = new ArrayList<Integer>();
        mAnnotMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);
        mAnnotMenu.setMenuItems(mMenuItems);

        mTmpPath = Environment.getExternalStorageDirectory() + "/FoxitSDK/AttaTmp/";

        mAnnotPropertyBar = fileAttachmentModule.getPropertyBar();
    }

    public String getTmpPath() {
        return mTmpPath;
    }

    public void deleteTmpPath() {
        File file = new File(mTmpPath);
        deleteDir(file);
    }

    private void deleteDir(File path) {
        if (!path.exists())
            return;
        if (path.isFile()) {
            path.delete();
            return;
        }
        File[] files = path.listFiles();
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            deleteDir(files[i]);
        }
        path.delete();
    }

    private FileAttachmentPBAdapter mPropertyListViewAdapter;

    public void setPropertyListViewAdapter(FileAttachmentPBAdapter adapter) {
        mPropertyListViewAdapter = adapter;
    }

    @Override
    public int getType() {
        return Annot.e_FileAttachment;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            return AppUtil.toRectF(annot.getRect());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        RectF rectF = new RectF();

        if (mPdfViewCtrl != null) {
            try {
                int index = annot.getPage().getIndex();
                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(index);
                rectF = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                rectF.inset(-10, -10);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return rectF.contains(point.x, point.y);
    }

    public void resetMenuItems(FileAttachment fileAttachment) {
        mMenuItems.clear();

        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
        } else {
            mMenuItems.add(AnnotMenu.AM_BT_STYLE);
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            mMenuItems.add(AnnotMenu.AM_BT_FLATTEN);
            if (!(AppAnnotUtil.isLocked(fileAttachment) || AppAnnotUtil.isReadOnly(fileAttachment))) {
                mMenuItems.add(AnnotMenu.AM_BT_DELETE);
            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mOpenView == null) {
            return false;
        } else if (mOpenView.getVisibility() == View.VISIBLE && keyCode == KeyEvent.KEYCODE_BACK) {
            mOpenView.closeAttachment();
            mOpenView.setVisibility(View.GONE);
            return true;
        }

        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                return true;
            }
        }
        return false;
    }

    public boolean onKeyBack() {
        if (mOpenView == null) {
            return false;
        } else if (mOpenView.getVisibility() == View.VISIBLE) {
            mOpenView.closeAttachment();
            mOpenView.setVisibility(View.GONE);
            return true;
        }

        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
            return true;
        }
        return false;
    }

    private int mTmpUndoColor;
    private float mTmpUndoOpacity;
    private String mTmpUndoIconName;
    private RectF mTmpUndoBbox;
    private String mTmpUndoModifiedDate;
    private String mTmpContents;

    @Override
    public void onAnnotSelected(final Annot annotation, boolean reRender) {
        if (!(annotation instanceof FileAttachment)) return;

        final FileAttachment annot = (FileAttachment) annotation;
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = annot.getOpacity();
            mTmpUndoIconName = annot.getIconName();
            mTmpContents = annot.getContent();
            mTmpUndoBbox = AppUtil.toRectF(annot.getRect());
            mTmpUndoModifiedDate = AppDmUtil.getLocalDateString(annot.getModifiedDateTime());
            mModifyIconName = mTmpUndoIconName;
            mModifyOpacity = mTmpUndoOpacity;
            mModifyColor = mTmpUndoColor;

            mBitmapAnnot = annot;
            mAnnotPropertyBar.setArrowVisible(false);
            resetMenuItems(annot);
            mAnnotMenu.setMenuItems(mMenuItems);

            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    try {
                        final int pageIndex = annotation.getPage().getIndex();
                        if (btType == AnnotMenu.AM_BT_COPY) {
                            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setPrimaryClip(ClipData.newPlainText(null, annot.getContent()));

                            AppAnnotUtil.toastAnnotCopy(mContext);
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        } else if (btType == AnnotMenu.AM_BT_DELETE) {
                            deleteAnnot(annot, true, null);
                        } else if (btType == AnnotMenu.AM_BT_STYLE) {
                            mAnnotMenu.dismiss();
                            mIsEditProperty = true;
                            mAnnotPropertyBar.setEditable(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
                            System.arraycopy(PropertyBar.PB_COLORS_FILEATTACHMENT, 0, mPBColors, 0, mPBColors.length);
                            mPBColors[0] = PropertyBar.PB_COLORS_FILEATTACHMENT[0];
                            mAnnotPropertyBar.setColors(mPBColors);
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, annot.getBorderColor());
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (annot.getOpacity() * 255f + 0.5f)));
                            mAnnotPropertyBar.reset(PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY);

                            mAnnotPropertyBar.addTab("", 0, mContext.getApplicationContext().getString(R.string.pb_icon_tab), 0);
                            mPropertyListViewAdapter.setNoteIconType(FileAttachmentUtil.getIconType(annot.getIconName()));
                            boolean canEdit = !(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot));
                            mAnnotPropertyBar.addCustomItem(PropertyBar.PROPERTY_FILEATTACHMENT, mFileAttachmentModule.getIconTypeView(canEdit), 0, 0);

                            mAnnotPropertyBar.setPropertyChangeListener(mFileAttachmentModule);
                            RectF annotRectF = AppUtil.toRectF(annot.getRect());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, pageIndex);

                            RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annotRectF);
                            mAnnotPropertyBar.show(rectF, false);
                        } else if (btType == AnnotMenu.AM_BT_COMMENT) {

                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                            onOpenAttachment(annot);

                        } else if (AnnotMenu.AM_BT_FLATTEN == btType) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                            UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            });

            int pageIndex = annot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
                RectF annotRectF = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, pageIndex);
                mAnnotMenu.show(annotRectF);

                RectF modifyRectF = AppUtil.toRectF(annot.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(modifyRectF, modifyRectF, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(modifyRectF));
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mBitmapAnnot = annot;
                }
            } else {
                mBitmapAnnot = annot;
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        if (annot == null || !(annot instanceof FileAttachment))
            return;
        mAnnotMenu.dismiss();

        if (mIsEditProperty) {
            mIsEditProperty = false;
        }
        try {
            PDFPage page = annot.getPage();
            if (page == null || page.isEmpty()) return;
            int pageIndex = page.getIndex();

            if (mIsAnnotModified && reRender) {
                if (mTmpUndoColor != mModifyColor
                        || mTmpUndoOpacity != mModifyOpacity
                        || !mTmpUndoIconName.equals(mModifyIconName)
                        || !mTmpUndoBbox.equals(AppUtil.toRectF(annot.getRect()))) {
                    modifyAnnot(annot, mModifyColor, mModifyOpacity, mModifyIconName, null, annot.getContent(), true, null);
                }
            } else if (mIsAnnotModified) {
                annot.setBorderColor(mTmpUndoColor);
                ((FileAttachment) annot).setOpacity(AppDmUtil.opacity100To255((int) mTmpUndoOpacity) / 255f);
                ((FileAttachment) annot).setIconName(mTmpUndoIconName);

                annot.move(AppUtil.toFxRectF(mTmpUndoBbox));
                annot.setModifiedDateTime(AppDmUtil.parseDocumentDate(mTmpUndoModifiedDate));
                annot.resetAppearanceStream();
            }
            mIsAnnotModified = false;
            RectF rect = AppUtil.toRectF(annot.getRect());
            mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, pageIndex);
            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(rect));

            mBitmapAnnot = null;

        } catch (PDFException e) {
            e.printStackTrace();
        }
        mBitmapAnnot = null;

    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {

        if (mFileAttachmentToolHandler != null) {
            mFileAttachmentToolHandler.addAnnot(pageIndex, content.getBBox(), result);
        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (annot == null || !(annot instanceof FileAttachment))
            return;
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = ((FileAttachment) annot).getOpacity();
            mTmpUndoIconName = ((FileAttachment) annot).getIconName();
            mTmpUndoBbox = AppUtil.toRectF(annot.getRect());
            mTmpContents = annot.getContent();
            mTmpUndoModifiedDate = AppDmUtil.getLocalDateString(annot.getModifiedDateTime());
            mIsAnnotModified = true;

            if (content instanceof UIAnnotReply.CommentContent) {
                UIAnnotReply.CommentContent commentContent = (UIAnnotReply.CommentContent) content;
                modifyAnnot(annot, (int) annot.getBorderColor(), ((FileAttachment) annot).getOpacity(), ((FileAttachment) annot).getIconName(),
                        AppDmUtil.getLocalDateString(commentContent.getModifiedDate()), commentContent.getContents(), addUndo, result);
            } else {
                modifyAnnot(annot, (int) annot.getBorderColor(), ((FileAttachment) annot).getOpacity(), ((FileAttachment) annot).getIconName(),
                        AppDmUtil.getLocalDateString(annot.getModifiedDateTime()), annot.getContent(), addUndo, result);
            }

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
        int action = motionEvent.getActionMasked();
        PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF point = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, point, pageIndex);
        PointF pageViewPt = new PointF(point.x, point.y);
        try {
            float envX = point.x;
            float envY = point.y;

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        try {
                            if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                                mDownPoint.set(envX, envY);
                                mLastPoint.set(envX, envY);
                                mTouchCaptured = true;
                                return true;
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    try {
                        if (mTouchCaptured && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                                && pageIndex == annot.getPage().getIndex()
                                && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                            if (envX != mLastPoint.x || envY != mLastPoint.y) {
                                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
                                RectF pageViewRectF = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                                RectF rectInv = new RectF(pageViewRectF);
                                RectF rectChanged = new RectF(pageViewRectF);

                                rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                rectChanged.offset(envX - mDownPoint.x, envY - mDownPoint.y);

                                float adjustx = 0;
                                float adjusty = 0;
                                if (rectChanged.left < 0) {
                                    adjustx = -rectChanged.left;
                                }
                                if (rectChanged.top < 0) {
                                    adjusty = -rectChanged.top;
                                }
                                if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex)) {
                                    adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right;
                                }
                                if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex)) {
                                    adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom;
                                }
                                rectChanged.offset(adjustx, adjusty);
                                rectInv.union(rectChanged);
                                rectInv.inset(-mBBoxSpace - 3, -mBBoxSpace - 3);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));

                                RectF rectInViewerF = new RectF(rectChanged);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                                if (mAnnotMenu.isShowing()) {
                                    mAnnotMenu.dismiss();
                                    mAnnotMenu.update(rectInViewerF);
                                }
                                if (mIsEditProperty) {
                                    mAnnotPropertyBar.dismiss();
                                }
                                mLastPoint.set(envX, envY);
                                mLastPoint.offset(adjustx, adjusty);
                            }
                            return true;
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                    return false;
                case MotionEvent.ACTION_UP:
                    if (mTouchCaptured && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() &&
                            annot.getPage().getIndex() == pageIndex
                            && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                        Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
                        RectF pageViewRectF = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));

                        RectF rectInv = new RectF(pageViewRectF);
                        RectF rectChanged = new RectF(pageViewRectF);

                        rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        rectChanged.offset(envX - mDownPoint.x, envY - mDownPoint.y);
                        float adjustx = 0;
                        float adjusty = 0;
                        if (rectChanged.left < 0) {
                            adjustx = -rectChanged.left;
                        }
                        if (rectChanged.top < 0) {
                            adjusty = -rectChanged.top;
                        }
                        if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex)) {
                            adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right;
                        }
                        if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex)) {
                            adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom;
                        }
                        rectChanged.offset(adjustx, adjusty);
                        rectInv.union(rectChanged);
                        rectInv.inset(-mBBoxSpace - 3, -mBBoxSpace - 3);

                        Rect invalidateRect = AppDmUtil.rectFToRect(rectInv);
                        mPdfViewCtrl.refresh(pageIndex, invalidateRect);
                        RectF rectInViewerF = new RectF(rectChanged);

                        RectF canvasRectF = new RectF();
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, canvasRectF, pageIndex);
                        if (mIsEditProperty) {
                            RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), canvasRectF);
                            if (mAnnotPropertyBar.isShowing()) {
                                mAnnotPropertyBar.update(rectF);
                            } else {
                                mAnnotPropertyBar.show(rectF, false);
                            }
                        } else {
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(canvasRectF);
                            } else {
                                mAnnotMenu.show(canvasRectF);
                            }
                        }

                        RectF rect = new RectF();
                        AppAnnotUtil.convertPageViewRectToPdfRect(mPdfViewCtrl, annot, rectChanged, rect);
                        if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {

                            mIsAnnotModified = true;
                            annot.move(AppUtil.toFxRectF(rect));
                            pageViewRectF.inset(-mBBoxSpace - 3, -mBBoxSpace - 3);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(pageViewRectF));
                            mModifyColor = (int) annot.getBorderColor();
                            mModifyOpacity = ((FileAttachment) annot).getOpacity();
                            mModifyIconName = ((FileAttachment) annot).getIconName();
                        }

                        mTouchCaptured = false;
                        mDownPoint.set(0, 0);
                        mLastPoint.set(0, 0);
                        return true;
                    }
                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    return false;
                case MotionEvent.ACTION_CANCEL:
                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    return false;
                default:
            }
        } catch (PDFException e1) {
            if (e1.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
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
        if (AppUtil.isFastDoubleClick()) {
            return true;
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
            onOpenAttachment(annot);
        }
        return true;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }


    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null) return;
        try {
            int index = annot.getPage().getIndex();
            if (AppAnnotUtil.equals(mBitmapAnnot, annot) && index == pageIndex) {
                canvas.save();
                RectF frameRectF = new RectF();
                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
                RectF rect = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                rect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                frameRectF.set(rect.left - mBBoxSpace, rect.top - mBBoxSpace, rect.right + mBBoxSpace, rect.bottom + mBBoxSpace);
                int color = (int) (annot.getBorderColor() | 0xFF000000);
                mPaintBbox.setColor(color);
                canvas.drawRect(frameRectF, mPaintBbox);
                canvas.restore();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private RectF mDrawLocalTmpF;

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();

        try {
            if (curAnnot != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this) {
                int pageIndex = curAnnot.getPage().getIndex();
                if (pageIndex == mPdfViewCtrl.getCurrentPage()) {

                    Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
                    RectF rect = AppUtil.toRectF(curAnnot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                    rect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                    rect.inset(-mBBoxSpace - 3, -mBBoxSpace - 3);

                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, pageIndex);

                    mDrawLocalTmpF.set(rect);
                    if (mIsEditProperty) {
                        RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mDrawLocalTmpF);
                        if (mAnnotPropertyBar.isShowing()) {
                            mAnnotPropertyBar.update(rectF);
                        } else {
                            mAnnotPropertyBar.show(rectF, false);
                        }
                    } else {
                        if (mAnnotMenu.isShowing()) {
                            mAnnotMenu.update(rect);
                        } else {
                            mAnnotMenu.show(rect);
                        }
                    }

                } else {
                    mAnnotMenu.dismiss();
                    if (mIsEditProperty) {
                        mAnnotPropertyBar.dismiss();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void setToolHandler(FileAttachmentToolHandler toolHandler) {
        mFileAttachmentToolHandler = toolHandler;
    }

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_FILEATTACHMENT.length];


    public void modifyAnnotColor(int color) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null) return;
        mModifyColor = color;
        try {
            if (mModifyColor != annot.getBorderColor()) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyColor);
                annot.resetAppearanceStream();
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void modifyAnnotOpacity(int opacity) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null || !(annot instanceof FileAttachment)) return;
        mModifyOpacity = AppDmUtil.opacity100To255(opacity) / 255f;
        try {
            if (mModifyOpacity != ((FileAttachment) annot).getOpacity()) {
                mIsAnnotModified = true;
                ((FileAttachment) annot).setOpacity(mModifyOpacity);
                annot.resetAppearanceStream();
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void modifyIconType(int type) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null) return;
        mModifyIconName = FileAttachmentUtil.getIconName(type);

        try {
            if (!mModifyIconName.equals(((FileAttachment) annot).getIconName())) {
                mIsAnnotModified = true;
                ((FileAttachment) annot).setIconName(mModifyIconName);
                annot.resetAppearanceStream();
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void invalidateForToolModify(Annot annot) {
        if (annot == null) return;
        try {
            int pageIndex = annot.getPage().getIndex();
            if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;

            RectF rectF = AppUtil.toRectF(annot.getRect());
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);

            Rect rect = rectRoundOut(rectF, mBBoxSpace);
            rect.inset(-1, -1);
            mPdfViewCtrl.refresh(pageIndex, rect);

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private Rect rectRoundOut(RectF rectF, int roundSize) {
        Rect rect = new Rect();
        rectF.roundOut(rect);
        rect.inset(-roundSize, -roundSize);
        return rect;
    }

    private void deleteAnnot(final Annot annot, final Boolean addUndo, final Event.Callback result) {
        final DocumentManager dmDoc = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        if (annot == dmDoc.getCurrentAnnot()) dmDoc.setCurrentAnnot(null, false);
        try {
            final PDFPage page = annot.getPage();
            if (page == null || page.isEmpty()) {
                if (result != null) {
                    result.result(null, false);
                }
                return;
            }

            final RectF annotRectF = AppUtil.toRectF(annot.getRect());
            final FileAttachmentDeleteUndoItem undoItem = new FileAttachmentDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mIconName = ((FileAttachment) annot).getIconName();
//            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.attacheName = ((FileAttachment) annot).getFileSpec().getFileName();
            if (AppAnnotUtil.isGrouped(annot))
                undoItem.mGroupNMList = GroupManager.getInstance().getGroupUniqueIDs(mPdfViewCtrl, annot);

            final String filePath = mAttachmentPath.get(AppAnnotUtil.getAnnotUniqueID(annot));
            if (filePath == null || filePath.isEmpty()) {
                String fileName = ((FileAttachment) annot).getFileSpec().getFileName();
                final String tmpPath = getTempPath(annot, fileName);

                FileAttachmentUtil.saveAttachment(mPdfViewCtrl, tmpPath, annot, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        saveTempPath(annot, tmpPath);
                        undoItem.mPath = tmpPath;
                        deleteAnnot(undoItem, annot, annotRectF, addUndo, result);
                    }
                });
            } else {
                File file = new File(filePath);
                if (file.exists()) {
                    undoItem.mPath = filePath;
                    deleteAnnot(undoItem, annot, annotRectF, addUndo, result);
                } else {
                    String fileName = ((FileAttachment) annot).getFileSpec().getFileName();
                    final String tmpPath = getTempPath(annot, fileName);

                    FileAttachmentUtil.saveAttachment(mPdfViewCtrl, tmpPath, annot, new Event.Callback() {
                        @Override
                        public void result(Event event, boolean success) {
                            saveTempPath(annot, tmpPath);
                            undoItem.mPath = tmpPath;
                            deleteAnnot(undoItem, annot, annotRectF, addUndo, result);
                        }
                    });
                }
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }

    }

    private void deleteAnnot(final FileAttachmentDeleteUndoItem undoItem, final Annot annot, final RectF annotRectF, final boolean addUndo, final Event.Callback result) {
        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();
            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
            final RectF deviceRectF = new RectF();
            if (matrix != null) {
                deviceRectF.set(AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix))));
            }
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            FileAttachmentEvent event = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (FileAttachment) annot, mPdfViewCtrl);
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(event, true);
                }
                return;
            }
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (undoItem.mGroupNMList.size() >= 2) {
                            ArrayList<String> newGroupList = new ArrayList<>(undoItem.mGroupNMList);
                            newGroupList.remove(undoItem.mNM);
                            if (newGroupList.size() >= 2)
                                GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, newGroupList);
                            else
                                GroupManager.getInstance().unGroup(page, newGroupList.get(0));
                        }

                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (addUndo) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }

                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
//                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(deviceRectF));
                        }
                    }

                    if (result != null) {
                        result.result(event, success);
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

    private void modifyAnnot(final Annot annot, int color, float opacity, String iconName, String modifyDate,
                             String content, final boolean addUndo, final Event.Callback callback) {
        try {
            PDFPage page = annot.getPage();
            if (null == page || page.isEmpty()) return;
            final FileAttachmentModifyUndoItem undoItem = new FileAttachmentModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mContents = content;
            undoItem.mIconName = iconName;
            undoItem.mColor = color;
            undoItem.mOpacity = opacity;

            undoItem.mRedoContents = content;
            undoItem.mRedoColor = color;
            undoItem.mRedoOpacity = opacity;
            undoItem.mRedoIconName = iconName;
            undoItem.mRedoBbox = AppUtil.toRectF(annot.getRect());
            undoItem.mUndoColor = mTmpUndoColor;
            undoItem.mUndoOpacity = mTmpUndoOpacity;
            undoItem.mUndoIconName = mTmpUndoIconName;
            undoItem.mUndoBbox = mTmpUndoBbox;
            undoItem.mUndoContents = mTmpContents;
            FileAttachmentEvent event = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (FileAttachment) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {

                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        try {
                            if (addUndo) {
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                            }

                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);
                            if (callback != null) {
                                callback.result(event, success);
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);

        } catch (PDFException e) {
            e.printStackTrace();
        }
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

    private void onOpenAttachment(final Annot annot) {
        try {
            showProgressDlg();
            String filePath = mAttachmentPath.get(AppAnnotUtil.getAnnotUniqueID(annot));
            if (filePath == null || filePath.isEmpty()) {
                FileSpec fileSpec = ((FileAttachment) annot).getFileSpec();
                String fileName = fileSpec.getFileName();
                final String newFilePath = getTempPath(annot, fileName);

                FileAttachmentUtil.saveAttachment(mPdfViewCtrl, newFilePath, annot, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            saveTempPath(annot, newFilePath);
                            openFile(newFilePath);
                        } else {
                            dismissProgressDlg();
                        }
                    }
                });
            } else {
                File file = new File(filePath);
                if (file.exists()) {
                    openFile(filePath);
                } else {
                    FileSpec fileSpec = ((FileAttachment) annot).getFileSpec();
                    String fileName = fileSpec.getFileName();
                    final String newFilePath = getTempPath(annot, fileName);

                    FileAttachmentUtil.saveAttachment(mPdfViewCtrl, newFilePath, annot, new Event.Callback() {
                        @Override
                        public void result(Event event, boolean success) {
                            if (success) {
                                saveTempPath(annot, newFilePath);
                                openFile(newFilePath);
                            }else {
                                dismissProgressDlg();
                            }
                        }
                    });
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private int mOldState;

    private void openFile(String path) {
        String ExpName = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
        if (ExpName.equals("pdf") || ExpName.equals("ppdf")) {
            if (mOpenView == null) {
                mOpenView = new FileSpecOpenView(mContext, mPdfViewCtrl, ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView());
            }

            mOpenView.setVisibility(View.VISIBLE);
            final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
            mOpenView.openAttachment(path, AppFileUtil.getFileName(path), new IAttachmentDocEvent() {
                @Override
                public void onAttachmentDocWillOpen() {
                    uiExtensionsManager.getMainFrame().getPanelManager().hidePanel();
                    mOldState = uiExtensionsManager.getState();
                    showProgressDlg();
                }

                @Override
                public void onAttachmentDocOpened(PDFDoc document, int errCode) {
                    dismissProgressDlg();
                    if (errCode == Constants.e_ErrSuccess) {
                        uiExtensionsManager.changeState(ReadStateConfig.STATE_NORMAL);
                        PageNavigationModule module = (PageNavigationModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_PAGENAV);
                        if (module != null) {
                            module.changPageNumberState(false);
                        }
                    }
                }

                @Override
                public void onAttachmentDocWillClose() {
                    uiExtensionsManager.changeState(mOldState);
                }

                @Override
                public void onAttachmentDocClosed() {
                }
            });
        } else {
            dismissProgressDlg();
            if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
            Activity context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            if (context == null) return;
            AppIntentUtil.openFile(context, path);
        }
    }

    private String getTempPath(Annot annot, String filename) {
        String tempPath = Environment.getExternalStorageDirectory() + "/FoxitSDK/AttaTmp/";
        String uuid = AppAnnotUtil.getAnnotUniqueID(annot);
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

    private void saveTempPath(Annot annot, String tmpPath) {
        mAttachmentPath.put(AppAnnotUtil.getAnnotUniqueID(annot), tmpPath);
    }

}
