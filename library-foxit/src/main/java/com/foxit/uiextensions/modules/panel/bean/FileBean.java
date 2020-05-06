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
package com.foxit.uiextensions.modules.panel.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The file attachment panel list's data model.
 *
 * @see BaseBean
 */
public class FileBean extends BaseBean implements Parcelable {
    private String title;
    private String name;
    private String date;
    private String size;
    private String desc;
    private String filePath;
    private String uuid;
    private boolean canDelete;


    public FileBean(){}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public boolean canDelete() {
        return canDelete;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(date);
        dest.writeString(size);
        dest.writeString(desc);
        dest.writeInt(getFlag());
        dest.writeString(filePath);
        dest.writeString(uuid);
        dest.writeString(name);
        dest.writeBooleanArray(new boolean[] {canDelete});
    }

    public static final Parcelable.Creator<FileBean> CREATOR = new Parcelable.Creator<FileBean>(){

        @Override
        public FileBean createFromParcel(Parcel source) {
            FileBean item =  new FileBean();
            item.setTitle(source.readString());
            item.setDate(source.readString());
            item.setSize(source.readString());
            item.setDesc(source.readString());
            item.setFlag(source.readInt());
            item.setFilePath(source.readString());
            item.setUuid(source.readString());
            item.setName(source.readString());
            boolean[] val = new boolean[1];
            source.readBooleanArray(val);
            item.setCanDelete(val[0]);
            return item;
        }

        @Override
        public FileBean[] newArray(int size) {
            return new FileBean[0];
        }
    };
}
