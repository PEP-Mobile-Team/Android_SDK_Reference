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
package com.foxit.uiextensions.annots.form;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.SparseArray;
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
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.modules.dynamicxfa.XFADocProvider;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IMainFrame;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.OnPageEventListener;
import com.foxit.uiextensions.utils.thread.AppThreadManager;


public class FormFillerModule implements Module, PropertyBar.PropertyChangeListener {
    public static final String ID_TAG = "FoxitPDFSDK";

    public static final int CREATE_NONE = 100;
    public static final int CREATE_TEXT_FILED = 101;
    public static final int CREATE_CHECKBOX = 102;
    public static final int CREATE_RADIO_BUTTON = 103;
    public static final int CREATE_COMBOBOX = 104;
    public static final int CREATE_LISTBOX = 105;
    public static final int CREATE_SIGNATURE_FILED = 106;

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

    private int mLastCreateMode = CREATE_NONE;

    protected void initForm(PDFDoc document) {
        try {
            if (mAnnotHandler.hasInitialized()) return;
            if (document != null && !mPdfViewCtrl.isDynamicXFA()) {
                boolean hasForm = document.hasForm();
                if (!hasForm) return;

                if (mHandlerThread == null)
                    initHandlerThread();
                mHandler.sendEmptyMessage(0);

                mForm = new Form(document);
                mAnnotHandler.init(mForm);
                if (mPdfViewCtrl.getXFADoc() != null)
                    mPdfViewCtrl.getXFADoc().setDocProviderCallback(mDocProvider = new XFADocProvider(mContext, mPdfViewCtrl));
                mHandler.sendEmptyMessage(1);
            }
        } catch (PDFException e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage(1);
        }
    }

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private void initHandlerThread() {
        mHandlerThread = new HandlerThread("handlerThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        showProgressDlg();
                        break;
                    case 1:
                        dismissProgressDlg();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private ProgressDialog mProgressDlg;

    private void showProgressDlg() {
        if (mProgressDlg == null) {
            mProgressDlg = new ProgressDialog(((UIExtensionsManager) mUiExtensionsManager).getAttachedActivity());
            mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDlg.setCancelable(false);
            mProgressDlg.setIndeterminate(false);
            mProgressDlg.setMessage(AppResource.getString(mContext, R.string.fx_string_opening));
        }

        AppDialogManager.getInstance().showAllowManager(mProgressDlg, null);
    }

    private void dismissProgressDlg() {
        if (mProgressDlg != null && mProgressDlg.isShowing()) {
            AppDialogManager.getInstance().dismiss(mProgressDlg);
            mProgressDlg = null;
        }
    }

    private PDFViewCtrl.IDocEventListener mDocumentEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != Constants.e_ErrSuccess) return;
            if (mAnnotHandler != null && mAnnotHandler.hasInitialized())
                mAnnotHandler.clear();
            if (mDocProvider != null)
                mDocProvider.setWillClose(true);

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
           mAnnotHandler.startSave();
        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {
            mAnnotHandler.stopSave();
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

        @Override
        public void onPageChanged(int oldPageIndex, int curPageIndex) {
            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
            AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
            Annot curAnnot = uiExtensionsManager.getDocumentManager().getCurrentAnnot();
            if (curAnnot != null && currentAnnotHandler == mAnnotHandler && !mPdfViewCtrl.isContinuous()) {
                uiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
            }
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
            ((UIExtensionsManager) mUiExtensionsManager).registerLayoutChangeListener(mLayoutChangeListener);
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
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLayoutChangeListener(mLayoutChangeListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLifecycleListener(mLifecycleEventListener);
        }
        mPdfViewCtrl.unregisterDocEventListener(mDocumentEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        mPdfViewCtrl.unregisterScaleGestureEventListener(mScaleGestureEventListener);
        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);

        if (mHandlerThread != null)
            mHandlerThread.quit();
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
                changeCreateMode(CREATE_NONE);
            }
        }
    };

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (newWidth != oldWidth || newHeight != oldHeight) {
                UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
                if (currentAnnotHandler == mAnnotHandler) {
                    mAnnotHandler.onLayoutChange(v, newWidth, newHeight, oldWidth, oldHeight);
                }
            }
        }
    };

    private int[] formTypes = new int[]{CREATE_TEXT_FILED, CREATE_CHECKBOX, CREATE_RADIO_BUTTON,
            CREATE_COMBOBOX, CREATE_LISTBOX, CREATE_SIGNATURE_FILED};

    private int[] formIcons = new int[]{R.drawable.create_form_textfield_selector, R.drawable.create_form_checkbox_selector, R.drawable.create_form_radiobutton_selector,
            R.drawable.create_form_combobox_selector, R.drawable.create_form_listbox_selector, R.drawable.create_form_signature_selector};

    private SparseArray<IBaseItem> mItems = new SparseArray<>();

    private void resetFormBar() {
        IMainFrame mainFrame = ((UIExtensionsManager) mUiExtensionsManager).getMainFrame();
        BaseBarImpl bottomBar = (BaseBarImpl) mainFrame.getFormBar();
        bottomBar.removeAllItems();

        for (int i = 0; i < formTypes.length; i++) {
            final int type = formTypes[i];
            if (type == CREATE_SIGNATURE_FILED) {
                ModulesConfig config = ((UIExtensionsManager) mUiExtensionsManager).getConfig().modules;
                if (!config.isLoadForm() || !config.isLoadSignature())
                    return;
            }

            IBaseItem item = new BaseItemImpl(mContext);
            item.setImageResource(formIcons[i]);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int mode = (mLastCreateMode == type) ? CREATE_NONE : type;
                    changeCreateMode(mode);
                }
            });
            bottomBar.addView(item, BaseBar.TB_Position.Position_CENTER);
            mItems.put(type, item);
        }
    }

    private void changeCreateMode(int mode) {
        if (mode == CREATE_NONE) {
            mLastCreateMode = mode;
            for (int i = 0; i < mItems.size(); i++) {
                IBaseItem item = mItems.valueAt(i);
                item.setSelected(false);
            }
            ((UIExtensionsManager) mUiExtensionsManager).setCurrentToolHandler(null);
            return;
        }

        IBaseItem lastItem = mItems.get(mLastCreateMode);
        if (lastItem != null)
            lastItem.setSelected(false);
        IBaseItem curItem = mItems.get(mode);
        if (curItem != null)
            curItem.setSelected(true);
        mLastCreateMode = mode;
        if (((UIExtensionsManager) mUiExtensionsManager).getCurrentToolHandler() != mToolHandler)
            ((UIExtensionsManager) mUiExtensionsManager).setCurrentToolHandler(mToolHandler);
        mToolHandler.changeCreateMode(mLastCreateMode);
    }

}
