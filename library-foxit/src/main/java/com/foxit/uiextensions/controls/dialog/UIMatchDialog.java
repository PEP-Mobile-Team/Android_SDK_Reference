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
package com.foxit.uiextensions.controls.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.utils.AppDisplay;

public class UIMatchDialog extends Dialog implements MatchDialog {
    protected Context mContext;
    private View mView;
    private RelativeLayout mRootView;
    private LinearLayout mTitleView;
    private ImageView mTitleBlueLine;

    private BaseBar mTitleBar;
    private IBaseItem mBackItem;
    private IBaseItem mTitleItem;
    private boolean mShowSeparateLine = true;
    private boolean mFullscreen = false;
    private int mTitleStyle = DLG_TITLE_STYLE_BG_BLUE;

    private LinearLayout mContentViewRoot;
    private LinearLayout mContentView;
    private LinearLayout mButtonsViewRoot;
    private LinearLayout mButtonsView;

    private Button mDlg_bt_cancel;
    private Button mDlg_bt_skip;
    private Button mDlg_bt_replace;
    private Button mDlg_bt_copy;
    private Button mDlg_bt_move;
    private Button mDlg_bt_ok;
    private Button mDlg_bt_open_only;
    private Button mDlg_bt_upload;

    private DisplayMetrics mMetrics;
    private DialogListener mDialogListener;
    private DismissListener mDismissListener;

    private int mHeight = -100;
    private int mButtonHeight;
    private boolean mShowMask = false;
    protected boolean mIsPad = false;

    public UIMatchDialog(Context context) {
        // If theme = R.style.rd_dialog_fullscreen_style, new a fullscreen dialog.
        // If theme = 0, it will be not a fullscreen dialog.
        super(context, AppDisplay.getInstance(context.getApplicationContext()).isPad() ? 0 : R.style.rd_dialog_fullscreen_style);
        this.mContext = context.getApplicationContext();
        mIsPad = AppDisplay.getInstance(mContext).isPad();
        this.mShowSeparateLine = true;
        mFullscreen = mIsPad ? false : true;
        mMetrics = mContext.getResources().getDisplayMetrics();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        initView();
    }

    public UIMatchDialog(Context context, boolean showSeparateLine) {
        super(context, AppDisplay.getInstance(context.getApplicationContext()).isPad() ? 0 : R.style.rd_dialog_fullscreen_style);
        this.mContext = context.getApplicationContext();
        this.mShowSeparateLine = showSeparateLine;
        mFullscreen = AppDisplay.getInstance(mContext).isPad() ? false : true;
        mMetrics = mContext.getResources().getDisplayMetrics();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        initView();
    }


    public UIMatchDialog(Context context, int theme) {
        // set theme = 0, it will be not a fullscreen dialog.
        // set theme = R.style.rd_dialog_fullscreen_style, it will be a fullscreen dialog.
        super(context, theme);
        this.mContext = context.getApplicationContext();
        this.mShowSeparateLine = true;
        if (theme == R.style.rd_dialog_fullscreen_style) {
            mFullscreen = true;
        } else {
            mFullscreen = false;
        }
        mMetrics = mContext.getResources().getDisplayMetrics();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        initView();
    }

    public UIMatchDialog(Context context, int theme, boolean showSeparateLine) {
        // set theme = 0, it will be not a fullscreen dialog.
        // set theme = R.style.rd_dialog_fullscreen_style, it will be a fullscreen dialog.
        super(context, theme);
        this.mContext = context.getApplicationContext();
        mIsPad = AppDisplay.getInstance(mContext).isPad();
        this.mShowSeparateLine = showSeparateLine;
        if (theme == R.style.rd_dialog_fullscreen_style) {
            mFullscreen = true;
        } else {
            mFullscreen = false;
        }
        mMetrics = mContext.getResources().getDisplayMetrics();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        initView();
    }

    private void initView() {
        mButtonHeight = (int) (mContext.getResources().getDimension(R.dimen.ux_dialog_button_height));

        mView = LayoutInflater.from(mContext).inflate(R.layout.dlg_root, null, false);
        mRootView = (RelativeLayout) mView.findViewById(R.id.dlg_root);
        mTitleView = (LinearLayout) mRootView.findViewById(R.id.dlg_top_title);
        mContentViewRoot = (LinearLayout) mRootView.findViewById(R.id.dlg_contentview_root);
        mContentView = (LinearLayout) mRootView.findViewById(R.id.dlg_contentview);
        mButtonsViewRoot = (LinearLayout) mRootView.findViewById(R.id.dlg_buttonview);
        mTitleBlueLine = (ImageView) mRootView.findViewById(R.id.dlg_top_title_line_blue);

        RelativeLayout.LayoutParams contentViewRootParams = (RelativeLayout.LayoutParams) mContentViewRoot.getLayoutParams();
        if (mIsPad) {
            contentViewRootParams.topMargin = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
        } else {
            contentViewRootParams.topMargin = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
        }
        mContentViewRoot.setLayoutParams(contentViewRootParams);

        mButtonsViewRoot.setPadding(0, 0, 0, AppDisplay.getInstance(mContext).dp2px(5.0f));
        mTitleBlueLine.setVisibility(View.GONE);

        if (mFullscreen) {
            mTitleStyle = DLG_TITLE_STYLE_BG_BLUE;
            if (mShowSeparateLine) {
                mTitleBar = new TopBarImpl(mContext);
            } else {
                mTitleBar = new BaseBarImpl(mContext);
            }
        } else {
            mTitleStyle = DLG_TITLE_STYLE_BG_WHITE;
            mTitleBar = new BaseBarImpl(mContext);
            if (mShowSeparateLine) {
                mTitleBlueLine.setVisibility(View.VISIBLE);
            } else {
                mTitleBlueLine.setVisibility(View.GONE);
            }
        }

        mBackItem = new BaseItemImpl(mContext);
        mBackItem.setDisplayStyle(IBaseItem.ItemType.Item_Image);
        if (mIsPad) {
            mBackItem.setImageResource(R.drawable.dlg_back_blue_selector);
        } else {
            mBackItem.setImageResource(R.drawable.cloud_back);
        }
        mBackItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogListener != null) {
                    mDialogListener.onBackClick();
                }
                UIMatchDialog.this.dismiss();
            }
        });

        mTitleItem = new BaseItemImpl(mContext);
        mTitleItem.setDisplayStyle(IBaseItem.ItemType.Item_Text);
        mTitleItem.setText("");
        mTitleItem.setTextColorResource(R.color.ux_text_color_title_light);
        mTitleItem.setTextSize(18.0f);

        if (mTitleStyle == DLG_TITLE_STYLE_BG_BLUE) {
            setTitleStyleBlue();
        } else if (mTitleStyle == DLG_TITLE_STYLE_BG_WHITE) {
            setTitleStyleWhite();
        } else {
            setTitleStyleBlue();
        }

        mTitleBar.addView(mBackItem, BaseBar.TB_Position.Position_LT);
        mTitleBar.addView(mTitleItem, BaseBar.TB_Position.Position_LT);
        mTitleView.addView(mTitleBar.getContentView());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.setContentView(mView);

        if (!mFullscreen) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = AppDisplay.getInstance(mContext).getDialogWidth();
            params.height = getDialogHeight();
            getWindow().setAttributes(params);
        }

        setCanceledOnTouchOutside(true);
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (mShowMask) {
                    mShowMask = false;
                }

                if (mDismissListener != null) {
                    mDismissListener.onDismiss();
                }
            }
        });
    }

    @Override
    public void setFullScreenWithStatusBar() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void setButton(long buttons) {
        if (buttons == DIALOG_NO_BUTTON) {
            mButtonsViewRoot.removeAllViews();
            mButtonsViewRoot.setPadding(0, 0, 0, AppDisplay.getInstance(mContext).dp2px(5.0f));
        } else {
            mButtonsViewRoot.removeAllViews();
            ImageView separator = new ImageView(mContext);
            separator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            separator.setImageResource(R.color.ux_color_dialog_cutting_line);
            mButtonsViewRoot.addView(separator);

            mButtonsView = new LinearLayout(mContext);
            mButtonsView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            mButtonsView.setOrientation(LinearLayout.HORIZONTAL);
            mButtonsView.setGravity(Gravity.CENTER_VERTICAL);
            mButtonsViewRoot.addView(mButtonsView);

            if ((buttons & MatchDialog.DIALOG_CANCEL) == MatchDialog.DIALOG_CANCEL) {
                mDlg_bt_cancel = new Button(mContext);
                addButton(mDlg_bt_cancel, mContext.getString(R.string.fx_string_cancel), MatchDialog.DIALOG_CANCEL);
            }
            if ((buttons & MatchDialog.DIALOG_SKIP) == MatchDialog.DIALOG_SKIP) {
                mDlg_bt_skip = new Button(mContext);
                addButton(mDlg_bt_skip, mContext.getString(R.string.fm_paste_skip), MatchDialog.DIALOG_SKIP);
            }
            if ((buttons & MatchDialog.DIALOG_REPLACE) == MatchDialog.DIALOG_REPLACE) {
                mDlg_bt_replace = new Button(mContext);
                addButton(mDlg_bt_replace, mContext.getString(R.string.fm_paste_replace), MatchDialog.DIALOG_REPLACE);
            }
            if ((buttons & MatchDialog.DIALOG_COPY) == MatchDialog.DIALOG_COPY) {
                mDlg_bt_copy = new Button(mContext);
                addButton(mDlg_bt_copy, mContext.getString(R.string.fx_string_copy), MatchDialog.DIALOG_COPY);
            }
            if ((buttons & MatchDialog.DIALOG_MOVE) == MatchDialog.DIALOG_MOVE) {
                mDlg_bt_move = new Button(mContext);
                addButton(mDlg_bt_move, mContext.getString(R.string.fm_move), MatchDialog.DIALOG_MOVE);
            }
            if ((buttons & MatchDialog.DIALOG_OK) == MatchDialog.DIALOG_OK) {
                mDlg_bt_ok = new Button(mContext);
                addButton(mDlg_bt_ok, mContext.getString(R.string.fx_string_ok), MatchDialog.DIALOG_OK);
            }
            if ((buttons & MatchDialog.DIALOG_OPEN_ONLY) == MatchDialog.DIALOG_OPEN_ONLY) {
                mDlg_bt_open_only = new Button(mContext);
                addButton(mDlg_bt_open_only, mContext.getString(R.string.rv_emailreview_mergedlg_openbutton), MatchDialog.DIALOG_OPEN_ONLY);
            }
            if ((buttons & MatchDialog.DIALOG_UPLOAD) == MatchDialog.DIALOG_UPLOAD) {
                mDlg_bt_upload = new Button(mContext);
                addButton(mDlg_bt_upload, mContext.getString(R.string.cloud_toolbar_more_upload), MatchDialog.DIALOG_UPLOAD);
            }

            if (mButtonsView.getChildCount() > 0) {
                mButtonsViewRoot.setPadding(0, 0, 0, 0);
                mButtonsView.getChildAt(0).setVisibility(View.GONE);
                int buttonNum = mButtonsView.getChildCount() / 2;
                if (buttonNum == 1) {
                    mButtonsView.getChildAt(buttonNum * 2 - 1).setBackgroundResource(R.drawable.dialog_button_background_selector);
                } else if (buttonNum == 2) {
                    mButtonsView.getChildAt(1).setBackgroundResource(R.drawable.dialog_left_button_background_selector);
                    mButtonsView.getChildAt(3).setBackgroundResource(R.drawable.dialog_right_button_background_selector);
                } else if (buttonNum > 2) {
                    mButtonsView.getChildAt(1).setBackgroundResource(R.drawable.dialog_left_button_background_selector);
                    mButtonsView.getChildAt(buttonNum * 2 - 1).setBackgroundResource(R.drawable.dialog_right_button_background_selector);

                    for (int i = 2; i < buttonNum; i++) {
                        mButtonsView.getChildAt(i * 2 - 1).setBackgroundResource(R.drawable.dlg_bt_bg_selector);
                    }
                }
            } else {
                mButtonsViewRoot.setPadding(0, 0, 0, AppDisplay.getInstance(mContext).dp2px(5.0f));
                separator.setVisibility(View.GONE);
            }

            if (mButtonsView.getChildCount() == 6 && ((Long) mButtonsView.getChildAt(5).getTag() == MatchDialog.DIALOG_OPEN_ONLY)) {
                LinearLayout.LayoutParams cancelLayoutParams = (LinearLayout.LayoutParams) mButtonsView.getChildAt(1).getLayoutParams();
                cancelLayoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                mButtonsView.getChildAt(1).setLayoutParams(cancelLayoutParams);

                LinearLayout.LayoutParams okLayoutParams = (LinearLayout.LayoutParams) mButtonsView.getChildAt(3).getLayoutParams();
                okLayoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                mButtonsView.getChildAt(3).setLayoutParams(okLayoutParams);

                LinearLayout.LayoutParams openOnlyLayoutParams = (LinearLayout.LayoutParams) mButtonsView.getChildAt(5).getLayoutParams();
                openOnlyLayoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                mButtonsView.getChildAt(5).setLayoutParams(openOnlyLayoutParams);
            } else {
                if (mDlg_bt_cancel != null) {
                    LinearLayout.LayoutParams cancelLayoutParams = (LinearLayout.LayoutParams) mDlg_bt_cancel.getLayoutParams();
                    cancelLayoutParams.width = 0;
                    mDlg_bt_cancel.setLayoutParams(cancelLayoutParams);
                }
                if (mDlg_bt_ok != null) {
                    LinearLayout.LayoutParams cancelLayoutParams = (LinearLayout.LayoutParams) mDlg_bt_ok.getLayoutParams();
                    cancelLayoutParams.width = 0;
                    mDlg_bt_ok.setLayoutParams(cancelLayoutParams);
                }
            }
        }
    }

    private void addButton(Button button, String title, long buttonType) {
        ImageView separator_vertical = new ImageView(mContext);
        separator_vertical.setLayoutParams(new LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT));
        separator_vertical.setImageResource(R.color.ux_color_dialog_cutting_line);
        mButtonsView.addView(separator_vertical);

        button.setLayoutParams(new LinearLayout.LayoutParams(0, mButtonHeight, 1));
        button.setBackgroundResource(R.drawable.dlg_bt_bg_selector);
        button.setGravity(Gravity.CENTER);
        button.setText(title);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimension(R.dimen.ux_text_height_button));
        button.setTextColor(mContext.getResources().getColor(R.color.dlg_bt_text_selector));
        button.setTag(buttonType);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogListener != null) {
                    long btType = (Long) v.getTag();
                    mDialogListener.onResult(btType);
                }
            }
        });
        mButtonsView.addView(button);
    }

    @Override
    public boolean isShowing() {
        return super.isShowing();
    }

    private void prepareShow() {
        resetForHeight();
    }

    public void showDialog() {
        prepareShow();
        AppDialogManager.getInstance().showAllowManager(this, null);
    }

    public void showDialogNoManage() {
        prepareShow();
    }

    @Override
    public void showDialog(boolean showMask) {
        prepareShow();

        if (this != null && !this.isShowing()) {
            AppDialogManager.getInstance().showAllowManager(this, null);
            mShowMask = showMask;
        }
    }

    @Override
    public void dismiss() {
        if (this != null && this.isShowing()) {
            super.dismiss();
        }
    }

    @Override
    public View getRootView() {
        return mRootView;
    }

    @Override
    public void setTitle(String title) {
        mTitleItem.setText(title);
    }

    @Override
    public void setContentView(View view) {
        if (view != null) {
            mContentView.removeAllViews();
            mContentView.addView(view);
        }
    }

    @Override
    public void setStyle(int style) {
        mTitleStyle = style;
        if (style == DLG_TITLE_STYLE_BG_BLUE) {
            setTitleStyleBlue();
        } else if (style == DLG_TITLE_STYLE_BG_WHITE) {
            setTitleStyleWhite();
        } else {
            mTitleStyle = DLG_TITLE_STYLE_BG_BLUE;
            setTitleStyleBlue();
        }
    }

    private void setTitleStyleBlue() {
        mBackItem.setImageResource(R.drawable.cloud_back);
        mTitleItem.setTextColorResource(R.color.ux_text_color_title_light);
        if (!mFullscreen) {
            getWindow().setBackgroundDrawableResource(R.drawable.dlg_title_bg_4circle_corner_white);
            mTitleBar.setBackgroundResource(R.drawable.dlg_title_bg_circle_corner_blue);
        } else {
            getWindow().setBackgroundDrawableResource(R.color.ux_color_white);
            mTitleBar.setBackgroundResource(R.color.ux_bg_color_toolbar_colour);
        }
    }

    private void setTitleStyleWhite() {
        mBackItem.setImageResource(R.drawable.dlg_back_blue_selector);
        mTitleItem.setTextColorResource(R.color.ux_text_color_subhead_colour);
        if (!mFullscreen) {
            getWindow().setBackgroundDrawableResource(R.drawable.dlg_title_bg_4circle_corner_white);
            mTitleBar.setBackgroundResource(R.drawable.dlg_title_bg_circle_corner_white);
        } else {
            getWindow().setBackgroundDrawableResource(R.color.ux_color_white);
            mTitleBar.setBackgroundResource(R.color.ux_color_white);
        }
    }

    @Override
    public void setBackButtonVisible(int visibility) {
        if (!AppDisplay.getInstance(mContext).isPad() && mFullscreen) {
            if (visibility == View.VISIBLE) {
                mTitleBar.removeItemByItem(mTitleItem);
                mTitleBar.addView(mTitleItem, BaseBar.TB_Position.Position_LT);
            } else {
                mTitleBar.removeItemByItem(mTitleItem);
                mTitleBar.addView(mTitleItem, BaseBar.TB_Position.Position_CENTER);
            }
        }
        mBackItem.getContentView().setVisibility(visibility);
    }

    @Override
    public void setButtonEnable(boolean enable, long buttons) {
        if ((buttons & MatchDialog.DIALOG_CANCEL) == MatchDialog.DIALOG_CANCEL) {
            setEnable(mDlg_bt_cancel, enable);
        }
        if ((buttons & MatchDialog.DIALOG_SKIP) == MatchDialog.DIALOG_SKIP) {
            setEnable(mDlg_bt_skip, enable);
        }
        if ((buttons & MatchDialog.DIALOG_REPLACE) == MatchDialog.DIALOG_REPLACE) {
            setEnable(mDlg_bt_replace, enable);
        }
        if ((buttons & MatchDialog.DIALOG_COPY) == MatchDialog.DIALOG_COPY) {
            setEnable(mDlg_bt_copy, enable);
        }
        if ((buttons & MatchDialog.DIALOG_MOVE) == MatchDialog.DIALOG_MOVE) {
            setEnable(mDlg_bt_move, enable);
        }
        if ((buttons & MatchDialog.DIALOG_OK) == MatchDialog.DIALOG_OK) {
            setEnable(mDlg_bt_ok, enable);
        }
        if ((buttons & MatchDialog.DIALOG_OPEN_ONLY) == MatchDialog.DIALOG_OPEN_ONLY) {
            setEnable(mDlg_bt_open_only, enable);
        }
        if ((buttons & MatchDialog.DIALOG_UPLOAD) == MatchDialog.DIALOG_UPLOAD) {
            setEnable(mDlg_bt_upload, enable);
        }
    }

    private void setEnable(Button button, boolean enable) {
        if (button != null) {
            if (enable) {
                button.setTextColor(mContext.getResources().getColor(R.color.dlg_bt_text_selector));
            } else {
                button.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));
            }
            button.setEnabled(enable);
        }
    }

    @Override
    public void setTitleBlueLineVisible(boolean visible) {
        if (visible) {
            mTitleBlueLine.setVisibility(View.VISIBLE);
        } else {
            mTitleBlueLine.setVisibility(View.GONE);
        }
    }

    /* width: WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.MATCH_PARENT
    */
    @Override
    public void setWidth(int width) {
        if (!mFullscreen) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = width;
            getWindow().setAttributes(params);
        }
    }

    /* height: WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.MATCH_PARENT, int value
    */
    @Override
    public void setHeight(int height) {
        if (!mFullscreen) {
            mHeight = height;

            WindowManager.LayoutParams params = getWindow().getAttributes();
            if (mHeight >= getDialogHeight() || (mHeight <= 0 && mHeight != WindowManager.LayoutParams.WRAP_CONTENT && mHeight != WindowManager.LayoutParams.MATCH_PARENT)
                    ) {
                params.height = getDialogHeight();
            } else {
                params.height = height;
            }
            getWindow().setAttributes(params);
        }
    }

    private void resetForHeight() {
        if (!mFullscreen && mHeight != -100) {
            mRootView.measure(0, 0);
            if (mHeight == WindowManager.LayoutParams.WRAP_CONTENT) {
                int maxTempHeight = getDialogHeight() - mTitleView.getMeasuredHeight();
                if (mButtonsView != null) {
                    if (mButtonsView.getChildCount() > 0) {
                        maxTempHeight = maxTempHeight - mButtonHeight - 1;
                    } else {
                        maxTempHeight = maxTempHeight - AppDisplay.getInstance(mContext).dp2px(5.0f);
                    }
                }
                if (mTitleBlueLine.getVisibility() != View.GONE) {
                    maxTempHeight = maxTempHeight - AppDisplay.getInstance(mContext).dp2px(1.5f);
                }

                if (mContentView.getMeasuredHeight() > maxTempHeight) {
                    WindowManager.LayoutParams params = getWindow().getAttributes();
                    params.height = getDialogHeight();
                    getWindow().setAttributes(params);
                } else {
                    RelativeLayout.LayoutParams contentViewRootParams = (RelativeLayout.LayoutParams) mContentViewRoot.getLayoutParams();
                    contentViewRootParams.addRule(RelativeLayout.ABOVE, 0);// remove ABOVE
                    mContentViewRoot.setLayoutParams(contentViewRootParams);

                    RelativeLayout.LayoutParams buttonViewParams = (RelativeLayout.LayoutParams) mButtonsViewRoot.getLayoutParams();
                    buttonViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);// remove ALIGN_PARENT_BOTTOM
                    buttonViewParams.addRule(RelativeLayout.BELOW, R.id.dlg_contentview_root);// add BELOW
                    mButtonsViewRoot.setLayoutParams(buttonViewParams);
                }
            }

        }
    }

    @Override
    public void setTitlePosition(BaseBar.TB_Position position) {
        if (position == BaseBar.TB_Position.Position_LT) {
            mTitleBar.removeItemByItem(mTitleItem);
            mTitleBar.addView(mTitleItem, BaseBar.TB_Position.Position_LT);
        } else if (position == BaseBar.TB_Position.Position_CENTER) {
            mTitleBar.removeItemByItem(mTitleItem);
            mTitleBar.addView(mTitleItem, BaseBar.TB_Position.Position_CENTER);
        } else if (position == BaseBar.TB_Position.Position_RB) {
            mTitleBar.removeItemByItem(mTitleItem);
            mTitleBar.addView(mTitleItem, BaseBar.TB_Position.Position_RB);
        } else {
            mTitleBar.removeItemByItem(mTitleItem);
            mTitleBar.addView(mTitleItem, BaseBar.TB_Position.Position_CENTER);
        }
    }

    @Override
    public void setListener(DialogListener dialogListener) {
        this.mDialogListener = dialogListener;
    }

    @Override
    public void setOnDLDismissListener(DismissListener dismissListener) {
        mDismissListener = dismissListener;
    }

    public int getDialogHeight() {
        return mMetrics.heightPixels * 7 / 10;
    }
}
