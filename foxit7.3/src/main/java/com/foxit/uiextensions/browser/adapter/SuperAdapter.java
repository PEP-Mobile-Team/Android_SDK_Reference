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
package com.foxit.uiextensions.browser.adapter;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.foxit.uiextensions.browser.adapter.viewholder.SuperViewHolder;
import com.foxit.uiextensions.modules.panel.bean.BaseBean;


public abstract class SuperAdapter extends RecyclerView.Adapter<SuperViewHolder> {
    private Context context;

    public SuperAdapter(Context context){
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public void onBindViewHolder(SuperViewHolder holder, int position) {
        SuperViewHolder viewHolder = (SuperViewHolder) holder;
        viewHolder.bind(getDataItem(position), position);
    }

    public abstract void notifyUpdateData();
    public abstract BaseBean getDataItem(int position);
}
