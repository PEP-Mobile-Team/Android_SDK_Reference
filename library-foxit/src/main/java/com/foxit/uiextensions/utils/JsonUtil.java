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
package com.foxit.uiextensions.utils;


import android.graphics.Color;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {

    public static int parseColorString(JSONObject jsonObject, String name, int defaultColor) {
        try {
            if (jsonObject.has(name) && jsonObject.get(name) instanceof String) {
                String colorString = jsonObject.getString(name);
                return Color.parseColor(colorString);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return defaultColor;
    }

    public static int getInt(JSONObject jsonObject, String name, int defaultVaule) {
        try {
            if (jsonObject.has(name) && (jsonObject.get(name) instanceof Integer || jsonObject.get(name) instanceof Number)) {
                return jsonObject.getInt(name);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return defaultVaule;
    }

    public static double getDouble(JSONObject jsonObject, String name, double defaultValue) {
        try {
            if (jsonObject.has(name) && (jsonObject.get(name) instanceof Double || jsonObject.get(name) instanceof Number)) {
                return jsonObject.getDouble(name);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public static String getString(JSONObject jsonObject, String name, String defaultValue) {
        try {
            if (jsonObject.has(name) && jsonObject.get(name) instanceof String) {
                return jsonObject.getString(name);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public static boolean getBoolean(JSONObject jsonObject, String name, boolean defaultValue) {
        try {
            if (jsonObject.has(name) && jsonObject.get(name) instanceof Boolean) {
                return jsonObject.getBoolean(name);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

}
