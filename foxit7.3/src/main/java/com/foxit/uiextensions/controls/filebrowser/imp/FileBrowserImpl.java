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
package com.foxit.uiextensions.controls.filebrowser.imp;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import androidx.core.view.MotionEventCompat;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.filebrowser.FileBrowser;
import com.foxit.uiextensions.controls.filebrowser.FileComparator;
import com.foxit.uiextensions.controls.filebrowser.FileDelegate;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FileBrowserImpl implements FileBrowser {
    private Context mContext;
    private FileDelegate mDelegate;
    private FileComparator mComparator;

    private final List<FileItem> mCheckedItems = new ArrayList<FileItem>();
    private final Stack<Integer> mSelectionStack = new Stack<Integer>();

    private ListView mListView;
    private boolean isTouchHold;
    private boolean isEditing;
    private String mCurrentPath;

    private FileAdapter mFileAdapter;
    private Handler mHandle;

    public void notifyDataSetChanged() {
        mHandle.sendEmptyMessage(1);
    }

    public FileBrowserImpl(Context context, FileDelegate delegate) {
        mContext = context;
        mDelegate = delegate;
        initView();

        mHandle = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case 1:
                        mFileAdapter.notifyDataSetChanged();
                        break;
                        default:
                            break;
                }
            }
        };
    }

    public void showCheckBox(boolean show){
        if(mFileAdapter != null){
            ((FileListAdapter)mFileAdapter).showCheckBox(show);
        }
    }

    private void initView() {
        mListView = new ListView(mContext);
        mListView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mListView.setCacheColorHint(mContext.getResources().getColor(R.color.ux_color_translucent));
        mListView.setDivider(new ColorDrawable(mContext.getResources().getColor(R.color.ux_color_seperator_gray)));
        mListView.setDividerHeight(AppDisplay.getInstance(mContext).dp2px(1));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileAdapter adapter = (FileAdapter) parent.getAdapter();
                if (adapter == null) return;
                if (AppUtil.isFastDoubleClick()) return;
                FileItem item = (FileItem) adapter.getItem(position);
                if (item == null) return;
                if (isEditing && item.type != FileItem.TYPE_FOLDER && item.type != FileItem.TYPE_ROOT) {
                    if (item.type != FileItem.TYPE_CLOUD_SELECT_FOLDER) {
                        if (item.type == FileItem.TYPE_ALL_PDF_FOLDER) return;
                        mFileAdapterDelegate.onItemChecked(!item.checked, position, item);
                        return;
                    }
                }
                if ((item.type & FileItem.TYPE_FOLDER) != 0) {
                    int firstPosition = mListView.getFirstVisiblePosition();
                    mSelectionStack.push(firstPosition);
                }
                mDelegate.onItemClicked(view, item);
            }
        });
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
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
        if (mFileAdapter != null) {
            mListView.setAdapter(mFileAdapter);
            return;
        }
        mFileAdapter = new FileListAdapter(mFileAdapterDelegate);
        mListView.setAdapter(mFileAdapter);
    }
    private boolean mOnlyOneSelect = false;
    private int mFileCounter, mFolderCounter;
    private FileAdapter.IFB_FileAdapterDelegate mFileAdapterDelegate = new FileAdapter.IFB_FileAdapterDelegate() {
        @Override
        public boolean onItemChecked(boolean checked, int position, FileItem item) {
            FileItem tempItem = item;
            for (int index = 0; index < mCheckedItems.size(); index ++) {
                if (mCheckedItems.get(index).path.equals(item.path)) {
                    tempItem = mCheckedItems.get(index);
                    break;
                }
            }
            if (checked) {
                if (mOnlyOneSelect&&mCheckedItems.size() >= 1) {
                    if (!mCheckedItems.contains(tempItem)) {
                        mCheckedItems.get(0).checked = false;
                        mCheckedItems.clear();
                        mCheckedItems.add(item);
                    }
                }
                if (!mCheckedItems.contains(tempItem)) {
                    mCheckedItems.add(item);
                    if ((item.type & FileItem.TYPE_FILE) != 0) {
                        mFileCounter++;
                    } else {
                        mFolderCounter++;
                    }
                }

            } else {
                if (mCheckedItems.remove(tempItem)) {
                    if ((item.type & FileItem.TYPE_FILE) != 0) {
                        mFileCounter--;
                    } else {
                        mFolderCounter--;
                    }
                }
            }
            item.checked = checked;
            updateDataSource(true);
            mDelegate.onItemsCheckedChanged(false, mFolderCounter, mFileCounter);
            return true;
        }
        @Override
        public boolean isEditState() {
            return isEditing;
        }
        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        public List<FileItem> getDataSource() {
            return mDelegate.getDataSource();
        }

        @Override
        public void updateItem(String path) {
            if (path == null || path.length() == 0) return;
            int start = mListView.getFirstVisiblePosition();
            for (int i = start, j = mListView.getLastVisiblePosition(); i <= j; i++) {
                FileItem info = FileItem.class.cast(mListView.getItemAtPosition(i));
                if (info != null && path.equals(info.path)) {
                    View view = mListView.getChildAt(i - start);
                    mFileAdapter.getView(i, view, mListView);
                    break;
                }
            }
        }
    };

    @Override
    public View getContentView() {
        return mListView;
    }


    @Override
    public FileComparator getComparator() {
        if (mComparator == null) {
            mComparator = new FileComparator();
        }
        return mComparator;
    }

    @Override
    public void updateThumbnail(String filePath, FileThumbnail.ThumbnailCallback callback) {
        if (mFileAdapter != null)
            ((FileListAdapter) mFileAdapter).updateThumbnail(filePath, callback);
    }

    @Override
    public void setEditState(boolean editState) {
        isEditing = editState;
        if (!editState) {
            mCheckedItems.clear();
            List<FileItem> items = mDelegate.getDataSource();
            for (FileItem item : items) {
                item.checked = false;
            }
        }
        mFileCounter = 0;
        mFolderCounter = 0;
        mDelegate.onItemsCheckedChanged(false, 0, 0);
        updateDataSource(true);
    }

    @Override
    public void setPath(String currentPath) {
        boolean isFolderBack;
        if (AppUtil.isEmpty(currentPath) || AppUtil.isEmpty(mCurrentPath)) {
            isFolderBack = false;
        } else {
            isFolderBack = !mCurrentPath.equals(currentPath) && mCurrentPath.startsWith(currentPath);
        }
        mCurrentPath = currentPath;
        mDelegate.onPathChanged(currentPath);
        updateDataSource(true);
        if (!isEditing) {
            if (isFolderBack && !mSelectionStack.empty()) {
                mListView.setSelection(mSelectionStack.pop());
            } else {
                mListView.setSelection(0);
            }

            clearCheckedItems();
        }
    }

    @Override
    public String getDisplayPath() {
        return mCurrentPath == null ? "" : mCurrentPath;
    }

    @Override
    public void updateDataSource(boolean isOnlyNotify) {
        if (!isOnlyNotify) mDelegate.onPathChanged(mCurrentPath);
        ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
    }

    @Override
    public List<FileItem> getCheckedItems() {
        return mCheckedItems;
    }

    @Override
    public void clearCheckedItems() {
        for (FileItem item : mCheckedItems) {
            item.checked = false;
        }
        mCheckedItems.clear();
        mFileCounter = 0;
        mFolderCounter = 0;
        mDelegate.onItemsCheckedChanged(false, mFolderCounter, mFileCounter);
    }

    public void setOnlyOneSelect(boolean isOne) {
        mOnlyOneSelect = isOne;
    }
}
