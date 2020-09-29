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
package com.foxit.uiextensions.controls.propertybar;

import android.view.View;

public interface MoreTools {
    interface IMT_MoreClickListener {
        void onMTClick(int type);

        int getType();
    }

    //
    int MT_TYPE_NOTE = 1;
    int MT_TYPE_ATTACHMENT = 2;
    int MT_TYPE_STAMP = 3;
    int MT_TYPE_IMAGE = 4;
    int MT_TYPE_AUDIO = 5;
    int MT_TYPE_VIDEO = 6;
    //
    int MT_TYPE_TYPEWRITER = 20;
    int MT_TYPE_CALLOUT = 21;
    int MT_TYPE_TEXTBOX = 22;
    //
    int MT_TYPE_HIGHLIGHT = 30;
    int MT_TYPE_UNDERLINE = 31;
    int MT_TYPE_SQUIGGLY = 32;
    int MT_TYPE_STRIKEOUT = 33;
    int MT_TYPE_REPLACE = 34;
    int MT_TYPE_INSERT = 35;
    //
    int MT_TYPE_LINE = 50;
    int MT_TYPE_ARROW = 51;
    int MT_TYPE_POLYLINE = 52;
    int MT_TYPE_SQUARE = 53;
    int MT_TYPE_CIRCLE = 54;
    int MT_TYPE_POLYGON = 55;
    int MT_TYPE_CLOUD = 56;
    int MT_TYPE_DISTANCE = 57;
    //
    int MT_TYPE_INK = 70;
    int MT_TYPE_ERASER = 71;
    //
    int MT_TYPE_DIVIDER = 80;
    int MT_TYPE_MULTI_SELECT = 81;

    //
    int MT_TYPE_MORE_NOTE = 200;
    int MT_TYPE_MORE_ATTACHMENT = 201;
    int MT_TYPE_MORE_STAMP = 202;
    int MT_TYPE_MORE_IMAGE = 203;
    int MT_TYPE_MORE_AUDIO = 204;
    int MT_TYPE_MORE_VIDEO = 205;
    //
    int MT_TYPE_MORE_TYPEWRITER = 220;
    int MT_TYPE_MORE_CALLOUT = 221;
    int MT_TYPE_MORE_TEXTBOX = 222;
    //
    int MT_TYPE_MORE_HIGHLIGHT = 230;
    int MT_TYPE_MORE_UNDERLINE = 231;
    int MT_TYPE_MORE_SQUIGGLY = 232;
    int MT_TYPE_MORE_STRIKEOUT = 233;
    int MT_TYPE_MORE_REPLACE = 234;
    int MT_TYPE_MORE_INSERT = 235;
    //
    int MT_TYPE_MORE_LINE = 250;
    int MT_TYPE_MORE_ARROW = 251;
    int MT_TYPE_MORE_POLYLINE = 252;
    int MT_TYPE_MORE_SQUARE = 253;
    int MT_TYPE_MORE_CIRCLE = 254;
    int MT_TYPE_MORE_POLYGON = 255;
    int MT_TYPE_MORE_CLOUD = 256;
    int MT_TYPE_MORE_DISTANCE = 257;
    //
    int MT_TYPE_MORE_INK = 270;
    int MT_TYPE_MORE_ERASER = 271;
    //
    int MT_TYPE_MORE_DIVIDER = 280;
    int MT_TYPE_MORE_MULTI_SELECT = 281;

    View getContentView();

    void registerListener(IMT_MoreClickListener listener);

    void unRegisterListener(IMT_MoreClickListener listener);
}
