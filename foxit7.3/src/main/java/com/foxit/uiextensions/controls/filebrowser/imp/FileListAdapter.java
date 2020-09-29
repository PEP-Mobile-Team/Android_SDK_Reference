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
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;


class FileListAdapter extends FileAdapter {
    private Context mContext;
    private boolean mShowCheckBox = true;

    protected FileListAdapter(IFB_FileAdapterDelegate delegate) {
        super(delegate);
        mContext = delegate.getContext();
    }

    public void showCheckBox(boolean show){
        mShowCheckBox = show;
    }

    @Override
    public int getCount() {
        if (mDelegate.getDataSource() == null) return 0;
        return mDelegate.getDataSource().size();
    }

    @Override
    public FileItem getItem(int position) {
        try {
            return mDelegate.getDataSource().get(position);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final FileItem item = mDelegate.getDataSource().get(position);
        if (item == null) {
            throw new NullPointerException("item == null");
        }
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            if (AppDisplay.getInstance(mContext).isPad())
                convertView = View.inflate(mContext, R.layout.fb_file_item_pad, null);
            else
                convertView = View.inflate(mContext, R.layout.fb_file_item_phone, null);

            holder.searchFolderLayout = convertView.findViewById(R.id.fb_item_search_layout);
            holder.searchFolderPathTextView = (TextView) convertView.findViewById(R.id.fb_item_search_path);

            holder.commonLayout = convertView.findViewById(R.id.fb_item_common_layout);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.fb_item_checkbox);
            holder.iconImageView = (ImageView) convertView.findViewById(R.id.fb_item_icon);
            holder.nameTextView = (TextView) convertView.findViewById(R.id.fb_item_name);
            holder.dateTextView = (TextView) convertView.findViewById(R.id.fb_item_date);
            holder.sizeTextView = (TextView) convertView.findViewById(R.id.fb_item_size);

            holder.fileCount = (TextView) convertView.findViewById(R.id.fb_item_filecount);

            holder.checkBox.setTag(new ClickListener(holder.checkBox, position) {
                @Override
                public void onClick(View v) {
                    FileItem item = getItem(this.getPosition());
                    if (((CompoundButton) v).isChecked()) {
                        if (mDelegate.onItemChecked(true, this.getPosition(), item))
                            ((CompoundButton) v).setChecked(true);
                        else
                            ((CompoundButton) v).setChecked(item.checked);
                    } else {
                        if (mDelegate.onItemChecked(false, this.getPosition(), item))
                            ((CompoundButton) v).setChecked(false);
                        else
                            ((CompoundButton) v).setChecked(item.checked);
                    }
                }
            });
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            ((ClickListener) holder.checkBox.getTag()).update(position);
        }
        if (mDelegate.isEditState()) {

            if (item.type != FileItem.TYPE_ALL_PDF_FOLDER && item.type != FileItem.TYPE_FOLDER
                    && item.type != FileItem.TYPE_ROOT) {
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(item.checked);
            } else if (item.type == FileItem.TYPE_FOLDER) {
                holder.checkBox.setVisibility(View.INVISIBLE);
            } else {
                holder.checkBox.setVisibility(View.GONE);
            }
        } else {

            holder.checkBox.setVisibility(View.GONE);
        }
        holder.iconImageView.setBackgroundDrawable(null);
        switch (item.type) {
            default:
            case FileItem.TYPE_FILE:
                setVisibility(holder.searchFolderLayout, false);
                setVisibility(holder.commonLayout, true);
                setVisibility(holder.sizeTextView, true);
                setVisibility(holder.fileCount, false);
                setIcon(holder.iconImageView, item.path);
                break;
            case FileItem.TYPE_FOLDER:
                setVisibility(holder.searchFolderLayout, false);
                setVisibility(holder.commonLayout, true);
                setVisibility(holder.sizeTextView, false);
                setVisibility(holder.fileCount, true);
                holder.fileCount.setText(item.fileCount + "");
                holder.iconImageView.setImageResource(R.drawable.fb_file_dir);
                break;
            case FileItem.TYPE_ROOT:
                setVisibility(holder.searchFolderLayout, false);
                setVisibility(holder.commonLayout, true);
                setVisibility(holder.sizeTextView, false);
                setVisibility(holder.fileCount, true);
                holder.fileCount.setText(item.fileCount + "");
                holder.iconImageView.setImageResource(R.drawable.fb_file_dir);
                break;
            case FileItem.TYPE_TARGET_FOLDER:
                setVisibility(holder.searchFolderLayout, false);
                setVisibility(holder.commonLayout, true);
                setVisibility(holder.sizeTextView, false);
                setVisibility(holder.fileCount, true);
                holder.fileCount.setText(item.fileCount + "");
                holder.iconImageView.setImageResource(R.drawable.fb_file_dir);
                break;
            case FileItem.TYPE_TARGET_FILE:
                setVisibility(holder.searchFolderLayout, false);
                setVisibility(holder.commonLayout, true);
                setVisibility(holder.sizeTextView, true);
                setVisibility(holder.fileCount, false);
                setIcon(holder.iconImageView, item.path);
                break;
            case FileItem.TYPE_ALL_PDF_FILE:
                setVisibility(holder.searchFolderLayout, false);
                setVisibility(holder.commonLayout, true);
                setVisibility(holder.sizeTextView, true);
                setIcon(holder.iconImageView, item.path);
                setVisibility(holder.fileCount, false);
                break;
            case FileItem.TYPE_ALL_PDF_FOLDER:
                setVisibility(holder.searchFolderLayout, true);
                setVisibility(holder.commonLayout, false);
                holder.searchFolderPathTextView.setText(item.path == null ? "" : item.path);
                setVisibility(holder.fileCount, false);
                break;
            case FileItem.TYPE_CLOUD_SELECT_FILE:
                setVisibility(holder.searchFolderLayout, false);
                setVisibility(holder.commonLayout, true);
                setVisibility(holder.sizeTextView, true);
                setVisibility(holder.fileCount, false);
                holder.checkBox.setVisibility(mShowCheckBox?View.VISIBLE:View.GONE);
                setIcon(holder.iconImageView, item.path);
                break;
            case FileItem.TYPE_CLOUD_SELECT_FOLDER:
                setVisibility(holder.searchFolderLayout, false);
                setVisibility(holder.commonLayout, true);
                setVisibility(holder.sizeTextView, false);
                setVisibility(holder.fileCount, true);
                holder.fileCount.setText(item.fileCount + "");
                holder.checkBox.setVisibility(mShowCheckBox?View.INVISIBLE:View.GONE);
                holder.iconImageView.setImageResource(R.drawable.fb_file_dir);
                break;
        }
        String name = item.name == null ? "" : item.name;
        holder.nameTextView.setText(name);
        holder.sizeTextView.setText(item.size == null ? "" : item.size);
        holder.dateTextView.setText(item.date == null ? "" : item.date);
        return convertView;
    }

    public static boolean isSupportThumbnail(String name) {
        String extension = name.substring(name.lastIndexOf(".") + 1, name.length()).toLowerCase();
        if (extension == null || extension.length() == 0) {
            return false;
        } else if (extension.equals("ofd")) {
            return true;
        } else {
            return false;
        }
    }



    private void setIcon(ImageView iconImageView, String path) {
        if (isSupportThumbnail(path)) {
            Bitmap bitmap = FileThumbnail.getInstance(mContext).getThumbnail(path, mThumbnailCallback);
            if (bitmap != null) {
                iconImageView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.fb_file_pdf_bg));
                iconImageView.setImageBitmap(bitmap);
            }
            return;
        }

        int drawableId = getDrawableByFileName(path);
        if (drawableId == -1) {
            Bitmap bitmap = FileThumbnail.getInstance(mContext).getThumbnail(path, mThumbnailCallback);
            if (bitmap == null) {
                iconImageView.setImageResource(R.drawable.fb_file_pdf);
            } else {
                iconImageView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.fb_file_pdf_bg));
                iconImageView.setImageBitmap(bitmap);
            }
        } else {
            iconImageView.setImageResource(drawableId);
        }
    }

    private FileThumbnail.ThumbnailCallback mThumbnailCallback = new FileThumbnail.ThumbnailCallback() {
        @Override
        public void result(boolean succeed, final String filePath) {
            if (succeed) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mDelegate.updateItem(filePath);
                    }
                });
            }
        }
    };

    private void setVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void updateThumbnail(String filePath, FileThumbnail.ThumbnailCallback callback) {
        FileThumbnail.getInstance(mContext).updateThumbnail(filePath, callback);
    }
}
