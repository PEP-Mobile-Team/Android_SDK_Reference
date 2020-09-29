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
package com.foxit.uiextensions.config.uisettings;


import android.content.Context;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Renderer;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;

import java.util.HashMap;

public class UISettingsManager {

    private PDFViewCtrl mPDFViewCtrl;
    private Context mContext;
    private UISettingsConfig mUISettings;
    private UIExtensionsManager mUIExtensionsManager;

    public UISettingsManager(Context context, PDFViewCtrl pdfViewCtrl) {
        mPDFViewCtrl = pdfViewCtrl;
        mContext = context;
        mUIExtensionsManager = (UIExtensionsManager) (pdfViewCtrl.getUIExtensionsManager());
        mUISettings = mUIExtensionsManager.getConfig().uiSettings;
    }

    public void setUISettings() {
        //pageMode
        String pageMode = mUISettings.pageMode;
        if (!"Reflow".equals(pageMode)) {
            HashMap<String, Integer> diaplayMaps = arrays2Map(R.array.pageModes);
            Integer pageModeVaule = diaplayMaps.get(pageMode);
            if (pageModeVaule != null) {
                mPDFViewCtrl.setPageLayoutMode(pageModeVaule);
            }
        }

        // isContinuous
        mPDFViewCtrl.setContinuous(mUISettings.continuous);

        //zoomMode
        HashMap<String, Integer> zoomMaps = arrays2Map(R.array.zoomModes);
        Integer zoomModeVaule = zoomMaps.get(mUISettings.zoomMode);
        if (zoomModeVaule != null) {
            mPDFViewCtrl.setZoomMode(zoomModeVaule);
        }

        //colorMode
        if ("Map".equals(mUISettings.colorMode)) {
            mPDFViewCtrl.setMappingModeForegroundColor(mUISettings.mapForegroundColor);
            mPDFViewCtrl.setMappingModeBackgroundColor(mUISettings.mapBackgroundColor);
            mPDFViewCtrl.setColorMode(Renderer.e_ColorModeMapping);
        }

        //Normal background color
//        mPDFViewCtrl.setPageBackgroundColor(mUISettings.pageBackgroundColor);
        mPDFViewCtrl.setReflowBackgroundColor(mUISettings.reflowBackgroundColor);

        //highlightForm
        mUIExtensionsManager.enableFormHighlight(mUISettings.highlightForm);
        //highlightFormColor
        mUIExtensionsManager.setFormHighlightColor(mUISettings.highlightFormColor);
        //highlightLink
        mUIExtensionsManager.enableLinkHighlight(mUISettings.highlightLink);
        //highlightLinkColor
        mUIExtensionsManager.setLinkHighlightColor(mUISettings.highlightLinkColor);

        /*---------annotations--------*/
        //continuouslyAdd
        mUIExtensionsManager.setContinueAddAnnot(mUISettings.annotations.continuouslyAdd);
    }

    private HashMap<String, Integer> arrays2Map(int resid) {
        String[] strings = mContext.getResources().getStringArray(resid);
        HashMap<String, Integer> map = new HashMap<>();
        for (String str : strings) {
            String[] items = str.split("#");
            map.put(items[1], Integer.parseInt(items[0]));
        }
        return map;
    }

}
