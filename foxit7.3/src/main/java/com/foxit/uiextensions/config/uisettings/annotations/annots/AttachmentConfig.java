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

public class AttachmentConfig extends BaseConfig {
    private static final String KEY_ICON = "icon";

    private static final int DEFAULT_COLOR = COLORS_TOOL_GROUP_1[0];
    private static final String DEFAULT_ICON = "PushPin"; //icon：Graph/PushPin/Paperclip/Tag

    public String icon;

    public AttachmentConfig() {
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
        return KEY_FILEATTACHMENT;
    }

    public void parseConfig(JSONObject jsonObject) {
        try {
            JSONObject object = jsonObject.getJSONObject(KEY_FILEATTACHMENT);

            color = getBordColor(object);
            opacity = getOpacity(object);
            icon = JsonUtil.getString(object, KEY_ICON, DEFAULT_ICON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
