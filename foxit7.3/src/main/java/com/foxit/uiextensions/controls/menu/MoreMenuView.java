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
package com.foxit.uiextensions.controls.menu;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.addon.xfa.XFADoc;
import com.foxit.sdk.fdf.FDFDoc;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.form.FormFillerModule;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFileSelectDialog;
import com.foxit.uiextensions.controls.filebrowser.imp.FileItem;
import com.foxit.uiextensions.modules.doc.docinfo.DocInfoModule;
import com.foxit.uiextensions.modules.doc.docinfo.DocInfoView;
import com.foxit.uiextensions.modules.doc.saveas.DocSaveAsModule;
import com.foxit.uiextensions.modules.dynamicxfa.DynamicXFAModule;
import com.foxit.uiextensions.modules.print.PrintModule;
import com.foxit.uiextensions.modules.snapshot.SnapshotDialogFragment;
import com.foxit.uiextensions.modules.snapshot.SnapshotPresenter;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.security.standard.PasswordConstants;
import com.foxit.uiextensions.security.standard.PasswordModule;
import com.foxit.uiextensions.security.trustcertificate.TrustCertificateModule;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.LayoutConfig;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

public class MoreMenuView {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private ViewGroup mParent = null;
    private MenuViewImpl mMoreMenu = null;
    private PopupWindow mMenuPopupWindow = null;
    private ViewGroup mRootView = null;
    private String mFilePath = null;
    private MenuItem mImportMenuItem = null;
    private MenuItem mExportMenuItem = null;
    private MenuItem mResetMenuItem = null;
    private MenuItem mCreateFormItem = null;
    private String mExportFilePath = null;
    private ProgressDialog mProgressDlg;
    private String mProgressMsg;
    private UIFileSelectDialog mFileSelectDialog;

    public MoreMenuView(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
    }

    public void show() {

        this.showMoreMenu();
    }

    public void hide() {
        this.hideMoreMenu();
    }

    public MenuViewImpl getMoreMenu() {
        return mMoreMenu;
    }

    public void initView() {
        if (mMoreMenu == null) {
            mMoreMenu = new MenuViewImpl(mContext, new MenuViewImpl.MenuCallback() {
                @Override
                public void onClosed() {
                    hideMoreMenu();
                }
            });
        }
        setMoreMenuView(mMoreMenu.getContentView());
    }

    private MenuItem mPrintItem = null;

    /**
     * add a menu item to more menu group, click this item will show the file information and permission.
     */
    public void addDocInfoItem() {
        MenuGroup group = mMoreMenu.getMenuGroup(MoreMenuConfig.GROUP_FILE);
        if (group == null) {
            group = new MenuGroup(mContext, MoreMenuConfig.GROUP_FILE, AppResource.getString(mContext.getApplicationContext(), R.string.rd_menu_file));
            mMoreMenu.addMenuGroup(group);
        }

        MenuItem item = new MenuItem(mContext, MoreMenuConfig.ITEM_DOCINFO,
                AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info), 0,
                new MenuViewCallback() {
                    /**
                     * when click "Properties", will show the document information, and hide the current more menu.
                     */
                    @Override
                    public void onClick(MenuItem item) {

                        DocInfoModule docInfoModule = (DocInfoModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DOCINFO);
                        if (docInfoModule != null) {
                            DocInfoView docInfoView = docInfoModule.getView();
                            if (docInfoView != null)
                                docInfoView.show();
                        }
                    }
                });

        MenuItem rfsItem = new MenuItem(mContext, MoreMenuConfig.ITEM_REDUCE_FILE_SIZE,
                AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_reduce_file_size), 0,
                new MenuViewCallback() {
                    @Override
                    public void onClick(MenuItem item) {

                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setSaveDocFlag(PDFDoc.e_SaveFlagXRefStream);
                        UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_reduce_file_size_toast), Toast.LENGTH_LONG);
                    }
                });

        MenuItem snapshotItem = new MenuItem(mContext, MoreMenuConfig.ITEM_SNAPSHOT,
                AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_snapshot), 0,
                new MenuViewCallback() {
                    @Override
                    public void onClick(MenuItem item) {

                        //there should verify the security info
                        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canCopy() ||
                                !((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canCopyForAssess()) {
                            Toast.makeText(mContext.getApplicationContext(), R.string.no_permission, Toast.LENGTH_LONG).show();
                            return;
                        }

                        FragmentActivity act = ((FragmentActivity) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
                        SnapshotDialogFragment mFragment = (SnapshotDialogFragment) act.getSupportFragmentManager().findFragmentByTag("SnapshotFragment");
                        if (mFragment == null) {
                            mFragment = new SnapshotDialogFragment();
                            mFragment.setPdfViewCtrl(mPdfViewCtrl);
                            mFragment.setContext(mContext);
                            new SnapshotPresenter(mContext, mFragment);
                        }

                        AppDialogManager.getInstance().showAllowManager(mFragment, act.getSupportFragmentManager(), "SnapshotFragment", null);
                        hideMoreMenu();
                    }
                });

        final DocSaveAsModule saveAsModule = (DocSaveAsModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_SAVE_AS);
        if (saveAsModule != null) {
            MenuItem saveasItem = new MenuItem(mContext, MoreMenuConfig.ITEM_SAVE_AS,
                    AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_saveas), 0, new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItem item) {
                            saveAsModule.showSaveAsDialog();
                        }
                    });
            mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FILE, saveasItem);
        }

        final PrintModule printModule = (PrintModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PRINT);
        if (null != printModule) {
            mPrintItem = new MenuItem(
                    mContext,
                    MoreMenuConfig.ITEM_PRINT_FILE,
                    mContext.getApplicationContext().getString(R.string.menu_more_print),
                    0,
                    new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItem item) {
                            printModule.showPrintSettingOptions();
                        }
                    });
            mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FILE, mPrintItem);
        }

        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FILE, item);
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FILE, rfsItem);
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FILE, snapshotItem);
    }

    protected void reloadDocInfoItems() {
        if (mPrintItem != null) {
            mPrintItem.setEnable(false);
            boolean canPrint = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canPrint();
            mPrintItem.setEnable((Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) && canPrint);
        }
    }

    public void setFilePath(String filePath) {
        mFilePath = filePath;

        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        DocInfoModule docInfoModule = (DocInfoModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_DOCINFO);
        if (docInfoModule != null) {
            docInfoModule.setFilePath(mFilePath);
        }

        PasswordModule passwordModule = (PasswordModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_PASSWORD);
        if (passwordModule != null && passwordModule.getPasswordSupport() != null) {
            passwordModule.getPasswordSupport().setFilePath(mFilePath);
        }
    }

    public void addFormItem(final Module module) {
        MenuGroup group = mMoreMenu.getMenuGroup(MoreMenuConfig.GROUP_FORM);
        if (group == null) {
            group = new MenuGroup(mContext, MoreMenuConfig.GROUP_FORM, mContext.getApplicationContext().getString(R.string.menu_more_group_form));
        }
        mMoreMenu.addMenuGroup(group);
        if (mImportMenuItem == null) {
            mImportMenuItem = new MenuItem(
                    mContext,
                    MoreMenuConfig.ITEM_IMPORT_FORM,
                    AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_item_import),
                    0,
                    new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItem item) {
                            importFormFromXML(mPdfViewCtrl.isDynamicXFA() ? ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DYNAMICXFA) : module);
                        }
                    });
        }
        if (mExportMenuItem == null) {
            mExportMenuItem = new MenuItem(
                    mContext,
                    MoreMenuConfig.ITEM_EXPORT_FORM,
                    AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_item_export),
                    0,
                    new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItem item) {
                            exportFormToXML(mPdfViewCtrl.isDynamicXFA() ? ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DYNAMICXFA) : module);
                        }
                    });
        }
        if (mResetMenuItem == null) {
            mResetMenuItem = new MenuItem(
                    mContext,
                    MoreMenuConfig.ITEM_RESET_FORM,
                    AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_item_reset),
                    0,
                    new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItem item) {
                            resetForm(mPdfViewCtrl.isDynamicXFA() ? ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DYNAMICXFA) : module);
                        }
                    });
        }
        if (mCreateFormItem == null) {
            mCreateFormItem = new MenuItem(
                    mContext,
                    MoreMenuConfig.ITEM_CREATE_FORM,
                    AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_item_create),
                    0,
                    new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItem item) {
                            hideMoreMenu();
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).triggerDismissMenuEvent();
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).changeState(ReadStateConfig.STATE_CREATE_FORM);
                        }
                    });
        }
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FORM, mImportMenuItem);
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FORM, mExportMenuItem);
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FORM, mResetMenuItem);
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FORM, mCreateFormItem);
    }

    protected void reloadFormItems() {
        if (mImportMenuItem != null)
            mImportMenuItem.setEnable(false);
        if (mExportMenuItem != null)
            mExportMenuItem.setEnable(false);
        if (mResetMenuItem != null)
            mResetMenuItem.setEnable(false);
        if (mCreateFormItem != null)
            mCreateFormItem.setEnable(false);

        PDFDoc doc = mPdfViewCtrl.getDoc();
        try {
            if (mPdfViewCtrl.isDynamicXFA()) {
                mImportMenuItem.setEnable(true);
                mResetMenuItem.setEnable(true);
                mExportMenuItem.setEnable(true);
            } else if (doc != null) {
                DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
                if (!documentManager.isXFA() && documentManager.canModifyForm() && !documentManager.isSign()) {
                    mCreateFormItem.setEnable(true);
                }
                if (doc.hasForm()) {
                    if (documentManager.canFillForm()) {
                        mImportMenuItem.setEnable(true);
                        mResetMenuItem.setEnable(true);
                    }

                    if (documentManager.canCopy()) {
                        mExportMenuItem.setEnable(true);
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    /**
     * create one popup window, which will contain the view of more menu
     *
     * @param view
     */
    private void setMoreMenuView(View view) {
        if (mMenuPopupWindow == null) {
            mMenuPopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
        }

        mMenuPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
        mMenuPopupWindow.setAnimationStyle(R.style.View_Animation_RtoL);
        mMenuPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                showToolbars();
            }
        });
    }

    /**
     * if rotate the screen, will reset the size of more menu.to fit the screen.
     */
    protected void onConfigurationChanged(Configuration newConfig) {
        if (mFileSelectDialog != null && mFileSelectDialog.isShowing()) {
            mFileSelectDialog.setHeight(mFileSelectDialog.getDialogHeight());
            mFileSelectDialog.showDialog();
        }
    }

    private void showMoreMenu() {
        showSystemUI();
        mRootView = (ViewGroup) mParent.getChildAt(0);
        Rect rect = new Rect();
        mRootView.getGlobalVisibleRect(rect);
        int top = rect.top;
        int right = rect.right;
        int screenWidth = AppDisplay.getInstance(mContext).getRawScreenWidth();

        int width = mRootView.getWidth();
        int height = mRootView.getHeight();
        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            int defaultWidth = (int) (AppDisplay.getInstance(mContext).getScreenWidth() * scale);
            width = Math.min(width, defaultWidth);
        }
        mMenuPopupWindow.setWidth(width);
        mMenuPopupWindow.setHeight(height);
        mMenuPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        mMenuPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        mMenuPopupWindow.showAtLocation(mRootView, Gravity.RIGHT | Gravity.TOP, screenWidth - right, top);
    }

    private void updateMoreMenu(int width, int height) {
        showSystemUI();
        Rect rect = new Rect();
        mRootView.getGlobalVisibleRect(rect);
        int top = rect.top;
        int right = rect.right;
        int screenWidth = AppDisplay.getInstance(mContext).getRawScreenWidth();

        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            int defaultWidth = (int) (AppDisplay.getInstance(mContext).getScreenWidth() * scale);
            width = Math.min(width, defaultWidth);
        }
        mMenuPopupWindow.update(screenWidth - right, top, width, height);
    }

    private void hideMoreMenu() {
        if (mMenuPopupWindow.isShowing()) {
            mMenuPopupWindow.dismiss();
        }
    }

    private void showToolbars() {
        MainFrame mainFrame = (MainFrame) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getMainFrame();
        mainFrame.setHideSystemUI(true);
        mainFrame.showToolbars();
    }

    private void showSystemUI() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        MainFrame mainFrame = (MainFrame) uiExtensionsManager.getMainFrame();
        if (mainFrame.isToolbarsVisible()) {
            mainFrame.setHideSystemUI(false);
        } else {
            AppUtil.showSystemUI(uiExtensionsManager.getAttachedActivity());
        }
    }

    private void importFormFromXML(final Module module) {
        mFileSelectDialog = new UIFileSelectDialog(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
        mFileSelectDialog.init(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isHidden() || !pathname.canRead()) return false;
                if (pathname.isFile() && !pathname.getName().toLowerCase().endsWith(".xml"))
                    return false;
                return true;
            }
        }, true);
        mFileSelectDialog.showDialog();
        mFileSelectDialog.setTitle(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity().getApplicationContext().getString(R.string.formfiller_import_title));
        mFileSelectDialog.setButton(UIMatchDialog.DIALOG_CANCEL | UIMatchDialog.DIALOG_OK);
        mFileSelectDialog.setButtonEnable(false, UIMatchDialog.DIALOG_OK);
        mFileSelectDialog.setListener(new UIMatchDialog.DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == UIMatchDialog.DIALOG_OK) {
                    List<FileItem> files = mFileSelectDialog.getSelectedFiles();

                    mFileSelectDialog.dismiss();
                    mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_form_importing);
                    showProgressDlg();
                    if (module instanceof FormFillerModule) {
                        ((FormFillerModule) module).importFormFromXML(files.get(0).path, new Event.Callback() {
                            @Override
                            public void result(Event event, boolean success) {
                                dismissProgressDlg();
                                hideMoreMenu();
                                if (success) {
                                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
                                    UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_success_import_data));
                                } else {
                                    UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_fail_import_data));
                                }
                            }
                        });
                    } else if (module instanceof DynamicXFAModule) {
                        ((DynamicXFAModule) module).importData(files.get(0).path, new Event.Callback() {
                            @Override
                            public void result(Event event, boolean success) {
                                dismissProgressDlg();
                                hideMoreMenu();
                                if (success) {
                                    int[] pages = mPdfViewCtrl.getVisiblePages();
                                    for (int pageIndex : pages) {
                                        mPdfViewCtrl.refresh(pageIndex, new Rect(0, 0, mPdfViewCtrl.getPageViewWidth(pageIndex), mPdfViewCtrl.getPageViewHeight(pageIndex)));
                                    }
                                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
                                    UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_success_import_data));
                                } else {
                                    UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_fail_import_data));
                                }
                            }
                        });
                    }
                } else if (btType == UIMatchDialog.DIALOG_CANCEL) {
                    mFileSelectDialog.dismiss();
                }
            }

            @Override
            public void onBackClick() {

            }
        });
    }

    public void exportFormToXML(final Module module) {
        final UITextEditDialog dialog = new UITextEditDialog(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity()
        );
        dialog.setTitle(mContext.getApplicationContext().getString(R.string.formfiller_export_title));
        dialog.getInputEditText().setVisibility(View.VISIBLE);
        String fileNameWithoutExt = AppFileUtil.getFileNameWithoutExt(mFilePath);
        dialog.getInputEditText().setText(fileNameWithoutExt + ".xml");
        CharSequence text = dialog.getInputEditText().getText();
        if (text instanceof Spannable) {
            Spannable spanText = (Spannable) text;
            Selection.setSelection(spanText, 0, fileNameWithoutExt.length());
        }
        AppUtil.showSoftInput(dialog.getInputEditText());
        dialog.getCancelButton().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.getOKButton().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();

                String name = dialog.getInputEditText().getText().toString();
                if (name.toLowerCase().endsWith(".xml")) {
                    mExportFilePath = AppFileUtil.getFileFolder(mFilePath) + "/" + name;
                } else {
                    mExportFilePath = AppFileUtil.getFileFolder(mFilePath) + "/" + name + ".xml";
                }
                File file = new File(mExportFilePath);
                if (file.exists()) {

                    final UITextEditDialog rmDialog = new UITextEditDialog(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
                    rmDialog.setTitle(mContext.getApplicationContext().getString(R.string.fm_file_exist));
                    rmDialog.getPromptTextView().setText(mContext.getApplicationContext().getString(R.string.fx_string_filereplace_warning));
                    rmDialog.getInputEditText().setVisibility(View.GONE);
                    rmDialog.show();

                    rmDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            rmDialog.dismiss();
                            mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_form_exporting);
                            showProgressDlg();
                            Event.Callback callback = new Event.Callback() {
                                @Override
                                public void result(Event event, boolean success) {
                                    dismissProgressDlg();
                                    hideMoreMenu();
                                    if (success) {
                                        UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_success_export_data));
                                    } else {
                                        UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.formfiller_export_error));
                                    }
                                }
                            };

                            if (module instanceof FormFillerModule) {
                                ((FormFillerModule) module).exportFormToXML(mExportFilePath, callback);
                            } else if (module instanceof DynamicXFAModule) {
                                ((DynamicXFAModule) module).exportData(mExportFilePath, XFADoc.e_ExportDataTypeXML, callback);
                            }
                        }
                    });

                    rmDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            rmDialog.dismiss();
                            exportFormToXML(module);
                        }
                    });
                } else {
                    mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_form_exporting);
                    showProgressDlg();
                    Event.Callback callback = new Event.Callback() {
                        @Override
                        public void result(Event event, boolean success) {
                            dismissProgressDlg();
                            hideMoreMenu();
                            if (success) {
                                UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_success_export_data));
                            } else {
                                UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.formfiller_export_error));
                            }
                        }
                    };

                    if (module instanceof FormFillerModule) {
                        ((FormFillerModule) module).exportFormToXML(mExportFilePath, callback);
                    } else if (module instanceof DynamicXFAModule) {
                        ((DynamicXFAModule) module).exportData(mExportFilePath, XFADoc.e_ExportDataTypeXML, callback);
                    }
                }
            }
        });

        dialog.show();
    }

    private void resetForm(final Module module) {
        mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_form_reseting);
        showProgressDlg();
        Event.Callback callback = new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                dismissProgressDlg();
                if (success) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().clearUndoRedo();
                }
            }
        };

        if (module instanceof FormFillerModule) {
            ((FormFillerModule) module).resetForm(callback);
        } else if (module instanceof DynamicXFAModule) {
            ((DynamicXFAModule) module).resetForm(callback);
        }
        hideMoreMenu();
    }


    // For Password Encryption
    private MenuItem enItem;
    private MenuItem deItem;
    private MenuItem trustCertItem;
    private UITextEditDialog mSwitchDialog;

    public void addPasswordItems(final PasswordModule module) {
        MenuGroup group = mMoreMenu.getMenuGroup(MoreMenuConfig.GROUP_PROTECT);
        if (group == null) {
            group = new MenuGroup(mContext,
                    MoreMenuConfig.GROUP_PROTECT,
                    AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_group_protect));
            mMoreMenu.addMenuGroup(group);
        }

        enItem = new MenuItem(mContext, MoreMenuConfig.ITEM_PASSWORD,
                AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_encrpty_standard), 0,
                new MenuViewCallback() {
                    @Override
                    public void onClick(MenuItem item) {
                        try {
                            int type = mPdfViewCtrl.getDoc().getEncryptionType();
                            if (type != PDFDoc.e_EncryptNone) {
                                mSwitchDialog = new UITextEditDialog(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
                                mSwitchDialog.getInputEditText().setVisibility(View.GONE);
                                mSwitchDialog.setTitle(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity().getApplicationContext().getString(R.string.rv_doc_encrypt_standard_switch_title));
                                mSwitchDialog.getPromptTextView().setText(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity().getApplicationContext().getString(R.string.rv_doc_encrypt_standard_switch_content));
                                mSwitchDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        mSwitchDialog.dismiss();
                                    }
                                });
                                mSwitchDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        mSwitchDialog.dismiss();
                                        if (module.getPasswordSupport() != null) {
                                            module.getPasswordSupport().passwordManager(PasswordConstants.OPERATOR_TYPE_CREATE);
                                        }
                                    }
                                });
                                mSwitchDialog.show();
                            } else {
                                if (module.getPasswordSupport() != null) {
                                    module.getPasswordSupport().passwordManager(PasswordConstants.OPERATOR_TYPE_CREATE);
                                }
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }
                });

        deItem = new MenuItem(
                mContext,
                MoreMenuConfig.ITEM_REMOVESECURITY_PASSWORD,
                AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_item_remove_encrytion),
                0,
                new MenuViewCallback() {
                    @Override
                    public void onClick(MenuItem item) {
                        if (module.getPasswordSupport() != null) {
                            module.getPasswordSupport().passwordManager(PasswordConstants.OPERATOR_TYPE_REMOVE);
                        }
                    }
                });

        trustCertItem = new MenuItem(
                mContext,
                MoreMenuConfig.ITEM_TRUST_CERTIFICATE,
                AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_item_trust_certificate),
                0,
                new MenuViewCallback() {
                    @Override
                    public void onClick(MenuItem item) {
                        TrustCertificateModule trustCertModule = (TrustCertificateModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_TRUST_CERTIFICATE);
                        if (trustCertModule != null)
                            trustCertModule.show();
                    }
                });
    }

    public void reloadPasswordItem(PasswordModule module) {
        if (mPdfViewCtrl.getDoc() != null) {
            boolean isPPDF = false;
            try {
                String filePath = mPdfViewCtrl.getFilePath();
                if (!TextUtils.isEmpty(filePath)) {
                    isPPDF = filePath.endsWith(".ppdf");
                }
                int encryptType = mPdfViewCtrl.getDoc().getEncryptionType();
                if (encryptType == PDFDoc.e_EncryptPassword  && !isPPDF) {
                    mMoreMenu.removeMenuItem(MoreMenuConfig.GROUP_PROTECT, MoreMenuConfig.ITEM_PASSWORD);
                    mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_PROTECT, deItem);
                } else {
                    mMoreMenu.removeMenuItem(MoreMenuConfig.GROUP_PROTECT, MoreMenuConfig.ITEM_REMOVESECURITY_PASSWORD);
                    mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_PROTECT, enItem);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }

            if (module.getSecurityHandler().isAvailable() && !isPPDF) {
                enItem.setEnable(true);
                deItem.setEnable(true);
            } else {
                enItem.setEnable(false);
                deItem.setEnable(false);
            }

            TrustCertificateModule trustCertModule = (TrustCertificateModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_TRUST_CERTIFICATE);
            if (trustCertModule != null) {
                mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_PROTECT, trustCertItem);
            }
        } else {
            mMoreMenu.setGroupVisibility(View.GONE, MoreMenuConfig.GROUP_PROTECT);
        }
    }

    private void showProgressDlg() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();

        if (mProgressDlg == null && uiExtensionsManager.getAttachedActivity() != null) {
            mProgressDlg = new ProgressDialog(uiExtensionsManager.getAttachedActivity());
            mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDlg.setCancelable(false);
            mProgressDlg.setIndeterminate(false);
        }

        if (mProgressDlg != null && !mProgressDlg.isShowing()) {
            mProgressDlg.setMessage(mProgressMsg);
            AppDialogManager.getInstance().showAllowManager(mProgressDlg, null);
        }
    }

    private void dismissProgressDlg() {
        if (mProgressDlg != null && mProgressDlg.isShowing()) {
            AppDialogManager.getInstance().dismiss(mProgressDlg);
            mProgressDlg = null;
        }
    }


    // for import/export annot

    private MenuItem mAnnotImportItem = null;
    private MenuItem mAnnotExportItem = null;

    public void addAnnotItem() {
        MenuGroup group = mMoreMenu.getMenuGroup(MoreMenuConfig.GROUP_ANNOTATION);
        if (group == null) {
            group = new MenuGroup(mContext, MoreMenuConfig.GROUP_ANNOTATION, mContext.getApplicationContext().getString(R.string.rd_bar_edit));
        }
        mMoreMenu.addMenuGroup(group);
        if (mAnnotImportItem == null) {
            mAnnotImportItem = new MenuItem(
                    mContext,
                    MoreMenuConfig.ITEM_ANNOTATION_IMPORT,
                    AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_item_annot_import),
                    0,
                    new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItem item) {
                            importAnnotFromFDF();
                        }
                    });
        }
        if (mAnnotExportItem == null) {
            mAnnotExportItem = new MenuItem(
                    mContext,
                    MoreMenuConfig.ITEM_ANNOTATION_EXPORT,
                    AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_item_annot_export),
                    0,
                    new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItem item) {
                            exportAnnotToFDF();
                        }
                    });
        }
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_ANNOTATION, mAnnotImportItem);
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_ANNOTATION, mAnnotExportItem);
    }

    protected void reloadAnnotItems() {
        if (mAnnotImportItem != null)
            mAnnotImportItem.setEnable(false);
        if (mAnnotExportItem != null)
            mAnnotExportItem.setEnable(false);

        PDFDoc doc = mPdfViewCtrl.getDoc();
        if (mPdfViewCtrl.isDynamicXFA()) {
            mAnnotImportItem.setEnable(false);
            mAnnotExportItem.setEnable(false);
        } else if (doc != null) {
            DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
            if (documentManager.canAddAnnot()) {
                mAnnotImportItem.setEnable(true);
            }

            if (documentManager.canCopy()) {
                mAnnotExportItem.setEnable(true);
            }
        }

    }

    private void importAnnotFromFDF() {
        mFileSelectDialog = new UIFileSelectDialog(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
        mFileSelectDialog.init(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isHidden() || !pathname.canRead()) return false;
                if (pathname.isFile() && !pathname.getName().toLowerCase().endsWith(".fdf"))
                    return false;
                return true;
            }
        }, true);
        mFileSelectDialog.showDialog();
        mFileSelectDialog.setTitle(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity().getApplicationContext().getString(R.string.formfiller_import_title));
        mFileSelectDialog.setButton(UIMatchDialog.DIALOG_CANCEL | UIMatchDialog.DIALOG_OK);
        mFileSelectDialog.setButtonEnable(false, UIMatchDialog.DIALOG_OK);
        mFileSelectDialog.setListener(new UIMatchDialog.DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == UIMatchDialog.DIALOG_OK) {
                    List<FileItem> files = mFileSelectDialog.getSelectedFiles();

                    mFileSelectDialog.dismiss();
                    mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_form_importing);
                    showProgressDlg();
                    ImportAndExportAnnots task = new ImportAndExportAnnots(files.get(0).path, ImportAndExportAnnots.TYPE_IMPORT, new Event.Callback() {
                        @Override
                        public void result(Event event, boolean success) {
                            dismissProgressDlg();
                            hideMoreMenu();
                            if (success) {
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnosImported();
                                int[] pages = mPdfViewCtrl.getVisiblePages();
                                if (pages != null) {
                                    for (int i = 0; i < pages.length; i++) {
                                        int pageIndex = pages[i];
                                        Rect rect = new Rect(0, 0, mPdfViewCtrl.getPageViewWidth(pageIndex), mPdfViewCtrl.getPageViewHeight(pageIndex));
                                        mPdfViewCtrl.refresh(pageIndex, rect);
                                    }
                                    UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_success_import_data));
                                } else {
                                    UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_unknown_error));
                                }

                            } else {
                                UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_fail_import_data));
                            }
                        }
                    });
                    mPdfViewCtrl.addTask(task);
                } else if (btType == UIMatchDialog.DIALOG_CANCEL) {
                    mFileSelectDialog.dismiss();
                }
            }

            @Override
            public void onBackClick() {

            }
        });
    }

    public void exportAnnotToFDF() {
        final UITextEditDialog dialog = new UITextEditDialog(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
        dialog.setTitle(mContext.getApplicationContext().getString(R.string.formfiller_export_title));
        dialog.getInputEditText().setVisibility(View.VISIBLE);
        String fileNameWithoutExt = AppFileUtil.getFileNameWithoutExt(mFilePath);
        dialog.getInputEditText().setText(fileNameWithoutExt + ".fdf");
        CharSequence text = dialog.getInputEditText().getText();
        if (text instanceof Spannable) {
            Spannable spanText = (Spannable) text;
            Selection.setSelection(spanText, 0, fileNameWithoutExt.length());
        }
        AppUtil.showSoftInput(dialog.getInputEditText());
        dialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.getOKButton().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                String name = dialog.getInputEditText().getText().toString();
                if (name.toLowerCase().endsWith(".fdf")) {
                    mExportFilePath = AppFileUtil.getFileFolder(mFilePath) + "/" + name;
                } else {
                    mExportFilePath = AppFileUtil.getFileFolder(mFilePath) + "/" + name + ".fdf";
                }
                File file = new File(mExportFilePath);
                if (file.exists()) {
                    final UITextEditDialog rmDialog = new UITextEditDialog(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
                    rmDialog.setTitle(mContext.getApplicationContext().getString(R.string.fm_file_exist));
                    rmDialog.getPromptTextView().setText(mContext.getApplicationContext().getString(R.string.fx_string_filereplace_warning));
                    rmDialog.getInputEditText().setVisibility(View.GONE);
                    rmDialog.show();

                    rmDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            rmDialog.dismiss();
                            mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_form_exporting);
                            showProgressDlg();
                            Event.Callback callback = new Event.Callback() {
                                @Override
                                public void result(Event event, boolean success) {
                                    dismissProgressDlg();
                                    hideMoreMenu();
                                    if (success) {
                                        UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_success_export_data));
                                    } else {
                                        UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.formfiller_export_error));
                                    }
                                }
                            };

                            ImportAndExportAnnots task = new ImportAndExportAnnots(mExportFilePath, ImportAndExportAnnots.TYPE_EXPORT, callback);
                            mPdfViewCtrl.addTask(task);
                        }
                    });

                    rmDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            rmDialog.dismiss();
                            exportAnnotToFDF();
                        }
                    });
                } else {
                    mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_form_exporting);
                    showProgressDlg();
                    Event.Callback callback = new Event.Callback() {
                        @Override
                        public void result(Event event, boolean success) {
                            dismissProgressDlg();
                            hideMoreMenu();
                            if (success) {
                                UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.menu_more_success_export_data));
                            } else {
                                UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.formfiller_export_error));
                            }
                        }
                    };

                    ImportAndExportAnnots task = new ImportAndExportAnnots(mExportFilePath, ImportAndExportAnnots.TYPE_EXPORT, callback);
                    mPdfViewCtrl.addTask(task);
                }
            }
        });

        dialog.show();
    }

    class ImportAndExportAnnots extends Task {
        public static final int TYPE_IMPORT = 0;
        public static final int TYPE_EXPORT = 1;
        private boolean mRet;
        private int mType;
        private String mPath;

        public ImportAndExportAnnots(String path, int type, final Event.Callback callBack) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    callBack.result(null, ((ImportAndExportAnnots) task).mRet);
                }
            });
            mPath = path;
            mType = type;
        }

        @Override
        protected void execute() {
            try {
                PDFViewCtrl.lock();
                if (mType == TYPE_IMPORT) {
                    FDFDoc fdfDoc = new FDFDoc(mPath);
                    mRet = mPdfViewCtrl.getDoc().importFromFDF(fdfDoc, PDFDoc.e_Annots, new com.foxit.sdk.common.Range());
                } else if (mType == TYPE_EXPORT) {
                    FDFDoc fdfDoc = new FDFDoc(FDFDoc.e_FDF);
                    mRet = mPdfViewCtrl.getDoc().exportToFDF(fdfDoc, PDFDoc.e_Annots, new com.foxit.sdk.common.Range());
                    if (mRet) {
                        fdfDoc.saveAs(mPath);
                    }
                }
            } catch (PDFException e) {
                mRet = false;
            } finally {
                PDFViewCtrl.unlock();
            }
        }
    }

    protected void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
        if (mMenuPopupWindow != null && mMenuPopupWindow.isShowing()) {
            if (oldWidth != newWidth || oldHeight != newHeight) {
                updateMoreMenu(newWidth, newHeight);
            }
        }
    }

}
