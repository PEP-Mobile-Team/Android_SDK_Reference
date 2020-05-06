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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.utils.AppUtil;
/** Search the text on the PDF document, and provides the information of all the found results.*/
public class SearchModule implements Module {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private ViewGroup mParent = null;
    private SearchView mSearchView = null;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    private IBaseItem mSearchButtonItem;
    private boolean mIsHideSystemUI;

    public SearchModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        if (context == null || parent == null || pdfViewCtrl == null) {
            throw new NullPointerException();
        }
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public boolean loadModule() {
        mSearchView = new SearchView(mContext, mParent, mPdfViewCtrl);
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
            ((UIExtensionsManager) mUiExtensionsManager).registerStateChangeListener(mStateChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerLayoutChangeListener(mLayoutChangeListener);
            initSearchItem();
        }
        return true;
    }


    private void initSearchItem(){
        final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;

        mSearchButtonItem = new BaseItemImpl(mContext);
        mSearchButtonItem.setTag(ToolbarItemConfig.ITEM_TOPBAR_SEARCH);
        mSearchButtonItem.setImageResource(R.drawable.rd_search_selector);
        mSearchButtonItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }

                mSearchView.setSearchCancelListener(new SearchView.SearchCancelListener() {
                    @Override
                    public void onSearchCancel() {
                        ((MainFrame)uiExtensionsManager.getMainFrame()).setHideSystemUI(mIsHideSystemUI);
                        uiExtensionsManager.changeState(ReadStateConfig.STATE_NORMAL);
                        uiExtensionsManager.getMainFrame().showToolbars();
                    }
                });
                mSearchView.launchSearchView();
                mSearchView.show();
            }
        });
        uiExtensionsManager.getMainFrame().getTopToolbar().addView(mSearchButtonItem, BaseBar.TB_Position.Position_RB);
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_SEARCH;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterStateChangeListener(mStateChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLayoutChangeListener(mLayoutChangeListener);
        }
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        mDocEventListener = null;
        mDrawEventListener = null;
        return true;
    }

    public SearchView getSearchView() {
        return mSearchView;
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            if (mSearchView.mIsCancel) {
                return;
            }

            if (mSearchView.mRect == null || mSearchView.mPageIndex == -1) {
                return;
            }

            if (mSearchView.mPageIndex == pageIndex) {
                if (mSearchView.mRect.size() > 0) {
                    Paint paint = new Paint();
                    paint.setARGB(150, 23, 156, 216);
                    for (int i = 0; i < mSearchView.mRect.size(); i++) {
                        RectF rectF = new RectF(mSearchView.mRect.get(i));
                        RectF deviceRect = new RectF();
                        if (mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, deviceRect, mSearchView.mPageIndex)) {
                            canvas.drawRect(deviceRect, paint);
                        }
                    }
                }
            }
        }
    };

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int i) {
            if (i == Constants.e_ErrSuccess) {
                if (mPdfViewCtrl.isDynamicXFA()) {
                    mSearchButtonItem.setImageResource(R.drawable.rd_search_pressed);
                    mSearchButtonItem.setEnable(false);
                } else {
                    mSearchButtonItem.setImageResource(R.drawable.rd_search);
                    mSearchButtonItem.setEnable(true);
                }
            }
        }

        @Override
        public void onDocWillClose(PDFDoc pdfDoc) {

        }

        @Override
        public void onDocClosed(PDFDoc pdfDoc, int i) {
            mSearchView.onDocumentClosed();
        }

        @Override
        public void onDocWillSave(PDFDoc pdfDoc) {

        }

        @Override
        public void onDocSaved(PDFDoc pdfDoc, int i) {

        }
    };

    public boolean onKeyBack() {
        if (mSearchView.getView().getVisibility() == View.VISIBLE) {
            mSearchView.onKeyBack();
            return true;

        }
        return false;
    }

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (mSearchView.getView().getVisibility() == View.VISIBLE) {
                mSearchView.onLayoutChange(v, newWidth, newHeight, oldWidth, oldHeight);
            }
        }
    };

    private IStateChangeListener mStateChangeListener = new IStateChangeListener() {

        @Override
        public void onStateChanged(int oldState, int newState) {
            if (newState == ReadStateConfig.STATE_SEARCH && oldState != ReadStateConfig.STATE_SEARCH) {
                final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;
                mIsHideSystemUI = ((MainFrame)uiExtensionsManager.getMainFrame()).isHideSystemUI();
                ((MainFrame)uiExtensionsManager.getMainFrame()).setHideSystemUI(false);
                uiExtensionsManager.triggerDismissMenuEvent();
                uiExtensionsManager.getMainFrame().hideToolbars();
            } else if (oldState == ReadStateConfig.STATE_SEARCH && newState != ReadStateConfig.STATE_SEARCH) {
                mSearchView.cleanSearch();
                final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;
                ((MainFrame)uiExtensionsManager.getMainFrame()).setHideSystemUI(mIsHideSystemUI);
            }
        }
    };

}
