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
package com.foxit.uiextensions.annots.form;


import android.os.Parcel;
import android.os.Parcelable;

import com.foxit.uiextensions.modules.panel.bean.BaseBean;

public class ChoiceItemInfo extends BaseBean implements Parcelable {
    public boolean selected;
    public String optionValue;
    public String optionLabel;
    public boolean defaultSelected;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.selected ? (byte) 1 : (byte) 0);
        dest.writeString(this.optionValue);
        dest.writeString(this.optionLabel);
        dest.writeByte(this.defaultSelected ? (byte) 1 : (byte) 0);
    }

    public ChoiceItemInfo() {
    }

    protected ChoiceItemInfo(Parcel in) {
        this.selected = in.readByte() != 0;
        this.optionValue = in.readString();
        this.optionLabel = in.readString();
        this.defaultSelected = in.readByte() != 0;
    }

    public static final Parcelable.Creator<ChoiceItemInfo> CREATOR = new Parcelable.Creator<ChoiceItemInfo>() {
        @Override
        public ChoiceItemInfo createFromParcel(Parcel source) {
            return new ChoiceItemInfo(source);
        }

        @Override
        public ChoiceItemInfo[] newArray(int size) {
            return new ChoiceItemInfo[0];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChoiceItemInfo)) return false;
        ChoiceItemInfo itemInfo = (ChoiceItemInfo) o;
        if ((itemInfo.optionLabel == null ? optionLabel == null : itemInfo.optionLabel.equals(optionLabel))
                && (itemInfo.optionValue == null ? optionValue == null : itemInfo.optionValue.equals(optionValue))
                && itemInfo.defaultSelected == defaultSelected
                && itemInfo.selected == selected)
            return true;
        return false;
    }

    @Override
    public ChoiceItemInfo clone() {
        ChoiceItemInfo itemInfo = new ChoiceItemInfo();
        itemInfo.optionValue = optionValue;
        itemInfo.optionLabel = optionLabel;
        itemInfo.defaultSelected = defaultSelected;
        itemInfo.selected = selected;
        return itemInfo;
    }
}
