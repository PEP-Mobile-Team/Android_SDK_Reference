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
package com.foxit.uiextensions.config.permissions;

import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class PermissionsConfig {

    private static final String KEY_RUN_JAVASCRIPT = "runJavaScript";
    private static final String KEY_COPY_TEXT = "copyText";
    private static final String KEY_DISABLE_LINK = "disableLink";

    private static final boolean DEFAULT_RUN_JAVASCRIPT = true;
    private static final boolean COPY_TEXT = true;
    private static final boolean DISABLE_LINK = false;

    public boolean runJavaScript = DEFAULT_RUN_JAVASCRIPT;
    public boolean copyText = COPY_TEXT;
    public boolean disableLink = DISABLE_LINK;

    public void parseConfig(JSONObject jsonObject) {
        try {
            JSONObject object = jsonObject.getJSONObject(Config.KEY_PERMISSIONS);
            runJavaScript = JsonUtil.getBoolean(object, KEY_RUN_JAVASCRIPT, DEFAULT_RUN_JAVASCRIPT);
            copyText = JsonUtil.getBoolean(object, KEY_COPY_TEXT, COPY_TEXT);
            disableLink = JsonUtil.getBoolean(object, KEY_DISABLE_LINK, DISABLE_LINK);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
