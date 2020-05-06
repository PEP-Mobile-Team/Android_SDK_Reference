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
package com.foxit.uiextensions.annots;

import android.graphics.RectF;

import com.foxit.sdk.common.DateTime;

/**
 * Interface that defines properties of annotation
 */
public interface AnnotContent {

    /** Return the page index of a annotation */
    public int getPageIndex();

    /** Return the type of a annotation */
    public int getType();

    /** Return the uniquely identifying of a annotation */
    public String getNM();

    /** Return the bbox of a annotation */
    public RectF getBBox();

    /** Return the color of a annotation */
    public int getColor();

    /** Return the opacity of a annotation */
    public int getOpacity();

    /** Return the line width of a annotation. usually for line, square, circle, polygon, polyline and so on*/
    public float getLineWidth();

    /** Return the subject of a annotation */
    public String getSubject();

    /** Return the modified date of a annotation */
    public DateTime getModifiedDate();

    /** Return the contents of a annotation */
    public String getContents();

    /** Return the intent of a annotation */
    public String getIntent();
}
