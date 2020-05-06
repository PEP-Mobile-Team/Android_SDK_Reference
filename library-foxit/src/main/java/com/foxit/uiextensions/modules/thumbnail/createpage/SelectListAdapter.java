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
package com.foxit.uiextensions.modules.thumbnail.createpage;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.foxit.uiextensions.R;

import java.util.List;

public class SelectListAdapter extends BaseAdapter {

    private Context mContext;
    private List<ItemBean> mItemBeans;

    public SelectListAdapter(Context context, List<ItemBean> list) {
        mContext = context;
        mItemBeans = list;
    }

    public List<ItemBean> getList(){
        return mItemBeans;
    }

    @Override
    public int getCount() {
        return mItemBeans.size();
    }

    @Override
    public Object getItem(int position) {
        return mItemBeans.get(position);
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
            convertView = LayoutInflater.from(mContext).inflate(R.layout.rd_list_check_layout, null, false);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvName = convertView.findViewById(R.id.tv_type_name);
        holder.ivChecked = convertView.findViewById(R.id.iv_type_checked);

        ItemBean itemBean = mItemBeans.get(position);
        holder.tvName.setText(itemBean.itemName);
        holder.ivChecked.setVisibility(itemBean.isChecked ? View.VISIBLE : View.GONE);
        return convertView;
    }

    private static class ViewHolder {
        private ImageView ivChecked;
        private TextView tvName;
    }

    public static class ItemBean {
        public String itemName;
        public int itemType;
        public boolean isChecked;

        public ItemBean(String itemName, int itemType, boolean isChecked) {
            this.itemName = itemName;
            this.itemType = itemType;
            this.isChecked = isChecked;
        }
    }

}
