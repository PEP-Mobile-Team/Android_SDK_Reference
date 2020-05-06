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
package com.foxit.uiextensions.modules.dynamicxfa;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.addon.xfa.XFAWidget;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.modules.PageNavigationModule;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;
import com.foxit.uiextensions.utils.AppSQLite;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.thread.AppThreadManager;
/**  XFA module is for reading the dynamic XFA file, and enable the user to fill the XFA form fields. */
public class DynamicXFAModule implements Module {
    private static final int RESET_FORM_FIELDS = 111;
    private static final int IMPORT_FORM_DATA = 222;
    private static final int EXPORT_FORM_DATA = 333;

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    private DynamicXFAWidgetHandler mXFAWidgetHandler = null;
    private XFADocProvider mDocProvider = null;

    public DynamicXFAModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_DYNAMICXFA;
    }

    @Override
    public boolean loadModule() {
        if (!AppSQLite.getInstance(mContext).isDBOpened()) {
            AppSQLite.getInstance(mContext).openDB();
        }
        mXFAWidgetHandler = new DynamicXFAWidgetHandler(mContext, mParent, mPdfViewCtrl);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
            ((UIExtensionsManager) mUiExtensionsManager).registerXFAPageEventListener(mXfaPageEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerXFAWidgetEventListener(mXFAWidgetEventListener);
			((UIExtensionsManager) mUiExtensionsManager).registerMenuEventListener(mMenuEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerLifecycleListener(mLifecycleEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(configurationChangedListener);
        }
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerScaleGestureEventListener(mScaleGestureEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterXFAPageEventListener(mXfaPageEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterXFAWidgetEventListener(mXFAWidgetEventListener);
			((UIExtensionsManager) mUiExtensionsManager).unregisterMenuEventListener(mMenuEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLifecycleListener(mLifecycleEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(configurationChangedListener);
        }
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterScaleGestureEventListener(mScaleGestureEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        return true;
    }

    public void resetForm(Event.Callback callback) {
        EditFormTask editFormTask = new EditFormTask(callback);
        mPdfViewCtrl.addTask(editFormTask);
    }

    public void importData(String path, Event.Callback callback) {
        EditFormTask editFormTask = new EditFormTask(path, callback);
        mPdfViewCtrl.addTask(editFormTask);
    }

    public void exportData(String path, int exportType, Event.Callback callBack) {
        EditFormTask editFormTask = new EditFormTask(exportType, path, callBack);
        mPdfViewCtrl.addTask(editFormTask);
    }

    class EditFormTask extends Task {

        private boolean ret;
        private int mExportType;
        private int mType;
        private String mPath;

        private EditFormTask(String path, final Event.Callback callBack) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    callBack.result(null, ((EditFormTask) task).ret);
                }
            });
            this.mPath = path;
            mType = IMPORT_FORM_DATA;
        }

        private EditFormTask(int exportType, String path, final Event.Callback callBack) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    callBack.result(null, ((EditFormTask) task).ret);
                }
            });
            this.mPath = path;
            mExportType = exportType;
            mType = EXPORT_FORM_DATA;
        }

        private EditFormTask(final Event.Callback callBack) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    callBack.result(null, ((EditFormTask) task).ret);
                }
            });
            mType = RESET_FORM_FIELDS;
        }

        @Override
        protected void execute() {
            switch (mType) {
                case RESET_FORM_FIELDS:
                    try {
                        PDFViewCtrl.lock();
                        mPdfViewCtrl.getXFADoc().resetForm();
                        ret = true;
                    } catch (PDFException e) {
                        e.printStackTrace();
                        ret = false;
                    } finally {
                        PDFViewCtrl.unlock();
                    }
                    break;
                case IMPORT_FORM_DATA:
                    try {
                        PDFViewCtrl.lock();
                        ret = mPdfViewCtrl.getXFADoc().importData(mPath);
                    } catch (PDFException e) {
                        ret = false;
                        e.printStackTrace();
                    } finally {
                        PDFViewCtrl.unlock();
                    }
                    break;
                case EXPORT_FORM_DATA:
                    try {
                        PDFViewCtrl.lock();
                        ret = mPdfViewCtrl.getXFADoc().exportData(mPath, mExportType);
                    } catch (PDFException e) {
                        ret = false;
                        e.printStackTrace();
                    } finally {
                        PDFViewCtrl.unlock();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public IXFAWidgetHandler getXFAWidgetHandler() {
        return mXFAWidgetHandler;
    }

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != Constants.e_ErrSuccess) return;
            if (document != null && mPdfViewCtrl.isDynamicXFA()) {
                mPdfViewCtrl.getXFADoc().setDocProviderCallback(mDocProvider = new XFADocProvider(mContext, mPdfViewCtrl));
                mXFAWidgetHandler.initialize();
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
            if (mDocProvider != null) {
                mDocProvider.setWillClose(true);
            }
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {

        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private PDFViewCtrl.IScaleGestureEventListener mScaleGestureEventListener = new PDFViewCtrl.IScaleGestureEventListener(){

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (mDocProvider != null) {
                mDocProvider.setScaleState(true);
            }

            return false;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mDocProvider != null) {
                mDocProvider.setScaleState(false);
            }
        }
    };

    private IXFAPageEventListener mXfaPageEventListener = new IXFAPageEventListener() {
        @Override
        public void onPagesInserted(boolean success, int pageIndex) {
           resetPageNavigation();
        }

        @Override
        public void onPagesRemoved(boolean success, int pageIndex) {
            resetPageNavigation();
        }
    };

    private void resetPageNavigation(){
        PageNavigationModule module = (PageNavigationModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PAGENAV);
        if(module != null) {
            module.resetJumpView();
        }
    }

    public boolean onKeyBack() {
        return mXFAWidgetHandler.onKeyBack();
    }

    public XFAWidget getCurrentXFAWidget() {
        if (mXFAWidgetHandler == null) return null;
        return mXFAWidgetHandler.getCurrentXFAWidget();
    }

    public void setCurrentXFAWidget(XFAWidget xfaWidget) {
        if (mXFAWidgetHandler != null) {
            mXFAWidgetHandler.setCurrentXFAWidget(xfaWidget);
        }
    }

    UIExtensionsManager.MenuEventListener mMenuEventListener = new UIExtensionsManager.MenuEventListener() {
        @Override
        public void onTriggerDismissMenu() {
            if (getCurrentXFAWidget() != null) {
                setCurrentXFAWidget(null);
            }
        }
    };

    private ILifecycleEventListener mLifecycleEventListener = new LifecycleEventListener(){

        @Override
        public void onResume(Activity act) {
            if (getCurrentXFAWidget() != null) {
                if (mXFAWidgetHandler.shouldShowInputSoft(getCurrentXFAWidget())){
                    AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            mXFAWidgetHandler.showSoftInput();
                        }
                    });
                }
            }
        }
    };

    private IXFAWidgetEventListener mXFAWidgetEventListener = new IXFAWidgetEventListener() {
        @Override
        public void onXFAWidgetAdded(XFAWidget xfaWidget) {

        }

        @Override
        public void onXFAWidgetWillRemove(XFAWidget xfaWidget) {
            if (xfaWidget.isEmpty() || getCurrentXFAWidget() == null) return;
            if (mXFAWidgetHandler != null) {
                mXFAWidgetHandler.update(xfaWidget);
            }
        }
    };

    private UIExtensionsManager.ConfigurationChangedListener configurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            if (mXFAWidgetHandler != null) {
                mXFAWidgetHandler.onConfigurationChanged(newConfig);
            }
        }
    };

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {
        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            if (mXFAWidgetHandler != null) {
                mXFAWidgetHandler.onDrawForControls(canvas);
            }
        }
    };
}
