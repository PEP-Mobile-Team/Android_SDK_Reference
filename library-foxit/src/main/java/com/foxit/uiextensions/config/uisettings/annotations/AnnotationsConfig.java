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
package com.foxit.uiextensions.config.uisettings.annotations;

import com.foxit.sdk.pdf.annots.Redact;
import com.foxit.uiextensions.config.uisettings.annotations.annots.ArrowConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.AttachmentConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.BaseConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.CalloutConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.CloudConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.DistanceConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.HighlightConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.ImageConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.InsertConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.LineConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.NoteConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.OvalConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.PencilConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.PolygonConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.PolylineConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.RectangleConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.RedactConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.ReplaceConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.SquigglyConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.StrikeoutConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.TextboxConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.TypewriterConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.UnderlineConfig;
import com.foxit.uiextensions.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class AnnotationsConfig {
    public static final String KEY_UISETTING_ANNOTATIONS = "annotations";

    private static final String KEY_CONTINUOUSLY_ADD = "continuouslyAdd";
    private static final boolean DEFAULT_CONTINUOUSLY_ADD = false;

    public boolean continuouslyAdd = DEFAULT_CONTINUOUSLY_ADD;

    public HighlightConfig highlight = new HighlightConfig();
    public UnderlineConfig underline = new UnderlineConfig();
    public SquigglyConfig squiggly = new SquigglyConfig();
    public StrikeoutConfig strikeout = new StrikeoutConfig();
    public InsertConfig insert = new InsertConfig();
    public ReplaceConfig replace = new ReplaceConfig();
    public RedactConfig redaction = new RedactConfig();

    public LineConfig line = new LineConfig();
    public RectangleConfig rectangle = new RectangleConfig();
    public OvalConfig oval = new OvalConfig();
    public ArrowConfig arrow = new ArrowConfig();
    public PencilConfig pencil = new PencilConfig();
    public PolygonConfig polygon = new PolygonConfig();
    public CloudConfig cloud = new CloudConfig();
    public PolylineConfig polyline = new PolylineConfig();

    public TypewriterConfig typewriter = new TypewriterConfig();
    public TextboxConfig textbox = new TextboxConfig();
    public CalloutConfig callout = new CalloutConfig();
    public NoteConfig note = new NoteConfig();
    public AttachmentConfig attachment = new AttachmentConfig();
    public ImageConfig image = new ImageConfig();
    public DistanceConfig distance = new DistanceConfig();

    public void parseConfig(JSONObject object) {
        try {
            JSONObject jsonObject = object.getJSONObject(KEY_UISETTING_ANNOTATIONS);
            //continuouslyAdd
            continuouslyAdd = JsonUtil.getBoolean(jsonObject, KEY_CONTINUOUSLY_ADD, DEFAULT_CONTINUOUSLY_ADD);
            //hightlight
            if (jsonObject.has(BaseConfig.KEY_TEXTMARK_HIGHLIGHT)) {
                highlight.parseConfig(jsonObject, BaseConfig.KEY_TEXTMARK_HIGHLIGHT);
            }
            //underline
            if (jsonObject.has(BaseConfig.KEY_TEXTMARK_UNDERLINE)) {
                underline.parseConfig(jsonObject, BaseConfig.KEY_TEXTMARK_UNDERLINE);
            }
            //squiggly
            if (jsonObject.has(BaseConfig.KEY_TEXTMARK_SQG)) {
                squiggly.parseConfig(jsonObject, BaseConfig.KEY_TEXTMARK_SQG);
            }
            //strikeout
            if (jsonObject.has(BaseConfig.KEY_TEXTMARK_STO)) {
                strikeout.parseConfig(jsonObject, BaseConfig.KEY_TEXTMARK_STO);
            }
            //insert
            if (jsonObject.has(BaseConfig.KEY_TEXTMARK_INSERT)) {
                insert.parseConfig(jsonObject, BaseConfig.KEY_TEXTMARK_INSERT);
            }
            //replace
            if (jsonObject.has(BaseConfig.KEY_TEXTMARK_REPLACE)) {
                replace.parseConfig(jsonObject, BaseConfig.KEY_TEXTMARK_REPLACE);
            }
            //redact
            if (jsonObject.has(BaseConfig.KEY_TEXTMARK_REDACT)) {
                redaction.parseConfig(jsonObject);
            }

            //line
            if (jsonObject.has(BaseConfig.KEY_DRAWING_LINE)) {
                line.parseConfig(jsonObject, BaseConfig.KEY_DRAWING_LINE);
            }
            //rectangle
            if (jsonObject.has(BaseConfig.KEY_DRAWING_SQUARE)) {
                rectangle.parseConfig(jsonObject, BaseConfig.KEY_DRAWING_SQUARE);
            }
            //oval
            if (jsonObject.has(BaseConfig.KEY_DRAWING_CIRCLE)) {
                oval.parseConfig(jsonObject, BaseConfig.KEY_DRAWING_CIRCLE);
            }
            //arrow
            if (jsonObject.has(BaseConfig.KEY_DRAWING_ARROW)) {
                arrow.parseConfig(jsonObject, BaseConfig.KEY_DRAWING_ARROW);
            }
            //pencil
            if (jsonObject.has(BaseConfig.KEY_DRAWING_PENCIL)) {
                pencil.parseConfig(jsonObject, BaseConfig.KEY_DRAWING_PENCIL);
            }
            //polygon
            if (jsonObject.has(BaseConfig.KEY_DRAWING_POLYGON)) {
                polygon.parseConfig(jsonObject, BaseConfig.KEY_DRAWING_POLYGON);
            }
            //cloud
            if (jsonObject.has(BaseConfig.KEY_DRAWING_CLOUD)) {
                cloud.parseConfig(jsonObject, BaseConfig.KEY_DRAWING_CLOUD);
            }
            //polyline
            if (jsonObject.has(BaseConfig.KEY_DRAWING_POLYLINE)) {
                polyline.parseConfig(jsonObject, BaseConfig.KEY_DRAWING_POLYLINE);
            }

            //typewriter
            if (jsonObject.has(BaseConfig.KEY_TYPWRITER)) {
                typewriter.parseConfig(jsonObject);
            }
            //callout
            if (jsonObject.has(BaseConfig.KEY_CALLOUT)) {
                callout.parseConfig(jsonObject);
            }
            //textbox
            if (jsonObject.has(BaseConfig.KEY_TEXTBOX)) {
                textbox.parseConfig(jsonObject);
            }
            //note
            if (jsonObject.has(BaseConfig.KEY_NOTE)) {
                note.parseConfig(jsonObject);
            }
            //attachment
            if (jsonObject.has(BaseConfig.KEY_FILEATTACHMENT)) {
                attachment.parseConfig(jsonObject);
            }
            //image
            if (jsonObject.has(BaseConfig.KEY_IMAGE)) {
                image.parseConfig(jsonObject, BaseConfig.KEY_IMAGE);
            }
            //distance
            if (jsonObject.has(BaseConfig.KEY_DISTANCE)) {
                distance.parseConfig(jsonObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
