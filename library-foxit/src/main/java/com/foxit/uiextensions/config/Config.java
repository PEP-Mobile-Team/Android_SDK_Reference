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
package com.foxit.uiextensions.config;


import com.foxit.uiextensions.config.modules.ModulesConfig;
import com.foxit.uiextensions.config.permissions.PermissionsConfig;
import com.foxit.uiextensions.config.uisettings.UISettingsConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;

/**
 * Class <code>Config</code> used to the json configure.
 */
public class Config {
    /** The key: modules*/
    public static final String KEY_MODULES = "modules";
    /** The key: permissions*/
    public static final String KEY_PERMISSIONS = "permissions";
    /** The key: uiSettings*/
    public static final String KEY_UISETTING = "uiSettings";

    /// @cond DEV

    public ModulesConfig modules;
    public PermissionsConfig permissions;
    public UISettingsConfig uiSettings;

    /// @endcond

    public Config() {
        modules = new ModulesConfig();
        uiSettings = new UISettingsConfig();
        permissions = new PermissionsConfig();
    }

    public Config(@NonNull InputStream stream) {
        modules = new ModulesConfig();
        uiSettings = new UISettingsConfig();
        permissions = new PermissionsConfig();

        read(stream);
    }

    private void read(InputStream stream) {
        byte[] buffer = new byte[1 << 13];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int n = 0;
        try {
            while (-1 != (n = stream.read(buffer))) {
                baos.write(buffer, 0, n);
            }
            String config = baos.toString("utf-8");
            if (config.trim().length() > 1) {
                parseConfig(config);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                baos.flush();
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseConfig(@NonNull String config) {
        try {
            JSONObject jsonObject = new JSONObject(config);
            //modules
            if (jsonObject.has(KEY_MODULES)) {
                modules.parseConfig(jsonObject);
            }
            //permission
            if (jsonObject.has(KEY_PERMISSIONS)) {
                permissions.parseConfig(jsonObject);
            }
            //uiSettings
            if (jsonObject.has(KEY_UISETTING)) {
                uiSettings.parseConfig(jsonObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
