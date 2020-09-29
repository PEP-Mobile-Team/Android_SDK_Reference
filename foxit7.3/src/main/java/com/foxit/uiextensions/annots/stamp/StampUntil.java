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
package com.foxit.uiextensions.annots.stamp;

import com.foxit.uiextensions.R;

public class StampUntil {

    public static int getStampTypeByName(String name) {
        if (name == null) return -1;
        if (name.equals("Approved")) return 0;
        if (name.equals("Completed")) return 1;
        if (name.equals("Confidential")) return 2;
        if (name.equals("Draft")) return 3;
        if (name.equals("Emergency")) return 4;
        if (name.equals("Expired")) return 5;
        if (name.equals("Final")) return 6;
        if (name.equals("Received")) return 7;
        if (name.equals("Reviewed")) return 8;
        if (name.equals("Revised")) return 9;
        if (name.equals("Verified")) return 10;
        if (name.equals("Void")) return 11;
        if (name.equals("Accepted")) return 12;
        if (name.equals("Initial")) return 13;
        if (name.equals("Rejected")) return 14;
        if (name.equals("Sign Here")) return 15;
        if (name.equals("Witness")) return 16;
        if (name.equals("DynaApproved")) return 17;
        if (name.equals("DynaConfidential")) return 18;
        if (name.equals("DynaReceived")) return 19;
        if (name.equals("DynaReviewed")) return 20;
        if (name.equals("DynaRevised")) return 21;
        return -1;
    }

    public static String getStampNameByType(int type) {
        if (type == 0) return "Approved";
        if (type == 1) return "Completed";
        if (type == 2) return "Confidential";
        if (type == 3) return "Draft";
        if (type == 4) return "Emergency";
        if (type == 5) return "Expired";
        if (type == 6) return "Final";
        if (type == 7) return "Received";
        if (type == 8) return "Reviewed";
        if (type == 9) return "Revised";
        if (type == 10) return "Verified";
        if (type == 11) return "Void";
        if (type == 12) return "Accepted";
        if (type == 13) return "Initial";
        if (type == 14) return "Rejected";
        if (type == 15) return "Sign Here";
        if (type == 16) return "Witness";
        if (type == 17) return "DynaApproved";
        if (type == 18) return "DynaConfidential";
        if (type == 19) return "DynaReceived";
        if (type == 20) return "DynaReviewed";
        if (type == 21) return "DynaRevised";
        return null;
    }

    private static Integer[] mStampIds = {
            R.drawable._feature_annot_stamp_style0,
            R.drawable._feature_annot_stamp_style1,
            R.drawable._feature_annot_stamp_style2,
            R.drawable._feature_annot_stamp_style3,
            R.drawable._feature_annot_stamp_style4,
            R.drawable._feature_annot_stamp_style5,
            R.drawable._feature_annot_stamp_style6,
            R.drawable._feature_annot_stamp_style7,
            R.drawable._feature_annot_stamp_style8,
            R.drawable._feature_annot_stamp_style9,
            R.drawable._feature_annot_stamp_style10,
            R.drawable._feature_annot_stamp_style11,
            R.drawable._feature_annot_stamp_style12,
            R.drawable._feature_annot_stamp_style13,
            R.drawable._feature_annot_stamp_style14,
            R.drawable._feature_annot_stamp_style15,
            R.drawable._feature_annot_stamp_style16,
            R.drawable._feature_annot_stamp_style17,
            R.drawable._feature_annot_stamp_style18,
            R.drawable._feature_annot_stamp_style19,
            R.drawable._feature_annot_stamp_style20,
            R.drawable._feature_annot_stamp_style21,
    };

    public static int getStampIconByType(int type) {
       return mStampIds[type];
    }

}
