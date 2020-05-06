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
package com.foxit.uiextensions.annots.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;

public class UIPopover extends UIPopupFragment {

    public static UIPopover create(FragmentActivity act, View contentView, boolean blackStyle, boolean showDivider) {
        UIPopover popupDlg = new UIPopover();
        popupDlg.setArrowStyle(act.getApplicationContext(),blackStyle);
        popupDlg.setShowDivider(showDivider);
        if (blackStyle)
            popupDlg.init(act, contentView, "UI_BLACK_POPOVER_FRAGMENT", false, !blackStyle);
        else
            popupDlg.init(act, contentView, "UI_WHITE_POPOVER_FRAGMENT", false, !blackStyle);
        return popupDlg;
    }

    public static final int ARROW_AUTO = 0;
    public static final int ARROW_LEFT = 1;
    public static final int ARROW_TOP = 2;
    public static final int ARROW_RIGHT = 3;
    public static final int ARROW_BOTTOM = 4;
    public static final int ARROW_CENTER = 5;

    boolean mIsBlackStyle;
    boolean mIsShowDivider;
    int mArrowDistance = 12;
    int mArrowWidthDp = 8;
    int mArrowHeightDp = 20;
    int mArrowColor = Color.WHITE;
    int mArrowPosition = ARROW_LEFT;
    boolean mShowArrow = true;
    ArrowImageView mArrowView;
    int mCornerRadius;
    private Context mContext;

    void setArrowStyle(Context context,boolean blackStyle) {
        mIsBlackStyle = blackStyle;
        if (blackStyle) {
            mArrowDistance = 12;
            mArrowWidthDp = 8;
            mArrowHeightDp = 20;
            mArrowColor = context.getResources().getColor( R.color.ux_color_black_popover_bg);
            mCornerRadius = AppDisplay.getInstance(context).dp2px(8);
        } else {
            mArrowDistance = 12;
            mArrowWidthDp = 16;
            mArrowHeightDp = 30;
            mArrowColor = Color.WHITE;
            mCornerRadius = AppDisplay.getInstance(context).dp2px(3);
        }
    }

    void setShowDivider(boolean showDivider) {
        mIsShowDivider = showDivider;
    }

    @Override
    void init(FragmentActivity act, View contentView, String fragName, boolean withMask, boolean withShadow) {
        super.init(act, contentView, fragName, withMask, withShadow);

        mArrowView = new ArrowImageView(act);
        mContext = act.getApplicationContext();
        mArrowView.setMinimumWidth(AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp));
        mArrowView.setMaxWidth(AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp));
        mArrowView.setMinimumHeight(AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp));
        mArrowView.setMaxHeight(AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp));

        RelativeLayout.LayoutParams angleLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        angleLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        angleLp.addRule(RelativeLayout.CENTER_VERTICAL);
        mArrowView.setLayoutParams(angleLp);

        mRootView0.addView(mArrowView, 1);

        if (mIsBlackStyle) {
            mRootViewWithShadow.setBackgroundResource(R.drawable.black_popover_bg);
            ((RelativeLayout.LayoutParams) mRootViewWithShadow.getLayoutParams()).setMargins(
                    AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp), 0, 0, 0);

            if (mIsShowDivider) {
                RelativeLayout dividerView = new RelativeLayout(act);
                RelativeLayout.LayoutParams dividerLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                dividerLp.setMargins(AppDisplay.getInstance(mContext).dp2px(8), 0, AppDisplay.getInstance(mContext).dp2px(8), 0);
                dividerView.setLayoutParams(dividerLp);
                dividerView.setBackgroundResource(R.color.ux_color_white);
                mRootViewWithShadow.addView(dividerView, 0);
            }
        } else {
            angleLp.setMargins(mShadowWidth - AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp), 0, 0, 0);
        }
    }

    public void setBackgroundResource(int resId) {
        mRootViewWithShadow.setBackgroundResource(resId);
    }

    public void setArrowColor(int color) {
        mArrowColor = color;
    }

    public void setShowArrow(boolean isShow) {
        mShowArrow = isShow;
    }

    public void showAtLocation(ViewGroup parent, Rect rect, int contentWidth, int contentHeight, int arrowPosition, int arrowDist) {
        int rootWidth = parent.getWidth();
        int rootHeight = parent.getHeight();
        int[] location = new int[2];
        parent.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        int width = contentWidth + getShadowLength() * 2;
        int height = contentHeight + getShadowLength() * 2;

        mArrowPosition = arrowPosition;
        mArrowDistance = arrowDist;

        if (mArrowPosition == ARROW_AUTO) {
            if (rect.top - height > 0) {
                mArrowPosition = ARROW_BOTTOM;
            } else if (rect.right + width  < rootWidth + x) {
                mArrowPosition = ARROW_LEFT;
            } else if (rect.left - width > 0) {
                mArrowPosition = ARROW_RIGHT;
            } else if (rect.bottom + height < rootHeight + y) {
                mArrowPosition = ARROW_TOP;
            } else {
                mArrowPosition = ARROW_CENTER;
            }
        }

        int leftMargin = 0;
        int rightMargin = 0;
        int topMargin = 0;
        int bottomMargin = 0;

        if (mShowArrow) {
            RelativeLayout.LayoutParams angleLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mArrowView.setLayoutParams(angleLp);
            mArrowView.setVisibility(View.VISIBLE);

            int screenInterval = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_screen_margin_text);

            if (mArrowPosition == ARROW_LEFT) {
                angleLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                angleLp.addRule(RelativeLayout.CENTER_VERTICAL);

                if (rect.centerY() - height / 2 - y < screenInterval) {
                    topMargin = height / 2 + y - rect.centerY() + screenInterval;
                    angleLp.topMargin = height / 2 - AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp) / 2 - topMargin;
                    if (angleLp.topMargin < mCornerRadius + mShadowWidth) {
                        angleLp.topMargin = mCornerRadius + mShadowWidth;
                    }
                    angleLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                } else if (rect.centerY() + height / 2 - y> rootHeight - screenInterval) {
                    bottomMargin = rect.centerY() + height / 2 - y - rootHeight + screenInterval;
                    angleLp.bottomMargin = height / 2 - AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp) / 2 - bottomMargin;
                    if (angleLp.bottomMargin < mCornerRadius + mShadowWidth) {
                        angleLp.bottomMargin = mCornerRadius + mShadowWidth;
                    }
                    angleLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                } else {
                    angleLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                }

                if (mIsBlackStyle) {
                    width += AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp);
                    ((RelativeLayout.LayoutParams) mRootViewWithShadow.getLayoutParams()).setMargins(
                            AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp), 0, 0, 0);
                } else {
                    angleLp.leftMargin = mShadowWidth - AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp);
                }
            } else if (mArrowPosition == ARROW_RIGHT) {
                angleLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                angleLp.addRule(RelativeLayout.CENTER_VERTICAL);

                if (rect.centerY() - height / 2 - y < screenInterval) {
                    topMargin = height / 2 + y - rect.centerY() + screenInterval;
                    angleLp.topMargin = height / 2 - AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp) / 2 - topMargin;
                    if (angleLp.topMargin < mCornerRadius + mShadowWidth) {
                        angleLp.topMargin = mCornerRadius + mShadowWidth;
                    }
                    angleLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                } else if (rect.centerY() + height / 2 - y > rootHeight - screenInterval) {
                    bottomMargin = rect.centerY() + height / 2 - y - rootHeight + screenInterval;
                    angleLp.bottomMargin = height / 2 - AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp) / 2 - bottomMargin;
                    if (angleLp.bottomMargin < mCornerRadius + mShadowWidth) {
                        angleLp.bottomMargin = mCornerRadius + mShadowWidth;
                    }
                    angleLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                } else {
                    angleLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                }

                if (mIsBlackStyle) {
                    width += AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp);
                    ((RelativeLayout.LayoutParams) mRootViewWithShadow.getLayoutParams()).setMargins(
                            0, 0, AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp), 0);
                } else {
                    angleLp.rightMargin = mShadowWidth - AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp);
                }
            } else if (mArrowPosition == ARROW_TOP) {
                angleLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);

                if (rect.centerX() - width / 2 - x < screenInterval) {
                    leftMargin = width / 2 + x + screenInterval - rect.centerX();
                    angleLp.leftMargin = width / 2 - AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp) / 2 - leftMargin;
                    if (angleLp.leftMargin < mCornerRadius + mShadowWidth) {
                        angleLp.leftMargin = mCornerRadius + mShadowWidth;
                    }
                    angleLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                } else if (rect.centerX() + width / 2 - x> rootWidth - screenInterval) {
                    rightMargin = rect.centerX() + width / 2 - x - rootWidth + screenInterval;
                    angleLp.rightMargin = width / 2 - AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp) / 2 - rightMargin;
                    if (angleLp.rightMargin < mCornerRadius + mShadowWidth) {
                        angleLp.rightMargin = mCornerRadius + mShadowWidth;
                    }
                    angleLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                } else {
                    angleLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                }

                if (mIsBlackStyle) {
                    height += AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp);
                    ((RelativeLayout.LayoutParams) mRootViewWithShadow.getLayoutParams()).setMargins(
                            0, AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp), 0, 0);
                } else {
                    angleLp.topMargin = mShadowWidth - AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp);
                }
            } else if (mArrowPosition == ARROW_BOTTOM) {
                angleLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                if (rect.centerX() - width / 2  - x < screenInterval) {
                    leftMargin = width / 2 + x + screenInterval - rect.centerX();
                    angleLp.leftMargin = width / 2 - AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp) / 2 - leftMargin;
                    if (angleLp.leftMargin < mCornerRadius + mShadowWidth) {
                        angleLp.leftMargin = mCornerRadius + mShadowWidth;
                    }
                    angleLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                } else if (rect.centerX() + width / 2 - x> rootWidth - screenInterval) {
                    rightMargin = rect.centerX() + width / 2 - x - rootWidth + screenInterval;
                    angleLp.rightMargin = width / 2 - AppDisplay.getInstance(mContext).dp2px(mArrowHeightDp) / 2 - rightMargin;
                    if (angleLp.rightMargin < mCornerRadius + mShadowWidth) {
                        angleLp.rightMargin = mCornerRadius + mShadowWidth;
                    }
                    angleLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                } else {
                    angleLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                }

                if (mIsBlackStyle) {
                    height += AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp);
                    ((RelativeLayout.LayoutParams) mRootViewWithShadow.getLayoutParams()).setMargins(
                            0, 0, 0, AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp));
                } else {
                    angleLp.bottomMargin = mShadowWidth - AppDisplay.getInstance(mContext).dp2px(mArrowWidthDp);
                }
            } else if (mArrowPosition == ARROW_CENTER) {
                mArrowView.setVisibility(View.INVISIBLE);
            }
        } else {
            mArrowView.setVisibility(View.INVISIBLE);
        }

        setWidth(width);
        setHeight(height);

        if (mArrowPosition == ARROW_LEFT) {
            showAtLocation(null, Gravity.LEFT | Gravity.TOP,
                    rect.right  + mArrowDistance,
                    rect.centerY() - mHeight / 2 + topMargin - bottomMargin);
        } else if (mArrowPosition == ARROW_RIGHT) {
            showAtLocation(null, Gravity.LEFT | Gravity.TOP,
                    rect.left - mArrowDistance - width,
                    rect.centerY() - mHeight / 2 + topMargin - bottomMargin);
        } else if (mArrowPosition == ARROW_TOP) {
            showAtLocation(null, Gravity.LEFT | Gravity.TOP,
                    rect.centerX()  - mWidth / 2,
                    rect.bottom + mArrowDistance);
        } else if (mArrowPosition == ARROW_BOTTOM) {
            showAtLocation(null, Gravity.LEFT | Gravity.TOP,
                    rect.centerX() - mWidth / 2 + leftMargin - rightMargin,
                    rect.top - mArrowDistance - height);
        } else {
            showAtLocation(null, Gravity.LEFT | Gravity.TOP,
                    rect.centerX() - mWidth / 2,
                    rect.centerY() - mHeight / 2);
        }
    }

    public void update(ViewGroup parent, Rect rect, int contentWidth, int contentHeight) {
        showAtLocation(parent, rect, contentWidth, contentHeight, mArrowPosition, mArrowDistance);
    }

    class ArrowImageView extends AppCompatImageView {
        Paint mPaint = new Paint();
        Path mPath = new Path();

        public ArrowImageView(Context context) {
            super(context);
        }

        public ArrowImageView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public ArrowImageView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            mPaint.setDither(true);
            mPaint.setAntiAlias(true);
            mPaint.setColor(mArrowColor);

            int width = getWidth();
            int height = getHeight();
            AppDisplay display = AppDisplay.getInstance(mContext);

            mPath.reset();
            switch (mArrowPosition) {
                case ARROW_LEFT:
                    mPath.moveTo(0, height / 2);
                    mPath.lineTo(display.dp2px(mArrowWidthDp), height / 2 - display.dp2px(mArrowHeightDp) / 2);
                    mPath.lineTo(display.dp2px(mArrowWidthDp), height / 2 + display.dp2px(mArrowHeightDp) / 2);
                    mPath.close();
                    break;
                case ARROW_RIGHT:
                    mPath.moveTo(width, height / 2);
                    mPath.lineTo(width - display.dp2px(mArrowWidthDp), height / 2 - display.dp2px(mArrowHeightDp) / 2);
                    mPath.lineTo(width - display.dp2px(mArrowWidthDp), height / 2 + display.dp2px(mArrowHeightDp) / 2);
                    mPath.close();
                    break;
                case ARROW_TOP:
                    mPath.moveTo(width / 2, 0);
                    mPath.lineTo(width / 2 - display.dp2px(mArrowHeightDp) / 2, display.dp2px(mArrowWidthDp));
                    mPath.lineTo(width / 2 + display.dp2px(mArrowHeightDp) / 2, display.dp2px(mArrowWidthDp));
                    mPath.close();
                    break;
                case ARROW_BOTTOM:
                    mPath.moveTo(width / 2, height);
                    mPath.lineTo(width / 2 - display.dp2px(mArrowHeightDp) / 2, height - display.dp2px(mArrowWidthDp));
                    mPath.lineTo(width / 2 + display.dp2px(mArrowHeightDp) / 2, height - display.dp2px(mArrowWidthDp));
                    mPath.close();
                    break;
                default:
            }

            canvas.drawPath(mPath, mPaint);
        }
    };
}
