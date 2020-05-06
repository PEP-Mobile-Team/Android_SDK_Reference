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
package com.foxit.uiextensions.annots.form;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.Control;
import com.foxit.sdk.pdf.interform.Field;
import com.foxit.sdk.pdf.interform.Filler;
import com.foxit.sdk.pdf.interform.Form;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.form.undo.FormFillerDeleteUndoItem;
import com.foxit.uiextensions.annots.form.undo.FormFillerModifyUndoItem;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;


public class FormFillerAnnotHandler implements AnnotHandler {
    private static final int DEFAULT_COLOR = PropertyBar.PB_COLORS_TOOL_GROUP_1[6];
    private static final int DEFAULT_THICKNESS = 4;

    /*
     *  LT     T     RT
     *   1-----2-----3
     *   |	         |
     *   |	         |
     * L 8           4 R
     *   |           |
     *   |           |
     *   7-----6-----5
     *   LB    B     RB
     *   */
    public static final int CTR_NONE = -1;
    public static final int CTR_LT = 1;
    public static final int CTR_T = 2;
    public static final int CTR_RT = 3;
    public static final int CTR_R = 4;
    public static final int CTR_RB = 5;
    public static final int CTR_B = 6;
    public static final int CTR_LB = 7;
    public static final int CTR_L = 8;
    private int mCurrentCtr = CTR_NONE;

    public static final int OPER_DEFAULT = -1;
    public static final int OPER_SCALE_LT = 1;// old:start at 0
    public static final int OPER_SCALE_T = 2;
    public static final int OPER_SCALE_RT = 3;
    public static final int OPER_SCALE_R = 4;
    public static final int OPER_SCALE_RB = 5;
    public static final int OPER_SCALE_B = 6;
    public static final int OPER_SCALE_LB = 7;
    public static final int OPER_SCALE_L = 8;
    public static final int OPER_TRANSLATE = 9;
    private int mLastOper = OPER_DEFAULT;

    private float mCtlPtLineWidth = 2;
    private float mCtlPtRadius = 5;
    private float mCtlPtTouchExt = 20;
    private float mCtlPtDeltyXY = 20;// Additional refresh range

    private Paint mFrmPaint;// outline
    private Paint mCtlPtPaint;

    private boolean mTouchCaptured = false;
    private PointF mDownPoint;
    private PointF mLastPoint;

    private ArrayList<Integer> mMenuText;
    private AnnotMenu mAnnotationMenu;

    private Annot mLastAnnot;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private Filler mFormFiller;
    private FormFillerAssistImpl mAssist;
    private Form mForm;
    private EditText mEditView = null;
    private PointF mLastTouchPoint = new PointF(0, 0);
    private FormNavigationModule mFNModule = null;
    private int mPageOffset;
    private boolean mIsBackBtnPush = false; //for some input method, double backspace click
    private boolean mAdjustPosition = false;
    private boolean mIsShowEditText = false;
    private String mLastInputText = "";
    private String mChangeText = null;
    private Paint mPathPaint;
    private boolean bInitialize = false;
    private int mOffset;
    private Blink mBlink = null;

    private boolean mIsModify = false;
    private boolean mIsLongPressTouchEvent = false;

    public FormFillerAnnotHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
    }

    public void init(final Form form) {
        mAssist = new FormFillerAssistImpl(mPdfViewCtrl);
        mAssist.bWillClose = false;
        mForm = form;
        mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setAntiAlias(true);
        mPathPaint.setDither(true);
        PathEffect effects = new DashPathEffect(new float[]{1, 2, 4, 8}, 1);
        mPathPaint.setPathEffect(effects);

        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Paint.Style.STROKE);
        mFrmPaint.setAntiAlias(true);
        mFrmPaint.setColor(DEFAULT_COLOR | 0xFF000000);

        mCtlPtPaint = new Paint();
        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mMenuText = new ArrayList<Integer>();
        mAnnotationMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);

        try {
            mFormFiller = new Filler(form, mAssist);

            boolean mEnableFormHighlight = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).isFormHighlightEnable();
            mFormFiller.highlightFormFields(mEnableFormHighlight);
            if (mEnableFormHighlight) {
                mFormFiller.setHighlightColor((int) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getFormHighlightColor());
            }
        } catch (PDFException e) {
            e.printStackTrace();
            return;
        }

        initFormNavigation();
        bInitialize = true;
    }

    private void initFormNavigation() {
        mFNModule = (FormNavigationModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_FORM_NAVIGATION);
        if (mFNModule != null) {
            mFNModule.hide();
            mFNModule.getPreView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppThreadManager.getInstance().startThread(preNavigation);
                }
            });

            mFNModule.getNextView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppThreadManager.getInstance().startThread(nextNavigation);
                }
            });

            mFNModule.getClearView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
                    if (annot instanceof Widget) {
                        try {
                            PDFViewCtrl.lock();
                            Control formControl = ((Widget) annot).getControl();
                            mFormFiller.killFocus();
                            Field field = formControl.getField();
                            field.reset();
                            mFormFiller.setFocus(formControl);
                            refreshField(field);
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
                        } catch (PDFException e) {
                            e.printStackTrace();
                        } finally {
                            PDFViewCtrl.unlock();
                        }
                    }
                }
            });

            mFNModule.getFinishView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() != null) {
                        if (shouldShowInputSoft(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot())) {
                            if (mBlink != null) {
                                mBlink.removeCallbacks((Runnable) mBlink);
                                mBlink = null;
                            }
                            AppUtil.dismissInputSoft(mEditView);
                            mParent.removeView(mEditView);
                        }
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    }
                    mFNModule.hide();
                    resetDocViewerOffset();
                }
            });

            mFNModule.setClearEnable(false);
        }

        ViewGroup parent = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView();
        AppKeyboardUtil.setKeyboardListener(parent, parent, new AppKeyboardUtil.IKeyboardListener() {
            @Override
            public void onKeyboardOpened(int keyboardHeight) {
                if (mFNModule != null) {
                    mFNModule.setPadding(0, 0, 0, getFNBottomPadding());
                }
            }

            @Override
            public void onKeyboardClosed() {
                if (mFNModule != null) {
                    mFNModule.setPadding(0, 0, 0, 0);
                }
            }
        });
    }

    protected boolean hasInitialized() {
        return bInitialize;
    }

    protected void showSoftInput() {
        AppUtil.showSoftInput(mEditView);
    }

    private void postDismissNavigation() {
        DismissNavigation dn = new DismissNavigation();
        dn.postDelayed(dn, 500);
    }

    private class DismissNavigation extends Handler implements Runnable {

        @Override
        public void run() {
            if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) return;
            Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
            if (!(annot instanceof Widget)) {
                if (mFNModule != null)
                    mFNModule.getLayout().setVisibility(View.INVISIBLE);
                AppUtil.dismissInputSoft(mEditView);
                resetDocViewerOffset();
            }
        }
    }

    private void modifyAnnot(final int pageIndex, final Annot annot, RectF bboxRect, String value,
                             boolean isModifyJni, final boolean addUndo, final Event.Callback result) {
        try {
            final FormFillerModifyUndoItem undoItem = new FormFillerModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.mBBox = new RectF(bboxRect);
            undoItem.mValue = value;
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

            undoItem.mRedoBbox = new RectF(bboxRect);
            undoItem.mRedoValue = value;

            undoItem.mUndoValue = mTempValue;
            undoItem.mUndoBbox = new RectF(mTempLastBBox);

            final PDFPage page = annot.getPage();

            if (isModifyJni) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(addUndo);

                FormFillerEvent event = new FormFillerEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Widget) annot, mPdfViewCtrl);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(page, annot);

                            if (addUndo) {
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                            }

                            RectF tempRectF = mTempLastBBox;
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

            mIsModify = true;
            if (!isModifyJni) {
                RectF oldRect = AppUtil.toRectF(annot.getRect());
                annot.move(AppUtil.toFxRectF(bboxRect));
                annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                annot.resetAppearanceStream();

                RectF annotRectF = AppUtil.toRectF(annot.getRect());
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());

                    mPdfViewCtrl.convertPdfRectToPageViewRect(oldRect, oldRect, pageIndex);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                    annotRectF.union(oldRect);
                    annotRectF.inset(-thickness - mCtlPtRadius - mCtlPtDeltyXY, -thickness - mCtlPtRadius - mCtlPtDeltyXY);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                }
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    private void deleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        final DocumentManager documentManager = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        if (annot == documentManager.getCurrentAnnot()) {
            documentManager.setCurrentAnnot(null, false);
        }

        try {
            final PDFPage page = annot.getPage();
            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            final int pageIndex = page.getIndex();

            final FormFillerDeleteUndoItem undoItem = new FormFillerDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.mFieldName = ((Widget)annot).getField().getName();
            undoItem.mFieldType = ((Widget)annot).getField().getType();
            undoItem.mValue =((Widget)annot).getField().getValue();
            undoItem.mRotation = ((Widget)annot).getMKRotation();

            documentManager.onAnnotWillDelete(page, annot);
            FormFillerEvent event = new FormFillerEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Widget) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        documentManager.onAnnotDeleted(page, annot);
                        if (addUndo) {
                            documentManager.addUndoItem(undoItem);
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


    private boolean shouldShowNavigation(Annot annot) {
        if (annot == null) return false;
        if (!(annot instanceof Widget)) return false;
        if (FormFillerUtil.getAnnotFieldType(mForm, annot) == Field.e_TypePushButton) return false;

        return true;
    }

    public void navigationDismiss() {
        if (mFNModule != null) {
            mFNModule.hide();
            mFNModule.setPadding(0, 0, 0, 0);
        }
        if (mBlink != null) {
            mBlink.removeCallbacks((Runnable) mBlink);
            mBlink = null;
        }
        if (mEditView != null) {
            mParent.removeView(mEditView);
        }
        resetDocViewerOffset();
        AppUtil.dismissInputSoft(mEditView);
    }

    private boolean isFind = false;
    private boolean isDocFinish = false;
    private PDFPage curPage = null;
    private int prePageIdx;
    private int preAnnotIdx;
    private int nextPageIdx;
    private int nextAnnotIdx;
    private CountDownLatch mCountDownLatch;
    private Runnable preNavigation = new Runnable() {

        @Override
        public void run() {

            Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
            try {
                if (curAnnot instanceof Widget) {
                    curPage = curAnnot.getPage();
                    final int curPageIdx = curPage.getIndex();
                    prePageIdx = curPageIdx;
                    final int curAnnotIdx = curAnnot.getIndex();
                    preAnnotIdx = curAnnotIdx;
                    isFind = false;
                    isDocFinish = false;
                    while (prePageIdx >= 0) {
                        mCountDownLatch = new CountDownLatch(1);
                        curPage = mPdfViewCtrl.getDoc().getPage(prePageIdx);
                        if (prePageIdx == curPageIdx && !isDocFinish) {
                            preAnnotIdx = curAnnotIdx - 1;
                        } else {
                            preAnnotIdx = curPage.getAnnotCount() - 1;
                        }

                        while (curPage != null && preAnnotIdx >= 0) {
                            final Annot preAnnot = AppAnnotUtil.createAnnot(curPage.getAnnot(preAnnotIdx));
                            final int preAnnotType = FormFillerUtil.getAnnotFieldType(mForm, preAnnot);
                            if ((preAnnot instanceof Widget)
                                    && (!FormFillerUtil.isReadOnly(preAnnot))
                                    && FormFillerUtil.isVisible(preAnnot)
                                    && (preAnnotType != Field.e_TypePushButton)
                                    && (preAnnotType != Field.e_TypeSignature)) {
                                isFind = true;
                                AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {

                                    @Override
                                    public void run() {
                                        try {
                                            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                                            if (preAnnotType == Field.e_TypeComboBox) {
                                                RectF rect = AppUtil.toRectF(preAnnot.getRect());
                                                rect.left += 5;
                                                rect.top -= 5;
                                                mLastTouchPoint.set(rect.left, rect.top);
                                            }
                                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                                            if (!preAnnot.isEmpty()) {
                                                if (uiExtensionsManager.getCurrentToolHandler() != null) {
                                                    uiExtensionsManager.setCurrentToolHandler(null);
                                                }
                                                RectF bbox = AppUtil.toRectF(preAnnot.getRect());
                                                RectF rect = new RectF(bbox);

                                                if (mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, prePageIdx)) {
                                                    float devX = rect.left - (mPdfViewCtrl.getWidth() - rect.width()) / 2;
                                                    float devY = rect.top - (mPdfViewCtrl.getHeight() - rect.height()) / 2;
                                                    mPdfViewCtrl.gotoPage(prePageIdx, devX, devY);
                                                } else {
                                                    mPdfViewCtrl.gotoPage(prePageIdx, new PointF(bbox.left, bbox.top));
                                                }

                                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(preAnnot);
                                                if (preAnnot instanceof Widget) {
                                                    mFormFiller.setFocus(((Widget) preAnnot).getControl());
                                                }
                                            }
                                        } catch (PDFException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                                break;
                            } else {
                                preAnnotIdx--;
                            }
                        }
                        mCountDownLatch.countDown();

                        try {
                            if (mCountDownLatch.getCount() > 0)
                                mCountDownLatch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (isFind) break;
                        prePageIdx--;
                        if (prePageIdx < 0) {
                            prePageIdx = mPdfViewCtrl.getDoc().getPageCount() - 1;
                            isDocFinish = true;
                        }
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    };

    private void refreshField(Field field) {
        int nPageCount = mPdfViewCtrl.getPageCount();
        for (int i = 0; i < nPageCount; i++) {
            if (!mPdfViewCtrl.isPageVisible(i)) continue;
            RectF rectF = getRefreshRect(field, i);
            if (rectF == null) continue;
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, i);
            mPdfViewCtrl.refresh(i, AppDmUtil.rectFToRect(rectF));
        }
    }

    private RectF getRefreshRect(Field field, int pageIndex) {
        RectF rectF = null;
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            int nControlCount = field.getControlCount(page);
            for (int i = 0; i < nControlCount; i++) {
                Control formControl = field.getControl(page, i);
                if (rectF == null) {
                    rectF = AppUtil.toRectF(formControl.getWidget().getRect());
                } else {
                    rectF.union(AppUtil.toRectF(formControl.getWidget().getRect()));
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rectF;
    }

    private Runnable nextNavigation = new Runnable() {

        @Override
        public void run() {
            Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
            try {
                if (curAnnot instanceof Widget) {
                    curPage = curAnnot.getPage();

                    final int curPageIdx = curPage.getIndex();
                    nextPageIdx = curPageIdx;
                    final int curAnnotIdx = curAnnot.getIndex();
                    nextAnnotIdx = curAnnotIdx;
                    isFind = false;
                    isDocFinish = false;

                    while (nextPageIdx < mPdfViewCtrl.getDoc().getPageCount()) {

                        mCountDownLatch = new CountDownLatch(1);
                        curPage = mPdfViewCtrl.getDoc().getPage(nextPageIdx);
                        if (nextPageIdx == curPageIdx && !isDocFinish) {
                            nextAnnotIdx = curAnnotIdx + 1;
                        } else {
                            nextAnnotIdx = 0;
                        }

                        while (curPage != null && nextAnnotIdx < curPage.getAnnotCount()) {
                            final Annot nextAnnot = AppAnnotUtil.createAnnot(curPage.getAnnot(nextAnnotIdx));
                            final int nextAnnotType = FormFillerUtil.getAnnotFieldType(mForm, nextAnnot);
                            if (nextAnnot instanceof Widget
                                    && !FormFillerUtil.isReadOnly(nextAnnot)
                                    && FormFillerUtil.isVisible(nextAnnot)
                                    && nextAnnotType != Field.e_TypePushButton
                                    && nextAnnotType != Field.e_TypeSignature) {
                                isFind = true;

                                AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {

                                    @Override
                                    public void run() {
                                        try {
                                            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                                            if (nextAnnotType == Field.e_TypeComboBox) {
                                                RectF rect = AppUtil.toRectF(nextAnnot.getRect());
                                                rect.left += 5;
                                                rect.top -= 5;
                                                mLastTouchPoint.set(rect.left, rect.top);
                                            }
                                            uiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                                            if (!nextAnnot.isEmpty()) {
                                                if (uiExtensionsManager.getCurrentToolHandler() != null) {
                                                    uiExtensionsManager.setCurrentToolHandler(null);
                                                }
                                                RectF bbox = AppUtil.toRectF(nextAnnot.getRect());
                                                RectF rect = new RectF(bbox);

                                                if (mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, nextPageIdx)) {
                                                    float devX = rect.left - (mPdfViewCtrl.getWidth() - rect.width()) / 2;
                                                    float devY = rect.top - (mPdfViewCtrl.getHeight() - rect.height()) / 2;
                                                    mPdfViewCtrl.gotoPage(nextPageIdx, devX, devY);
                                                } else {
                                                    mPdfViewCtrl.gotoPage(nextPageIdx, new PointF(bbox.left, bbox.top));
                                                }

                                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(nextAnnot);
                                                if (nextAnnot instanceof Widget) {
                                                    mFormFiller.setFocus(((Widget) nextAnnot).getControl());
                                                }
                                            }
                                        } catch (PDFException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                });

                                break;
                            } else {
                                nextAnnotIdx++;
                            }
                        }
                        mCountDownLatch.countDown();

                        try {
                            if (mCountDownLatch.getCount() > 0)
                                mCountDownLatch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (isFind) break;
                        nextPageIdx++;
                        if (nextPageIdx >= mPdfViewCtrl.getDoc().getPageCount()) {
                            nextPageIdx = 0;
                            isDocFinish = true;
                        }
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    };

    protected void clear() {
        if (mAssist != null) {
            mAssist.bWillClose = true;
        }

        bInitialize = false;
        mForm = null;

        if (mFormFiller != null) {
            mFormFiller = null;
        }
    }

    public FormFillerAssistImpl getFormFillerAssist() {
        return mAssist;
    }

    @Override
    public int getType() {
        return Annot.e_Widget;
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
        try {
            RectF r = AppUtil.toRectF(annot.getRect());
            RectF rf = new RectF(r.left, r.top, r.right, r.bottom);
            PointF p = new PointF(point.x, point.y);
            int pageIndex = annot.getPage().getIndex();
            Control control = AppAnnotUtil.getControlAtPos(annot.getPage(), p, 1);

            mPdfViewCtrl.convertPdfRectToPageViewRect(rf, rf, pageIndex);
            mPdfViewCtrl.convertPdfPtToPageViewPt(p, p, pageIndex);

            if (rf.contains(p.x, p.y)) {
                return true;
            } else {
                if (AppAnnotUtil.isSameAnnot(annot, control != null ? control.getWidget() : null))
                    return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }


    public void onBackspaceBtnDown() {
        try {
            mFormFiller.onChar((char) 8, 0);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private boolean mOldIsHideSystem;

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        try {
            RectF rectF = AppUtil.toRectF(annot.getRect());
            mTempLastBBox = new RectF(rectF);
            mTempValue = ((Widget) annot).getField().getValue();

            if (TextUtils.isEmpty(annot.getUniqueID())){
                annot.setUniqueID(AppDmUtil.randomUUID(null));
            }

            if (mIsLongPressTouchEvent) {
                onAnnotSeletedByLongPress(annot, needInvalid);
            } else {
                onAnnotSeletedBySingleTap(annot, needInvalid);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void onAnnotSeletedBySingleTap(final Annot annot, boolean needInvalid) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        MainFrame mainFrame = (MainFrame) uiExtensionsManager.getMainFrame();
        mOldIsHideSystem = mainFrame.isHideSystemUI();

        if (shouldShowInputSoft(annot)) {
            if (mainFrame.isToolbarsVisible()) {
                mainFrame.setHideSystemUI(false);
            } else {
                AppUtil.showSystemUI(uiExtensionsManager.getAttachedActivity());
            }

            mIsShowEditText = true;
            mAdjustPosition = true;
            mLastInputText = " ";

            if (mEditView != null) {
                mParent.removeView(mEditView);
            }
            mEditView = new EditText(mContext);
            mEditView.setLayoutParams(new LayoutParams(1, 1));
            mEditView.setSingleLine(false);
            mEditView.setText(" ");
            mEditView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                    if (actionId == EditorInfo.IME_ACTION_DONE) {
//                        AppThreadManager.getInstance().startThread(nextNavigation);
//                        return true;
//                    }
                    return false;
                }
            });

            mParent.addView(mEditView);
            showSoftInput();

            mEditView.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                        onBackspaceBtnDown();
                        mIsBackBtnPush = true;
                    }
                    return false;
                }
            });

            mEditView.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        if (s.length() >= mLastInputText.length()) {
                            String afterchange = s.subSequence(start, start + before).toString();
                            if (mChangeText.equals(afterchange)) {
                                for (int i = 0; i < s.length() - mLastInputText.length(); i++) {
                                    char c = s.charAt(mLastInputText.length() + i);
                                    if (FormFillerUtil.isEmojiCharacter((int) c))
                                        break;
                                    if ((int) c == 10)
                                        c = 13;
                                    final char value = c;

                                    mFormFiller.onChar(value, 0);
                                }
                            } else {
                                for (int i = 0; i < before; i++) {
                                    onBackspaceBtnDown();
                                }
                                for (int i = 0; i < count; i++) {
                                    char c = s.charAt(s.length() - count + i);

                                    if (FormFillerUtil.isEmojiCharacter((int) c))
                                        break;
                                    if ((int) c == 10)
                                        c = 13;
                                    final char value = c;

                                    mFormFiller.onChar(value, 0);
                                }
                            }
                        } else if (s.length() < mLastInputText.length()) {

                            if (!mIsBackBtnPush) {
                                for (int i = 0; i < before; i++) {
                                    onBackspaceBtnDown();
                                }

                                for (int i = 0; i < count; i++) {
                                    char c = s.charAt(s.length() - count + i);

                                    if (FormFillerUtil.isEmojiCharacter((int) c))
                                        break;
                                    if ((int) c == 10)
                                        c = 13;
                                    final char value = c;

                                    mFormFiller.onChar(value, 0);
                                }
                            }
                            mIsBackBtnPush = false;
                        }

                        if (s.toString().length() == 0)
                            mLastInputText = " ";
                        else
                            mLastInputText = s.toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count,
                                              int after) {
                    mChangeText = s.subSequence(start, start + count).toString();
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.toString().length() == 0)
                        s.append(" ");
                }
            });

            if (mBlink == null) {
                mBlink = new Blink(annot);
                mBlink.postDelayed((Runnable) mBlink, 300);
            } else {
                mBlink.setAnnot(annot);
            }
        }

        int fieldType = FormFillerUtil.getAnnotFieldType(mForm, annot);

        if (mFNModule != null) {
            if (!FormFillerUtil.isReadOnly(annot))
                mFNModule.setClearEnable(true);
            else
                mFNModule.setClearEnable(false);
            if (fieldType != Field.e_TypePushButton)
                mFNModule.show();
        }
    }

    private String mTempValue;
    private RectF mTempLastBBox = new RectF();
    private RectF mPageViewRect = new RectF(0, 0, 0, 0);

    private void onAnnotSeletedByLongPress(final Annot annot, boolean needInvalid) {
        mCtlPtRadius = AppDisplay.getInstance(mContext).dp2px(mCtlPtRadius);
        mCtlPtDeltyXY = AppDisplay.getInstance(mContext).dp2px(mCtlPtDeltyXY);
        try {
            mPageViewRect.set(AppUtil.toRectF(annot.getRect()));
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
            prepareAnnotMenu(annot);
            RectF menuRect = new RectF(mPageViewRect);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
            mAnnotationMenu.show(menuRect);

            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(mPageViewRect));
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mLastAnnot = annot;
                }
            } else {
                mLastAnnot = annot;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void prepareAnnotMenu(final Annot annot) {
        try {
            mMenuText.clear();
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                if (!(AppAnnotUtil.isLocked(annot) || FormFillerUtil.isReadOnly(annot))) {

                    String uid = annot.getUniqueID();
                    if (!TextUtils.isEmpty(uid) && uid.contains(FormFillerModule.ID_TAG)){
                        mMenuText.add(AnnotMenu.AM_BT_DELETE);
                    }
                }
            }
            mAnnotationMenu.setMenuItems(mMenuText);
            mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    if (btType == AnnotMenu.AM_BT_DELETE) {
                        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                            deleteAnnot(annot, true, null);
                        }
                    }
                }
            });
        }catch (PDFException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(final Annot annot, boolean needInvalid) {
        mIsLongPressTouchEvent = false;
        mCtlPtRadius = 5;
        mCtlPtDeltyXY = 20;

        MainFrame mainFrame = (MainFrame) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getMainFrame();
        mainFrame.setHideSystemUI(mOldIsHideSystem);
        postDismissNavigation();
        if (mBlink != null) {
            mBlink.removeCallbacks((Runnable) mBlink);
            mBlink = null;
        }
        try {
            PDFPage page = annot.getPage();
            if (needInvalid && mIsModify) {
                // must calculate BBox again
                RectF rectF = AppUtil.toRectF(annot.getRect());
                String value = ((Widget) annot).getField().getValue();
                if (!mTempLastBBox.equals(rectF)/* || !mTempValue.equals(value)*/) {
                    modifyAnnot(page.getIndex(), annot, rectF, value, true, true, null);
                }
            } else if (mIsModify) {
                ((Widget) annot).getField().setValue(mTempValue);
                annot.move(AppUtil.toFxRectF(mTempLastBBox));
                annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                annot.resetAppearanceStream();
            }

            mFormFiller.killFocus();
            if (annot instanceof Widget)
                refreshField(((Widget) annot).getField());
            if (mIsShowEditText) {
                AppUtil.dismissInputSoft(mEditView);
                mParent.removeView(mEditView);
                mIsShowEditText = false;
            }
            if (mAnnotationMenu.isShowing()) {
                mAnnotationMenu.dismiss();
            }
            mLastAnnot = null;
            mIsModify = false;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!hasInitialized() || mFormFiller == null) return false;
        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canFillForm())
            return false;
        if (FormFillerUtil.isReadOnly(annot))
            return false;

        if (mIsLongPressTouchEvent) {
            return onTouchEventByLongPress(pageIndex, motionEvent, annot);
        } else {
            return onTouchEventBySingleTap(pageIndex, motionEvent, annot);
        }
    }

    private boolean isDown = false;
    private PointF oldPoint = new PointF();

    private boolean onTouchEventBySingleTap(int pageIndex, MotionEvent motionEvent, Annot annot) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);

            PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF pageViewPt = new PointF();
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, pageViewPt, pageIndex);
            PointF pdfPointF = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pdfPointF, pageIndex);

            int action = motionEvent.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pdfPointF)) {
                        isDown = true;
                        mFormFiller.onLButtonDown(page, AppUtil.toFxPointF(pdfPointF), 0);
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (getDistanceOfPoints(pageViewPt, oldPoint) > 0 && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        oldPoint.set(pageViewPt);
                        mFormFiller.onMouseMove(page, AppUtil.toFxPointF(pdfPointF), 0);
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    if (pageIndex == annot.getPage().getIndex() && (isHitAnnot(annot, pdfPointF) || isDown)) {
                        isDown = false;
                        mFormFiller.onLButtonUp(page, AppUtil.toFxPointF(pdfPointF), 0);
                        mIsModify = true;
                        if (shouldShowInputSoft(annot)) {
                            int keybordHeight = AppKeyboardUtil.getKeyboardHeight(mParent);
                            if (0 <= keybordHeight && keybordHeight < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
                                showSoftInput();
                            }
                        }
                        return true;
                    }
                    return false;
                 default:
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private PointF mDocViewerPt = new PointF(0, 0);
    private RectF mPageDrawRect = new RectF();
    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);
    private RectF mAnnotMenuRect = new RectF(0, 0, 0, 0);
    private float mThickness = 0f;


    private boolean onTouchEventByLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyForm()) return false;
        // in pageView evX and evY
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        float evX = point.x;
        float evY = point.y;

        PointF pdfPointF = new PointF();
        mPdfViewCtrl.convertPageViewPtToPdfPt(point, pdfPointF, pageIndex);

        int action = motionEvent.getAction();
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() && pageIndex == annot.getPage().getIndex()) {
                        mThickness = thicknessOnPageView(pageIndex, DEFAULT_THICKNESS);
                        RectF pageViewBBox = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                        RectF pdfRect = AppUtil.toRectF(annot.getRect());
                        mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
                        mPageViewRect.inset(mThickness / 2f, mThickness / 2f);

                        mCurrentCtr = isTouchControlPoint(pageViewBBox, evX, evY);

                        mDownPoint.set(evX, evY);
                        mLastPoint.set(evX, evY);
                        mDocViewerPt.set(motionEvent.getX(), motionEvent.getY());

                        if (mCurrentCtr == CTR_LT) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_LT;
                            return true;
                        } else if (mCurrentCtr == CTR_T) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_T;
                            return true;
                        } else if (mCurrentCtr == CTR_RT) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_RT;
                            return true;
                        } else if (mCurrentCtr == CTR_R) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_R;
                            return true;
                        } else if (mCurrentCtr == CTR_RB) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_RB;
                            return true;
                        } else if (mCurrentCtr == CTR_B) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_B;
                            return true;
                        } else if (mCurrentCtr == CTR_LB) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_LB;
                            return true;
                        } else if (mCurrentCtr == CTR_L) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_L;
                            return true;
                        } else if (isHitAnnot(annot, pdfPointF)) {
                            mTouchCaptured = true;
                            mLastOper = OPER_TRANSLATE;
                            return true;
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (pageIndex == annot.getPage().getIndex() && mTouchCaptured
                            && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
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
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    break;
                                }
                                case OPER_SCALE_LT: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(evX, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_T: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RT: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {

                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, evY, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_R: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RB: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.y);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_B: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_LB: {
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
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_L: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                default:
                                    break;
                            }
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    boolean ret = false;
                    if (mTouchCaptured && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() && pageIndex == annot.getPage().getIndex()) {
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
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_T: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RT: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_R: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RB: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_B: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_LB: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_L: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            default:
                                break;
                        }
                        RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                        float _lineWidth = DEFAULT_THICKNESS;
                        viewDrawBox.inset(-thicknessOnPageView(pageIndex, _lineWidth) / 2, -thicknessOnPageView(pageIndex, _lineWidth) / 2);
                        if (mLastOper != OPER_DEFAULT && !mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                            RectF bboxRect = new RectF(viewDrawBox);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(bboxRect, bboxRect, pageIndex);

                            modifyAnnot(pageIndex, annot, bboxRect, ((Widget) annot).getField().getValue(), false, false, null);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (mAnnotationMenu.isShowing()) {
                                mAnnotationMenu.update(viewDrawBox);
                            } else {
                                mAnnotationMenu.show(viewDrawBox);
                            }

                        } else {
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (mAnnotationMenu.isShowing()) {
                                mAnnotationMenu.update(viewDrawBox);
                            } else {
                                mAnnotationMenu.show(viewDrawBox);
                            }
                        }

                        ret = true;
                    }

                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mLastOper = OPER_DEFAULT;
                    mCurrentCtr = CTR_NONE;
                    return ret;
                default:
            }
            return false;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private RectF mThicknessRectF = new RectF();

    private float thicknessOnPageView(int pageIndex, float thickness) {
        mThicknessRectF.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mThicknessRectF, mThicknessRectF, pageIndex);
        return Math.abs(mThicknessRectF.width());
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
     *   1-----2-----3
     *   |	         |
     *   |	         |
     *   8           4
     *   |           |
     *   |           |
     *   7-----6-----5
     *   */
    RectF mMapBounds = new RectF();

    private PointF[] calculateControlPoints(RectF rect) {
        rect.sort();
        mMapBounds.set(rect);
        mMapBounds.inset(-mCtlPtRadius - mCtlPtLineWidth / 2f, -mCtlPtRadius - mCtlPtLineWidth / 2f);// control rect
        PointF p1 = new PointF(mMapBounds.left, mMapBounds.top);
        PointF p2 = new PointF((mMapBounds.right + mMapBounds.left) / 2, mMapBounds.top);
        PointF p3 = new PointF(mMapBounds.right, mMapBounds.top);
        PointF p4 = new PointF(mMapBounds.right, (mMapBounds.bottom + mMapBounds.top) / 2);
        PointF p5 = new PointF(mMapBounds.right, mMapBounds.bottom);
        PointF p6 = new PointF((mMapBounds.right + mMapBounds.left) / 2, mMapBounds.bottom);
        PointF p7 = new PointF(mMapBounds.left, mMapBounds.bottom);
        PointF p8 = new PointF(mMapBounds.left, (mMapBounds.bottom + mMapBounds.top) / 2);

        return new PointF[]{p1, p2, p3, p4, p5, p6, p7, p8};
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

    private double getDistanceOfPoints(PointF p1, PointF p2) {
        return Math.sqrt(Math.abs((p1.x - p2.x)
                * (p1.x - p2.x) + (p1.y - p2.y)
                * (p1.y - p2.y)));
    }

    private class Blink extends Handler implements Runnable {
        private Annot mAnnot;

        public Blink(Annot annot) {
            mAnnot = annot;
        }

        public void setAnnot(Annot annot) {
            mAnnot = annot;
        }

        @Override
        public void run() {
            if (mFNModule != null) {
                if (shouldShowInputSoft(mAnnot)) {
                    int keybordHeight = AppKeyboardUtil.getKeyboardHeight(mParent);
                    if (0 < keybordHeight && keybordHeight < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
                        mFNModule.setPadding(0, 0, 0, 0);
                    }
                } else {
                    mFNModule.setPadding(0, 0, 0, 0);
                }
            }

            postDelayed(Blink.this, 500);
        }
    }

    private int getFNBottomPadding() {
        Rect rect = new Rect();
        mParent.getGlobalVisibleRect(rect);

        int padding;
        int top = rect.top;
        int bottom = rect.bottom;
        int keyboardHeight = AppKeyboardUtil.getKeyboardHeight(mParent);
        int screenHeight = AppDisplay.getInstance(mContext).getRawScreenHeight();

        if (Build.VERSION.SDK_INT < 14 && keyboardHeight < screenHeight / 5) {
            padding = 0;
        } else if ((screenHeight - bottom - AppDisplay.getInstance(mContext).getNavBarHeight()) >= keyboardHeight) {
            padding = 0;
        } else if (screenHeight - top > keyboardHeight && screenHeight - bottom - AppDisplay.getInstance(mContext).getNavBarHeight() < keyboardHeight) {
            padding = keyboardHeight - (screenHeight - bottom - AppDisplay.getInstance(mContext).getNavBarHeight());
        } else {
            padding = 0;
        }
        return padding;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!hasInitialized() || mFormFiller == null) return false;
        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyForm())
            return false;
        if (FormFillerUtil.isReadOnly(annot)) return false;

        boolean ret = false;
        mIsLongPressTouchEvent = false;
        try {
            PointF docViewerPt = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF point = new PointF();
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(docViewerPt, point, pageIndex);
            PointF pageViewPt = new PointF(point.x, point.y);
            final PointF pdfPointF = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pdfPointF, pageIndex);

            boolean isHit = isHitAnnot(annot, pdfPointF);

            if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHit) {
                    mIsLongPressTouchEvent = true;
                    ret = true;
                } else {
                    mIsLongPressTouchEvent = false;
                    ret = false;

                    if (shouldShowInputSoft(annot)) {
                        if (mBlink != null) {
                            mBlink.removeCallbacks((Runnable) mBlink);
                            mBlink = null;
                        }
                        AppUtil.dismissInputSoft(mEditView);
                        mParent.removeView(mEditView);
                    }
                    if (shouldShowNavigation(annot)) {
                        if (mFNModule != null) {
                            mFNModule.hide();
                            mFNModule.setPadding(0, 0, 0, 0);
                        }
                        resetDocViewerOffset();
                    }
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
            } else {
                ret = true;
                mIsLongPressTouchEvent = true;
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(annot);
            }
        } catch (PDFException e) {
            ret = false;
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!hasInitialized() || mFormFiller == null) return false;
        mLastTouchPoint.set(0, 0);
        boolean ret = false;
        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canFillForm())
            return false;
        if (FormFillerUtil.isReadOnly(annot))
            return false;

        mIsLongPressTouchEvent = false;
        try {
            PointF docViewerPt = new PointF(motionEvent.getX(), motionEvent.getY());

            PointF point = new PointF();
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(docViewerPt, point, pageIndex);
            PointF pageViewPt = new PointF(point.x, point.y);
            final PointF pdfPointF = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pdfPointF, pageIndex);
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);

            Annot annotTmp = AppAnnotUtil.createAnnot(page.getAnnotAtPoint(AppUtil.toFxPointF(pdfPointF), 1));

            boolean isHit = isHitAnnot(annot, pdfPointF);

            if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHit) {
                    ret = true;
                } else {
                    if (shouldShowInputSoft(annot)) {
                        if (mBlink != null) {
                            mBlink.removeCallbacks((Runnable) mBlink);
                            mBlink = null;
                        }
                        AppUtil.dismissInputSoft(mEditView);
                        mParent.removeView(mEditView);
                    }
                    if (shouldShowNavigation(annot)) {
                        if (mFNModule != null) {
                            mFNModule.hide();
                            mFNModule.setPadding(0, 0, 0, 0);
                        }
                        resetDocViewerOffset();
                    }
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    ret = false;
                }
            } else {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(annot);
                ret = true;
            }

            if (annotTmp == null || (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                    && pageIndex == annot.getPage().getIndex())) {
                PointF touchPointF = pdfPointF;
                if (isHit) {
                    touchPointF = adjustTouchPointF(pdfPointF, AppUtil.toRectF(annot.getRect()));
                }
                mFormFiller.onLButtonDown(page, AppUtil.toFxPointF(touchPointF), 0);
                mFormFiller.onLButtonUp(page, AppUtil.toFxPointF(touchPointF), 0);
                mIsModify = true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private PointF adjustTouchPointF(PointF pdfPointF, RectF annotRectF){
        if (pdfPointF.x < annotRectF.left){
            pdfPointF.x = annotRectF.left;
        }
        if (pdfPointF.x > annotRectF.right){
            pdfPointF.x = annotRectF.right;
        }
        if (pdfPointF.y < annotRectF.bottom){
            pdfPointF.y = annotRectF.bottom;
        }
        if (pdfPointF.y > annotRectF.top){
            pdfPointF.y = annotRectF.top;
        }
        return pdfPointF;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }

    private static PointF getPageViewOrigin(PDFViewCtrl pdfViewCtrl, int pageIndex, float x, float y) {
        PointF pagePt = new PointF(x, y);
        pdfViewCtrl.convertPageViewPtToDisplayViewPt(pagePt, pagePt, pageIndex);
        RectF rect = new RectF(0, 0, pagePt.x, pagePt.y);
        pdfViewCtrl.convertDisplayViewRectToPageViewRect(rect, rect, pageIndex);
        PointF originPt = new PointF(x - rect.width(), y - rect.height());
        return originPt;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null || annot.isEmpty() || !(annot instanceof Widget)) return;
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() != this)
            return;

        if (mIsLongPressTouchEvent) {
            onDrawByLongPress(annot, pageIndex, canvas);
        } else {
            onDrawBySingleTap(annot, pageIndex, canvas);
        }
    }

    private void onDrawBySingleTap(Annot annot, int pageIndex, Canvas canvas) {
        try {
            int index = annot.getPage().getIndex();
            if (index != pageIndex) return;
            RectF rect = AppUtil.toRectF(annot.getRect());

            PointF viewpoint = new PointF(rect.left, rect.bottom);
            PointF point = new PointF(rect.left, rect.bottom);
            mPdfViewCtrl.convertPdfPtToPageViewPt(viewpoint, viewpoint, pageIndex);
            mPdfViewCtrl.convertPdfPtToPageViewPt(point, point, pageIndex);
            mPdfViewCtrl.convertPageViewPtToDisplayViewPt(viewpoint, viewpoint, pageIndex);
            int type = FormFillerUtil.getAnnotFieldType(mForm, annot);

            if ((type == Field.e_TypeTextField) ||
                    (type == Field.e_TypeComboBox && (((Widget) annot).getField().getFlags() & Field.e_FlagComboEdit) != 0)) {
                int keyboardHeight = AppKeyboardUtil.getKeyboardHeight(mParent);
                if (mAdjustPosition && keyboardHeight > AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
                    int rawScreenHeight = AppDisplay.getInstance(mContext).getRawScreenHeight();
                    int navBarHeight = AppDisplay.getInstance(mContext).getRealNavBarHeight();

                    if (rawScreenHeight - viewpoint.y < (keyboardHeight + AppDisplay.getInstance(mContext).dp2px(116)) + navBarHeight) {
                        mPageOffset = (int) (keyboardHeight - (rawScreenHeight - viewpoint.y));

                        if (mPageOffset != 0 && pageIndex == mPdfViewCtrl.getPageCount() - 1 ||
                                ((!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE) ||
                                mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING ||
                                mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER)) {

                            PointF point1 = new PointF(0, mPdfViewCtrl.getPageViewHeight(pageIndex));
                            mPdfViewCtrl.convertPageViewPtToDisplayViewPt(point1, point1, pageIndex);
                            float screenHeight = AppDisplay.getInstance(mContext).getScreenHeight();
                            if (point1.y <= screenHeight) {
                                int offset = mPageOffset + AppDisplay.getInstance(mContext).dp2px(116) + navBarHeight;
//                                mOffset = 0;
                                setBottomOffset(offset);
                            }
                        }

                        PointF oriPoint = getPageViewOrigin(mPdfViewCtrl, pageIndex, point.x, point.y);
                        mPdfViewCtrl.gotoPage(pageIndex,
                                oriPoint.x, oriPoint.y + mPageOffset + AppDisplay.getInstance(mContext).dp2px(116) + navBarHeight);
                        mAdjustPosition = false;
                    } else {
                        resetDocViewerOffset();
                    }
                }
            }

            if (pageIndex != mPdfViewCtrl.getPageCount() - 1 &&
                    !(!mPdfViewCtrl.isContinuous() && (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE ||
                            mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING||
                            mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER))) {
                resetDocViewerOffset();
            }

            if (AppKeyboardUtil.getKeyboardHeight(mParent) < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5
                    && (pageIndex == mPdfViewCtrl.getPageCount() - 1 ||
                    (!mPdfViewCtrl.isContinuous() && (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE ||
                            mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING ||
                            mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER)))) {
                resetDocViewerOffset();
            }

            int fieldType = FormFillerUtil.getAnnotFieldType(mForm, annot);
            if (mFNModule != null) {
                if (fieldType != Field.e_TypePushButton) {
                    if ((fieldType == Field.e_TypeTextField ||
                            (fieldType == Field.e_TypeComboBox && (((Widget) annot).getField().getFlags() & Field.e_FlagComboEdit) != 0))) {
                        mFNModule.setPadding(0, 0, 0, getFNBottomPadding());

                    } else {
                        mFNModule.setPadding(0, 0, 0, 0);
                    }
                }
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() == null) {
                    mFNModule.hide();
                }
            }
            canvas.save();
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            if (index == pageIndex && fieldType != Field.e_TypePushButton) {
                RectF bbox = AppUtil.toRectF(annot.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
                bbox.sort();
                bbox.inset(-5, -5);

                canvas.drawLine(bbox.left, bbox.top, bbox.left, bbox.bottom, mPathPaint);
                canvas.drawLine(bbox.left, bbox.bottom, bbox.right, bbox.bottom, mPathPaint);
                canvas.drawLine(bbox.right, bbox.bottom, bbox.right, bbox.top, mPathPaint);
                canvas.drawLine(bbox.left, bbox.top, bbox.right, bbox.top, mPathPaint);
            }
            canvas.restore();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private RectF mBBoxInOnDraw = new RectF();
    private RectF mViewDrawRectInOnDraw = new RectF();
    private DrawFilter mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private void onDrawByLongPress(Annot annot, int pageIndex, Canvas canvas) {
        try {
            int annotPageIndex = annot.getPage().getIndex();
            if (AppAnnotUtil.equals(mLastAnnot, annot) && annotPageIndex == pageIndex) {
                canvas.save();
                canvas.setDrawFilter(mDrawFilter);
                float thickness = thicknessOnPageView(pageIndex, DEFAULT_THICKNESS);
                RectF _rect = AppUtil.toRectF(annot.getRect());
                mViewDrawRectInOnDraw.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRectInOnDraw, mViewDrawRectInOnDraw, pageIndex);
                mViewDrawRectInOnDraw.inset(thickness / 2f, thickness / 2f);
                if (mLastOper == OPER_SCALE_LT) {// SCALE
                    mBBoxInOnDraw.set(mLastPoint.x, mLastPoint.y, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_T) {
                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.y, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_RT) {
                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.y, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_R) {
                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_RB) {
                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mLastPoint.y);
                } else if (mLastOper == OPER_SCALE_B) {
                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.y);
                } else if (mLastOper == OPER_SCALE_LB) {
                    mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.y);
                } else if (mLastOper == OPER_SCALE_L) {
                    mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
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

    private void drawControlPoints(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
        for (PointF ctlPt : ctlPts) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            mCtlPtPaint.setColor(DEFAULT_COLOR);
            mCtlPtPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    private Path mImaginaryPath = new Path();

    private void drawControlImaginary(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mFrmPaint.setStrokeWidth(mCtlPtLineWidth);

        mImaginaryPath.reset();
        // set path
        pathAddLine(mImaginaryPath, ctlPts[0].x + mCtlPtRadius, ctlPts[0].y, ctlPts[1].x - mCtlPtRadius, ctlPts[1].y);
        pathAddLine(mImaginaryPath, ctlPts[1].x + mCtlPtRadius, ctlPts[1].y, ctlPts[2].x - mCtlPtRadius, ctlPts[2].y);
        pathAddLine(mImaginaryPath, ctlPts[2].x, ctlPts[2].y + mCtlPtRadius, ctlPts[3].x, ctlPts[3].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[3].x, ctlPts[3].y + mCtlPtRadius, ctlPts[4].x, ctlPts[4].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[4].x - mCtlPtRadius, ctlPts[4].y, ctlPts[5].x + mCtlPtRadius, ctlPts[5].y);
        pathAddLine(mImaginaryPath, ctlPts[5].x - mCtlPtRadius, ctlPts[5].y, ctlPts[6].x + mCtlPtRadius, ctlPts[6].y);
        pathAddLine(mImaginaryPath, ctlPts[6].x, ctlPts[6].y - mCtlPtRadius, ctlPts[7].x, ctlPts[7].y + mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[7].x, ctlPts[7].y - mCtlPtRadius, ctlPts[0].x, ctlPts[0].y + mCtlPtRadius);

        canvas.drawPath(mImaginaryPath, mFrmPaint);
    }

    private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);
    }

    private RectF mViewDrawRect = new RectF(0, 0, 0, 0);
    private RectF mDocViewerBBox = new RectF(0, 0, 0, 0);

    protected void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (mIsLongPressTouchEvent && curAnnot != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this) {
            try {
                int annotPageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                    float thickness = thicknessOnPageView(annotPageIndex, DEFAULT_THICKNESS);
                    RectF _rect = AppUtil.toRectF(curAnnot.getRect());
                    mViewDrawRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRect, mViewDrawRect, annotPageIndex);
                    mViewDrawRect.inset(thickness / 2f, thickness / 2f);
                    if (mLastOper == OPER_SCALE_LT) {
                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mLastPoint.y;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_T) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mLastPoint.y;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_RT) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mLastPoint.y;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_R) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_RB) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mLastPoint.y;
                    } else if (mLastOper == OPER_SCALE_B) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mLastPoint.y;
                    } else if (mLastOper == OPER_SCALE_LB) {
                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mLastPoint.y;
                    } else if (mLastOper == OPER_SCALE_L) {
                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
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
                    mAnnotationMenu.update(mDocViewerBBox);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent contentSupplier, boolean addUndo,
                         Event.Callback result) {
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
    }

    protected boolean shouldShowInputSoft(Annot annot) {
        if (annot == null) return false;
        if (!(annot instanceof Widget)) return false;
        int type = FormFillerUtil.getAnnotFieldType(mForm, annot);
        try {
            if ((type == Field.e_TypeTextField) ||
                    (type == Field.e_TypeComboBox && (((Widget) annot).getField().getFlags() & Field.e_FlagComboEdit) != 0))
                return true;

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void resetDocViewerOffset() {
        if (mPageOffset != 0) {
            mPageOffset = 0;
            setBottomOffset(0);
        }
    }

    private void setBottomOffset(int offset) {
        if (mOffset == -offset)
            return;
        mOffset = -offset;
        mPdfViewCtrl.layout(0, mOffset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() + mOffset);
    }

    protected boolean onKeyBack() {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (curAnnot == null) return false;
            if (curAnnot.getType() != Annot.e_Widget) return false;
            Field field = ((Widget) curAnnot).getField();
            if (field == null || field.isEmpty()) return false;
            int type = field.getType();
            if (type != Field.e_TypeSignature && type != Field.e_TypeUnknown) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                navigationDismiss();
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        mAdjustPosition = true;
    }

}
