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
package com.foxit.uiextensions.home.local;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.addon.comparison.Comparison;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.PasswordDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFileSelectDialog;
import com.foxit.uiextensions.controls.filebrowser.FileBrowser;
import com.foxit.uiextensions.controls.filebrowser.FileComparator;
import com.foxit.uiextensions.controls.filebrowser.FileDelegate;
import com.foxit.uiextensions.controls.filebrowser.imp.FileBrowserImpl;
import com.foxit.uiextensions.controls.filebrowser.imp.FileItem;
import com.foxit.uiextensions.controls.filebrowser.imp.FileThumbnail;
import com.foxit.uiextensions.controls.popupwindow.ColorPopupWindow;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.home.IHomeModule;
import com.foxit.uiextensions.home.view.PathView;
import com.foxit.uiextensions.modules.compare.ComparisonPDF;
import com.foxit.uiextensions.modules.connectpdf.account.AccountModule;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppStorageManager;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIToast;
import com.foxit.uiextensions.utils.thread.AppAsyncTask;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class LocalModule implements Module, IHomeModule{
    public static final int STATE_NORMAL = 0;
    public static final int STATE_ALL_PDF = 1;
    public static final int STATE_EDIT = 2;

    protected static final int MSG_UPDATE_PDFs = 11002;
    protected static final int MSG_PDFs_STOP = 11008;

    protected static final int MSG_FILE_OBSERVER = 11010;
    protected static final int MSG_UPDATE_THUMBNAIL = 11012;
    private final Context mContext;

    private RelativeLayout mRootView;
    private RelativeLayout mContentView;
    private RelativeLayout mTopToolBar;
    private LocalView mLocalView;
    private PathView mPathView;
    private BaseBar mTopBar;
    private FileBrowser mFileBrowser;
    private FileObserver mFileObserver;

    private String mCurrentPath;
    private int mCurrentState = STATE_ALL_PDF;
    private int mLastState = -1;
    private int mSortMode = 1;
    private boolean isSortUp = true;
    private boolean mIsMkDirSuccess = false;

    private BaseItemImpl mEditFinishItem;
    private BaseItemImpl mEditCounterItem;
    private BaseItemImpl mEditDividerItem;
    private BaseItemImpl mEditCompareItem;

    private BaseItemImpl mEditItem;

    private onFileItemEventListener mOnFileItemEventListener = null;
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    private final List<FileItem> mFileItems = new ArrayList<FileItem>();
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case MSG_UPDATE_PDFs:
                    if (msg.obj instanceof FileItem[]) {
                        FileItem[] items = (FileItem[]) msg.obj;
                        if (mCurrentState == STATE_ALL_PDF || (mLastState == STATE_ALL_PDF && mCurrentState == STATE_EDIT)) {
                            Collections.addAll(mFileItems, items);
                        }
                        mFileBrowser.updateDataSource(true);
                    }
                    break;
                case MSG_FILE_OBSERVER:
                    mFileBrowser.clearCheckedItems();
                    mFileItems.clear();
                    LocalTask.AllPDFs.start(mContext, mHandler);
                    mFileBrowser.updateDataSource(false);
                    break;
                case MSG_UPDATE_THUMBNAIL:
                    mFileBrowser.updateDataSource(false);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public String getName() {
        return MODULE_NAME_LOCAL;
    }

    public LocalModule(Context context) {
        mContext = context;
        mFileObserver = new SDCardFileObserver(Environment.getExternalStorageDirectory().getPath());
    }

    @Override
    public boolean loadModule() {
        mFileObserver.startWatching();
        AccountModule.getInstance().registerAccountEventListener(mAccountListener);
        loadHomeModule(mContext);
        onActivated();
        return true;
    }

    @Override
    public boolean unloadModule() {
        mDisposable.clear();
        mFileObserver.stopWatching();
        mOnFileItemEventListener = null;
        mActivity = null;
        AccountModule.getInstance().unregisterAccountEventListener(mAccountListener);
        return true;
    }

    @Override
    public String getTag() {
        return HOME_MODULE_TAG_LOCAL;
    }

    @Override
    public void loadHomeModule(Context context) {
        if (context == null) return;
        initItems(context);
        if (mTopBar == null) {
            mTopBar = new TopBarImpl(context);
            mTopBar.setBackgroundColor(context.getResources().getColor( R.color.ux_text_color_subhead_colour));
        }
        if (mLocalView == null) {
            mLocalView = new LocalView(context);
            mPathView = new PathView(context);

            mFileBrowser = new FileBrowserImpl(context, mFileBrowserDelegate);
            mLocalView.addFileView(mFileBrowser.getContentView());
            mPathView.setPathChangedListener(new PathView.pathChangedListener() {
                @Override
                public void onPathChanged(String newPath) {
                    mFileBrowser.setPath(newPath);
                }
            });
        }
        if (AppFileUtil.isSDAvailable()) {
            if (mCurrentPath == null) {
                mCurrentPath = AppFileUtil.getSDPath() + File.separator + "FoxitSDK";
            }
            File file = new File(mCurrentPath);
            if (!file.exists())
                mIsMkDirSuccess = file.mkdirs();
            if (!file.exists()) {
                mCurrentPath = AppFileUtil.getSDPath();
            } else {
                mCurrentPath = file.getPath();
            }
            mPathView.setPath(mCurrentPath);
            mFileBrowser.setPath(mCurrentPath);

//            if (!new File(file.getPath() + File.separator + "Sample.pdf").exists()) {
//                CopyAsy task = new CopyAsy();
//                AppThreadManager.getInstance().startThread(task, file.getPath());
//            }
//            if (!new File(file.getPath() + File.separator + "complete_pdf_viewer_guide_android.pdf").exists()) {
//                CopyAsy task = new CopyAsy();
//                AppThreadManager.getInstance().startThread(task, file.getPath());
//            }
        }

        if (AppDisplay.getInstance(context).isPad())
            mRootView = (RelativeLayout) View.inflate(mContext, R.layout.hf_home_right_pad, null);
        else
            mRootView = (RelativeLayout) View.inflate(mContext, R.layout.hf_home_right_phone, null);

        mTopToolBar = (RelativeLayout) mRootView.findViewById(R.id.toptoolbar);
        mContentView = (RelativeLayout) mRootView.findViewById(R.id.contentview);

        mContentView.removeAllViews();
        mContentView.addView(mLocalView);

        View view = mTopBar.getContentView();
        if (view == null) {
            mTopToolBar.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mContentView.getLayoutParams();
            params.topMargin = 0;
            mContentView.setLayoutParams(params);
        } else {
            mTopToolBar.setVisibility(View.VISIBLE);
            mTopToolBar.addView(view);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mContentView.getLayoutParams();
            if (AppDisplay.getInstance(context).isPad())
                params.topMargin = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            else
                params.topMargin = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
            mContentView.setLayoutParams(params);
        }

        resetSortMode(); //init sort mode
//        mCurrentState = STATE_ALL_PDF;
//        setStateAllPDFs();

        //List folders and PDFs
        switchState(STATE_NORMAL);
        mLastState = -1;
    }

    @Override
    public void unloadHomeModule(Context context) {
    }

    @Override
    public View getTopToolbar(Context context) {
        return mTopBar.getContentView();
    }

    @Override
    public BaseBar getTopToolbar() {
        return mTopBar;
    }

    @Override
    public View getContentView(Context context) {
        return mRootView;
    }

    @Override
    public boolean isNewVersion() {
        return false;
    }

    private void setStateAllPDFs() {
        mTopBar.removeAllItems();
        mTopBar.addView(mSignItem, BaseBar.TB_Position.Position_LT);
        mTopBar.addView(mDocumentItem, BaseBar.TB_Position.Position_LT);
        mTopBar.addView(mEditItem, BaseBar.TB_Position.Position_RB);
        mLocalView.removeAllTopView();
        mLocalView.setTopLayoutVisible(false);
        mLocalView.setBottomLayoutVisible(false);
    }

    private void setStateNormal() {
        mTopBar.removeAllItems();
        mTopBar.addView(mSignItem, BaseBar.TB_Position.Position_LT);
        mTopBar.addView(mDocumentItem, BaseBar.TB_Position.Position_LT);
        mLocalView.removeAllTopView();
        mLocalView.setTopLayoutVisible(!AppUtil.isEmpty(mFileBrowser.getDisplayPath()));
        mLocalView.addPathView(mPathView.getContentView());

        mTopBar.addView(mEditItem, BaseBar.TB_Position.Position_RB);

        mLocalView.setBottomLayoutVisible(false);
    }

    private void setStateEdit() {
        mTopBar.removeAllItems();
        mTopBar.addView(mEditFinishItem, BaseBar.TB_Position.Position_LT);
        mTopBar.addView(mEditDividerItem, BaseBar.TB_Position.Position_LT);
        mTopBar.addView(mEditCounterItem, BaseBar.TB_Position.Position_LT);
        mTopBar.addView(mEditCompareItem, BaseBar.TB_Position.Position_RB);

        mEditCounterItem.setText("0");
        mEditCompareItem.setEnable(false);
        mLocalView.removeAllTopView();
        mLocalView.setTopLayoutVisible(true);

        mLocalView.setBottomLayoutVisible(true);
        mLocalView.addPathView(mPathView.getContentView());
        mFileBrowserDelegate.onItemsCheckedChanged(false, 0, 0);// TODO: 2019/6/11
    }

    @Override
    public void onActivated() {
        if (mCurrentState == STATE_ALL_PDF) {
            setStateAllPDFs();
            LocalTask.AllPDFs.stop();
            mFileItems.clear();
            LocalTask.AllPDFs.start(mContext, mHandler);
            mFileBrowser.updateDataSource(true);
        }
    }

    @Override
    public void onDeactivated() {
    }

    @Override
    public boolean onWillDestroy() {
        return false;
    }

    @Override
    public void setFileItemEventListener(onFileItemEventListener listener) {
        mOnFileItemEventListener = listener;
    }


    private FileDelegate mFileBrowserDelegate = new FileDelegate() {

        @Override
        public List<FileItem> getDataSource() {
            return mFileItems;
        }

        @Override
        public void onPathChanged(String path) {
            if (mCurrentState != STATE_NORMAL && mCurrentState != STATE_EDIT) return;
            if (AppUtil.isEmpty(path)) {
                mPathView.setPath(null);
                mEditItem.setEnable(false);
                mLocalView.setTopLayoutVisible(false);
                mFileItems.clear();
                List<String> paths = AppStorageManager.getInstance(mContext).getVolumePaths();
                for (String p : paths) {
                    File f = new File(p);
                    FileItem item = new FileItem();
                    item.parentPath = path;
                    item.path = f.getPath();
                    item.name = f.getName();
                    item.date = AppDmUtil.getLocalDateString(AppDmUtil.javaDateToDocumentDate(f.lastModified()));
                    item.lastModifyTime = f.lastModified();
                    item.type = FileItem.TYPE_ROOT;
                    File[] fs = f.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            if (pathname.isHidden() || !pathname.canRead()) return false;
                            return true;
                        }
                    });
                    if (fs != null) {
                        item.fileCount = fs.length;
                    } else {
                        item.fileCount = 0;
                    }
                    mFileItems.add(item);
                }
                return;
            }
            File file = new File(path);
            if (!file.exists()) return;
            File[] files;
            try {
                files = file.listFiles(mFileFilter);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            mCurrentPath = path;
            mEditItem.setEnable(true);
            mLocalView.setTopLayoutVisible(true);
            mFileItems.clear();
            mPathView.setPath(mCurrentPath);
            if (files == null) return;
            for (File f : files) {
                FileItem item = new FileItem();
                item.parentPath = file.getPath();
                item.path = f.getPath();
                item.name = f.getName();
                item.date = AppDmUtil.getLocalDateString(AppDmUtil.javaDateToDocumentDate(f.lastModified()));
                item.lastModifyTime = f.lastModified();
                if (f.isDirectory()) {
                    item.type = FileItem.TYPE_FOLDER;
                    File[] fs = f.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            if (pathname.isHidden() || !pathname.canRead()) return false;
                            if (pathname.isDirectory()) return true;
                            return pathname.isFile();// && pathname.getName().toLowerCase().endsWith(".pdf");
                        }
                    });
                    if (fs != null) {
                        item.fileCount = fs.length;
                    } else {
                        item.fileCount = 0;
                    }
                    mFileItems.add(item);
                    continue;
                }
                item.type = FileItem.TYPE_FILE;
                item.size = AppFileUtil.formatFileSize(f.length());
                item.length = f.length();

                if (mFileBrowser.getCheckedItems().size() > 0) {
                    for (int i = 0; i < mFileBrowser.getCheckedItems().size(); i ++) {
                        if (item.path.equalsIgnoreCase(mFileBrowser.getCheckedItems().get(i).path)) {
                            item.checked = mFileBrowser.getCheckedItems().get(i).checked;
                            break;
                        }
                    }
                }
                mFileItems.add(item);
            }
            if (mFileItems.size() == 0) {
                mEditItem.setEnable(false);
            } else {
                mEditItem.setEnable(true);
            }
            Collections.sort(mFileItems, mFileBrowser.getComparator());
        }

        @Override
        public void onItemClicked(View view, FileItem item) {
            if (item.type == FileItem.TYPE_FOLDER || item.type == FileItem.TYPE_ROOT) {
                mFileBrowser.setPath(item.path);
            } else if ((item.type & FileItem.TYPE_FILE) != 0) {
                if (mOnFileItemEventListener != null) {
                    mOnFileItemEventListener.onFileItemClicked(FILE_EXTRA, item.path);
                } else {
                    UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.the_fileitem_listener_is_null));
                }
            }
        }

        @Override
        public void onItemsCheckedChanged(boolean isAllSelected, int folderCount, int fileCount) {
            mEditCounterItem.setText(fileCount + "");
            if (fileCount == 2) {
                mEditCompareItem.setEnable(true);
            } else {
                mEditCompareItem.setEnable(false);
            }
        }

    };

    private void switchState(int state) {
        if (mCurrentState == state) return;
        if (mCurrentState == STATE_ALL_PDF) {
            LocalTask.AllPDFs.stop();
        } else if (mCurrentState == STATE_EDIT) {
            mFileBrowser.setEditState(false);
        }

        if (state == STATE_NORMAL) {
            mLastState = mCurrentState;
            mCurrentState = state;
            setStateNormal();
            if (mLastState != STATE_EDIT) {
                mFileBrowser.setPath(mFileBrowser.getDisplayPath());
            }
            return;
        }

        if (state == STATE_EDIT) {
            mLastState = mCurrentState;
            mCurrentState = state;
            setStateEdit();
            mFileBrowser.setEditState(true);
            return;
        }

        if (state == STATE_ALL_PDF) {
            mLastState = mCurrentState;
            mCurrentState = state;
            setStateAllPDFs();
            if (mLastState != STATE_EDIT) {
                mFileItems.clear();
                LocalTask.AllPDFs.start(mContext, mHandler);
            }

            mFileBrowser.updateDataSource(true);
        }
    }

    private void resetSortMode() {
        if (mSortMode == 0) {
            if (isSortUp) {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_TIME_UP);
            } else {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_TIME_DOWN);
            }
        } else if (mSortMode == 1) {
            if (isSortUp) {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_NAME_UP);
            } else {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_NAME_DOWN);
            }
        } else if (mSortMode == 2) {
            if (isSortUp) {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_SIZE_UP);
            } else {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_SIZE_DOWN);
            }
        }
        if (!mFileItems.isEmpty()) {
            Collections.sort(mFileItems, mFileBrowser.getComparator());
            mFileBrowser.updateDataSource(true);
        }
    }

    private FileFilter mFileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            if (pathname.isHidden() || !pathname.canRead()) return false;
            if (mCurrentState == STATE_NORMAL || mCurrentState == STATE_EDIT) {
                if (pathname.isDirectory()) return true;
            }
//            String name = pathname.getName().toLowerCase();
            return pathname.isFile();// && name.endsWith(".pdf");
        }
    };

    private BaseItemImpl mDocumentItem;
    private BaseItemImpl mSignItem;

    private void initItems(Context context) {
        mSignItem = new BaseItemImpl(context);
        mSignItem.setId(R.id.fb_local_item_sign);
        mSignItem.setImageResource(R.drawable.hm_sign_selector);
//        mSignItem.setImageContentDescription(AppResource.getString(mContext, R.string.hm_sign));
        mSignItem.setRelation(BaseItemImpl.RELATION_BELOW);
        mSignItem.setText(context.getApplicationContext().getString(R.string.hm_sign));
//        mSignItem.setTextSize(AppDisplay.getInstance(context).px2dp(context.getResources().getDimensionPixelOffset(R.dimen.ux_text_height_title)));
        mSignItem.setTextColor(context.getResources().getColor(R.color.ux_color_white));
        mSignItem.setOnClickListener(mOnClickListener);

        mDocumentItem = new BaseItemImpl(context);
        mDocumentItem.setText(context.getApplicationContext().getString(R.string.hm_document));
        mDocumentItem.setTextSize(AppDisplay.getInstance(context).px2dp(context.getResources().getDimensionPixelOffset(R.dimen.ux_text_height_title)));
        mDocumentItem.setTextColor(context.getResources().getColor(R.color.ux_color_white));

        //for compare
        mEditItem = new BaseItemImpl(context);
        mEditItem.setId(R.id.fb_local_item_edit);
        mEditItem.setImageResource(R.drawable.hm_edit_selector);
        mEditItem.setOnClickListener(mOnClickListener);

        mEditFinishItem = new BaseItemImpl(context);
        mEditFinishItem.setId(R.id.fb_local_item_edit_finish);
        mEditFinishItem.setImageResource(R.drawable.cloud_back);
        mEditFinishItem.setOnClickListener(mOnClickListener);

        mEditCounterItem = new BaseItemImpl(context);
        mEditCounterItem.setId(R.id.fb_local_item_edit_counter);
        mEditCounterItem.setTextColor(context.getResources().getColor(R.color.ux_color_white));
        mEditCounterItem.setTextSize(AppDisplay.getInstance(context).px2dp(mContext.getApplicationContext().getResources().getDimensionPixelOffset(R.dimen.ux_text_height_title)));

        mEditDividerItem = new BaseItemImpl(context);
        View view = new View(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(AppDisplay.getInstance(context).dp2px(1), AppDisplay.getInstance(context).dp2px(24));
        view.setLayoutParams(params);
        view.setBackgroundColor(context.getResources().getColor(R.color.ux_color_white));
        mEditDividerItem.setContentView(view);

        mEditCompareItem = new BaseItemImpl(context);
        mEditCompareItem.setId(R.id.fb_local_item_edit_compare);
        mEditCompareItem.setImageResource(R.drawable.fb_compare_selector);
        mEditCompareItem.setOnClickListener(mOnClickListener);
        mEditCompareItem.setEnable(false);
    }

    AccountModule.IAccountEventListener mAccountListener = new AccountModule.IAccountEventListener() {
        @Override
        public void onSignIn(boolean success) {
            if (success) {
                AppThreadManager.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSignItem.setImageResource(R.drawable.hm_signin);

                        mSignItem.setId(R.id.fb_local_item_signout);
                        mSignItem.setText(mContext.getApplicationContext().getString(R.string.hm_signout));
                    }
                });
            }
        }

        @Override
        public void onSignOut(boolean success) {
            if (success) {
                AppThreadManager.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSignItem.setImageResource(R.drawable.hm_sign_selector);

                        mSignItem.setId(R.id.fb_local_item_sign);
                        mSignItem.setText(mContext.getApplicationContext().getString(R.string.hm_sign));
                    }
                });
            }
        }
    };

    public void updateStoragePermissionGranted() {
        if (AppFileUtil.isSDAvailable()) {
            if (mCurrentPath == null || !mIsMkDirSuccess) {
                mCurrentPath = AppFileUtil.getSDPath() + File.separator + "FoxitSDK";
            }
            File curFile = new File(mCurrentPath);
            if (!curFile.exists())
                mIsMkDirSuccess = curFile.mkdirs();
            if (!curFile.exists()) {
                mCurrentPath = AppFileUtil.getSDPath();
            } else {
                mCurrentPath = curFile.getPath();
            }
            mPathView.setPath(mCurrentPath);
            mFileBrowser.setPath(mCurrentPath);
        }
        resetSortMode(); //init sort mode
        mCurrentState = STATE_ALL_PDF;
        switchState(STATE_NORMAL);
        onActivated();
    }

    public void updateThumbnail(String filePath) {
        mFileBrowser.updateThumbnail(filePath, new FileThumbnail.ThumbnailCallback() {
            @Override
            public void result(boolean succeed, String filePath) {
                Message msg = new Message();
                msg.what = MSG_UPDATE_THUMBNAIL;
                mHandler.sendMessage(msg);
            }
        });
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.fb_local_item_sign) {
                AccountModule.getInstance().showLoginDialog(null);
                return;
            }

            if (id == R.id.fb_local_item_signout) {
                AccountModule.getInstance().showLogoutDialog();
                return;
            }

            if (id == R.id.fb_local_item_edit) {
                if (AppUtil.isFastDoubleClick()) return;
                switchState(STATE_EDIT);
                return;
            }
            if (id == R.id.fb_local_item_edit_finish) {
                if (AppUtil.isFastDoubleClick()) return;
                switchState(mLastState);
                onFinishEdit();
                return;
            }
            if (id == R.id.fb_local_item_edit_compare) {
                if (AppUtil.isFastDoubleClick()) return;

                initComparisonOption();

                checkDocAndShowComparisonDialog();
                return;
            }
        }
    };

    class SDCardFileObserver extends FileObserver {
        public SDCardFileObserver(String path, int mask) {
            super(path, mask);
        }

        public SDCardFileObserver(String path) {
            super(path);
        }

        @Override
        public void onEvent(int event, String path) {
            final int action = event & FileObserver.ALL_EVENTS;
            switch (action) {
                case FileObserver.ACCESS:
                case FileObserver.OPEN:
                    break;
                case FileObserver.CREATE:
                case FileObserver.DELETE:
                case FileObserver.MODIFY:
                    Message msg = new Message();
                    msg.what = MSG_FILE_OBSERVER;
                    mHandler.sendMessage(msg);
                    break;
                default:
                    break;
            }
        }

    }


    public void setCurrentPath(String path) {
        if (path == null || path.trim().length() < 1) {
            return;
        }
        mCurrentPath = path;
    }

    /**
     * copy the file from assert to the target file.
     *
     * @param file The target file.
     *
     * @return
     */
    public boolean copyFileFromAssertsToTargetFile(File file) {
        if (file == null || file.isDirectory()) {
            return false;
        }


        if (AppFileUtil.isSDAvailable()) {
//            if (mCurrentPath == null) {
//                mCurrentPath = AppFileUtil.getSDPath() + File.separator + "FoxitSDK";
//            }
//            File curFile = new File(mCurrentPath);
//            if (!curFile.exists())
//                curFile.mkdirs();
//            if (!curFile.exists()) {
//                mCurrentPath = AppFileUtil.getSDPath();
//            } else {
//                mCurrentPath = curFile.getPath();
//            }

            CopyAsy task = new CopyAsy(file);
            AppThreadManager.getInstance().startThread(task, file.getParent());
            return true;
        }
        return false;
    }

    class CopyAsy extends AppAsyncTask {

        private File mFile;
        public CopyAsy(File file) {
            mFile = file;
        }

        @Override
        public String doInBackground(Object... params) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File[] files = new File[1];
                files[0] = new File(params[0] + File.separator + mFile.getName());
                String[] assertFiles = new String[] {mFile.getName()};
                if (mergeFiles(files, assertFiles)) {
                    return params[0] + File.separator + mFile.getName();
                }
            }
            return null;
        }

        @Override
        public void onPostExecute(Object result) {
            if (mFileBrowser != null) {
                mFileBrowser.setPath(mCurrentPath);
                mFileBrowser.updateDataSource(true);
            }
        }
    }

    private boolean mergeFiles(File[] outFile, String[] files) {
        boolean success = false;
        for(int i = 0; i < outFile.length; i++) {
            OutputStream os = null;

            try {
                os = new FileOutputStream(outFile[i]);
                byte[] buffer = new byte[1 << 13];

                InputStream is = mContext.getAssets().open(files[i]);
                int len = is.read(buffer);
                while (len != -1) {
                    os.write(buffer, 0, len);
                    len = is.read(buffer);
                }
                is.close();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (os != null) {
                        os.flush();
                        os.close();
                        success = true;
                    }
                } catch (IOException ignore) {
                }
            }
        }
        return success;
    }

    /** Interface for editing file state.*/
    public interface IFinishEditListener {
        /**
         * When finish editing file state will call this method.
         */
        void onFinishEdit();
    }

    private ArrayList<IFinishEditListener> mFinishEditListeners = new ArrayList<IFinishEditListener>();

    /**
     * Register a <CODE>IFinishEditListener</CODE>  listener.
     *
     * @param listener An <CODE>IFinishEditListener</CODE> object to be registered.
     */
    public void registerFinishEditListener(IFinishEditListener listener) {
        mFinishEditListeners.add(listener);
    }

    /**
     * Unregister a <CODE>IFinishEditListener</CODE>  listener.
     *
     * @param listener An <CODE>IFinishEditListener</CODE> object to be unregistered.
     */
    public void unregisterFinishEditListener(IFinishEditListener listener) {
        mFinishEditListeners.remove(listener);
    }

    private void onFinishEdit() {
        for (IFinishEditListener listener : mFinishEditListeners) {
            listener.onFinishEdit();
        }
    }

    private Activity mActivity = null;
    public void setAttachedActivity(Activity attachedActivity) {
        mActivity = attachedActivity;
    }

    private UIMatchDialog mComparisonDialog = null;
    private View mComparisonView = null;
    private ColorPopupWindow mColorPopupWindow = null;
    private ComparisonPDF.ComparisonOption mOldFileOption = null;
    private ComparisonPDF.ComparisonOption mNewFileOption = null;
    private PDFDoc mOldDoc = null;
    private PDFDoc mNewDoc = null;
    private void showComparisonDialog() {
        if (mActivity == null) {
            throw new RuntimeException("The attached activity is null.");
        }
        if (mComparisonDialog == null) {
            mComparisonDialog = new UIMatchDialog(mActivity);
            mComparisonDialog.setBackButtonVisible(View.GONE);
            mComparisonDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.hm_comparison_title));
            mComparisonDialog.setTitlePosition(BaseBar.TB_Position.Position_CENTER);
            mComparisonDialog.setButton(MatchDialog.DIALOG_OK | MatchDialog.DIALOG_CANCEL);
            mComparisonDialog.setButtonEnable(true, MatchDialog.DIALOG_OK);
            mComparisonDialog.setButtonEnable(true, MatchDialog.DIALOG_CANCEL);
            mComparisonDialog.setListener(new MatchDialog.DialogListener() {
                @Override
                public void onResult(long btType) {
                    if (btType == MatchDialog.DIALOG_OK) {
                        if (mOldFileOption.filePath.equals(mNewFileOption.filePath)) {
                            UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.compare_diff_file));
                            return;
                        }
                        mComparisonDialog.setButtonEnable(false, MatchDialog.DIALOG_OK);
                        mComparisonDialog.setButtonEnable(false, MatchDialog.DIALOG_CANCEL);
                        final FxProgressDialog dialog = new FxProgressDialog(mActivity, AppResource.getString(mContext.getApplicationContext(), R.string.compare_progress));
                        dialog.show();
                        mDisposable.add(ComparisonPDF.doCompare(mContext, mOldDoc, mNewDoc, Comparison.e_CompareTypeAll)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<String>() {
                                    @Override
                                    public void accept(String s) throws Exception {
                                        int state = ICompareListener.STATE_SUCCESS;
                                        if (s == null) {
                                            state = ICompareListener.STATE_ERROR;
                                        }

                                        dialog.dismiss();
                                        afterComparisonClicked(state, s);
                                        switchState(mLastState);
                                        onFinishEdit();
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        dialog.dismiss();
                                        afterComparisonClicked(ICompareListener.STATE_ERROR, null);
                                    }
                                }));

                    } else {
                        mFileBrowser.clearCheckedItems();
                        mFileBrowser.updateDataSource(false);
                        afterComparisonClicked(ICompareListener.STATE_CANCEL, null);
                    }
                }

                @Override
                public void onBackClick() {

                }
            });
        } else {
            mComparisonDialog.setButtonEnable(true, MatchDialog.DIALOG_OK);
            mComparisonDialog.setButtonEnable(true, MatchDialog.DIALOG_CANCEL);
        }

        mComparisonDialog.setContentView(getComparisonView());
        mComparisonDialog.setHeight(mComparisonDialog.getDialogHeight());
        mComparisonDialog.showDialog();
    }

    private View getComparisonView() {
        if (mComparisonView == null) {
            mComparisonView = View.inflate(mContext, R.layout.hm_comparison_layout, null);
        }

        final LinearLayout llOverlay = mComparisonView.findViewById(R.id.ll_comparison_mode_overlay);
        final LinearLayout llSideBySide = mComparisonView.findViewById(R.id.ll_comparison_mode_side_by_side);
        final TextView tvOldFile = mComparisonView.findViewById(R.id.tv_old_file_name);
        final TextView tvNewFile = mComparisonView.findViewById(R.id.tv_new_file_name);
        final LinearLayout llOldFileSelectColor = mComparisonView.findViewById(R.id.ll_comparison_old_file_selected_color);
        final LinearLayout llNewFileSelectColor = mComparisonView.findViewById(R.id.ll_comparison_new_file_selected_color);

        final TextView tvOldSelected = mComparisonView.findViewById(R.id.tv_old_file_selected_color);
        final TextView tvNewSelected = mComparisonView.findViewById(R.id.tv_new_file_selected_color);

        // unsupported now.
        llOverlay.setVisibility(View.GONE);
        llOldFileSelectColor.setVisibility(View.GONE);
        llNewFileSelectColor.setVisibility(View.GONE);
        tvOldSelected.setVisibility(View.GONE);
        tvNewSelected.setVisibility(View.GONE);

        View.OnClickListener comparisonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.ll_comparison_mode_overlay) {
                    llOverlay.setBackground(mContext.getResources().getDrawable(R.drawable.hm_comparison_selected_background));
                    llSideBySide.setBackground(mContext.getResources().getDrawable(R.drawable.hm_comparison_unselected_background));
                } else if (v.getId() == R.id.ll_comparison_mode_side_by_side){
                    llSideBySide.setBackground(mContext.getResources().getDrawable(R.drawable.hm_comparison_selected_background));
                    llOverlay.setBackground(mContext.getResources().getDrawable(R.drawable.hm_comparison_unselected_background));
                } else if (v.getId() == R.id.tv_old_file_name) {
                    showFileSelectDialog(tvOldFile, mOldFileOption);
                } else if (v.getId() == R.id.tv_new_file_name) {
                    showFileSelectDialog(tvNewFile, mNewFileOption);
                } else if (v.getId() == R.id.ll_comparison_old_file_selected_color) {
                    Rect rect = new Rect();
                    llOldFileSelectColor.getGlobalVisibleRect(rect);
                    showColorView(llOldFileSelectColor, mOldFileOption, new RectF(rect));
                } else if (v.getId() == R.id.ll_comparison_new_file_selected_color) {
                    Rect rect = new Rect();
                    llNewFileSelectColor.getGlobalVisibleRect(rect);
                    showColorView(llNewFileSelectColor, mNewFileOption, new RectF(rect));
                }
            }
        };

//        mOldFileOption.displayColor = ((ColorDrawable)llOldFileSelectColor.getBackground()).getColor();
//        mNewFileOption.displayColor = ((ColorDrawable)llNewFileSelectColor.getBackground()).getColor();

        // show comparison files name
        int index = mOldFileOption.filePath.lastIndexOf(File.separatorChar);
        String name = mOldFileOption.filePath.substring(index + 1);
        tvOldFile.setText(name);

        index = mNewFileOption.filePath.lastIndexOf(File.separatorChar);
        name = mNewFileOption.filePath.substring(index + 1);
        tvNewFile.setText(name);


        //binding listener
        llOverlay.setOnClickListener(comparisonListener);
        llSideBySide.setOnClickListener(comparisonListener);

        tvOldFile.setOnClickListener(comparisonListener);
        tvNewFile.setOnClickListener(comparisonListener);

        llOldFileSelectColor.setOnClickListener(comparisonListener);
        llNewFileSelectColor.setOnClickListener(comparisonListener);

        return mComparisonView;
    }

    private ColorPopupWindow getColorPopupWindow() {
        if (mColorPopupWindow == null) {
            mColorPopupWindow = new ColorPopupWindow(mContext, (ViewGroup) mComparisonView);
            int[] colors = new int[PropertyBar.PB_COLORS_SQUARE.length];
            System.arraycopy(PropertyBar.PB_COLORS_SQUARE, 0, colors, 0, colors.length);
            colors[0] = PropertyBar.PB_COLORS_SQUARE[0];
            mColorPopupWindow.setColors(colors);
        }
        return mColorPopupWindow;
    }

    private void showColorView(final View selectedColorView, final ComparisonPDF.ComparisonOption option, RectF rect) {
        ColorPopupWindow colorPopupWindow = getColorPopupWindow();
        colorPopupWindow.setValue(((ColorDrawable)selectedColorView.getBackground()).getColor());
        colorPopupWindow.reset();
        colorPopupWindow.setColorChangedListener(new ColorPopupWindow.IColorChangedListener() {
            @Override
            public void onValueChanged(int value) {
                selectedColorView.setBackgroundColor(value);
                option.displayColor = value;
            }
        });
        if (colorPopupWindow.isShowing()) {
            colorPopupWindow.update(rect);
        } else {
            colorPopupWindow.show(rect, false);
        }
    }

    private UIFileSelectDialog mFileSelectDialog = null;
    private void showFileSelectDialog(final TextView selectedFileView, final ComparisonPDF.ComparisonOption option) {
        if (mFileSelectDialog != null && mFileSelectDialog.isShowing()) return;
        if (mActivity == null) {
            throw new RuntimeException("The attached activity is null.");
        }

//        if (mFileSelectDialog == null) {
            mFileSelectDialog = new UIFileSelectDialog(mActivity);
            mFileSelectDialog.init(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.isHidden() || !pathname.canRead()) return false;
                    return true;
                }
            }, true);
            mFileSelectDialog.setTitle(mContext.getString(R.string.hm_comparison_selected_file_title));
            mFileSelectDialog.setButton(UIMatchDialog.DIALOG_CANCEL | UIMatchDialog.DIALOG_OK);
            mFileSelectDialog.setButtonEnable(false, UIMatchDialog.DIALOG_OK);
            mFileSelectDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        mFileSelectDialog.dismiss();
                    }
                    return true;
                }
            });
//        } else {
//            mFileSelectDialog.notifyDataSetChanged();
//        }

        mFileSelectDialog.setListener(new UIMatchDialog.DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == UIMatchDialog.DIALOG_OK) {
                    final FileItem fileItem = mFileSelectDialog.getSelectedFiles().get(0);
                    try {
                        final PDFDoc pdfDoc = new PDFDoc(fileItem.path);
                        loadComparisonDoc(pdfDoc, null, fileItem.name, new PasswordDialog.IPasswordDialogListener() {
                            @Override
                            public void onConfirm(byte[] password) {
                                loadComparisonDoc(pdfDoc, password, fileItem.name, this);
                                if (mError == Constants.e_ErrSuccess) {
                                    int id = selectedFileView.getId();
                                    if (id == R.id.tv_old_file_name) {
                                        mOldDoc = pdfDoc;
                                    } else {
                                        mNewDoc = pdfDoc;
                                    }
                                    replaceFile(selectedFileView, fileItem, option);
                                    mFileSelectDialog.dismiss();
                                }
                            }

                            @Override
                            public void onDismiss() {

                            }

                            @Override
                            public void onKeyBack() {

                            }
                        });
                        if (mError != Constants.e_ErrSuccess) {
                            return;
                        }

                        int id = selectedFileView.getId();
                        if (id == R.id.tv_old_file_name) {
                            mOldDoc = pdfDoc;
                        } else {
                            mNewDoc = pdfDoc;
                        }
                        replaceFile(selectedFileView, fileItem, option);
                    } catch (PDFException e) {
                        return;
                    }
                }
                mFileSelectDialog.dismiss();
            }

            @Override
            public void onBackClick() {
            }
        });
        mFileSelectDialog.setHeight(mFileSelectDialog.getDialogHeight());
        mFileSelectDialog.showDialog(false);
    }

    private void replaceFile(final TextView selectedFileView, FileItem fileItem, final ComparisonPDF.ComparisonOption option) {
        String oldName = selectedFileView.getText().toString();
        for (int i = 0; i < mFileBrowser.getCheckedItems().size(); i ++){
            if (mFileBrowser.getCheckedItems().get(i).name.equalsIgnoreCase(oldName)) {
                mFileBrowser.getCheckedItems().get(i).checked = false;
                // update the checked item info
                fileItem.type = mFileBrowser.getCheckedItems().get(i).type;
                mFileBrowser.getCheckedItems().remove(i);
                mFileBrowser.getCheckedItems().add(i, fileItem);

                mFileBrowser.updateDataSource(false);
                break;
            }
        }
        selectedFileView.setText(fileItem.name);
        option.filePath = fileItem.path;
        mFileSelectDialog.clearCheckedItems();
    }

    public interface ICompareListener {
        int STATE_SUCCESS = 0;
        int STATE_ERROR = 1;
        int STATE_CANCEL = 2;

        /**
         * Called when compare files.
         *
         * @param state The comparison state.
         * @param filePath The file path of comparison.
         */
        void onCompareClicked(int state, String filePath);
    }

    private ICompareListener mCompareListener;
    public void setCompareListener(ICompareListener listener) {
        mCompareListener = listener;
    }

    private void onCompareClicked(int state, String filePath) {
        if (mCompareListener != null) {
            mCompareListener.onCompareClicked(state, filePath);
        }
    }

    private void initComparisonOption() {
        if (mOldFileOption == null) {
            mOldFileOption = new ComparisonPDF.ComparisonOption();
        }

        if (mNewFileOption == null) {
            mNewFileOption = new ComparisonPDF.ComparisonOption();
        }

        List<FileItem> list = mFileBrowser.getCheckedItems();
        if (list.get(0).lastModifyTime < list.get(1).lastModifyTime) {
            //0--> old file, 1--> new file
            mOldFileOption.filePath = list.get(0).path;
            mNewFileOption.filePath = list.get(1).path;
        } else {
            //1--> old file, 0--> new file
            mOldFileOption.filePath = list.get(1).path;
            mNewFileOption.filePath = list.get(0).path;
        }
    }

    private boolean mIsPasswordError = false;
    private int mError = Constants.e_ErrSuccess;
    private void checkDocAndShowComparisonDialog() {
        try {
            mOldDoc = new PDFDoc(mOldFileOption.filePath);
            int index = mOldFileOption.filePath.lastIndexOf(File.separatorChar);
            final String name = mOldFileOption.filePath.substring(index + 1);
            PasswordDialog.IPasswordDialogListener OldListener = new PasswordDialog.IPasswordDialogListener() {
                @Override
                public void onConfirm(byte[] password) {
                    loadComparisonDoc(mOldDoc, password, name, this);
                    if (mError == Constants.e_ErrSuccess) {
                        checkNewDocAndShowComparisonDialog();
                    }
                }

                @Override
                public void onDismiss() {

                }

                @Override
                public void onKeyBack() {

                }
            };
            loadComparisonDoc(mOldDoc, null, name, OldListener);
            if (mError != Constants.e_ErrSuccess) {
                return;
            }

            checkNewDocAndShowComparisonDialog();
        } catch (PDFException e) {
            // toast
            String message = AppUtil.getMessage(mContext, e.getLastError());
            UIToast.getInstance(mContext).show(message);
        }
    }

    private void checkNewDocAndShowComparisonDialog() {
        try {
            mNewDoc = new PDFDoc(mNewFileOption.filePath);
            int index = mNewFileOption.filePath.lastIndexOf(File.separatorChar);
            final String newName = mNewFileOption.filePath.substring(index + 1);
            PasswordDialog.IPasswordDialogListener newListener = new PasswordDialog.IPasswordDialogListener() {

                @Override
                public void onConfirm(byte[] password) {
                    loadComparisonDoc(mNewDoc, password, newName, this);
                    if (mError == Constants.e_ErrSuccess) {
                        showComparisonDialog();
                    }
                }

                @Override
                public void onDismiss() {

                }

                @Override
                public void onKeyBack() {

                }
            };
            loadComparisonDoc(mNewDoc, null, newName, newListener);
            if (mError != Constants.e_ErrSuccess) {
                return;
            }

            showComparisonDialog();
        } catch (PDFException e) {
            // toast
            String message = AppUtil.getMessage(mContext, e.getLastError());
            UIToast.getInstance(mContext).show(message);
        }
    }

    private void loadComparisonDoc(@NonNull final PDFDoc doc, byte[] password,
                                   final String title, PasswordDialog.IPasswordDialogListener listener) {
        try {
            mError = doc.load(password);
            if (mError == Constants.e_ErrSuccess) {
                if (doc.isXFA() || doc.isWrapper() || doc.isCDRM()) {
                    // unsupported
                    String message = AppResource.getString(mContext, R.string.unsupported_file_format) + ": " + title;
                    UIToast.getInstance(mContext).show(message);
                    mError = Constants.e_ErrUnsupported;
                }

                mIsPasswordError = false;
                return;
            }

            if (mError == Constants.e_ErrPassword) {
                String tips;
                if (mIsPasswordError) {
                    tips = AppResource.getString(mContext.getApplicationContext(), R.string.rv_tips_password_error);
                } else {
                    tips = AppResource.getString(mContext.getApplicationContext(), R.string.rv_tips_password);
                }

                mIsPasswordError = true;
                showPasswordDialog(title, tips, listener);
            } else if (mError == Constants.e_ErrSecurityHandler) {
                String message = AppResource.getString(mContext, R.string.unsupported_file_format) + ": " + title;
                UIToast.getInstance(mContext).show(message);
                mIsPasswordError = false;
            } else {
                String message = AppUtil.getMessage(mContext, mError) + ": " + title;
                UIToast.getInstance(mContext).show(message);
                mIsPasswordError = false;
            }
        } catch (PDFException e) {
            mError = e.getLastError();
        }
    }

    private void showPasswordDialog(String title, String tips, PasswordDialog.IPasswordDialogListener listener) {
        if (mActivity == null) {
            throw new RuntimeException("The attached activity is null.");
        }
        PasswordDialog passwordDialog = new PasswordDialog(mActivity, listener);
        passwordDialog.setTitle(title).setPromptTips(tips).show();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (mComparisonDialog != null && mComparisonDialog.isShowing()) {
            mComparisonDialog.setHeight(mComparisonDialog.getDialogHeight());
            mComparisonDialog.showDialog();
        }
        if (mFileSelectDialog != null && mFileSelectDialog.isShowing()) {
            mFileSelectDialog.setHeight(mFileSelectDialog.getDialogHeight());
            mFileSelectDialog.showDialog();
        }
    }

    private void afterComparisonClicked(int state, String filePath) {
        mOldDoc.delete();
        mNewDoc.delete();
        mOldDoc = null;
        mNewDoc = null;

        onCompareClicked(state, filePath);
        mComparisonDialog.dismiss();
    }
}

