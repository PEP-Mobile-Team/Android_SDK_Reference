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
package com.foxit.uiextensions.controls.dialog.fileselect;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.filebrowser.FileDelegate;
import com.foxit.uiextensions.controls.filebrowser.imp.FileBrowserImpl;
import com.foxit.uiextensions.controls.filebrowser.imp.FileItem;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppStorageManager;
import com.foxit.uiextensions.utils.AppUtil;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UIFolderSelectDialog extends UIMatchDialog {
    private FileBrowserImpl mFileBrowser;
    private RelativeLayout mContentView;
    private List<FileItem> mFileItems = new ArrayList<FileItem>();
    private FileItem mCurrentItem;
    private RelativeLayout mPathLayout;
    private Context mContext;
    public UIFolderSelectDialog(Context context) {
        super(context, 0, true);
        mContext = context.getApplicationContext();
        onCreateView();

    }

    public View onCreateView()
    {
        mContentView = (RelativeLayout) View.inflate(mContext, R.layout.cloud_select_file, null);
        RelativeLayout fileBrowserView = (RelativeLayout) mContentView.findViewById(R.id.select_file_file_browser);
        mPathLayout = (RelativeLayout) mContentView.findViewById(R.id.select_file_path);

        TextView mTextView = new TextView(mContext);
        mTextView.setSingleLine();
        mTextView.setText(mContext.getString(R.string.hm_back));
        mTextView.setTextColor(mContext.getResources().getColorStateList(R.color.hm_back_color_selector));
        mTextView.setGravity(Gravity.CENTER | Gravity.LEFT);
        mTextView.setPadding(AppDisplay.getInstance(mContext).dp2px(6), 0, 0, 0);
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f);

        ImageView imageView = new ImageView(mContext);
        imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.pathctl_back));


        final LinearLayout mLinearLayout = new LinearLayout(mContext);
        mLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        if (AppDisplay.getInstance(mContext).isPad()) {
            mLinearLayout.setPadding(AppDisplay.getInstance(mContext).dp2px(26), 0, 0, 0);
        } else {
            mLinearLayout.setPadding(AppDisplay.getInstance(mContext).dp2px(13), 0, 0, 0);
        }
        LinearLayout.LayoutParams saParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mLinearLayout.addView(imageView, saParams);
        mLinearLayout.addView(mTextView, saParams);

        if (AppDisplay.getInstance(mContext).isPad())
            saParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)mContext.getResources().getDimension(R.dimen.ux_list_item_height_2l_pad));
        else
            saParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)mContext.getResources().getDimension(R.dimen.ux_list_item_height_2l_phone));
        saParams.gravity = Gravity.CENTER_VERTICAL;
        mPathLayout.addView(mLinearLayout);
        mPathLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = mFileBrowser.getDisplayPath();
                if (path == null || path.length() == 0) return;
                List<String> roots = AppStorageManager.getInstance(mContext).getVolumePaths();
                if (roots.contains(path)) {
                    mFileBrowser.setPath(null);
                    return;
                }
                int lastIndex = path.lastIndexOf(File.separator);
                if (lastIndex != -1) {
                    path = path.substring(0, lastIndex);
                    mFileBrowser.setPath(path);
                    return;
                }
                mFileBrowser.setPath(null);
            }
        });

        mFileBrowser = new FileBrowserImpl(mContext, new FileDelegate() {
            @Override
            public List<FileItem> getDataSource() {
                return mFileItems;
            }

            @Override
            public void onPathChanged(String path) {
                if (AppUtil.isEmpty(path)) {
                    setButtonEnable(false, MatchDialog.DIALOG_OK);
                    mPathLayout.setVisibility(View.GONE);
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
                        item.type = FileItem.TYPE_TARGET_FOLDER;
                        File[] fs = f.listFiles(mFileFilter);
                        if (fs != null) {
                            item.fileCount = fs.length;
                        } else {
                            item.fileCount = 0;
                        }
                        if (AppStorageManager.getInstance(mContext).checkStorageCanWrite(f.getPath()))
                            mFileItems.add(item);
                    }
                    return;
                }
                setButtonEnable(true, MatchDialog.DIALOG_OK);
                mPathLayout.setVisibility(View.VISIBLE);
                mFileItems.clear();
                File file = new File(path);
                if (!file.exists()) return;
                File[] files = file.listFiles(mFileFilter);
                if (files == null) return;
                for (File f : files) {
                    FileItem item = new FileItem();
                    item.parentPath = path;
                    item.path = f.getPath();
                    item.name = f.getName();
                    item.size = AppFileUtil.formatFileSize(f.length());
                    item.date = AppDmUtil.getLocalDateString(AppDmUtil.javaDateToDocumentDate(f.lastModified()));
                    if (f.isFile()) {
                        item.type = FileItem.TYPE_TARGET_FILE;
                    }
                    else {
                        item.type = FileItem.TYPE_TARGET_FOLDER;
                        File[] childFiles = f.listFiles(mFileFilter);
                        item.fileCount = childFiles == null ? 0 : childFiles.length;
                    }
                    item.length = f.length();
                    mFileItems.add(item);
                }
                Collections.sort(mFileItems, mFileBrowser.getComparator());
            }

            @Override
            public void onItemClicked(View view, FileItem item) {
                if ((item.type & FileItem.TYPE_FILE) > 0) return;
                mCurrentItem = item;
                mFileBrowser.setPath(item.path);
            }

            @Override
            public void onItemsCheckedChanged(boolean isAllSelected, int folderCount, int fileCount) {

            }


        });
        fileBrowserView.addView(mFileBrowser.getContentView());
        mCurrentItem = new FileItem();
        mCurrentItem.path = AppFileUtil.getSDPath();
        setContentView(mContentView);
        setTitleBlueLineVisible(true);
        setBackButtonVisible(View.GONE);
        return mContentView;
    }

    public String getCurrentPath() {
        return mCurrentItem.path;
    }

    private FileFilter mFileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            if (pathname.isHidden() || !pathname.canRead()) return false;
            return true;
        }
    };

    public void setFileFilter (FileFilter fileFilter) {
        if (fileFilter != null)
            mFileFilter = fileFilter;
        mFileBrowser.setPath(AppFileUtil.getSDPath());
    }

    public void notifyDataSetChanged(){
        mFileBrowser.notifyDataSetChanged();
    }

}
