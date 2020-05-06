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
package com.foxit.uiextensions.controls.propertybar.imp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.line.DistanceMeasurement;
import com.foxit.uiextensions.annots.note.NoteConstants;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyBarImpl extends PopupWindow implements PropertyBar {
    private static final int TYPE_SHOW_FONTSIZE = 2;
    private static final int TYPE_SHOW_FONT = 1;

    // highlight,1,typewriter,2,squareness,3,text,4,
    // strikeout,5,squiggly,6,underline,7,textinsert,8,strikeoutinsert,9,
    // line,10,circle,11,arrow,12,pencil,13,eraser,,
    // callout,14,sign,15,stamp,,
    private Context mContext;

    private LinearLayout mPopupView;
    private LinearLayout mLl_root;
    private LinearLayout mTopShadow;
    private LinearLayout mLlArrowTop;
    private ImageView mIvArrowTop;
    private LinearLayout mLlArrowLeft;
    private ImageView mIvArrowLeft;
    private LinearLayout mLlArrowRight;
    private ImageView mIvArrowRight;
    private LinearLayout mLlArrowBottom;
    private ImageView mIvArrowBottom;
    private boolean mArrowVisible = false;

    private LinearLayout mLl_PropertyBar;
    private List<String> mTabs;
    private LinearLayout mLl_topTabs;
    private LinearLayout mTopTitleLayout;
    private TextView mTopTitle;
    private LinearLayout mLl_titles;
    private LinearLayout mLl_title_checked;
    private ImageView mIv_title_shadow;
    private LinearLayout mLl_tabContents;
    private long mSupportProperty = 0;
    private long mCustomProperty = 0;
    private int mCurrentTab = 0;
    private String[] mSupportTabNames;
    private List<Map<String, Object>> mCustomTabList;
    private List<Map<String, Object>> mCustomItemList;

    private int[] mColors;
    private int[] mOpacitys = PB_OPACITYS;
    private int[] mRotations = PB_ROTAIIONS;
    private int mColor;
    private int mOpacity = mOpacitys[mOpacitys.length - 1];
    private int mRotation = mRotations[0];
    private String mFontname;
    private String mOverlayText;
    private float mFontsize = 24.0f;
    private float mLinewith = 6.0f;
    private int[] mLinestyles = new int[]{1, 2, 3, 4, 5};
    private int mLinestyle = mLinestyles[0];

    private int[] mOpacityIds = new int[]{R.drawable.pb_opacity25, R.drawable.pb_opacity50, R.drawable.pb_opacity75, R.drawable.pb_opacity100};
    private int[] mOpacityIdsChecked = new int[]{R.drawable.pb_opacity25_pressed, R.drawable.pb_opacity50_pressed, R.drawable.pb_opacity75_pressed, R.drawable.pb_opacity100_pressed};
    private int[] mTypePicIds = new int[]{R.drawable.pb_note_type_comment, R.drawable.pb_note_type_key, R.drawable.pb_note_type_note,
            R.drawable.pb_note_type_help, R.drawable.pb_note_type_new_paragraph, R.drawable.pb_note_type_paragraph, R.drawable.pb_note_type_insert};
    private int mNoteIconType = NoteConstants.TA_ICON_COMMENT;

    private String[] mFontNames;
    private boolean[] mFontChecked;

    private ArrayList<String> mDistanceUnitArrayString;
    private boolean[] mDistanceUnitChecked;
    private int currentDistanceUnit = 0;
    private int scaleFromValue=1;

    private ArrayList<String> mDistanceDisplayTipArrayString;
    private boolean[] mDistanceDisplayTipChecked;
    private int currentDistanceDisplayTip = 0;
    private int scaleToValue = 1;

    private float[] mFontSizes = PB_FONTSIZES;
    private boolean[] mFontSizeChecked;

    private EditText mScaleEdt;
    private int mScalePercent = 20;
    private int mScaleSwitch = 0;

    private FontAdapter mFontAdapter;
    private FontSizeAdapter mFontSizeAdapter;
    private TypeAdapter mTypeAdapter;
    private DistanceUnitAdapter distanceUnitAdapter;

    private PropertyChangeListener mPropertyChangeListener;
    private PropertyBar.DismissListener mDismissListener;
    private int mPadWidth;
    private boolean mShowMask = false;

    private RectF mRectF;
    private float offset = 0;
    private boolean mClearCustomProperty = true;
    private boolean isFullScreen = false;

    private AppDisplay display;
    private PDFViewCtrl mPdfViewCtrl = null;

    private int mLastRotationId = -1;

    private boolean canEdit = true;

    public PropertyBarImpl(Context context, PDFViewCtrl pdfViewer) {
        this(context, null, pdfViewer);
    }

    public PropertyBarImpl(Context context, AttributeSet attrs, PDFViewCtrl pdfViewer) {
        this(context, attrs, 0, pdfViewer);
    }

    public PropertyBarImpl(Context context, AttributeSet attrs, int defStyleAttr, PDFViewCtrl pdfViewCtrl) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        this.mPdfViewCtrl = pdfViewCtrl;
        display = AppDisplay.getInstance(context);
        initVariable();
        initView();

        if (display.isPad()) {
            setWidth(mPadWidth);
        } else {
            setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        }
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setContentView(mPopupView);

        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(0));
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        if (!display.isPad()) {
            setAnimationStyle(R.style.PB_PopupAnimation);
        }
        setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (mDismissListener != null) {
                    mDismissListener.onDismiss();
                }

                if (mShowMask) {
                    mShowMask = false;
                }
                if (!display.isPad()) {
                    setPhoneFullScreen(false);
                }
                UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
                // move full screen to original position if it's moved to somewhere in selected annotation model.
                if ((!display.isPad()) && currentAnnotHandler != null && uiExtensionsManager.getCurrentToolHandler() == null) {
                    if (offset > 0) {
                        mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                        offset = 0;
                    }
                }
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() != null) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
            }
        });

        if (mPdfViewCtrl.getUIExtensionsManager() != null) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).registerConfigurationChangedListener(configurationChangedListener);
        }
    }

    private void initVariable() {
        mPadWidth = display.dp2px(320.0f);

        int[] colors = new int[PropertyBar.PB_COLORS_TEXT.length];
        System.arraycopy(PropertyBar.PB_COLORS_TEXT, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_TEXT[0];
        mColors = colors;
        mColor = mColors[0];
        mRectF = new RectF();
        mSupportTabNames = new String[]{mContext.getApplicationContext().getString(R.string.pb_icon_tab),
                mContext.getApplicationContext().getString(R.string.pb_fill_tab),
                mContext.getApplicationContext().getString(R.string.pb_border_tab),
                mContext.getApplicationContext().getString(R.string.pb_fontname_tab),
                mContext.getApplicationContext().getString(R.string.pb_watermark_tab),
                mContext.getApplicationContext().getString(R.string.pb_overlay_text_tab)};
        mTabs = new ArrayList<String>();
        mCustomTabList = new ArrayList<Map<String, Object>>();
        mCustomItemList = new ArrayList<Map<String, Object>>();

        mFontname = mContext.getApplicationContext().getString(R.string.fx_font_courier);
        mFontNames = new String[]{mContext.getApplicationContext().getString(R.string.fx_font_courier),
                mContext.getApplicationContext().getString(R.string.fx_font_helvetica),
                mContext.getApplicationContext().getString(R.string.fx_font_times)};
        mFontChecked = new boolean[mFontNames.length];
        for (int i = 0; i < mFontNames.length; i++) {
            if (i == 0) {
                mFontChecked[i] = true;
            } else {
                mFontChecked[i] = false;
            }
        }
        mFontSizeChecked = new boolean[mFontSizes.length];
        for (int i = 0; i < mFontSizes.length; i++) {
            if (i == 0) {
                mFontSizeChecked[i] = true;
            } else {
                mFontSizeChecked[i] = false;
            }
        }
        mClearCustomProperty = true;

        mDistanceUnitArrayString = fakeDate();
        mDistanceUnitChecked = new boolean[mDistanceUnitArrayString.size()];
        for (int i = 0; i < mDistanceUnitArrayString.size(); i++) {
            //default:pt
            if (i == 0) {
                mDistanceUnitChecked[i] = true;
            } else {
                mDistanceUnitChecked[i] = false;
            }
        }


        mDistanceDisplayTipArrayString = fakeDate();
        mDistanceDisplayTipChecked = new boolean[mDistanceDisplayTipArrayString.size()];
        for (int i = 0; i < mDistanceDisplayTipArrayString.size(); i++) {
            //default:pt
            if (i == 0) {
                mDistanceDisplayTipChecked[i] = true;
            } else {
                mDistanceDisplayTipChecked[i] = false;
            }
        }
    }

    private ArrayList<String> fakeDate() {
        ArrayList<String> list = new ArrayList<>();
        for (DistanceMeasurement rest : DistanceMeasurement.values()) {
            list.add(rest.getName());
        }
        return list;
    }


    private void initView() {
        mPopupView = new LinearLayout(mContext);
        mPopupView.setOrientation(LinearLayout.VERTICAL);
        mLl_root = new LinearLayout(mContext);
        mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mLl_root.setOrientation(LinearLayout.VERTICAL);
        mPopupView.addView(mLl_root);

        // ---phone top shadow
        if (!display.isPad()) {
            mTopShadow = new LinearLayout(mContext);
            mTopShadow.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mTopShadow.setOrientation(LinearLayout.VERTICAL);
            mLl_root.addView(mTopShadow);

            ImageView shadow = new ImageView(mContext);
            shadow.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_shadow_height)));
            shadow.setImageResource(R.drawable.search_shadow_bg270);
            mTopShadow.addView(shadow);

            ImageView shadowLine = new ImageView(mContext);
            shadowLine.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            shadowLine.setImageResource(R.color.ux_color_shadow_solid_line);
            mTopShadow.addView(shadowLine);
        }

        // ---top
        mLlArrowTop = new LinearLayout(mContext);
        mLlArrowTop.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mLlArrowTop.setOrientation(LinearLayout.VERTICAL);
        mLl_root.addView(mLlArrowTop);

        mIvArrowTop = new ImageView(mContext);
        mIvArrowTop.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mIvArrowTop.setImageResource(R.drawable.pb_arrow_top);
        mLlArrowTop.addView(mIvArrowTop);
        mLlArrowTop.setVisibility(View.GONE);

        // ---left center and right
        LinearLayout mLlArrowCenter = new LinearLayout(mContext);
        mLlArrowCenter.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        mLlArrowCenter.setOrientation(LinearLayout.HORIZONTAL);
        mLl_root.addView(mLlArrowCenter);

        //---left
        mLlArrowLeft = new LinearLayout(mContext);
        mLlArrowLeft.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        mLlArrowLeft.setOrientation(LinearLayout.VERTICAL);
        mLlArrowCenter.addView(mLlArrowLeft);

        mIvArrowLeft = new ImageView(mContext);
        mIvArrowLeft.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mIvArrowLeft.setImageResource(R.drawable.pb_arrow_left);
        mLlArrowLeft.addView(mIvArrowLeft);
        mLlArrowLeft.setVisibility(View.GONE);

        // ---center
        mLl_PropertyBar = new LinearLayout(mContext);
        mLl_PropertyBar.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        mLl_PropertyBar.setOrientation(LinearLayout.VERTICAL);
        if (display.isPad()) {
            mLl_PropertyBar.setBackgroundResource(R.drawable.pb_popup_bg_shadow);
            mLl_PropertyBar.setPadding(display.dp2px(4.0f), display.dp2px(4.0f), display.dp2px(4.0f), display.dp2px(4.0f));
        } else {
            mLl_PropertyBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
        }
        mLlArrowCenter.addView(mLl_PropertyBar);

        // ---right
        mLlArrowRight = new LinearLayout(mContext);
        mLlArrowRight.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        mLlArrowRight.setOrientation(LinearLayout.VERTICAL);
        mLlArrowCenter.addView(mLlArrowRight);

        mIvArrowRight = new ImageView(mContext);
        mIvArrowRight.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mIvArrowRight.setImageResource(R.drawable.pb_arrow_right);
        mLlArrowRight.addView(mIvArrowRight);
        mLlArrowRight.setVisibility(View.GONE);

        // ---bottom
        mLlArrowBottom = new LinearLayout(mContext);
        mLlArrowBottom.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mLlArrowBottom.setOrientation(LinearLayout.VERTICAL);
        mLl_root.addView(mLlArrowBottom);

        mIvArrowBottom = new ImageView(mContext);
        mIvArrowBottom.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mIvArrowBottom.setImageResource(R.drawable.pb_arrow_bottom);
        mLlArrowBottom.addView(mIvArrowBottom);
        mLlArrowBottom.setVisibility(View.GONE);

        addPbAll();
    }

    private void addPbAll() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.pb_rl_propertybar, null, false);

        mLl_topTabs = (LinearLayout) view.findViewById(R.id.pb_ll_top);
        if (display.isPad()) {
            mLl_topTabs.setBackgroundResource(R.drawable.pb_tabs_bg);
        } else {
            mLl_topTabs.setBackgroundResource(R.color.ux_text_color_subhead_colour);
        }

        mTopTitleLayout = (LinearLayout) view.findViewById(R.id.pb_topTitle_ll);
        mTopTitleLayout.setVisibility(View.GONE);
        mTopTitleLayout.setTag(0);
        if (display.isPad()) {
            mTopTitle = new TextView(mContext);
            mTopTitle.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            mTopTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimension(R.dimen.ux_text_height_title));
            mTopTitle.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
            mTopTitle.setTypeface(Typeface.DEFAULT);
            mTopTitle.setGravity(Gravity.CENTER);
            mTopTitle.setSingleLine(true);
            mTopTitle.setEllipsize(TextUtils.TruncateAt.END);

            mTopTitleLayout.addView(mTopTitle);
        } else {
            RelativeLayout relativeLayout = new RelativeLayout(mContext);
            relativeLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            relativeLayout.setGravity(Gravity.CENTER_VERTICAL);
            mTopTitleLayout.addView(relativeLayout);

            mTopTitle = new TextView(mContext);
            RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            titleLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            titleLayoutParams.leftMargin = display.dp2px(70.0f);
            mTopTitle.setLayoutParams(titleLayoutParams);
            mTopTitle.setSingleLine(true);
            mTopTitle.setEllipsize(TextUtils.TruncateAt.END);

            mTopTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimension(R.dimen.ux_text_height_title));
            mTopTitle.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
            mTopTitle.setTypeface(Typeface.DEFAULT);
            mTopTitle.setGravity(Gravity.CENTER_VERTICAL);
            mTopTitle.setSingleLine(true);
            mTopTitle.setEllipsize(TextUtils.TruncateAt.END);
            relativeLayout.addView(mTopTitle);

            ImageView img = new ImageView(mContext);
            RelativeLayout.LayoutParams imgLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            imgLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            imgLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            imgLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            img.setLayoutParams(imgLayoutParams);
            img.setImageResource(R.drawable.panel_topbar_close_selector);
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            relativeLayout.addView(img);

        }

        mLl_titles = (LinearLayout) view.findViewById(R.id.pb_ll_titles);
        mLl_title_checked = (LinearLayout) view.findViewById(R.id.pb_ll_title_checks);
        mIv_title_shadow = (ImageView) view.findViewById(R.id.pb_iv_title_shadow);
        mLl_tabContents = (LinearLayout) view.findViewById(R.id.pb_ll_tabContents);

        mLl_PropertyBar.addView(view);
    }

    @Override
    public void setPhoneFullScreen(boolean fullScreen) {
        if (!display.isPad()) {
            isFullScreen = fullScreen;

            LinearLayout tabLayout = (LinearLayout) mLl_tabContents.getParent();
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tabLayout.getLayoutParams();

            LinearLayout.LayoutParams tabContentsLayoutParams = (LinearLayout.LayoutParams) mLl_tabContents.getLayoutParams();
            if (fullScreen) {
                setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                tabContentsLayoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
            } else {
                setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                tabContentsLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            }
            tabLayout.setLayoutParams(layoutParams);
            mLl_tabContents.setLayoutParams(tabContentsLayoutParams);
        }
    }

    private View getScaleView() {
        View scaleItem = LayoutInflater.from(mContext).inflate(R.layout.pb_scale, null, false);
        mScaleEdt = (EditText) scaleItem.findViewById(R.id.pb_scale_percent);
        LinearLayout switchLayout = (LinearLayout) scaleItem.findViewById(R.id.pb_scale_switch_ll);
        ImageView switchImg = (ImageView) scaleItem.findViewById(R.id.pb_scale_switch);

        mScaleEdt.setText(String.valueOf(mScalePercent));
        mScaleEdt.setSelection(String.valueOf(mScalePercent).length());
        mScaleEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mScaleSwitch == 1) {
                    if (s.toString().length() == 0) {
                        return;
                    }
                    int percent = Integer.parseInt(s.toString());
                    if (percent < 1) {
                        mScaleEdt.setText(String.valueOf(mScalePercent));
                        mScaleEdt.selectAll();
                    } else if (percent > 100) {
                        mScaleEdt.setText(s.toString().substring(0, s.toString().length() - 1));
                        mScaleEdt.selectAll();
                    } else {
                        mScalePercent = percent;
                        if (mPropertyChangeListener != null) {
                            mPropertyChangeListener.onValueChanged(PROPERTY_SCALE_PERCENT, mScalePercent);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        if (mScaleSwitch == 1) {
            switchImg.setImageResource(R.drawable.setting_on);
            mScaleEdt.setEnabled(true);
        } else {
            switchImg.setImageResource(R.drawable.setting_off);
            mScaleEdt.setEnabled(false);
        }
        switchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView switchImage = (ImageView) ((LinearLayout) v).getChildAt(0);
                EditText scaleEdit = (EditText) ((LinearLayout) v.getParent()).getChildAt(0);

                if (mScaleSwitch == 1) {
                    mScaleSwitch = 0;
                    switchImage.setImageResource(R.drawable.setting_off);
                    scaleEdit.setEnabled(false);
                } else {
                    mScaleSwitch = 1;
                    switchImage.setImageResource(R.drawable.setting_on);
                    scaleEdit.setEnabled(true);
                }

                if (mPropertyChangeListener != null) {
                    mPropertyChangeListener.onValueChanged(PROPERTY_SCALE_SWITCH, mScaleSwitch);
                }
            }
        });

        return scaleItem;
    }

    private View getIconTypeView() {
        LinearLayout typeItem = new LinearLayout(mContext);
        typeItem.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        typeItem.setGravity(Gravity.CENTER);
        typeItem.setOrientation(LinearLayout.HORIZONTAL);

        ListView lv_type = new ListView(mContext);
        lv_type.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        lv_type.setCacheColorHint(mContext.getResources().getColor(R.color.ux_color_translucent));

        lv_type.setDivider(new ColorDrawable(mContext.getResources().getColor(R.color.ux_color_seperator_gray)));
        lv_type.setDividerHeight(1);
        typeItem.addView(lv_type);

        String[] typeNames = new String[]{mContext.getString(R.string.annot_text_comment),
                mContext.getApplicationContext().getString(R.string.annot_text_key),
                mContext.getApplicationContext().getString(R.string.annot_text_note),
                mContext.getApplicationContext().getString(R.string.annot_text_help),
                mContext.getApplicationContext().getString(R.string.annot_text_newparagraph),
                mContext.getApplicationContext().getString(R.string.annot_text_paragraph),
                mContext.getApplicationContext().getString(R.string.annot_text_insert)};
        mTypeAdapter = new TypeAdapter(mContext, mTypePicIds, typeNames);
        mTypeAdapter.setNoteIconType(mNoteIconType);
        lv_type.setAdapter(mTypeAdapter);

        if (canEdit) {
            lv_type.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mNoteIconType = ICONTYPES[position];
                    mTypeAdapter.setNoteIconType(mNoteIconType);
                    mTypeAdapter.notifyDataSetChanged();
                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_ANNOT_TYPE, mNoteIconType);
                    }
                }
            });
        } else {
            lv_type.setEnabled(false);
            typeItem.setAlpha(PB_ALPHA);
        }

        return typeItem;
    }

    private View getLineWidthView() {
        View lineWidthItem = LayoutInflater.from(mContext).inflate(R.layout.pb_linewidth, null, false);
        ThicknessImage thicknessImage = (ThicknessImage) lineWidthItem.findViewById(R.id.pb_img_lineWidth_mypic);
        TextView tv_width = (TextView) lineWidthItem.findViewById(R.id.pb_tv_lineWidth_size);
        tv_width.setText((int) (mLinewith + 0.5f) + "px");
        SeekBar sb_lineWidth = (SeekBar) lineWidthItem.findViewById(R.id.sb_lineWidth);
        sb_lineWidth.setProgress((int) (mLinewith - 1 + 0.5f));
        if (canEdit) {
            sb_lineWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    LinearLayout linearLayout = (LinearLayout) (seekBar.getParent());
                    ThicknessImage thicknessImage = (ThicknessImage) linearLayout.getChildAt(0);
                    TextView tv_width = (TextView) linearLayout.getChildAt(1);
                    if (progress >= 0 && progress < 12) {
                        mLinewith = progress + 1;
                        thicknessImage.setBorderThickness(progress + 1);
                        tv_width.setText((progress + 1) + "px");
                        if (mPropertyChangeListener != null) {
                            mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_LINEWIDTH, (float) (progress + 1));
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        } else {
            sb_lineWidth.setEnabled(false);
            lineWidthItem.setAlpha(PB_ALPHA);
        }

        thicknessImage.setBorderThickness(mLinewith);
        thicknessImage.setColor(mColor);

        return lineWidthItem;
    }

    private View getLineStyleView() {
        View lineStyleItem = LayoutInflater.from(mContext).inflate(R.layout.pb_linestyle, null, false);
        LinearLayout pb_ll_borderStyle = (LinearLayout) lineStyleItem.findViewById(R.id.pb_ll_borderStyle);
        for (int i = 0; i < mLinestyles.length; i++) {
            if (i + 1 == mLinestyle) {
                pb_ll_borderStyle.getChildAt(i).setBackgroundResource(R.drawable.pb_border_style_checked);
            } else {
                pb_ll_borderStyle.getChildAt(i).setBackgroundResource(0);
            }
        }
        for (int i = 0; i < mLinestyles.length; i++) {
            ImageView imageView = (ImageView) pb_ll_borderStyle.getChildAt(i);
            imageView.setTag(i);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout linearLayout = (LinearLayout) (v.getParent());
                    int tag = Integer.parseInt(v.getTag().toString());

                    for (int i = 0; i < mLinestyles.length; i++) {
                        if (i == tag) {
                            linearLayout.getChildAt(i).setBackgroundResource(R.drawable.pb_border_style_checked);
                        } else {
                            linearLayout.getChildAt(i).setBackgroundResource(0);
                        }
                    }
                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_LINE_STYLE, mLinestyles[tag]);
                    }
                }
            });
        }

        return lineStyleItem;
    }

    @Override
    public void scaleFromUnit(int index) {
        this.currentDistanceUnit = index;
    }

    @Override
    public void scaleFromValue(int value) {
        scaleFromValue = value;
    }

    @Override
    public void scaleToUnit(int index) {
        this.currentDistanceDisplayTip = index;
    }

    @Override
    public void scaleToValue(int value) {
        scaleToValue = value;
    }

    private String[] displayScale;

    @Override
    public void setDistanceScale(String[] distanceScale) {
        this.displayScale = distanceScale;
    }

    @Override
    public void setEditable(boolean canEdit) {
        this.canEdit = canEdit;
    }

    private View getDistanceUnitSettingView() {
        View distanceItem = LayoutInflater.from(mContext).inflate(R.layout.pb_distance_unit, null, false);
        TextView pb_tv_distance = (TextView) distanceItem.findViewById(R.id.pb_tv_distance_unit_1);
        TextView pb_tv_distance_distip = (TextView) distanceItem.findViewById(R.id.pb_tv_distance_unit_2);
        final EditText pv_et_distance1 = (EditText)distanceItem.findViewById(R.id.pb_tv_distance_unit_1_value);
        pv_et_distance1.setText(""+scaleFromValue);
        pv_et_distance1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!AppUtil.isBlank(s.toString())) {
                    scaleFromValue = Integer.parseInt(pv_et_distance1.getText().toString());
                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_DISTANCE_VALUE, Integer.parseInt(pv_et_distance1.getText().toString()));
                    }
                }
            }
        });

        final EditText pv_et_distance2 = (EditText)distanceItem.findViewById(R.id.pb_tv_distance_unit_2_value);
        pv_et_distance2.setText(""+scaleToValue);
        pv_et_distance2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!AppUtil.isBlank(s.toString())) {
                    scaleToValue = Integer.parseInt(pv_et_distance2.getText().toString());
                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_DISTANCE_TIP_VALUE, Integer.parseInt(pv_et_distance2.getText().toString()));
                    }
                }
            }
        });

        pb_tv_distance_distip.setText(mDistanceDisplayTipArrayString.get(currentDistanceDisplayTip));
        pb_tv_distance_distip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtil.dismissInputSoft(pv_et_distance1);
                mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mLl_root.getMeasuredHeight()));
                mLl_topTabs.setVisibility(View.GONE);
                mIv_title_shadow.setVisibility(View.GONE);
                mLl_tabContents.removeAllViews();
                mLl_tabContents.addView(getDistanceDisplayTipSelectedView());// 1 means show Font page, 2 means show FontSize page
            }
        });

        pb_tv_distance.setText(mDistanceUnitArrayString.get(currentDistanceUnit));
        pb_tv_distance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtil.dismissInputSoft(pv_et_distance1);
                mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mLl_root.getMeasuredHeight()));
                mLl_topTabs.setVisibility(View.GONE);
                mIv_title_shadow.setVisibility(View.GONE);
                mLl_tabContents.removeAllViews();
                mLl_tabContents.addView(getDistanceUnitSelectedView());// 1 means show Font page, 2 means show FontSize page
            }
        });


        if (!canEdit) {
            distanceItem.setAlpha(PB_ALPHA);
        }
        return distanceItem;
    }


    private View getDistanceUnitDisplayView() {
        View distanceItem = LayoutInflater.from(mContext).inflate(R.layout.pb_distance_scale_display, null, false);
        TextView pb_tv_distance = (TextView) distanceItem.findViewById(R.id.pb_tv_distance_unit_1);
        TextView pb_tv_distance_distip = (TextView) distanceItem.findViewById(R.id.pb_tv_distance_unit_2);
        TextView pv_et_distance1 = (TextView)distanceItem.findViewById(R.id.pb_tv_distance_unit_1_value);
        pv_et_distance1.setText(displayScale[0]);


        TextView pv_et_distance2 = (TextView)distanceItem.findViewById(R.id.pb_tv_distance_unit_2_value);
        pv_et_distance2.setText(displayScale[3]);

        pb_tv_distance_distip.setText(displayScale[4]);

        pb_tv_distance.setText(displayScale[1]);

        return distanceItem;
    }



    private View getDistanceUnitSelectedView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.pb_fontstyle_set, null, false);
        ImageView pb_iv_fontstyle_back = (ImageView) view.findViewById(R.id.pb_iv_fontstyle_back);
        pb_iv_fontstyle_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClearCustomProperty = false;
                reset(mSupportProperty);
            }
        });

        TextView pb_font_select_title = (TextView) view.findViewById(R.id.pb_font_select_title);
        ListView pb_lv_font = (ListView) view.findViewById(R.id.pb_lv_font);

        pb_font_select_title.setText(mContext.getApplicationContext().getString(R.string.fx_string_unit));
        for (int i = 0; i < mDistanceUnitArrayString.size(); i++) {
            if (i == currentDistanceUnit) {
                mDistanceUnitChecked[i] = true;
            } else {
                mDistanceUnitChecked[i] = false;
            }
        }
        distanceUnitAdapter = new DistanceUnitAdapter(mContext, mDistanceUnitArrayString, mDistanceUnitChecked);
        pb_lv_font.setAdapter(distanceUnitAdapter);

        pb_lv_font.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                for (int i = 0; i < mDistanceUnitChecked.length; i++) {
                    if (i == position) {
                        mDistanceUnitChecked[i] = true;
                    } else {
                        mDistanceUnitChecked[i] = false;
                    }
                }
                distanceUnitAdapter.notifyDataSetChanged();
                currentDistanceUnit = position;
                if (mPropertyChangeListener != null) {
                    mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_DISTANCE, position);
                }

            }
        });

        return view;
    }

    private View getDistanceDisplayTipSelectedView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.pb_fontstyle_set, null, false);
        ImageView pb_iv_fontstyle_back = (ImageView) view.findViewById(R.id.pb_iv_fontstyle_back);
        pb_iv_fontstyle_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClearCustomProperty = false;
                reset(mSupportProperty);
            }
        });

        TextView pb_font_select_title = (TextView) view.findViewById(R.id.pb_font_select_title);
        ListView pb_lv_font = (ListView) view.findViewById(R.id.pb_lv_font);

        pb_font_select_title.setText(mContext.getApplicationContext().getString(R.string.fx_string_unit));
        for (int i = 0; i < mDistanceDisplayTipArrayString.size(); i++) {
            if (i == currentDistanceDisplayTip) {
                mDistanceDisplayTipChecked[i] = true;
            } else {
                mDistanceDisplayTipChecked[i] = false;
            }
        }
        distanceUnitAdapter = new DistanceUnitAdapter(mContext, mDistanceDisplayTipArrayString, mDistanceDisplayTipChecked);
        pb_lv_font.setAdapter(distanceUnitAdapter);

        pb_lv_font.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                for (int i = 0; i < mDistanceDisplayTipChecked.length; i++) {
                    if (i == position) {
                        mDistanceDisplayTipChecked[i] = true;
                    } else {
                        mDistanceDisplayTipChecked[i] = false;
                    }
                }
                distanceUnitAdapter.notifyDataSetChanged();
                currentDistanceDisplayTip = position;
                if (mPropertyChangeListener != null) {
                    mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_DISTANCE_TIP, position);
                }

            }
        });

        return view;
    }



    private View getFontView() {
        View fontStyleItem = LayoutInflater.from(mContext).inflate(R.layout.pb_fontstyle, null, false);
        TextView pb_tv_font = (TextView) fontStyleItem.findViewById(R.id.pb_tv_font);
        pb_tv_font.setText(mFontname);
        if (canEdit) {
            pb_tv_font.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mLl_root.getMeasuredHeight()));
                    mLl_topTabs.setVisibility(View.GONE);
                    mIv_title_shadow.setVisibility(View.GONE);
                    mLl_tabContents.removeAllViews();
                    mLl_tabContents.addView(getFontSelectedView(TYPE_SHOW_FONT));// 1 means show Font page, 2 means show FontSize page
                }
            });
        } else {
            pb_tv_font.setClickable(false);
        }
        TextView pb_tv_fontSize = (TextView) fontStyleItem.findViewById(R.id.pb_tv_fontSize);
        pb_tv_fontSize.setText(((int) mFontsize) + "px");
        if (canEdit) {
            pb_tv_fontSize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mLl_root.getMeasuredHeight()));
                    mLl_topTabs.setVisibility(View.GONE);
                    mIv_title_shadow.setVisibility(View.GONE);
                    mLl_tabContents.removeAllViews();
                    mLl_tabContents.addView(getFontSelectedView(TYPE_SHOW_FONTSIZE));// 1 means show Font page, 2 means show FontSize page
                }
            });
        } else {
            pb_tv_fontSize.setClickable(false);
            fontStyleItem.setAlpha(PB_ALPHA);
        }

        return fontStyleItem;
    }

    private View getFontSelectedView(final int type) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.pb_fontstyle_set, null, false);
        ImageView pb_iv_fontstyle_back = (ImageView) view.findViewById(R.id.pb_iv_fontstyle_back);
        pb_iv_fontstyle_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClearCustomProperty = false;
                reset(mSupportProperty);
            }
        });

        TextView pb_font_select_title = (TextView) view.findViewById(R.id.pb_font_select_title);
        ListView pb_lv_font = (ListView) view.findViewById(R.id.pb_lv_font);
        if (type == TYPE_SHOW_FONT) {
            pb_font_select_title.setText(mContext.getApplicationContext().getString(R.string.fx_string_font));
            for (int i = 0; i < mFontNames.length; i++) {
                if (mFontNames[i].equals(mFontname)) {
                    mFontChecked[i] = true;
                } else {
                    mFontChecked[i] = false;
                }
            }
            mFontAdapter = new FontAdapter(mContext, mFontNames, mFontChecked);
            pb_lv_font.setAdapter(mFontAdapter);
        } else if (type == TYPE_SHOW_FONTSIZE) {
            pb_font_select_title.setText(mContext.getApplicationContext().getString(R.string.fx_string_fontsize));
            for (int i = 0; i < mFontSizes.length; i++) {
                if (mFontSizes[i] == mFontsize) {
                    mFontSizeChecked[i] = true;
                } else {
                    mFontSizeChecked[i] = false;
                }
            }
            mFontSizeAdapter = new FontSizeAdapter(mContext, mFontSizes, mFontSizeChecked);
            pb_lv_font.setAdapter(mFontSizeAdapter);
        }

        pb_lv_font.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (type == TYPE_SHOW_FONT) {
                    for (int i = 0; i < mFontChecked.length; i++) {
                        if (i == position) {
                            mFontChecked[i] = true;
                        } else {
                            mFontChecked[i] = false;
                        }
                    }
                    mFontAdapter.notifyDataSetChanged();
                    mFontname = mFontNames[position];
                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_FONTNAME, mFontNames[position]);
                    }
                } else if (type == TYPE_SHOW_FONTSIZE) {
                    for (int i = 0; i < mFontSizeChecked.length; i++) {
                        if (i == position) {
                            mFontSizeChecked[i] = true;
                        } else {
                            mFontSizeChecked[i] = false;
                        }
                    }
                    mFontSizeAdapter.notifyDataSetChanged();
                    mFontsize = mFontSizes[position];
                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_FONTSIZE, mFontSizes[position]);
                    }
                }
            }
        });

        return view;
    }

    private View getOpacityView() {
        View opacityItem = LayoutInflater.from(mContext).inflate(R.layout.pb_opacity, null, false);
        final LinearLayout pb_ll_opacity = (LinearLayout) opacityItem.findViewById(R.id.pb_ll_opacity);
        for (int i = 0; i < pb_ll_opacity.getChildCount(); i++) {
            if (i % 2 == 0) {
                ImageView iv_opacity_item = (ImageView) (((LinearLayout) pb_ll_opacity.getChildAt(i)).getChildAt(0));
                TextView tv_opacity_item = (TextView) (((LinearLayout) pb_ll_opacity.getChildAt(i)).getChildAt(1));
                iv_opacity_item.setTag(i);
                if (canEdit) {
                    iv_opacity_item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int tag = Integer.parseInt(v.getTag().toString());

                            for (int j = 0; j < pb_ll_opacity.getChildCount(); j++) {
                                if (j % 2 == 0) {
                                    ImageView iv_opacity = (ImageView) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((ImageView) v).getParent()).getParent()).getChildAt(j)).getChildAt(0);
                                    TextView tv_opacity = (TextView) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((ImageView) v).getParent()).getParent()).getChildAt(j)).getChildAt(1);
                                    if (tag == j) {
                                        ((ImageView) v).setImageResource(mOpacityIdsChecked[j / 2]);
                                        tv_opacity.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_button_colour));
                                    } else {
                                        iv_opacity.setImageResource(mOpacityIds[j / 2]);
                                        tv_opacity.setTextColor(mContext.getResources().getColor(R.color.ux_color_dark));
                                    }
                                }
                            }
                            if (mPropertyChangeListener != null) {
                                mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_OPACITY, mOpacitys[tag / 2]);
                                mOpacity = mOpacitys[tag / 2];
                            }
                        }
                    });
                } else {
                    iv_opacity_item.setEnabled(false);
                }

                if (mOpacity == mOpacitys[i / 2]) {
                    iv_opacity_item.setImageResource(mOpacityIdsChecked[i / 2]);
                    tv_opacity_item.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_button_colour));
                } else {
                    iv_opacity_item.setImageResource(mOpacityIds[i / 2]);
                    tv_opacity_item.setTextColor(mContext.getResources().getColor(R.color.ux_color_dark));
                }
            }
        }

        if (!canEdit) {
            opacityItem.setAlpha(PB_ALPHA);
        }

        return opacityItem;
    }

    private View getOverlayTextView() {
        final View overlayView = LayoutInflater.from(mContext).inflate(R.layout.pb_overlaytext, null, false);
        final EditText editOverlayText = overlayView.findViewById(R.id.pb_edit_overlaytext);
        editOverlayText.setText(mOverlayText);
        editOverlayText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mOverlayText = editOverlayText.getText().toString();
                if (mPropertyChangeListener != null) {
                    mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_OVERLAY_TEXT, mOverlayText);
                }
            }
        });

        ViewGroup parent = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView();
        AppKeyboardUtil.setKeyboardListener(parent, parent, new AppKeyboardUtil.IKeyboardListener() {

            @Override
            public void onKeyboardOpened(int keyboardHeight) {
                if (overlayView.hasFocus())
                    onLayout();
            }

            @Override
            public void onKeyboardClosed() {
                if (overlayView.hasFocus())
                    onLayout();
            }
        });

        editOverlayText.setEnabled(canEdit);
        if (!canEdit) {
            overlayView.setAlpha(PB_ALPHA);
        }
        return overlayView;
    }

    private void onLayout(){
        if (!display.isPad()) {
            int w1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int h1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mLl_root.measure(w1, h1);

            // move full screen to somewhere in selected annotation model.
            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
            AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();

            if (currentAnnotHandler != null && uiExtensionsManager.getCurrentToolHandler() == null) {
                ViewGroup viewGroup = uiExtensionsManager.getRootView();
                int height = viewGroup.getHeight();
                int width = viewGroup.getWidth();

                mLl_root.measure(w1, h1);
                if (mRectF.bottom > 0 && mRectF.bottom <= height) {
                    if (mRectF.bottom > height - mLl_root.getMeasuredHeight()) {
                        offset = mLl_root.getMeasuredHeight() - (height - mRectF.bottom);
                        mPdfViewCtrl.layout(0, 0 - (int) offset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() - (int) offset);
                    }

                } else if (mRectF.top >= 0 && mRectF.top <= height && mRectF.bottom > height) {
                    if (mRectF.top > height - mLl_root.getMeasuredHeight()) {
                        offset = mLl_root.getMeasuredHeight() - (height - mRectF.top) + 10;
                        mPdfViewCtrl.layout(0, 0 - (int) offset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() - (int) offset);
                    }
                }
            }
        }
    }

    private View getRotationView() {
        final View rotationItem = LayoutInflater.from(mContext).inflate(R.layout.pb_rotation, null, false);

        TextView pb_btn_rotation0 = (TextView) rotationItem.findViewById(R.id.pb_btn_rotation_0);
        TextView pb_btn_rotation90 = (TextView) rotationItem.findViewById(R.id.pb_btn_rotation_90);
        TextView pb_btn_rotation180 = (TextView) rotationItem.findViewById(R.id.pb_btn_rotation_180);
        TextView pb_btn_rotation270 = (TextView) rotationItem.findViewById(R.id.pb_btn_rotation_270);

        if (canEdit) {
            View.OnClickListener rotationClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = v.getId();
                    int tag = 0;

                    if (mLastRotationId == id) {
                        return;
                    }

                    if (mLastRotationId > 0) {
                        rotationItem.findViewById(mLastRotationId).setSelected(false);
                    }
                    rotationItem.findViewById(id).setSelected(true);

                    if (id == R.id.pb_btn_rotation_0) {
                        tag = 0;
                    } else if (id == R.id.pb_btn_rotation_90) {
                        tag = 1;
                    } else if (id == R.id.pb_btn_rotation_180) {
                        tag = 2;
                    } else if (id == R.id.pb_btn_rotation_270) {
                        tag = 3;
                    }

                    if (mPropertyChangeListener != null) {
                        mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_ROTATION, mRotations[tag]);
                        mRotation = mRotations[tag];
                    }

                    mLastRotationId = id;
                }
            };

            pb_btn_rotation0.setOnClickListener(rotationClickListener);
            pb_btn_rotation90.setOnClickListener(rotationClickListener);
            pb_btn_rotation180.setOnClickListener(rotationClickListener);
            pb_btn_rotation270.setOnClickListener(rotationClickListener);
        } else {
            pb_btn_rotation0.setClickable(false);
            pb_btn_rotation90.setClickable(false);
            pb_btn_rotation180.setClickable(false);
            pb_btn_rotation270.setClickable(false);

            rotationItem.setAlpha(PB_ALPHA);
        }

        int[] ids = {R.id.pb_btn_rotation_0, R.id.pb_btn_rotation_90, R.id.pb_btn_rotation_180, R.id.pb_btn_rotation_270};
        int rotationsLength = mRotations.length;
        for (int i = 0; i < rotationsLength; i++) {
            if (mRotation == mRotations[i]) {
                mLastRotationId = ids[i];
                rotationItem.findViewById(ids[i]).setSelected(true);
            } else {
                rotationItem.findViewById(ids[i]).setSelected(false);
            }
        }

        return rotationItem;
    }

    private View getColorView() {
        ViewGroup parent = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView();
        ColorView colorView = new ColorView(mContext, parent, mColor, mColors, canEdit);
        ViewGroup colorParent = (ViewGroup) colorView.getParent();
        if (colorParent != null) {
            colorParent.removeView(colorView);
        }
        colorView.setPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void onValueChanged(long property, int value) {
                mColor = value;
                if (mTabs.contains(mSupportTabNames[2])) {
                    ThicknessImage thicknessImage = (ThicknessImage) ((LinearLayout) ((LinearLayout) ((LinearLayout) mLl_tabContents.getChildAt(mTabs.indexOf(mSupportTabNames[2]))).getChildAt(1)).getChildAt(1)).getChildAt(0);
                    thicknessImage.setColor(mColor);
                }

                if (mPropertyChangeListener != null) {
                    mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_SELF_COLOR, value);
                }
            }

            @Override
            public void onValueChanged(long property, float value) {
            }

            @Override
            public void onValueChanged(long property, String value) {
            }
        });
        if (!canEdit) {
            colorView.setAlpha(PropertyBar.PB_ALPHA);
        }
        return colorView;
    }

    private UIExtensionsManager.ConfigurationChangedListener configurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            if ((mSupportProperty != 0 || mCustomProperty != 0) && mPdfViewCtrl.getDoc() != null) {
                mClearCustomProperty = false;
                reset(mSupportProperty);
            }
        }
    };

    @Override
    public void reset(final long items) {
        mSupportProperty = items;
        if (mClearCustomProperty) {
            mCustomProperty = 0;
            mCurrentTab = 0;
            mCustomTabList.clear();
            mCustomItemList.clear();
        }

        mTabs.clear();
        mLl_titles.removeAllViews();
        mLl_title_checked.removeAllViews();
        for (int i = 0; i < mLl_tabContents.getChildCount(); i++) {
            ViewGroup tabContent = (ViewGroup) mLl_tabContents.getChildAt(i);
            tabContent.removeAllViews();
        }
        mLl_tabContents.removeAllViews();

        mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (items == PropertyBar.PROPERTY_UNKNOWN) {
            mLl_topTabs.setVisibility(View.GONE);
            mIv_title_shadow.setVisibility(View.GONE);
        } else {
            mLl_topTabs.setVisibility(View.VISIBLE);
            mIv_title_shadow.setVisibility(View.VISIBLE);

            resetSupportedView();
        }

        if (!mClearCustomProperty) {
            if (mCustomProperty != 0) {
                resetCustomView();
            }
            mClearCustomProperty = true;
        }
    }

    @Override
    public void reset(long items, boolean clearCustomProperty) {
        mClearCustomProperty = clearCustomProperty;
        reset(items);
    }

    private void resetSupportedView() {
        if ((mSupportProperty & PropertyBar.PROPERTY_ANNOT_TYPE) == PropertyBar.PROPERTY_ANNOT_TYPE) {
            String iconTabTitle = mSupportTabNames[0];
            int iconTabIndex = 0;
            if (mTabs.size() > 0) {
                if (mTabs.contains(iconTabTitle)) {
                    iconTabIndex = mTabs.indexOf(iconTabTitle);
                    if (iconTabIndex < 0) {
                        iconTabIndex = 0;
                    }
                } else {
                    iconTabIndex = 0;
                }
            } else {
                iconTabIndex = 0;
            }
            mTopTitleLayout.setVisibility(View.GONE);
            addTab(iconTabTitle, iconTabIndex);
            addCustomItem(0, getIconTypeView(), iconTabIndex, -1);
        }

        if ((mSupportProperty & PropertyBar.PROPERTY_FONTNAME) == PropertyBar.PROPERTY_FONTNAME
                || (mSupportProperty & PropertyBar.PROPERTY_FONTSIZE) == PropertyBar.PROPERTY_FONTSIZE
                || (mSupportProperty & PropertyBar.PROPERTY_LINEWIDTH) == PropertyBar.PROPERTY_LINEWIDTH
                || (mSupportProperty & PropertyBar.PROPERTY_COLOR) == PropertyBar.PROPERTY_COLOR
                || (mSupportProperty & PropertyBar.PROPERTY_OPACITY) == PropertyBar.PROPERTY_OPACITY
                || (mSupportProperty & PropertyBar.PROPERTY_ROTATION) == PropertyBar.PROPERTY_ROTATION
                || (mSupportProperty & PropertyBar.PROPERTY_SCALE_PERCENT) == PropertyBar.PROPERTY_SCALE_PERCENT
                || (mSupportProperty & PropertyBar.PROPERTY_SCALE_SWITCH) == PropertyBar.PROPERTY_SCALE_SWITCH
                || (mSupportProperty & PropertyBar.PROPERTY_OVERLAY_TEXT) == PropertyBar.PROPERTY_OVERLAY_TEXT) {
            String propertyTabTitle = "";

            if ((mSupportProperty & PropertyBar.PROPERTY_OVERLAY_TEXT) == PropertyBar.PROPERTY_OVERLAY_TEXT) {
                propertyTabTitle = mSupportTabNames[5];
            }else if ((mSupportProperty & PropertyBar.PROPERTY_FONTNAME) == PropertyBar.PROPERTY_FONTNAME
                    || (mSupportProperty & PropertyBar.PROPERTY_FONTSIZE) == PropertyBar.PROPERTY_FONTSIZE
                    || (mSupportProperty & PropertyBar.PROPERTY_DISTANCE) == PropertyBar.PROPERTY_DISTANCE
                    || (mSupportProperty & PropertyBar.PROPERTY_DISTANCE_DISPLAY) == PropertyBar.PROPERTY_DISTANCE_DISPLAY) {
                propertyTabTitle = mSupportTabNames[3];
            } else if ((mSupportProperty & PropertyBar.PROPERTY_LINEWIDTH) == PropertyBar.PROPERTY_LINEWIDTH
                    || (mSupportProperty & PropertyBar.PROPERTY_LINE_STYLE) == PropertyBar.PROPERTY_LINE_STYLE) {
                propertyTabTitle = mSupportTabNames[2];
            } else if ((mSupportProperty & PropertyBar.PROPERTY_SCALE_PERCENT) == PropertyBar.PROPERTY_SCALE_PERCENT
                    || (mSupportProperty & PropertyBar.PROPERTY_SCALE_SWITCH) == PropertyBar.PROPERTY_SCALE_SWITCH) {
                propertyTabTitle = mSupportTabNames[4];
            } else {
                propertyTabTitle = mSupportTabNames[1];
            }

            int propertyTabIndex = mTabs.size();
            mTopTitleLayout.setVisibility(View.GONE);
            addTab(propertyTabTitle, propertyTabIndex);

            if ((mSupportProperty & PropertyBar.PROPERTY_SCALE_PERCENT) == PropertyBar.PROPERTY_SCALE_PERCENT
                    || (mSupportProperty & PropertyBar.PROPERTY_SCALE_SWITCH) == PropertyBar.PROPERTY_SCALE_SWITCH) {
                addCustomItem(0, getScaleView(), propertyTabIndex, -1);
            }

            if ((mSupportProperty & PropertyBar.PROPERTY_OVERLAY_TEXT) == PropertyBar.PROPERTY_OVERLAY_TEXT) {
                addCustomItem(0, getOverlayTextView(), propertyTabIndex, -1);
            }

            if ((mSupportProperty & PropertyBar.PROPERTY_FONTNAME) == PropertyBar.PROPERTY_FONTNAME
                    || (mSupportProperty & PropertyBar.PROPERTY_FONTSIZE) == PropertyBar.PROPERTY_FONTSIZE) {
                addCustomItem(0, getFontView(), propertyTabIndex, -1);
            }

            if ((mSupportProperty & PropertyBar.PROPERTY_DISTANCE) == PropertyBar.PROPERTY_DISTANCE) {
                addCustomItem(0, getDistanceUnitSettingView(), propertyTabIndex, -1);
            }

            if ((mSupportProperty & PropertyBar.PROPERTY_DISTANCE_DISPLAY) == PropertyBar.PROPERTY_DISTANCE_DISPLAY) {
                addCustomItem(0, getDistanceUnitDisplayView(), propertyTabIndex, -1);
            }

            if ((mSupportProperty & PropertyBar.PROPERTY_COLOR) == PropertyBar.PROPERTY_COLOR) {
                addCustomItem(0, getColorView(), propertyTabIndex, -1);
            }

            if ((mSupportProperty & PropertyBar.PROPERTY_LINEWIDTH) == PropertyBar.PROPERTY_LINEWIDTH) {
                addCustomItem(0, getLineWidthView(), propertyTabIndex, -1);
            }
            if ((mSupportProperty & PropertyBar.PROPERTY_LINE_STYLE) == PropertyBar.PROPERTY_LINE_STYLE) {
                addCustomItem(0, getLineStyleView(), propertyTabIndex, -1);
            }

            if ((mSupportProperty & PropertyBar.PROPERTY_ROTATION) == PropertyBar.PROPERTY_ROTATION) {
                addCustomItem(0, getRotationView(), propertyTabIndex, -1);
            }

            if ((mSupportProperty & PropertyBar.PROPERTY_OPACITY) == PropertyBar.PROPERTY_OPACITY) {
                addCustomItem(0, getOpacityView(), propertyTabIndex, -1);
            }
        }
    }

    private void resetCustomView() {
        for (int i = 0; i < mCustomTabList.size(); i++) {
            addTab(mCustomTabList.get(i).get("topTitle").toString(), (Integer) mCustomTabList.get(i).get("resid_img"),
                    mCustomTabList.get(i).get("title").toString(), (Integer) mCustomTabList.get(i).get("tabIndex"));
        }
        for (int i = 0; i < mCustomItemList.size(); i++) {
            long item = (Long) mCustomItemList.get(i).get("item");
            if ((item & mCustomProperty) == item) {
                addCustomItem(item, (View) mCustomItemList.get(i).get("itemView"), (Integer) mCustomItemList.get(i).get("tabIndex"),
                        (Integer) mCustomItemList.get(i).get("index"));
            }
        }
    }

    private void doAfterAddContentItem() {
        for (int i = 0; i < mLl_tabContents.getChildCount(); i++) {
            LinearLayout tabContentTemp = (LinearLayout) mLl_tabContents.getChildAt(i);
            if (tabContentTemp != null && tabContentTemp.getChildCount() > 0) {
                for (int j = 0; j < tabContentTemp.getChildCount(); j++) {
                    View viewItem = tabContentTemp.getChildAt(j);
                    if (viewItem != null) {
                        View separator = viewItem.findViewById(R.id.pb_separator_iv);
                        if (separator != null) {
                            if (j == tabContentTemp.getChildCount() - 1) {
                                separator.setVisibility(View.GONE);
                            } else {
                                separator.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
            }
        }

        resetContentHeight();

        for (int i = 0; i < mLl_tabContents.getChildCount(); i++) {
            if (i == mCurrentTab) {
                mLl_tabContents.getChildAt(i).setVisibility(View.VISIBLE);
            } else {
                mLl_tabContents.getChildAt(i).setVisibility(View.GONE);
            }
        }
    }

    private void resetContentHeight() {
        int maxTabContentHeight = 0;
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int iconTabIndex = -1;
        if (mTabs.contains(mSupportTabNames[0])) {
            iconTabIndex = mTabs.indexOf(mSupportTabNames[0]);
        }
        for (int i = 0; i < mLl_tabContents.getChildCount(); i++) {
            LinearLayout child = (LinearLayout) mLl_tabContents.getChildAt(i);
            child.measure(w, h);
            int childHeight = child.getMeasuredHeight();
            if (i == iconTabIndex) {
                childHeight = 0;
            }
            if (childHeight > maxTabContentHeight) {
                maxTabContentHeight = childHeight;
            }
        }

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mLl_tabContents.getLayoutParams();
        if (!(!display.isPad() && layoutParams.height == LinearLayout.LayoutParams.MATCH_PARENT)) {
            layoutParams.height = maxTabContentHeight;
            mLl_tabContents.setLayoutParams(layoutParams);
        }
    }

    private void checkContained() {
        boolean colorContained = false;
        for (int i = 0; i < mColors.length; i++) {
            if (mColor == mColors[i]) {
                colorContained = true;
                break;
            }
        }
        if (!colorContained) {
            mColor = mColors[0];
        }

        boolean colorOpacity = false;
        for (int i = 0; i < mOpacitys.length; i++) {
            if (mOpacity == mOpacitys[i]) {
                colorOpacity = true;
                break;
            }
        }
        if (!colorOpacity) {
            mOpacity = mOpacitys[mOpacitys.length - 1];
        }
    }

    @Override
    public void addTab(String title, int tabIndex) {
        if (tabIndex > mTabs.size() || tabIndex < 0)
            return;
        if (title.length() == 0) {
            mTabs.add(tabIndex, "");
        } else {
            mTabs.add(tabIndex, title);
        }

        TextView tv_title = new TextView(mContext);
        tv_title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tv_title.setText(title);
        tv_title.setTextSize(16.0f);
        tv_title.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
        tv_title.setTypeface(Typeface.DEFAULT);
        tv_title.setGravity(Gravity.CENTER);
        tv_title.setSingleLine(true);
        tv_title.setEllipsize(TextUtils.TruncateAt.END);
        tv_title.setPadding(0, display.dp2px(5.0f), 0, display.dp2px(10.0f));

        tv_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int clickTagIndex = 0;
                for (int i = 0; i < mLl_titles.getChildCount(); i++) {
                    if (v == mLl_titles.getChildAt(i)) {
                        clickTagIndex = i;
                    }
                }

                if (mCurrentTab != clickTagIndex) {
                    mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mLl_root.getMeasuredHeight()));
                    mCurrentTab = clickTagIndex;
                    setCurrentTab(mCurrentTab);
                }
            }
        });
        mLl_titles.addView(tv_title, tabIndex);

        ImageView iv_title_checked = new ImageView(mContext);
        iv_title_checked.setLayoutParams(new LinearLayout.LayoutParams(0, (int) mContext.getResources()
                .getDimension(R.dimen.ux_tab_selection_height), 1));
        mLl_title_checked.addView(iv_title_checked);

        LinearLayout ll_content = new LinearLayout(mContext);
        ll_content.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ll_content.setOrientation(LinearLayout.VERTICAL);
        mLl_tabContents.addView(ll_content, tabIndex);

        if (mTabs.size() + mCustomTabList.size() > 0) {
            if (mTabs.size() + mCustomTabList.size() == 1) {
                mLl_topTabs.setVisibility(View.GONE);
                mIv_title_shadow.setVisibility(View.GONE);
            } else {
                mLl_topTabs.setVisibility(View.VISIBLE);
                mIv_title_shadow.setVisibility(View.VISIBLE);

                setCurrentTab(mCurrentTab);
            }
        }
    }

    @Override
    public void setTopTitleVisible(boolean visible) {
        if (visible) {
            mTopTitleLayout.setVisibility(View.VISIBLE);
            mTopTitleLayout.setTag(1);
        } else {
            mTopTitleLayout.setVisibility(View.GONE);
            mTopTitleLayout.setTag(0);
        }
    }

    @Override
    public void addTab(String topTitle, int resid_img, String title, int tabIndex) {

        if (tabIndex > mTabs.size() + mCustomTabList.size() || tabIndex < 0) {
            return;
        }

        if (mClearCustomProperty) {
            Map<String, Object> map = new HashMap<String, Object>();
            if (title.length() == 0) {
                map.put("title", "");
            } else {
                map.put("title", title);
            }
            if (topTitle.length() == 0) {
                map.put("topTitle", "");
            } else {
                map.put("topTitle", topTitle);
            }
            map.put("resid_img", resid_img);
            map.put("tabIndex", tabIndex);
            mCustomTabList.add(map);
        }

        LinearLayout titleLayout = new LinearLayout(mContext);
        titleLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleLayout.setGravity(Gravity.CENTER);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setPadding(0, display.dp2px(5.0f), 0, display.dp2px(10.0f));

        titleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int clickTagIndex = 0;
                for (int i = 0; i < mLl_titles.getChildCount(); i++) {
                    if (v == mLl_titles.getChildAt(i)) {
                        clickTagIndex = i;
                    }
                }

                if (mCurrentTab != clickTagIndex) {
                    mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mLl_root.getMeasuredHeight()));
                    mCurrentTab = clickTagIndex;
                    setCurrentTab(mCurrentTab);
                }
            }
        });

        if (resid_img != 0 && resid_img > 0) {
            ImageView img = new ImageView(mContext);
            img.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            img.setImageResource(resid_img);
            titleLayout.addView(img);
        }

        if (title != null && !"".equals(title)) {
            TextView tv_title = new TextView(mContext);
            tv_title.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            tv_title.setText(title);
            tv_title.setTextSize(16.0f);
            tv_title.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
            tv_title.setGravity(Gravity.CENTER);
            tv_title.setSingleLine(true);
            tv_title.setEllipsize(TextUtils.TruncateAt.END);
            titleLayout.addView(tv_title);
        }
        mLl_titles.addView(titleLayout, tabIndex);

        ImageView iv_title_checked = new ImageView(mContext);
        iv_title_checked.setLayoutParams(new LinearLayout.LayoutParams(0, (int) mContext.getResources()
                .getDimension(R.dimen.ux_tab_selection_height), 1));
        mLl_title_checked.addView(iv_title_checked);

        LinearLayout ll_content = new LinearLayout(mContext);
        ll_content.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ll_content.setOrientation(LinearLayout.VERTICAL);
        mLl_tabContents.addView(ll_content, tabIndex);

        if (mTabs.size() + mCustomTabList.size() > 0) {
            if (mTabs.size() + mCustomTabList.size() == 1) {
                mLl_topTabs.setVisibility(View.GONE);
                mIv_title_shadow.setVisibility(View.GONE);
            } else {
                mLl_topTabs.setVisibility(View.VISIBLE);
                mIv_title_shadow.setVisibility(View.VISIBLE);

                setCurrentTab(mCurrentTab);
            }
        }
    }

    @Override
    public int getCurrentTabIndex() {
        return mCurrentTab;
    }

    @Override
    public void setCurrentTab(int currentTab) {
        mCurrentTab = currentTab;
        for (int i = 0; i < mLl_titles.getChildCount(); i++) {
            if (i == currentTab) {
                View viewTab = mLl_titles.getChildAt(i);
                if (viewTab instanceof TextView) {
                    mTopTitle.setText("");
                    mTopTitleLayout.setVisibility(View.GONE);
                } else if (viewTab instanceof LinearLayout) {
                    if ((Integer) mTopTitleLayout.getTag() == 1) {
                        mTopTitleLayout.setVisibility(View.VISIBLE);
                        if (mTopTitleLayout.getVisibility() == View.VISIBLE) {
                            String topTitle = "";
                            for (int j = 0; j < mCustomTabList.size(); j++) {
                                if (currentTab == (Integer) mCustomTabList.get(j).get("tabIndex")) {
                                    topTitle = mCustomTabList.get(j).get("topTitle").toString();
                                    break;
                                }
                            }
                            mTopTitle.setText(topTitle);
                        }
                    }

                    View view = ((LinearLayout) viewTab).getChildAt(0);
                    if (view != null && view instanceof ImageView) {
                        ((ImageView) view).setImageState(new int[]{android.R.attr.state_selected}, true);
                        ((ImageView) view).setSelected(true);
                    }
                }

                ((ImageView) mLl_title_checked.getChildAt(i)).setImageDrawable(new ColorDrawable(Color.WHITE));
                mLl_tabContents.getChildAt(i).setVisibility(View.VISIBLE);
            } else {
                View viewTab = mLl_titles.getChildAt(i);
                if (viewTab instanceof LinearLayout) {
                    View view = ((LinearLayout) mLl_titles.getChildAt(i)).getChildAt(0);
                    if (view != null && view instanceof ImageView) {
                        ((ImageView) view).setImageState(new int[]{}, true);
                        ((ImageView) view).setSelected(false);
                    }
                } else if (viewTab instanceof TextView) {

                }

                ((ImageView) mLl_title_checked.getChildAt(i)).setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
                mLl_tabContents.getChildAt(i).setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemIndex(long item) {
        int indexItemInTab = -1;
        if ((mSupportProperty & item) == item) {
            if (item == PropertyBar.PROPERTY_ANNOT_TYPE) {
                indexItemInTab = mTabs.indexOf(mSupportTabNames[0]);
            } else {
                if (mTabs.contains(mSupportTabNames[1])) {
                    indexItemInTab = mTabs.indexOf(mSupportTabNames[1]);
                }
                if (mTabs.contains(mSupportTabNames[2])) {
                    indexItemInTab = mTabs.indexOf(mSupportTabNames[2]);
                }
                if (mTabs.contains(mSupportTabNames[3])) {
                    indexItemInTab = mTabs.indexOf(mSupportTabNames[3]);
                }
                if (mTabs.contains(mSupportTabNames[4])) {
                    indexItemInTab = mTabs.indexOf(mSupportTabNames[4]);
                }
            }
        } else {
            if ((mCustomProperty & item) == item) {
                for (int i = 0; i < mCustomItemList.size(); i++) {
                    if (item == (Long) mCustomItemList.get(i).get("item")) {
                        indexItemInTab = (Integer) mCustomItemList.get(i).get("tabIndex");
                        break;
                    }
                }
            }
        }

        return indexItemInTab;
    }

    @Override
    public void addCustomItem(long item, View itemView, int tabIndex, int index) {
        if (itemView == null) {
            return;
        }
        if (tabIndex < 0 || tabIndex > mLl_tabContents.getChildCount() - 1) {
            return;
        }
        View view = mLl_tabContents.getChildAt(tabIndex);
        if (view != null) {
            LinearLayout ll_content = (LinearLayout) view;
            if (index != -1 && (index < 0 || index > ll_content.getChildCount())) {
                return;
            }

            if (item > 0 && mClearCustomProperty) {
                mCustomProperty = mCustomProperty | item;
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("item", item);
                map.put("itemView", itemView);
                map.put("tabIndex", tabIndex);
                map.put("index", index);
                mCustomItemList.add(map);
            }

            ViewGroup itemParent = (ViewGroup) itemView.getParent();
            if (itemParent != null) {
                itemParent.removeView(itemView);
            }

            if (index == -1) {
                ll_content.addView(itemView);
            } else {
                if (index < 0 || index > ll_content.getChildCount()) {
                    return;
                }
                ll_content.addView(itemView, index);
            }
        }
        doAfterAddContentItem();
    }

    @Override
    public void addContentView(View contentView) {
        mLl_tabContents.addView(contentView);
    }

    @Override
    public View getContentView() {
        return super.getContentView();
    }

    @Override
    public void update(RectF rectF){
        ViewGroup parent = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView();
        update(parent, rectF);
    }

    @Override
    public void update(View parent, RectF rectF) {
        showSystemUI();

        mRectF.set(rectF);
        int height = parent.getHeight();
        int width = parent.getWidth();
        if (display.isPad()) {
            int w1 = View.MeasureSpec.makeMeasureSpec(mPadWidth, View.MeasureSpec.EXACTLY);
            int h1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mLl_root.measure(w1, h1);

            int[] location = new int[2];
            parent.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];

            int arrowPosition;
            if (rectF.top >= mLl_root.getMeasuredHeight()) {
                arrowPosition = ARROW_BOTTOM;
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.VISIBLE);
            } else if (height + y - rectF.bottom >= mLl_root.getMeasuredHeight()) {
                arrowPosition = ARROW_TOP;
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.VISIBLE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.GONE);
            } else if (width + x - rectF.right >= mPadWidth) {
                arrowPosition = ARROW_LEFT;
                mLlArrowLeft.setVisibility(View.VISIBLE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.GONE);
            } else if (rectF.left >= mPadWidth) {
                arrowPosition = ARROW_RIGHT;
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.VISIBLE);
                mLlArrowBottom.setVisibility(View.GONE);
            } else {
                arrowPosition = ARROW_CENTER;
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.GONE);
            }
            if (mArrowVisible) {
                mLl_PropertyBar.setBackgroundResource(R.drawable.pb_popup_bg);
                mLl_PropertyBar.setPadding(0, 0, 0, display.dp2px(5.0f));
            } else {
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.GONE);
                mLl_PropertyBar.setBackgroundResource(R.drawable.pb_popup_bg_shadow);
                mLl_PropertyBar.setPadding(display.dp2px(4.0f), display.dp2px(4.0f),
                        display.dp2px(4.0f), display.dp2px(4.0f));
            }

            mLl_root.measure(w1, h1);
            if (arrowPosition == ARROW_BOTTOM) {
                mIvArrowBottom.measure(0, 0);

                int toLeft;
                if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                    if (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                        if (mArrowVisible) {
                            mLlArrowBottom.setPadding((int) (mPadWidth / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0, 0, 0);
                        }
                    } else {
                        toLeft = width + x - mPadWidth;
                        if (mArrowVisible) {
                            if (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f > mIvArrowBottom.getMeasuredWidth() / 2.0f) {
                                mLlArrowBottom.setPadding(0, 0, (int) (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0);
                            } else {
                                mLlArrowBottom.setPadding(mPadWidth - mIvArrowBottom.getMeasuredWidth(), 0, 0, 0);
                            }
                        }
                    }
                } else {
                    toLeft = 0;
                    if (mArrowVisible) {
                        if (rectF.left + (rectF.right - rectF.left) / 2.0f > mIvArrowBottom.getMeasuredWidth() / 2.0f) {
                            mLlArrowBottom.setPadding((int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0, 0, 0);
                        } else {
                            mLlArrowBottom.setPadding(0, 0, 0, 0);
                        }
                    }
                }

                update(toLeft, (int) (rectF.top - mLl_root.getMeasuredHeight()), -1, -1);
            } else if (arrowPosition == ARROW_TOP) {
                mIvArrowTop.measure(0, 0);

                int toLeft;
                if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                    if (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                        if (mArrowVisible) {
                            mLlArrowTop.setPadding((int) (mPadWidth / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0, 0, 0);
                        }
                    } else {
                        toLeft = width + x - mPadWidth;
                        if (mArrowVisible) {
                            if (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f > mIvArrowTop.getMeasuredWidth() / 2.0f) {
                                mLlArrowTop.setPadding(0, 0, (int) (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0);
                            } else {
                                mLlArrowTop.setPadding(mPadWidth - mIvArrowTop.getMeasuredWidth(), 0, 0, 0);
                            }
                        }
                    }
                } else {
                    toLeft = 0;
                    if (mArrowVisible) {
                        if (rectF.left + (rectF.right - rectF.left) / 2.0f > mIvArrowTop.getMeasuredWidth() / 2.0f) {
                            mLlArrowTop.setPadding((int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0, 0, 0);
                        } else {
                            mLlArrowTop.setPadding(0, 0, 0, 0);
                        }
                    }
                }

                update(toLeft, (int) rectF.bottom, -1, -1);

            } else if (arrowPosition == ARROW_LEFT) {
                mIvArrowLeft.measure(0, 0);

                int toTop;
                if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                    if (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                        if (mArrowVisible) {
                            mLlArrowLeft.setPadding(0, (int) (mLl_root.getMeasuredHeight() / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f), 0, 0);
                        }
                    } else {
                        toTop = height + y - mLl_root.getMeasuredHeight();
                        if (mArrowVisible) {
                            if (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mIvArrowLeft.getMeasuredHeight() / 2.0f) {
                                mLlArrowLeft.setPadding(0, 0, 0, (int) (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f));
                            } else {
                                mLlArrowLeft.setPadding(0, mLl_root.getMeasuredHeight() - mIvArrowLeft.getMeasuredHeight(), 0, 0);
                            }
                        }
                    }
                } else {
                    toTop = 0;
                    if (mArrowVisible) {
                        if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mIvArrowLeft.getMeasuredHeight() / 2.0f) {
                            mLlArrowLeft.setPadding(0, (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f), 0, 0);
                        } else {
                            mLlArrowLeft.setPadding(0, 0, 0, 0);
                        }
                    }
                }

                update((int) (rectF.right), toTop, -1, -1);
            } else if (arrowPosition == ARROW_RIGHT) {
                mIvArrowRight.measure(0, 0);

                int toTop;
                if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                    if (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                        if (mArrowVisible) {
                            mLlArrowRight.setPadding(0, (int) (mLl_root.getMeasuredHeight() / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f), 0, 0);
                        }
                    } else {
                        toTop = height + y - mLl_root.getMeasuredHeight();
                        if (mArrowVisible) {
                            if (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mIvArrowRight.getMeasuredHeight() / 2.0f) {
                                mLlArrowRight.setPadding(0, 0, 0, (int) (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f));
                            } else {
                                mLlArrowRight.setPadding(0, mLl_root.getMeasuredHeight() - mIvArrowRight.getMeasuredHeight(), 0, 0);
                            }
                        }
                    }
                } else {
                    toTop = 0;
                    if (mArrowVisible) {
                        if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mIvArrowRight.getMeasuredHeight() / 2.0f) {
                            mLlArrowRight.setPadding(0, (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f), 0, 0);
                        } else {
                            mLlArrowRight.setPadding(0, 0, 0, 0);
                        }
                    }
                }

                update((int) (rectF.left - mPadWidth), toTop, -1, -1);
            } else if (arrowPosition == ARROW_CENTER) {
                update((int) (rectF.left + (rectF.right - rectF.left) / 4.0f), (int) (rectF.top + (rectF.bottom - rectF.top) / 4.0f), -1, -1);
            }
        } else {
            mArrowVisible = false;
            mLlArrowLeft.setVisibility(View.GONE);
            mLlArrowTop.setVisibility(View.GONE);
            mLlArrowRight.setVisibility(View.GONE);
            mLlArrowBottom.setVisibility(View.GONE);
            mLl_PropertyBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));

            if (Build.VERSION.SDK_INT == 24) {
                int screenHeight = AppDisplay.getInstance(mContext).getRawScreenHeight();
                mLl_root.measure(0, 0);
                int barHeight = mLl_root.getMeasuredHeight();
                int navBarHeight = AppDisplay.getInstance(mContext).getNavBarHeight();

                if (isFullScreen) {
                    update(0, 0, width, screenHeight - navBarHeight);
                } else {
                    update(0, screenHeight - barHeight - navBarHeight, width, -1);
                }
            } else {
                update(0, 0, width, -1);
            }
        }
    }

    @Override
    public boolean isShowing() {
        return super.isShowing();
    }


    @Override
    public void show(RectF rectF, boolean showMask) {
        if (this != null && !this.isShowing()) {
            ViewGroup viewGroup = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView();
            show(viewGroup,rectF,showMask);
        }
    }

    @Override
    public void show(View parent, RectF rectF, boolean showMask) {
        showSystemUI();
        mRectF.set(rectF);
        if (this != null && !this.isShowing()) {
            setFocusable(true);

            int height = parent.getHeight();
            int width = parent.getWidth();

            int w1;
            if (display.isPad()) {
                w1 = View.MeasureSpec.makeMeasureSpec(mPadWidth, View.MeasureSpec.EXACTLY);
            } else {
                w1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            }
            int h1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mLl_root.measure(w1, h1);

            int[] location = new int[2];
            parent.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];

            if (display.isPad()) {
                int arrowPosition;
                if (rectF.top >= mLl_root.getMeasuredHeight()) {
                    arrowPosition = ARROW_BOTTOM;
                    mLlArrowLeft.setVisibility(View.GONE);
                    mLlArrowTop.setVisibility(View.GONE);
                    mLlArrowRight.setVisibility(View.GONE);
                    mLlArrowBottom.setVisibility(View.VISIBLE);
                } else if (height + y - rectF.bottom >= mLl_root.getMeasuredHeight()) {
                    arrowPosition = ARROW_TOP;
                    mLlArrowLeft.setVisibility(View.GONE);
                    mLlArrowTop.setVisibility(View.VISIBLE);
                    mLlArrowRight.setVisibility(View.GONE);
                    mLlArrowBottom.setVisibility(View.GONE);
                } else if (width + x - rectF.right >= mPadWidth) {
                    arrowPosition = ARROW_LEFT;
                    mLlArrowLeft.setVisibility(View.VISIBLE);
                    mLlArrowTop.setVisibility(View.GONE);
                    mLlArrowRight.setVisibility(View.GONE);
                    mLlArrowBottom.setVisibility(View.GONE);
                } else if (rectF.left>= mPadWidth) {
                    arrowPosition = ARROW_RIGHT;
                    mLlArrowLeft.setVisibility(View.GONE);
                    mLlArrowTop.setVisibility(View.GONE);
                    mLlArrowRight.setVisibility(View.VISIBLE);
                    mLlArrowBottom.setVisibility(View.GONE);
                } else {
                    arrowPosition = ARROW_CENTER;
                    mLlArrowLeft.setVisibility(View.GONE);
                    mLlArrowTop.setVisibility(View.GONE);
                    mLlArrowRight.setVisibility(View.GONE);
                    mLlArrowBottom.setVisibility(View.GONE);
                }
                if (mArrowVisible) {
                    mLl_PropertyBar.setBackgroundResource(R.drawable.pb_popup_bg);
                    mLl_PropertyBar.setPadding(0, 0, 0, display.dp2px(5.0f));
                } else {
                    mLlArrowLeft.setVisibility(View.GONE);
                    mLlArrowTop.setVisibility(View.GONE);
                    mLlArrowRight.setVisibility(View.GONE);
                    mLlArrowBottom.setVisibility(View.GONE);
                    mLl_PropertyBar.setBackgroundResource(R.drawable.pb_popup_bg_shadow);
                    mLl_PropertyBar.setPadding(display.dp2px(4.0f), display.dp2px(4.0f),
                            display.dp2px(4.0f), display.dp2px(4.0f));
                }

                int w2 = View.MeasureSpec.makeMeasureSpec(mPadWidth, View.MeasureSpec.EXACTLY);
                int h2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                mLl_root.measure(w2, h2);

                if (arrowPosition == ARROW_BOTTOM) {
                    mIvArrowBottom.measure(0, 0);

                    int toLeft;
                    if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        if (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                            toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                            if (mArrowVisible) {
                                mLlArrowBottom.setPadding((int) (mPadWidth / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0, 0, 0);
                            }
                        } else {
                            toLeft = width + x - mPadWidth;
                            if (mArrowVisible) {
                                if (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f > mIvArrowBottom.getMeasuredWidth() / 2.0f) {
                                    mLlArrowBottom.setPadding(0, 0, (int) (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0);
                                } else {
                                    mLlArrowBottom.setPadding(mPadWidth - mIvArrowBottom.getMeasuredWidth(), 0, 0, 0);
                                }
                            }
                        }
                    } else {
                        toLeft = 0;
                        if (mArrowVisible) {
                            if (rectF.left + (rectF.right - rectF.left) / 2.0f > mIvArrowBottom.getMeasuredWidth() / 2.0f) {
                                mLlArrowBottom.setPadding((int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mIvArrowBottom.getMeasuredWidth() / 2.0f), 0, 0, 0);
                            } else {
                                mLlArrowBottom.setPadding(0, 0, 0, 0);
                            }
                        }
                    }

                    showAtLocation(parent, Gravity.LEFT | Gravity.TOP, toLeft, (int) (rectF.top - mLl_root.getMeasuredHeight()));
                } else if (arrowPosition == ARROW_TOP) {
                    mIvArrowTop.measure(0, 0);

                    int toLeft;
                    if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        if (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                            toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                            if (mArrowVisible) {
                                mLlArrowTop.setPadding((int) (mPadWidth / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0, 0, 0);
                            }
                        } else {
                            toLeft = width + x - mPadWidth;
                            if (mArrowVisible) {
                                if (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f > mIvArrowTop.getMeasuredWidth() / 2.0f) {
                                    mLlArrowTop.setPadding(0, 0, (int) (width + x - rectF.left - (rectF.right - rectF.left) / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0);
                                } else {
                                    mLlArrowTop.setPadding(mPadWidth - mIvArrowTop.getMeasuredWidth(), 0, 0, 0);
                                }
                            }
                        }
                    } else {
                        toLeft = 0;
                        if (mArrowVisible) {
                            if (rectF.left + (rectF.right - rectF.left) / 2.0f > mIvArrowTop.getMeasuredWidth() / 2.0f) {
                                mLlArrowTop.setPadding((int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mIvArrowTop.getMeasuredWidth() / 2.0f), 0, 0, 0);
                            } else {
                                mLlArrowTop.setPadding(0, 0, 0, 0);
                            }
                        }
                    }

                    showAtLocation(parent, Gravity.LEFT | Gravity.TOP, toLeft, (int) rectF.bottom);
                } else if (arrowPosition == ARROW_LEFT) {
                    mIvArrowLeft.measure(0, 0);

                    int toTop;
                    if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        if (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                            toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                            if (mArrowVisible) {
                                mLlArrowLeft.setPadding(0, (int) (mLl_root.getMeasuredHeight() / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f), 0, 0);
                            }
                        } else {
                            toTop = height + y - mLl_root.getMeasuredHeight();
                            if (mArrowVisible) {
                                if (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mIvArrowLeft.getMeasuredHeight() / 2.0f) {
                                    mLlArrowLeft.setPadding(0, 0, 0, (int) (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f));
                                } else {
                                    mLlArrowLeft.setPadding(0, mLl_root.getMeasuredHeight() - mIvArrowLeft.getMeasuredHeight(), 0, 0);
                                }
                            }
                        }
                    } else {
                        toTop = 0;
                        if (mArrowVisible) {
                            if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mIvArrowLeft.getMeasuredHeight() / 2.0f) {
                                mLlArrowLeft.setPadding(0, (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mIvArrowLeft.getMeasuredHeight() / 2.0f), 0, 0);
                            } else {
                                mLlArrowLeft.setPadding(0, 0, 0, 0);
                            }
                        }
                    }

                    showAtLocation(parent, Gravity.LEFT | Gravity.TOP, (int) (rectF.right), toTop);
                } else if (arrowPosition == ARROW_RIGHT) {
                    mIvArrowRight.measure(0, 0);

                    int toTop;
                    if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        if (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                            toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                            if (mArrowVisible) {
                                mLlArrowRight.setPadding(0, (int) (mLl_root.getMeasuredHeight() / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f), 0, 0);
                            }
                        } else {
                            toTop = height + y - mLl_root.getMeasuredHeight();
                            if (mArrowVisible) {
                                if (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mIvArrowRight.getMeasuredHeight() / 2.0f) {
                                    mLlArrowRight.setPadding(0, 0, 0, (int) (height + y - rectF.top - (rectF.bottom - rectF.top) / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f));
                                } else {
                                    mLlArrowRight.setPadding(0, mLl_root.getMeasuredHeight() - mIvArrowRight.getMeasuredHeight(), 0, 0);
                                }
                            }
                        }
                    } else {
                        toTop = 0;
                        if (mArrowVisible) {
                            if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mIvArrowRight.getMeasuredHeight() / 2.0f) {
                                mLlArrowRight.setPadding(0, (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mIvArrowRight.getMeasuredHeight() / 2.0f), 0, 0);
                            } else {
                                mLlArrowRight.setPadding(0, 0, 0, 0);
                            }
                        }
                    }

                    showAtLocation(parent, Gravity.LEFT | Gravity.TOP,
                            (int) (rectF.left - mPadWidth), toTop);
                } else if (arrowPosition == ARROW_CENTER) {
                    showAtLocation(parent, Gravity.LEFT | Gravity.TOP,
                            (int) (rectF.left + (rectF.right - rectF.left) / 4.0f), (int) (rectF.top + (rectF.bottom - rectF.top) / 4.0f));
                }

            } else {
                if (showMask) {
                    mTopShadow.setVisibility(View.GONE);
                } else {
                    mTopShadow.setVisibility(View.VISIBLE);
                }

                mArrowVisible = false;
                mLlArrowLeft.setVisibility(View.GONE);
                mLlArrowTop.setVisibility(View.GONE);
                mLlArrowRight.setVisibility(View.GONE);
                mLlArrowBottom.setVisibility(View.GONE);
                mLl_PropertyBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));

                setWidth(width);
                showAtLocation(parent, Gravity.LEFT | Gravity.BOTTOM, 0, 0);

                // move full screen to somewhere in selected annotation model.
                UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
                if (currentAnnotHandler != null && uiExtensionsManager.getCurrentToolHandler() == null) {

                    mLl_root.measure(w1, h1);
                    if (rectF.bottom > 0 && rectF.bottom <= height) {
                        if (rectF.bottom > height - mLl_root.getMeasuredHeight()) {
                            offset = mLl_root.getMeasuredHeight() - (height - rectF.bottom);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mPdfViewCtrl.layout(0, 0 - (int) offset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() - (int) offset);
                                }
                            }, 300);
                        }

                    } else if (rectF.top >= 0 && rectF.top <= height && rectF.bottom > height) {
                        if (rectF.top > height - mLl_root.getMeasuredHeight()) {
                            offset = mLl_root.getMeasuredHeight() - (height - rectF.top) + 10;

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mPdfViewCtrl.layout(0, 0 - (int) offset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() - (int) offset);
                                }
                            }, 300);
                        }
                    }
                }
            }

            mShowMask = showMask;
        }
    }

    @Override
    public void dismiss() {
        if (this != null && this.isShowing()) {
            setFocusable(false);
            hideSystemUI();
            super.dismiss();
        }
    }

    private void hideSystemUI(){
        MainFrame mainFrame = (MainFrame) ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getMainFrame();
        mainFrame.setHideSystemUI(true);
        if (!mainFrame.isToolbarsVisible()){
            AppUtil.hideSystemUI(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
        }
    }

    private void showSystemUI(){
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager();
        MainFrame mainFrame = (MainFrame)uiExtensionsManager.getMainFrame();
        if (mainFrame.isToolbarsVisible()){
            mainFrame.setHideSystemUI(false);
        } else {
            AppUtil.showSystemUI(uiExtensionsManager.getAttachedActivity());
        }
    }

    @Override
    public void setArrowVisible(boolean visible) {
        mArrowVisible = visible;
    }

    @Override
    public void setColors(int[] colors) {
        this.mColors = colors;
    }

    @Override
    public void setProperty(long property, int value) {
        if (property == PropertyBar.PROPERTY_COLOR) {
            mColor = value;
            int r = Color.red(mColor);
            int g = Color.green(mColor);
            int b = Color.blue(mColor);
            for (int i = 0; i < mColors.length; i++) {
                int r2 = Color.red(mColors[i]);
                int g2 = Color.green(mColors[i]);
                int b2 = Color.blue(mColors[i]);
                if (Math.abs(r2 - r) <= 3 && Math.abs(g2 - g) <= 3 && Math.abs(b2 - b) <= 3) {
                    mColor = mColors[i];
                    break;
                }
            }
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            mOpacity = value;
        } else if (property == PropertyBar.PROPERTY_ANNOT_TYPE) {
            mNoteIconType = value;
        } else if (property == PropertyBar.PROPERTY_SCALE_PERCENT) {
            mScalePercent = value;
        } else if (property == PropertyBar.PROPERTY_SCALE_SWITCH) {
            mScaleSwitch = value;
        } else if (property == PropertyBar.PROPERTY_ROTATION) {
            mRotation = value;
        }
    }

    @Override
    public void setProperty(long property, float value) {
        if (property == PropertyBar.PROPERTY_LINEWIDTH) {
            mLinewith = value;
        } else if (property == PropertyBar.PROPERTY_FONTSIZE) {
            mFontsize = value;
        }
    }

    @Override
    public void setProperty(long property, String value) {
        if (property == PropertyBar.PROPERTY_FONTNAME) {
            mFontname = value;
        } else if (property == PropertyBar.PROPERTY_OVERLAY_TEXT){
            mOverlayText = value;
        }
    }

    @Override
    public PropertyChangeListener getPropertyChangeListener() {
        return this.mPropertyChangeListener;
    }

    @Override
    public void setPropertyChangeListener(PropertyChangeListener listener) {
        this.mPropertyChangeListener = listener;
    }

    @Override
    public void setDismissListener(PropertyBar.DismissListener dismissListener) {
        this.mDismissListener = dismissListener;
    }

}
