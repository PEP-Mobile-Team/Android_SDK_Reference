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
package com.foxit.uiextensions.config.uisettings;


import android.graphics.Color;

import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.config.uisettings.annotations.AnnotationsConfig;
import com.foxit.uiextensions.config.uisettings.signature.SignatureConfig;
import com.foxit.uiextensions.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class UISettingsConfig {
    private static final String KEY_PAGE_MODE = "pageMode";
    private static final String KEY_IS_CONTINUOUS = "continuous";
    private static final String KEY_ZOOM_MODE = "zoomMode";
    private static final String KEY_COLOR_MODE = "colorMode";
    private static final String KEY_MAP_FOREGROUND_COLOR = "mapForegroundColor";
    private static final String KEY_MAP_BACKGROUND_COLOR = "mapBackgroundColor";
    private static final String KEY_DISABLE_FORM_NAVIGATION = "disableFormNavigationBar";
    private static final String KEY_HIGHLIGHT_FORM = "highlightForm";
    private static final String KEY_HIGHLIGHT_FORM_COLOR = "highlightFormColor";
    private static final String KEY_HIGHLIGHT_LINK = "highlightLink";
    private static final String KEY_HIGHLIGHT_LINK_COLOR = "highlightLinkColor";
    private static final String KEY_FULLSCREEN = "fullscreen";
//    private static final String KEY_PAGEBACKGROUNDCOLOR = "pageBackgroundColor";
    private static final String KEY_REFLOWBACKGROUNDCOLOR = "reflowBackgroundColor";

    private static final String DEFLAULT_PAGE_MODE = "Single";// ("Single"/"Facing"/"CoverLeft"/"CoverMiddle"/"CoverRight"/"Reflow")
    private static final boolean DEFLAULT_IS_CONTINUOUS = false;
    private static final String DEFLAULT_ZOOM_MODE = "FitWidth";//("FitWidth"/"FitPage")
    private static final String DEFLAULT_COLOR_MODE = "Normal";//("Normal"/"Night"/"Map")
    private static final int DEFLAULT_MAP_FOREGROUND_COLOR = Color.argb(0xff, 0x5d, 0x5b, 0x71);
    private static final int DEFLAULT_MAP_BACKGROUND_COLOR = Color.argb(0xff, 0x00, 0x00, 0x1b);
    private static final boolean DEFLAULT_DISABLE_FORM_NAVIGATION = false;
    private static final boolean DEFLAULT_HIGHLIGHT_FORM = true;
    private static final int DEFLAULT_HIGHLIGHT_FORM_COLOR = 0x200066cc;
    private static final boolean DEFLAULT_HIGHLIGHT_LINK = true;
    private static final int DEFLAULT_HIGHLIGHT_LINK_COLOR = 0x16007FFF;
    private static final boolean DEFLAULT_FULLSCREEN = true;

    public AnnotationsConfig annotations = new AnnotationsConfig();
    public SignatureConfig signature = new SignatureConfig();

    public String pageMode = DEFLAULT_PAGE_MODE;
    public boolean continuous = DEFLAULT_IS_CONTINUOUS;
    public String zoomMode = DEFLAULT_ZOOM_MODE;
    public String colorMode = DEFLAULT_COLOR_MODE;
    public int mapForegroundColor = DEFLAULT_MAP_FOREGROUND_COLOR;
    public int mapBackgroundColor = DEFLAULT_MAP_BACKGROUND_COLOR;
    public boolean disableFormNavigationBar = DEFLAULT_DISABLE_FORM_NAVIGATION;
    public boolean highlightForm = DEFLAULT_HIGHLIGHT_FORM;
    public int highlightFormColor = DEFLAULT_HIGHLIGHT_FORM_COLOR;
    public boolean highlightLink = DEFLAULT_HIGHLIGHT_LINK;
    public int highlightLinkColor = DEFLAULT_HIGHLIGHT_LINK_COLOR;
    public boolean fullscreen = DEFLAULT_FULLSCREEN;
//    public int pageBackgroundColor = Color.WHITE;
    public int reflowBackgroundColor = Color.WHITE;

    public void parseConfig(JSONObject jsonObject) {
        try {
            JSONObject object = jsonObject.getJSONObject(Config.KEY_UISETTING);

            pageMode = JsonUtil.getString(object, KEY_PAGE_MODE, DEFLAULT_PAGE_MODE);
            continuous = JsonUtil.getBoolean(object, KEY_IS_CONTINUOUS, DEFLAULT_IS_CONTINUOUS);
            zoomMode = JsonUtil.getString(object, KEY_ZOOM_MODE, DEFLAULT_ZOOM_MODE);
            colorMode = JsonUtil.getString(object, KEY_COLOR_MODE, DEFLAULT_COLOR_MODE);
            mapForegroundColor = JsonUtil.parseColorString(object, KEY_MAP_FOREGROUND_COLOR, DEFLAULT_MAP_FOREGROUND_COLOR);
            mapBackgroundColor = JsonUtil.parseColorString(object, KEY_MAP_BACKGROUND_COLOR, DEFLAULT_MAP_BACKGROUND_COLOR);
            disableFormNavigationBar = JsonUtil.getBoolean(object, KEY_DISABLE_FORM_NAVIGATION, DEFLAULT_DISABLE_FORM_NAVIGATION);
            highlightForm = JsonUtil.getBoolean(object, KEY_HIGHLIGHT_FORM, DEFLAULT_HIGHLIGHT_FORM);
            highlightFormColor = JsonUtil.parseColorString(object, KEY_HIGHLIGHT_FORM_COLOR, DEFLAULT_HIGHLIGHT_FORM_COLOR);
            highlightLink = JsonUtil.getBoolean(object, KEY_HIGHLIGHT_LINK, DEFLAULT_HIGHLIGHT_LINK);
            highlightLinkColor = JsonUtil.parseColorString(object, KEY_HIGHLIGHT_LINK_COLOR, DEFLAULT_HIGHLIGHT_LINK_COLOR);
            fullscreen = JsonUtil.getBoolean(object, KEY_FULLSCREEN, DEFLAULT_FULLSCREEN);
//            pageBackgroundColor = JsonUtil.parseColorString(object, KEY_PAGEBACKGROUNDCOLOR, pageBackgroundColor);
            reflowBackgroundColor = JsonUtil.parseColorString(object, KEY_REFLOWBACKGROUNDCOLOR, reflowBackgroundColor);

            //annotations
            if (object.has(AnnotationsConfig.KEY_UISETTING_ANNOTATIONS)) {
                annotations.parseConfig(object);
            }
            //signature
            if (object.has(SignatureConfig.KEY_UISETTING_SIGNATURE)) {
                signature.parseConfig(object);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
