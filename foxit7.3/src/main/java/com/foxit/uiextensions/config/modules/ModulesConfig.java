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
package com.foxit.uiextensions.config.modules;

import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.config.modules.annotations.AnnotationsConfig;
import com.foxit.uiextensions.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class ModulesConfig {
    public static final String KEY_MODULE_ANNOTATIONS = "annotations";

    private static final String KEY_MODULE_READINGBOOKMARK = "readingbookmark";
    private static final String KEY_MODULE_OUTLINE = "outline";
    private static final String KEY_MODULE_THUMBNAIL = "thumbnail";
    private static final String KEY_MODULE_ATTACHMENT = "attachment";
    private static final String KEY_MODULE_SIGNATURE = "signature";
    private static final String KEY_MODULE_SEARCH = "search";
    private static final String KEY_MODULE_SELECTION = "selection";

    private static final String KEY_MODULE_OLD_PAGENAVIGATION = "pageNavigation";
    private static final String KEY_MODULE_PAGENAVIGATION = "navigation";

    private static final String KEY_MODULE_ENCRYPTION = "encryption";
    private static final String KEY_MODULE_FORM = "form";
    private static final String KEY_MODULE_MULTI_SELECT = "multipleSelection";

    private boolean isLoadReadingBookmark = true;
    private boolean isLoadOutline = true;
    private boolean isLoadAnnotations = true;
    private boolean isLoadThumbnail = true;
    private boolean isLoadAttachment = true;
    private boolean isLoadSignature = true;
    private boolean isLoadSearch = true;
    private boolean isLoadTextSelection = true;
    private boolean isLoadPageNavigation = true;
    private boolean isLoadFileEncryption = true;
    private boolean isLoadForm = true;
    private boolean isLoadMultiSelect = true;

    private AnnotationsConfig annotations;

    public ModulesConfig() {
        annotations = new AnnotationsConfig();
    }

    public void parseConfig(JSONObject jsonObject) {
        try {
            JSONObject modules = jsonObject.getJSONObject(Config.KEY_MODULES);
            isLoadReadingBookmark = JsonUtil.getBoolean(modules, KEY_MODULE_READINGBOOKMARK, true);
            isLoadOutline = JsonUtil.getBoolean(modules, KEY_MODULE_OUTLINE, true);
            isLoadThumbnail = JsonUtil.getBoolean(modules, KEY_MODULE_THUMBNAIL, true);
            isLoadAttachment = JsonUtil.getBoolean(modules, KEY_MODULE_ATTACHMENT, true);
            isLoadSignature = JsonUtil.getBoolean(modules, KEY_MODULE_SIGNATURE, true);
            isLoadSearch = JsonUtil.getBoolean(modules, KEY_MODULE_SEARCH, true);
            isLoadTextSelection = JsonUtil.getBoolean(modules, KEY_MODULE_SELECTION, true);

            String[] navigationKeys = new String[]{KEY_MODULE_OLD_PAGENAVIGATION, KEY_MODULE_PAGENAVIGATION};
            for (String str : navigationKeys) {
                if (modules.has(str) && modules.get(str) instanceof Boolean) {
                    isLoadPageNavigation = JsonUtil.getBoolean(modules, str, true);
                    break;
                }
            }

            isLoadFileEncryption = JsonUtil.getBoolean(modules, KEY_MODULE_ENCRYPTION, true);
            isLoadForm = JsonUtil.getBoolean(modules, KEY_MODULE_FORM, true);
            isLoadMultiSelect = JsonUtil.getBoolean(modules, KEY_MODULE_MULTI_SELECT, true);

            annotations.setLoadFileattach(isLoadAttachment);
            if (modules.has(KEY_MODULE_ANNOTATIONS)) {
                if (modules.get(KEY_MODULE_ANNOTATIONS) instanceof JSONObject) {
                    annotations.parseConfig(modules);

                    boolean isLoadAnnotsConfig = false;
                    Map<String, Boolean> mAnnotState = annotations.getAnnotConfigMap();
                    for (Boolean bool : mAnnotState.values()) {
                        if (bool) {
                            isLoadAnnotsConfig = true;
                            break;
                        }
                    }
                    isLoadAnnotations = isLoadAnnotsConfig || isLoadAttachment();
                } else {
                    boolean isLoadAnnotsConfig = JsonUtil.getBoolean(modules, KEY_MODULE_ANNOTATIONS, true);
                    if (!isLoadAnnotsConfig) {
                        annotations.closeAnnotsConfig();
                    }
                    isLoadAnnotations = isLoadAnnotsConfig || isLoadAttachment();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean isLoadReadingBookmark() {
        return isLoadReadingBookmark;
    }

    public boolean isLoadOutline() {
        return isLoadOutline;
    }

    public boolean isLoadAnnotations() {
        return isLoadAnnotations;
    }

    public boolean isLoadThumbnail() {
        return isLoadThumbnail;
    }

    public boolean isLoadAttachment() {
        return isLoadAttachment;
    }

    public boolean isLoadSignature() {
        return isLoadSignature;
    }

    public boolean isLoadSearch() {
        return isLoadSearch;
    }

    public boolean isLoadTextSelection() {
        return isLoadTextSelection;
    }

    public boolean isLoadPageNavigation() {
        return isLoadPageNavigation;
    }

    public boolean isLoadFileEncryption() {
        return isLoadFileEncryption;
    }

    public boolean isLoadForm() {
        return isLoadForm;
    }

    public boolean isLoadMultiSelect() {
        return isLoadMultiSelect;
    }

    public AnnotationsConfig getAnnotConfig() {
        return annotations;
    }

    public void enableAnnotations(boolean enable) {
        isLoadAnnotations = enable;
        isLoadAttachment = enable;
        annotations.setLoadFileattach(enable);
        if (!enable) {
            annotations.closeAnnotsConfig();
        }
    }

}
