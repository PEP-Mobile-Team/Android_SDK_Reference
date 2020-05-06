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


import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.config.modules.annotations.AnnotationsConfig;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.AnnotItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BaseBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

import java.util.ArrayList;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;

public class UIAnnotToolbar extends RelativeLayout implements MoreTools {

    // dimens
    private int mBlackPopoverHeightDp = 36;

    private BaseBarImpl mAnnotBar;
    private RelativeLayout mRootView;
    private RelativeLayout mContainerLayout;

    private ArrayList<Integer> mBtnTags = new ArrayList<>();
    private ArrayList<IBaseItem> mBtnItems = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> mBtnMoreTags = new ArrayList<>();
    private ArrayList<ArrayList<IBaseItem>> mBtnMoreItems = new ArrayList<>();
    private ArrayList<BaseBarImpl> mBtnMoreBars = new ArrayList<>();

    private SparseBooleanArray mAnnotConfigArrays = new SparseBooleanArray();
    private SparseIntArray mBtnDrawableMap = new SparseIntArray();
    private SparseIntArray mWhite2BlackTagMap = new SparseIntArray();
    private SparseArray<IMT_MoreClickListener> mListeners = new SparseArray<>();

    private PDFViewCtrl mPdfViewCtrl;
    private Context mContext;

    private float mDividerHeight;
    private float mDividerWidth;
    private IBaseItem mCurItem;

    private IBaseItem mDividerItem;
    private ImageView mDividerView;

    public UIAnnotToolbar(Context context, PDFViewCtrl pdfViewCtrl, Config config) {
        this(context, null, pdfViewCtrl, config);
    }

    public UIAnnotToolbar(Context context, AttributeSet attrs, PDFViewCtrl pdfViewCtrl, Config config) {
        this(context, attrs, 0, pdfViewCtrl, config);
    }

    public UIAnnotToolbar(Context context, AttributeSet attrs, int defStyleAttr, PDFViewCtrl pdfViewCtrl, Config config) {
        this(context, attrs, defStyleAttr, 0, pdfViewCtrl, config);
    }

    public UIAnnotToolbar(final Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, PDFViewCtrl pdfViewCtrl, Config config) {
        super(context, attrs, defStyleAttr);
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
        mRootView = (RelativeLayout) View.inflate(context, R.layout.rd_new_annot_toolbar, null);
        mContainerLayout = mRootView.findViewById(R.id.annot_bar_container);

        mDividerHeight = AppDisplay.getInstance(mContext).dp2px(30);
        mDividerWidth = AppDisplay.getInstance(mContext).dp2px(0.5f);

        mAnnotBar = new BaseBarImpl(context);
        RelativeLayout contentLayout = mRootView.findViewById(R.id.annot_bar_content_container);
        contentLayout.addView(mAnnotBar.getContentView(), 0);
        addView(mRootView);

        initConfig(config);
        initButtonList();
        initButtonItems(context);
        initButtonMore(context);
        layoutAnnotBar();

        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).registerStateChangeListener(mStatusChangeListener);
        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).registerToolHandlerChangedListener(mHandlerEventListener);
    }

    @Override
    public View getContentView() {
        return mRootView;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        onItemLayout();
    }

    @Override
    public void registerListener(IMT_MoreClickListener listener) {
        if (null == mListeners.get(listener.getType())) {
            this.mListeners.put(listener.getType(), listener);
        }
    }

    @Override
    public void unRegisterListener(IMT_MoreClickListener listener) {
        if (null != mListeners.get(listener.getType())) {
            this.mListeners.remove(listener.getType());
        }
    }

    private void initConfig(Config config) {
        AnnotationsConfig annotConfig = config.modules.getAnnotConfig();
        //
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_NOTE, annotConfig.isLoadNote());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_ATTACHMENT, annotConfig.isLoadFileattach());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_STAMP, annotConfig.isLoadStamp());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_IMAGE, annotConfig.isLoadImage());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_AUDIO, annotConfig.isLoadAudio());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_VIDEO, annotConfig.isLoadVideo());
        //
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_TYPEWRITER, annotConfig.isLoadTypewriter());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_CALLOUT, annotConfig.isLoadCallout());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_TEXTBOX, annotConfig.isLoadTextbox());
        //
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_HIGHLIGHT, annotConfig.isLoadHighlight());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_UNDERLINE, annotConfig.isLoadUnderline());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_SQUIGGLY, annotConfig.isLoadSquiggly());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_STRIKEOUT, annotConfig.isLoadStrikeout());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_REPLACE, annotConfig.isLoadReplaceText());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_INSERT, annotConfig.isLoadInsertText());
        //
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_LINE, annotConfig.isLoadDrawLine());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_ARROW, annotConfig.isLoadDrawArrow());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_POLYLINE, annotConfig.isLoadDrawPolyLine());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_SQUARE, annotConfig.isLoadDrawSquare());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_CIRCLE, annotConfig.isLoadDrawCircle());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_POLYGON, annotConfig.isLoadDrawPolygon());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_CLOUD, annotConfig.isLoadDrawCloud());
        //
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_INK, annotConfig.isLoadDrawPencil());
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_ERASER, annotConfig.isLoadEraser());
        //
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_DISTANCE, annotConfig.isLoadDrawDistance());
        //
        mAnnotConfigArrays.put(MoreTools.MT_TYPE_MULTI_SELECT, config.modules.isLoadMultiSelect());
    }

    private void initButtonList() {
        // button tag and drawable maps
        {
            mBtnDrawableMap.put(MoreTools.MT_TYPE_NOTE, R.drawable.mt_iv_note);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_ATTACHMENT, R.drawable.annot_fileattachment_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_STAMP, R.drawable._feature_annot_stamp_moretools);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_IMAGE, R.drawable.annot_image_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_AUDIO, R.drawable.annot_audio);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_VIDEO, R.drawable.annot_video);
            //
            mBtnDrawableMap.put(MoreTools.MT_TYPE_TYPEWRITER, R.drawable.annot_typewriter_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_CALLOUT, R.drawable.annot_callout_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_TEXTBOX, R.drawable.annot_textbox_normal);
            //
            mBtnDrawableMap.put(MoreTools.MT_TYPE_HIGHLIGHT, R.drawable.annot_highlight_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_UNDERLINE, R.drawable.annot_unl_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_SQUIGGLY, R.drawable.annot_sqg_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_STRIKEOUT, R.drawable.annot_sto_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_REPLACE, R.drawable.mt_iv_replace);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_INSERT, R.drawable.mt_iv_insert);
            //
            mBtnDrawableMap.put(MoreTools.MT_TYPE_LINE, R.drawable.mt_iv_line);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_ARROW, R.drawable.mt_iv_arrow);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_POLYLINE, R.drawable.annot_polyline_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_SQUARE, R.drawable.annot_square_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_CIRCLE, R.drawable.annot_circle_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_POLYGON, R.drawable.annot_polygon_normal);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_CLOUD, R.drawable.annot_polygoncloud_normal);
            //
            mBtnDrawableMap.put(MoreTools.MT_TYPE_INK, R.drawable.mt_iv_pencil);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_ERASER, R.drawable.mt_eraser_normal);
            //
            mBtnDrawableMap.put(MoreTools.MT_TYPE_DISTANCE, R.drawable.icon_annot_distance_normal);
            //
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MULTI_SELECT, R.drawable.mt_iv_multi_select);

            // white
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_NOTE, R.drawable.mt_iv_note_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_ATTACHMENT, R.drawable.annot_fileattachment_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_STAMP, R.drawable._feature_annot_stamp_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_IMAGE, R.drawable.annot_image_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_AUDIO, R.drawable.annot_audio_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_VIDEO, R.drawable.annot_video_white);
            //
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_TYPEWRITER, R.drawable.annot_typewriter_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_CALLOUT, R.drawable.annot_callout_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_TEXTBOX, R.drawable.annot_textbox_white);
            //
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_HIGHLIGHT, R.drawable.annot_highlight_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_UNDERLINE, R.drawable.annot_unl_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_SQUIGGLY, R.drawable.annot_sqg_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_STRIKEOUT, R.drawable.annot_sto_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_REPLACE, R.drawable.mt_iv_replace_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_INSERT, R.drawable.mt_iv_insert_white);
            //
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_INK, R.drawable.mt_iv_pencil_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_ERASER, R.drawable.mt_eraser_white);
            //
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_LINE, R.drawable.mt_iv_line_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_ARROW, R.drawable.mt_iv_arrow_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_POLYLINE, R.drawable.annot_polyline_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_SQUARE, R.drawable.annot_square_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_CIRCLE, R.drawable.annot_circle_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_POLYGON, R.drawable.annot_polygon_white);
            mBtnDrawableMap.put(MoreTools.MT_TYPE_MORE_CLOUD, R.drawable.annot_polygoncloud_white);
        }

        // white to black tag map
        {
            //
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_NOTE, MoreTools.MT_TYPE_NOTE);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_ATTACHMENT, MoreTools.MT_TYPE_ATTACHMENT);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_STAMP, MoreTools.MT_TYPE_STAMP);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_IMAGE, MoreTools.MT_TYPE_IMAGE);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_AUDIO, MoreTools.MT_TYPE_AUDIO);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_VIDEO, MoreTools.MT_TYPE_VIDEO);
            //
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_TYPEWRITER, MoreTools.MT_TYPE_TYPEWRITER);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_CALLOUT, MoreTools.MT_TYPE_CALLOUT);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_TEXTBOX, MoreTools.MT_TYPE_TEXTBOX);
            //
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_HIGHLIGHT, MoreTools.MT_TYPE_HIGHLIGHT);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_UNDERLINE, MoreTools.MT_TYPE_UNDERLINE);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_SQUIGGLY, MoreTools.MT_TYPE_SQUIGGLY);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_STRIKEOUT, MoreTools.MT_TYPE_STRIKEOUT);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_REPLACE, MoreTools.MT_TYPE_REPLACE);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_INSERT, MoreTools.MT_TYPE_INSERT);
            //
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_INK, MoreTools.MT_TYPE_INK);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_ERASER, MoreTools.MT_TYPE_ERASER);
            //
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_LINE, MoreTools.MT_TYPE_LINE);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_ARROW, MoreTools.MT_TYPE_ARROW);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_POLYLINE, MoreTools.MT_TYPE_POLYLINE);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_SQUARE, MoreTools.MT_TYPE_SQUARE);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_CIRCLE, MoreTools.MT_TYPE_CIRCLE);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_POLYGON, MoreTools.MT_TYPE_POLYGON);
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_CLOUD, MoreTools.MT_TYPE_CLOUD);
            //
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_DISTANCE, MoreTools.MT_TYPE_DISTANCE);
            //
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_MULTI_SELECT, MoreTools.MT_TYPE_MULTI_SELECT);
            //
            mWhite2BlackTagMap.put(MoreTools.MT_TYPE_MORE_DIVIDER, MoreTools.MT_TYPE_DIVIDER);
        }

        int typeCounts = 7;
        ArrayList<ArrayList<Integer>> typeList = new ArrayList<>();
        for (int i = 0; i < typeCounts; i++) {
            typeList.add(new ArrayList<Integer>());
        }

        int size = mAnnotConfigArrays.size();
        for (int i = 0; i < size; i++) {
            boolean isLoaded = mAnnotConfigArrays.valueAt(i);
            if (isLoaded) {
                int tag = mAnnotConfigArrays.keyAt(i);
                switch (tag) {
                    //type1
                    case MoreTools.MT_TYPE_NOTE:
                        typeList.get(0).add(MoreTools.MT_TYPE_MORE_NOTE);
                        break;
                    case MoreTools.MT_TYPE_ATTACHMENT:
                        typeList.get(0).add(MoreTools.MT_TYPE_MORE_ATTACHMENT);
                        break;
                    case MoreTools.MT_TYPE_STAMP:
                        typeList.get(0).add(MoreTools.MT_TYPE_MORE_STAMP);
                        break;
                    case MoreTools.MT_TYPE_IMAGE:
                        typeList.get(0).add(MoreTools.MT_TYPE_MORE_IMAGE);
                        break;
                    case MoreTools.MT_TYPE_AUDIO:
                        typeList.get(0).add(MoreTools.MT_TYPE_MORE_AUDIO);
                        break;
                    case MoreTools.MT_TYPE_VIDEO:
                        typeList.get(0).add(MoreTools.MT_TYPE_MORE_VIDEO);
                        break;
                    //type2
                    case MoreTools.MT_TYPE_TYPEWRITER:
                        typeList.get(1).add(MoreTools.MT_TYPE_MORE_TYPEWRITER);
                        break;
                    case MoreTools.MT_TYPE_CALLOUT:
                        typeList.get(1).add(MoreTools.MT_TYPE_MORE_CALLOUT);
                        break;
                    case MoreTools.MT_TYPE_TEXTBOX:
                        typeList.get(1).add(MoreTools.MT_TYPE_MORE_TEXTBOX);
                        break;
                    //type3
                    case MoreTools.MT_TYPE_HIGHLIGHT:
                        typeList.get(2).add(MoreTools.MT_TYPE_MORE_HIGHLIGHT);
                        break;
                    case MoreTools.MT_TYPE_UNDERLINE:
                        typeList.get(2).add(MoreTools.MT_TYPE_MORE_UNDERLINE);
                        break;
                    case MoreTools.MT_TYPE_SQUIGGLY:
                        typeList.get(2).add(MoreTools.MT_TYPE_MORE_SQUIGGLY);
                        break;
                    case MoreTools.MT_TYPE_STRIKEOUT:
                        typeList.get(2).add(MoreTools.MT_TYPE_MORE_STRIKEOUT);
                        break;
                    case MoreTools.MT_TYPE_REPLACE:
                        typeList.get(2).add(MoreTools.MT_TYPE_MORE_REPLACE);
                        break;
                    case MoreTools.MT_TYPE_INSERT:
                        typeList.get(2).add(MoreTools.MT_TYPE_MORE_INSERT);
                        break;
                    //type4
                    case MoreTools.MT_TYPE_LINE:
                        typeList.get(3).add(MoreTools.MT_TYPE_MORE_LINE);
                        break;
                    case MoreTools.MT_TYPE_ARROW:
                        typeList.get(3).add(MoreTools.MT_TYPE_MORE_ARROW);
                        break;
                    case MoreTools.MT_TYPE_POLYLINE:
                        typeList.get(3).add(MoreTools.MT_TYPE_MORE_POLYLINE);
                        break;
                    case MoreTools.MT_TYPE_SQUARE:
                        typeList.get(3).add(MoreTools.MT_TYPE_MORE_SQUARE);
                        break;
                    case MoreTools.MT_TYPE_CIRCLE:
                        typeList.get(3).add(MoreTools.MT_TYPE_MORE_CIRCLE);
                        break;
                    case MoreTools.MT_TYPE_POLYGON:
                        typeList.get(3).add(MoreTools.MT_TYPE_MORE_POLYGON);
                        break;
                    case MoreTools.MT_TYPE_CLOUD:
                        typeList.get(3).add(MoreTools.MT_TYPE_MORE_CLOUD);
                        break;
                    //type5
                    case MoreTools.MT_TYPE_INK:
                        typeList.get(4).add(MoreTools.MT_TYPE_MORE_INK);
                        break;
                    case MoreTools.MT_TYPE_ERASER:
                        typeList.get(4).add(MoreTools.MT_TYPE_MORE_ERASER);
                        break;
                    //type 6
                    case MoreTools.MT_TYPE_DISTANCE:
                        typeList.get(5).add(MoreTools.MT_TYPE_MORE_DISTANCE);
                        break;
                    //type 7
                    case MoreTools.MT_TYPE_MULTI_SELECT:
                        typeList.get(6).add(MoreTools.MT_TYPE_MORE_MULTI_SELECT);
                        break;
                    default:
                        break;
                }
            }
        }

        for (int i = 0; i < typeList.size(); i++) {
            initButtonList(typeList.get(i));
        }
    }

    private void initButtonList(ArrayList<Integer> moreTags) {
        if (moreTags.size() > 0) {
            if (moreTags.contains(MoreTools.MT_TYPE_MORE_MULTI_SELECT) && mBtnMoreTags.size() > 0) {
                ArrayList<Integer> dividers = new ArrayList<>();
                dividers.add(MoreTools.MT_TYPE_MORE_DIVIDER);
                mBtnMoreTags.add(dividers);
                mBtnTags.add(MoreTools.MT_TYPE_DIVIDER);
            }
            mBtnMoreTags.add(moreTags);
            int tag = mWhite2BlackTagMap.get(moreTags.get(0));
            mBtnTags.add(tag);
        }
    }

    private void initButtonItems(final Context context) {
        for (int i = 0; i < mBtnTags.size(); i++) {
            IBaseItem btnItem;
            if (mBtnTags.get(i) == MoreTools.MT_TYPE_DIVIDER) {
                btnItem = createDividerButtonItem(context);
            } else {
                btnItem = new AnnotItemImpl(context);
                btnItem.setImageResource(mBtnDrawableMap.get(mBtnTags.get(i)));
            }
            btnItem.getContentView().setTag(mBtnTags.get(i));
            btnItem.setOnClickListener(mClickListener);

            ArrayList<Integer> moreTags = mBtnMoreTags.get(i);
            if (moreTags.size() > 1) {
                btnItem.setOnLongPressListener(mLongClickListener);
                btnItem.setBackgroundResource(R.drawable.annot_bar_tag_right);
            }
            mAnnotBar.addView(btnItem, BaseBar.TB_Position.Position_CENTER);
            mBtnItems.add(btnItem);
        }
    }

    private IBaseItem createDividerButtonItem(Context context) {
        mDividerView = new AppCompatImageView(context) {
            Paint mPaint = new Paint();
            RectF mRect = new RectF();

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                mPaint.setDither(true);
                mPaint.setAntiAlias(true);
                mPaint.setColor(mContext.getResources().getColor(R.color.ux_color_grey_ff878787));
                mPaint.setStyle(Paint.Style.FILL);

                int width = getWidth();
                int height = getHeight();

                mRect.set((width - mDividerWidth) / 2, (height - mDividerHeight) / 2,
                        (width + mDividerWidth) / 2, (height + mDividerHeight) / 2);
                canvas.drawRect(mRect, mPaint);
            }
        };

        mDividerView.setMinimumWidth((int) (mDividerWidth + 2));
        mDividerView.setMinimumHeight((int) (mDividerHeight + 2));
        mDividerItem = new BaseItemImpl(context, mDividerView);
        return mDividerItem;
    }

    private void initButtonMore(Context context) {
        for (int i = 0; i < mBtnTags.size(); i++) {
            ArrayList<Integer> moreTags = mBtnMoreTags.get(i);

            if (moreTags == null || moreTags.size() <= 1) {
                mBtnMoreItems.add(null);
                mBtnMoreBars.add(null);
                continue;
            }

            ArrayList<IBaseItem> moreItems = new ArrayList<>();
            BaseBarImpl baseBar = new BaseBarImpl(context);
            baseBar.setBackgroundResource(R.color.ux_color_translucent);
            baseBar.setItemInterval(AppDisplay.getInstance(context).dp2px(0.5f));
            baseBar.setHeight(AppDisplay.getInstance(context).dp2px(mBlackPopoverHeightDp));

            for (int j = 0; j < moreTags.size(); j++) {
                IBaseItem btnItem = new BaseItemImpl(context);
                btnItem.setImageResource(mBtnDrawableMap.get(moreTags.get(j)));
                btnItem.getContentView().setTag(moreTags.get(j));
                btnItem.setOnClickListener(mClickListener);
                btnItem.setImagePadding(AppDisplay.getInstance(context).dp2px(8), 0, AppDisplay.getInstance(context).dp2px(8), 0);
                if (j == 0) {
                    btnItem.setBackgroundResource(R.drawable.black_popover_bg_leftbtn);
                } else if (j == moreTags.size() - 1) {
                    btnItem.setBackgroundResource(R.drawable.black_popover_bg_rightbtn);
                } else {
                    btnItem.setBackgroundResource(R.color.ux_color_black_popover_bg);
                }

                moreItems.add(btnItem);
                baseBar.addView(btnItem, BaseBar.TB_Position.Position_CENTER);
                baseBar.resetMargin(0, 0);
            }

            mBtnMoreItems.add(moreItems);
            mBtnMoreBars.add(baseBar);
        }
    }

    private void layoutAnnotBar() {
        if (mDividerView != null) {
            mDividerWidth = AppDisplay.getInstance(mContext).dp2px(0.5f);
            mDividerHeight = AppDisplay.getInstance(mContext).dp2px(30);
            mDividerView.setMinimumWidth((int) (mDividerWidth + 2));
            mDividerView.setMinimumHeight((int) (mDividerHeight + 2));
            mDividerView.invalidate();
        }
        mAnnotBar.setOrientation(BaseBar.HORIZONTAL, LayoutParams.MATCH_PARENT, AppResource.getDimensionPixelSize(mContext, R.dimen.ux_bottombar_height));
        mAnnotBar.updateLayout();

        Point size = mAnnotBar.measureSize();
        LayoutParams containerLp = new LayoutParams(0, 0);
        mContainerLayout.setPadding(0, 0, 0, 0);
        containerLp.width = LayoutParams.MATCH_PARENT;
        containerLp.height = size.y + AppResource.getDimensionPixelSize(mContext,R.dimen.ux_toolbar_solidLine_height) + AppResource.getDimensionPixelSize(mContext,R.dimen.ux_toolbar_shadowLine_height);
        containerLp.setMargins(0, 0, 0, 0);
        containerLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        containerLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mContainerLayout.setLayoutParams(containerLp);
    }

    private int getArrowPosition() {
        return UIPopover.ARROW_BOTTOM;
    }

    private boolean onItemClicked(IBaseItem item) {
        int tag = (int) item.getContentView().getTag();
        if (tag == 0)
            return false;
        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).triggerDismissMenuEvent();

        IMT_MoreClickListener annotSupply = mListeners.get(tag);
        if (annotSupply != null) {
            for (int i = 0; i < mBtnItems.size(); i++) {
                if (mBtnItems.get(i) != item) {
                    mBtnItems.get(i).setChecked(false);
                } else {
                    item.setChecked(!item.isChecked());
                    if (item.isChecked()) {
                        mCurItem = item;
                    } else {
                        mCurItem = null;
                    }
                }
            }
            mListeners.get(tag).onMTClick(tag);
            return true;
        }
        return false;
    }

    private IBaseItem.OnClickListener mClickListener = new IBaseItem.OnClickListener() {
        @Override
        public void onClick(IBaseItem item, View v) {
            int tag = (int) item.getContentView().getTag();
            if (tag == 0 || onItemClicked(item)) return;

            switch (tag) {
                //
                case MoreTools.MT_TYPE_MORE_NOTE:
                case MoreTools.MT_TYPE_MORE_ATTACHMENT:
                case MoreTools.MT_TYPE_MORE_STAMP:
                case MoreTools.MT_TYPE_MORE_IMAGE:
                case MoreTools.MT_TYPE_MORE_AUDIO:
                case MoreTools.MT_TYPE_MORE_VIDEO:
                    //
                case MoreTools.MT_TYPE_MORE_TYPEWRITER:
                case MoreTools.MT_TYPE_MORE_CALLOUT:
                case MoreTools.MT_TYPE_MORE_TEXTBOX:
                    //
                case MoreTools.MT_TYPE_MORE_HIGHLIGHT:
                case MoreTools.MT_TYPE_MORE_UNDERLINE:
                case MoreTools.MT_TYPE_MORE_SQUIGGLY:
                case MoreTools.MT_TYPE_MORE_STRIKEOUT:
                case MoreTools.MT_TYPE_MORE_REPLACE:
                case MoreTools.MT_TYPE_MORE_INSERT:
                    //
                case MoreTools.MT_TYPE_MORE_INK:
                case MoreTools.MT_TYPE_MORE_ERASER:
                    //
                case MoreTools.MT_TYPE_MORE_LINE:
                case MoreTools.MT_TYPE_MORE_ARROW:
                case MoreTools.MT_TYPE_MORE_POLYLINE:
                case MoreTools.MT_TYPE_MORE_SQUARE:
                case MoreTools.MT_TYPE_MORE_CIRCLE:
                case MoreTools.MT_TYPE_MORE_POLYGON:
                case MoreTools.MT_TYPE_MORE_CLOUD: {
                    boolean finded = false;
                    for (int i = 0; i < mBtnMoreItems.size(); i++) {
                        ArrayList<IBaseItem> moreItems = mBtnMoreItems.get(i);
                        if (moreItems != null) {
                            for (int j = 0; j < moreItems.size(); j++) {
                                IBaseItem moreBtn = moreItems.get(j);
                                if (moreBtn == item) {
                                    if (mCurItem != null) {
                                        onItemClicked(mCurItem);
                                    }
                                    int blackTag = mWhite2BlackTagMap.get(tag);
                                    int blackDrawable = mBtnDrawableMap.get(blackTag);

                                    IBaseItem btnItem = mBtnItems.get(i);
                                    btnItem.getContentView().setTag(blackTag);
                                    btnItem.setImageResource(blackDrawable);

                                    mBtnTags.remove(i);
                                    mBtnTags.add(i, blackTag);
                                    onItemClicked(btnItem);
                                    finded = true;
                                    break;
                                }
                            }

                            if (finded)
                                break;
                        }
                    }
                    if (mBlackPopover != null && mBlackPopover.isShowing()) {
                        mBlackPopover.dismiss();
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    private UIPopover mBlackPopover;
    private RelativeLayout mBlackRootView;
    private BaseBarImpl mCurBlackBar;
    private IBaseItem mCurBlackItem;
    private IBaseItem.onLongPressListener mLongClickListener = new IBaseItem.onLongPressListener() {
        @Override
        public boolean onLongPress(IBaseItem item, View v) {
            int tag = (int) item.getContentView().getTag();
            if (tag == 0) return false;

            int index = mBtnItems.indexOf(item);
            if (index >= 0) {
                mCurBlackBar = mBtnMoreBars.get(index);
                mCurBlackItem = item;

                if (mCurBlackBar != null) {
                    if (mBlackPopover == null) {
                        mBlackRootView = new RelativeLayout(mContext.getApplicationContext());

                        Activity activity = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                        mBlackPopover = UIPopover.create((FragmentActivity) activity, mBlackRootView, true, true);
                    }

                    mBlackRootView.removeAllViews();
                    mBlackRootView.addView(mCurBlackBar.getContentView());

                    Point size = mCurBlackBar.measureSize();
                    Rect rect = new Rect();
                    item.getContentView().getGlobalVisibleRect(rect);

                    int arrowPos = getArrowPosition();
                    mBlackPopover.showAtLocation(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), rect,
                            size.x + 32, AppDisplay.getInstance(mContext).dp2px(mBlackPopoverHeightDp),
                            arrowPos, AppDisplay.getInstance(mContext).dp2px(12));
                }
                return true;
            }

            return false;
        }
    };

    private void onItemLayout() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();

        if (mBlackPopover != null && mBlackPopover.isShowing() && mCurBlackBar != null && mCurBlackItem != null) {
            Point size = mCurBlackBar.measureSize();
            Rect rect = new Rect();
            boolean success = mCurBlackItem.getContentView().getGlobalVisibleRect(rect);
            if (!success) {
                int[] location = new int[2];
                mCurBlackItem.getContentView().getLocationInWindow(location);
                rect.set(location[0], location[1], location[0] + mCurBlackItem.getContentView().getWidth(),
                        location[1] + mCurBlackItem.getContentView().getHeight());
            }

            int arrowPos = getArrowPosition();
            mBlackPopover.showAtLocation(uiExtensionsManager.getRootView(), rect,
                    size.x + 32, AppDisplay.getInstance(mContext).dp2px(mBlackPopoverHeightDp),
                    arrowPos, AppDisplay.getInstance(mContext).dp2px(12));
        }
    }

    private UIExtensionsManager.ToolHandlerChangedListener mHandlerEventListener = new UIExtensionsManager.ToolHandlerChangedListener() {
        @Override
        public void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
            if (currentTool == null) {
                if (mCurItem != null) {
                    mCurItem.setChecked(false);
                    mCurItem = null;
                }
            }
        }
    };

    private IStateChangeListener mStatusChangeListener = new IStateChangeListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            if (ReadStateConfig.STATE_EDIT == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getState()) {
                ToolHandler toolHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler();
                if (toolHandler == null && mCurItem != null) {
                    mCurItem.setChecked(false);
                    mCurItem = null;
                }
            }
        }
    };

    public void setWidth(int width){
        mAnnotBar.setWidth(width);
    }

    public void updateLayout(){
        mAnnotBar.updateLayout();
    }

}
