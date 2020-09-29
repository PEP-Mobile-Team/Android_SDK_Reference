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
package com.foxit.uiextensions.config.uisettings.form.field;


import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class BaseFieldConfig {
    private static final String KEY_TEXT_COLOR = "textColor";
    private static final String KEY_TEXT_FACE = "textFace";
    private static final String KEY_TEXT_SIZE = "textSize";

    private static final int DEFAULT_TEXT_COLOR = PropertyBar.PB_COLORS_FORM[12];
    private static final String DEFAULT_TEXT_FACE = "Courier";
    private static final int DEFAULT_TEXT_SIZE = 0;

    public int textSize = DEFAULT_TEXT_SIZE;
    public int textColor = DEFAULT_TEXT_COLOR;
    public String textFace = DEFAULT_TEXT_FACE;

    public void parseConfig(JSONObject jsonObject, String name) {
        try {
            JSONObject object = jsonObject.getJSONObject(name);

            //textColor
            if (object.has(KEY_TEXT_COLOR)) {
                textColor = JsonUtil.parseColorString(object, KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR);
            }
            //textFace
            if (object.has(KEY_TEXT_FACE)) {
                textFace = JsonUtil.getString(object, KEY_TEXT_FACE, DEFAULT_TEXT_FACE);
            }
            //textSize
            if (object.has(KEY_TEXT_SIZE)) {
                textSize = JsonUtil.getInt(object, KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
