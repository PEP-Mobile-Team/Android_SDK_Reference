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


import com.foxit.uiextensions.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class FreeTextConfig extends BaseConfig {
    private static final String KEY_TEXT_FACE = "textFace";
    private static final String KEY_TEXT_SIZE = "textSize";

    protected static final int DEFAULT_COLOR = COLORS_TOOL_GROUP_1[6];
    private static final int DEFAULT_TEXT_COLOR = COLORS_TOOL_GROUP_1[10];
    private static final String DEFAULT_TEXT_FACE = "Courier";
    private static final int DEFAULT_TEXT_SIZE = 18;

    public String textFace;
    public int textSize;

    public FreeTextConfig() {
        color = DEFAULT_COLOR;
        textColor = DEFAULT_TEXT_COLOR;
        textFace = DEFAULT_TEXT_FACE;
        textSize = DEFAULT_TEXT_SIZE;
    }

    public void parseConfig(JSONObject jsonObject) {
        try {
            JSONObject object = jsonObject.getJSONObject(getTypeString());

            color = getBordColor(object);
            textColor = getTextColor(object);
            opacity = getOpacity(object);
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
    public AnnotConfigInfo getAnnotConfigInfo() {
        AnnotConfigInfo info = new AnnotConfigInfo();
        info.defaultColor = DEFAULT_COLOR;
        info.defaultOpacity = DEFAULT_OPACITY;
        info.defaultTextColor = DEFAULT_TEXT_COLOR;
        return info;
    }
}
