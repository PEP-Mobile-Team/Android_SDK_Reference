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

public class SignatureBean extends BaseBean implements Parcelable {
    private String signer;
    private String date;
    private String uuid;
    private boolean isSigned;
    private boolean isReadOnly;
    private int mSignedIndex;
    private int pageIndex;

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setSigned(boolean isSigned) {
        this.isSigned = isSigned;
    }

    public boolean isSigned() {
        return isSigned;
    }

    public int getSignedIndex() {
        return mSignedIndex;
    }

    public void setSignedIndex(int signedInex) {
        this.mSignedIndex = signedInex;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly(boolean readOnly) {
        isReadOnly = readOnly;
    }

    public SignatureBean() {
    }

    protected SignatureBean(Parcel in) {
        setFlag(in.readInt());
        date = in.readString();
        signer = in.readString();
        uuid = in.readString();
        boolean[] val = new boolean[1];
        in.readBooleanArray(val);
        isSigned = val[0];

        boolean[] readOnly = new boolean[1];
        in.readBooleanArray(readOnly);
        isReadOnly = readOnly[0];

        mSignedIndex = in.readInt();
        pageIndex = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getFlag());
        dest.writeString(date);
        dest.writeString(signer);
        dest.writeString(uuid);
        dest.writeBooleanArray(new boolean[] {isSigned});
        dest.writeBooleanArray(new boolean[] {isReadOnly});
        dest.writeInt(mSignedIndex);
        dest.writeInt(pageIndex);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SignatureBean> CREATOR = new Creator<SignatureBean>() {
        @Override
        public SignatureBean createFromParcel(Parcel in) {
            return new SignatureBean(in);
        }

        @Override
        public SignatureBean[] newArray(int size) {
            return new SignatureBean[0];
        }
    };
}
