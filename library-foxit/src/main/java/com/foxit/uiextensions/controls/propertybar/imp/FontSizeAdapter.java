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
import com.foxit.uiextensions.utils.AppDisplay;


public class FontSizeAdapter extends BaseAdapter {
    private Context mContext;
    private float[] mFontSizes;
    private boolean[] mFontSizeChecked;

    public FontSizeAdapter(Context context, float[] fontSizes, boolean[] fontSizeChecked) {
        this.mContext = context;
        this.mFontSizes = fontSizes;
        this.mFontSizeChecked = fontSizeChecked;
    }

    @Override
    public int getCount() {
        return mFontSizes.length;
    }

    @Override
    public Object getItem(int position) {
        return mFontSizes[position];
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
            convertView = LayoutInflater.from(mContext).inflate(R.layout.pb_fontstyle_fontsizeitem, null, false);

            AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
            if (AppDisplay.getInstance(mContext).isPad()) {
                layoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_pad);
            } else {
                layoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_phone);
            }
            convertView.setLayoutParams(layoutParams);
            int padding = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            convertView.setPadding(padding, 0, padding, 0);

            holder.pb_tv_fontSizeItem = (TextView) convertView.findViewById(R.id.pb_tv_fontSizeItem);
            holder.pb_iv_fontSizeItem_selected = (ImageView) convertView.findViewById(R.id.pb_iv_fontSizeItem_selected);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.pb_tv_fontSizeItem.setText((int) mFontSizes[position] + "px");
        holder.pb_iv_fontSizeItem_selected.setImageResource(R.drawable.pb_selected);
        if (mFontSizeChecked[position]) {
            holder.pb_tv_fontSizeItem.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_button_colour));
            holder.pb_iv_fontSizeItem_selected.setVisibility(View.VISIBLE);
        } else {
            holder.pb_tv_fontSizeItem.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_body1_gray));
            holder.pb_iv_fontSizeItem_selected.setVisibility(View.GONE);
        }
        return convertView;
    }
    private static class ViewHolder {
        private TextView pb_tv_fontSizeItem;
        private ImageView pb_iv_fontSizeItem_selected;
    }
}
