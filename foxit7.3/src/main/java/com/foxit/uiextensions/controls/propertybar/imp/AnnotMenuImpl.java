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

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

import java.util.ArrayList;

public class AnnotMenuImpl implements AnnotMenu {
    private Context mContext;
    private ArrayList<Integer> mMenuItems;
    private LinearLayout mPopView;
    private ScrollView mContentView;

    private int mMaxWidth;
    private int mMinWidth;
    private ClickListener mListener;//menu click listener
    private PopupWindow mPopupWindow;
    private boolean mShowing = false;
    private AppDisplay display;
    private PDFViewCtrl mPDFViewCtrl;
    private int mShowingPosition = SHOWING_TOP;

    public AnnotMenuImpl(Context context, PDFViewCtrl pdfViewCtrl) {
        this.mContext = context;
        this.mPDFViewCtrl = pdfViewCtrl;
        display = AppDisplay.getInstance(context);
        mMaxWidth = display.dp2px(5000.0f);
        mMinWidth = display.dp2px(80.0f);
    }

    @Override
    public void setMenuItems(ArrayList<Integer> menuItems) {
        this.mMenuItems = menuItems;
        initView();
    }

    private void initView() {
        if (mContentView == null) {
            mContentView = new ScrollView(mContext);
            mContentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mContentView.setBackgroundResource(R.drawable.am_popup_bg);
        } else {
            mContentView.removeAllViews();
        }

        mPopView = new LinearLayout(mContext);
        mPopView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mPopView.setOrientation(LinearLayout.VERTICAL);
        mContentView.addView(mPopView);

        for (int i = 0; i < mMenuItems.size(); i++) {
            if (i > 0) {
                ImageView separate = new ImageView(mContext);
                separate.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, display.dp2px(1.0f)));
                separate.setImageResource(R.color.ux_color_seperator_gray);
                mPopView.addView(separate);
            }
            if (mMenuItems.get(i) == AnnotMenu.AM_BT_COPY) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.rd_am_item_copy_text));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_HIGHLIGHT) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_highlight));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_UNDERLINE) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_underline));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_STRIKEOUT) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_strikeout));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_SQUIGGLY) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_squiggly));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_EDIT) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_edit));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_STYLE) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_am_style));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_COMMENT) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_open));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_REPLY) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_reply));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_DELETE) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_delete));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_NOTE) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_note));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_SIGNATURE) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.rd_security_dsg_addSig));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_SIGN) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_signature));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_CANCEL) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_cancel));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_VERIFY_SIGNATURE) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.rv_security_dsg_verify));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_PALY) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.multimedia_play));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_FLATTEN) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_flatten));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_REDACT) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_redaction));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_APPLY) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_apply));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_SIGN_LIST) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.rv_sign_model));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_TTS) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.rd_tts_speak));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_TTS_STRING) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.rd_tts_speak_string));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_TTS_START) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.rd_tts_speak_start));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_GROUP) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_group));
            } else if (mMenuItems.get(i) == AnnotMenu.AM_BT_UNGROUP) {
                addSubMenu(mMenuItems.get(i), AppResource.getString(mContext, R.string.fx_string_ungroup));
            }
        }

        setMenuWidth();

        if (mPopupWindow == null) {
            mPopupWindow = new PopupWindow(mContentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopupWindow.setTouchable(true);
            mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setShowAlways(false);
    }

    @Override
    public void setShowAlways(boolean showAlways) {
        mPopupWindow.setFocusable(false);
        mPopupWindow.setOutsideTouchable(false);
    }

    private void addSubMenu(int menuTag, String text) {
        TextView textView = new TextView(mContext);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, display.dp2px(56.0f)));
        textView.setText(text);
        textView.setTypeface(Typeface.DEFAULT);
        textView.setTextSize(14.0f);
        textView.setTextColor(mContext.getResources().getColor(R.color.ux_color_dark));

        textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        textView.setPadding(display.dp2px(8.0f), display.dp2px(5.0f), display.dp2px(8.0f), display.dp2px(5.0f));
        textView.setBackgroundResource(R.drawable.am_tv_bg_selector);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTag(menuTag);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tag = (Integer) v.getTag();
                if (mListener != null) {
                    mListener.onAMClick(tag);
                }
            }
        });

        mPopView.addView(textView);
    }

    private void setMenuWidth() {
        int width = getMenuWidth();
        ImageView separate;
        LinearLayout.LayoutParams separateParams;
        TextView textView;
        for (int i = 0; i < mMenuItems.size(); i++) {
            if (i > 0) {
                separate = (ImageView) mPopView.getChildAt(2 * i - 1);
                if (separate != null) {
                    separateParams = (LinearLayout.LayoutParams) separate.getLayoutParams();
                    if (separateParams != null) {
                        separateParams.width = width;
                        separate.setLayoutParams(separateParams);
                    }
                }
            }
            textView = (TextView) mPopView.getChildAt(2 * i);
            if (textView != null) {
                textView.setWidth(width);
                textView.setMaxWidth(mMaxWidth);
            }
        }
    }

    private int getMenuWidth() {
        int realShowWidth = 0;
        TextView textView;
        for (int i = 0; i < mMenuItems.size(); i++) {
            textView = (TextView) mPopView.getChildAt(2 * i);
            if (textView != null) {
                textView.measure(0, 0);
                if (textView.getMeasuredWidth() < mMaxWidth) {
                    if (textView.getMeasuredWidth() > realShowWidth) {
                        realShowWidth = textView.getMeasuredWidth();
                    }
                } else {
                    realShowWidth = mMaxWidth;
                    break;
                }
            }
        }

        if (realShowWidth == 0 || realShowWidth < mMinWidth) {
            realShowWidth = mMinWidth;
        }

        return realShowWidth;
    }


    @Override
    public PopupWindow getPopupWindow() {
        return mPopupWindow;
    }

    @Override
    public void show(RectF rectF) {
        if (mMenuItems != null && mMenuItems.size() > 0) {
            int space = display.dp2px(10.0f);
            RelativeLayout view = (RelativeLayout) ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getRootView();
            int height = view.getHeight();
            int width = view.getWidth();

            int[] location = new int[2];
            view.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];

            RectF expandRectF = new RectF(rectF.left - space + x, rectF.top - space + y, rectF.right + space + x, rectF.bottom + space + y);

            if (mPopupWindow != null && !mPopupWindow.isShowing()) {
                int top = 0 + y;
                int left = 0 + x;
                int right = width + x;
                int bottom = height + y;
                RectF rectFScreen = new RectF(left, top, right, bottom);
                if (RectF.intersects(expandRectF, rectFScreen)) {
                    mPopupWindow.getContentView().measure(0, 0);
                    if (expandRectF.top >= mPopupWindow.getContentView().getMeasuredHeight()) {//top
                        mShowingPosition = SHOWING_TOP;
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                                (int) (expandRectF.top - mPopupWindow.getContentView().getMeasuredHeight()));
                    } else if (height - expandRectF.bottom >= mPopupWindow.getContentView().getMeasuredHeight()) {//bottom
                        mShowingPosition = SHOWING_BOTTOM;
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                                (int) (expandRectF.bottom));
                    } else if (width - expandRectF.right >= mPopupWindow.getContentView().getMeasuredWidth()) {//right
                        mShowingPosition = SHOWING_RIGHT;
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2.0f));
                    } else if (expandRectF.left >= mPopupWindow.getContentView().getMeasuredWidth()) {//left
                        mShowingPosition = SHOWING_LEFT;
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.left - mPopupWindow.getContentView().getMeasuredWidth()),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2));

                    } else {//center
                        mShowingPosition = SHOWING_CENTER;
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2));
                    }
                }

                if (mPopupWindow.isShowing()) {
                    mShowing = true;
                }
            }
        }
    }

    @Override
    public void show(RectF rectF, int pageWidth, int pageHeight, boolean autoDismiss) {
        if (mMenuItems != null && mMenuItems.size() > 0) {
            int space = display.dp2px(10.0f);
            RelativeLayout view = (RelativeLayout) ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getRootView();
            int height = pageHeight;
            int width = pageWidth;

            int[] location = new int[2];
            view.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            RectF expandRectF = new RectF(rectF.left - space + x, rectF.top - space + y, rectF.right + space + x, rectF.bottom + space + y);

            if (mPopupWindow != null && !mPopupWindow.isShowing()) {
                int top = y;
                int left = x;
                int right = pageWidth + x;
                int bottom = pageHeight + y;
                RectF rectFScreen = new RectF(left, top, right, bottom);
                if (RectF.intersects(expandRectF, rectFScreen) || !autoDismiss) {
                    mPopupWindow.getContentView().measure(0, 0);
                    if (expandRectF.top >= mPopupWindow.getContentView().getMeasuredHeight()) {//top
                        mShowingPosition = SHOWING_TOP;
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                                (int) (expandRectF.top - mPopupWindow.getContentView().getMeasuredHeight()));
                    } else if (height - expandRectF.bottom >= mPopupWindow.getContentView().getMeasuredHeight()) {//bottom
                        mShowingPosition = SHOWING_BOTTOM;
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                                (int) (expandRectF.bottom));
                    } else if (width - expandRectF.right >= mPopupWindow.getContentView().getMeasuredWidth()) {//right
                        mShowingPosition = SHOWING_RIGHT;
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2.0f));
                    } else if (expandRectF.left >= mPopupWindow.getContentView().getMeasuredWidth()) {//left
                        mShowingPosition = SHOWING_LEFT;
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.left - mPopupWindow.getContentView().getMeasuredWidth()),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2));

                    } else {//center
                        mShowingPosition = SHOWING_CENTER;
                        mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                                (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                                (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2));
                    }
                }

                if (mPopupWindow.isShowing()) {
                    mShowing = true;
                }
            }
        }
    }

    @Override
    public void show(RectF rectF, View view) {
        if (mMenuItems != null && mMenuItems.size() > 0) {
            int space = display.dp2px(10.0f);
            ViewGroup parent = ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getRootView();
            int height = parent.getHeight();
            int width = parent.getWidth();

            int[] location = new int[2];
            view.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            RectF expandRectF = new RectF(rectF.left - space + x, rectF.top - space + y, rectF.right + space + x, rectF.bottom + space + y);

            if (mPopupWindow != null && !mPopupWindow.isShowing()) {
                mPopupWindow.getContentView().measure(0, 0);
                if (expandRectF.top >= mPopupWindow.getContentView().getMeasuredHeight()) {
                    mShowingPosition = SHOWING_TOP;
                    mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                            (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                            (int) (expandRectF.top - mPopupWindow.getContentView().getMeasuredHeight()));
                } else if (height + y - expandRectF.bottom >= mPopupWindow.getContentView().getMeasuredHeight()) {
                    mShowingPosition = SHOWING_BOTTOM;
                    mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                            (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                            (int) (expandRectF.bottom));
                } else if (width + x - expandRectF.right >= mPopupWindow.getContentView().getMeasuredWidth()) {
                    mShowingPosition = SHOWING_RIGHT;
                    mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                            (int) (expandRectF.right),
                            (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2.0f));
                } else if (expandRectF.left >= mPopupWindow.getContentView().getMeasuredWidth()) {
                    mShowingPosition = SHOWING_LEFT;
                    mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                            (int) (expandRectF.left - mPopupWindow.getContentView().getMeasuredWidth()),
                            (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2));
                } else {
                    mShowingPosition = SHOWING_CENTER;
                    mPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP,
                            (int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                            (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2));
                }

                if (mPopupWindow.isShowing()) {
                    mShowing = true;
                }
            }
        }
    }

    @Override
    public void update(RectF rectF) {
        if (mMenuItems == null)
            return;
        if (mMenuItems.size() > 0) {
            int space = display.dp2px(10.0f);
            ViewGroup parent = ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getRootView();
            int height = parent.getHeight();
            int width = parent.getWidth();

            int[] location = new int[2];
            parent.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            RectF expandRectF = new RectF(rectF.left - space + x, rectF.top - space + y, rectF.right + space + x, rectF.bottom + space + y);

            if (expandRectF.top >= mPopupWindow.getContentView().getMeasuredHeight()) {
                mShowingPosition = SHOWING_TOP;
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                        (int) (expandRectF.top - mPopupWindow.getContentView().getMeasuredHeight()), -1, -1);
            } else if (height + y - expandRectF.bottom >= mPopupWindow.getContentView().getMeasuredHeight()) {
                mShowingPosition = SHOWING_BOTTOM;
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                        (int) (expandRectF.bottom), -1, -1);
            } else if (width + x - expandRectF.right >= mPopupWindow.getContentView().getMeasuredWidth()) {
                mShowingPosition = SHOWING_RIGHT;
                mPopupWindow.update((int) (expandRectF.right),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            } else if (expandRectF.left >= mPopupWindow.getContentView().getMeasuredWidth()) {
                mShowingPosition = SHOWING_LEFT;
                mPopupWindow.update((int) (expandRectF.left - mPopupWindow.getContentView().getMeasuredWidth()),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            } else {
                mShowingPosition = SHOWING_CENTER;
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            }

            if (mShowing) {
                int top = y;
                int left = x;
                int right = width + x;
                int bottom = height + y;
                if (isShowing()) {
                    if (expandRectF.bottom <= top || expandRectF.right <= left || expandRectF.left >= right || expandRectF.top >= bottom) {
                        mPopupWindow.dismiss();
                    }
                } else {
                    RectF screenRectF = new RectF(left, top, right, bottom);
                    if (RectF.intersects(expandRectF, screenRectF)) {
                        boolean showing = mShowing;
                        show(rectF);
                        mShowing = showing;
                    }
                }
            }
        }
    }

    @Override
    public void update(RectF rectF, int pageWidth, int pageHeight, boolean autoDismiss) {
        if (mMenuItems != null && mMenuItems.size() > 0) {
            int space = display.dp2px(10.0f);
            ViewGroup parent = ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getRootView();
            int[] location = new int[2];
            parent.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];

            RectF expandRectF = new RectF(rectF.left - space + x, rectF.top - space + y, rectF.right + space + x, rectF.bottom + space + y);
            int height = pageHeight;
            int width = pageWidth;

            if (expandRectF.top >= mPopupWindow.getContentView().getMeasuredHeight()) {
                mShowingPosition = SHOWING_TOP;
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                        (int) (expandRectF.top - mPopupWindow.getContentView().getMeasuredHeight()), -1, -1);
            } else if (height + y - expandRectF.bottom >= mPopupWindow.getContentView().getMeasuredHeight()) {
                mShowingPosition = SHOWING_BOTTOM;
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                        (int) (expandRectF.bottom), -1, -1);
            } else if (width + x - expandRectF.right >= mPopupWindow.getContentView().getMeasuredWidth()) {
                mShowingPosition = SHOWING_RIGHT;
                mPopupWindow.update((int) (expandRectF.right),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            } else if (expandRectF.left >= mPopupWindow.getContentView().getMeasuredWidth()) {
                mShowingPosition = SHOWING_LEFT;
                mPopupWindow.update((int) (expandRectF.left - mPopupWindow.getContentView().getMeasuredWidth()),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            } else {
                mShowingPosition = SHOWING_CENTER;
                mPopupWindow.update((int) (expandRectF.right - (expandRectF.right - expandRectF.left) / 2 - mPopupWindow.getContentView().getMeasuredWidth() / 2.0f),
                        (int) (expandRectF.bottom - mPopupWindow.getContentView().getMeasuredHeight() / 2.0f - (expandRectF.bottom - expandRectF.top) / 2), -1, -1);
            }
            if (autoDismiss) {
                if (mShowing) {
                    int top = y;
                    int left = x;
                    int right = pageWidth + x;
                    int bottom = pageHeight + y;
                    if (isShowing()) {
                        if (expandRectF.bottom <= top || expandRectF.right <= left || expandRectF.left >= right || expandRectF.top >= bottom) {
                            mPopupWindow.dismiss();
                        }
                    } else {
                        RectF screenRectF = new RectF(left, top, right, bottom);
                        if (RectF.intersects(expandRectF, screenRectF)) {
                            boolean showing = mShowing;
                            show(rectF, pageWidth, pageHeight, autoDismiss);
                            mShowing = showing;
                        }
                    }
                }
            }
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
    public void dismiss() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
            mShowing = false;
        }
    }

    @Override
    public void setListener(ClickListener listener) {
        mListener = listener;
    }

    @Override
    public int getShowingPosition() {
        return mShowingPosition;
    }
}
