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


import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseConfig {
    // Text Markup
    public static final String KEY_TEXTMARK_HIGHLIGHT = "highlight";
    public static final String KEY_TEXTMARK_UNDERLINE = "underline";
    public static final String KEY_TEXTMARK_SQG = "squiggly";
    public static final String KEY_TEXTMARK_STO = "strikeout";
    public static final String KEY_TEXTMARK_INSERT = "insert";
    public static final String KEY_TEXTMARK_REPLACE = "replace";
    public static final String KEY_TEXTMARK_REDACT= "redaction";

    // Drawing
    public static final String KEY_DRAWING_LINE = "line";
    public static final String KEY_DRAWING_SQUARE = "rectangle";
    public static final String KEY_DRAWING_CIRCLE = "oval";
    public static final String KEY_DRAWING_ARROW = "arrow";
    public static final String KEY_DRAWING_PENCIL = "pencil";
    public static final String KEY_DRAWING_POLYGON = "polygon";
    public static final String KEY_DRAWING_CLOUD = "cloud";
    public static final String KEY_DRAWING_POLYLINE = "polyline";

    //Others
    public static final String KEY_TYPWRITER = "typewriter";
    public static final String KEY_CALLOUT = "callout";
    public static final String KEY_TEXTBOX = "textbox";
    public static final String KEY_NOTE = "note";
    public static final String KEY_FILEATTACHMENT = "attachment";
    public static final String KEY_DISTANCE = "distance";
    public static final String KEY_IMAGE = "image";

    protected static final int[] COLORS_TOOL_GROUP_1 = PropertyBar.PB_COLORS_TOOL_GROUP_1.clone();

    protected static final String KEY_BORD_COLOR = "color";
    protected static final String KEY_TEXT_COLOR = "textColor";
    protected static final String KEY_OPACITY = "opacity";
    protected static final String KEY_THICKNESS = "thickness";
    protected static final String KEY_ROTATION = "rotation";

    protected static final int DEFAULT_BORD_COLOR = COLORS_TOOL_GROUP_1[0];
    protected static final int DEFAULT_TEXT_COLOR = COLORS_TOOL_GROUP_1[0];
    protected static final float DEFAULT_OPACITY = 1.0f;
    protected static final int DEFAULT_THICKNESS = 2;
    protected static final int DEFAULT_ROTATION = 0;

    public int color = DEFAULT_BORD_COLOR;
    public int textColor = DEFAULT_TEXT_COLOR;
    public double opacity = DEFAULT_OPACITY;
    public int thickness = DEFAULT_THICKNESS;
    public int rotation = DEFAULT_ROTATION;

    public void parseConfig(JSONObject jsonObject, String name) {
        try {
            JSONObject object = jsonObject.getJSONObject(name);
            //color
            if (object.has(KEY_BORD_COLOR)) {
                color = getBordColor(object);
            }
            //textcolor
            if (object.has(KEY_TEXT_COLOR)) {
                textColor = getTextColor(object);
            }
            //opacity
            if (object.has(KEY_OPACITY)) {
                opacity = getOpacity(object);
            }
            //thickness
            if (object.has(KEY_THICKNESS)) {
                thickness = getThickness(object);
            }
            //rotation
            if (object.has(KEY_ROTATION)) {
                rotation = JsonUtil.getInt(object, KEY_ROTATION, getAnnotConfigInfo().defaultRotation);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected int getBordColor(JSONObject jsonObject) {
        return JsonUtil.parseColorString(jsonObject, KEY_BORD_COLOR, getAnnotConfigInfo().defaultColor);
    }

    protected int getTextColor(JSONObject jsonObject) {
        return JsonUtil.parseColorString(jsonObject, KEY_TEXT_COLOR, getAnnotConfigInfo().defaultTextColor);
    }

    protected Double getOpacity(JSONObject jsonObject) {
        double opacity = JsonUtil.getDouble(jsonObject, KEY_OPACITY, getAnnotConfigInfo().defaultOpacity);
        if (opacity < 0 || opacity > 1) {
            opacity = getAnnotConfigInfo().defaultOpacity;
        }
        return opacity;
    }

    protected int getThickness(JSONObject jsonObject) {
        int thickness = JsonUtil.getInt(jsonObject, KEY_THICKNESS, getAnnotConfigInfo().defaultThickness);
        if (thickness < 1 || thickness > 12) {
            thickness = getAnnotConfigInfo().defaultThickness;
        }
        return thickness;
    }

    public class AnnotConfigInfo {
        public int defaultColor = COLORS_TOOL_GROUP_1[0];
        public int defaultTextColor = COLORS_TOOL_GROUP_1[0];
        public double defaultOpacity = 1.0f;
        public int defaultThickness = 5;
        public int defaultRotation = 0;
    }

    public abstract String getTypeString();

    public abstract AnnotConfigInfo getAnnotConfigInfo();

}
