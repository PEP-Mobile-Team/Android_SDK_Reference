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
package com.foxit.uiextensions.config.uisettings.form;


import com.foxit.uiextensions.config.uisettings.form.field.CheckBoxConfig;
import com.foxit.uiextensions.config.uisettings.form.field.ComboBoxConfig;
import com.foxit.uiextensions.config.uisettings.form.field.ListBoxConfig;
import com.foxit.uiextensions.config.uisettings.form.field.RadioButtonConfig;
import com.foxit.uiextensions.config.uisettings.form.field.TextFieldConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class FormConfig {
    public static final String KEY_UISETTING_FORM = "form";

    private static final String KEY_TEXTFIELD = "textField";
    private static final String KEY_CHECKBOX = "checkBox";
    private static final String KEY_RADIOBUTTON = "radioButton";
    private static final String KEY_COMOBOX = "comboBox";
    private static final String KEY_LISTBOX = "listBox";

    public TextFieldConfig textField = new TextFieldConfig();
    public CheckBoxConfig checkBox = new CheckBoxConfig();
    public RadioButtonConfig radioButton = new RadioButtonConfig();
    public ComboBoxConfig comboBox = new ComboBoxConfig();
    public ListBoxConfig listBox = new ListBoxConfig();

    public void parseConfig(JSONObject object) {
        try {
            JSONObject jsonObject = object.getJSONObject(KEY_UISETTING_FORM);
            //textField
            if (jsonObject.has(KEY_TEXTFIELD)) {
                textField.parseConfig(jsonObject, KEY_TEXTFIELD);
            }
            //checkbox
            if (jsonObject.has(KEY_CHECKBOX)) {
                checkBox.parseConfig(jsonObject, KEY_CHECKBOX);
            }
            //radiobutton
            if (jsonObject.has(KEY_RADIOBUTTON)) {
                radioButton.parseConfig(jsonObject, KEY_RADIOBUTTON);
            }
            //combobox
            if (jsonObject.has(KEY_COMOBOX)) {
                comboBox.parseConfig(jsonObject, KEY_COMOBOX);
            }
            //listbox
            if (jsonObject.has(KEY_LISTBOX)) {
                listBox.parseConfig(jsonObject, KEY_LISTBOX);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
