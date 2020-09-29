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
package com.foxit.uiextensions.modules;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.OnPageEventListener;

import java.lang.ref.WeakReference;

/** The module enable user to navigate between pdf pages by index. */
public class PageNavigationModule implements Module {
    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;
    private AppDisplay mDisplay;

    private InputMethodManager mInputMethodMgr = null;

    private boolean mIsClosedState = true;
    private RelativeLayout mClosedRootLayout;
    private LinearLayout mClosedPageLabel;
    private TextView mClosedPageLabel_Total;
    private TextView mClosedPageLabel_Current;
    private ImageView mPreImageView;
    private ImageView mNextImageView;
    private RelativeLayout mOpenedRootLayout;
    private EditText mOpenedPageIndex;
    private ImageView mOpenedClearBtn;
    private TextView mOpenedGoBtn;
    private MyHandler mHandler;
    private OpenJumpPageBackground mOpenJumpPageBackground;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    private int mLastState = ReadStateConfig.STATE_NORMAL;

    public PageNavigationModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
        mParent = parent;
        mDisplay = new AppDisplay(mContext);
    }

    @Override
    public String getName() {
        return MODULE_NAME_PAGENAV;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
            ((UIExtensionsManager) mUiExtensionsManager).registerStateChangeListener(mStateChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerLifecycleListener(mLifecycleEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerLayoutChangeListener(mLayoutChangeListener);
        }
        mInputMethodMgr = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        mHandler = new MyHandler(this);
        mHandler.postDelayed(runnable, 1000 * 5);

        initClosedUI();
        initOpenedUI();
        mPdfViewCtrl.registerDocEventListener(mDocumentEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);
        mPdfViewCtrl.registerLayoutEventListener(layoutEventListener);
        mPdfViewCtrl.setOnKeyListener(mOnKeyKListener);
        onUIStatusChanged();
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterStateChangeListener(mStateChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLifecycleListener(mLifecycleEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLayoutChangeListener(mLayoutChangeListener);
        }

        disInitClosedUI();
        disInitOpenedUI();
        mPdfViewCtrl.unregisterDocEventListener(mDocumentEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        mPdfViewCtrl.unregisterLayoutEventListener(layoutEventListener);

        mHandler.removeCallbacks(runnable);
        return true;
    }


    private void initClosedUI() {
        mClosedRootLayout = (RelativeLayout) View.inflate(mContext, R.layout.rd_gotopage_close, null);
        mClosedPageLabel = (LinearLayout) mClosedRootLayout.findViewById(R.id.rd_gotopage_pagenumber);
        mClosedPageLabel_Total = (TextView) mClosedRootLayout.findViewById(R.id.rd_gotopage_pagenumber_total);
        mClosedPageLabel_Current = (TextView) mClosedRootLayout.findViewById(R.id.rd_gotopage_pagenumber_current);
        mClosedPageLabel_Current.setText("");
        mClosedPageLabel_Current.setTextColor(Color.WHITE);
        mClosedPageLabel_Total.setText("-");
        mClosedPageLabel_Total.setTextColor(Color.WHITE);
        mClosedPageLabel.setEnabled(false);

        mPreImageView = (ImageView) mClosedRootLayout.findViewById(R.id.rd_jumppage_previous);
        mNextImageView = (ImageView) mClosedRootLayout.findViewById(R.id.rd_jumppage_next);

        setClosedUIClickListener();
        RelativeLayout.LayoutParams closedLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        closedLP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mParent.addView(mClosedRootLayout, closedLP);
        if (mDisplay.isPad()) {
            mClosedRootLayout.setPadding((int) (AppResource.getDimension(mContext, R.dimen.ux_horz_left_margin_pad) + mDisplay.dp2px(4)), 0, 0, (int) (AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_pad) + mDisplay.dp2px(16)));
        }
        mPreImageView.setVisibility(View.GONE);
        mNextImageView.setVisibility(View.GONE);
    }

    private void initOpenedUI() {
        mOpenedRootLayout = (RelativeLayout) View.inflate(mContext, R.layout.rd_gotopage_open, null);
        mOpenedPageIndex = (EditText) mOpenedRootLayout.findViewById(R.id.rd_gotopage_index_et);
        mOpenedClearBtn = (ImageView) mOpenedRootLayout.findViewById(R.id.rd_gotopage_edit_clear);
        mOpenedGoBtn = (TextView) mOpenedRootLayout.findViewById(R.id.rd_gotopage_togo_iv);

        mOpenJumpPageBackground = new OpenJumpPageBackground(mContext);
        mOpenedClearBtn.setVisibility(View.INVISIBLE);
        mOpenedRootLayout.setVisibility(View.GONE);
        mOpenJumpPageBackground.setVisibility(View.GONE);
        setOpenedClickListener();
    }

    private void addOpenedLayoutToMainFrame() {
        try {
            mOpenedRootLayout.setVisibility(View.VISIBLE);
            mOpenJumpPageBackground.setVisibility(View.VISIBLE);
            mParent.addView(mOpenedRootLayout);
            if (mDisplay.isPad()) {
                mOpenedRootLayout.getLayoutParams().height = mContext.getResources().getDimensionPixelOffset(R.dimen.ux_toolbar_height_pad);
            } else {
                mOpenedRootLayout.getLayoutParams().height = mContext.getResources().getDimensionPixelOffset(R.dimen.ux_toolbar_height_phone);
            }
            RelativeLayout.LayoutParams openJumpPageBackgroundLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            openJumpPageBackgroundLP.addRule(RelativeLayout.BELOW, R.id.rd_gotopage_open_root_layout);
            mParent.addView(mOpenJumpPageBackground, openJumpPageBackgroundLP);
        } catch (Exception ignored) {
        }
    }

    private void removeOpenedLayoutFromMainFrame() {
        mParent.removeView(mOpenedRootLayout);
        mParent.removeView(mOpenJumpPageBackground);
    }

    class OpenJumpPageBackground extends RelativeLayout {
        public OpenJumpPageBackground(Context context) {
            super(context);
        }
    }

    private void setOpenedClickListener() {
        mOpenedGoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                onGotoPage();
            }
        });
        mOpenedClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (mOpenedPageIndex != null) {
                    mOpenedPageIndex.setText("");
                }
            }
        });
        mOpenedPageIndex.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (KeyEvent.KEYCODE_ENTER == keyCode && event.getAction() == KeyEvent.ACTION_DOWN) {
                    InputMethodManager inputManager = (InputMethodManager)
                            mOpenedPageIndex.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(mOpenedPageIndex.getWindowToken(), 0);
                    onGotoPage();
                    return true;
                }
                return false;
            }
        });
        mOpenedPageIndex.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mOpenedPageIndex == v) {
                    if (!hasFocus) {
                        InputMethodManager inputManager = (InputMethodManager)
                                mOpenedPageIndex.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(mOpenedPageIndex.getWindowToken(), 0);

                        mIsClosedState = true;
                        onUIStatusChanged();
                    }
                }
            }
        });
        mOpenedPageIndex.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (mOpenedPageIndex.getText() != null) {
                            if (mOpenedPageIndex.getText().length() != 0) {
                                mOpenedClearBtn.setVisibility(View.VISIBLE);
                                Integer number = null;
                                if (!mOpenedPageIndex.getText().toString().trim().equals("")) {
                                    int index = mOpenedPageIndex.getText().toString().indexOf("/");
                                    try {
                                        if (index == -1) {
                                            number = Integer.valueOf(mOpenedPageIndex.getText().toString());
                                        } else {
                                            number = Integer.valueOf(mOpenedPageIndex.getText().subSequence(0, index).toString());
                                        }
                                    } catch (Exception e) {
                                        number = null;
                                    }
                                }
                                if (number == null || 0 > number || number > mPdfViewCtrl.getPageCount()) {
                                    mOpenedPageIndex.setText(mOpenedPageIndex.getText().toString().substring(0, mOpenedPageIndex.getText().length() - 1));
                                    mOpenedPageIndex.selectAll();
                                    Toast toast = new Toast(mContext);
                                    int i = mPdfViewCtrl.getPageCount();
                                    String str = AppResource.getString(mContext.getApplicationContext(), R.string.rv_gotopage_error_toast)
                                            + " " + "(1-" + String.valueOf(i) + ")";
                                    LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                    View toastLayout = inflate.inflate(R.layout.rd_gotopage_tips, null);
                                    TextView tv = (TextView) toastLayout.findViewById(R.id.rd_gotopage_toast_tv);
                                    tv.setText(str);
                                    toast.setView(toastLayout);
                                    toast.setDuration(Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                }
                            } else {
                                mOpenedClearBtn.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            mOpenedClearBtn.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }

        );
        mOpenedRootLayout.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return true;
                    }
                }

        );
        mOpenJumpPageBackground.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mIsClosedState) {
                            mIsClosedState = true;
                            InputMethodManager inputManager = (InputMethodManager)
                                    mOpenedPageIndex.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(mOpenedPageIndex.getWindowToken(), 0);
                            ((UIExtensionsManager) mUiExtensionsManager).changeState(mLastState);
//                            onUIStatusChanged();
                        }
                    }
                }

        );
    }

    private void disInitClosedUI() {
        mParent.removeView(mClosedRootLayout);
    }

    private void disInitOpenedUI() {
        AppKeyboardUtil.removeKeyboardListener(mOpenedRootLayout);
        mParent.removeView(mOpenedRootLayout);
    }

    private void triggerDismissMenu() {

        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).triggerDismissMenuEvent();
        }

        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() != null) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
        }
    }

    private void setClosedUIClickListener() {
        mClosedPageLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLastState = ((UIExtensionsManager) mUiExtensionsManager).getState();
                ((UIExtensionsManager) mUiExtensionsManager).changeState(ReadStateConfig.STATE_PAGENAVIGATION);
            }
        });

        mPreImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PDFDoc dmDoc = mPdfViewCtrl.getDoc();
                if (dmDoc != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                }
                mPdfViewCtrl.gotoPrevView();
                triggerDismissMenu();
                if (mPdfViewCtrl.hasPrevView()) {
                    mPreImageView.setVisibility(View.VISIBLE);
                } else {
                    mPreImageView.setVisibility(View.GONE);
                }
                if (mPdfViewCtrl.hasNextView()) {
                    mNextImageView.setVisibility(View.VISIBLE);
                } else {
                    mNextImageView.setVisibility(View.GONE);
                }
                Message msg = new Message();
                msg.what = SHOW_RESET;
                mHandler.sendMessage(msg);
            }
        });
        mNextImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PDFDoc dmDoc = mPdfViewCtrl.getDoc();
                if (dmDoc != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                }
                mPdfViewCtrl.gotoNextView();
                triggerDismissMenu();
                if (mPdfViewCtrl.hasPrevView()) {
                    mPreImageView.setVisibility(View.VISIBLE);
                } else {
                    mPreImageView.setVisibility(View.GONE);
                }
                if (mPdfViewCtrl.hasNextView()) {
                    mNextImageView.setVisibility(View.VISIBLE);
                } else {
                    mNextImageView.setVisibility(View.GONE);
                }
                Message msg = new Message();
                msg.what = SHOW_RESET;
                mHandler.sendMessage(msg);
            }
        });

        mClosedRootLayout.findViewById(R.id.rv_gotopage_relativeLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    private void showReset() {
        mHandler.removeCallbacks(runnable);
        mHandler.postDelayed(runnable, 1000 * 5);
    }

    private void showOver() {
        if (mPreImageView.getVisibility() == View.VISIBLE) {
            mPreImageView.setVisibility(View.GONE);
        }
        if (mNextImageView.getVisibility() == View.VISIBLE) {
            mNextImageView.setVisibility(View.GONE);
        }
        if (mClosedRootLayout.getVisibility() == View.VISIBLE) {
            mClosedRootLayout.setVisibility(View.GONE);
        }
    }

    //The timer
    private static final int SHOW_OVER = 100;
    private static final int SHOW_RESET = 200;

    private class MyHandler extends Handler {
        WeakReference<PageNavigationModule> mNavRef;

        public MyHandler(PageNavigationModule module) {
            mNavRef = new WeakReference<PageNavigationModule>(module);
        }

        @Override
        public void handleMessage(Message msg) {
            PageNavigationModule module = mNavRef.get();
            switch (msg.what) {
                case SHOW_RESET:
                    if (module != null) {
                        module.showReset();
                    }

                    break;
                case SHOW_OVER:
                    if (module != null) {
                        module.showOver();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Message msg = new Message();
            msg.what = SHOW_OVER;
            mHandler.sendMessage(msg);
        }
    };

    private void onUIStatusChanged() {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        if (mIsClosedState
                || uiExtensionsManager.getCurrentToolHandler() != null
                || uiExtensionsManager.getCurrentAnnotHandler() != null) {
            if (!mIsClosedState) {
                mIsClosedState = true;
            }
            if (mClosedRootLayout.getVisibility() != View.VISIBLE) {
                startShow();
                mClosedPageLabel.setEnabled(true);
                mClosedRootLayout.setVisibility(View.VISIBLE);
                if (mPdfViewCtrl.hasPrevView()) {
                    mPreImageView.setVisibility(View.VISIBLE);
                } else {
                    mPreImageView.setVisibility(View.GONE);
                }
                if (mPdfViewCtrl.hasNextView()) {
                    mNextImageView.setVisibility(View.VISIBLE);
                } else {
                    mNextImageView.setVisibility(View.GONE);
                }
            }
            Message msg = new Message();
            msg.what = SHOW_RESET;
            mHandler.sendMessage(msg);
            if (mOpenedRootLayout.getVisibility() != View.GONE) {
                mOpenedRootLayout.setVisibility(View.GONE);
                mOpenJumpPageBackground.setVisibility(View.GONE);
                removeOpenedLayoutFromMainFrame();
            }
        } else {
            if (mClosedRootLayout.getVisibility() != View.GONE) {
                endShow();
                mClosedRootLayout.setVisibility(View.GONE);
            }
            if (mOpenedRootLayout.getVisibility() != View.VISIBLE) {
                mOpenedRootLayout.setVisibility(View.VISIBLE);
                mOpenJumpPageBackground.setVisibility(View.VISIBLE);
                addOpenedLayoutToMainFrame();
            }
        }
    }

    private void onGotoPage() {
        Toast toast = new Toast(mContext);
        Editable text = mOpenedPageIndex.getText();
        Integer number = null;
        if (!text.toString().trim().equals("")) {
            int index = text.toString().indexOf("/");
            try {
                if (index == -1) {//no '/'
                    number = Integer.valueOf(text.toString());
                } else {
                    number = Integer.valueOf(text.subSequence(0, index).toString());
                }
            } catch (Exception e) {
                number = null;
            }
        }

        if (number != null && 0 < number && number <= mPdfViewCtrl.getPageCount()) {
            mPdfViewCtrl.gotoPage(number - 1, 0, 0);
            mIsClosedState = true;
            mInputMethodMgr.hideSoftInputFromWindow(mOpenedPageIndex.getWindowToken(), 0);
            ((UIExtensionsManager) mUiExtensionsManager).changeState(mLastState);
//            onUIStatusChanged();
            if (mClosedRootLayout.getVisibility() != View.VISIBLE) {
                startShow();
                mClosedRootLayout.setVisibility(View.VISIBLE);
            }
            Message msg = new Message();
            msg.what = SHOW_RESET;
            mHandler.sendMessage(msg);
        } else {
            int i = mPdfViewCtrl.getPageCount();
            String str = AppResource.getString(mContext.getApplicationContext(), R.string.rv_gotopage_error_toast)
                    + " " + "(1-" + String.valueOf(i) + ")";
            LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View toastLayout = inflate.inflate(R.layout.rd_gotopage_tips, null);
            TextView tv = (TextView) toastLayout.findViewById(R.id.rd_gotopage_toast_tv);
            tv.setText(str);
            toast.setView(toastLayout);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            mOpenedPageIndex.selectAll();
        }
    }

    private PDFViewCtrl.ILayoutEventListener layoutEventListener = new PDFViewCtrl.ILayoutEventListener() {
        @Override
        public void onLayoutModeChanged(int origin, int present) {
            mPageEventListener.onPageChanged(mPdfViewCtrl.getCurrentPage(), mPdfViewCtrl.getCurrentPage());
        }
    };

    private IStateChangeListener mStateChangeListener = new IStateChangeListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            if (newState == ReadStateConfig.STATE_PAGENAVIGATION) {
                PDFDoc dmDoc = mPdfViewCtrl.getDoc();
                if (dmDoc != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                }
                triggerDismissMenu();
                mIsClosedState = false;
                int pageIndex = mPdfViewCtrl.getCurrentPage();
                mPageEventListener.onPageChanged(pageIndex, pageIndex);

                addOpenedLayoutToMainFrame();
                mOpenedPageIndex.selectAll();
                mOpenedPageIndex.requestFocus();
                mInputMethodMgr.showSoftInput(mOpenedPageIndex, 0);
                mOpenedPageIndex.setText("");
            } else {
                if (!mIsClosedState) {
                    mIsClosedState = true;
                    InputMethodManager inputManager = (InputMethodManager)
                            mOpenedPageIndex.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(mOpenedPageIndex.getWindowToken(), 0);
                }
            }
            changPageNumberState(((UIExtensionsManager) mUiExtensionsManager).getMainFrame().isToolbarsVisible());
        }
    };

    private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener() {
        @Override
        public void onPageChanged(int old, int current) {
            if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) return;
            mClosedPageLabel.setEnabled(true);
            String str = (current + 1) + "/" + mPdfViewCtrl.getPageCount();
            if (((mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING ||
                    mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER) &&
                    current < mPdfViewCtrl.getPageCount() - 1 && current >= 0) ||
                    (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING && mPdfViewCtrl.getPageCount() % 2 == 0)) {

                if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER && current == 0) {
                    str = (current + 1) + "/" + mPdfViewCtrl.getPageCount();
                    mClosedPageLabel_Current.setText("" + (current + 1));
                } else {
                    if (current != 0 &&
                            (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING && current % 2 != 0) || (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER && current % 2 == 0)) {
                        current = current - 1;
                    }
                    str = (current + 1) + "," + (current + 2) + "/" + mPdfViewCtrl.getPageCount();
                    mClosedPageLabel_Current.setText("" + (current + 1) + "," + (current + 2));
                }

            } else if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER && current == -1) {
                mClosedPageLabel_Current.setText("" + (current + 2));
            } else {
                mClosedPageLabel_Current.setText("" + (current + 1));
            }
            mClosedPageLabel_Total.setText("/" + mPdfViewCtrl.getPageCount());
            if (!mIsClosedState) {
                mOpenedPageIndex.setHint(str);
            }
            //reset jumpView
            if (mPdfViewCtrl.hasPrevView()) {
                mPreImageView.setVisibility(View.VISIBLE);
            } else {
                mPreImageView.setVisibility(View.GONE);
            }
            if (mPdfViewCtrl.hasNextView()) {
                mNextImageView.setVisibility(View.VISIBLE);
            } else {
                mNextImageView.setVisibility(View.GONE);
            }
            if (mClosedRootLayout.getVisibility() != View.VISIBLE) {
                startShow();
                mClosedRootLayout.setVisibility(View.VISIBLE);
            }
            if (old != current) {
                Message msg = new Message();
                msg.what = SHOW_RESET;
                mHandler.sendMessage(msg);
            }
        }

        @Override
        public void onPageJumped() {
            if (mPdfViewCtrl.hasPrevView()) {
                mPreImageView.setVisibility(View.VISIBLE);
            } else {
                mPreImageView.setVisibility(View.GONE);
            }
            if (mPdfViewCtrl.hasNextView()) {
                mNextImageView.setVisibility(View.VISIBLE);
            } else {
                mNextImageView.setVisibility(View.GONE);
            }
            if (mClosedRootLayout.getVisibility() != View.VISIBLE) {
                mClosedRootLayout.setVisibility(View.VISIBLE);
            }
            Message msg = new Message();
            msg.what = SHOW_RESET;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onPageVisible(int index) {

        }

        @Override
        public void onPageInvisible(int index) {

        }
    };

    public void resetJumpView() {
        if (mPdfViewCtrl.hasPrevView()) {
            mPreImageView.setVisibility(View.VISIBLE);
        } else {
            mPreImageView.setVisibility(View.GONE);
        }
        if (mPdfViewCtrl.hasNextView()) {
            mNextImageView.setVisibility(View.VISIBLE);
        } else {
            mNextImageView.setVisibility(View.GONE);
        }
        if (mClosedRootLayout.getVisibility() != View.VISIBLE) {
            mClosedRootLayout.setVisibility(View.VISIBLE);
        }

        if ((mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING)
                && mPdfViewCtrl.getCurrentPage() < mPdfViewCtrl.getPageCount() - 1) {
            mClosedPageLabel_Current.setText("" + (mPdfViewCtrl.getCurrentPage() + 1) + "," + (mPdfViewCtrl.getCurrentPage() + 2));
        } else {
            mClosedPageLabel_Current.setText("" + (mPdfViewCtrl.getCurrentPage() + 1));
        }
        mClosedPageLabel_Total.setText("/" + mPdfViewCtrl.getPageCount());
        Message msg = new Message();
        msg.what = SHOW_RESET;
        mHandler.sendMessage(msg);
    }

    private void startShow() {
        mClosedRootLayout.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.view_anim_visible_show));
    }

    private void endShow() {
        mClosedRootLayout.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.view_anim_visible_hide));
    }

    private View.OnKeyListener mOnKeyKListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                if (!mIsClosedState) {
                    mIsClosedState = true;
                    ((UIExtensionsManager) mUiExtensionsManager).changeState(mLastState);
//                    onUIStatusChanged();
                    return true;
                }
            }
            return false;
        }
    };

    public boolean onKeyBack() {
        if (ReadStateConfig.STATE_PAGENAVIGATION == ((UIExtensionsManager) mUiExtensionsManager).getState()) {
            ((UIExtensionsManager) mUiExtensionsManager).changeState(mLastState);
            return true;
        }
        return false;
    }

    private PDFViewCtrl.IDocEventListener mDocumentEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int errCode) {
            if (pdfDoc == null || errCode != Constants.e_ErrSuccess) {
                return;
            }
            mPageEventListener.onPageChanged(mPdfViewCtrl.getCurrentPage(), mPdfViewCtrl.getCurrentPage());
            onUIStatusChanged();
        }

        @Override
        public void onDocWillClose(PDFDoc pdfDoc) {
        }

        @Override
        public void onDocClosed(PDFDoc pdfDoc, int i) {
            mHandler.removeCallbacksAndMessages(null);
        }

        @Override
        public void onDocWillSave(PDFDoc pdfDoc) {
        }

        @Override
        public void onDocSaved(PDFDoc pdfDoc, int i) {
        }
    };

    public void changPageNumberState(boolean isToolbarsVisible) {
        if (!isToolbarsVisible) {
            if (mClosedRootLayout.getVisibility() != View.GONE) {
                endShow();
                mClosedRootLayout.setVisibility(View.GONE);
            }
            if (mIsClosedState) {
                if (mOpenedRootLayout.getVisibility() != View.GONE) {
                    mOpenedRootLayout.setVisibility(View.GONE);
                    mOpenJumpPageBackground.setVisibility(View.GONE);
                    removeOpenedLayoutFromMainFrame();
                }
            }
        } else {
            onUIStatusChanged();
        }
    }

    private ILifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {

        @Override
        public void onResume(Activity act) {
            changPageNumberState(((UIExtensionsManager) mUiExtensionsManager).getMainFrame().isToolbarsVisible());
        }
    };

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (oldWidth != newWidth || oldHeight != newHeight) {
                mClosedPageLabel_Current.requestLayout();
                if (mPdfViewCtrl != null && mPdfViewCtrl.getDoc() != null)
                    resetJumpView();
            }
        }
    };

}
