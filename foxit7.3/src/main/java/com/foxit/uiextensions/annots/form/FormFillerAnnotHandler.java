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
package com.foxit.uiextensions.annots.form;

import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.Control;
import com.foxit.sdk.pdf.interform.Field;
import com.foxit.sdk.pdf.interform.Filler;
import com.foxit.sdk.pdf.interform.Form;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.form.undo.FormFillerDeleteUndoItem;
import com.foxit.uiextensions.annots.form.undo.FormFillerModifyUndoItem;
import com.foxit.uiextensions.annots.form.undo.FormFillerUndoItem;
import com.foxit.uiextensions.annots.freetext.FtTextUtil;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.BaseDialogFragment;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.IResult;
import com.foxit.uiextensions.utils.UIToast;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import androidx.fragment.app.FragmentActivity;

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

    private PointF mDownPoint;
    private PointF mLastPoint;
    private PointF mLastTouchPoint = new PointF(0, 0);

    private PropertyBar mPropertyBar;
    private FtTextUtil mTextUtil;

    private Blink mBlink = null;
    private AnnotMenu mAnnotationMenu;
    private Annot mLastAnnot;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private Filler mFormFiller;
    private FormFillerAssistImpl mAssist;
    private Form mForm;
    private EditText mEditView = null;
    private FormNavigationModule mFNModule = null;

    private ArrayList<Integer> mMenuText;
    private String mLastInputText = "";
    private String mChangeText = null;
    private String mLastFieldName = "";
    private Paint mPathPaint;
    private int mOffset;
    private int mPageOffset;
    private int mLastPageIndex;

    private boolean mTouchCaptured = false;
    private boolean mIsBackBtnPush = false; //for some input method, double backspace click
    private boolean mAdjustPosition = false;
    private boolean mIsShowEditText = false;
    private boolean bInitialize = false;
    private boolean mIsModify = false;
    private boolean mIsLongPressTouchEvent = false;
    private boolean mIsEdittextOffset = false;
    private boolean mAnnotIsSelected = false;
    private int mCurrentRadionCheckIndex = -1;

    public FormFillerAnnotHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
    }

    public void init(final Form form) {
        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl);
        mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);

        mAssist = new FormFillerAssistImpl(mPdfViewCtrl, new FillerFocusEventListener() {
            @Override
            public void focusGotOnControl(Control control, String filedValue) {
            }

            @Override
            public void focusLostFromControl(Control control, String filedValue) {
                if (!mAnnotIsSelected) return;
                if (control == null || control.isEmpty())
                    return;

                try {
                    Widget widget = control.getWidget();
                    if (widget == null || widget.isEmpty())
                        return;

                    Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
                    if (AppAnnotUtil.equals(widget, curAnnot)) {
                        if (shouldShowInputSoft(widget)) {
                            if (mBlink != null) {
                                mBlink.removeCallbacks((Runnable) mBlink);
                                mBlink = null;
                            }
                            AppUtil.dismissInputSoft(mEditView);
                            mParent.removeView(mEditView);
                        }
                        if (shouldShowNavigation(widget)) {
                            if (mFNModule != null) {
                                mFNModule.hide();
                                mFNModule.setPadding(0, 0, 0, 0);
                            }
                            resetDocViewerOffset();
                        }
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        });

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

    private void preparePropertyBar(Annot annot) {
        DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        mPropertyBar.setEditable(documentManager.canAddAnnot() && documentManager.canModifyForm() && !(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
        int[] colors = new int[PropertyBar.PB_COLORS_FORM.length];
        System.arraycopy(PropertyBar.PB_COLORS_FORM, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_FORM[0];
        mPropertyBar.setColors(colors);
        mPropertyBar.setArrowVisible(false);
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);

        try {
            int fieldType = FormFillerUtil.getAnnotFieldType(annot);
            if (fieldType == Field.e_TypeCheckBox) {
                DefaultAppearance da = ((Widget) annot).getField().getDefaultAppearance();
                mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, da.getText_color());
                mPropertyBar.reset(getProperties(Field.e_TypeCheckBox));
            } else if (fieldType == Field.e_TypeTextField) {
                float[] fontSizes = new float[PropertyBar.PB_FONTSIZES.length + 1];
                fontSizes[0] = 0.0f;
                System.arraycopy(PropertyBar.PB_FONTSIZES, 0, fontSizes, 1, fontSizes.length - 1);
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, fontSizes);

                DefaultAppearance da = ((Widget) annot).getField().getDefaultAppearance();
                mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, da.getText_color());
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTNAME, mTextUtil.getSupportFontName(mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())));
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, da.getText_size());
                mPropertyBar.reset(getProperties(Field.e_TypeTextField));
            } else if (fieldType == Field.e_TypeRadioButton) {
                DefaultAppearance da = ((Widget) annot).getControl().getDefaultAppearance();
                mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, da.getText_color());
                mPropertyBar.setProperty(PropertyBar.PROPERTY_EDIT_TEXT, mTempFieldName);
                mPropertyBar.setPropertyTitle(PropertyBar.PROPERTY_EDIT_TEXT, AppResource.getString(mContext, R.string.fx_string_name), AppResource.getString(mContext, R.string.fx_string_name));
                mPropertyBar.reset(getProperties(Field.e_TypeRadioButton));
            } else if (fieldType == Field.e_TypeComboBox) {
                float[] fontSizes = new float[PropertyBar.PB_FONTSIZES.length + 1];
                fontSizes[0] = 0.0f;
                System.arraycopy(PropertyBar.PB_FONTSIZES, 0, fontSizes, 1, fontSizes.length - 1);
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, fontSizes);

                DefaultAppearance da = ((Widget) annot).getField().getDefaultAppearance();
                mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, da.getText_color());
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTNAME, mTextUtil.getSupportFontName(mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())));
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, da.getText_size());
                mPropertyBar.reset(getProperties(Field.e_TypeComboBox));

                mPropertyBar.addTab("", 0, mContext.getApplicationContext().getString(R.string.pb_options_tab), 1);
                mPropertyBar.addCustomItem(PropertyBar.PROPERTY_OPTIONS, getListView(Field.e_TypeComboBox, annot), 1, 0);
            } else if (fieldType == Field.e_TypeListBox) {
                float[] fontSizes = new float[PropertyBar.PB_FONTSIZES.length + 1];
                fontSizes[0] = 0.0f;
                System.arraycopy(PropertyBar.PB_FONTSIZES, 0, fontSizes, 1, fontSizes.length - 1);
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, fontSizes);

                DefaultAppearance da = ((Widget) annot).getField().getDefaultAppearance();
                mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, da.getText_color());
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTNAME, mTextUtil.getSupportFontName(mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())));
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, da.getText_size());
                mPropertyBar.reset(getProperties(Field.e_TypeListBox));

                mPropertyBar.addTab("", 0, mContext.getApplicationContext().getString(R.string.pb_options_tab), 1);
                mPropertyBar.addCustomItem(PropertyBar.PROPERTY_OPTIONS, getListView(Field.e_TypeListBox, annot), 1, 0);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private ChoiceItemAdapter mChoiceItemAdapter;
    private ListView mLvChoiceOptions;
    private TextView mTvAddChoiceOptions;
    private ArrayList<ChoiceItemInfo> mCurOptions = new ArrayList<>();

    private View getListView(final int type, final Annot annot) {
        DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        boolean canEdit = documentManager.canAddAnnot() && documentManager.canModifyForm() && !(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot));
        final View view = View.inflate(mContext, R.layout.pb_form_itemlist, null);
        if (!canEdit)
            view.setAlpha(PropertyBar.PB_ALPHA);
        try {
            final Switch customSwitch = view.findViewById(R.id.rd_switch_custom_text);
            customSwitch.setEnabled(canEdit);
            if (type == Field.e_TypeComboBox) {
                customSwitch.setText(AppResource.getString(mContext, R.string.fx_combox_custom_text));
                customSwitch.setChecked((((Widget) annot).getField().getFlags() & Field.e_FlagComboEdit) != 0);
            } else if (type == Field.e_TypeListBox) {
                customSwitch.setText(AppResource.getString(mContext, R.string.fx_listbox_multiple_text));
                customSwitch.setChecked((((Widget) annot).getField().getFlags() & Field.e_FlagChoiceMultiSelect) != 0);
            }
            customSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int flags = 0;
                    try {
                        flags = ((Widget) annot).getField().getFlags();
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }

                    if (type == Field.e_TypeComboBox) {
                        if (isChecked)
                            flags = flags | Field.e_FlagComboEdit;
                        else
                            flags = flags & (~Field.e_FlagComboEdit);
                    } else if (type == Field.e_TypeListBox) {
                        if (isChecked) {
                            flags = flags | Field.e_FlagChoiceMultiSelect;
                        } else {
                            flags = flags & (~Field.e_FlagChoiceMultiSelect);

                            boolean hasSelected = false;
                            for (ChoiceItemInfo itemInfo : mCurOptions) {
                                if (!hasSelected && itemInfo.selected) {
                                    hasSelected = true;
                                    continue;
                                }
                                itemInfo.selected = false;
                            }
                            mChoiceItemAdapter.notifyDataSetChanged();
                        }
                    }
                    FormFillerContent fillerContent = new FormFillerContent(mPdfViewCtrl, (Widget) mLastAnnot);
                    fillerContent.mFieldFlags = flags;
                    fillerContent.mOptions = mCurOptions;
                    modifyAnnot(fillerContent, false, false, null);
                }
            });

            mLvChoiceOptions = view.findViewById(R.id.rd_form_item_list);
            mLvChoiceOptions.setEnabled(canEdit);
            mTvAddChoiceOptions = view.findViewById(R.id.pb_tv_no_item_tips);

            mCurOptions.clear();
            mCurOptions = FormFillerUtil.cloneChoiceOptions(mTempOptions);
            long size = mCurOptions.size();
            if (size > 0) {
                mLvChoiceOptions.setVisibility(View.VISIBLE);
                mTvAddChoiceOptions.setVisibility(View.GONE);
            } else {
                mLvChoiceOptions.setVisibility(View.GONE);
                mTvAddChoiceOptions.setVisibility(View.VISIBLE);
            }

            mChoiceItemAdapter = new ChoiceItemAdapter(mContext, mCurOptions);
            mLvChoiceOptions.setAdapter(mChoiceItemAdapter);
            mLvChoiceOptions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ChoiceOptionsAdapter.SelectMode selectMode = ChoiceOptionsAdapter.SelectMode.SINGLE_SELECT;
                    if (type == Field.e_TypeListBox && customSwitch.isChecked()) {
                        selectMode = ChoiceOptionsAdapter.SelectMode.MULTI_SELECT;
                    }

                    gotoChoiceOptionsPage(selectMode, mCurOptions, mPickOptionsCallback);
                }
            });

            mTvAddChoiceOptions.setEnabled(canEdit);
            mTvAddChoiceOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ChoiceOptionsAdapter.SelectMode selectMode = ChoiceOptionsAdapter.SelectMode.SINGLE_SELECT;
                    if (type == Field.e_TypeListBox && customSwitch.isChecked()) {
                        selectMode = ChoiceOptionsAdapter.SelectMode.MULTI_SELECT;
                    }

                    gotoChoiceOptionsPage(selectMode, mCurOptions, mPickOptionsCallback);
                }
            });
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return view;
    }

    private IResult<ArrayList<ChoiceItemInfo>, Object, Object> mPickOptionsCallback = new IResult<ArrayList<ChoiceItemInfo>, Object, Object>() {
        @Override
        public void onResult(boolean success, ArrayList<ChoiceItemInfo> itemInfos, Object o1, Object o2) {
            if (success) {
                ArrayList<ChoiceItemInfo> lastInfos = mChoiceItemAdapter.getChoiceInfos();
                if (FormFillerUtil.optionsIsChanged(lastInfos, itemInfos)) {
                    if (itemInfos.size() > 0) {
                        mLvChoiceOptions.setVisibility(View.VISIBLE);
                        mTvAddChoiceOptions.setVisibility(View.GONE);
                    } else {
                        mLvChoiceOptions.setVisibility(View.GONE);
                        mTvAddChoiceOptions.setVisibility(View.VISIBLE);
                    }

                    mCurOptions.clear();
                    mCurOptions = FormFillerUtil.cloneChoiceOptions(itemInfos);
                    mChoiceItemAdapter.setChoiceInfos(mCurOptions);
                    mChoiceItemAdapter.notifyDataSetChanged();

                    FormFillerContent fillerContent = new FormFillerContent(mPdfViewCtrl, (Widget) mLastAnnot);
                    fillerContent.mOptions = mCurOptions;
                    modifyAnnot(fillerContent, false, false, null);
                }
            }
        }
    };

    private long getProperties(int type) {
        long properties;
        if (type == Field.e_TypeCheckBox) {
            properties = PropertyBar.PROPERTY_COLOR;
        } else if (type == Field.e_TypeTextField) {
            properties = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_FONTSIZE | PropertyBar.PROPERTY_FONTNAME;
        } else if (type == Field.e_TypeRadioButton) {
            properties = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_EDIT_TEXT;
        } else if (type == Field.e_TypeComboBox) {
            properties = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_FONTSIZE | PropertyBar.PROPERTY_FONTNAME;
        } else if (type == Field.e_TypeListBox) {
            properties = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_FONTSIZE | PropertyBar.PROPERTY_FONTNAME;
        } else {
            properties = PropertyBar.PROPERTY_COLOR;
        }
        return properties;
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
                            mAnnotIsSelected = false;
                            mFormFiller.killFocus();
                            Field field = formControl.getField();
                            field.reset();
                            mAnnotIsSelected = true;
                            mFormFiller.setFocus(formControl);
                            mIsModify = true;
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
                setEdittextLocation(mLastAnnot);
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

    private void modifyAnnot(IFormFillerContent formContent, final boolean isModifyJni, final boolean addUndo, final Event.Callback result) {
        try {
            final int pageIndex = formContent.getPageIndex();
            final Annot annot = formContent.getAnnot();

            if (isModifyJni) {
                final FormFillerModifyUndoItem undoItem = (FormFillerModifyUndoItem) createModifyUndoItem(formContent);
                final PDFPage page = annot.getPage();

                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(addUndo);
                FormFillerEvent event = new FormFillerEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Widget) annot, mPdfViewCtrl);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(page, annot);

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
                boolean isModify = false;

                RectF oldRect = AppUtil.toRectF(annot.getRect());
                Field field = ((Widget) annot).getField();
                int fieldType = field.getType();
                DefaultAppearance da;
                if (field.getType() == Field.e_TypeRadioButton)
                    da = ((Widget) annot).getControl().getDefaultAppearance();
                else
                    da = field.getDefaultAppearance();

                int flags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;
                da.setFlags(flags);
                if (da.getText_color() != formContent.getFontColor()) {
                    isModify = true;
                    da.setText_color(formContent.getFontColor());
                }

                if (formContent.getFontId() != mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())) {
                    isModify = true;
                    Font font = mTextUtil.getStandardFont(formContent.getFontId());
                    da.setFont(font);
                }
                if (formContent.getFontSize() != da.getText_size()) {
                    isModify = true;
                    da.setText_size(formContent.getFontSize());
                }

                if (fieldType == Field.e_TypeRadioButton) {
                    if (isModify)
                        ((Widget) annot).getControl().setDefaultAppearance(da);

                    String name = field.getName();
                    if (!name.equals(formContent.getFieldName()) && mForm != null) {
                        isModify = true;
                        FormFillerUtil.renameField(mForm, ((Widget) annot).getControl(), formContent.getFieldName());
                        field = ((Widget) annot).getField();
                    }

                    int checkedIndex = FormFillerUtil.getCheckedIndex(field);
                    if ((checkedIndex != formContent.getCheckedIndex())) {
                        isModify = true;
                        field.getControl(formContent.getCheckedIndex()).setChecked(true);
                    }
                } else if (fieldType == Field.e_TypeListBox || fieldType == Field.e_TypeComboBox) {
                    if (isModify)
                        field.setDefaultAppearance(da);

                    if (formContent.getFieldFlags() != field.getFlags()) {
                        isModify = true;
                        field.setFlags(formContent.getFieldFlags());
                    }
                    ArrayList<ChoiceItemInfo> choiceOptions = FormFillerUtil.getOptions(field);
                    if (FormFillerUtil.optionsIsChanged(formContent.getOptions(), choiceOptions)) {
                        isModify = true;
                        field.setOptions(FormFillerUtil.options2Native(formContent.getOptions()));
                    }
                } else {
                    if (isModify) {
                        field.setDefaultAppearance(da);
                    }
                }

                if (isModify) {
                    annot.resetAppearanceStream();
                }

                if (!AppUtil.toRectF(annot.getRect()).equals(formContent.getBBox())) {
                    isModify = true;
                    annot.move(AppUtil.toFxRectF(formContent.getBBox()));
                }
                if (isModify)
                    annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());

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
        final DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        if (annot == documentManager.getCurrentAnnot()) {
            documentManager.setCurrentAnnot(null, false);
        }

        try {
            final PDFPage page = annot.getPage();
            Field field = ((Widget) annot).getField();
            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            final int pageIndex = page.getIndex();
            int fieldType = FormFillerUtil.getAnnotFieldType(annot);

            final FormFillerDeleteUndoItem undoItem = new FormFillerDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.mFieldType = fieldType;
            if (fieldType == Field.e_TypeCheckBox && field.getControlCount() > 0)
                undoItem.mFieldName = AppDmUtil.randomUUID(null);
            else
                undoItem.mFieldName = field.getName();
            undoItem.mFieldFlags = field.getFlags();
            undoItem.mOptions = FormFillerUtil.getOptions(field);
            undoItem.mValue = field.getValue();
            undoItem.mRotation = ((Widget) annot).getMKRotation();

            DefaultAppearance da;
            if (fieldType == Field.e_TypeRadioButton)
                da = ((Widget) annot).getControl().getDefaultAppearance();
            else
                da = field.getDefaultAppearance();
            undoItem.mFontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
            undoItem.mFontColor = da.getText_color();
            undoItem.mFontSize = da.getText_size();
            undoItem.mIsChecked = ((Widget) annot).getControl().isChecked();

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
        if (FormFillerUtil.getAnnotFieldType(annot) == Field.e_TypePushButton) return false;
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
                            final int preAnnotType = FormFillerUtil.getAnnotFieldType(preAnnot);
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
                            final int nextAnnotType = FormFillerUtil.getAnnotFieldType(nextAnnot);
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

    void stopSave(){
        if (mAssist != null) {
            mAssist.bWillSave = false;
        }
    }

    void startSave(){
        if (mAssist != null) {
            mAssist.bWillSave = true;
        }
    }

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

    private String mTempValue;
    private String mTempFieldName;
    private int mTempFieldFlags;
    private ArrayList<ChoiceItemInfo> mTempOptions = new ArrayList<>();

    private RectF mTempLastBBox = new RectF();
    private int mTempLastFontColor;
    private int mTempLastFontID;
    private float mTempLastFontSize;
    private int mTempLastCheckedIndex;

    private RectF mTempLastDisplayBBox = new RectF();
    private RectF mPageViewRect = new RectF(0, 0, 0, 0);

    private boolean mOldIsHideSystem;

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        mAnnotIsSelected = true;
        try {
            RectF rectF = AppUtil.toRectF(annot.getRect());
            mLastPageIndex = annot.getPage().getIndex();
            mTempLastBBox = new RectF(rectF);

            RectF pageViewRect = new RectF(mTempLastBBox);
            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, mLastPageIndex);
            mTempLastDisplayBBox = new RectF(pageViewRect);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mTempLastDisplayBBox, mTempLastDisplayBBox, mLastPageIndex);

            Field field = ((Widget) annot).getField();
            int fieldType = FormFillerUtil.getAnnotFieldType(annot);

            DefaultAppearance da;
            if (fieldType == Field.e_TypeComboBox || fieldType == Field.e_TypeListBox) {
                mTempFieldFlags = field.getFlags();
                mTempOptions = FormFillerUtil.getOptions(field);
                da = field.getDefaultAppearance();
            } else if (fieldType == Field.e_TypeRadioButton) {
                mTempLastCheckedIndex = FormFillerUtil.getCheckedIndex(field);
                da = ((Widget) annot).getControl().getDefaultAppearance();
            } else {
                da = field.getDefaultAppearance();
            }

            mTempLastFontColor = da.getText_color();
            mTempLastFontID = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
            mTempLastFontSize = da.getText_size();
            mTempFieldName = field.getName();
            mTempValue = field.getValue();

            mPageViewRect.set(rectF);
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, mLastPageIndex);
            if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mLastAnnot = annot;
                }
            } else {
                mLastAnnot = annot;
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

            Rect rect = new Rect();
            mParent.getGlobalVisibleRect(rect);
            if (AppDisplay.getInstance(mContext).isPad() && rect.top != 0) {
                mParent.addView(mEditView, 0);
                mIsEdittextOffset = true;
            } else {
                mEditView.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
                mEditView.setSingleLine(false);
                mParent.addView(mEditView);
                mIsEdittextOffset = false;
            }
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

        if (mFNModule != null) {
            if (!FormFillerUtil.isReadOnly(annot))
                mFNModule.setClearEnable(true);
            else
                mFNModule.setClearEnable(false);

            int fieldType = FormFillerUtil.getAnnotFieldType(annot);
            if (fieldType != Field.e_TypePushButton)
                mFNModule.show();
        }
    }

    private void onAnnotSeletedByLongPress(final Annot annot, boolean needInvalid) {
        mCtlPtRadius = AppDisplay.getInstance(mContext).dp2px(mCtlPtRadius);
        mCtlPtDeltyXY = AppDisplay.getInstance(mContext).dp2px(mCtlPtDeltyXY);
        try {
            int pageIndex = annot.getPage().getIndex();
            prepareAnnotMenu(annot);
            RectF menuRect = new RectF(mPageViewRect);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
            mAnnotationMenu.show(menuRect);
            preparePropertyBar(annot);

            if (mPdfViewCtrl.isPageVisible(pageIndex))
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(mPageViewRect));
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void prepareAnnotMenu(final Annot annot) {
        mMenuText.clear();
        String uid = AppAnnotUtil.getAnnotUniqueID(annot);
        if (!TextUtils.isEmpty(uid) && uid.contains(FormFillerModule.ID_TAG)) {
            DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
            int fieldType = FormFillerUtil.getAnnotFieldType(annot);
            if (fieldType == Field.e_TypeCheckBox
                    || fieldType == Field.e_TypeTextField
                    || fieldType == Field.e_TypeRadioButton
                    || fieldType == Field.e_TypeListBox
                    || fieldType == Field.e_TypeComboBox) {
                mMenuText.add(AnnotMenu.AM_BT_STYLE);
            }

            if (documentManager.canAddAnnot()
                    && documentManager.canModifyForm()
                    && !(AppAnnotUtil.isLocked(annot) || FormFillerUtil.isReadOnly(annot))) {
                mMenuText.add(AnnotMenu.AM_BT_DELETE);
            }

            mAnnotationMenu.setMenuItems(mMenuText);
            mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    try {
                        if (btType == AnnotMenu.AM_BT_DELETE) {
                            if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                                deleteAnnot(annot, true, null);
                            }
                        } else if (btType == AnnotMenu.AM_BT_STYLE) {

                            int pageIndex = annot.getPage().getIndex();
                            RectF annotRectF = AppUtil.toRectF(annot.getRect());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, pageIndex);

                            RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annotRectF);
                            mPropertyBar.show(rectF, false);
                        }
                        mAnnotationMenu.dismiss();
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void onAnnotDeselected(final Annot annot, boolean needInvalid) {
        mAnnotIsSelected = false;
        mIsLongPressTouchEvent = false;
        mCtlPtRadius = 5;
        mCtlPtDeltyXY = 20;
        try {
            mFormFiller.killFocus();
            refreshField(((Widget) annot).getField());

            MainFrame mainFrame = (MainFrame) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getMainFrame();
            mainFrame.setHideSystemUI(mOldIsHideSystem);
            postDismissNavigation();
            if (mBlink != null) {
                mBlink.removeCallbacks((Runnable) mBlink);
                mBlink = null;
            }

            Field field = ((Widget) annot).getField();
            DefaultAppearance da;
            if (field.getType() == Field.e_TypeRadioButton)
                da = ((Widget) annot).getControl().getDefaultAppearance();
            else
                da = field.getDefaultAppearance();

            if (needInvalid && mIsModify) {
                int pageIndex = annot.getPage().getIndex();
                RectF rectF = AppUtil.toRectF(annot.getRect());
                String value = field.getValue();
                String lastFieldName = mLastFieldName.trim();

                if (lastFieldName.startsWith(".")
                        || lastFieldName.endsWith(".")
                        || lastFieldName.contains("..")) {
                    showInputIncorrectTips();

                    while (lastFieldName.contains("..")) {
                        lastFieldName = lastFieldName.replaceAll("\\.\\.", ".");
                    }
                    if (lastFieldName.startsWith("."))
                        lastFieldName = lastFieldName.replaceFirst("\\.", "");
                    if (lastFieldName.endsWith("."))
                        lastFieldName = lastFieldName.substring(0, lastFieldName.lastIndexOf("."));
                }
                String name = TextUtils.isEmpty(lastFieldName) ? field.getName() : lastFieldName;
                int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                int fontColor = da.getText_color();
                float fontSize = da.getText_size();
                int fieldFlags = field.getFlags();
                int checkedIndex = ((Widget) annot).getControl().getIndex();
                ArrayList<ChoiceItemInfo> options = FormFillerUtil.getOptions(field);

                boolean isModify = !mTempValue.equals(value)
                        || mTempLastFontColor != fontColor
                        || mTempLastFontID != fontId
                        || mTempLastFontSize != fontSize
                        || !mTempLastBBox.equals(rectF);

                int fieldType = field.getType();
                if (fieldType == Field.e_TypeRadioButton) {
                    checkedIndex = FormFillerUtil.getCheckedIndex(field);
                    isModify = isModify || !mTempFieldName.equals(name) || mTempLastCheckedIndex != checkedIndex;
                } else if (fieldType == Field.e_TypeComboBox || fieldType == Field.e_TypeListBox) {
                    isModify = isModify || mTempFieldFlags != fieldFlags || FormFillerUtil.optionsIsChanged(mTempOptions, options);
                }

                FormFillerContent fillerContent = new FormFillerContent(mPdfViewCtrl, (Widget) annot);
                fillerContent.mPageIndex = pageIndex;
                fillerContent.mRectF = rectF;
                fillerContent.mFieldValue = value;
                fillerContent.mFontId = fontId;
                fillerContent.mFontColor = fontColor;
                fillerContent.mFontSize = fontSize;
                fillerContent.mFieldName = name;
                fillerContent.mOptions = options;
                fillerContent.mFieldFlags = fieldFlags;
                fillerContent.mCheckedIndex = checkedIndex;
                modifyAnnot(fillerContent, isModify, isModify, null);
            } else if (mIsModify) {
                int fieldType = field.getType();
                int flags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;
                da.setFlags(flags);
                if (da.getText_color() != mTempLastFontColor)
                    da.setText_color(mTempLastFontColor);
                if (mTempLastFontID != mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())) {
                    Font font = mTextUtil.getStandardFont(mTempLastFontID);
                    da.setFont(font);
                }
                if (mTempLastFontSize != da.getText_size())
                    da.setText_size(mTempLastFontSize);

                if (field.getType() == Field.e_TypeRadioButton)
                    ((Widget) annot).getControl().setDefaultAppearance(da);
                else
                    field.setDefaultAppearance(da);

                if (fieldType == Field.e_TypeRadioButton) {
                    String lastFieldName = mLastFieldName.trim();

                    while (lastFieldName.contains("..")) {
                        lastFieldName = lastFieldName.replaceAll("\\.\\.", ".");
                    }
                    if (lastFieldName.startsWith(".")) {
                        lastFieldName = lastFieldName.replaceFirst("\\.", "");
                    } else if (lastFieldName.endsWith(".")) {
                        lastFieldName = lastFieldName.substring(0, lastFieldName.lastIndexOf("."));
                    }
                    String name = TextUtils.isEmpty(lastFieldName) ? field.getName() : lastFieldName;
                    if (!name.equals(mTempFieldName) && mForm != null) {
                        FormFillerUtil.renameField(mForm, ((Widget) annot).getControl(), mTempFieldName);
                        field = ((Widget) annot).getField();
                    }
                } else if (fieldType == Field.e_TypeListBox || fieldType == Field.e_TypeComboBox) {
                    if (mTempFieldFlags != field.getFlags())
                        field.setFlags(mTempFieldFlags);

                    ArrayList<ChoiceItemInfo> choiceOptions = FormFillerUtil.getOptions(field);
                    if (FormFillerUtil.optionsIsChanged(mTempOptions, choiceOptions))
                        field.setOptions(FormFillerUtil.options2Native(mTempOptions));
                }
                if (!AppUtil.toRectF(annot.getRect()).equals(mTempLastBBox))
                    annot.move(AppUtil.toFxRectF(mTempLastBBox));
                if (!field.getValue().equals(mTempValue))
                    field.setValue(mTempValue);
                annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                annot.resetAppearanceStream();
            }

            if (mIsShowEditText) {
                AppUtil.dismissInputSoft(mEditView);
                mParent.removeView(mEditView);
                mIsShowEditText = false;
            }
            if (mAnnotationMenu.isShowing())
                mAnnotationMenu.dismiss();
            if (mPropertyBar.isShowing())
                mPropertyBar.dismiss();
            mLastAnnot = null;
            mIsModify = false;
            mIsEdittextOffset = false;
            mLastPageIndex = -1;
            mLastFieldName = "";
            mTempOptions.clear();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void showInputIncorrectTips() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        final UITextEditDialog dialog = new UITextEditDialog(uiExtensionsManager.getAttachedActivity());
        dialog.getInputEditText().setVisibility(View.GONE);
        dialog.getCancelButton().setVisibility(View.GONE);
        dialog.setTitle(AppResource.getString(mContext, R.string.fx_string_error));
        dialog.getPromptTextView().setText(AppResource.getString(mContext, R.string.fx_form_rename_toast));
        dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!hasInitialized() || mFormFiller == null) return false;
        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canFillForm())
            return false;
        if (AppAnnotUtil.isLocked(annot) || FormFillerUtil.isReadOnly(annot))
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
                        int type = FormFillerUtil.getAnnotFieldType(annot);
                        if (type == Field.e_TypeRadioButton) {
                            mTempLastCheckedIndex = FormFillerUtil.getCheckedIndex(((Widget) annot).getField());
                        }

                        isDown = false;
                        mFormFiller.onLButtonUp(page, AppUtil.toFxPointF(pdfPointF), 0);

                        if (type == Field.e_TypeRadioButton) {
                            if (mTempLastCheckedIndex != FormFillerUtil.getCheckedIndex(((Widget) annot).getField())) {
                                FormFillerContent formContent = new FormFillerContent(mPdfViewCtrl, (Widget) annot);
                                FormFillerModifyUndoItem undoItem = (FormFillerModifyUndoItem) createModifyUndoItem(formContent);
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                            }
                        } else {
                            mIsModify = true;
                        }

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
        DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        if (!documentManager.canModifyForm() || documentManager.isSign())
            return false;
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

                            FormFillerContent fillerContent = new FormFillerContent(mPdfViewCtrl, (Widget) annot);
                            fillerContent.mPageIndex = pageIndex;
                            fillerContent.mRectF = bboxRect;
                            modifyAnnot(fillerContent, false, false, null);
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

    private void setEdittextLocation(Annot annot) {
        Rect rect = new Rect();
        mParent.getGlobalVisibleRect(rect);

        if (AppDisplay.getInstance(mContext).isPad() && rect.top != 0) {
            if (annot != null && !annot.isEmpty()) {
                int keyboardHeight = AppKeyboardUtil.getKeyboardHeight(mParent);
                int rawScreenHeight = AppDisplay.getInstance(mContext).getRawScreenHeight();
                int navBarHeight = AppDisplay.getInstance(mContext).getRealNavBarHeight();

                if (rawScreenHeight - mTempLastDisplayBBox.bottom - rect.top < (keyboardHeight + AppDisplay.getInstance(mContext).dp2px(116)) + navBarHeight) {
                    mIsEdittextOffset = true;
                    mEditView.setLayoutParams(new RelativeLayout.LayoutParams(100, 100));
                    mEditView.setX(mTempLastDisplayBBox.left);
                    mEditView.setY(mTempLastDisplayBBox.bottom + 50);
                    mEditView.setSingleLine(true);
                    mEditView.setSelection(mEditView.getText().length());
                    mEditView.setBackground(null);
                    mEditView.setCursorVisible(false);
                } else {
                    mIsEdittextOffset = false;
                    mEditView.setSingleLine(false);
                    mEditView.setLayoutParams(new RelativeLayout.LayoutParams(1, 1));
                }


                if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
                    RectF rectF = new RectF(mPageViewRect);
                    rectF.inset(-100, -100);
                    mPdfViewCtrl.refresh(mLastPageIndex, AppDmUtil.rectFToRect(rectF));
                }
            }
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

        if (mIsEdittextOffset) {
            if ((screenHeight - bottom - AppDisplay.getInstance(mContext).getNavBarHeight()) >= keyboardHeight) {
                padding = 0;
            } else if (screenHeight - top - (int) mTempLastDisplayBBox.bottom - 150 >= keyboardHeight && screenHeight - bottom - AppDisplay.getInstance(mContext).getNavBarHeight() < keyboardHeight) {
                int[] location = new int[2];
                mParent.getLocationOnScreen(location);
                int y = location[1];

                padding = mParent.getHeight() - (screenHeight - keyboardHeight - AppDisplay.getInstance(mContext).getNavBarHeight() - y);
            } else if (mParent.getHeight() - (int) mTempLastDisplayBBox.bottom > 150) {
                padding = mParent.getHeight() - (int) mTempLastDisplayBBox.bottom - 150;
            } else {
                padding = 0;
            }
        } else {
            if (Build.VERSION.SDK_INT < 14 && keyboardHeight < screenHeight / 5) {
                padding = 0;
            } else if ((screenHeight - bottom - AppDisplay.getInstance(mContext).getNavBarHeight()) >= keyboardHeight) {
                padding = 0;
            } else if (screenHeight - top > keyboardHeight && screenHeight - bottom - AppDisplay.getInstance(mContext).getNavBarHeight() < keyboardHeight) {
                padding = keyboardHeight - (screenHeight - bottom - AppDisplay.getInstance(mContext).getNavBarHeight());
            } else {
                padding = 0;
            }
        }
        return padding;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!hasInitialized() || mFormFiller == null) return false;
        DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        if (documentManager.isSign() || documentManager.isXFA())
            return false;
        if (FormFillerUtil.isReadOnly(annot)) return false;

        mIsLongPressTouchEvent = true;
        try {
            final PointF pdfPointF = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);

            if (annot == documentManager.getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pdfPointF)) {
                    return true;
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
                    documentManager.setCurrentAnnot(null);
                    return true;
                }
            } else {
                documentManager.setCurrentAnnot(annot);
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
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
            PointF pdfPointF = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
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

                Field field = ((Widget) annot).getField();
                int type = field.getType();
                if (type == Field.e_TypeRadioButton) {
                    mTempLastCheckedIndex = FormFillerUtil.getCheckedIndex(field);
                }

                mFormFiller.onLButtonDown(page, AppUtil.toFxPointF(touchPointF), 0);
                mFormFiller.onLButtonUp(page, AppUtil.toFxPointF(touchPointF), 0);

                if (type == Field.e_TypeRadioButton) {
                    if (mTempLastCheckedIndex != FormFillerUtil.getCheckedIndex(field)) {
                        FormFillerContent formContent = new FormFillerContent(mPdfViewCtrl, (Widget) annot);
                        FormFillerModifyUndoItem undoItem = (FormFillerModifyUndoItem) createModifyUndoItem(formContent);
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                    }
                } else {
                    mIsModify = true;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private FormFillerUndoItem createModifyUndoItem(IFormFillerContent formContent) {
        final FormFillerModifyUndoItem undoItem = new FormFillerModifyUndoItem(mPdfViewCtrl);
        undoItem.setCurrentValue(formContent.getAnnot());
        undoItem.mNeedResetChecked = false;

        undoItem.mPageIndex = formContent.getPageIndex();
        undoItem.mBBox = new RectF(formContent.getBBox());
        undoItem.mValue = formContent.getFieldValue();
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mFontId = formContent.getFontId();
        undoItem.mFontColor = formContent.getFontColor();
        undoItem.mFontSize = formContent.getFontSize();
        undoItem.mFieldName = formContent.getFieldName();
        undoItem.mFieldFlags = formContent.getFieldFlags();
        undoItem.mOptions = FormFillerUtil.cloneChoiceOptions(formContent.getOptions());
        undoItem.mCheckedIndex = formContent.getCheckedIndex();

        undoItem.mRedoBbox = new RectF(formContent.getBBox());
        undoItem.mRedoValue = formContent.getFieldValue();
        undoItem.mRedoFontId = formContent.getFontId();
        undoItem.mRedoFontColor = formContent.getFontColor();
        undoItem.mRedoFontSize = formContent.getFontSize();
        undoItem.mRedoFieldName = formContent.getFieldName();
        undoItem.mRedoFieldFlags = formContent.getFieldFlags();
        undoItem.mRedoOptions = FormFillerUtil.cloneChoiceOptions(formContent.getOptions());
        undoItem.mRedoCheckedIndex = formContent.getCheckedIndex();

        undoItem.mUndoValue = mTempValue;
        undoItem.mUndoBbox = new RectF(mTempLastBBox);
        undoItem.mUndoFontId = mTempLastFontID;
        undoItem.mUndoFontColor = mTempLastFontColor;
        undoItem.mUndoFontSize = mTempLastFontSize;
        undoItem.mUndoFieldName = mTempFieldName;
        undoItem.mUndoFieldFlags = mTempFieldFlags;
        undoItem.mUndoOptions = FormFillerUtil.cloneChoiceOptions(mTempOptions);
        undoItem.mUndoCheckedIndex = mTempLastCheckedIndex;

        return undoItem;
    }

    private PointF adjustTouchPointF(PointF pdfPointF, RectF annotRectF) {
        if (pdfPointF.x < annotRectF.left) {
            pdfPointF.x = annotRectF.left;
        }
        if (pdfPointF.x > annotRectF.right) {
            pdfPointF.x = annotRectF.right;
        }
        if (pdfPointF.y < annotRectF.bottom) {
            pdfPointF.y = annotRectF.bottom;
        }
        if (pdfPointF.y > annotRectF.top) {
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

            if (!mIsEdittextOffset) {
                RectF rect = AppUtil.toRectF(annot.getRect());

                PointF viewpoint = new PointF(rect.left, rect.bottom);
                PointF point = new PointF(rect.left, rect.bottom);
                mPdfViewCtrl.convertPdfPtToPageViewPt(viewpoint, viewpoint, pageIndex);
                mPdfViewCtrl.convertPdfPtToPageViewPt(point, point, pageIndex);
                mPdfViewCtrl.convertPageViewPtToDisplayViewPt(viewpoint, viewpoint, pageIndex);

                if (shouldShowInputSoft(annot)) {
                    int keyboardHeight = AppKeyboardUtil.getKeyboardHeight(mParent);
                    if (mAdjustPosition && keyboardHeight > AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
                        int rawScreenHeight = AppDisplay.getInstance(mContext).getRawScreenHeight();
                        int navBarHeight = AppDisplay.getInstance(mContext).getRealNavBarHeight();
                        Rect parentRect = new Rect();
                        mParent.getGlobalVisibleRect(parentRect);

                        if (rawScreenHeight - viewpoint.y - parentRect.top < (keyboardHeight + AppDisplay.getInstance(mContext).dp2px(116)) + navBarHeight) {
                            mPageOffset = (int) (keyboardHeight - (rawScreenHeight - viewpoint.y - parentRect.top));

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
                                mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING ||
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
            }

            int fieldType = FormFillerUtil.getAnnotFieldType(annot);
            if (mFNModule != null) {
                if (fieldType != Field.e_TypePushButton) {
                    if (shouldShowInputSoft(annot)) {
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
            if (fieldType != Field.e_TypePushButton) {
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
                mViewDrawRectInOnDraw.set(AppUtil.toRectF(annot.getRect()));
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
                    mViewDrawRect.set(AppUtil.toRectF(curAnnot.getRect()));
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
                    if (mPropertyBar.isShowing()) {
                        RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mDocViewerBBox);
                        mPropertyBar.update(rectF);
                    }
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
        int type = FormFillerUtil.getAnnotFieldType(annot);
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

    protected void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
        if (newWidth != oldWidth || newHeight != oldHeight) {
            mAdjustPosition = true;

            if (mLastAnnot != null && !mLastAnnot.isEmpty()) {
                if (mIsEdittextOffset) {
                    AppUtil.dismissInputSoft(mEditView);
                    RectF pageViewRect = new RectF(mTempLastBBox);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, mLastPageIndex);
                    mTempLastDisplayBBox = new RectF(pageViewRect);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mTempLastDisplayBBox, mTempLastDisplayBBox, mLastPageIndex);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AppUtil.showSoftInput(mEditView);

                        }
                    }, 200);
                }
            }
        }
    }

    private PropertyBar.PropertyChangeListener mPropertyChangeListener = new PropertyBar.PropertyChangeListener() {
        @Override
        public void onValueChanged(long property, int value) {
            if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
                UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
                if (FormFillerAnnotHandler.this == currentAnnotHandler) {
                    try {
                        if (mLastAnnot != null && !mLastAnnot.isEmpty()) {
                            Field field = ((Widget) mLastAnnot).getField();
                            DefaultAppearance da;
                            if (field.getType() == Field.e_TypeRadioButton)
                                da = ((Widget) mLastAnnot).getControl().getDefaultAppearance();
                            else
                                da = field.getDefaultAppearance();

                            if (value != da.getText_color()) {
                                FormFillerContent fillerContent = new FormFillerContent(mPdfViewCtrl, (Widget) mLastAnnot);
                                fillerContent.mFontColor = value;
                                modifyAnnot(fillerContent, false, false, null);
                            }
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void onValueChanged(long property, float value) {
            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
            AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
            if (FormFillerAnnotHandler.this == currentAnnotHandler) {
                if (property == PropertyBar.PROPERTY_FONTSIZE) {
                    try {
                        if (mLastAnnot != null && !mLastAnnot.isEmpty()) {
                            Field field = ((Widget) mLastAnnot).getField();
                            DefaultAppearance da;
                            if (field.getType() == Field.e_TypeRadioButton)
                                da = ((Widget) mLastAnnot).getControl().getDefaultAppearance();
                            else
                                da = field.getDefaultAppearance();
                            if (value != da.getText_size()) {
                                FormFillerContent fillerContent = new FormFillerContent(mPdfViewCtrl, (Widget) mLastAnnot);
                                fillerContent.mFontSize = value;
                                modifyAnnot(fillerContent, false, false, null);
                            }
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void onValueChanged(long property, String value) {
            try {
                UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                if (FormFillerAnnotHandler.this == uiExtensionsManager.getCurrentAnnotHandler()) {
                    if (property == PropertyBar.PROPERTY_FONTNAME) {
                        if (mLastAnnot != null && !mLastAnnot.isEmpty()) {
                            Field field = ((Widget) mLastAnnot).getField();
                            DefaultAppearance da;
                            if (field.getType() == Field.e_TypeRadioButton)
                                da = ((Widget) mLastAnnot).getControl().getDefaultAppearance();
                            else
                                da = field.getDefaultAppearance();

                            int fontId = mTextUtil.getSupportFontID(value);
                            if (fontId != mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())) {
                                FormFillerContent fillerContent = new FormFillerContent(mPdfViewCtrl, (Widget) mLastAnnot);
                                fillerContent.mFontId = fontId;
                                modifyAnnot(fillerContent, false, false, null);
                            }
                        }
                    } else if (property == PropertyBar.PROPERTY_EDIT_TEXT) {
                        if (mLastAnnot != null && !mLastAnnot.isEmpty()) {
                            String fieldName = ((Widget) mLastAnnot).getField().getName();
                            if (!value.equals(fieldName)) {
                                mIsModify = true;
                                mLastFieldName = value;
                            }
                        }
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

    };

    private void gotoChoiceOptionsPage(ChoiceOptionsAdapter.SelectMode selectMode, ArrayList<ChoiceItemInfo> itemInfos, IResult<ArrayList<ChoiceItemInfo>, Object, Object> pickOptionsCallback) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        if (uiExtensionsManager == null || uiExtensionsManager.getAttachedActivity() == null)
            return;

        if (!(uiExtensionsManager.getAttachedActivity() instanceof FragmentActivity)) {
            UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.the_attached_activity_is_not_fragmentActivity));
            return;
        }

        FragmentActivity act = (FragmentActivity) uiExtensionsManager.getAttachedActivity();
        ChoiceOptionsFragment choiceOptions = (ChoiceOptionsFragment) act.getSupportFragmentManager().findFragmentByTag("ChoiceOptions");
        if (choiceOptions == null)
            choiceOptions = ChoiceOptionsFragment.newInstance(selectMode, itemInfos);
        choiceOptions.init(mPdfViewCtrl, pickOptionsCallback);
        choiceOptions.setDismissListener(new BaseDialogFragment.DismissListener() {
            @Override
            public void onDismiss() {
                if (mPropertyBar.isShowing())
                    mPropertyBar.requestLayout();
            }
        });

        AppDialogManager.getInstance().showAllowManager(choiceOptions, act.getSupportFragmentManager(), "ChoiceOptions", null);
    }

    protected interface FillerFocusEventListener {
        void focusGotOnControl(Control control, String filedValue);

        void focusLostFromControl(Control control, String filedValue);
    }

}
