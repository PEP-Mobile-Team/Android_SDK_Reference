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

public class NoteConfig extends BaseConfig {
    public static final String KEY_ICON = "icon";

    public static final int DEFAULT_COLOR = COLORS_TOOL_GROUP_1[0];
    public static final String DEFAULT_ICON = "Comment";

    public String icon;

    public NoteConfig() {
        color = DEFAULT_COLOR;
        icon = DEFAULT_ICON;
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
        return KEY_NOTE;
    }

    public void parseConfig(JSONObject jsonObject) {
        try {
            JSONObject object = jsonObject.getJSONObject(KEY_NOTE);

            color = getBordColor(object);
            opacity = getOpacity(object);
            icon = JsonUtil.getString(object, KEY_ICON, DEFAULT_ICON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
