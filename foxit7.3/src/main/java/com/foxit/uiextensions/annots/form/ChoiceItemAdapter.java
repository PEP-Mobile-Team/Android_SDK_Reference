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


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.foxit.uiextensions.R;

import java.util.ArrayList;

public class ChoiceItemAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<ChoiceItemInfo> mItems = new ArrayList<>();

    public ChoiceItemAdapter(Context context, ArrayList<ChoiceItemInfo> items) {
        this.mContext = context;
        this.mItems = items;
    }

    public void setChoiceInfos(ArrayList<ChoiceItemInfo> infos) {
        this.mItems = infos;
    }

    public ArrayList<ChoiceItemInfo> getChoiceInfos() {
        return mItems;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
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
            convertView = LayoutInflater.from(mContext).inflate(R.layout.pb_form_choice_item, null, false);
            holder.menuName = convertView.findViewById(R.id.tv_choice_item_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        ChoiceItemInfo itemInfo = mItems.get(position);
        holder.menuName.setText(itemInfo.optionValue);
        holder.menuName.setSelected(itemInfo.selected);
        return convertView;
    }

    private static class ViewHolder {
        private TextView menuName;
    }

}
