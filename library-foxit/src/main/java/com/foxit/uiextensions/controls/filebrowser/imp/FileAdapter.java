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
package com.foxit.uiextensions.controls.filebrowser.imp;


import android.content.Context;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;


import com.foxit.uiextensions.R;

import java.util.List;

abstract class FileAdapter extends BaseAdapter {
    protected interface IFB_FileAdapterDelegate {
        boolean onItemChecked(boolean checked, int position, FileItem item);

        boolean isEditState();

        Context getContext();

        List<FileItem> getDataSource();

        void updateItem(String path);
    }

    protected IFB_FileAdapterDelegate mDelegate;

    public FileAdapter(IFB_FileAdapterDelegate delegate) {
        this.mDelegate = delegate;
    }


    protected static final class ViewHolder {
        public View searchFolderLayout;
        public TextView searchFolderPathTextView;
        public View commonLayout;
        public TextView sizeTextView;
        public ImageView iconImageView;
        public TextView nameTextView;
        public TextView dateTextView;
        public CheckBox checkBox;
        public TextView fileCount;
    }
    protected static abstract class ClickListener implements View.OnClickListener {
        private int position;

        ClickListener(View view, int position) {
            view.setOnClickListener(this);
            this.position = position;
        }

        void update(int position) {
            this.position = position;
        }

        int getPosition() {
            return position;
        }
    }
    public static int getDrawableByFileName(String name) {
        if (name == null || name.length() == 0) {
            return R.drawable.fb_file_other;
        }
        String extension = name.substring(name.lastIndexOf(".") + 1, name.length()).toLowerCase();
        if (extension == null || extension.length() == 0) {
            return R.drawable.fb_file_other;
        } else if (extension.equals("doc") || extension.equals("docx")) {
            return R.drawable.fb_file_doc;
        } else if (extension.equals("xls") || extension.equals("xlsx")) {
            return R.drawable.fb_file_xls;
        } else if (extension.equals("jpg")) {
            return R.drawable.fb_file_jpg;
        } else if (extension.equals("png")) {
            return R.drawable.fb_file_png;
        } else if (extension.equals("txt")) {
            return R.drawable.fb_file_txt;
        } else if (extension.equals("xml")) {
            return R.drawable.fb_file_xml;
        } else if (extension.equals("pdf")) {
            return -1;
        } else if (extension.equals("ppdf")) {
            return R.drawable.fb_file_ppdf;
        }
        return R.drawable.fb_file_other;
    }
}