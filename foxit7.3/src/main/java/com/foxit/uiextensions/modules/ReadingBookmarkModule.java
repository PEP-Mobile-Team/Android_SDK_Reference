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
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.view.MotionEventCompat;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.panel.PanelHost;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.controls.panel.impl.PanelHostImpl;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.LayoutConfig;
import com.foxit.uiextensions.utils.OnPageEventListener;

import java.util.ArrayList;
/** The module enable user to add or delete their own customized bookmark to the PDF document.*/
public class ReadingBookmarkModule implements Module, PanelSpec {
    private boolean mIsReadingBookmark = false;
    protected PDFViewCtrl mPdfViewCtrl;
    private Context mContext;
    private AppDisplay mDisplay;
    private View mTopBarView;
    private Boolean mIsPad;
    private View mClearView;
    private UITextEditDialog mDialog;
    private ArrayList<IBaseItem> mMarkItemList;
    protected View mContentView;
    private RelativeLayout mReadingMarkContent;

    private ListView mReadingBookmarkListView;
    private TextView mReadingBookmarkNoInfoTv;
    private ReadingBookmarkSupport mSupport;

    private boolean isTouchHold;
    protected ArrayList<Boolean> mItemMoreViewShow;
    private PanelHost mPanelHost;
    private PopupWindow mPanelPopupWindow = null;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    private IBaseItem mBookmarkAddButton;

    public ReadingBookmarkModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        if (context == null || pdfViewCtrl == null) {
            throw new NullPointerException();
        }
        mContext = context.getApplicationContext();
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
        this.mItemMoreViewShow = new ArrayList<Boolean>();
        mMarkItemList = new ArrayList<IBaseItem>();
        mDisplay = new AppDisplay(mContext);
        mIsPad = mDisplay.isPad();
    }

    public void setPanelHost(PanelHost panelHost) {
        mPanelHost = panelHost;
    }

    public PanelHost getPanelHost(){
        return mPanelHost;
    }

    public void setPopupWindow(PopupWindow window) {
        mPanelPopupWindow = window;
    }

    public PopupWindow getPopupWindow() {
        return mPanelPopupWindow;
    }

    public void changeMarkItemState(boolean mark) {
        mIsReadingBookmark = mark;
        for(IBaseItem item:mMarkItemList){
            item.setSelected(mIsReadingBookmark);
        }
    }

    public void addMarkedItem(IBaseItem item) {
        if(mMarkItemList.contains(item))
            return;
        mMarkItemList.add(item);
        View.OnClickListener  listener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mIsReadingBookmark = !isMarked(mPdfViewCtrl.getCurrentPage());
                if (mIsReadingBookmark) {
                    addMark(mPdfViewCtrl.getCurrentPage());
                } else {
                    removeMark(mPdfViewCtrl.getCurrentPage());
                }
                changeMarkItemState(mIsReadingBookmark);
                ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().setDocModified(true);
                ((UIExtensionsManager) mUiExtensionsManager).resetHideToolbarsTimer();
            }
        };

       item.setOnClickListener(listener);
    }

    public void removeMarkedItem(IBaseItem item){
        if(!mMarkItemList.contains(item)) return;
        mMarkItemList.remove(item);
        // just remove mark item while close document
//        ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().setDocModified(true);
    }

    private void prepareSupport(){
        if(mSupport == null){
            mSupport = new ReadingBookmarkSupport(ReadingBookmarkModule.this);
            mReadingBookmarkListView.setAdapter(mSupport.getAdapter());
        }
        mSupport.getAdapter().initBookmarkList();
    }

    private final PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int errCode) {
            if (errCode != Constants.e_ErrSuccess) {
                return;
            }
            if (mPdfViewCtrl.isDynamicXFA()) {
                mBookmarkAddButton.setEnable(false);
            } else {
                boolean canAssemble = ((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().canAssemble();
                mBookmarkAddButton.setEnable(canAssemble);
                mClearView.setEnabled(canAssemble);
            }
            prepareReadingBookMark();
        }

        @Override
        public void onDocWillClose(PDFDoc pdfDoc) {

        }

        @Override
        public void onDocClosed(PDFDoc pdfDoc, int i) {

        }

        @Override
        public void onDocWillSave(PDFDoc pdfDoc) {

        }

        @Override
        public void onDocSaved(PDFDoc pdfDoc, int i) {

        }
    };

    private void prepareReadingBookMark(){
        prepareSupport();
        remarkItemState(mPdfViewCtrl.getCurrentPage());
    }

    private final PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener(){

        @Override
        public void onPageChanged(int oldPageIndex, int curPageIndex) {
            if (mSupport == null) return;

            if (curPageIndex < 0) {
                curPageIndex = 0;
            }
            remarkItemState(curPageIndex);
        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {
            mSupport.getAdapter().onPageMoved(success,index,dstIndex);

            remarkItemState(mPdfViewCtrl.getCurrentPage());
        }

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {
            for(int i = 0; i < pageIndexes.length; i++) {
                mSupport.getAdapter().onPageRemoved(success,pageIndexes[i] - i);
            }

            remarkItemState(mPdfViewCtrl.getCurrentPage());
        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
            mSupport.getAdapter().onPagesInsert(success, dstIndex, range);
            remarkItemState(mPdfViewCtrl.getCurrentPage());
        }
    };

    public void remarkItemState(final int index) {
        ((Activity)((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(IBaseItem item:mMarkItemList){
                    item.setSelected(isMarked(index));
                }
            }
        });
    }

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (mPanelPopupWindow != null && mPanelPopupWindow.isShowing() && mPanelHost != null && mPanelHost.getCurrentSpec() == ReadingBookmarkModule.this) {
                if (oldWidth != newWidth || oldHeight != newHeight) {
                    update(newWidth, newHeight);
                }
            }
        }
    };

    public void changeViewState(boolean enable) {
        boolean canAssemble = ((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().canAssemble();
        mClearView.setEnabled(enable&&canAssemble);
        if (!enable) {
            mReadingBookmarkNoInfoTv.setVisibility(View.VISIBLE);
        } else {
            mReadingBookmarkNoInfoTv.setVisibility(View.GONE);
        }
    }

    public void show() {
        ViewGroup rootView = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView();
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        int width = rootView.getWidth();
        int height = rootView.getHeight();
        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            int defaultWidth = (int) (AppDisplay.getInstance(mContext).getScreenWidth() * scale);
            width = Math.min(width, defaultWidth);
        }
        mPanelPopupWindow.setWidth(width);
        mPanelPopupWindow.setHeight(height);
        mPanelPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        mPanelPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        mPanelHost.setCurrentSpec(PanelType.ReadingBookmarks);
        mPanelPopupWindow.showAtLocation(rootView, Gravity.LEFT | Gravity.TOP, x, y);
    }

    public void update(int width, int height) {
        int[] location = new int[2];
        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView().getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            int defaultWidth = (int) (AppDisplay.getInstance(mContext).getScreenWidth() * scale);
            width = Math.min(width, defaultWidth);
        }
        mPanelPopupWindow.update(x, y, width, height);
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            mPanelHost = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager().getPanel();
            mPanelPopupWindow = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager().getPanelWindow();
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);

            mBookmarkAddButton = new BaseItemImpl(mContext);
            mBookmarkAddButton.setTag(ToolbarItemConfig.ITEM_TOPBAR_READINGMARK);
            mBookmarkAddButton.setImageResource(R.drawable.rd_readingmark_add_selector);

            addMarkedItem(mBookmarkAddButton);
            ((UIExtensionsManager) mUiExtensionsManager).getMainFrame().getTopToolbar().addView(mBookmarkAddButton, BaseBar.TB_Position.Position_RB);
        }

        if (mPanelPopupWindow == null) {
            mPanelPopupWindow = new PopupWindow(mPanelHost.getContentView(), RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
            mPanelPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
            mPanelPopupWindow.setAnimationStyle(R.style.View_Animation_LtoR);
            mPanelPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {

                }
            });
        }

        if (mPanelHost == null)
            mPanelHost = new PanelHostImpl(mContext, new PanelHost.ICloseDefaultPanelCallback() {
                @Override
                public void closeDefaultPanel(View v) {
                    if(mPanelPopupWindow != null && mPanelPopupWindow.isShowing()){
                        mPanelPopupWindow.dismiss();
                    }
                }
            });
        mTopBarView = View.inflate(mContext, R.layout.panel_bookmark_topbar, null);
        View closeView = mTopBarView.findViewById(R.id.panel_bookmark_close);
        TextView topTitle = (TextView) mTopBarView.findViewById(R.id.panel_bookmark_title);
        mClearView = mTopBarView.findViewById(R.id.panel_bookmark_clear);
        if (mIsPad) {
            closeView.setVisibility(View.GONE);
        } else {
            closeView.setVisibility(View.VISIBLE);
            closeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPanelPopupWindow.isShowing())
                        mPanelPopupWindow.dismiss();
                }
            });
        }
        View topNormalView = mTopBarView.findViewById(R.id.panel_bookmark_rl_top);
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
            public void onClick(View v) {
                if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
                Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                if (context == null) return;
                mDialog = new UITextEditDialog(context);
                mDialog.setTitle(mContext.getApplicationContext().getString(R.string.hm_clear));
                mDialog.getPromptTextView().setText(mContext.getApplicationContext().getString(R.string.rd_panel_clear_readingbookmarks));
                mDialog.getInputEditText().setVisibility(View.GONE);
                mDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSupport.clearAllNodes();
                        changeViewState(false);
                        changeMarkItemState(false);
                        mDialog.dismiss();
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
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
        });

        mContentView = LayoutInflater.from(mContext).inflate(R.layout.panel_bookmark_main, null);
        mReadingMarkContent = (RelativeLayout) mContentView.findViewById(R.id.panel_bookmark_content_root);
        mReadingBookmarkListView = (ListView) mReadingMarkContent.findViewById(R.id.panel_bookmark_lv);
        mReadingBookmarkNoInfoTv = (TextView) mReadingMarkContent.findViewById(R.id.panel_nobookmark_tv);

        mReadingBookmarkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (AppUtil.isFastDoubleClick()) return;
                ReadingBookmarkSupport.ReadingBookmarkNode bookmarkNode = (ReadingBookmarkSupport.ReadingBookmarkNode) mSupport.getAdapter().getItem(position);
                mPdfViewCtrl.gotoPage(bookmarkNode.getIndex());
                if (mPanelPopupWindow.isShowing())
                    mPanelPopupWindow.dismiss();

            }
        });
        mReadingBookmarkListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        boolean show = false;
                        int position = 0;
                        for (int i = 0; i < mSupport.getAdapter().getCount(); i++) {
                            if (mItemMoreViewShow.get(i)) {
                                show = true;
                                position = i;
                                break;
                            }
                        }
                        if (show) {
                            mItemMoreViewShow.set(position, false);
                            mSupport.getAdapter().notifyDataSetChanged();
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

        mPanelHost.addSpec(this);
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerLayoutChangeListener(mLayoutChangeListener);
        }
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPanelHost.removeSpec(this);
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLayoutChangeListener(mLayoutChangeListener);
        }
        return true;
    }

    public void addPanel(){
        mPanelHost.addSpec(this);
    }

    public void removePanel(){
        mPanelHost.removeSpec(this);
    }

    @Override
    public String getName() {
        return MODULE_NAME_BOOKMARK;
    }

    @Override
    public void onActivated() {

        if (mSupport != null){
            changeViewState(mSupport.getAdapter().getCount() != 0);
            if(mSupport.needRelayout()){
                mSupport.getAdapter().notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDeactivated() {

    }

    @Override
    public View getTopToolbar() {
        return mTopBarView;
    }

    @Override
    public int getIcon() {
        return R.drawable.panel_tabing_readingmark_selector;
    }

    @Override
    public PanelType getPanelType() {
        return PanelType.ReadingBookmarks;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }

    public boolean isMarked(int pageIndex){
        return mSupport.getAdapter().isMarked(pageIndex);
    }

    public void addMark(int index){
        mSupport.addReadingBookmarkNode(index, mContext.getApplicationContext().getString(R.string.fx_page_book_mark, index + 1));
    }

    public void removeMark(int index){
        mSupport.removeReadingBookmarkNode(index);
    }

}
