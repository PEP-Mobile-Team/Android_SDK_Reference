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
package com.foxit.uiextensions.controls.filebrowser;

import android.view.View;

import com.foxit.uiextensions.controls.filebrowser.imp.FileItem;

import java.util.List;

public interface FileDelegate<T extends FileItem> {
    List<T> getDataSource();

    void onPathChanged(String path);

    void onItemClicked(View view, FileItem item);

    void onItemsCheckedChanged(boolean isAllSelected, int folderCount, int fileCount);
}
