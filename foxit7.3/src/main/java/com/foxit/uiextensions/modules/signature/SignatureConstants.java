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
package com.foxit.uiextensions.modules.signature;


public class SignatureConstants {
    public static final int SG_EVENT_DRAW = 10;
    public static final int SG_EVENT_COLOR = 11;
    public static final int SG_EVENT_THICKNESS = 12;
    public static final int SG_EVENT_CLEAR = 13;
    public static final int SG_EVENT_RELEASE = 14;
    public static final int SG_EVENT_SIGN = 15;

    public static final String getRecentTableName() {
            return "_sg_rec";
    }

    public static final String getModelTableName() {

            return "_sg_mod";

    }

    public static final String SG_DSG_PATH_FIELD = "_sg_dsgPath";
}
