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
package com.foxit.uiextensions.config.modules.annotations;


import com.foxit.uiextensions.config.modules.ModulesConfig;
import com.foxit.uiextensions.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AnnotationsConfig {
    // Text Markup
    private static final String KEY_TEXTMARK_HIGHLIGHT = "highlight";
    private static final String KEY_TEXTMARK_UNDERLINE = "underline";
    private static final String KEY_TEXTMARK_SQG = "squiggly";
    private static final String KEY_TEXTMARK_STO = "strikeout";
    private static final String KEY_TEXTMARK_REDACTION = "redaction";

    private static final String KEY_TEXTMARK_OLD_INSERT = "inserttext";
    private static final String KEY_TEXTMARK_INSERT = "insert";
    private static final String KEY_TEXTMARK_OLD_REPLACE = "replacetext";
    private static final String KEY_TEXTMARK_REPLACE = "replace";

    // Drawing
    private static final String KEY_DRAWING_LINE = "line";
    private static final String KEY_DRAWING_SQUARE = "rectangle";
    private static final String KEY_DRAWING_CIRCLE = "oval";
    private static final String KEY_DRAWING_ARROW = "arrow";
    private static final String KEY_DRAWING_PENCIL = "pencil";
    private static final String KEY_DRAWING_ERASER = "eraser";
    private static final String KEY_DRAWING_POLYGON = "polygon";
    private static final String KEY_DRAWING_CLOUD = "cloud";
    private static final String KEY_DRAWING_POLYLINE = "polyline";

    //Others
    private static final String KEY_TYPWRITER = "typewriter";
    private static final String KEY_CALLOUT = "callout";
    private static final String KEY_TEXTBOX = "textbox";
    private static final String KEY_NOTE = "note";
    private static final String KEY_STAMP = "stamp";
    private static final String KEY_FILEATTACH = "attachment";
    private static final String KEY_DISTANCE = "distance";
    private static final String KEY_IMAGE = "image";
    private static final String KEY_AUDIO = "audio";
    private static final String KEY_VIDEO = "video";

    private boolean isLoadHighlight = true;
    private boolean isLoadUnderline = true;
    private boolean isLoadSquiggly = true;
    private boolean isLoadStrikeout = true;
    private boolean isLoadRedaction = true;
    private boolean isLoadInsertText = true;
    private boolean isLoadReplaceText = true;

    private boolean isLoadDrawLine = true;
    private boolean isLoadDrawSquare = true;
    private boolean isLoadDrawCircle = true;
    private boolean isLoadDrawArrow = true;
    private boolean isLoadDrawDistance = true;
    private boolean isLoadDrawPencil = true;
    private boolean isLoadEraser = true;
    private boolean isLoadDrawPolygon = true;
    private boolean isLoadDrawCloud = true;
    private boolean isLoadDrawPolyLine = true;

    private boolean isLoadTypewriter = true;
    private boolean isLoadTextbox = true;
    private boolean isLoadCallout = true;
    private boolean isLoadNote = true;
    private boolean isLoadStamp = true;
    private boolean isLoadFileattach = true;
    private boolean isLoadImage = true;
    private boolean isLoadAudio = true;
    private boolean isLoadVideo = true;

    private Map<String, Boolean> mMapSaveAnnotConfig = new HashMap<String, Boolean>();

    public void parseConfig(JSONObject jsonObject) {
        try {
            JSONObject annotObject = jsonObject.getJSONObject(ModulesConfig.KEY_MODULE_ANNOTATIONS);
            isLoadHighlight = JsonUtil.getBoolean(annotObject, KEY_TEXTMARK_HIGHLIGHT, true);
            isLoadUnderline = JsonUtil.getBoolean(annotObject, KEY_TEXTMARK_UNDERLINE, true);
            isLoadSquiggly = JsonUtil.getBoolean(annotObject, KEY_TEXTMARK_SQG, true);
            isLoadStrikeout = JsonUtil.getBoolean(annotObject, KEY_TEXTMARK_STO, true);
            isLoadRedaction = JsonUtil.getBoolean(annotObject, KEY_TEXTMARK_REDACTION, true);

            String[] insertKeys = new String[]{KEY_TEXTMARK_OLD_INSERT, KEY_TEXTMARK_INSERT};
            for (String str : insertKeys) {
                if (annotObject.has(str) && annotObject.get(str) instanceof Boolean) {
                    isLoadInsertText = JsonUtil.getBoolean(annotObject, str, true);
                    break;
                }
            }
            String[] replaceKeys = new String[]{KEY_TEXTMARK_OLD_REPLACE, KEY_TEXTMARK_REPLACE};
            for (String str : replaceKeys) {
                if (annotObject.has(str) && annotObject.get(str) instanceof Boolean) {
                    isLoadReplaceText = JsonUtil.getBoolean(annotObject, str, true);
                    break;
                }
            }

            isLoadDrawLine = JsonUtil.getBoolean(annotObject, KEY_DRAWING_LINE, true);
            isLoadDrawSquare = JsonUtil.getBoolean(annotObject, KEY_DRAWING_SQUARE, true);
            isLoadDrawCircle = JsonUtil.getBoolean(annotObject, KEY_DRAWING_CIRCLE, true);
            isLoadDrawArrow = JsonUtil.getBoolean(annotObject, KEY_DRAWING_ARROW, true);
            isLoadDrawPencil = JsonUtil.getBoolean(annotObject, KEY_DRAWING_PENCIL, true);
            isLoadEraser = JsonUtil.getBoolean(annotObject, KEY_DRAWING_ERASER, true);
            isLoadDrawPolygon = JsonUtil.getBoolean(annotObject, KEY_DRAWING_POLYGON, true);
            isLoadDrawCloud = JsonUtil.getBoolean(annotObject, KEY_DRAWING_CLOUD, true);
            isLoadDrawPolyLine = JsonUtil.getBoolean(annotObject, KEY_DRAWING_POLYLINE, true);

            isLoadTypewriter = JsonUtil.getBoolean(annotObject, KEY_TYPWRITER, true);
            isLoadCallout = JsonUtil.getBoolean(annotObject, KEY_CALLOUT, true);
            isLoadTextbox = JsonUtil.getBoolean(annotObject, KEY_TEXTBOX, true);
            isLoadNote = JsonUtil.getBoolean(annotObject, KEY_NOTE, true);
            isLoadStamp = JsonUtil.getBoolean(annotObject, KEY_STAMP, true);
            isLoadDrawDistance = JsonUtil.getBoolean(annotObject, KEY_DISTANCE, true);
            isLoadImage = JsonUtil.getBoolean(annotObject, KEY_IMAGE, true);
            isLoadAudio = JsonUtil.getBoolean(annotObject, KEY_AUDIO, true);
            isLoadVideo = JsonUtil.getBoolean(annotObject, KEY_VIDEO, true);

            initMap();
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void closeAnnotsConfig() {
        isLoadHighlight = false;
        isLoadUnderline = false;
        isLoadSquiggly = false;
        isLoadStrikeout = false;
        isLoadRedaction = false;
        isLoadInsertText = false;
        isLoadReplaceText = false;

        isLoadDrawLine = false;
        isLoadDrawSquare = false;
        isLoadDrawCircle = false;
        isLoadDrawArrow = false;
        isLoadDrawPencil = false;
        isLoadEraser = false;
        isLoadDrawPolygon = false;
        isLoadDrawCloud = false;
        isLoadDrawPolyLine = false;

        isLoadTypewriter = false;
        isLoadCallout = false;
        isLoadTextbox = false;
        isLoadNote = false;
        isLoadStamp = false;
//                isLoadFileattach = false;
        isLoadDrawDistance = false;
        isLoadImage = false;
        isLoadAudio = false;
        isLoadVideo = false;
    }

    private void initMap() {
        mMapSaveAnnotConfig.put(KEY_TEXTMARK_HIGHLIGHT, isLoadHighlight);
        mMapSaveAnnotConfig.put(KEY_TEXTMARK_UNDERLINE, isLoadUnderline);
        mMapSaveAnnotConfig.put(KEY_TEXTMARK_SQG, isLoadSquiggly);
        mMapSaveAnnotConfig.put(KEY_TEXTMARK_STO, isLoadStrikeout);
        mMapSaveAnnotConfig.put(KEY_TEXTMARK_REDACTION, isLoadRedaction);
        mMapSaveAnnotConfig.put(KEY_TEXTMARK_INSERT, isLoadInsertText);
        mMapSaveAnnotConfig.put(KEY_TEXTMARK_REPLACE, isLoadReplaceText);

        mMapSaveAnnotConfig.put(KEY_DRAWING_LINE, isLoadDrawLine);
        mMapSaveAnnotConfig.put(KEY_DRAWING_SQUARE, isLoadDrawSquare);
        mMapSaveAnnotConfig.put(KEY_DRAWING_CIRCLE, isLoadDrawCircle);
        mMapSaveAnnotConfig.put(KEY_DRAWING_ARROW, isLoadDrawArrow);
        mMapSaveAnnotConfig.put(KEY_DRAWING_PENCIL, isLoadDrawPencil);
        mMapSaveAnnotConfig.put(KEY_DRAWING_ERASER, isLoadEraser);
        mMapSaveAnnotConfig.put(KEY_DRAWING_POLYGON, isLoadDrawPolygon);
        mMapSaveAnnotConfig.put(KEY_DRAWING_CLOUD, isLoadDrawCloud);
        mMapSaveAnnotConfig.put(KEY_DRAWING_POLYLINE, isLoadDrawPolyLine);

        mMapSaveAnnotConfig.put(KEY_TYPWRITER, isLoadTypewriter);
        mMapSaveAnnotConfig.put(KEY_CALLOUT, isLoadCallout);
        mMapSaveAnnotConfig.put(KEY_TEXTBOX, isLoadTextbox);
        mMapSaveAnnotConfig.put(KEY_NOTE, isLoadNote);
        mMapSaveAnnotConfig.put(KEY_STAMP, isLoadStamp);
        mMapSaveAnnotConfig.put(KEY_DISTANCE, isLoadDrawDistance);
        mMapSaveAnnotConfig.put(KEY_IMAGE, isLoadImage);
        mMapSaveAnnotConfig.put(KEY_AUDIO, isLoadAudio);
        mMapSaveAnnotConfig.put(KEY_VIDEO, isLoadVideo);
    }

    public boolean isLoadHighlight() {
        return isLoadHighlight;
    }

    public boolean isLoadUnderline() {
        return isLoadUnderline;
    }

    public boolean isLoadSquiggly() {
        return isLoadSquiggly;
    }

    public boolean isLoadStrikeout() {
        return isLoadStrikeout;
    }

    public boolean isLoadRedaction() {
        return isLoadRedaction;
    }

    public boolean isLoadInsertText() {
        return isLoadInsertText;
    }

    public boolean isLoadReplaceText() {
        return isLoadReplaceText;
    }

    public boolean isLoadDrawLine() {
        return isLoadDrawLine;
    }

    public boolean isLoadDrawSquare() {
        return isLoadDrawSquare;
    }

    public boolean isLoadDrawCircle() {
        return isLoadDrawCircle;
    }

    public boolean isLoadDrawArrow() {
        return isLoadDrawArrow;
    }

    public boolean isLoadDrawDistance() {
        return isLoadDrawDistance;
    }

    public boolean isLoadDrawPencil() {
        return isLoadDrawPencil;
    }

    public boolean isLoadEraser() {
        return isLoadEraser;
    }

    public boolean isLoadDrawPolygon() {
        return isLoadDrawPolygon;
    }

    public boolean isLoadDrawCloud() {
        return isLoadDrawCloud;
    }

    public boolean isLoadDrawPolyLine() {
        return isLoadDrawPolyLine;
    }

    public boolean isLoadTypewriter() {
        return isLoadTypewriter;
    }

    public boolean isLoadCallout() {
        return isLoadCallout;
    }

    public boolean isLoadTextbox() {
        return isLoadTextbox;
    }

    public boolean isLoadNote() {
        return isLoadNote;
    }

    public boolean isLoadStamp() {
        return isLoadStamp;
    }

    public boolean isLoadFileattach() {
        return isLoadFileattach;
    }

    public boolean isLoadImage() {
        return isLoadImage;
    }

    public boolean isLoadAudio() {
        return isLoadAudio;
    }

    public boolean isLoadVideo() {
        return isLoadVideo;
    }

    public Map<String, Boolean> getAnnotConfigMap() {
        return mMapSaveAnnotConfig;
    }

    public void setLoadFileattach(boolean loadFileattach) {
        isLoadFileattach = loadFileattach;
    }

}
