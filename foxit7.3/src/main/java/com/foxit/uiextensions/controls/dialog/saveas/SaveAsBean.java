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
package com.foxit.uiextensions.controls.dialog.saveas;


public class SaveAsBean {
    public OptimizerImageSettings imageSettings;
    public OptimizerMonoSettings monoSettings;

    public String title;
    public int format;
    public boolean checked;
    public boolean haveSecondOptions;

    public SaveAsBean(String title, int format, boolean checked) {
        this.format = format;
        this.title = title;
        this.checked = checked;
    }

    public static class OptimizerImageSettings {
        public int quality;
    }

    public static class OptimizerMonoSettings {
        public int quality;
    }
}
