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
import com.foxit.uiextensions.controls.filebrowser.imp.FileThumbnail;

import java.util.List;

public interface FileBrowser {
    View getContentView();

    void setEditState(boolean editState);

    void setPath(String currentPath);

    String getDisplayPath();

    void updateDataSource(boolean isOnlyNotify);

    List<FileItem> getCheckedItems();

    void clearCheckedItems();

    FileComparator getComparator();

    void updateThumbnail(String filePath, FileThumbnail.ThumbnailCallback callback);
}
