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
package com.foxit.uiextensions.modules.panel.annot;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.foxit.uiextensions.R;

import java.util.ArrayList;
import java.util.List;


public class MenuItemAdapter extends BaseAdapter {

    private Context mContext;
    private List<ItemInfo> mItems = new ArrayList<>();

    public MenuItemAdapter(Context context, List<ItemInfo> items) {
        this.mContext = context;
        this.mItems = items;
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
            convertView = LayoutInflater.from(mContext).inflate(R.layout.panel_menu_item, null, false);
            holder.menuName = convertView.findViewById(R.id.tv_annot_panel_menu_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        ItemInfo itemInfo = mItems.get(position);
        holder.menuName.setText(itemInfo.itemName);
        holder.menuName.setEnabled(itemInfo.enabled);
        return convertView;
    }

    public List<ItemInfo> getItems() {
        return mItems;
    }

    private static class ViewHolder {
        private TextView menuName;
    }

    protected static class ItemInfo {
        public String itemName;
        public boolean enabled;

        public ItemInfo(String itemName, boolean enabled) {
            this.itemName = itemName;
            this.enabled = enabled;
        }
    }
}
