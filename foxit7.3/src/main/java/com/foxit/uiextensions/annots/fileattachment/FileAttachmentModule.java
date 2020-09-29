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
package com.foxit.uiextensions.annots.fileattachment;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.PropertyCircleItemImp;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.io.File;


public class FileAttachmentModule implements Module, PropertyBar.PropertyChangeListener {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private FileAttachmentAnnotHandler mAnnotHandler;
    private FileAttachmentToolHandler mToolHandler;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private PropertyBar mPropertyBar;

    @Override
    public String getName() {
        return Module.MODULE_NAME_FILEATTACHMENT;
    }

    public FileAttachmentModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    @Override
    public boolean loadModule() {
        mToolHandler = new FileAttachmentToolHandler(mContext, mPdfViewCtrl);
        mCurrentColor = PropertyBar.PB_COLORS_FILEATTACHMENT[0];
        mCurrentOpacity = 100;
        mFlagType = FileAttachmentConstants.ICONTYPE_PUSHPIN;

        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl);
        String[] typeNames = new String[]{
                mContext.getApplicationContext().getString(R.string.annot_fat_icontext_graph),
                mContext.getApplicationContext().getString(R.string.annot_fat_icontext_paperclip),
                mContext.getApplicationContext().getString(R.string.annot_fat_icontext_pushpin),
                mContext.getApplicationContext().getString(R.string.annot_fat_icontext_tag)};
        mAdapter = new FileAttachmentPBAdapter(mContext, mTypePicIds, typeNames);

        mAnnotHandler = new FileAttachmentAnnotHandler(mContext, mPdfViewCtrl, this);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            Config config = ((UIExtensionsManager) mUiExtensionsManager).getConfig();
            mCurrentColor = config.uiSettings.annotations.attachment.color;
            mCurrentOpacity = (int) (config.uiSettings.annotations.attachment.opacity * 100);
            String iconName = config.uiSettings.annotations.attachment.icon;
            mFlagType = FileAttachmentUtil.getIconType(iconName);

            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);

            ((UIExtensionsManager) mUiExtensionsManager).getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
                @Override
                public void onMTClick(int type) {
                    if (type == MoreTools.MT_TYPE_ATTACHMENT) {
                        ((UIExtensionsManager) mUiExtensionsManager).setCurrentToolHandler(mToolHandler);
                        ((UIExtensionsManager) mUiExtensionsManager).changeState(ReadStateConfig.STATE_ANNOTTOOL);

                        resetAnnotBar();
                        resetPropertyBar();
                    }
                }

                @Override
                public int getType() {
                    return MoreTools.MT_TYPE_ATTACHMENT;
                }
            });
            ((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigureChangeListener);
        }
        mToolHandler.setOpacity(mCurrentOpacity);
        mToolHandler.setIconType(mFlagType);
        mToolHandler.setColor(mCurrentColor);

        mAdapter.setNoteIconType(mFlagType);
        mAnnotHandler.setToolHandler(mToolHandler);
        mAnnotHandler.setPropertyListViewAdapter(mAdapter);

        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigureChangeListener);
        }
        // delete temp files
        String tempPath = mAnnotHandler.getTmpPath();
        File tempFile = new File(tempPath);
        if (tempFile.exists()) {
            AppFileUtil.deleteFolder(tempFile, false);
        }
        return true;
    }

    private int mCurrentColor;
    private int mCurrentOpacity;
    private int mFlagType;

    @Override
    public void onValueChanged(long property, int value) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;
        AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
        ToolHandler currentToolHandler = uiExtensionsManager.getCurrentToolHandler();
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            if (currentToolHandler == mToolHandler) {
                mCurrentColor = value;
                mToolHandler.setColor(mCurrentColor);
                if (mPropertyItem != null)
                    mPropertyItem.setCentreCircleColor(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.modifyAnnotColor(value);
            }
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            if (currentToolHandler == mToolHandler) {
                mCurrentOpacity = value;
                mToolHandler.setOpacity(mCurrentOpacity);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.modifyAnnotOpacity(value);
            }
        }
    }

    @Override
    public void onValueChanged(long property, float value) {
    }

    @Override
    public void onValueChanged(long property, String value) {
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (mToolHandler.onKeyDown(keyCode, event) || mAnnotHandler.onKeyDown(keyCode, event));
    }

    public boolean onKeyBack() {
        return (mToolHandler.onKeyBack() || mAnnotHandler.onKeyBack());
    }

    private int[] mTypePicIds = new int[]{R.drawable.pb_fat_type_graph, R.drawable.pb_fat_type_paperclip, R.drawable.pb_fat_type_pushpin,
            R.drawable.pb_fat_type_tag};
    private int[] mPBColors = new int[PropertyBar.PB_COLORS_FILEATTACHMENT.length];
    private FileAttachmentPBAdapter mAdapter;

    private void resetPropertyBar() {
        final FileAttachmentToolHandler toolHandler = (FileAttachmentToolHandler) getToolHandler();
        long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
        System.arraycopy(PropertyBar.PB_COLORS_FILEATTACHMENT, 0, mPBColors, 0, mPBColors.length);
        mPBColors[0] = PropertyBar.PB_COLORS_FILEATTACHMENT[0];

        mPropertyBar.setColors(mPBColors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, toolHandler.getColor());

        int opacity = toolHandler.getOpacity();
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, opacity);
        mPropertyBar.reset(supportProperty);

        mPropertyBar.setPropertyChangeListener(this);
        mPropertyBar.addTab("", 0, mContext.getApplicationContext().getString(R.string.pb_icon_tab), 0);
        mPropertyBar.addCustomItem(PropertyBar.PROPERTY_FILEATTACHMENT, getIconTypeView(true), 0, 0);
        mAdapter.setNoteIconType(mFlagType);
    }

    protected View getIconTypeView(boolean canEdit) {
        //IconListView
        LinearLayout iconItem_ly = new LinearLayout(mContext);
        iconItem_ly.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        iconItem_ly.setGravity(Gravity.CENTER);
        iconItem_ly.setOrientation(LinearLayout.HORIZONTAL);

        ListView listView = new ListView(mContext);
        listView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        listView.setCacheColorHint(mContext.getResources().getColor(R.color.ux_color_translucent));

        listView.setDivider(new ColorDrawable(mContext.getResources().getColor(R.color.ux_color_seperator_gray)));
        listView.setDividerHeight(1);
        iconItem_ly.addView(listView);
        listView.setAdapter(mAdapter);

        if (canEdit) {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;
                    AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
                    ToolHandler currentToolHandler = uiExtensionsManager.getCurrentToolHandler();

                    if (mToolHandler == currentToolHandler) {
                        mFlagType = position;
                        mToolHandler.setIconType(mFlagType);
                    } else if (mAnnotHandler == currentAnnotHandler) {
                        mAnnotHandler.modifyIconType(position);
                    }
                    mAdapter.setNoteIconType(position);
                    mAdapter.notifyDataSetChanged();
                }
            });
        } else {
            listView.setEnabled(false);
            iconItem_ly.setAlpha(PropertyBar.PB_ALPHA);
        }
        return iconItem_ly;
    }


    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    private boolean mIsContinuousCreate = false;

    private void resetAnnotBar() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;
            uiExtensionsManager.getMainFrame().getToolSetBar().removeAllItems();
            mOKItem = new BaseItemImpl(mContext);
            mOKItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_OK);
            mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
            mOKItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    uiExtensionsManager.changeState(ReadStateConfig.STATE_EDIT);
                    uiExtensionsManager.setCurrentToolHandler(null);
                }
            });

            mPropertyItem = new PropertyCircleItemImp(mContext) {

                @Override
                public void onItemLayout(int l, int t, int r, int b) {
                    if (mToolHandler == uiExtensionsManager.getCurrentToolHandler()) {
                        if (mPropertyBar.isShowing()) {
                            Rect rect = new Rect();
                            mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                            mPropertyBar.update(new RectF(rect));
                        }
                    }
                }
            };
            mPropertyItem.setTag(ToolbarItemConfig.ITEM_ANNOT_PROPERTY);
            mPropertyItem.setCentreCircleColor(mToolHandler.getColor());

            final Rect rect = new Rect();
            mPropertyItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetPropertyBar();
                    mPropertyBar.setArrowVisible(true);
                    mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                    mPropertyBar.show(new RectF(rect), true);
                }
            });

            mContinuousCreateItem = new BaseItemImpl(mContext);
            mContinuousCreateItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_CONTINUE);
            mIsContinuousCreate = mToolHandler.isContinueAddAnnot();
            mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinuousCreate));

            mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AppUtil.isFastDoubleClick()) {
                        return;
                    }

                    mIsContinuousCreate = !mIsContinuousCreate;
                    mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinuousCreate));
                    mToolHandler.setContinueAddAnnot(mIsContinuousCreate);
                    AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
                }
            });

            uiExtensionsManager.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
            uiExtensionsManager.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
            uiExtensionsManager.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
        }
    }

    private int getContinuousIcon(boolean isContinuous) {
        int iconId;
        if (isContinuous) {
            iconId = R.drawable.rd_annot_create_continuously_true_selector;
        } else {
            iconId = R.drawable.rd_annot_create_continuously_false_selector;
        }
        return iconId;
    }

    private UIExtensionsManager.ConfigurationChangedListener mConfigureChangeListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            mToolHandler.onConfigurationChanged(newConfig);
        }
    };

}
