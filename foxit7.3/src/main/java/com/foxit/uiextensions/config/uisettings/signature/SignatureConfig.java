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
package com.foxit.uiextensions.config.uisettings.signature;


import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class SignatureConfig {
    public static final String KEY_UISETTING_SIGNATURE = "signature";
    private static final String KEY_COLOR = "color";
    private static final String KEY_THICKNESS = "thickness";

    public static final int DEFAULT_COLOR = PropertyBar.PB_COLORS_TOOL_GROUP_2.clone()[12];
    public static final int DEFAULT_THICKNESS = 4;

    public int color;
    public int thickness;

    public SignatureConfig() {
        color = DEFAULT_COLOR;
        thickness = DEFAULT_THICKNESS;
    }

    public void parseConfig(JSONObject jsonObject) {
        try {
            JSONObject object = jsonObject.getJSONObject(KEY_UISETTING_SIGNATURE);
            //color
            if (object.has(KEY_COLOR)){
                color = JsonUtil.parseColorString(object, KEY_COLOR, DEFAULT_COLOR);
            }
            //thickness
            if (object.has(KEY_THICKNESS)){
                thickness = JsonUtil.getInt(object, KEY_THICKNESS, DEFAULT_THICKNESS);
                if (thickness < 1 || thickness > 12) {
                    thickness = DEFAULT_THICKNESS;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
