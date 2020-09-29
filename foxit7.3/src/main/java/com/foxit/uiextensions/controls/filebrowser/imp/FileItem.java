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
package com.foxit.uiextensions.controls.filebrowser.imp;

public class FileItem {
    public static final int TYPE_ROOT = 0x00000000;

    public static final int TYPE_FILE = 0x00000001;
    public static final int TYPE_FOLDER = 0x00000010;

    public static final int TYPE_ALL_PDF_FILE = 0x00000101;
    public static final int TYPE_ALL_PDF_FOLDER = 0x00000100;

    public static final int TYPE_TARGET_FILE = 0x00100001;
    public static final int TYPE_TARGET_FOLDER = 0x00100010;

    public static final int TYPE_CLOUD_SELECT_FILE      = 0x00010001;
    public static final int TYPE_CLOUD_SELECT_FOLDER    = 0x00010010;
    public int type;
    public String path;
    public String parentPath;
    public String name;

    public String date;
    public String size;

    public long createTime;
    public long lastModifyTime;
    public long length;
    public boolean checked;
    public String pattern;

    public int fileCount;

    public FileItem() {

    }

    public FileItem(FileItem item) {
        this.type = item.type;
        this.path = item.path;
        this.parentPath = item.parentPath;
        this.name = item.name;
        this.date = item.date;
        this.size = item.size;
        this.createTime = item.createTime;
        this.lastModifyTime = item.lastModifyTime;
        this.length = item.length;
        this.checked = item.checked;
        this.pattern = item.pattern;
        this.fileCount = item.fileCount;
    }
}
