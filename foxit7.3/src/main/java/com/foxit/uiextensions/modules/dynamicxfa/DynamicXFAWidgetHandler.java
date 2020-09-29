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
package com.foxit.uiextensions.modules.dynamicxfa;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.addon.xfa.XFAPage;
import com.foxit.sdk.addon.xfa.XFAWidget;
import com.foxit.sdk.common.fxcrt.Matrix2D;
import com.foxit.sdk.pdf.Signature;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.form.FormFillerUtil;
import com.foxit.uiextensions.annots.form.FormNavigationModule;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UISaveAsDialog;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.home.local.LocalModule;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureModule;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureSecurityHandler;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureUtil;
import com.foxit.uiextensions.security.digitalsignature.IDigitalSignatureCallBack;
import com.foxit.uiextensions.security.digitalsignature.IDigitalSignatureCreateCallBack;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppSQLite;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class DynamicXFAWidgetHandler implements IXFAWidgetHandler {
    private static final Lock lock = new ReentrantLock();
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;

    private XFAWidget mCurrentXFAWidget = null;

    private FormNavigationModule mFNModule = null;
    private int mPageOffset;
    private int mOffset;
    private EditText mEditView = null;
    private PointF mLastTouchPoint = new PointF(0, 0);
    private boolean mIsBackBtnPush = false; //for some input method, double backspace click
    private boolean mAdjustPosition = false;
    private boolean mIsShowEditText = false;
    private String mLastInputText = "";
    private String mChangeText = null;
    private Paint mPathPaint;
    private int mKeyBoardHeight;

    public DynamicXFAWidgetHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
    }

    public void initialize() {
        mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setAntiAlias(true);
        mPathPaint.setDither(true);
        PathEffect effects = new DashPathEffect(new float[]{1, 2, 4, 8}, 1);
        mPathPaint.setPathEffect(effects);

        initFormNavigation();
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
                    if (mCurrentXFAWidget != null && !mCurrentXFAWidget.isEmpty()) {
                        try {
                            PDFViewCtrl.lock();
                            mPdfViewCtrl.getXFADoc().killFocus();
                            mCurrentXFAWidget.resetData();
                            mPdfViewCtrl.getXFADoc().setFocus(mCurrentXFAWidget);
                            refresh(mCurrentXFAWidget);
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
                    if (mCurrentXFAWidget != null) {
                        if (shouldShowInputSoft(mCurrentXFAWidget)) {
                            AppUtil.dismissInputSoft(mEditView);
                            mParent.removeView(mEditView);
                        }
                        setCurrentXFAWidget(null);
                    }
                    mFNModule.hide();
                    resetDocViewerOffset();
                }
            });

            mFNModule.setClearEnable(false);
        }

        ViewGroup parent = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView();
        AppKeyboardUtil.setKeyboardListener(parent, parent, new AppKeyboardUtil.IKeyboardListener() {
            @Override
            public void onKeyboardOpened(int keyboardHeight) {
                if (Build.VERSION.SDK_INT < 14 && keyboardHeight < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
                    keyboardHeight = 0;
                }
                if (mFNModule != null){
                    mFNModule.setPadding(0, 0, 0, keyboardHeight);
                }

                try {
                    if (keyboardHeight != 0 && mKeyBoardHeight == 0){
                        if (mCurrentXFAWidget != null && !mCurrentXFAWidget.isEmpty()){
                            refresh(mCurrentXFAWidget);
                        }
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onKeyboardClosed() {
                if (mFNModule != null){
                    mFNModule.setPadding(0, 0, 0, 0);
                }
            }
        });
    }

    private boolean isFind = false;
    private boolean isDocFinish = false;
    private int prePageIdx;
    private int preWidgetIdx;
    private int nextPageIdx;
    private int nextWidgetIdx;
    private CountDownLatch mCountDownLatch;
    private Runnable preNavigation = new Runnable() {

        @Override
        public void run() {
            try {
                if (mCurrentXFAWidget != null && !mCurrentXFAWidget.isEmpty()) {
//                    refresh(mCurrentXFAWidget);
                    XFAPage curPage = mCurrentXFAWidget.getXFAPage();
                    final int curPageIdx = curPage.getIndex();
                    prePageIdx = curPageIdx;
                    final int curWidgetIdx = mCurrentXFAWidget.getIndex();
                    preWidgetIdx = curWidgetIdx;
                    isFind = false;
                    isDocFinish = false;
                    while (prePageIdx >= 0) {
                        mCountDownLatch = new CountDownLatch(1);
                        curPage = mPdfViewCtrl.getXFADoc().getPage(prePageIdx);
                        if (prePageIdx == curPageIdx && isDocFinish == false) {
                            preWidgetIdx = curWidgetIdx - 1;
                        } else {
                            preWidgetIdx = curPage.getWidgetCount() - 1;
                        }

                        while (curPage != null && preWidgetIdx >= 0) {
                            final XFAWidget preWidget = curPage.getWidget(preWidgetIdx);
                            final int preWidgetType = preWidget.getType();
                            if (preWidget != null
                                    && !preWidget.isEmpty()
                                    && preWidgetType != XFAWidget.e_WidgetTypeArc
                                    && preWidgetType != XFAWidget.e_WidgetTypeRectangle
                                    && preWidgetType != XFAWidget.e_WidgetTypeLine
                                    && preWidgetType != XFAWidget.e_WidgetTypePushButton
                                    && preWidgetType != XFAWidget.e_WidgetTypeSignature
                                    && preWidgetType != XFAWidget.e_WidgetTypeUnknown) {
                                isFind = true;
                                AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {

                                    @Override
                                    public void run() {
                                        try {
                                            if (preWidgetType == XFAWidget.e_WidgetTypeChoiceList) {
                                                RectF rect = AppUtil.toRectF(preWidget.getRect());
                                                rect.left += 5;
                                                rect.top -= 5;
                                                mLastTouchPoint.set(rect.left, rect.top);
                                            }
                                            setCurrentXFAWidget(null);
                                            if (preWidget != null && !preWidget.isEmpty()) {
                                                RectF bbox = AppUtil.toRectF(preWidget.getRect());
                                                RectF rect = new RectF(bbox);

                                                if (mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, prePageIdx)) {
                                                    float devX = rect.left - (mPdfViewCtrl.getWidth() - rect.width()) / 2;
                                                    float devY = rect.top - (mPdfViewCtrl.getHeight() - rect.height()) / 2;
                                                    mPdfViewCtrl.gotoPage(prePageIdx, devX, devY);
                                                } else {
                                                    mPdfViewCtrl.gotoPage(prePageIdx, new PointF(bbox.left, bbox.top));
                                                }
                                                setCurrentXFAWidget(preWidget);
                                                mPdfViewCtrl.getXFADoc().setFocus(preWidget);
                                            }
                                        } catch (PDFException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                                break;
                            } else {
                                preWidgetIdx--;
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
                            prePageIdx = mPdfViewCtrl.getXFADoc().getPageCount() - 1;
                            isDocFinish = true;
                        }
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    };

    private Runnable nextNavigation = new Runnable() {

        @Override
        public void run() {
            try {
                if (mCurrentXFAWidget != null && !mCurrentXFAWidget.isEmpty()) {
//                    refresh(mCurrentXFAWidget);
                    XFAPage curPage = mCurrentXFAWidget.getXFAPage();

                    final int curPageIdx = curPage.getIndex();
                    nextPageIdx = curPageIdx;
                    final int curWidgetIdx = mCurrentXFAWidget.getIndex();
                    nextWidgetIdx = curWidgetIdx;
                    isFind = false;
                    isDocFinish = false;

                    while (nextPageIdx < mPdfViewCtrl.getXFADoc().getPageCount()) {

                        mCountDownLatch = new CountDownLatch(1);
                        curPage = mPdfViewCtrl.getXFADoc().getPage(nextPageIdx);
                        if (nextPageIdx == curPageIdx && isDocFinish == false) {
                            nextWidgetIdx = curWidgetIdx + 1;
                        } else {
                            nextWidgetIdx = 0;
                        }

                        while (curPage != null && nextWidgetIdx < curPage.getWidgetCount()) {
                            final XFAWidget nextWidget = curPage.getWidget(nextWidgetIdx);
                            final int nextWidgetType = nextWidget.getType();
                            if (nextWidget != null
                                    && !nextWidget.isEmpty()
                                    && nextWidgetType != XFAWidget.e_WidgetTypeArc
                                    && nextWidgetType != XFAWidget.e_WidgetTypeRectangle
                                    && nextWidgetType != XFAWidget.e_WidgetTypeLine
                                    && nextWidgetType != XFAWidget.e_WidgetTypePushButton
                                    && nextWidgetType != XFAWidget.e_WidgetTypeSignature
                                    && nextWidgetType != XFAWidget.e_WidgetTypeUnknown) {
                                isFind = true;

                                AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {

                                    @Override
                                    public void run() {
                                        try {
                                            if (nextWidgetType == XFAWidget.e_WidgetTypeChoiceList) {
                                                RectF rect = AppUtil.toRectF(nextWidget.getRect());
                                                rect.left += 5;
                                                rect.top -= 5;
                                                mLastTouchPoint.set(rect.left, rect.top);
                                            }
                                            setCurrentXFAWidget(null);
                                            if (nextWidget != null && !nextWidget.isEmpty()) {
                                                RectF bbox = AppUtil.toRectF(nextWidget.getRect());
                                                RectF rect = new RectF(bbox);

                                                if (mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, nextPageIdx)) {
                                                    float devX = rect.left - (mPdfViewCtrl.getWidth() - rect.width()) / 2;
                                                    float devY = rect.top - (mPdfViewCtrl.getHeight() - rect.height()) / 2;
                                                    mPdfViewCtrl.gotoPage(nextPageIdx, devX, devY);
                                                } else {
                                                    mPdfViewCtrl.gotoPage(nextPageIdx, new PointF(bbox.left, bbox.top));
                                                }

                                                setCurrentXFAWidget(nextWidget);
                                                mPdfViewCtrl.getXFADoc().setFocus(nextWidget);
                                            }
                                        } catch (PDFException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                });

                                break;
                            } else {
                                nextWidgetIdx++;
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
                        if (nextPageIdx >= mPdfViewCtrl.getXFADoc().getPageCount()) {
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

    protected void showSoftInput(){
        AppUtil.showSoftInput(mEditView);
    }

    private void postDismissNavigation() {
        DismissNavigation dn = new DismissNavigation();
        dn.postDelayed(dn, 500);
    }

    private class DismissNavigation extends Handler implements Runnable {

        @Override
        public void run() {
            if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null || mPdfViewCtrl.getXFADoc() == null) return;
            if (mCurrentXFAWidget == null || mCurrentXFAWidget.isEmpty()) {
                if (mFNModule != null)
                    mFNModule.getLayout().setVisibility(View.INVISIBLE);
                AppUtil.dismissInputSoft(mEditView);
                resetDocViewerOffset();
            }
        }
    }

    private boolean shouldShowNavigation(XFAWidget xfaWidget) {
        if (xfaWidget == null || xfaWidget.isEmpty()) return false;
        try {
            int type = xfaWidget.getType();
            if (type == XFAWidget.e_WidgetTypeArc || type == XFAWidget.e_WidgetTypeRectangle
                    || type == XFAWidget.e_WidgetTypeLine || type == XFAWidget.e_WidgetTypePushButton) return false;
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void navigationDismiss() {
        if (mFNModule != null) {
            mFNModule.hide();
            mFNModule.setPadding(0, 0, 0, 0);
        }

        if (mEditView != null) {
            mParent.removeView(mEditView);
        }
        resetDocViewerOffset();
        AppUtil.dismissInputSoft(mEditView);
    }


    private boolean isHitXFAWidget(XFAWidget xfaWidget, PointF pointF, int pageIndex) {
        try {
            int hitArea = xfaWidget.onHitTest(AppUtil.toFxPointF(pointF));
            if (hitArea != XFAWidget.e_HitTestAreaUnknown) return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isSameXFAWidget(XFAWidget xfaWidget, XFAWidget other) {
        if (xfaWidget == null || xfaWidget.isEmpty()) return false;
        if (other == null || other.isEmpty()) return false;
        return xfaWidget.equal(other);
    }

    protected void setCurrentXFAWidget(XFAWidget xfaWidget) {
        lock.lock();
        if (mCurrentXFAWidget == null && xfaWidget == null) return;
        if (isSameXFAWidget(mCurrentXFAWidget, xfaWidget)) return;
        if (xfaWidget == null) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).startHideToolbarsTimer();
        } else {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).stopHideToolbarsTimer();
        }
        XFAWidget lastWidget = mCurrentXFAWidget;
        if (lastWidget != null && !lastWidget.isEmpty()) {
            onXFAWidgetDeselected(lastWidget, true);
        }

        mCurrentXFAWidget = xfaWidget;
        if (xfaWidget != null && !xfaWidget.isEmpty()) {
            onXFAWidgetSelected(xfaWidget, true);
        }
        lock.unlock();
    }

    protected void update(XFAWidget xfaWidget) {
        if (mCurrentXFAWidget == null && xfaWidget == null) return;
        try {
            mCurrentXFAWidget = null;
            mPdfViewCtrl.getXFADoc().killFocus();
//            refresh(xfaWidget);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected boolean shouldShowInputSoft(XFAWidget xfaWidget) {
        if (xfaWidget == null || xfaWidget.isEmpty()) return false;
        try {
            int type = xfaWidget.getType();
            if (type == XFAWidget.e_WidgetTypeDateTimeEdit || type == XFAWidget.e_WidgetTypeNumericEdit
                    || type == XFAWidget.e_WidgetTypePasswordEdit || type == XFAWidget.e_WidgetTypeTextEdit) {
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void resetDocViewerOffset() {
        if (mPageOffset != 0) {
            mPageOffset = 0;
            setBottomOffset(0);
        }
    }

    private void setBottomOffset(int offset) {
        if (mOffset == -offset)
            return;
        mOffset = -offset;
        mPdfViewCtrl.layout(0, 0 + mOffset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() + mOffset);
    }

    protected boolean onKeyBack() {
        if (mCurrentXFAWidget == null || mCurrentXFAWidget.isEmpty()) return false;
        try {
            int type = mCurrentXFAWidget.getType();
            if (type != XFAWidget.e_WidgetTypeSignature && type != XFAWidget.e_WidgetTypeUnknown) {
                setCurrentXFAWidget(null);
                navigationDismiss();
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static PointF getPageViewOrigin(PDFViewCtrl pdfViewCtrl, int pageIndex, float x, float y) {
        PointF pagePt = new PointF(x, y);
        pdfViewCtrl.convertPageViewPtToDisplayViewPt(pagePt, pagePt, pageIndex);
        RectF rect = new RectF(0, 0, pagePt.x, pagePt.y);
        pdfViewCtrl.convertDisplayViewRectToPageViewRect(rect, rect, pageIndex);
        PointF originPt = new PointF(x - rect.width(), y - rect.height());
        return originPt;
    }

    private int getKeyboardHeight() {
        Rect r = new Rect();
        mParent.getWindowVisibleDisplayFrame(r);
        int screenHeight = mParent.getRootView().getHeight();
        int viewHeight = mParent.getHeight();
        int navBarHeight = 0;

        if (screenHeight - viewHeight > 0){
            navBarHeight = AppDisplay.getInstance(mParent.getContext()).getNavBarHeight();
        }

        return screenHeight - (r.bottom - r.top) - navBarHeight;
    }

    private double getDistanceOfPoints(PointF p1, PointF p2) {
        return Math.sqrt(Math.abs((p1.x - p2.x)
                * (p1.x - p2.x) + (p1.y - p2.y)
                * (p1.y - p2.y)));
    }

    private void refresh(XFAWidget xfaWidget) throws PDFException{
        if (xfaWidget == null || xfaWidget.isEmpty()) return;
        RectF rectF = AppUtil.toRectF(xfaWidget.getRect());
        int pageIndex = xfaWidget.getXFAPage().getIndex();
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(rectF));
    }

    private void invalidate(XFAWidget xfaWidget) throws PDFException{
        if (xfaWidget == null || xfaWidget.isEmpty()) return;
        RectF rectF = AppUtil.toRectF(xfaWidget.getRect());
        int pageIndex = xfaWidget.getXFAPage().getIndex();
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
    }

    private boolean mOldIsHideSystem;

    @Override
    public void onXFAWidgetSelected(final XFAWidget widget, boolean reRender) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager();
        MainFrame mainFrame = (MainFrame)uiExtensionsManager.getMainFrame();
        mOldIsHideSystem = mainFrame.isHideSystemUI();

        if (isXFAWidgetSignature(widget) && uiExtensionsManager.getConfig().modules.isLoadSignature()) {
            try {
                Signature signature = widget.getSignature();
                if (signature.isEmpty()) return;

                RectF rectF = AppUtil.toRectF(widget.getRect());
                mPageIndex = widget.getXFAPage().getIndex();
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, mPageIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mPageIndex);
                if (uiExtensionsManager.getDocumentManager().canModifyXFAForm()) {
                    showAnnotMenu(signature.isSigned(), rectF);
                }
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return;
        }

        if (shouldShowInputSoft(widget)) {
            if (mainFrame.isToolbarsVisible()){
                mainFrame.setHideSystemUI(false);
            } else {
                AppUtil.showSystemUI(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
            }

            mIsShowEditText = true;
            mAdjustPosition = true;
            mLastInputText = " ";

            if (mEditView != null) {
                mParent.removeView(mEditView);
            }
            mEditView = new EditText(mContext);
            mEditView.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
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

            mEditView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                        onBackspaceBtnDown(widget);
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

                                    widget.onChar(value, 0);
                                }
                            } else {
                                for (int i = 0; i < before; i++) {
                                    onBackspaceBtnDown(widget);
                                }
                                for (int i = 0; i < count; i++) {
                                    char c = s.charAt(s.length() - count + i);

                                    if (FormFillerUtil.isEmojiCharacter((int) c))
                                        break;
                                    if ((int) c == 10)
                                        c = 13;
                                    final char value = c;

                                    widget.onChar(value, 0);
                                }
                            }
                        } else if (s.length() < mLastInputText.length()) {

                            if (!mIsBackBtnPush){
                                for (int i = 0; i < before; i++) {
                                    onBackspaceBtnDown(widget);
                                }

                                for (int i = 0; i < count; i++) {
                                    char c = s.charAt(s.length() - count + i);

                                    if (FormFillerUtil.isEmojiCharacter((int) c))
                                        break;
                                    if ((int) c == 10)
                                        c = 13;
                                    final char value = c;

                                    widget.onChar(value, 0);
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
        }

        try {
            int type = widget.getType();
            if (mFNModule != null) {
                mFNModule.setClearEnable(true);
                if (type != XFAWidget.e_WidgetTypeArc && type != XFAWidget.e_WidgetTypeRectangle
                        && type != XFAWidget.e_WidgetTypeLine && type != XFAWidget.e_WidgetTypePushButton) {
                    mFNModule.show();
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onXFAWidgetDeselected(XFAWidget widget, boolean reRender) {
        MainFrame mainFrame = (MainFrame) ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getMainFrame();
        mainFrame.setHideSystemUI(mOldIsHideSystem);
        if (isXFAWidgetSignature(widget)) {
            hideAnnotMenu();
            mPageIndex = -1;
            try {
                mPdfViewCtrl.getXFADoc().killFocus();
                invalidate(widget);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else {
            postDismissNavigation();
            try {
                mPdfViewCtrl.getXFADoc().killFocus();
                refresh(widget);
            } catch (PDFException e) {
                e.printStackTrace();
            }
            if (mIsShowEditText) {
                AppUtil.dismissInputSoft(mEditView);
                mParent.removeView(mEditView);
                mIsShowEditText = false;
            }
        }
    }

    private boolean isDown = false;
    PointF oldPoint = new PointF();

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, XFAWidget widget) {
        try {
            if (!((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyXFAForm()) return false;
            PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF pageViewPt = new PointF();
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, pageViewPt, pageIndex);
            PointF pdfPointF = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pdfPointF, pageIndex);

            int action = motionEvent.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (isSameXFAWidget(mCurrentXFAWidget, widget) && isHitXFAWidget(widget, pdfPointF, pageIndex)) {
                        isDown = true;
                        widget.onLButtonDown(AppUtil.toFxPointF(pdfPointF), 0);
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (getDistanceOfPoints(pageViewPt, oldPoint) > 0 && isSameXFAWidget(mCurrentXFAWidget, widget)) {
                        oldPoint.set(pageViewPt);
                        widget.onMouseMove(AppUtil.toFxPointF(pdfPointF), 0);
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isHitXFAWidget(widget, pdfPointF, pageIndex) || isDown) {
                        isDown = false;
                        widget.onLButtonUp(AppUtil.toFxPointF(pdfPointF), 0);
                        if (shouldShowInputSoft(widget)){
                            int keybordHeight = AppKeyboardUtil.getKeyboardHeight(mParent);
                            if (0 <= keybordHeight && keybordHeight < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5){
                                showSoftInput();
                            }
                        }
                        return true;
                    }
                    return false;
                default:
            }
        } catch (PDFException e) {

        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, XFAWidget widget) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, XFAWidget widget) {
        try {
            mLastTouchPoint.set(0, 0);
            if (!((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyXFAForm()) return false;
            PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF pageViewPt = new PointF();
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, pageViewPt, pageIndex);
            PointF pdfPointF = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pdfPointF, pageIndex);

            boolean ret = false;
            boolean isHit = isHitXFAWidget(widget, pdfPointF, pageIndex);
            if (isSameXFAWidget(mCurrentXFAWidget, widget)) {
                if (isHit) {
                    ret = true;
                } else {
                    if (shouldShowNavigation(widget)) {
                        if (mFNModule != null) {
                            mFNModule.hide();
                            mFNModule.setPadding(0, 0, 0, 0);
                        }
                        resetDocViewerOffset();
                    }
                    setCurrentXFAWidget(null);
                    ret = false;
                }
            } else {
                if(isXFAWidgetSignature(widget)) {
                    if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getConfig().modules.isLoadSignature()) {
                        setCurrentXFAWidget(widget);
                        ret = true;
                    } else {
                        setCurrentXFAWidget(null);
                        ret = false;
                    }
                } else {
                    setCurrentXFAWidget(widget);
                    ret = true;
                }
            }

            if (isSameXFAWidget(mCurrentXFAWidget, widget) && isHit) {
                widget.onMouseMove(AppUtil.toFxPointF(pdfPointF), 0);
                widget.onLButtonDown(AppUtil.toFxPointF(pdfPointF), 0);
                widget.onLButtonUp(AppUtil.toFxPointF(pdfPointF), 0);
            }

            return ret;
        } catch (PDFException e) {

        }
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (!mPdfViewCtrl.isDynamicXFA() || !mPdfViewCtrl.isPageVisible(pageIndex)) return;
        if (mCurrentXFAWidget == null || mCurrentXFAWidget.isEmpty()) return;
        try {
            int index = mCurrentXFAWidget.getXFAPage().getIndex();
            if (index != pageIndex) return;
            RectF rect = AppUtil.toRectF(mCurrentXFAWidget.getRect());
            PointF viewpoint = new PointF(rect.left, rect.bottom);
            PointF point = new PointF(rect.left, rect.bottom);
            mPdfViewCtrl.convertPdfPtToPageViewPt(viewpoint, viewpoint, pageIndex);
            mPdfViewCtrl.convertPdfPtToPageViewPt(point, point, pageIndex);
            mPdfViewCtrl.convertPageViewPtToDisplayViewPt(viewpoint, viewpoint, pageIndex);
            int type = mCurrentXFAWidget.getType();
            if (shouldShowInputSoft(mCurrentXFAWidget)) {
                mKeyBoardHeight = getKeyboardHeight();
                if (mAdjustPosition && getKeyboardHeight() > AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
                    if (AppDisplay.getInstance(mContext).getRawScreenHeight() - viewpoint.y < (getKeyboardHeight() + AppDisplay.getInstance(mContext).dp2px(116))) {
                        int keyboardHeight = getKeyboardHeight();
                        int rawScreenHeight = AppDisplay.getInstance(mContext).getRawScreenHeight();
                        mPageOffset = (int) (keyboardHeight - (rawScreenHeight - viewpoint.y));

                        if (mPageOffset != 0 && pageIndex == mPdfViewCtrl.getPageCount() - 1 ||
                                (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE) ||
                                mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING ||
                                mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER) {

                            PointF point1 = new PointF(0, mPdfViewCtrl.getPageViewHeight(pageIndex));
                            mPdfViewCtrl.convertPageViewPtToDisplayViewPt(point1, point1, pageIndex);
                            float screenHeight = AppDisplay.getInstance(mContext).getScreenHeight();
                            if (point1.y <= screenHeight) {
                                int offset = mPageOffset + AppDisplay.getInstance(mContext).dp2px(116);
                                setBottomOffset(offset);
                            }
                        }
                        PointF oriPoint = getPageViewOrigin(mPdfViewCtrl, pageIndex, point.x, point.y);
                        mPdfViewCtrl.gotoPage(pageIndex,
                                oriPoint.x, oriPoint.y + mPageOffset + AppDisplay.getInstance(mContext).dp2px(116));
                        mAdjustPosition = false;
                    } else {
                        resetDocViewerOffset();
                    }
                }
            }

            if (pageIndex != mPdfViewCtrl.getPageCount() - 1 &&
                    !(!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE) &&
                    mPdfViewCtrl.getPageLayoutMode() != PDFViewCtrl.PAGELAYOUTMODE_FACING &&
                    mPdfViewCtrl.getPageLayoutMode() != PDFViewCtrl.PAGELAYOUTMODE_COVER) {
                resetDocViewerOffset();
            }
            if (getKeyboardHeight() < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5
                    && (pageIndex == mPdfViewCtrl.getPageCount() - 1 ||
                    (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE) ||
                    mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING ||
                    mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER)) {
                resetDocViewerOffset();
            }

            if (mFNModule != null) {
                if (type != XFAWidget.e_WidgetTypeArc && type != XFAWidget.e_WidgetTypeRectangle
                        && type != XFAWidget.e_WidgetTypeLine && type != XFAWidget.e_WidgetTypePushButton) {
                    if (shouldShowInputSoft(mCurrentXFAWidget)) {
                        int paddingBottom = getKeyboardHeight();
                        if (Build.VERSION.SDK_INT < 14 && getKeyboardHeight() < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
                            paddingBottom = 0;
                        }
                        mFNModule.setPadding(0, 0, 0, paddingBottom);

                    } else {
                        mFNModule.setPadding(0, 0, 0, 0);
                    }
                }
                if (mCurrentXFAWidget == null) {
                    mFNModule.hide();
                }
            }
            canvas.save();
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            if (index == pageIndex && type != XFAWidget.e_WidgetTypeArc &&
                    type != XFAWidget.e_WidgetTypeRectangle && type != XFAWidget.e_WidgetTypeLine && type != XFAWidget.e_WidgetTypePushButton) {
                RectF bbox = AppUtil.toRectF(mCurrentXFAWidget.getRect());
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


    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        if (!((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyXFAForm()) return false;
        int action = motionEvent.getActionMasked();
        XFAWidget xfaWidget = null;
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                xfaWidget = mCurrentXFAWidget;
                if (xfaWidget != null && !xfaWidget.isEmpty()) {
                    if (onTouchEvent(pageIndex, motionEvent, xfaWidget)) {
                        return true;
                    }
                }

                xfaWidget = getXFAWidget(mPdfViewCtrl, pageIndex, motionEvent);
                break;
            }
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                xfaWidget = mCurrentXFAWidget;
                break;
            default:
                return false;
        }
        if (xfaWidget != null && !xfaWidget.isEmpty()) {
            return onTouchEvent(pageIndex, motionEvent, xfaWidget);
        }

        return false;
    }

    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        if (!((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyXFAForm()) return false;
        boolean isCanceled = false;
        if (mCurrentXFAWidget != null && !mCurrentXFAWidget.isEmpty()) {
            if (onLongPress(pageIndex, motionEvent, mCurrentXFAWidget)) {
                return true;
            }
            if (mCurrentXFAWidget == null) {
                isCanceled = true;
            }
        }

        XFAWidget xfaWidget = getXFAWidget(mPdfViewCtrl, pageIndex, motionEvent);
        if (xfaWidget != null && !xfaWidget.isEmpty()) {
            if (onLongPress(pageIndex, motionEvent, xfaWidget)) {
                return true;
            }
        }

        if (isCanceled) {
            return true;
        }

        return false;
    }

    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        if (!((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyXFAForm()) return false;
        boolean isCanceled = false;
        if (mCurrentXFAWidget != null && !mCurrentXFAWidget.isEmpty()) {
            if (onSingleTapConfirmed(pageIndex, motionEvent, mCurrentXFAWidget)) {
                return true;
            }
            if (mCurrentXFAWidget == null) {
                isCanceled = true;
            }
        }

        XFAWidget xfaWidget = getXFAWidget(mPdfViewCtrl, pageIndex, motionEvent);
        if (xfaWidget != null && !xfaWidget.isEmpty()) {
            if (onSingleTapConfirmed(pageIndex, motionEvent, xfaWidget)) {
                return true;
            }
        }

        if (isCanceled) {
            return true;
        }

        return false;
    }

    private XFAWidget getXFAWidget(PDFViewCtrl pdfViewCtrl, int pageIndex, MotionEvent motionEvent) {
        try {
            PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF pageViewPt = new PointF();
            pdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, pageViewPt, pageIndex);
            PointF pdfPointF = new PointF();
            pdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pdfPointF, pageIndex);

            XFAPage xfaPage = pdfViewCtrl.getXFADoc().getPage(pageIndex);
            Matrix matrix = pdfViewCtrl.getDisplayMatrix(pageIndex);
            if (matrix == null)  return null;
            Matrix2D matrix2D = AppUtil.toMatrix2D(matrix);
            return xfaPage.getWidgetAtDevicePoint(matrix2D, AppUtil.toFxPointF(pageViewPt), 10.0f);
        } catch (PDFException e) {

        }
        return null;
    }

    private void onBackspaceBtnDown(XFAWidget widget) {
        try {
            widget.onChar((char)8, 0);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected XFAWidget getCurrentXFAWidget() {
        return mCurrentXFAWidget;
    }


    // For XFAWidget signature
    private AnnotMenu mAnnotMenu;
    private ArrayList<Integer> mMenuItems;
    private DigitalSignatureUtil mDSUtil;
    private UITextEditDialog mWillSignDialog;
    private boolean mDefaultAdd = false;
    private int mPageIndex = -1;
    private DigitalSignatureSecurityHandler mDSSecurityHandler;
    private boolean isXFAWidgetSignature(XFAWidget xfaWidget) {
        if (xfaWidget == null || xfaWidget.isEmpty()) return false;
        try {
            if (xfaWidget.getType() == XFAWidget.e_WidgetTypeSignature) return true;
        } catch (PDFException e) {
//            e.printStackTrace();
        }
        return false;
    }

    private void showAnnotMenu(boolean isSigned, RectF rectF) {
        if (mAnnotMenu == null) {
            mAnnotMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);
        }

        if (mMenuItems == null) {
            mMenuItems = new ArrayList<Integer>();
        }

        mMenuItems.clear();
        if (!isSigned) {
            mMenuItems.add(AnnotMenu.AM_BT_SIGNATURE);
        } else {
            mMenuItems.add(AnnotMenu.AM_BT_VERIFY_SIGNATURE);
        }

        mMenuItems.add(AnnotMenu.AM_BT_CANCEL);

        mAnnotMenu.setMenuItems(mMenuItems);
        mAnnotMenu.setListener(mMenuListener);
        mAnnotMenu.setShowAlways(true);

        mAnnotMenu.show(rectF);
    }

    private void hideAnnotMenu() {
        if (mAnnotMenu == null) return;
        mAnnotMenu.dismiss();
        mMenuItems.clear();
    }

    private AnnotMenu.ClickListener mMenuListener = new AnnotMenu.ClickListener() {

        @Override
        public void onAMClick(int id) {
            if (AppUtil.isFastDoubleClick()) return;
            if (id == AnnotMenu.AM_BT_SIGNATURE) {
                mAnnotMenu.dismiss();
                selectCertificate();
            } else if (AnnotMenu.AM_BT_VERIFY_SIGNATURE == id) {
                doVerify();
                setCurrentXFAWidget(null);
            } else if (AnnotMenu.AM_BT_CANCEL == id) {
                setCurrentXFAWidget(null);
            }
        }
    };

    private void selectCertificate() {
        if (mDSUtil == null) {
            mDSUtil = new DigitalSignatureUtil(mContext, mPdfViewCtrl);
            initDBTableForDSG();
        }

        mDSUtil.addCertList(new IDigitalSignatureCallBack() {
            @Override
            public void onCertSelect(String path, String name) {
                if (!AppUtil.isEmpty(path) && !AppUtil.isEmpty(name)) {
                    doSign(path);
                } else {
                    setCurrentXFAWidget(null);
                }
            }
        });
    }


    private static final String DB_TABLE_DSG_PFX 			= "_pfx_dsg_cert";
    private static final String PUBLISHER 				= "publisher";
    private static final String ISSUER 					= "issuer";
    private static final String SERIALNUMBER			= "serial_number";
    private static final String FILEPATH				= "file_path";
    private static final String CHANGEFILEPATH				= "file_change_path";
    private static final String FILENAME				= "file_name";
    private static final String PASSWORD				= "password";
    private void initDBTableForDSG() {
        if (!AppSQLite.getInstance(mContext).isTableExist(DB_TABLE_DSG_PFX)) {
            ArrayList<AppSQLite.FieldInfo> fieldInfos  = new ArrayList<AppSQLite.FieldInfo>();
            fieldInfos.add(new AppSQLite.FieldInfo(SERIALNUMBER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new AppSQLite.FieldInfo(ISSUER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new AppSQLite.FieldInfo(PUBLISHER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new AppSQLite.FieldInfo(FILEPATH, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new AppSQLite.FieldInfo(CHANGEFILEPATH, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new AppSQLite.FieldInfo(FILENAME, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new AppSQLite.FieldInfo(PASSWORD, AppSQLite.KEY_TYPE_VARCHAR));
            AppSQLite.getInstance(mContext).createTable(DB_TABLE_DSG_PFX, fieldInfos);
        }
        String filePath = mContext.getFilesDir() + "/DSGCert";
        File file = new File(filePath);
        if(!file.exists()) {
            file.mkdirs();
        }
    }

    private boolean doSign(final String certPath) {
        if (!isXFAWidgetSignature(mCurrentXFAWidget)) return false;
        final Signature signature;
        final RectF rectF;
        try {
            rectF = AppUtil.toRectF(mCurrentXFAWidget.getRect());
            signature = mCurrentXFAWidget.getSignature();
            if (signature == null || signature.isEmpty()) return false;
        } catch (PDFException e) {
            return false;
        }
        if (mDefaultAdd) {
            sign2Doc(signature, certPath, rectF);
            return true;
        }
        if (mWillSignDialog == null || mWillSignDialog.getDialog().getOwnerActivity() == null) {
            if (mPdfViewCtrl.getUIExtensionsManager() == null)  return false;
            Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            if (context == null) return false;
            mWillSignDialog = new UITextEditDialog(context);
            mWillSignDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (AppUtil.isFastDoubleClick()) return;
                    mWillSignDialog.dismiss();
                    setCurrentXFAWidget(null);
                    if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                        mPdfViewCtrl.invalidate();
                    }

                }
            });
            mWillSignDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    setCurrentXFAWidget(null);

                }
            });
            mWillSignDialog.getPromptTextView().setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_sign_dialog_description));
            mWillSignDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.rv_sign_dialog_title));
            mWillSignDialog.getInputEditText().setVisibility(View.GONE);
        }
        mWillSignDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) return;
                mWillSignDialog.dismiss();
                sign2Doc(signature, certPath, rectF);
                mDefaultAdd = true;
            }
        });
        mWillSignDialog.show();
        return true;
    }

    private void sign2Doc(Signature signature, String certPath, RectF rectF) {
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        if (!AppUtil.isEmpty(certPath)) {
            // do digital signature sign
            signDigitalSignature(context, signature, certPath, rectF);
        }
    }

    private UISaveAsDialog mSaveAsDialog;
    private void signDigitalSignature(@NonNull Context context, final Signature signature, final String certPath, final RectF rectF) {
        mSaveAsDialog = new UISaveAsDialog(context, "sign.pdf", "pdf", new UISaveAsDialog.ISaveAsOnOKClickCallBack() {
            @Override
            public void onOkClick(final String newFilePath) {
                String tmpPath = newFilePath;
                final File file = new File(newFilePath);
                if (file.exists()) {
                    tmpPath = newFilePath + "_tmp.pdf";
                }
                final String finalTmpPath = tmpPath;
                mDSUtil.addCertSignature(tmpPath, certPath, signature, rectF, mPageIndex, false, new IDigitalSignatureCreateCallBack() {
                    @Override
                    public void onCreateFinish(boolean success) {
                        if (!success){
                            File file = new File(finalTmpPath);
                            file.delete();
                            return;
                        }

                        File newFile = new File(newFilePath);
                        File file = new File(finalTmpPath);
                        file.renameTo(newFile);

                        if (!mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            setCurrentXFAWidget(null);
                            return;
                        }

                        mPdfViewCtrl.cancelAllTask();
                        setCurrentXFAWidget(null);
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().clearUndoRedo();
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(false);
                        mPdfViewCtrl.openDoc(newFilePath, null);
                        updateThumbnail(newFilePath);
                    }
                });
            }

            @Override
            public void onCancelClick() {
                mWillSignDialog.dismiss();
                setCurrentXFAWidget(null);
            }
        });

        mSaveAsDialog.setOnCancelListener(new UISaveAsDialog.ICancelListener() {
            @Override
            public void onCancelListener() {
                setCurrentXFAWidget(null);
            }
        });
        mSaveAsDialog.showDialog();
        return;
    }

    private void updateThumbnail(String path){
        LocalModule module = (LocalModule) ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager())
                .getModuleByName(Module.MODULE_NAME_LOCAL);
        if (module != null) {
            module.updateThumbnail(path);
        }
    }

    private void doVerify() {
        if (mCurrentXFAWidget == null || mCurrentXFAWidget.isEmpty()) return;
        try {
            Signature signature = mCurrentXFAWidget.getSignature();
            if (signature == null || signature.isEmpty() || !signature.isSigned()) return;
            verifySignature(signature);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void verifySignature(final Signature signature) {
        if (mDSSecurityHandler == null) {
//            mDSSecurityHandler = new DigitalSignatureSecurityHandler(mContext, mPdfViewCtrl, null);
            DigitalSignatureModule module = (DigitalSignatureModule) ((UIExtensionsManager)(mPdfViewCtrl.getUIExtensionsManager())).getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE);
            if (module == null) return;
            mDSSecurityHandler = module.getSecurityHandler();
        }

        mDSSecurityHandler.verifySignature(signature);
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        if (mSaveAsDialog != null && mSaveAsDialog.isShowing()) {
            mSaveAsDialog.setHeight(mSaveAsDialog.getDialogHeight());
            mSaveAsDialog.showDialog();
        }
    }

    public void onDrawForControls(Canvas canvas) {
        if (mAnnotMenu == null || mCurrentXFAWidget == null || mCurrentXFAWidget.isEmpty()) return;
        try {
            int pageIndex = mCurrentXFAWidget.getXFAPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF bbox = AppUtil.toRectF(mCurrentXFAWidget.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
                mAnnotMenu.update(bbox);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }
}
