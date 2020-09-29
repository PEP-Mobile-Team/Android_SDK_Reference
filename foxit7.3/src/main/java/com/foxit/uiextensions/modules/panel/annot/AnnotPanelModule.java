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
package com.foxit.uiextensions.modules.panel.annot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.modules.panel.PanelWindow;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.OnPageEventListener;

import java.util.ArrayList;
import java.util.List;

/** All the annotations on the document would be listed on the annotation panel, so they could be easily reviewed, edited or deleted. */
public class AnnotPanelModule implements Module, PanelSpec {
    private PDFViewCtrl mPdfViewCtrl;
    private Context mContext;
    private AppDisplay mDisplay;
    private View mTopBarView;
    private Boolean mIsPad;
    private View mClearView;
    private UITextEditDialog mDialog;

    private View mContentView;
    private AnnotPanel mAnnotPanel;

    private boolean isTouchHold;
    private ArrayList<Boolean> mItemMoreViewShow;
    private TextView mNoInfoView;
    private TextView mSearchingTextView;
    private int mPausedPageIndex = 0;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private MenuItemAdapter mMenuAdapter;

    private PanelWindow mPanelWindow;

    public AnnotPanelModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        if (context == null || pdfViewCtrl == null) {
            throw new NullPointerException();
        }
        mContext = context.getApplicationContext();
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
        this.mItemMoreViewShow = new ArrayList<Boolean>();
        mDisplay = new AppDisplay(mContext);
        mIsPad = mDisplay.isPad();
    }

    public PopupWindow getPopupWindow() {
        return mPanelWindow.getPanelWindow();
    }

    public void show() {
        if (mPanelWindow != null) {
            mPanelWindow.show(PanelType.Annotations);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            mPanelWindow = new PanelWindow(mContext, mPdfViewCtrl, mUiExtensionsManager);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }

        mTopBarView = View.inflate(mContext, R.layout.panel_annot_topbar, null);
        View closeView = mTopBarView.findViewById(R.id.panel_annot_top_close_iv);
        TextView topTitle = (TextView) mTopBarView.findViewById(R.id.rv_panel_annot_title);
        mClearView = mTopBarView.findViewById(R.id.panel_annot_top_clear_tv);
        if (mIsPad) {
            closeView.setVisibility(View.GONE);
        } else {
            closeView.setVisibility(View.VISIBLE);
            closeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPanelWindow.isShowing()) {
                        mPanelWindow.dismiss();
                    }
                }
            });
        }
        View topNormalView = mTopBarView.findViewById(R.id.panel_annot_top_normal);
        topNormalView.setVisibility(View.VISIBLE);

        if (mIsPad) {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            closeView.setLayoutParams(topCloseLayoutParams);
            RelativeLayout.LayoutParams topClearLayoutParams = (RelativeLayout.LayoutParams) mClearView.getLayoutParams();
            topClearLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            mClearView.setLayoutParams(topClearLayoutParams);
        } else {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topTitleLayoutParams = (RelativeLayout.LayoutParams) topTitle.getLayoutParams();
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            topTitleLayoutParams.leftMargin = mDisplay.dp2px(70.0f);
            topTitle.setLayoutParams(topTitleLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            closeView.setLayoutParams(topCloseLayoutParams);
            RelativeLayout.LayoutParams topClearLayoutParams = (RelativeLayout.LayoutParams) mClearView.getLayoutParams();
            topClearLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            mClearView.setLayoutParams(topClearLayoutParams);
        }

        mClearView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mMoreMenuView.setVisibility(View.VISIBLE);
            }
        });

        mContentView = View.inflate(mContext, R.layout.panel_annot_content, null);
        mNoInfoView = (TextView) mContentView.findViewById(R.id.rv_panel_annot_noinfo);
        mSearchingTextView = (TextView) mContentView.findViewById(R.id.rv_panel_annot_searching);
        ListView listView = (ListView) mContentView.findViewById(R.id.rv_panel_annot_list);

        mAnnotPanel = new AnnotPanel(this, mContext, mPdfViewCtrl, mContentView, mItemMoreViewShow);
        listView.setAdapter(mAnnotPanel.getAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (AppUtil.isFastDoubleClick()) return;
                if (mAnnotPanel.jumpToPage(position)) {
                    if (mPanelWindow.isShowing()) {
                        mPanelWindow.dismiss();
                    }
                }
            }
        });
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        boolean show = false;
                        int position = 0;
                        for (int i = 0; i < mAnnotPanel.getAdapter().getCount(); i++) {
                            if (mItemMoreViewShow.get(i)) {
                                show = true;
                                position = i;
                                break;
                            }
                        }
                        if (show) {
                            mAnnotPanel.getAdapter().getSelectedGroupNodeList().clear();
                            mItemMoreViewShow.set(position, false);
                            mAnnotPanel.getAdapter().notifyDataSetChanged();
                            isTouchHold = true;
                            return true;
                        }
                    case MotionEvent.ACTION_UP:

                    case MotionEvent.ACTION_CANCEL:
                        if (isTouchHold) {
                            isTouchHold = false;
                            return true;
                        }
                    default:

                }
                return false;
            }
        });

        mPanelWindow.addPanelSpec(this);
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);

        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerAnnotEventListener(mAnnotPanel.getAnnotEventListener());
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerFlattenEventListener(mAnnotPanel.getFlattenEventListener());
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerRedactionEventListener(mAnnotPanel.getRedactionEventListener());
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerImportedAnnotsEventListener(mAnnotPanel.getImportAnnotsListener());
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerGroupEventListener(mAnnotPanel.getGroupEventListener());
            ((UIExtensionsManager) mUiExtensionsManager).registerLayoutChangeListener(mLayoutChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerMenuEventListener(mMenuEventListener);
        }

        initMoreMenuPop();
        return true;
    }

    private LinearLayout mMoreMenuView;

    private void initMoreMenuPop() {
        ListView lv_menu = new ListView(mContext);
        lv_menu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        lv_menu.setCacheColorHint(mContext.getResources().getColor(R.color.ux_color_white));
        lv_menu.setDivider(new ColorDrawable(mContext.getResources().getColor(R.color.ux_color_seperator_gray)));
        lv_menu.setDividerHeight(1);

        List<MenuItemAdapter.ItemInfo> itemInfos = new ArrayList<>();
        MenuItemAdapter.ItemInfo applyInfo = new MenuItemAdapter.ItemInfo(mContext.getApplicationContext().getString(R.string.fx_string_apply_all), true);
        itemInfos.add(applyInfo);
        MenuItemAdapter.ItemInfo clearInfo = new MenuItemAdapter.ItemInfo(mContext.getApplicationContext().getString(R.string.hm_clear), true);
        itemInfos.add(clearInfo);
        mMenuAdapter = new MenuItemAdapter(mContext, itemInfos);
        lv_menu.setAdapter(mMenuAdapter);

        int totalWidth = 0;
        int totalHeight = 0;
        for (int i = 0; i < mMenuAdapter.getCount(); i++) {
            View listItem = mMenuAdapter.getView(i, null, lv_menu);
//             listItem.measure(0, 0);
            listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalWidth += listItem.getMeasuredWidth();
            totalHeight += listItem.getMeasuredHeight();
        }
        lv_menu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                MenuItemAdapter.ItemInfo itemInfo = (MenuItemAdapter.ItemInfo) parent.getItemAtPosition(position);
                if (itemInfo.enabled) {
                    mMoreMenuView.setVisibility(View.GONE);
                    UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
                    final Context context = uiExtensionsManager.getAttachedActivity();
                    //default position = 0
                    String title = AppResource.getString(context.getApplicationContext(), R.string.fx_string_warning);
                    String prompt = AppResource.getString(context.getApplicationContext(), R.string.fx_string_redact_apply_toast);
                    if (position == 1) {
                        title = AppResource.getString(context.getApplicationContext(), R.string.hm_clear);
                        prompt = AppResource.getString(context.getApplicationContext(), R.string.rd_panel_clear_comment);
                    }

                    mDialog = new UITextEditDialog(context);
                    mDialog.setTitle(title);
                    mDialog.getPromptTextView().setText(prompt);
                    mDialog.getInputEditText().setVisibility(View.GONE);
                    mDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (position == 0) {
                                mClearView.setEnabled(false);
                                mAnnotPanel.applyAllRedaction(new Event.Callback() {
                                    @Override
                                    public void result(Event event, boolean success) {
                                        //reloaded the annot list
                                        mAnnotPanel.prepare();
                                        mClearView.setEnabled(false);
                                        mAnnotPanel.startSearch(0);
                                    }
                                });
                            } else {
                                mAnnotPanel.clearAllNodes();
                            }
                            mDialog.dismiss();
                        }
                    });
                    mDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mDialog.dismiss();
                        }
                    });
                    mDialog.show();
                }
            }
        });

        mMoreMenuView = new LinearLayout(mContext);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        mMoreMenuView.setLayoutParams(params);
        mMoreMenuView.setGravity(Gravity.RIGHT);
        mMoreMenuView.setBackgroundColor(mContext.getResources().getColor(R.color.ux_color_translucent));
        mPanelWindow.getPanelHost().getContentView().addView(mMoreMenuView);

        LinearLayout contentLayout = new LinearLayout(mContext);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                totalWidth, totalHeight);
        contentParams.gravity = Gravity.TOP | Gravity.RIGHT;
        contentParams.setMargins(0, 16, 16, 0);
        contentLayout.setLayoutParams(contentParams);
        contentLayout.addView(lv_menu);
        contentLayout.setBackgroundColor(mContext.getResources().getColor(R.color.ux_color_white));

        mMoreMenuView.addView(contentLayout);
        mMoreMenuView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (View.VISIBLE == mMoreMenuView.getVisibility()) {
                    mMoreMenuView.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        });
        mMoreMenuView.setVisibility(View.GONE);
    }

    private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener() {

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {
            for (int i = 0; i < pageIndexes.length; i++)
                mAnnotPanel.getAdapter().onPageRemoved(success, pageIndexes[i] - i);
        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {
            mAnnotPanel.getAdapter().onPageMoved(success, index, dstIndex);
        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
            //when we inserted pages or docs, the annotlist should be reloaded.
            mAnnotPanel.prepare();
            mAnnotPanel.startSearch(0);
        }
    };

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode == Constants.e_ErrSuccess) {
                prepare();
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
            mAnnotPanel.onDocWillClose();
            mSearchingTextView.setVisibility(View.GONE);
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            mMoreMenuView.setVisibility(View.GONE);
            mSearchingTextView.setVisibility(View.GONE);
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (mPanelWindow != null && mPanelWindow.isShowing() && mPanelWindow.getCurrentPanelSpec() == AnnotPanelModule.this) {
                if (oldWidth != newWidth || oldHeight != newHeight) {
                    mPanelWindow.update(newWidth, newHeight);
                }
            }
        }
    };

    public void prepare() {
        mAnnotPanel.prepare();
    }

    @Override
    public boolean unloadModule() {
        mPanelWindow.removePanelSpec(this);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterAnnotEventListener(mAnnotPanel.getAnnotEventListener());
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterFlattenEventListener(mAnnotPanel.getFlattenEventListener());
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterRedactionEventListener(mAnnotPanel.getRedactionEventListener());
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterImportedAnnotsEventListener(mAnnotPanel.getImportAnnotsListener());
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterGroupEventListener(mAnnotPanel.getGroupEventListener());
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLayoutChangeListener(mLayoutChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterMenuEventListener(mMenuEventListener);
        }
        return true;
    }

    @Override
    public String getName() {
        return MODULE_NAME_ANNOTPANEL;
    }

    private void resetClearButton() {
        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()
                || mAnnotPanel.getCurrentStatus() == AnnotPanel.STATUS_LOADING
                || mAnnotPanel.getCurrentStatus() == AnnotPanel.STATUS_PAUSED) {
            mClearView.setEnabled(false);
        } else {
            mClearView.setVisibility(View.VISIBLE);
            if (mAnnotPanel.getCount() > 0) {
                mClearView.setEnabled(true);
                resetApplyStatus();
            } else {
                mClearView.setEnabled(false);
            }
        }
    }

    protected void resetApplyStatus() {
        List<MenuItemAdapter.ItemInfo> itemInfos = mMenuAdapter.getItems();
        if (!AppAnnotUtil.hasModuleLicenseRight(Constants.e_ModuleNameRedaction)
                || ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isSign()
                || !((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyContents()) {
            itemInfos.get(0).enabled = false;
        } else {
            itemInfos.get(0).enabled = mAnnotPanel.getRedactCount() > 0;
        }
        mMenuAdapter.notifyDataSetChanged();
    }

    public void pauseSearch(int pageIndex) {
        mPausedPageIndex = pageIndex;
    }

    public void showNoAnnotsView() {
        mClearView.setEnabled(false);
        mNoInfoView.setText(AppResource.getString(mContext, R.string.rv_panel_annot_noinformation));
        mNoInfoView.setVisibility(View.VISIBLE);
    }

    public void hideNoAnnotsView() {
        if (mPdfViewCtrl.getDoc() != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
            mClearView.setEnabled(true);
            resetApplyStatus();
        }
        if (mNoInfoView.getVisibility() == View.GONE) return;
        mNoInfoView.setVisibility(View.GONE);
    }

    public void updateLoadedPage(int curPageIndex, int total) {
        if (mAnnotPanel.getCurrentStatus() == AnnotPanel.STATUS_DONE) {
            if (curPageIndex == 0 && total == 0) {
                mSearchingTextView.setVisibility(View.GONE);
            }
            if (mSearchingTextView.isShown()) mSearchingTextView.setVisibility(View.GONE);

            mNoInfoView.setText(mContext.getString(R.string.rv_panel_annot_noinformation));
            if (mAnnotPanel.getCount() > 0) {
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                    mClearView.setEnabled(true);
                    resetApplyStatus();
                }
                mNoInfoView.setVisibility(View.GONE);
            } else {
                mClearView.setEnabled(false);
                mNoInfoView.setVisibility(View.VISIBLE);
            }
        } else if (mAnnotPanel.getCurrentStatus() == AnnotPanel.STATUS_FAILED) {
            mSearchingTextView.setVisibility(View.GONE);
            if (mAnnotPanel.getCount() == 0) {
                mNoInfoView.setText(mContext.getString(R.string.rv_panel_annot_noinformation));
                mNoInfoView.setVisibility(View.VISIBLE);
                mClearView.setEnabled(false);
            } else {
                mClearView.setEnabled(true);
                resetApplyStatus();
            }
        } else {
            mNoInfoView.setVisibility(View.GONE);
            if (!mSearchingTextView.isShown()) mSearchingTextView.setVisibility(View.VISIBLE);
            mSearchingTextView.setText(AppResource.getString(mContext, R.string.rv_panel_annot_item_pagenum) + ": " + curPageIndex + " / " + total);
        }
    }

    @Override
    public PanelType getPanelType() {
        return PanelType.Annotations;
    }

    @Override
    public int getIcon() {
        return R.drawable.panel_tabing_annotation_selector;
    }

    @Override
    public View getTopToolbar() {
        return mTopBarView;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }

    @Override
    public void onActivated() {
        resetClearButton();
        switch (mAnnotPanel.getCurrentStatus()) {
            case AnnotPanel.STATUS_CANCEL:
                mNoInfoView.setText(mContext.getString(R.string.rv_panel_annot_loading_start));
                mNoInfoView.setVisibility(View.VISIBLE);
                mAnnotPanel.startSearch(0);

                break;
            case AnnotPanel.STATUS_LOADING:
                mAnnotPanel.setStatusPause(false);

                break;
            case AnnotPanel.STATUS_DONE:
                if (mSearchingTextView.getVisibility() != View.GONE)
                    mSearchingTextView.setVisibility(View.GONE);
                if (mAnnotPanel.getCount() > 0) {
                    mNoInfoView.setVisibility(View.GONE);
                } else {
                    mNoInfoView.setVisibility(View.VISIBLE);
                }
                break;
            case AnnotPanel.STATUS_PAUSED:
                mAnnotPanel.setStatusPause(false);
                mAnnotPanel.startSearch(mPausedPageIndex);
                break;
            case AnnotPanel.STATUS_FAILED:
            case AnnotPanel.STATUS_DELETING:
            case AnnotPanel.STATUS_REDACTING:
            default:
                break;
        }
        if (mAnnotPanel.getAdapter().hasDataChanged()) {
            if (mAnnotPanel.getAdapter().isEmpty())
                showNoAnnotsView();
            mAnnotPanel.getAdapter().notifyDataSetChanged();
            mAnnotPanel.getAdapter().resetDataChanged();
        }
    }

    @Override
    public void onDeactivated() {
        if (View.VISIBLE == mMoreMenuView.getVisibility())
            mMoreMenuView.setVisibility(View.GONE);

        if (mAnnotPanel.getCurrentStatus() == AnnotPanel.STATUS_LOADING) {
            mAnnotPanel.setStatusPause(true);
        }
    }

    private UIExtensionsManager.MenuEventListener mMenuEventListener = new UIExtensionsManager.MenuEventListener() {
        @Override
        public void onTriggerDismissMenu() {
            if (mMoreMenuView != null && View.VISIBLE == mMoreMenuView.getVisibility()) {
                mMoreMenuView.setVisibility(View.GONE);
            }
        }
    };

}
