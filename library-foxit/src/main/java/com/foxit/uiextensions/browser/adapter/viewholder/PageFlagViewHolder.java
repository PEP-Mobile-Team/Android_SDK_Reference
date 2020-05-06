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
package com.foxit.uiextensions.browser.adapter.viewholder;

import android.view.View;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.modules.panel.bean.BaseBean;

/**
 * page flag
 *
 * @see SuperViewHolder
 */
public class PageFlagViewHolder extends SuperViewHolder{
    private TextView flag;

    public PageFlagViewHolder(View itemView) {
        super(itemView);
        flag = (TextView)itemView.findViewById(R.id.panel_item_fileattachment_page_flag);
    }

    @Override
    public void bind(BaseBean data) {
        flag.setText(data.getTag());
    }

    @Override
    public void onClick(View v) {

    }
}