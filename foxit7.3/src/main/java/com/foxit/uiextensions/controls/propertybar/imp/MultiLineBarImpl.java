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
package com.foxit.uiextensions.controls.propertybar.imp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;

import java.util.HashMap;
import java.util.Map;


public class MultiLineBarImpl extends ViewGroup implements IMultiLineBar {
    private Context mContext;
    private ViewGroup mRootView;
    private Config mConfig;
    private int mLight = 50;
    private boolean mDay = true;
    private boolean mSysLight = true;
    private int mPageModeFlag = PDFViewCtrl.PAGELAYOUTMODE_SINGLE;
    private boolean mLockScreen = false;
    private boolean mIsCrop = false;
    private int mZoomMode = PDFViewCtrl.ZOOMMODE_FITPAGE;
    private Map<Integer, IValueChangeListener> mListeners;
    private PopupWindow mPopupWindow;

    private View mLl_root;

    private TextView mTv_pagetip;
    private ImageView mIv_singlepage;
    private ImageView mIv_facingpage;
    private ImageView mIv_coverpage;
    private ImageView mIv_continuepage;

    private ImageView mIv_setfitpage;
    private ImageView mIv_setfitwidth;
    private TextView mTv_pageViewTip;

    private ImageView mIv_setlockscreen;

    private ImageView mIv_light_small;
    private ImageView mIv_light_big;
    private ImageView mIv_setreflow;
    private ImageView mIv_setcrop;
    private SeekBar mSb_light;
    private ImageView mIv_daynight;
    private ImageView mIv_syslight;
    private ImageView mIv_setThumbnail;
    private ImageView mIv_setpanzoom;
    private ImageView mIv_setrotateview;

    private ImageView mIv_setTTS;

    private Map<Integer, Integer> mIdsMap = new HashMap<Integer, Integer>();

    private PDFViewCtrl mPDFViewCtrl;

    public MultiLineBarImpl(Context context, PDFViewCtrl pdfViewCtrl) {
        this(context, null, 0);
        this.mContext = context;
        mListeners = new HashMap<Integer, IValueChangeListener>();
        mPDFViewCtrl = pdfViewCtrl;

        initMap();
    }

    private void initMap() {
        mIdsMap.put(IMultiLineBar.TYPE_SINGLEPAGE, R.id.ml_iv_singlepage);
        mIdsMap.put(IMultiLineBar.TYPE_CONTINUOUSPAGE, R.id.ml_ll_conpage);
        mIdsMap.put(IMultiLineBar.TYPE_THUMBNAIL, R.id.ml_ll_thumbnail);
        mIdsMap.put(IMultiLineBar.TYPE_SYSLIGHT, R.id.ml_ll_light);
        mIdsMap.put(IMultiLineBar.TYPE_DAYNIGHT, R.id.ml_ll_daynight);
        mIdsMap.put(IMultiLineBar.TYPE_REFLOW, R.id.ml_ll_reflow);
        mIdsMap.put(IMultiLineBar.TYPE_CROP, R.id.ml_ll_crop);
        mIdsMap.put(IMultiLineBar.TYPE_LOCKSCREEN, R.id.ml_ll_lockscreen);
        mIdsMap.put(IMultiLineBar.TYPE_FACINGPAGE, R.id.ml_iv_facingpage);
        mIdsMap.put(IMultiLineBar.TYPE_COVERPAGE, R.id.ml_iv_coverpage);
        mIdsMap.put(IMultiLineBar.TYPE_PANZOOM, R.id.ml_ll_panzoom);
        mIdsMap.put(IMultiLineBar.TYPE_FITPAGE, R.id.ml_iv_fitpage);
        mIdsMap.put(IMultiLineBar.TYPE_FITWIDTH, R.id.ml_iv_fitwidth);
        mIdsMap.put(IMultiLineBar.TYPE_ROTATEVIEW, R.id.ml_ll_rotateview);
        mIdsMap.put(IMultiLineBar.TYPE_TTS, R.id.ml_ll_tts);
    }

    public void init(ViewGroup viewGroup, Config config) {
        mRootView = viewGroup;
        mConfig = config;
        initView();
    }

    public MultiLineBarImpl(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiLineBarImpl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setProperty(int property, Object value) {
        if (property == IMultiLineBar.TYPE_LIGHT) {
            this.mLight = (Integer) value;
            mSb_light.setProgress(this.mLight);
        } else if (property == IMultiLineBar.TYPE_DAYNIGHT) {
            this.mDay = (Boolean) value;
            if (mDay) {
                mIv_daynight.setImageResource(R.drawable.setting_off);
            } else {
                mIv_daynight.setImageResource(R.drawable.setting_on);
            }
        } else if (property == IMultiLineBar.TYPE_SYSLIGHT) {
            this.mSysLight = (Boolean) value;
            if (mSysLight) {
                mIv_syslight.setImageResource(R.drawable.setting_on);
                Rect bounds = mSb_light.getProgressDrawable().getBounds();
                mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_unenable_bg));
                mSb_light.getProgressDrawable().setBounds(bounds);
                mSb_light.setEnabled(false);
                mIv_light_small.setImageResource(R.drawable.ml_light_small_pressed);
                mIv_light_big.setImageResource(R.drawable.ml_light_big_pressed);
            } else {
                mIv_syslight.setImageResource(R.drawable.setting_off);
                Rect bounds = mSb_light.getProgressDrawable().getBounds();
                mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_bg));
                mSb_light.getProgressDrawable().setBounds(bounds);
                mSb_light.setEnabled(true);
                if (mSb_light.getProgress() >= 1) {
                    mSb_light.setProgress(mSb_light.getProgress() - 1);
                    mSb_light.setProgress(mSb_light.getProgress() + 1);
                }

                mIv_light_small.setImageResource(R.drawable.ml_light_small);
                mIv_light_big.setImageResource(R.drawable.ml_light_big);

            }
        } else if (property == IMultiLineBar.TYPE_SINGLEPAGE) {
            this.mPageModeFlag = (Integer) value;
            mIv_continuepage.setSelected(mPDFViewCtrl.isContinuous());

            if (this.mPageModeFlag == PDFViewCtrl.PAGELAYOUTMODE_SINGLE) {
                mTv_pagetip.setText(mContext.getApplicationContext().getString(R.string.rv_page_present_single));
                mIv_singlepage.setSelected(true);
                mIv_facingpage.setSelected(false);
                mIv_coverpage.setSelected(false);
            } else if (this.mPageModeFlag == PDFViewCtrl.PAGELAYOUTMODE_FACING) {
                mTv_pagetip.setText(mContext.getApplicationContext().getString(R.string.rd_facingmode_topbar_title));
                mIv_singlepage.setSelected(false);
                mIv_facingpage.setSelected(true);
                mIv_coverpage.setSelected(false);
            } else if (this.mPageModeFlag == PDFViewCtrl.PAGELAYOUTMODE_COVER) {
                mTv_pagetip.setText(mContext.getApplicationContext().getString(R.string.rd_coverpage_topbar_title));
                mIv_singlepage.setSelected(false);
                mIv_facingpage.setSelected(false);
                mIv_coverpage.setSelected(true);
            }
        } else if (property == IMultiLineBar.TYPE_LOCKSCREEN) {
            this.mLockScreen = (Boolean) value;
            mIv_setlockscreen.setSelected(mLockScreen);
        } else if (property == IMultiLineBar.TYPE_CROP) {
            this.mIsCrop = (Boolean) value;
            if (mIsCrop) {
                mIv_setcrop.setSelected(true);
                mIv_setpanzoom.setEnabled(false);
            } else {
                mIv_setcrop.setSelected(false);
                mIv_setpanzoom.setEnabled(true);
            }
        } else if (property == IMultiLineBar.TYPE_FITPAGE) {
            this.mZoomMode = (int) value;
            if (mZoomMode == PDFViewCtrl.ZOOMMODE_FITPAGE) {
                mIv_setfitpage.setEnabled(false);
                mIv_setfitpage.setSelected(true);

                mTv_pageViewTip.setText(mContext.getApplicationContext().getString(R.string.rd_fitpage_topbar_title));
            } else {
                mIv_setfitpage.setEnabled(true);
                mIv_setfitpage.setSelected(false);

                mTv_pageViewTip.setText("");
            }
            mIv_setfitwidth.setEnabled(true);
            mIv_setfitwidth.setSelected(false);

        } else if (property == IMultiLineBar.TYPE_FITWIDTH) {
            this.mZoomMode = (int) value;
            if (mZoomMode == PDFViewCtrl.ZOOMMODE_FITWIDTH) {
                mIv_setfitwidth.setEnabled(false);
                mIv_setfitwidth.setSelected(true);

                mTv_pageViewTip.setText(mContext.getApplicationContext().getString(R.string.rd_fitwidth_topbar_title));
            } else {
                mIv_setfitwidth.setEnabled(true);
                mIv_setfitwidth.setSelected(false);

                mTv_pageViewTip.setText("");
            }
            mIv_setfitpage.setEnabled(true);
            mIv_setfitpage.setSelected(false);
        }
    }

    @Override
    public View getContentView() {
        return mLl_root;
    }

    @Override
    public void registerListener(IValueChangeListener listener) {
        int type = listener.getType();
        if (!mListeners.containsKey(type)) {
            this.mListeners.put(type, listener);
        }
    }

    @Override
    public void unRegisterListener(IValueChangeListener listener) {
        if (mListeners.containsKey(listener.getType())) {
            this.mListeners.remove(listener.getType());
        }
    }

    @Override
    public boolean isShowing() {
        if (mPopupWindow != null) {
            return mPopupWindow.isShowing();
        } else {
            return false;
        }
    }

    @Override
    public void show() {
        if (mPopupWindow != null && !isShowing()) {
            mPopupWindow.setFocusable(true);
            mPopupWindow.showAtLocation(mRootView, Gravity.BOTTOM, 0, 0);
        }
    }

    @Override
    public int getVisibility(int type) {
        Integer integer = mIdsMap.get(type);
        if (integer == null) {
            return -1;
        }

        int id = integer.intValue();
        if (id > 0) {
            return mLl_root.findViewById(id).getVisibility();
        }
        return -1;
    }

    @Override
    public void setVisibility(int type, int visibility) {
        Integer integer = mIdsMap.get(type);
        if (null == integer) return;
        if (visibility == INVISIBLE) visibility = GONE;

        int id = integer.intValue();
        if (id > 0) {
            if (id == R.id.ml_iv_thumbnail && !mConfig.modules.isLoadThumbnail()) {
                return;
            }

            mLl_root.findViewById(id).setVisibility(visibility);

            if (mLl_root.findViewById(R.id.ml_ll_light).getVisibility() == VISIBLE) {
                mLl_root.findViewById(R.id.ml_iv_syslight_divider).setVisibility(VISIBLE);
            } else {
                mLl_root.findViewById(R.id.ml_iv_syslight_divider).setVisibility(VISIBLE);
            }

            if (mLl_root.findViewById(R.id.ml_ll_daynight).getVisibility() == VISIBLE) {
                mLl_root.findViewById(R.id.ml_iv_light_divider).setVisibility(VISIBLE);
            } else {
                mLl_root.findViewById(R.id.ml_iv_light_divider).setVisibility(VISIBLE);
            }

            if (id == R.id.ml_iv_singlepage || id == R.id.ml_iv_facingpage || id == R.id.ml_iv_coverpage) {
                if (mLl_root.findViewById(R.id.ml_iv_singlepage).getVisibility() == VISIBLE ||
                        mLl_root.findViewById(R.id.ml_iv_facingpage).getVisibility() == VISIBLE ||
                        mLl_root.findViewById(R.id.ml_iv_coverpage).getVisibility() == VISIBLE) {
                    mLl_root.findViewById(R.id.ml_ll_page).setVisibility(VISIBLE);
                } else {
                    mLl_root.findViewById(R.id.ml_ll_page).setVisibility(GONE);
                }
            }

            if (mLl_root.findViewById(R.id.ml_ll_page).getVisibility() == VISIBLE ||
                    mLl_root.findViewById(R.id.ml_ll_conpage).getVisibility() == VISIBLE) {
                mLl_root.findViewById(R.id.ml_iv_page_divider).setVisibility(VISIBLE);
            } else {
                mLl_root.findViewById(R.id.ml_iv_page_divider).setVisibility(GONE);
            }

            if (id == R.id.ml_iv_fitpage || id == R.id.ml_iv_fitwidth) {
                if (mLl_root.findViewById(R.id.ml_iv_fitpage).getVisibility() == VISIBLE ||
                        mLl_root.findViewById(R.id.ml_iv_fitwidth).getVisibility() == VISIBLE) {
                    mLl_root.findViewById(R.id.ml_ll_zoommode).setVisibility(VISIBLE);
                    mLl_root.findViewById(R.id.ml_iv_zoommode_divider).setVisibility(VISIBLE);
                } else {
                    mLl_root.findViewById(R.id.ml_ll_zoommode).setVisibility(GONE);
                    mLl_root.findViewById(R.id.ml_iv_zoommode_divider).setVisibility(GONE);
                }
            }
        }
    }

    @Override
    public void dismiss() {
        if (mPopupWindow != null && isShowing()) {
            mPopupWindow.setFocusable(false);
            mPopupWindow.dismiss();
        }
    }

    OnClickListener mPhoneClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();

            if (id == R.id.ml_iv_singlepage) {
                if (mListeners.get(IMultiLineBar.TYPE_SINGLEPAGE) != null) {
                    if (mPageModeFlag != PDFViewCtrl.PAGELAYOUTMODE_SINGLE){
                        mPageModeFlag = PDFViewCtrl.PAGELAYOUTMODE_SINGLE;
                        mListeners.get(IMultiLineBar.TYPE_SINGLEPAGE).onValueChanged(IMultiLineBar.TYPE_SINGLEPAGE, mPageModeFlag);
                    }
                    mIv_singlepage.setSelected(true);
                    mIv_facingpage.setSelected(false);
                    mIv_coverpage.setSelected(false);
                }
            } else if (id == R.id.ml_iv_facingpage) {
                if (mListeners.get(IMultiLineBar.TYPE_SINGLEPAGE) != null) {
                    if (mPageModeFlag != PDFViewCtrl.PAGELAYOUTMODE_FACING){
                        mPageModeFlag = PDFViewCtrl.PAGELAYOUTMODE_FACING;
                        mListeners.get(IMultiLineBar.TYPE_SINGLEPAGE).onValueChanged(IMultiLineBar.TYPE_SINGLEPAGE, mPageModeFlag);
                    }
                    mIv_singlepage.setSelected(false);
                    mIv_facingpage.setSelected(true);
                    mIv_coverpage.setSelected(false);
                }
            } else if (id == R.id.ml_iv_coverpage) {
                if (mListeners.get(IMultiLineBar.TYPE_SINGLEPAGE) != null) {
                    if (mPageModeFlag != PDFViewCtrl.PAGELAYOUTMODE_COVER){
                        mPageModeFlag = PDFViewCtrl.PAGELAYOUTMODE_COVER;
                        mListeners.get(IMultiLineBar.TYPE_SINGLEPAGE).onValueChanged(IMultiLineBar.TYPE_SINGLEPAGE, mPageModeFlag);
                    }
                    mIv_singlepage.setSelected(false);
                    mIv_facingpage.setSelected(false);
                    mIv_coverpage.setSelected(true);
                }
            } else if (id == R.id.ml_iv_continuepage) {
                if (mListeners.get(IMultiLineBar.TYPE_SINGLEPAGE) != null) {
                    if (mIv_continuepage.isSelected()){
                        mIv_continuepage.setSelected(false);
                        mPDFViewCtrl.setContinuous(false);
                    } else {
                        mIv_continuepage.setSelected(true);
                        mPDFViewCtrl.setContinuous(true);
                    }
                    mPageModeFlag = mPDFViewCtrl.getPageLayoutMode();
                    mListeners.get(IMultiLineBar.TYPE_SINGLEPAGE).onValueChanged(IMultiLineBar.TYPE_SINGLEPAGE, mPageModeFlag);
                }
            } else if (id == R.id.ml_iv_thumbnail) {
                if (mListeners.get(IMultiLineBar.TYPE_THUMBNAIL) != null) {
                    mListeners.get(IMultiLineBar.TYPE_THUMBNAIL).onValueChanged(IMultiLineBar.TYPE_THUMBNAIL, 0);
                }
            } else if (id == R.id.ml_iv_lockscreen) {
                if (mListeners.get(IMultiLineBar.TYPE_LOCKSCREEN) != null) {
                    mListeners.get(IMultiLineBar.TYPE_LOCKSCREEN).onValueChanged(IMultiLineBar.TYPE_LOCKSCREEN, mLockScreen);
                    mIv_setlockscreen.setSelected(mLockScreen);
                }
            } else if (id == R.id.ml_iv_daynight) {
                if (mListeners.get(IMultiLineBar.TYPE_DAYNIGHT) != null) {
                    mDay = !mDay;
                    mListeners.get(IMultiLineBar.TYPE_DAYNIGHT).onValueChanged(IMultiLineBar.TYPE_DAYNIGHT, mDay);
                    ImageView imageView = (ImageView) v;
                    if (mDay) {
                        imageView.setImageResource(R.drawable.setting_off);
                    } else {
                        imageView.setImageResource(R.drawable.setting_on);
                    }
                }
            } else if (id == R.id.ml_iv_syslight) {
                if (mListeners.get(IMultiLineBar.TYPE_SYSLIGHT) != null) {
                    mSysLight = !mSysLight;
                    mListeners.get(IMultiLineBar.TYPE_SYSLIGHT).onValueChanged(IMultiLineBar.TYPE_SYSLIGHT, mSysLight);
                    if (mSysLight) {
                        ((ImageView) v).setImageResource(R.drawable.setting_on);
                        Rect bounds = mSb_light.getProgressDrawable().getBounds();
                        mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_unenable_bg));
                        mSb_light.getProgressDrawable().setBounds(bounds);
                        mSb_light.setEnabled(false);
                        mIv_light_small.setImageResource(R.drawable.ml_light_small_pressed);
                        mIv_light_big.setImageResource(R.drawable.ml_light_big_pressed);
                    } else {
                        ((ImageView) v).setImageResource(R.drawable.setting_off);
                        Rect bounds = mSb_light.getProgressDrawable().getBounds();
                        mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_bg));
                        mSb_light.getProgressDrawable().setBounds(bounds);
                        mSb_light.setEnabled(true);
                        if (mSb_light.getProgress() >= 1) {
                            mSb_light.setProgress(mSb_light.getProgress() - 1);
                            mSb_light.setProgress(mSb_light.getProgress() + 1);
                        }

                        mIv_light_small.setImageResource(R.drawable.ml_light_small);
                        mIv_light_big.setImageResource(R.drawable.ml_light_big);
                    }
                }
            } else if (id == R.id.ml_iv_reflow) {
                if (mListeners.get(IMultiLineBar.TYPE_REFLOW) != null) {
                    mListeners.get(IMultiLineBar.TYPE_REFLOW).onValueChanged(IMultiLineBar.TYPE_REFLOW, true);
                }
            } else if (id == R.id.ml_iv_crop) {
                if (mListeners.get(IMultiLineBar.TYPE_CROP) != null) {
                    mListeners.get(IMultiLineBar.TYPE_CROP).onValueChanged(IMultiLineBar.TYPE_CROP, true);
                }
            } else if (id == R.id.ml_iv_panzoom) {
                if (mListeners.get(IMultiLineBar.TYPE_PANZOOM) != null) {
                    mListeners.get(IMultiLineBar.TYPE_PANZOOM).onValueChanged(IMultiLineBar.TYPE_PANZOOM, true);
                }
            } else if (id == R.id.ml_iv_fitpage) {
                if (mListeners.get(IMultiLineBar.TYPE_FITPAGE) != null && mZoomMode != PDFViewCtrl.ZOOMMODE_FITPAGE) {
                    mZoomMode = PDFViewCtrl.ZOOMMODE_FITPAGE;
                    mListeners.get(IMultiLineBar.TYPE_FITPAGE).onValueChanged(IMultiLineBar.TYPE_FITPAGE, mZoomMode);
                }
            } else if (id == R.id.ml_iv_fitwidth) {
                if (mListeners.get(IMultiLineBar.TYPE_FITWIDTH) != null && mZoomMode != PDFViewCtrl.ZOOMMODE_FITWIDTH) {
                    mZoomMode = PDFViewCtrl.ZOOMMODE_FITWIDTH;
                    mListeners.get(IMultiLineBar.TYPE_FITWIDTH).onValueChanged(IMultiLineBar.TYPE_FITWIDTH, mZoomMode);
                }
            } else if (id == R.id.ml_iv_rotateview) {
                if (mListeners.get(IMultiLineBar.TYPE_ROTATEVIEW) != null) {
                    mListeners.get(IMultiLineBar.TYPE_ROTATEVIEW).onValueChanged(IMultiLineBar.TYPE_ROTATEVIEW, true);
                }
            } else if (id == R.id.ml_iv_tts) {
                if (mListeners.get(IMultiLineBar.TYPE_TTS) != null) {
                    mListeners.get(IMultiLineBar.TYPE_TTS).onValueChanged(IMultiLineBar.TYPE_TTS, true);
                }
            }
        }
    };

    @SuppressLint("NewApi")
    private void initView() {
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setBackgroundColor(Color.WHITE);
        mLl_root = LayoutInflater.from(mContext).inflate(R.layout.ml_setbar, null, false);
        mLl_root.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(mLl_root);

        mTv_pagetip = mLl_root.findViewById(R.id.ml_iv_page_tip);

        mIv_singlepage = (ImageView) mLl_root.findViewById(R.id.ml_iv_singlepage);
        mIv_singlepage.setOnClickListener(mPhoneClickListener);

        mIv_continuepage = (ImageView) mLl_root.findViewById(R.id.ml_iv_continuepage);
        mIv_continuepage.setOnClickListener(mPhoneClickListener);
        mIv_continuepage.setSelected(false);

        mIv_facingpage = (ImageView) mLl_root.findViewById(R.id.ml_iv_facingpage);
        mIv_facingpage.setOnClickListener(mPhoneClickListener);

        mIv_coverpage = (ImageView) mLl_root.findViewById(R.id.ml_iv_coverpage);
        mIv_coverpage.setOnClickListener(mPhoneClickListener);


        mTv_pageViewTip = mLl_root.findViewById(R.id.ml_iv_zoommode_tip);

        mIv_setfitpage = (ImageView) mLl_root.findViewById(R.id.ml_iv_fitpage);
        mIv_setfitpage.setOnClickListener(mPhoneClickListener);

        mIv_setfitwidth = (ImageView) mLl_root.findViewById(R.id.ml_iv_fitwidth);
        mIv_setfitwidth.setOnClickListener(mPhoneClickListener);

        if (mConfig.modules.isLoadThumbnail()) {
            mIv_setThumbnail = (ImageView) mLl_root.findViewById(R.id.ml_iv_thumbnail);
            mIv_setThumbnail.setOnClickListener(mPhoneClickListener);
        } else {
            mLl_root.findViewById(R.id.ml_ll_thumbnail).setVisibility(GONE);
        }

        mIv_setrotateview = (ImageView) mLl_root.findViewById(R.id.ml_iv_rotateview);
        mIv_setrotateview.setOnClickListener(mPhoneClickListener);

        mIv_setreflow = (ImageView) mLl_root.findViewById(R.id.ml_iv_reflow);
        mIv_setreflow.setOnClickListener(mPhoneClickListener);

        mIv_setcrop = (ImageView) mLl_root.findViewById(R.id.ml_iv_crop);
        mIv_setcrop.setOnClickListener(mPhoneClickListener);

        mIv_setlockscreen = (ImageView) mLl_root.findViewById(R.id.ml_iv_lockscreen);
        mIv_setlockscreen.setOnClickListener(mPhoneClickListener);

        mIv_setpanzoom = (ImageView) mLl_root.findViewById(R.id.ml_iv_panzoom);
        mIv_setpanzoom.setOnClickListener(mPhoneClickListener);

        mIv_setTTS = (ImageView) mLl_root.findViewById(R.id.ml_iv_tts);
        mIv_setTTS.setOnClickListener(mPhoneClickListener);

        mIv_daynight = (ImageView) mLl_root.findViewById(R.id.ml_iv_daynight);
        mIv_syslight = (ImageView) mLl_root.findViewById(R.id.ml_iv_syslight);
        mSb_light = (SeekBar) mLl_root.findViewById(R.id.ml_sb_light);
        mIv_light_small = (ImageView) mLl_root.findViewById(R.id.ml_iv_light_small);
        mIv_light_big = (ImageView) mLl_root.findViewById(R.id.ml_iv_light_big);

        mIv_daynight.setOnClickListener(mPhoneClickListener);
        mIv_syslight.setOnClickListener(mPhoneClickListener);

        if (mDay) {
            mIv_daynight.setImageResource(R.drawable.setting_off);
        } else {
            mIv_daynight.setImageResource(R.drawable.setting_on);
        }
        if (mSysLight) {
            mIv_syslight.setImageResource(R.drawable.setting_on);
            Rect bounds = mSb_light.getProgressDrawable().getBounds();
            mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_unenable_bg));
            mSb_light.getProgressDrawable().setBounds(bounds);
            mSb_light.setEnabled(false);
            mIv_light_small.setImageResource(R.drawable.ml_light_small_pressed);
            mIv_light_big.setImageResource(R.drawable.ml_light_big_pressed);
        } else {
            mIv_syslight.setImageResource(R.drawable.setting_off);
            Rect bounds = mSb_light.getProgressDrawable().getBounds();
            mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_bg));
            mSb_light.getProgressDrawable().setBounds(bounds);
            mSb_light.setEnabled(true);
            if (mSb_light.getProgress() >= 1) {
                mSb_light.setProgress(mSb_light.getProgress() - 1);
                mSb_light.setProgress(mSb_light.getProgress() + 1);
            }

            mIv_light_small.setImageResource(R.drawable.ml_light_small);
            mIv_light_big.setImageResource(R.drawable.ml_light_big);
        }
        mSb_light.setProgress(mLight);
        mSb_light.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mLight = progress;
                if (mListeners.get(IMultiLineBar.TYPE_LIGHT) != null) {
                    mListeners.get(IMultiLineBar.TYPE_LIGHT).onValueChanged(IMultiLineBar.TYPE_LIGHT, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (mPopupWindow == null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int heightPixels = displayMetrics.heightPixels;
            if (heightPixels < 480) {
                mPopupWindow = new PopupWindow(this, LayoutParams.MATCH_PARENT, heightPixels);
            } else {
                mPopupWindow = new PopupWindow(this, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            }

            mPopupWindow.setTouchable(true);
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    mListeners.get(IMultiLineBar.TYPE_LIGHT).onDismiss();
                }
            });
        } else {
            mPopupWindow.setContentView(this);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int measureWidth = 0;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        switch (widthMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                measureWidth = widthSize;
                break;
            default:
                break;
        }

        int measureHeight = 0;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int childCount = getChildCount();
        int totalHeight = 0;
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            int childHeight = childView.getMeasuredHeight();
            totalHeight += childHeight;
        }
        switch (heightMode) {
            case MeasureSpec.AT_MOST:
                measureHeight = totalHeight;
                break;
            case MeasureSpec.EXACTLY:
                measureHeight = heightSize;
                break;
            default:
                break;
        }

        setMeasuredDimension(measureWidth, measureHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int mTotalHeight = 0;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            int measureHeight = childView.getMeasuredHeight();
            int measuredWidth = childView.getMeasuredWidth();

            childView.layout(l, mTotalHeight, l + measuredWidth, mTotalHeight + measureHeight);
            mTotalHeight += measureHeight;
        }
    }

    @Override
    public void enableBar(int property, boolean enable) {
        switch (property) {
            case TYPE_REFLOW:
                mIv_setreflow.setEnabled(enable);
                break;
            case TYPE_CROP:
                mIv_setcrop.setEnabled(enable);
                break;
            case TYPE_PANZOOM:
                mIv_setpanzoom.setEnabled(enable);
                break;
            case TYPE_FITPAGE:
                mIv_setfitpage.setEnabled(enable);
                mIv_setfitpage.setSelected(!enable);
                break;
            case TYPE_FITWIDTH:
                mIv_setfitwidth.setEnabled(enable);
                mIv_setfitwidth.setSelected(!enable);
                break;
            case TYPE_ROTATEVIEW:
                mIv_setrotateview.setEnabled(enable);
                break;
            case TYPE_TTS:
                mIv_setTTS.setEnabled(enable);
                break;
            default:
                break;
        }
    }
}