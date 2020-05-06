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
package com.foxit.uiextensions.annots.note;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
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
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Note;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
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

//ToolHandler is for creation, and AnnotHandler is for edition.
public class NoteToolHandler implements ToolHandler {
    private Context mContext;
    private AppDisplay mDisplay;
    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiExtensionsManager;

    private EditText mET_Content;
    private TextView mDialog_title;
    private Button mCancel;
    private Button mSave;
    private int mColor;
    private int mOpacity;
    private int mIconType;

    private boolean mIsContinuousCreate;

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    private PropertyBar mPropertyBar;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    public NoteToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mDisplay = new AppDisplay(context);
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        mPropertyBar = mUiExtensionsManager.getMainFrame().getPropertyBar();
        mUiExtensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                mUiExtensionsManager.setCurrentToolHandler(NoteToolHandler.this);
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_NOTE;
            }
        });
    }

    @Override
    public void onActivate() {
        resetPropertyBar();
        resetAnnotBar();
    }

    protected void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    protected void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

    private void resetPropertyBar() {
        int[] colors = new int[PropertyBar.PB_COLORS_NOTE.length];
        System.arraycopy(PropertyBar.PB_COLORS_NOTE, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_NOTE[0];
        mPropertyBar.setColors(colors);

        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, mOpacity);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_ANNOT_TYPE, mIconType);
        mPropertyBar.setArrowVisible(true);
        mPropertyBar.reset(getSupportedProperties());
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_ANNOT_TYPE;
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
                if (NoteToolHandler.this == mUiExtensionsManager.getCurrentToolHandler()) {
                    if (mPropertyBar.isShowing()) {
                        Rect rect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                        mPropertyBar.update(new RectF(rect));
                    }
                }
            }
        };
        mPropertyItem.setTag(ToolbarItemConfig.ITEM_ANNOT_PROPERTY);
        mPropertyItem.setCentreCircleColor(mColor);

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
    public void onDraw(int pageIndex, Canvas canvas) {
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pageViewPt, pageIndex);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initDialog(pageIndex, pageViewPt);
                return true;
        }
        return false;
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

    //show dialog to input note content
    protected void initDialog(final int pageIndex, final PointF point) {
        Context context = mUiExtensionsManager.getAttachedActivity();
        if (context == null) {
            return;
        }
        final Dialog dialog;
        View mView = View.inflate(context.getApplicationContext(), R.layout.rd_note_dialog_edit, null);
        mDialog_title = (TextView) mView.findViewById(R.id.rd_note_dialog_edit_title);
        mET_Content = (EditText) mView.findViewById(R.id.rd_note_dialog_edit);
        mCancel = (Button) mView.findViewById(R.id.rd_note_dialog_edit_cancel);
        mSave = (Button) mView.findViewById(R.id.rd_note_dialog_edit_ok);

        mView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog = new Dialog(context, R.style.rv_dialog_style);
        dialog.setContentView(mView, new ViewGroup.LayoutParams(mDisplay.getUITextEditDialogWidth(), ViewGroup.LayoutParams.WRAP_CONTENT));
        mET_Content.setMaxLines(10);

        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dlg_title_bg_4circle_corner_white);
        mDialog_title.setText(mContext.getApplicationContext().getString(R.string.fx_string_note));

        mCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                AppUtil.dismissInputSoft(mET_Content);
            }
        });
        mSave.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PointF pdfPoint = new PointF(point.x, point.y);
//                final RectF rect = new RectF(pdfPoint.x - 10, pdfPoint.y + 10, pdfPoint.x + 10, pdfPoint.y - 10);
                final RectF rect = new RectF(pdfPoint.x, pdfPoint.y, pdfPoint.x + 20, pdfPoint.y - 20);

                try {
                    final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                    final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Note, AppUtil.toFxRectF(rect)), Annot.e_Note);
                    if (annot == null) {
                        if (!mIsContinuousCreate) {
                           mUiExtensionsManager.setCurrentToolHandler(null);
                        }
                        dialog.dismiss();
                        return;
                    }

                    final NoteAddUndoItem undoItem = new NoteAddUndoItem(mPdfViewCtrl);
                    undoItem.mPageIndex = pageIndex;
                    undoItem.mNM = AppDmUtil.randomUUID(null);
                    undoItem.mContents = mET_Content.getText().toString();
                    undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
                    undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
                    undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
                    undoItem.mFlags = Annot.e_FlagPrint | Annot.e_FlagNoZoom | Annot.e_FlagNoRotate;
                    undoItem.mColor = mColor;
                    undoItem.mOpacity = AppDmUtil.opacity100To255(mOpacity) / 255f;
                    undoItem.mOpenStatus = false;
                    undoItem.mIconName = NoteUtil.getIconNameByType(mIconType);
                    undoItem.mBBox = new RectF(rect);
                    undoItem.mSubject = "Note";

                    addAnnot(pageIndex, annot, undoItem, true, null);

                    dialog.dismiss();
                    AppUtil.dismissInputSoft(mET_Content);

                    if (!mIsContinuousCreate) {
                        mUiExtensionsManager.setCurrentToolHandler(null);
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        });
        dialog.show();

        AppUtil.showSoftInput(mET_Content);
    }

    protected void addAnnot(int pageIndex, NoteAnnotContent content, boolean addUndo, Event.Callback result) {
        if (content.getFromType().equals(Module.MODULE_NAME_SELECTION)) {
            PointF point = new PointF(content.getBBox().left, content.getBBox().top);
//            mPdfViewCtrl.convertPdfPtToPageViewPt(point, point, pageIndex);
            initDialog(pageIndex, point);// use pdf point now.
        } else if (content.getFromType().equals(Module.MODULE_NAME_REPLY)) {
            NoteAddUndoItem undoItem = new NoteAddUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(content);
            undoItem.mIsFromReplyModule = true;
            undoItem.mParentNM = content.getParentNM();
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();

            try {
                PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                Annot annot = mUiExtensionsManager.getDocumentManager().getAnnot(page, undoItem.mParentNM);
                Note reply = ((Markup) annot).addReply();
                addAnnot(pageIndex, reply, undoItem, addUndo, result);
            } catch (PDFException e) {
                e.printStackTrace();
            }

        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    protected void addAnnot(final int pageIndex, final Annot annot, final NoteAddUndoItem undoItem, final boolean addUndo, final Event.Callback result) {
        NoteEvent event = new NoteEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, (Note) annot, mPdfViewCtrl);

        final EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success) {
                    try {
                        mUiExtensionsManager.getDocumentManager().onAnnotAdded(annot.getPage(), annot);
                        if (addUndo) {
                            mUiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                        }

                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
                            final RectF pageViewRect = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                            Rect rectResult = new Rect();
                            pageViewRect.roundOut(rectResult);
                            rectResult.inset(-10, -10);
                            mPdfViewCtrl.refresh(pageIndex, rectResult);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }

                if (result != null) {
                    result.result(event, success);
                }
            }
        });
        mPdfViewCtrl.addTask(task);

    }

    protected void setColor(int color) {
        mColor = color;
        setProItemColor(color);
    }

    protected void setOpacity(int opacity) {
        mOpacity = opacity;
    }

    protected void setIconType(int iconType) {
        mIconType = iconType;
    }

    private void setProItemColor(int color){
        if (mPropertyItem == null) return;
        mPropertyItem.setCentreCircleColor(color);
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_NOTE;
    }

}
