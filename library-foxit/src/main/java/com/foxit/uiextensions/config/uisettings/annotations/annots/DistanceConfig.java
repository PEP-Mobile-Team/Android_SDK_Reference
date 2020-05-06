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

public class DistanceConfig extends BaseConfig {
    private static final String KEY_SCALE_FROM_UNIT = "scaleFromUnit";
    private static final String KEY_SCALE_TO_UNIT = "scaleToUnit";
    private static final String KEY_SCALE_FROM_VALUE = "scaleFromValue";
    private static final String KEY_SCALE_TO_VALUE = "scaleToValue";

    private static final String DEFAULT_SCALE_FROM_UNIT = "pt";
    private static final String DEFAULT_SCALE_TO_UNIT = "pt";
    private static final int DEFAULT_SCALE_FROM_VALUE = 1;
    private static final int DEFAULT_SCALE_TO_VALUE = 1;
    private static final int DEFAULT_THICKNESS = 2;
    private static final int DEFAULT_COLOR = COLORS_TOOL_GROUP_1[6];

    public String scaleFromUnit;
    public String scaleToUnit;
    public int scaleFromValue;
    public int scaleToValue;

    public DistanceConfig() {
        color = DEFAULT_COLOR;
        scaleFromUnit = DEFAULT_SCALE_FROM_UNIT;
        scaleFromValue = DEFAULT_SCALE_FROM_VALUE;
        scaleToUnit = DEFAULT_SCALE_TO_UNIT;
        scaleToValue = DEFAULT_SCALE_TO_VALUE;
        thickness = DEFAULT_THICKNESS;
    }

    @Override
    public AnnotConfigInfo getAnnotConfigInfo() {
        AnnotConfigInfo info = new AnnotConfigInfo();
        info.defaultColor = DEFAULT_COLOR;
        info.defaultOpacity = DEFAULT_OPACITY;
        info.defaultThickness = DEFAULT_THICKNESS;
        return info;
    }

    @Override
    public String getTypeString() {
        return KEY_DISTANCE;
    }

    public void parseConfig(JSONObject jsonObject) {
        try {
            JSONObject object = jsonObject.getJSONObject(KEY_DISTANCE);

            color = getBordColor(object);
            opacity = getOpacity(object);
            thickness = getThickness(object);
            scaleFromUnit = JsonUtil.getString(object, KEY_SCALE_FROM_UNIT, DEFAULT_SCALE_FROM_UNIT);
            scaleToUnit = JsonUtil.getString(object, KEY_SCALE_TO_UNIT, DEFAULT_SCALE_TO_UNIT);
            scaleFromValue = JsonUtil.getInt(object, KEY_SCALE_FROM_VALUE, DEFAULT_SCALE_FROM_VALUE);
            if (scaleFromValue < 0) scaleFromValue = DEFAULT_SCALE_FROM_VALUE;
            scaleToValue = JsonUtil.getInt(object, KEY_SCALE_TO_VALUE, DEFAULT_SCALE_TO_VALUE);
            if (scaleToValue < 0) scaleToValue = DEFAULT_SCALE_TO_VALUE;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
