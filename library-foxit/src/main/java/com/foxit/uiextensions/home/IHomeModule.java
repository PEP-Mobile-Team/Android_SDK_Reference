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
package com.foxit.uiextensions.home;

import android.content.Context;
import android.view.View;

import com.foxit.uiextensions.controls.toolbar.BaseBar;

public interface IHomeModule {
    String HOME_MODULE_TAG_LOCAL = "HOME_MODULE_LOCAL";

    String FILE_EXTRA = "filePath";
    interface onFileItemEventListener{
        void onFileItemClicked(String fileExtra, String filePath);
    }

    String getTag();

    View getTopToolbar(Context context);

    BaseBar getTopToolbar();

    View getContentView(Context context);

    boolean isNewVersion();

    void loadHomeModule(Context context);

    void unloadHomeModule(Context context);

    void onActivated();

    void onDeactivated();

    boolean onWillDestroy();

    void setFileItemEventListener(onFileItemEventListener listener);
}
