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
package com.foxit.uiextensions.modules;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.fxcrt.RectFArray;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.SearchCancelCallback;
import com.foxit.sdk.pdf.TextSearch;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchView {
    private static final int HIDE_SOFT_INPUT = 111;
    private static final int SHOW_SETTINGS_POPUP = 222;

    private Context mContext = null;
    private ViewGroup mParent = null;
    private PDFViewCtrl mPdfViewCtrl = null;

    private View mSearchView = null;

    private boolean mIsBlank = true;
    private String mSearch_content;

    private LinearLayout mRd_search_ll_top;
    private EditText mTop_et_content;
    private ImageView mTop_iv_clear;
    private ImageView mIvSearchSettings;
    private Button mTop_bt_cancel;

    private LinearLayout mRd_search_ll_center;
    private View mViewCenterLeft;
    private LinearLayout mViewCenterRight;
    private TextView mCenter_tv_total_number;
    private ListView mCenter_lv_result_list;

    private LinearLayout mRd_search_ll_bottom;
    private ImageView mBottom_iv_prev;
    private ImageView mBottom_iv_next;
    private ImageView mBottom_iv_result;
    private LinearLayout mBottom_ll_shadow;

    protected List<RectF> mRect = new ArrayList<RectF>();
    protected int mPageIndex = -1;//The page index of the search result
    protected boolean mIsCancel = true;
    private DisplayMetrics mMetrics;
    private LayoutInflater mInflater;
    private SearchAdapter mAdapterSearch;
    private AppDisplay mAppDisplay;
    private long mSearchId = 0;

    private PopupWindow mSearchSettingsPopup;
    private CheckBox mCheckBoxMatchWholeWords;
    private CheckBox mCheckBoxMatchCase;
    private int mSearchFlags = TextSearch.e_SearchNormal;
    private ImageView mIv_left_search;

    protected SearchView(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {

        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;

        mAppDisplay = AppDisplay.getInstance(context);
        mMetrics = context.getResources().getDisplayMetrics();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAdapterSearch = new SearchAdapter();

        this.mSearchView = LayoutInflater.from(context).inflate(R.layout.search_layout, null, false);
        mSearchView.setVisibility(View.GONE);

        mParent = parent;
        mParent.addView(mSearchView);

        initView();
        bindEvent();
    }

    private void initView() {
        mRd_search_ll_top = (LinearLayout) mSearchView.findViewById(R.id.rd_search_ll_top);
        mTop_et_content = (EditText) mSearchView.findViewById(R.id.top_et_content);
        mTop_iv_clear = (ImageView) mSearchView.findViewById(R.id.top_iv_clear);

        mTop_iv_clear = (ImageView) mSearchView.findViewById(R.id.top_iv_clear);
        mTop_bt_cancel = (Button) mSearchView.findViewById(R.id.top_bt_cancel);
        mIvSearchSettings = (ImageView) mSearchView.findViewById(R.id.top_search_settings);

        mRd_search_ll_center = (LinearLayout) mSearchView.findViewById(R.id.rd_search_ll_center);
        mViewCenterLeft = mSearchView.findViewById(R.id.rd_search_center_left);
        mViewCenterRight = (LinearLayout) mSearchView.findViewById(R.id.rd_search_center_right);
        mCenter_tv_total_number = (TextView) mSearchView.findViewById(R.id.center_tv_total_number);
        mCenter_lv_result_list = (ListView) mSearchView.findViewById(R.id.center_lv_result_list);

        mRd_search_ll_bottom = (LinearLayout) mSearchView.findViewById(R.id.rd_search_ll_bottom);
        mBottom_iv_prev = (ImageView) mSearchView.findViewById(R.id.bottom_iv_prev);
        mBottom_iv_next = (ImageView) mSearchView.findViewById(R.id.bottom_iv_next);
        mBottom_iv_result = (ImageView) mSearchView.findViewById(R.id.bottom_iv_result);
        mBottom_ll_shadow = (LinearLayout) mSearchView.findViewById(R.id.bottom_ll_shadow);

        mIv_left_search = (ImageView) mSearchView.findViewById(R.id.iv_left_search);

        RelativeLayout.LayoutParams topParams = (RelativeLayout.LayoutParams) mRd_search_ll_top.getLayoutParams();
        RelativeLayout.LayoutParams bottomParams = (RelativeLayout.LayoutParams) mRd_search_ll_bottom.getLayoutParams();
        if (mAppDisplay.isPad()) {
            topParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            bottomParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
        } else {
            topParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
            bottomParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
        }
        mRd_search_ll_top.setLayoutParams(topParams);
        mRd_search_ll_bottom.setLayoutParams(bottomParams);

        mRd_search_ll_center.setVisibility(View.VISIBLE);
        mRd_search_ll_center.setBackgroundResource(R.color.ux_color_translucent);
        mViewCenterLeft.setVisibility(View.GONE);
        mViewCenterRight.setVisibility(View.GONE);
        mRd_search_ll_bottom.setVisibility(View.GONE);
        mBottom_ll_shadow.setVisibility(View.GONE);

        setSearchResultWidth();
    }

    private PopupWindow getSettingsPopup(){
        View settingsView = mInflater.inflate(R.layout.search_settings, null);
        mSearchSettingsPopup = new PopupWindow(settingsView,LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        mSearchSettingsPopup.setOutsideTouchable(true);
        mSearchSettingsPopup.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.settings_popup_bg));
        mSearchSettingsPopup.setAnimationStyle(R.style.View_Animation_TtoB);

        RelativeLayout relaMatchWholeWords = settingsView.findViewById(R.id.rela_search_whole_words);
        RelativeLayout relaMatchCase = settingsView.findViewById(R.id.rela_search_case_sensitive);
        mCheckBoxMatchWholeWords = settingsView.findViewById(R.id.cb_search_whole_words);
        mCheckBoxMatchCase = settingsView.findViewById(R.id.cb_search_case_sensitive);
        TextView tvSearchInInternet = settingsView.findViewById(R.id.tv_search_in_internet);

        relaMatchWholeWords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCheckBoxMatchWholeWords.isChecked()) {
                    mCheckBoxMatchWholeWords.setChecked(false);
                    mSearchFlags = mSearchFlags & (~TextSearch.e_SearchMatchWholeWord);
                } else {
                    mCheckBoxMatchWholeWords.setChecked(true);
                    mSearchFlags = mSearchFlags | TextSearch.e_SearchMatchWholeWord;
                }

                if (!TextUtils.isEmpty(mTop_et_content.getText().toString()))
                    startSearch();
                mSearchSettingsPopup.dismiss();
            }
        });

        relaMatchCase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCheckBoxMatchCase.isChecked()) {
                    mCheckBoxMatchCase.setChecked(false);
                    mSearchFlags = mSearchFlags & (~TextSearch.e_SearchMatchCase);
                } else {
                    mCheckBoxMatchCase.setChecked(true);
                    mSearchFlags = mSearchFlags | TextSearch.e_SearchMatchCase;
                }

                if (!TextUtils.isEmpty(mTop_et_content.getText().toString()))
                    startSearch();
                mSearchSettingsPopup.dismiss();
            }
        });

        tvSearchInInternet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchContent = mTop_et_content.getText().toString();
                if (!TextUtils.isEmpty(searchContent)){
                    UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
                    Activity activity = uiExtensionsManager.getAttachedActivity();
                    if (null != activity){
                        Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
                        search.putExtra(SearchManager.QUERY, searchContent);
                        activity.startActivity(search);
                    }
                }
                mSearchSettingsPopup.dismiss();
            }
        });

        mIv_left_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearch();
            }
        });
        return mSearchSettingsPopup;
    }

    protected View getView() {
        return  mSearchView;
    }

    private void bindEvent() {
        AppUtil.dismissInputSoft(mTop_et_content);

        mTop_et_content.addTextChangedListener(new myTextWatcher());
        mTop_et_content.setOnKeyListener(mySearchListener);
        mTop_et_content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).triggerDismissMenuEvent();
            }
        });

        mIvSearchSettings.setOnClickListener(searchModelListener);
        mTop_iv_clear.setOnClickListener(searchModelListener);
        mTop_bt_cancel.setOnClickListener(searchModelListener);
        mRd_search_ll_top.setOnClickListener(searchModelListener);

        mViewCenterLeft.setOnClickListener(searchModelListener);
        mViewCenterRight.setOnClickListener(searchModelListener);

        mBottom_iv_result.setOnClickListener(searchModelListener);
        mBottom_iv_prev.setOnClickListener(searchModelListener);
        mBottom_iv_next.setOnClickListener(searchModelListener);
        mBottom_iv_prev.setEnabled(false);
        mBottom_iv_next.setEnabled(false);

        mRd_search_ll_bottom.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mCenter_lv_result_list.setAdapter(mAdapterSearch);
        mCenter_lv_result_list.setOnItemClickListener(mOnItemClickListener);
    }

    protected void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight){
        if (mSearchSettingsPopup != null && mSearchSettingsPopup.isShowing()){
            if (newWidth != oldWidth || newHeight != oldHeight){
                mSearchSettingsPopup.dismiss();
                mHandler.sendEmptyMessage(SHOW_SETTINGS_POPUP);
            }
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case HIDE_SOFT_INPUT:
                    AppKeyboardUtil.hideInputMethodWindow(mContext, mTop_et_content);
                    break;
                case SHOW_SETTINGS_POPUP:
                    mCheckBoxMatchWholeWords.setChecked((mSearchFlags & TextSearch.e_SearchMatchWholeWord) == TextSearch.e_SearchMatchWholeWord);
                    mCheckBoxMatchCase.setChecked((mSearchFlags & TextSearch.e_SearchMatchCase) == TextSearch.e_SearchMatchCase);
                    mSearchSettingsPopup.showAsDropDown(mRd_search_ll_top, mIvSearchSettings.getLeft(),0);
                    break;
                default:
                    break;
            }
        }
    };

    protected void setToolbarIcon() {
        mBottom_iv_prev.setImageDrawable(mSearchView.getResources().getDrawable(R.drawable.search_previous));
        mBottom_iv_next.setImageDrawable(mSearchView.getResources().getDrawable(R.drawable.search_next));
        mBottom_iv_prev.setEnabled(true);
        mBottom_iv_next.setEnabled(true);

        if (isFirstSearchResult()) {
            mBottom_iv_prev.setImageDrawable(mSearchView.getResources().getDrawable(R.drawable.search_previous_pressed));
            mBottom_iv_prev.setEnabled(false);
        }

        if (isLastSearchResult()) {
            mBottom_iv_next.setImageDrawable(mSearchView.getResources().getDrawable(R.drawable.search_next_pressed));
            mBottom_iv_next.setEnabled(false);

        }
    }

    private void setSearchNumber(int count) {
        mCenter_tv_total_number.setText(mContext.getApplicationContext().getString(R.string.searching_find_number,count));
    }

    private void setTotalNumber(int count) {
        mCenter_tv_total_number.setText(mContext.getApplicationContext().getString(R.string.search_find_number,count));
    }

    private void setSearchResultWidth() {
        LinearLayout.LayoutParams leftParams = (LinearLayout.LayoutParams) mViewCenterLeft.getLayoutParams();
        LinearLayout.LayoutParams rightParams = (LinearLayout.LayoutParams) mViewCenterRight.getLayoutParams();
        if (mAppDisplay.isPad()) {
            if (mAppDisplay.isLandscape()) {
                leftParams.width = 0;
                leftParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                leftParams.weight = 2.0f;

                rightParams.width = 0;
                rightParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                rightParams.weight = 1.0f;

            } else {
                leftParams.width = 0;
                leftParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                leftParams.weight = 1.0f;

                rightParams.width = 0;
                rightParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                rightParams.weight = 1.0f;

            }
        } else {
                leftParams.width = 0;
                leftParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                leftParams.weight = 1.0f;

                rightParams.width = 0;
                rightParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                rightParams.weight = 4.0f;
        }
        mViewCenterLeft.setLayoutParams(leftParams);
        mViewCenterRight.setLayoutParams(rightParams);
    }

    public void show() {
        if (mSearchView != null) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).changeState(ReadStateConfig.STATE_SEARCH);
            mSearchView.setVisibility(View.VISIBLE);
        }
    }

    public void dismiss() {
        if (mSearchView != null) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).changeState(ReadStateConfig.STATE_NORMAL);
            mSearchView.setVisibility(View.GONE);
        }
    }

    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (isFastDoubleClick()) {
                return;
            }

            onPreItemClick();

            if (mTagResultList.contains(mShowResultList.get(position))) {
                mCurrentPosition = position + 1;
                setCurrentPageX();
                RectF rectF = new RectF(mCurrentPageX, mCurrentPageY, mCurrentSearchR, mCurrentSearchB);
                RectF canvasRectF = new RectF();
                boolean transSuccess = mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, canvasRectF, mShowResultList.get(mCurrentPosition).mPageIndex);
                int screenWidth = getVisibleWidth().width();
                int screenHeight = getVisibleWidth().height();
                if (!transSuccess || canvasRectF.left < 0 || canvasRectF.right > screenWidth || canvasRectF.top < 0 || canvasRectF.bottom > screenHeight) {
                    int x = (int) (mCurrentPageX - getScreenWidth() / 4.0f);
                    int y = (int) (mCurrentPageY - getScreenHeight() / 4.0f);
                    mPdfViewCtrl.gotoPage(mShowResultList.get(mCurrentPosition).mPageIndex, x, y);
                }
                mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
                mRect = mShowResultList.get(mCurrentPosition).mRects;
                setToolbarIcon();
                mPdfViewCtrl.invalidate();
            } else {
                mCurrentPosition = position;
                setCurrentPageX();
                RectF rectF = new RectF(mCurrentPageX, mCurrentPageY, mCurrentSearchR, mCurrentSearchB);
                RectF canvasRectF = new RectF();
                boolean transSuccess = mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, canvasRectF, mShowResultList.get(mCurrentPosition).mPageIndex);
                int screenWidth = getVisibleWidth().width();
                int screenHeight = getVisibleWidth().height();
                if (!transSuccess || canvasRectF.left < 0 || canvasRectF.right > screenWidth || canvasRectF.top < 0 || canvasRectF.bottom > screenHeight) {
                    int x = (int) (mCurrentPageX - getScreenWidth() / 4.0f);
                    int y = (int) (mCurrentPageY - getScreenHeight() / 4.0f);
                    mPdfViewCtrl.gotoPage(mShowResultList.get(mCurrentPosition).mPageIndex, x, y);
                }
                mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
                mRect = mShowResultList.get(mCurrentPosition).mRects;
                setToolbarIcon();
                mPdfViewCtrl.invalidate();
            }
        }
    };

    private void onPreItemClick() {
        AppUtil.dismissInputSoft(mTop_et_content);
        Animation animationL2R = AnimationUtils.loadAnimation(mContext, R.anim.view_anim_rtol_hide);
        animationL2R.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mViewCenterRight.setVisibility(View.GONE);
                mViewCenterLeft.setVisibility(View.GONE);
                mRd_search_ll_center.setBackgroundResource(R.color.ux_color_translucent);
                mRd_search_ll_bottom.setVisibility(View.VISIBLE);
//                mBottom_ll_shadow.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animationL2R.setStartOffset(0);
        mViewCenterRight.startAnimation(animationL2R);
    }

    //search content changed "before" "on" and "after"
    private class myTextWatcher implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                mTop_iv_clear.setVisibility(View.VISIBLE);
                mIsBlank = false;
            } else {
                mTop_iv_clear.setVisibility(View.INVISIBLE);
                mIsBlank = true;
            }
        }
    }

    private SearchCancelListener mSearchCancelListener = null;

    public void setSearchCancelListener(SearchCancelListener listener) {
        mSearchCancelListener = listener;
    }

    public interface SearchCancelListener {
        void onSearchCancel();
    }

    private void startSearch(){
        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).triggerDismissMenuEvent();
        if (!mIsBlank) {
            mSearch_content = mTop_et_content.getText().toString();
            AppUtil.dismissInputSoft(mTop_et_content);
            if (mSearch_content != null && !"".equals(mSearch_content.trim())) {
                if (mRd_search_ll_bottom.getVisibility() == View.VISIBLE) {
                    mRd_search_ll_bottom.setVisibility(View.GONE);
                    mBottom_ll_shadow.setVisibility(View.GONE);
                }
                if (mViewCenterRight.getVisibility() == View.VISIBLE) {
                    mIsCancel = false;
                    searchText(mSearch_content, mSearchFlags);
                } else {
                    mRd_search_ll_center.setBackgroundResource(R.color.ux_color_mask_background);
                    mViewCenterLeft.setVisibility(View.VISIBLE);
                    mViewCenterLeft.setClickable(false);
                    mViewCenterRight.setVisibility(View.VISIBLE);
                    Animation animationR2L = AnimationUtils.loadAnimation(mContext, R.anim.view_anim_rtol_show);
                    animationR2L.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mViewCenterLeft.setClickable(true);
                            mIsCancel = false;
                            searchText(mSearch_content, mSearchFlags);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    animationR2L.setStartOffset(300);
                    mViewCenterRight.startAnimation(animationR2L);
                }
            }
        }
    }

    private View.OnKeyListener mySearchListener = new View.OnKeyListener() {

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (KeyEvent.KEYCODE_ENTER == keyCode && event.getAction() == KeyEvent.ACTION_DOWN) {
                startSearch();
                return true;
            }
            return false;
        }
    };

    protected void onKeyBack() {
        mRd_search_ll_center.setBackgroundResource(R.color.ux_color_translucent);
        mViewCenterLeft.setVisibility(View.GONE);
        mViewCenterRight.setVisibility(View.GONE);
        cancel();
    }

    protected void cancel() {
        mIsCancel = true;
        mIsBlank = true;
        searchCancel();

        mSearchView.setVisibility(View.INVISIBLE);
        cancelSearchText();
        AppUtil.dismissInputSoft(mTop_et_content);
        if (mSearchCancelListener != null)
            mSearchCancelListener.onSearchCancel();
    }

    protected void cleanSearch() {
        mIsCancel = true;
        mIsBlank = true;
        searchCancel();
        mSearchView.setVisibility(View.INVISIBLE);
        mRd_search_ll_bottom.setVisibility(View.GONE);
        AppUtil.dismissInputSoft(mTop_et_content);
    }

    protected void setVisibility(int visibility) {
        mSearchView.setVisibility(visibility);
    }

    public void launchSearchView(){
        mIsCancel = false;
        bCancelSearchText = false;
        mRect = null;
        if (mTop_et_content.getText().length() > 0) {
            mTop_et_content.selectAll();
            mTop_iv_clear.setVisibility(View.VISIBLE);
        }

        mTop_et_content.requestFocus();
        mTop_et_content.setFocusable(true);
        AppUtil.showSoftInput(mTop_et_content);
    }

    private View.OnClickListener searchModelListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).triggerDismissMenuEvent();
            if (v.getId() == R.id.top_iv_clear) {
                searchCancel();
            } else if (v.getId() == R.id.top_bt_cancel) {
                cancel();

            } else if (v.getId() == R.id.rd_search_ll_top) {

            } else if (v.getId() == R.id.rd_search_center_right) {

            } else if (v.getId() == R.id.rd_search_center_left) {
                dismiss();
//                if (isFastDoubleClick()) {
//                    return;
//                }
//                AppUtil.dismissInputSoft(mTop_et_content);
//                Animation animationL2R = AnimationUtils.loadAnimation(mContext, R.anim.view_anim_rtol_hide);
//                animationL2R.setAnimationListener(new Animation.AnimationListener() {
//                    @Override
//                    public void onAnimationStart(Animation animation) {
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animation animation) {
//                        mRd_search_ll_center.setBackgroundResource(R.color.ux_color_translucent);
//                        mViewCenterLeft.setVisibility(View.GONE);
//                        mViewCenterRight.setVisibility(View.GONE);
//                        mRd_search_ll_bottom.setVisibility(View.VISIBLE);
////                        mBottom_ll_shadow.setVisibility(View.VISIBLE);
//                    }
//
//                    @Override
//                    public void onAnimationRepeat(Animation animation) {
//                    }
//                });
//                animationL2R.setStartOffset(0);
//                mViewCenterRight.startAnimation(animationL2R);
            } else if (v.getId() == R.id.bottom_iv_result) {
                mRd_search_ll_bottom.setVisibility(View.GONE);
                mBottom_ll_shadow.setVisibility(View.GONE);
                mRd_search_ll_center.setBackgroundResource(R.color.ux_color_mask_background);
                mViewCenterLeft.setVisibility(View.VISIBLE);
                mViewCenterRight.setVisibility(View.VISIBLE);
                Animation animationR2L = AnimationUtils.loadAnimation(mContext, R.anim.view_anim_rtol_show);
                animationR2L.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                animationR2L.setStartOffset(0);
                mViewCenterRight.startAnimation(animationR2L);

            } else if (v.getId() == R.id.bottom_iv_prev) {
                searchPre();
            } else if (v.getId() == R.id.bottom_iv_next) {
                searchNext();
            } else if (v.getId() == R.id.top_search_settings) {
                if (null == mSearchSettingsPopup){
                    mSearchSettingsPopup = getSettingsPopup();
                }
                if (!mSearchSettingsPopup.isShowing()){

                    if (mTop_et_content.hasWindowFocus()){
                        mHandler.sendEmptyMessage(HIDE_SOFT_INPUT);
                        mHandler.sendEmptyMessageDelayed(SHOW_SETTINGS_POPUP, 200);
                    } else {
                        mHandler.sendEmptyMessage(SHOW_SETTINGS_POPUP);
                    }
                }
            }
        }
    };

    private void searchCancel() {
        mTop_et_content.setText("");
        mTop_iv_clear.setVisibility(View.INVISIBLE);
    }

    private static long sLastTimeMillis;

    private boolean isFastDoubleClick() {
        long currentTimeMillis = System.currentTimeMillis();
        long delta = currentTimeMillis - sLastTimeMillis;
        if (Math.abs(delta) < 500) {
            return true;
        }
        sLastTimeMillis = currentTimeMillis;
        return false;
    }

    protected int getScreenWidth() {
        return mMetrics.widthPixels;
    }

    protected int getScreenHeight() {
        return mMetrics.heightPixels;
    }

    //search text function
    public interface TaskResult<T1, T2, T3> {
        void onResult(int errCode, T1 p1, T2 p2, T3 p3);
        void setTag(long tag);
        long getTag();
    }

    public static class SearchResult {
        public int mPageIndex;
        public String mSentence;
        public int mPatternStart;
        public ArrayList<RectF> mRects;

        public SearchResult(int pageIndex, String sentence, int patternStart) {
            mPageIndex = pageIndex;
            mSentence = sentence;
            mPatternStart = patternStart;
            mRects = new ArrayList<RectF>();
        }
    }

    class SearchPageTask implements Runnable {
        protected int mPageIndex;
        protected String mPattern; // match text.
        protected ArrayList<SearchResult> mSearchResults;
        protected PDFViewCtrl mPdfView;
        protected TaskResult mTaskResult;
        private int mSearchFlags;

        public SearchPageTask(PDFViewCtrl pdfView, int pageIndex, String pattern, int flag,
                              TaskResult<Integer, String, ArrayList<SearchResult>> taskResult) {
            mPdfView = pdfView;
            mPageIndex = pageIndex;
            mPattern = pattern;
            mTaskResult = taskResult;
            mSearchFlags = flag;
        }

        @Override
        public void run() {
            if (mSearchResults == null) {
                mSearchResults = new ArrayList<SearchResult>();
            }

            int err = searchPage();
            if (mTaskResult != null) {
                mTaskResult.onResult(err, Integer.valueOf(mPageIndex), mPattern, mSearchResults);
            }
        }

        private int searchPage() {
            int errCode = Constants.e_ErrSuccess;
            PDFDoc document = mPdfView.getDoc();
            try {
                SearchCancelCallback cancelCallback = new SearchCancelCallback() {
                    @Override
                    public boolean needToCancelNow() {
                        return true;
                    }
                };
                TextSearch textSearch = new TextSearch(document, cancelCallback);

                textSearch.setStartPage(mPageIndex);
                textSearch.setPattern(mPattern);
                textSearch.setSearchFlags(mSearchFlags);

                boolean bRet = textSearch.findNext();
                while (bRet) {
                    if (textSearch.getMatchPageIndex() != mPageIndex) {
                        break;
                    }
                    String sentence = textSearch.getMatchSentence();
                    if (sentence == null) sentence = "";
                    int sentencePos = textSearch.getMatchSentenceStartIndex();
                    SearchResult searchResult = new SearchResult(mPageIndex, sentence, sentencePos);
                    RectFArray rectFArray = textSearch.getMatchRects();
//                    int count = textSearch.getMatchRectCount();
                    for (int i = 0; i < rectFArray.getSize(); i++) {
                        com.foxit.sdk.common.fxcrt.RectF _rect = rectFArray.getAt(i);// pdf rect
                        RectF rect = new RectF(_rect.getLeft(), _rect.getTop(), _rect.getRight(), _rect.getBottom());
                        searchResult.mRects.add(rect);
                    }

                    mSearchResults.add(searchResult);

                    bRet = textSearch.findNext();
                }
            } catch (PDFException e) {
                errCode = e.getLastError();
            }
            return errCode;
        }

    }

    private String mSearchText = null;
    private boolean bCancelSearchText = true;
    private int mCurrentPosition = -1;
    private float mCurrentPageX;
    private float mCurrentPageY;
    private float mCurrentSearchR;
    private float mCurrentSearchB;
    private ArrayList<SearchResult> mTagResultList = new ArrayList<SearchResult>();
    private ArrayList<SearchResult> mValueResultList = new ArrayList<SearchResult>();
    private ArrayList<SearchResult> mShowResultList = new ArrayList<SearchResult>();

    private void searchPage(int pageIndex, String pattern, int flag,
                            final TaskResult<Integer, String, ArrayList<SearchResult>> result) {
        SearchPageTask searchPageTask = new SearchPageTask(mPdfViewCtrl, pageIndex, pattern, flag, result);
        Handler handler = new Handler();
        handler.post(searchPageTask);
    }

    private void _searchText(final String pattern, final int flag, final int pageIndex) {
        this.mSearchText = pattern.trim();
        TaskResult<Integer, String, ArrayList<SearchResult>> taskResult;
        searchPage(pageIndex, pattern.trim(), flag, taskResult = new TaskResult<Integer, String, ArrayList<SearchResult>>() {
            private long mTaskId;
            @Override
            public void onResult(int errCode, Integer p1, String p2, ArrayList<SearchResult> p3) {
                if (errCode == Constants.e_ErrOutOfMemory) {
                    mPdfViewCtrl.recoverForOOM();
                    setTotalNumber(mValueResultList.size());
                    return;
                }

                if (this.mTaskId != mSearchId) {
                    setTotalNumber(mValueResultList.size());
                    return;
                }

                if (p3 == null) {
                    setTotalNumber(mValueResultList.size());
                    return;
                }

                if (p3.size() > 0) {
                    SearchResult searchResult = new SearchResult(p1, "tag", p3.size());
                    mTagResultList.add(searchResult);
                    mShowResultList.add(searchResult);

                }
                mValueResultList.addAll(p3);
                mShowResultList.addAll(p3);
                if (p3.size() > 0) {
                    notifyDataSetChangedSearchAdapter();
                }
                setSearchNumber(mValueResultList.size());
                if (pageIndex == mPdfViewCtrl.getPageCount() - 1) {
                    setTotalNumber(mValueResultList.size());

                    if (mCurrentPosition == -1 && mShowResultList.size() > 0) {
                        mCurrentPosition = mShowResultList.size() - 1;
                        if (mCurrentPosition != -1) {
                            mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
                            mRect = mShowResultList.get(mCurrentPosition).mRects;
                            setToolbarIcon();
                            mPdfViewCtrl.invalidate();
                        }
                    }

                    return;
                }
                if (p1 >= mPdfViewCtrl.getCurrentPage() && mCurrentPosition == -1 && p3.size() > 0) {
                    mCurrentPosition = mShowResultList.size() - p3.size();
                    mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
                    mRect = mShowResultList.get(mCurrentPosition).mRects;
                    setToolbarIcon();
                    mPdfViewCtrl.invalidate();
                }
                setToolbarIcon();
                if (bCancelSearchText) {
                    bCancelSearchText = false;
                    setTotalNumber(mValueResultList.size());
                } else {
                    _searchText(pattern, flag, pageIndex + 1);
                }
            }

            @Override
            public void setTag(long taskId) {
                mTaskId = taskId;
            }

            @Override
            public long getTag() {
                return mTaskId;
            }
        });
        taskResult.setTag(mSearchId);
    }

    private void searchText(String pattern, int flag) {
        cancelSearchText();
        clearSearchResult();
        mCurrentPosition = -1;
        mSearchId++;
        mRect = null;
        mIsCancel = false;
        mSearchText = null;
        synchronized (this) {
            bCancelSearchText = false;
        }

        _searchText(pattern, flag, 0);
    }

    private void cancelSearchText() {
        synchronized (this) {
            if (!bCancelSearchText) {
                bCancelSearchText = true;

                // do cancel search text
                onCancelSearchText();
            }
        }
    }

    private void notifyDataSetChangedSearchAdapter() {
        if (mAdapterSearch != null) {
            mAdapterSearch.notifyDataSetChanged();
        }
    }

    private void clearSearchResult() {
        if (mShowResultList != null){
            mShowResultList.clear();
        }

        if (mTagResultList != null ){
            mTagResultList.clear();
        }

        if (mValueResultList != null){
            mValueResultList.clear();
        }

        notifyDataSetChangedSearchAdapter();
    }

    private void onCancelSearchText() {
        mRd_search_ll_bottom.setVisibility(View.GONE);
        mPdfViewCtrl.invalidate();
    }

    private Rect getVisibleWidth() {
        Rect rect = new Rect();
        mPdfViewCtrl.getGlobalVisibleRect(rect);
        return rect;
    }

    private void searchPre() {
        if (mSearchText == null || bCancelSearchText) {
            return;
        }

        if (mCurrentPosition <= 1) {
            mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
            mRect = mShowResultList.get(mCurrentPosition).mRects;
            setToolbarIcon();
            mPdfViewCtrl.invalidate();
            return;
        }
        mCurrentPosition--;
        if (mShowResultList.get(mCurrentPosition).mSentence.endsWith("tag")) {
            mCurrentPosition--;
        }
        setCurrentPageX();
        RectF rectF = new RectF(mCurrentPageX, mCurrentPageY, mCurrentSearchR, mCurrentSearchB);
        RectF canvasRectF = new RectF();
        boolean transSuccess = mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, canvasRectF, mShowResultList.get(mCurrentPosition).mPageIndex);
        int screenWidth = getVisibleWidth().width();
        int screenHeight = getVisibleWidth().height();
        if (!transSuccess || canvasRectF.left < 0 || canvasRectF.right > screenWidth || canvasRectF.top < 0 || canvasRectF.bottom > screenHeight) {
            int x = (int) (mCurrentPageX - getScreenWidth() / 4.0f);
            int y = (int) (mCurrentPageY - getScreenHeight() / 4.0f);
            mPdfViewCtrl.gotoPage(mShowResultList.get(mCurrentPosition).mPageIndex, x, y);
        }
        mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
        mRect = mShowResultList.get(mCurrentPosition).mRects;
        setToolbarIcon();
        mPdfViewCtrl.invalidate();
    }

    private void searchNext() {
        if (mSearchText == null || bCancelSearchText) {
            return;
        }

        if (mCurrentPosition >= mShowResultList.size() - 1) {
            mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
            mRect = mShowResultList.get(mCurrentPosition).mRects;
            setToolbarIcon();
            mPdfViewCtrl.invalidate();
            return;
        }
        mCurrentPosition++;
        if (mShowResultList.get(mCurrentPosition).mSentence.endsWith("tag")) {
            mCurrentPosition++;
        }
        setCurrentPageX();
        RectF rectF = new RectF(mCurrentPageX, mCurrentPageY, mCurrentSearchR, mCurrentSearchB);
        RectF canvasRectF = new RectF();
        boolean transSuccess = mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, canvasRectF, mShowResultList.get(mCurrentPosition).mPageIndex);
        int screenWidth = getVisibleWidth().width();
        int screenHeight = getVisibleWidth().height();
        if (!transSuccess || canvasRectF.left < 0 || canvasRectF.right > screenWidth || canvasRectF.top < 0 || canvasRectF.bottom > screenHeight) {
            int x = (int) (mCurrentPageX - getScreenWidth() / 4.0f);
            int y = (int) (mCurrentPageY - getScreenHeight() / 4.0f);
            mPdfViewCtrl.gotoPage(mShowResultList.get(mCurrentPosition).mPageIndex, x, y);
        }
        mPageIndex = mShowResultList.get(mCurrentPosition).mPageIndex;
        mRect = mShowResultList.get(mCurrentPosition).mRects;
        setToolbarIcon();
        mPdfViewCtrl.invalidate();
    }

    private boolean isFirstSearchResult() {
        return mCurrentPosition <= 1;
    }

    private boolean isLastSearchResult() {
        return mCurrentPosition < 1 || mCurrentPosition >= mShowResultList.size() - 1;
    }

    private void setCurrentPageX() {
        float x = 0, y = 0, r = 0, b = 0;
        for (int i = 0; i < mShowResultList.get(mCurrentPosition).mRects.size(); i++) {
            RectF pageRect = new RectF(mShowResultList.get(mCurrentPosition).mRects.get(i));
            RectF pageViewRect = new RectF();
            if (mPdfViewCtrl.convertPdfRectToPageViewRect(pageRect, pageViewRect, mShowResultList.get(mCurrentPosition).mPageIndex)) {
                    if (i == 0) {
                        x = pageViewRect.left;
                        y = pageViewRect.top;
                        r = pageViewRect.right;
                        b = pageViewRect.bottom;
                    } else {
                        if (pageViewRect.left < x) {
                            x = pageViewRect.left;
                        }
                        if (pageViewRect.top < y) {
                            y = pageViewRect.top;
                        }
                        if (pageViewRect.right > r) {
                            r = pageViewRect.right;
                        }
                        if (pageViewRect.bottom > b) {
                            b = pageViewRect.bottom;
                        }
                    }
            }
        }
        mCurrentPageX = x;
        mCurrentPageY = y;
        mCurrentSearchR = r;
        mCurrentSearchB = b;
    }

    //Search Adapter
    class SearchAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mShowResultList == null ? 0 : mShowResultList.size();
        }

        @Override
        public Object getItem(int position) {
            return mShowResultList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                LinearLayout container = new LinearLayout(mContext);
                SearchItemTag mItemTag = null;
                SearchItemView mItemView = null;
                if (mTagResultList.contains(mShowResultList.get(position))) {
                    View viewTag;
                    viewTag = mInflater.inflate(R.layout.search_item_tag, null);

                    mItemTag = new SearchItemTag();
                    mItemTag.search_pageIndex = (TextView) viewTag.findViewById(R.id.search_page_tv);
                    mItemTag.search_pageCount = (TextView) viewTag.findViewById(R.id.search_curpage_count);
                    String pageNumber = mContext.getApplicationContext().getString(R.string.search_page_number);
                    pageNumber = String.format(pageNumber, mShowResultList.get(position).mPageIndex + 1 + "");
                    mItemTag.search_pageIndex.setText(pageNumber);
                    mItemTag.search_pageCount.setText(mShowResultList.get(position).mPatternStart + "");
                    container.addView(viewTag, params);
                } else {
                    mItemView = new SearchItemView();
                    View viewContent = null;
                    viewContent = mInflater.inflate(R.layout.search_item_content, null);

                    mItemView.search_content = (TextView) viewContent.findViewById(R.id.search_content_tv);
                    String mContent = mShowResultList.get(position).mSentence;
                    SpannableString searchContent = new SpannableString(mContent);
                    String matchText = mSearchText.replaceAll("\r", " ");
                    matchText = matchText.replaceAll("\n", " ");
                    matchText = matchText.replaceAll("\\s+", " ");
                    if (matchText.length() > mContent.length()) {
                        matchText = mContent.substring(mShowResultList.get(position).mPatternStart);
                    }

                    try {
                        Pattern pattern = Pattern.compile(matchText, Pattern.CASE_INSENSITIVE);//ignore the case
                        Matcher matcher = pattern.matcher(searchContent);
                        while (matcher.find()) {
                            searchContent.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.ux_text_color_subhead_colour)),
                                    mShowResultList.get(position).mPatternStart,
                                    mShowResultList.get(position).mPatternStart + matchText.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        mItemView.search_content.setText(searchContent);
                        container.addView(viewContent, params);
                    } catch (PatternSyntaxException e) {
                        String splitContent = searchContent.subSequence(mShowResultList.get(position).mPatternStart, mShowResultList.get(position).mPatternStart + matchText.length()).toString();
                        if (splitContent.equalsIgnoreCase(matchText)) {
                            searchContent.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.ux_text_color_subhead_colour)),
                                    mShowResultList.get(position).mPatternStart,
                                    mShowResultList.get(position).mPatternStart + matchText.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        mItemView.search_content.setText(searchContent);
                        container.addView(viewContent, params);
                    }
                }
                return container;
            } else {
                LinearLayout container = (LinearLayout) convertView;
                container.removeAllViews();
                SearchItemTag mItemTag = null;
                SearchItemView mItemView = null;
                if (mTagResultList.contains(mShowResultList.get(position))) {
                    View viewTag = null;
                    viewTag = mInflater.inflate(R.layout.search_item_tag, null);

                    mItemTag = new SearchItemTag();
                    mItemTag.search_pageIndex = (TextView) viewTag.findViewById(R.id.search_page_tv);
                    mItemTag.search_pageCount = (TextView) viewTag.findViewById(R.id.search_curpage_count);
                    String pageNumber = mContext.getApplicationContext().getString(R.string.search_page_number);
                    pageNumber = String.format(pageNumber, mShowResultList.get(position).mPageIndex + 1 + "");
                    mItemTag.search_pageIndex.setText(pageNumber);
                    mItemTag.search_pageCount.setText(mShowResultList.get(position).mPatternStart + "");
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    container.addView(viewTag, lp);
                } else {
                    mItemView = new SearchItemView();
                    View viewContent = null;
                    viewContent = mInflater.inflate(R.layout.search_item_content, null);

                    mItemView.search_content = (TextView) viewContent.findViewById(R.id.search_content_tv);
                    String mContent = mShowResultList.get(position).mSentence;
                    SpannableString searchContent = new SpannableString(mContent);
                    String matchText = mSearchText.replaceAll("\r", " ");
                    matchText = matchText.replaceAll("\n", " ");
                    matchText = matchText.replaceAll("\\s+", " ");
                    if (matchText.length() > mContent.length()) {
                        matchText = mContent.substring(mShowResultList.get(position).mPatternStart);
                    }
                    try {
                        Pattern pattern = Pattern.compile(matchText, Pattern.CASE_INSENSITIVE);//ignore the case
                        Matcher matcher = pattern.matcher(searchContent);
                        while (matcher.find()) {
                            searchContent.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.ux_text_color_subhead_colour)),
                                    mShowResultList.get(position).mPatternStart,
                                    mShowResultList.get(position).mPatternStart + matchText.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        mItemView.search_content.setText(searchContent);

                        container.addView(viewContent);
                    } catch (PatternSyntaxException e) {
                        String splitContent = searchContent.subSequence(mShowResultList.get(position).mPatternStart, mShowResultList.get(position).mPatternStart + matchText.length()).toString();
                        if (splitContent.equalsIgnoreCase(matchText)) {
                            searchContent.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.ux_text_color_subhead_colour)),
                                    mShowResultList.get(position).mPatternStart,
                                    mShowResultList.get(position).mPatternStart + matchText.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        mItemView.search_content.setText(searchContent);
                        container.addView(viewContent);
                    }

                }
                return container;
            }
        }
    }

    private static class SearchItemTag {
        public TextView search_pageIndex;
        public TextView search_pageCount;
    }

    private static class SearchItemView {
        public TextView search_content;
    }

    protected void onDocumentClosed() {
        mTop_et_content.setText("");
        clearSearchResult();
        mCenter_tv_total_number.setText("");
        mSearchFlags = TextSearch.e_SearchNormal;
    }

}
