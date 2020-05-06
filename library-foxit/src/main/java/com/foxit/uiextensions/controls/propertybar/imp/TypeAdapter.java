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
package com.foxit.uiextensions.controls.propertybar.imp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.annots.note.NoteConstants;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDisplay;

public class TypeAdapter extends BaseAdapter {
    private Context mContext;
    private int[] mTypePicIds;
    private String[] mTypeNames;
    private int mNoteIconType = NoteConstants.TA_ICON_COMMENT;

    private AppDisplay display;

    public TypeAdapter(Context context, int[] typePicIds, String[] typeNames) {
        this.mContext = context;
        this.mTypePicIds = typePicIds;
        this.mTypeNames = typeNames;

        display = AppDisplay.getInstance(context);
    }

    public void setNoteIconType(int noteIconType) {
        this.mNoteIconType = noteIconType;
    }

    @Override
    public int getCount() {
        return mTypePicIds.length;
    }

    @Override
    public Object getItem(int position) {
        return mTypePicIds[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.pb_type, null, false);

            if (display.isPad()) {
                int height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_pad);
                convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, height));
            } else {
                int height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_phone);
                convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, height));
            }

            holder.pb_iv_typePic = (ImageView) convertView.findViewById(R.id.pb_iv_typePic);
            holder.pb_tv_typeName = (TextView) convertView.findViewById(R.id.pb_tv_typeName);
            holder.pb_tv_type_tag = (ImageView) convertView.findViewById(R.id.pb_tv_type_tag);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.pb_iv_typePic.setImageResource(mTypePicIds[position]);
        holder.pb_tv_typeName.setText(mTypeNames[position]);
        if (mNoteIconType == PropertyBar.ICONTYPES[position]) {
            holder.pb_tv_type_tag.setVisibility(View.VISIBLE);
        } else {
            holder.pb_tv_type_tag.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    private static class ViewHolder {
        private ImageView pb_iv_typePic;
        private TextView pb_tv_typeName;
        private ImageView pb_tv_type_tag;
    }
}
