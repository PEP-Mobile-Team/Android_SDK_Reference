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
package com.foxit.uiextensions.annots.form;


import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.interform.Form;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.config.modules.ModulesConfig;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.modules.dynamicxfa.XFADocProvider;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IMainFrame;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.OnPageEventListener;
import com.foxit.uiextensions.utils.thread.AppThreadManager;


public class FormFillerModule implements Module, PropertyBar.PropertyChangeListener {
    public static final String ID_TAG = "FoxitPDFSDK";

    public static final int CREATE_MODE_NONE = 100;
    public static final int CREATE_TEXT_FILED_MODE = 101;
    public static final int CREATE_CHECKBOX_FILED_MODE = 102;
    public static final int CREATE_SIGNATURE_FILED_MODE = 103;

    private static final int RESET_FORM_FIELDS = 111;
    private static final int IMPORT_FORM_DATA = 222;
    private static final int EXPORT_FORM_DATA = 333;

    private FormFillerToolHandler mToolHandler;
    private FormFillerAnnotHandler mAnnotHandler;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private Form mForm = null;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private XFADocProvider mDocProvider = null;

    private int mCreateMode = CREATE_MODE_NONE;

    private IBaseItem mCreateSignatureItem;
    private IBaseItem mCreateTextItem;
    private IBaseItem mCreateCheckboxItem;

    protected void initForm(PDFDoc document) {
        try {
            if (document != null && !mPdfViewCtrl.isDynamicXFA()) {
                boolean hasForm = document.hasForm();
                if (!hasForm) return;
                mForm = new Form(document);
                mAnnotHandler.init(mForm);

                if (mPdfViewCtrl.getXFADoc() != null) {
                    mPdfViewCtrl.getXFADoc().setDocProviderCallback(mDocProvider = new XFADocProvider(mContext, mPdfViewCtrl));
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private PDFViewCtrl.IDocEventListener mDocumentEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != Constants.e_ErrSuccess) return;

            mToolHandler.reset();
            initForm(document);
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
            mAnnotHandler.clear();
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

    private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener() {
        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
            if (!success || mAnnotHandler.hasInitialized()) return;
            AppThreadManager.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initForm(mPdfViewCtrl.getDoc());
                }
            });
        }
    };

    private PDFViewCtrl.IScaleGestureEventListener mScaleGestureEventListener = new PDFViewCtrl.IScaleGestureEventListener() {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (mAnnotHandler.getFormFillerAssist() != null) {
                mAnnotHandler.getFormFillerAssist().setScaling(true);
            }

            if (mDocProvider != null) {
                mDocProvider.setScaleState(true);
            }
            return false;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mAnnotHandler.getFormFillerAssist() != null) {
                mAnnotHandler.getFormFillerAssist().setScaling(false);
            }

            if (mDocProvider != null) {
                mDocProvider.setScaleState(false);
            }
        }
    };


    public FormFillerModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_FORMFILLER;
    }

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    public void resetForm(Event.Callback callback) {
        EditFormTask editFormTask = new EditFormTask(RESET_FORM_FIELDS, callback);
        mPdfViewCtrl.addTask(editFormTask);
    }

    public void exportFormToXML(String path, Event.Callback callback) {
        EditFormTask editFormTask = new EditFormTask(EXPORT_FORM_DATA, path, callback);
        mPdfViewCtrl.addTask(editFormTask);
    }

    public void importFormFromXML(final String path, Event.Callback callback) {
        EditFormTask editFormTask = new EditFormTask(IMPORT_FORM_DATA, path, callback);
        mPdfViewCtrl.addTask(editFormTask);
    }

    class EditFormTask extends Task {

        private boolean ret;
        private int mType;
        private String mPath;

        private EditFormTask(int type, String path, final Event.Callback callBack) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    callBack.result(null, ((EditFormTask) task).ret);
                }
            });
            this.mPath = path;
            mType = type;
        }

        private EditFormTask(int type, final Event.Callback callBack) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    callBack.result(null, ((EditFormTask) task).ret);
                }
            });
            mType = type;
        }

        @Override
        protected void execute() {
            switch (mType) {
                case RESET_FORM_FIELDS:
                    try {
                        PDFViewCtrl.lock();
                        ret = mForm.reset();
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
                        ret = mForm.importFromXML(mPath);
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
                        ret = mForm.exportToXML(mPath);
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

    @Override
    public boolean loadModule() {
        mToolHandler = new FormFillerToolHandler(mContext, mPdfViewCtrl, this);
        mAnnotHandler = new FormFillerAnnotHandler(mContext, mParent, mPdfViewCtrl);

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerStateChangeListener(mStateChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigureChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
            ((UIExtensionsManager) mUiExtensionsManager).registerLifecycleListener(mLifecycleEventListener);
        }
        mPdfViewCtrl.registerDocEventListener(mDocumentEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);
        mPdfViewCtrl.registerScaleGestureEventListener(mScaleGestureEventListener);
        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterStateChangeListener(mStateChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigureChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLifecycleListener(mLifecycleEventListener);
        }
        mPdfViewCtrl.unregisterDocEventListener(mDocumentEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        mPdfViewCtrl.unregisterScaleGestureEventListener(mScaleGestureEventListener);
        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        return true;
    }

    private PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
        @Override
        public void onWillRecover() {
            mAnnotHandler.clear();
        }

        @Override
        public void onRecovered() {
        }
    };

    @Override
    public void onValueChanged(long property, int value) {

    }

    @Override
    public void onValueChanged(long property, float value) {

    }

    @Override
    public void onValueChanged(long property, String value) {

    }

    public boolean onKeyBack() {
        return mToolHandler.onKeyBack() || mAnnotHandler.onKeyBack();
    }

    private ILifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {

        @Override
        public void onResume(Activity act) {
            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
            AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
            Annot curAnnot = uiExtensionsManager.getDocumentManager().getCurrentAnnot();
            if (curAnnot != null && currentAnnotHandler == mAnnotHandler) {
                if (mAnnotHandler.shouldShowInputSoft(curAnnot)) {

                    AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            mAnnotHandler.showSoftInput();
                        }
                    });
                }
            }
        }
    };

    private IStateChangeListener mStateChangeListener = new IStateChangeListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            if (ReadStateConfig.STATE_CREATE_FORM == newState) {
                resetFormBar();
                changeCreateMode(CREATE_MODE_NONE);
            }
        }
    };

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    private UIExtensionsManager.ConfigurationChangedListener mConfigureChangeListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            mAnnotHandler.onConfigurationChanged(newConfig);
        }
    };

    public void resetFormBar() {
        IMainFrame mainFrame = ((UIExtensionsManager) mUiExtensionsManager).getMainFrame();
        BaseBarImpl bottomBar = (BaseBarImpl) mainFrame.getFormBar();
        bottomBar.removeAllItems();

        //text filed
        mCreateTextItem = new BaseItemImpl(mContext);
        mCreateTextItem.setImageResource(R.drawable.create_form_textfield_selector);
        mCreateTextItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mode = (mCreateMode == CREATE_TEXT_FILED_MODE) ? CREATE_MODE_NONE : CREATE_TEXT_FILED_MODE;
                changeCreateMode(mode);
            }
        });
        bottomBar.addView(mCreateTextItem, BaseBar.TB_Position.Position_CENTER);

        //checkbox filed
        mCreateCheckboxItem = new BaseItemImpl(mContext);
        mCreateCheckboxItem.setImageResource(R.drawable.create_form_checkbox_selector);
        mCreateCheckboxItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mode = (mCreateMode == CREATE_CHECKBOX_FILED_MODE) ? CREATE_MODE_NONE : CREATE_CHECKBOX_FILED_MODE;
                changeCreateMode(mode);
            }
        });
        bottomBar.addView(mCreateCheckboxItem, BaseBar.TB_Position.Position_CENTER);

        ModulesConfig config = ((UIExtensionsManager)mUiExtensionsManager).getConfig().modules;
        if (config.isLoadForm() && config.isLoadSignature()){
            //signature filed
            mCreateSignatureItem = new BaseItemImpl(mContext);
            mCreateSignatureItem.setImageResource(R.drawable.create_form_signature_selector);
            mCreateSignatureItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int mode = (mCreateMode == CREATE_SIGNATURE_FILED_MODE) ? CREATE_MODE_NONE : CREATE_SIGNATURE_FILED_MODE;
                    changeCreateMode(mode);
                }
            });
            bottomBar.addView(mCreateSignatureItem, BaseBar.TB_Position.Position_CENTER);
        }
    }

    private void changeCreateMode(int mode) {
        mCreateMode = mode;

        if (mode == CREATE_MODE_NONE) {
            mCreateTextItem.setSelected(false);
            mCreateCheckboxItem.setSelected(false);
            if (mCreateSignatureItem != null)
                mCreateSignatureItem.setSelected(false);
            ((UIExtensionsManager) mUiExtensionsManager).setCurrentToolHandler(null);
            return;
        }

        if (mode == CREATE_TEXT_FILED_MODE) {
            mCreateTextItem.setSelected(true);
            mCreateCheckboxItem.setSelected(false);
            if (mCreateSignatureItem != null)
                mCreateSignatureItem.setSelected(false);
        } else if (mode == CREATE_CHECKBOX_FILED_MODE) {
            mCreateTextItem.setSelected(false);
            mCreateCheckboxItem.setSelected(true);
            if (mCreateSignatureItem != null)
                mCreateSignatureItem.setSelected(false);
        } else if (mode == CREATE_SIGNATURE_FILED_MODE) {
            mCreateTextItem.setSelected(false);
            mCreateCheckboxItem.setSelected(false);
            if (mCreateSignatureItem != null)
                mCreateSignatureItem.setSelected(true);
        }

        if (((UIExtensionsManager) mUiExtensionsManager).getCurrentToolHandler() != mToolHandler) {
            ((UIExtensionsManager) mUiExtensionsManager).setCurrentToolHandler(mToolHandler);
        }
        mToolHandler.setCreateMode(mCreateMode);
    }

}
