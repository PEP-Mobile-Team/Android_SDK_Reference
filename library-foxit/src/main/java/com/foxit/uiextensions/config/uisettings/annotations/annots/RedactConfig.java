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
package com.foxit.uiextensions.config.uisettings.annotations.annots;


import com.foxit.uiextensions.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class RedactConfig extends BaseConfig {

    private static final String KEY_TEXT_FACE = "textFace";
    private static final String KEY_TEXT_SIZE = "textSize";
    private static final String KEY_TEXT_COLOR = "textColor";
    private static final String KEY_FILL_COLOR = "fillColor";

    protected static final int DEFAULT_FILL_COLOR = COLORS_TOOL_GROUP_1[12];
    private static final int DEFAULT_TEXT_COLOR = COLORS_TOOL_GROUP_1[6];
    private static final String DEFAULT_TEXT_FACE = "Courier";
    private static final int DEFAULT_TEXT_SIZE = 12;

    public String textFace;
    public int textSize;
    public int textColor;
    public int fillColor;

    public RedactConfig() {
        fillColor = DEFAULT_FILL_COLOR;
        textColor = DEFAULT_TEXT_COLOR;
        textFace = DEFAULT_TEXT_FACE;
        textSize = DEFAULT_TEXT_SIZE;
    }

    public void parseConfig(JSONObject jsonObject) {
        try {
            JSONObject object = jsonObject.getJSONObject(getTypeString());

            fillColor = JsonUtil.parseColorString(object, KEY_FILL_COLOR, DEFAULT_FILL_COLOR);
            textColor = JsonUtil.parseColorString(object, KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR);
            textFace = JsonUtil.getString(object, KEY_TEXT_FACE, DEFAULT_TEXT_FACE);
            textSize = JsonUtil.getInt(object, KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE);
            if (textSize <= 0) {
                textSize = DEFAULT_TEXT_SIZE;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getTypeString() {
        return KEY_TEXTMARK_REDACT;
    }

    @Override
    public AnnotConfigInfo getAnnotConfigInfo() {
        return null;
    }
}
