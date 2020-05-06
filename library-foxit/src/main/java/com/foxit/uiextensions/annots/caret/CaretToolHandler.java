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
package com.foxit.uiextensions.annots.caret;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.TextPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.sdk.pdf.objects.PDFObject;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContentAbs;
import com.foxit.uiextensions.annots.textmarkup.TextSelector;
import com.foxit.uiextensions.annots.textmarkup.strikeout.StrikeoutEvent;
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
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

public class CaretToolHandler implements ToolHandler {
    private final Context mContext;
    private final PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiextensionsManager;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;
    private PropertyBar mPropertyBar;

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    private int mOpacity;
    private boolean mIsInsertTextModule;

    private int[] mColors;
    private int mColor;

    private Dialog mDialog;
    private EditText mDlgContent;

    private final RectF mCharSelectedRectF;
    private final TextSelector mTextSelector;
    private int mSelectedPageIndex;
    private boolean mIsContinuousCreate = false;
    private final Paint mPaint;
    private boolean mRPLCreating = false;
    private boolean mSelecting = false;
    private int mCaretRotate = 0;

    public CaretToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiextensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
        mPropertyBar = mUiextensionsManager.getMainFrame().getPropertyBar();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        mTextSelector = new TextSelector(mPdfViewCtrl);
        mCharSelectedRectF = new RectF();

        mRPLCreating = false;
    }

    protected void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    protected void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

    protected void init(boolean isInsertTextModule) {
        mIsInsertTextModule = isInsertTextModule;
        mColors = PropertyBar.PB_COLORS_CARET;
        mOpacity = 100;

        if (mIsInsertTextModule){
            mColor = mUiextensionsManager.getConfig().uiSettings.annotations.insert.color;
            mOpacity = (int) (mUiextensionsManager.getConfig().uiSettings.annotations.insert.opacity * 100);
            mUiextensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
                @Override
                public void onMTClick(int type) {
                    mUiextensionsManager.setCurrentToolHandler(CaretToolHandler.this);
                    mUiextensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                }

                @Override
                public int getType() {
                    return MoreTools.MT_TYPE_INSERT;
                }
            });
        } else {
            mColor = mUiextensionsManager.getConfig().uiSettings.annotations.replace.color;
            mOpacity = (int) (mUiextensionsManager.getConfig().uiSettings.annotations.replace.opacity * 100);
            mUiextensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
                @Override
                public void onMTClick(int type) {
                    mUiextensionsManager.setCurrentToolHandler(CaretToolHandler.this);
                    mUiextensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                }

                @Override
                public int getType() {
                    return MoreTools.MT_TYPE_REPLACE;
                }
            });
        }
    }

    protected void changeCurrentColor(int currentColor) {
        mColor = currentColor;
        setProItemColor(currentColor);
    }

    protected void changeCurrentOpacity(int currentOpacity) {
        mOpacity = currentOpacity;
    }

    private void setProItemColor(int color){
        if (mPropertyItem == null) return;
        mPropertyItem.setCentreCircleColor(color);
    }

    @Override
    public String getType() {
        if (mIsInsertTextModule)
            return ToolHandler.TH_TYPR_INSERTTEXT;
        return ToolHandler.TH_TYPE_REPLACE;
    }

    @Override
    public void onActivate() {
        mTextSelector.clear();
        mCharSelectedRectF.setEmpty();
        resetPropertyBar();
        resetAnnotBar();
    }

    @Override
    public void onDeactivate() {
        mCharSelectedRectF.setEmpty();
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
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    private int getCharIndexAtPoint(int pageIndex, PointF point) {
        int index = 0;
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            index = getCharIndexAtPoint(page, point);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return index;
    }

    private void resetPropertyBar() {
        long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
        System.arraycopy(PropertyBar.PB_COLORS_CARET, 0, mColors, 0, mColors.length);
        mPropertyBar.setColors(mColors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, mOpacity);
        mPropertyBar.reset(supportProperty);
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    private void resetAnnotBar(){
        mUiextensionsManager.getMainFrame().getToolSetBar().removeAllItems();
        mPropertyItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (CaretToolHandler.this == mUiextensionsManager.getCurrentToolHandler()) {
                    if (mPropertyBar.isShowing()) {
                        Rect mProRect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(mProRect);
                        mPropertyBar.update(new RectF(mProRect));
                    }
                }
            }
        };

        mPropertyItem.setTag(ToolbarItemConfig.ITEM_ANNOT_PROPERTY);
        mPropertyItem.setCentreCircleColor(mColor);
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPropertyBar.setArrowVisible(true);
                Rect rect = new Rect();
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


        mOKItem = new BaseItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mUiextensionsManager.setCurrentToolHandler(null);
                mUiextensionsManager.changeState(ReadStateConfig.STATE_EDIT);
            }
        });

        mUiextensionsManager.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mUiextensionsManager.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mUiextensionsManager.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
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

    private boolean onSelectDown(int pageIndex, PointF point, TextSelector selectInfo) {
        if (selectInfo == null) return false;
        int index = getCharIndexAtPoint(pageIndex, point);
        if (index >= 0) {
            selectInfo.setStart(index);
            selectInfo.setEnd(index);

            return true;
        }
        return false;
    }

    private boolean onSelectMove(int pageIndex, PointF point, TextSelector selectInfo) {
        if (selectInfo == null || selectInfo.getStart() < 0)
            return false;
        if (mSelectedPageIndex != pageIndex)
            return false;

        int index = getCharIndexAtPoint(pageIndex, point);
        if (index >= 0) {
            selectInfo.setEnd(index);
            return true;
        }
        return false;
    }

    private boolean onSelectRelease(final int pageIndex, final TextSelector selectorInfo) {
        try {
            if (!mIsInsertTextModule && mRPLCreating) {
                if (selectorInfo.getStart() >= 0 && selectorInfo.getEnd() >= 0) {
                    selectorInfo.computeSelected(mPdfViewCtrl.getDoc().getPage(pageIndex), selectorInfo.getStart(), selectorInfo.getEnd());
                    if (selectorInfo.getRectFList().size() > 0){
                        mCharSelectedRectF.set(selectorInfo.getRectFList().get(selectorInfo.getRectFList().size() - 1));
                    }
                }
                View.OnClickListener cancelClickListener = new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mDialog.dismiss();
                        AppUtil.dismissInputSoft(mDlgContent);
                        selectorInfo.clear();
                    }
                };
                View.OnClickListener okClickListener = new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        //add Caret Annotation
                        addCaretAnnot(pageIndex, getCaretRectFromSelectRect(selectorInfo, pageIndex, null), mCaretRotate, mTextSelector, null);
                        mDialog.dismiss();
                        AppUtil.dismissInputSoft(mDlgContent);

                        if (!mIsContinuousCreate) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        }
                    }
                };
                initDialog(cancelClickListener, okClickListener);
                mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mSelecting = false;
                        mRPLCreating = false;
                        RectF selectedRectF = new RectF(mCharSelectedRectF);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(selectedRectF, selectedRectF, pageIndex);
                        Rect selectedRect = new Rect();
                        selectedRectF.roundOut(selectedRect);
                        getInvalidateRect(selectedRect);
                        mPdfViewCtrl.invalidate(selectedRect);
                        mCharSelectedRectF.setEmpty();
                    }
                });

                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private int getCharIndexAtPoint(PDFPage page, PointF pdfPt) {
        int index = 0;
        try {
            TextPage textSelect = new TextPage(page, TextPage.e_ParseTextNormal);
            index = textSelect.getIndexAtPos(pdfPt.x, pdfPt.y, 10);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return index;
    }

    //Non standard dictionary entry, Adobe Acrobat and Foxit RDK support, but not supported by the Foxit Phantom
    private void setCaretRotate(Annot caret, int rotate) {
        if (!(caret instanceof Caret))
            return;
        if (rotate < Constants.e_Rotation0 || rotate > Constants.e_RotationUnknown)
            rotate = 0;
        try {
            caret.getDict().setAt("Rotate", PDFObject.createFromInteger(360 - rotate * 90));
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private RectF getCaretRectFromSelectRect(TextSelector selector, int pageIndex, PointF point) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            TextPage textSelect = new TextPage(page, TextPage.e_ParseTextNormal);
            int start = Math.min(selector.getStart(), selector.getEnd());
            int end = Math.max(selector.getStart(), selector.getEnd());
            int nCount = textSelect.getTextRectCount(start, end - start + 1);
            RectF docSelectedRectF = AppUtil.toRectF(textSelect.getTextRect(nCount - 1));
            mCaretRotate = textSelect.getBaselineRotation(nCount - 1) % Constants.e_RotationUnknown;
            RectF caretRect = new RectF();
            float w, h;
            if (mCaretRotate % 2 != 0) {
                w = (docSelectedRectF.right - docSelectedRectF.left);
                h = w * 2 / 3;
            } else {
                h = (docSelectedRectF.top - docSelectedRectF.bottom);
                w = h * 2 / 3;
            }
            float offsetY = h * 0.9f;
            float offsetX = w * 0.9f;
            switch (mCaretRotate) {
                case Constants.e_Rotation0:{
                    if ((point != null && point.x - docSelectedRectF.left <= (docSelectedRectF.right - docSelectedRectF.left) / 2)) {
                        caretRect.set(docSelectedRectF.left - w / 2, docSelectedRectF.bottom + h, docSelectedRectF.left + w / 2, docSelectedRectF.bottom);
                    } else {
                        caretRect.set(docSelectedRectF.right - w / 2, docSelectedRectF.bottom + h, docSelectedRectF.right + w / 2, docSelectedRectF.bottom);
                    }

                    caretRect.offset(0, 0 - offsetY);
                }
                break;
                case Constants.e_Rotation90: {
                    if ((point != null && point.y - docSelectedRectF.bottom >= (docSelectedRectF.top - docSelectedRectF.bottom) / 2)) {
                        caretRect.set(docSelectedRectF.left, docSelectedRectF.top + h / 2, docSelectedRectF.left + w, docSelectedRectF.top - h / 2);
                    } else {
                        caretRect.set(docSelectedRectF.left, docSelectedRectF.bottom + h / 2, docSelectedRectF.left + w, docSelectedRectF.bottom - h / 2);
                    }
                    caretRect.offset(0 - offsetX, 0);
                }
                break;
                case Constants.e_Rotation180: {
                    if ((point != null && point.x - docSelectedRectF.left >= (docSelectedRectF.right - docSelectedRectF.left) / 2)) {
                        caretRect.set(docSelectedRectF.right - w / 2, docSelectedRectF.top, docSelectedRectF.right + w / 2, docSelectedRectF.top -h);
                    } else {
                        caretRect.set(docSelectedRectF.left - w / 2, docSelectedRectF.top, docSelectedRectF.left + w / 2, docSelectedRectF.top -h);
                    }
                    caretRect.offset(0, offsetY);

                }
                break;
                case Constants.e_Rotation270: {
                    if ((point != null && point.y - docSelectedRectF.bottom <= (docSelectedRectF.top - docSelectedRectF.bottom) / 2)) {
                        caretRect.set(docSelectedRectF.right - w, docSelectedRectF.bottom + h / 2, docSelectedRectF.right, docSelectedRectF.bottom - h / 2);
                    } else {
                        caretRect.set(docSelectedRectF.right - w, docSelectedRectF.top + h / 2, docSelectedRectF.right, docSelectedRectF.top - h / 2);
                    }
                    caretRect.offset(offsetX, 0);
                }
                break;
                default:
                    break;
            }

            return caretRect;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public boolean onTouchEvent(final int pageIndex, MotionEvent motionEvent) {
        final PointF point = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        try {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if (mIsInsertTextModule) {
                        mTextSelector.clear();
                        mSelectedPageIndex = pageIndex;
                        PointF docPoint = new PointF(point.x, point.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(docPoint, docPoint, pageIndex);

                        final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                        int index = getCharIndexAtPoint(page, docPoint);
                        if (index == -1) {
                            return true;
                        }

                        if (index >= 0) {
                            mSelecting = true;
                            mTextSelector.setStart(index);
                            mTextSelector.setEnd(index);

                            mTextSelector.computeSelected(page, mTextSelector.getStart(), mTextSelector.getEnd());
                            mCharSelectedRectF.set(mTextSelector.getBbox());
                            invalidateTouch(pageIndex, mTextSelector);
                            final PointF pointTemp = new PointF(point.x, point.y);
                            View.OnClickListener cancelClickListener = new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    mDialog.dismiss();
                                    AppUtil.dismissInputSoft(mDlgContent);

                                }
                            };
                            View.OnClickListener okClickListener = new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {

                                    PointF pdfPoint = new PointF(pointTemp.x, pointTemp.y);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPoint, pdfPoint, pageIndex);
                                    addCaretAnnot(pageIndex, getCaretRectFromSelectRect(mTextSelector, pageIndex, pdfPoint), mCaretRotate, mTextSelector, null);
                                    mDialog.dismiss();
                                    AppUtil.dismissInputSoft(mDlgContent);
                                    if (!mIsContinuousCreate) {
                                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                                    }
                                }
                            };
                            initDialog(cancelClickListener, okClickListener);
                            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    mSelecting = false;
                                    RectF selectedRectF = new RectF(mCharSelectedRectF);
                                    clearSelectedRectF();
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(selectedRectF, selectedRectF, pageIndex);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(selectedRectF, selectedRectF, pageIndex);
                                    Rect selectedRect = new Rect();
                                    selectedRectF.roundOut(selectedRect);
                                    getInvalidateRect(selectedRect);
                                    mPdfViewCtrl.invalidate(selectedRect);
                                }
                            });

                            return true;
                        }
                    } else {
                        mTextSelector.clear();
                        mSelectedPageIndex = pageIndex;
                        PointF docPoint = new PointF(point.x, point.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(docPoint, docPoint, pageIndex);
                        mRPLCreating = onSelectDown(pageIndex, docPoint, mTextSelector);
                        mSelecting = mRPLCreating;
                        return mRPLCreating;
                    }
                    return false;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (!mIsInsertTextModule && mRPLCreating) {
                        PointF docPoint = new PointF(point.x, point.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(docPoint, docPoint, pageIndex);
                        if (onSelectMove(pageIndex, docPoint, mTextSelector)) {
                            mTextSelector.computeSelected(mPdfViewCtrl.getDoc().getPage(pageIndex), mTextSelector.getStart(), mTextSelector.getEnd());
                            invalidateTouch(pageIndex, mTextSelector);
                            return true;
                        }
                    }
                }
                break;
                case MotionEvent.ACTION_CANCEL:
                    break;
                case MotionEvent.ACTION_UP:
                    return onSelectRelease(pageIndex, mTextSelector);
                default:
                    break;
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void initDialog(View.OnClickListener cancelClickListener, View.OnClickListener okClickListener) {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        View mView = View.inflate(mContext, R.layout.rd_note_dialog_edit, null);
        final TextView dialogTitle = (TextView) mView.findViewById(R.id.rd_note_dialog_edit_title);
        mDlgContent = (EditText) mView.findViewById(R.id.rd_note_dialog_edit);
        final Button cancelButton = (Button) mView.findViewById(R.id.rd_note_dialog_edit_cancel);
        final Button applayButton = (Button) mView.findViewById(R.id.rd_note_dialog_edit_ok);

        mView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mDialog = new Dialog(context, R.style.rv_dialog_style);
        mDialog.setContentView(mView, new ViewGroup.LayoutParams(AppDisplay.getInstance(mContext).getUITextEditDialogWidth(), ViewGroup.LayoutParams.WRAP_CONTENT));
        mDlgContent.setMaxLines(10);

        mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mDialog.getWindow().setBackgroundDrawableResource(R.drawable.dlg_title_bg_4circle_corner_white);

        if (mIsInsertTextModule) {
            dialogTitle.setText(mContext.getApplicationContext().getString(R.string.fx_string_inserttext));
        } else {
            dialogTitle.setText(mContext.getApplicationContext().getString(R.string.fx_string_replacetext));
        }

        applayButton.setEnabled(false);
        //noinspection deprecation
        applayButton.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));

        mDlgContent.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mDlgContent.getText().length() == 0) {
                    applayButton.setEnabled(false);
                    //noinspection deprecation
                    applayButton.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));
                } else {
                    applayButton.setEnabled(true);
                    //noinspection deprecation
                    applayButton.setTextColor(mContext.getResources().getColor(R.color.dlg_bt_text_selector));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        cancelButton.setOnClickListener(cancelClickListener);
        applayButton.setOnClickListener(okClickListener);
        mDialog.show();
        AppUtil.showSoftInput(mDlgContent);
    }


    protected void addAnnot(final int pageIndex, final CaretAnnotContent content, boolean addUndo, final Event.Callback result) {

        CaretAddUndoItem undoItem = new CaretAddUndoItem(mPdfViewCtrl);
        undoItem.setCurrentValue(content);
        try {
            //step 1 add annot to pdf
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            RectF docRect = content.getBBox();
            final Caret caret = (Caret) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Caret, AppUtil.toFxRectF(docRect)), Annot.e_Caret);
            if(caret == null) return;

            undoItem.mAuthor = content.getAuthor();
            undoItem.mContents = content.getContents();
            undoItem.mCreationDate = content.getCreatedDate();
            undoItem.mModifiedDate = content.getModifiedDate();
            undoItem.mRotate = content.getRotate();
            TextSelector selector = new TextSelector(mPdfViewCtrl);
            selector.setStart(mTextSelector.getStart());
            selector.setEnd(mTextSelector.getEnd());
            selector.computeSelected(page, selector.getStart(), selector.getEnd());
            selector.setContents(mTextSelector.getContents());
            undoItem.mTextSelector = selector;
            undoItem.mIsReplace = !mIsInsertTextModule;
            addAnnot(caret, undoItem, addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void addAnnot(final Annot annot, final CaretAddUndoItem undoItem, final boolean addUndo, final Event.Callback result) {
        //add replace strikeout
        if (!mIsInsertTextModule) {
            TextMarkupContentAbs strikeoutAbs = new TextMarkupContentAbs() {
                @Override
                public TextSelector getTextSelector() {
                    return undoItem.mTextSelector;
                }

                @Override
                public int getPageIndex() {
                    return undoItem.mPageIndex;
                }

                @Override
                public int getType() {
                    return Annot.e_StrikeOut;
                }

                @Override
                public String getIntent() {
                    return "StrikeOutTextEdit";
                }

                @Override
                public int getColor() {
                    return (int)undoItem.mColor;
                }

                @Override
                public int getOpacity() {
                    return (int)(undoItem.mOpacity * 255f + 0.5f);
                }

                @Override
                public String getSubject() {
                    return "Replace";
                }
            };

            AnnotHandler annotHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_StrikeOut);
            annotHandler.addAnnot(undoItem.mPageIndex, strikeoutAbs, false, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (!(event instanceof StrikeoutEvent)) return;
                    undoItem.strikeOutEvent = (StrikeoutEvent) event;
                }
            });
        }

        CaretEvent event = new CaretEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, (Caret) annot, mPdfViewCtrl);
        EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success) {
                    try {
                        final PDFPage page = annot.getPage();
                        final int pageIndex = page.getIndex();
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);

                        if (addUndo) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }

                        //step 2 invalidate page view
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF viewRect = AppUtil.toRectF(annot.getRect());
                            if (!mIsInsertTextModule) {
                                StrikeOut strikeOut = AppAnnotUtil.getStrikeOutFromCaret((Caret) annot);
                                if (strikeOut != null && !strikeOut.isEmpty()) {
                                    RectF sto_Rect = AppUtil.toRectF(strikeOut.getRect());
                                    sto_Rect.union(viewRect);

                                    viewRect.set(sto_Rect);
                                }
                            }
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            Rect rect = new Rect();
                            viewRect.roundOut(rect);
                            rect.inset(-10, -10);
                            mPdfViewCtrl.refresh(pageIndex, rect);
                        }

                        if (result != null) {
                            result.result(null, success);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mPdfViewCtrl.addTask(task);
    }

    private void addCaretAnnot(final int pageIndex, final RectF annotRect, final int rotate, final TextSelector textSelector, final Event.Callback result) {
        final DateTime dateTime = AppDmUtil.currentDateToDocumentDate();
        CaretAnnotContent caretAnnotContent = new CaretAnnotContent(mIsInsertTextModule) {
            @Override
            public int getPageIndex() {
                return pageIndex;
            }

            @Override
            public int getType() {
                return Annot.e_Caret;
            }

            @Override
            public float getLineWidth() {
                return 0;
            }

            @Override
            public int getRotate() {
                return rotate;
            }

            @Override
            public TextSelector getTextSelector() {
                return textSelector;
            }

            @Override
            public DateTime getCreatedDate() {
                return dateTime;
            }

            @Override
            public RectF getBBox() {
                return annotRect;
            }

            @Override
            public int getColor() {
                return mColor;
            }

            @Override
            public int getOpacity() {
                return AppDmUtil.opacity100To255(mOpacity);
            }


            @Override
            public DateTime getModifiedDate() {
                return dateTime;
            }

            @Override
            public String getContents() {
                return mDlgContent.getText().toString();
            }

        };

        addAnnot(pageIndex, caretAnnotContent, true, result);

    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {

        if (mSelectedPageIndex != pageIndex) return;
        if (mIsInsertTextModule) {
            if (mSelecting && mTextSelector != null && !(mCharSelectedRectF.left >= mCharSelectedRectF.right || mCharSelectedRectF.top <= mCharSelectedRectF.bottom)) {
                mPaint.setColor(calColorByMultiply(0x73C1E1, 150));
                Rect clipRect = canvas.getClipBounds();
                RectF tmp = new RectF(mTextSelector.getBbox());
                mPdfViewCtrl.convertPdfRectToPageViewRect(tmp, tmp, pageIndex);
                Rect r = new Rect();
                tmp.round(r);
                if (r.intersect(clipRect)) {
                    canvas.save();
                    canvas.drawRect(r, mPaint);

                    if (mTextSelector.getRectFList().size() > 0) {
                        RectF start = new RectF(mTextSelector.getRectFList().get(0));
                        RectF end = new RectF(mTextSelector.getRectFList().get(mTextSelector.getRectFList().size() - 1));
                        mPdfViewCtrl.convertPdfRectToPageViewRect(start, start, pageIndex);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(end, end, pageIndex);

                        mPaint.setARGB(255, 76, 121, 164);
                        canvas.drawLine(start.left, start.top, start.left, start.bottom, mPaint);
                        canvas.drawLine(end.right, end.top, end.right, end.bottom, mPaint);
                    }

                    canvas.restore();
                }
            }
        } else {
            if (mSelecting && mTextSelector != null && mTextSelector.getStart() >= 0) {
                mPaint.setColor(calColorByMultiply(0x73C1E1, 150));
                Rect clipRect = canvas.getClipBounds();
                for (RectF rect : mTextSelector.getRectFList()) {
                    RectF tmp = new RectF(rect);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(tmp, tmp, pageIndex);
                    Rect r = new Rect();
                    tmp.round(r);
                    if (r.intersect(clipRect)) {
                        canvas.save();
                        canvas.drawRect(r, mPaint);
                        canvas.restore();
                    }
                }
                if (mTextSelector.getRectFList().size() > 0) {
                    RectF start = new RectF(mTextSelector.getRectFList().get(0));
                    RectF end = new RectF(mTextSelector.getRectFList().get(mTextSelector.getRectFList().size() - 1));
                    mPdfViewCtrl.convertPdfRectToPageViewRect(start, start, pageIndex);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(end, end, pageIndex);

                    mPaint.setARGB(255, 76, 121, 164);
                    canvas.drawLine(start.left, start.top, start.left, start.bottom, mPaint);
                    canvas.drawLine(end.right, end.top, end.right, end.bottom, mPaint);
                }
            }
        }

    }

    private final RectF mTmpRect = new RectF();

    private void invalidateTouch(int pageIndex, TextSelector textSelector) {

        if (textSelector == null) return;
        RectF rectF = new RectF();
        rectF.set(textSelector.getBbox());
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
        RectF rF = calculate(rectF, mTmpRect);
        Rect rect = new Rect();
        rF.roundOut(rect);
        getInvalidateRect(rect);
        mPdfViewCtrl.invalidate(rect);
        mTmpRect.set(rectF);
    }

    private RectF calculate(RectF desRectF, RectF srcRectF) {
        RectF mTmpDesRect = new RectF();
        if (srcRectF.isEmpty()) return desRectF;
        int count = 0;
        if (desRectF.left == srcRectF.left && desRectF.top == srcRectF.top) count++;
        if (desRectF.right == srcRectF.right && desRectF.top == srcRectF.top) count++;
        if (desRectF.left == srcRectF.left && desRectF.bottom == srcRectF.bottom) count++;
        if (desRectF.right == srcRectF.right && desRectF.bottom == srcRectF.bottom) count++;
        mTmpDesRect.set(desRectF);
        if (count == 2) {
            mTmpDesRect.union(srcRectF);
            RectF rectF = new RectF();
            rectF.set(mTmpDesRect);
            mTmpDesRect.intersect(srcRectF);
            rectF.intersect(mTmpDesRect);
            return rectF;
        } else if (count == 3 || count == 4) {
            return mTmpDesRect;
        } else {
            mTmpDesRect.union(srcRectF);
            return mTmpDesRect;
        }
    }

    private void getInvalidateRect(Rect rect) {
        rect.top -= 20;
        rect.bottom += 20;
        rect.left -= 20 / 2;
        rect.right += 20 / 2;
        rect.inset(-20, -20);
    }

    private void clearSelectedRectF() {
        mTextSelector.clear();
        mCharSelectedRectF.setEmpty();
    }

    private int calColorByMultiply(int color, int opacity) {
        int rColor = color | 0xFF000000;
        int r = (rColor & 0xFF0000) >> 16;
        int g = (rColor & 0xFF00) >> 8;
        int b = (rColor & 0xFF);
        float rOpacity = opacity / 255.0f;
        r = (int) (r * rOpacity + 255 * (1 - rOpacity));
        g = (int) (g * rOpacity + 255 * (1 - rOpacity));
        b = (int) (b * rOpacity + 255 * (1 - rOpacity));
        rColor = (rColor & 0xFF000000) | (r << 16) | (g << 8) | (b);
        return rColor;
    }

}
