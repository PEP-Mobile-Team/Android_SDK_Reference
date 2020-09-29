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
package com.foxit.uiextensions.config.uisettings.annotations.annots;


public class StrikeoutConfig extends BaseConfig {

    public static final int DEFAULT_COLOR = COLORS_TOOL_GROUP_1[6];

    public StrikeoutConfig() {
        color = DEFAULT_COLOR;
    }

    @Override
    public AnnotConfigInfo getAnnotConfigInfo() {
        AnnotConfigInfo info = new AnnotConfigInfo();
        info.defaultColor = DEFAULT_COLOR;
        info.defaultOpacity = DEFAULT_OPACITY;
        return info;
    }

    @Override
    public String getTypeString() {
        return KEY_TEXTMARK_STO;
    }
}
