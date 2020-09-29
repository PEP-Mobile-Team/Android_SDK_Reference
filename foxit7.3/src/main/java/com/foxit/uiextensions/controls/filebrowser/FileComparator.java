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

import com.foxit.uiextensions.controls.filebrowser.imp.FileItem;

import java.util.Comparator;

public class FileComparator implements Comparator<FileItem> {
    public static final int ORDER_NAME_UP           = 0;
    public static final int ORDER_NAME_DOWN         = 1;
    public static final int ORDER_TIME_UP           = 2;
    public static final int ORDER_TIME_DOWN         = 3;
    public static final int ORDER_SIZE_UP           = 4;
    public static final int ORDER_SIZE_DOWN         = 5;

    private int mNaturalOrder = ORDER_NAME_UP;

    public void setOrderBy(int orderBy) {
        mNaturalOrder = orderBy;
    }

    @Override
    public int compare(FileItem lhs, FileItem rhs) {
        switch (mNaturalOrder) {
            default:
            case ORDER_NAME_UP:
                if (lhs.type == rhs.type) {
                    return lhs.name.compareToIgnoreCase(rhs.name);
                } else {
                    return rhs.type - lhs.type;
                }
            case ORDER_NAME_DOWN:
                if (lhs.type == rhs.type) {
                    return lhs.name.compareToIgnoreCase(rhs.name) * (-1);
                } else {
                    return rhs.type - lhs.type;
                }
            case ORDER_TIME_UP:
                if (lhs.type == rhs.type) {
                    if (lhs.lastModifyTime - rhs.lastModifyTime > 0) {
                        return 1;
                    }
                    else if (lhs.lastModifyTime - rhs.lastModifyTime < 0) {
                        return -1;
                    }
                    else {
                        return 0;
                    }
                } else {
                    return rhs.type - lhs.type;
                }
            case ORDER_TIME_DOWN:
                if (lhs.type == rhs.type) {
                    if (rhs.lastModifyTime - lhs.lastModifyTime > 0) {
                        return 1;
                    } else if (rhs.lastModifyTime - lhs.lastModifyTime < 0) {
                        return -1;
                    } else {
                        return 0;
                    }
                } else {
                    return rhs.type - lhs.type;
                }
            case ORDER_SIZE_UP:
                if (lhs.type == rhs.type) {
                    return (int) (lhs.length - rhs.length);
                } else {
                    return rhs.type - lhs.type;
                }
            case ORDER_SIZE_DOWN:
                if (lhs.type == rhs.type) {
                    return (int) (rhs.length - lhs.length);
                } else {
                    return rhs.type - lhs.type;
                }
        }
    }
}
