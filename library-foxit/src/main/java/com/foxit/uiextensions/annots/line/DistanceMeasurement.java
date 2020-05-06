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
package com.foxit.uiextensions.annots.line;

/**
 *
 * This is the distance tool' unit enum;
 *
 * If you want to add unit conversion, please add the new item here ONLY.
 *
 * @see
 */
public enum DistanceMeasurement {

    PT(0,"pt",1.0f),

    INCH(1,"inch",0.013889f),

    FT(2,"ft",0.013889f*0.0833f),

    YD(3,"yd",0.013889f*0.0278f),

    P(4,"p",0.013889f*6f),

    MM(5,"mm",0.013889f*25.4f),

    CM(6,"cm",0.013889f*2.54f),

    M(7,"m",0.013889f*0.0254f);


    //the item index,a self increasing sequence
    private final int index;
    //the item name;
    private String name;
    //the value scale with default(pdf unit)
    private float scaleWithDefault;

    private DistanceMeasurement(int index, String name, float scale) {
        this.index = index;
        this.name = name;
        this.scaleWithDefault = scale;
    }

    public static DistanceMeasurement valueOf(Integer ret) {
        for (DistanceMeasurement message : values()) {
            if (ret.equals(message.getIndex())) {
                return message;
            }
        }
        throw new IllegalArgumentException("No matching constant for [ " + ret + "]");
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public float getScaleWithDefault(){
        return scaleWithDefault;
    }
}
