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
package com.foxit.uiextensions.utils;

import android.content.Context;
import android.graphics.DashPathEffect;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.common.Library;
import com.foxit.sdk.common.fxcrt.Matrix2D;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.Rendition;
import com.foxit.sdk.pdf.Signature;
import com.foxit.sdk.pdf.actions.Action;
import com.foxit.sdk.pdf.actions.RenditionAction;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.sdk.pdf.annots.Line;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.sdk.pdf.annots.Note;
import com.foxit.sdk.pdf.annots.Redact;
import com.foxit.sdk.pdf.annots.Screen;
import com.foxit.sdk.pdf.annots.Sound;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.Control;
import com.foxit.sdk.pdf.interform.Field;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.line.LineConstants;
import com.foxit.uiextensions.annots.multiselect.GroupManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class AppAnnotUtil {
    public static float ANNOT_SELECT_TOLERANCE = 10.0f;
    private static AppAnnotUtil mAppAnnotUtil = null;
    private Context mContext;

    public static AppAnnotUtil getInstance(Context context) {
        if (mAppAnnotUtil == null) {
            mAppAnnotUtil = new AppAnnotUtil(context);
        }
        return mAppAnnotUtil;
    }

    private AppDisplay mDisplay;

    public AppAnnotUtil(Context context) {
        mContext = context;
        mDisplay = AppDisplay.getInstance(context);
    }

    public static PathEffect getAnnotBBoxPathEffect() {
        return new DashPathEffect(new float[]{6, 2}, 0);
    }


    public static int getAnnotBBoxSpace() {
        return 5;
    }

    public float getAnnotBBoxStrokeWidth() {
        return mDisplay.dp2px(1.0f);
    }

    private static PathEffect mPathEffect;

    public static PathEffect getBBoxPathEffect2() {
        if (mPathEffect == null) {
            mPathEffect = new DashPathEffect(new float[]{6.0f, 6.0f}, 0);
        }
        return mPathEffect;
    }

    public static void toastAnnotCopy(Context context) {
        UIToast.getInstance(context).show(context.getApplicationContext().getString(R.string.fm_annot_copy));
    }


    private static final List<String> TYPES = Collections.unmodifiableList(Arrays.asList("Highlight", "Text", "StrikeOut", "Underline",
            "Squiggly", "Circle", "Square", "FreeTextTypewriter",
            "Stamp", "Caret", "Replace", "Ink",
            "Line", "LineArrow", "FileAttachment", "TextBox",
            "LineDimension", "Image", "Polygon", "PolygonCloud",
            "FreeTextCallout", "PolyLine", "Audio", "Video", "Redaction", "Sound"));

    private static final List<Integer> IDS = Collections.unmodifiableList(Arrays.asList(// 1.Highlight
            R.drawable.rv_panel_annot_highlight_type,
            // 2.Text
            R.drawable.rv_panel_annot_text_type,
            // 3.StrikeOut
            R.drawable.rv_panel_annot_strikeout_type,
            // 4.UnderLine
            R.drawable.rv_panel_annot_underline_type,
            // 5.Squiggly
            R.drawable.rv_panel_annot_squiggly_type,
            // 6.Circle
            R.drawable.rv_panel_annot_circle_type,
            // 7.Square
            R.drawable.rv_panel_annot_square_type,
            // 8.Typewriter
            R.drawable.rv_panel_annot_typewriter_type,
            // 9.Stamp
            R.drawable.rv_panel_annot_stamp_type,
            // 10.Insert Text
            R.drawable.rv_panel_annot_caret_type,
            // 11.Replace
            R.drawable.rv_panel_annot_replace_type,
            //12.Ink(pencil)
            R.drawable.rv_panel_annot_ink_type,
            //13.Line
            R.drawable.rv_panel_annot_line_type,
            //14.Arrow
            R.drawable.rv_panel_annot_arrow_type,
            //15.FileAttachment
            R.drawable.rv_panel_annot_accthment_type,
            //16.FreeText - TextBox
            R.drawable.annot_textbox_pressed,
            //17. LineDimension(Distance)
            R.drawable.icon_annot_distance_panel_item,
            //18.Screen - Image
            R.drawable.rv_panel_annot_screen_type,
            //19.Polygon
            R.drawable.rv_panel_annot_polygon_type,
            //20.Polygon cloud
            R.drawable.rv_panel_annot_polygoncloud_type,
            //21. FreeText-Callout
            R.drawable.rv_panel_annot_callout_type,
            //22.PolyLine
            R.drawable.rv_panel_annot_polyline_type,
            //23.Screen - audio
            R.drawable.rv_panel_annot_audio_type,
            //24.Screen - video
            R.drawable.rv_panel_annot_video_type,
            //25.Redact
            R.drawable.rv_panel_annot_redact_type,
            //26.Sound
            R.drawable.rv_panel_annot_sound_type));

    public static boolean isSupportReply(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (annot == null || annot.isEmpty())
            return false;

        if (GroupManager.getInstance().isGrouped(pdfViewCtrl, annot))
            return GroupManager.getInstance().canReply(pdfViewCtrl, annot);
        else
            return isAnnotSupportReply(annot) && !AppAnnotUtil.isReadOnly(annot);
    }

    public static boolean isAnnotSupportReply(Annot annot) {
        try {
            switch (annot.getType()) {
                case Annot.e_Note: {
                    Note note = (Note) annot;
                    if (note.isStateAnnot())
                        return false;
                }
                case Annot.e_Highlight:
                case Annot.e_Underline:
                case Annot.e_Squiggly:
                case Annot.e_StrikeOut:
                case Annot.e_Circle:
                case Annot.e_Square:
//                case Annot.e_Screen:
//                    return !isSupportGroupElement(annot);
                case Annot.e_Stamp:
                case Annot.e_Caret:
                case Annot.e_Line:
                case Annot.e_Ink:
                case Annot.e_Polygon:
                case Annot.e_PolyLine:
                case Annot.e_Redact:
                    return true;
                default:
                    return false;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isSupportComment(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (annot == null || annot.isEmpty())
            return false;

        if (GroupManager.getInstance().isGrouped(pdfViewCtrl, annot)) {
            return false;
        } else {
            return !isLocked(annot) && !isReadOnly(annot);
        }
    }

    public static String getTypeString(Annot annot) {
        try {
            switch (annot.getType()) {
                case Annot.e_Note:
                    return "Text";
                case Annot.e_Link:
                    return "Link";
                case Annot.e_FreeText: {
                    String intent = ((FreeText) annot).getIntent();
                    intent = intent == null ? "TextBox" : intent;
                    return intent;
                }
                case Annot.e_Line: {
                    String intent = ((Line) annot).getIntent();
                    if ("LineArrow".equals(intent))
                        return "LineArrow";
                    if (LineConstants.INTENT_LINE_DIMENSION.equals(intent))
                        return "LineDimension";
                    return "Line";
                }
                case Annot.e_Square:
                    return "Square";
                case Annot.e_Circle:
                    return "Circle";
                case Annot.e_Polygon:
                    BorderInfo borderInfo = annot.getBorderInfo();
                    if (borderInfo != null && borderInfo.getStyle() == BorderInfo.e_Cloudy) {
                        return "PolygonCloud";
                    }
                    return "Polygon"; //"PolyLineDimension"
                case Annot.e_PolyLine:
                    return "PolyLine";
                case Annot.e_Highlight:
                    return "Highlight";
                case Annot.e_Underline:
                    return "Underline";
                case Annot.e_Squiggly:
                    return "Squiggly";
                case Annot.e_StrikeOut:
                    return "StrikeOut";
                case Annot.e_Stamp:
                    return "Stamp";
                case Annot.e_Caret:
                    return isReplaceCaret(annot) ? "Replace" : "Caret";
                case Annot.e_Ink:
                    return "Ink";
                case Annot.e_PSInk:
                    return "PSInk";
                case Annot.e_FileAttachment:
                    return "FileAttachment";
                case Annot.e_Sound:
                    return "Sound";
                case Annot.e_Movie:
                    return "Movie";
                case Annot.e_Widget:
                    return "Widget";
                case Annot.e_Screen:
                    return getScreenTypeString(annot);
                case Annot.e_PrinterMark:
                    return "PrinterMark";
                case Annot.e_TrapNet:
                    return "TrapNet";
                case Annot.e_Watermark:
                    return "Watermark";
                case Annot.e_3D:
                    return "3D";
                case Annot.e_Redact:
                    return "Redaction";
                default:
                    return "Unknown";
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public static String getScreenTypeString(Annot annot) {
        try {
            Screen screen = (Screen) annot;
            Action action = screen.getAction();
            if (action.isEmpty()) {
                return "Image";
            } else {
                if (Action.e_TypeRendition != action.getType()) {
                    return "Image";
                }

                // Note: the action have sub action,but at the moment we only judge the first level.
                RenditionAction renditionAction = new RenditionAction(action);
                if (renditionAction.isEmpty() || renditionAction.getRenditionCount() == 0) {
                    return "Image";
                } else {
                    Rendition rendition = renditionAction.getRendition(0);
                    if (rendition.isEmpty()) {
                        return "Image";
                    } else {
                        String mimeType = rendition.getMediaClipContentType();
                        if (mimeType == null || mimeType.isEmpty()) {
                            return "Image";
                        } else if (mimeType.toLowerCase().contains("audio")) {
                            return "Audio";
                        } else if (mimeType.toLowerCase().contains("video")
                                || mimeType.contains("application/x-shockwave-flash")
                                || mimeType.contains("application/futuresplash")) {
                            return "Video";
                        } else {
                            return "Image";
                        }
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public static String getTypeToolName(Annot annot) {
        try {
            switch (annot.getType()) {
                case Annot.e_Line: {
                    String intent = ((Line) annot).getIntent();
                    if ("LineArrow".equals(intent))
                        return ToolHandler.TH_TYPE_ARROW;
                    if ("LineDimension".equals(intent))
                        return ToolHandler.TH_TYPE_DISTANCE;
                    return ToolHandler.TH_TYPE_LINE;
                }
                case Annot.e_Polygon:
                    BorderInfo borderInfo = annot.getBorderInfo();
                    if (borderInfo != null && borderInfo.getStyle() == BorderInfo.e_Cloudy) {
                        return ToolHandler.TH_TYPE_POLYGONCLOUD;
                    }
                    return ToolHandler.TH_TYPE_POLYGON;
                case Annot.e_Caret:
                    return isReplaceCaret(annot) ? ToolHandler.TH_TYPE_REPLACE : ToolHandler.TH_TYPR_INSERTTEXT;
                case Annot.e_Screen:
                    String typeString = getScreenTypeString(annot);
                    if ("Image".equals(typeString)) {
                        return ToolHandler.TH_TYPE_PDFIMAGE;
                    } else if ("Audio".equals(typeString)) {
                        return ToolHandler.TH_TYPE_SCREEN_AUDIO;
                    } else {
                        return ToolHandler.TH_TYPE_SCREEN_VIDEO;
                    }
                default:
                    return "Unknown";
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public static boolean contentsModifiable(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (annot == null || annot.isEmpty())
            return false;

        if (GroupManager.getInstance().isGrouped(pdfViewCtrl, annot))
            return GroupManager.getInstance().contentsModifiable(pdfViewCtrl, annot);
        else
            return contentsModifiable(getTypeString(annot));
    }

    public static boolean contentsModifiable(String type) {
        return "Text".equals(type)
                || "Line".equals(type)
                || "LineArrow".equals(type)
                || "LineDimension".equals(type)
                || "Square".equals(type)
                || "Circle".equals(type)
                || "Highlight".equals(type)
                || "Underline".equals(type)
                || "Squiggly".equals(type)
                || "StrikeOut".equals(type)
                || "Stamp".equals(type)
                || "Caret".equals(type)
                || "Replace".equals(type)
                || "Ink".equals(type)
//                || "Screen".equals(type)
                || "Polygon".equals(type)
                || "PolygonCloud".equals(type)
                || "PolyLine".equals(type)
                || "Redaction".equals(type);
    }

    public static Annot getAnnot(PDFPage page, String UID) {
        if (page == null) return null;
        try {
            long nCount = page.getAnnotCount();
            for (int i = 0; i < nCount; i++) {
                Annot annot = AppAnnotUtil.createAnnot(page.getAnnot(i));
                if (annot != null) {
                    if (AppUtil.isEmpty(annot.getUniqueID())) {
                        if (annot.getDict() != null && String.valueOf(annot.getDict().getObjNum()).compareTo(UID) == 0) {
                            return annot;
                        }
                    } else if (annot.getUniqueID().compareTo(UID) == 0) {
                        return annot;
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Control getControlAtPos(PDFPage page, PointF point, float tolerance) throws PDFException {
        Annot annot = AppAnnotUtil.createAnnot(page.getAnnotAtPoint(AppUtil.toFxPointF(point), tolerance));
        if (annot != null && annot.getType() == Annot.e_Widget) {
            return ((Widget) annot).getControl();
        }
        return null;
    }


    public static Signature getSignatureAtPos(PDFPage page, PointF point, float tolerance) throws PDFException {
        Annot annot = AppAnnotUtil.createAnnot(page.getAnnotAtPoint(AppUtil.toFxPointF(point), tolerance));
        if (annot != null && annot.getType() == Annot.e_Widget) {
            Field field = ((Widget) annot).getField();
            if (field != null && field.getType() == Field.e_TypeSignature)
                return (Signature) field;
        }
        return null;
    }

    public static boolean isSameAnnot(Annot annot, Annot comparedAnnot) {
        boolean ret = false;
        try {
            long objNumA = 0;
            if (annot != null && !annot.isEmpty())
                objNumA = annot.getDict().getObjNum();
            long objNumB = 0;
            if (comparedAnnot != null && !comparedAnnot.isEmpty())
                objNumB = comparedAnnot.getDict().getObjNum();
            if (objNumA == objNumB)
                ret = true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return ret;
    }

    // if the annot in a support group by rdk and the annot is not the header of group  return true, otherwise return false.
    public static boolean isSupportGroupElement(Annot annot) {
        if (!isSupportGroup(annot))
            return false;
        try {
            return !isSameAnnot(annot, ((Markup) annot).getGroupHeader());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isSupportGroup(Annot annot) {
        if (annot == null || annot.isEmpty()) return false;
        try {
            if (!annot.isMarkup() || !((Markup) annot).isGrouped()) return false;
            Markup head = ((Markup) annot).getGroupHeader();

            //now just support replace annot (Caret, StikeOut)
            switch (head.getType()) {
                case Annot.e_Caret:
                    return isReplaceCaret(head);
                default:
                    return false;
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isGrouped(Annot annot) {
        if (annot == null || annot.isEmpty()) return false;
        try {
            return annot.isMarkup() && ((Markup) annot).isGrouped();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isSupportAnnotGroup(Annot annot) {
        int annotType = Annot.e_UnknownType;
        try {
            annotType = annot.getType();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return Annot.e_Note == annotType
                || Annot.e_FileAttachment == annotType
                || Annot.e_Stamp == annotType
                || Annot.e_FreeText == annotType
                || Annot.e_Line == annotType
                || Annot.e_Square == annotType
                || Annot.e_Circle == annotType
                || Annot.e_Polygon == annotType
                || Annot.e_PolyLine == annotType
                || Annot.e_Ink == annotType;
    }

    public static boolean isReplaceCaret(Annot annot) {
        try {
            if (annot == null || annot.getType() != Annot.e_Caret || !((Markup) annot).isGrouped())
                return false;
            Caret caret = (Caret) AppAnnotUtil.createAnnot(annot, Annot.e_Caret);
            Markup head = caret.getGroupHeader();
            MarkupArray markupArray = head.getGroupElements();
            if (head.getType() != Annot.e_Caret || markupArray.getSize() != 2 || !isSameAnnot(head, caret))
                return false;
            for (int i = 0; i < 2; i++) {
                Markup markup = markupArray.getAt(i);//caret.getGroupElement(i);
                if (markup.getType() == Annot.e_StrikeOut) {
                    return true;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static StrikeOut getStrikeOutFromCaret(@NonNull Caret caret) {
        if (caret.isEmpty()) return null;
        try {
            MarkupArray markupArray = caret.getGroupElements();
            long nCount = markupArray.getSize();
            for (int i = 0; i < nCount; i++) {
                Markup groupAnnot = markupArray.getAt(i);
                if (groupAnnot.getType() == Annot.e_StrikeOut)
                    return (StrikeOut) AppAnnotUtil.createAnnot(groupAnnot, Annot.e_StrikeOut);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Annot getReplyToAnnot(Annot annot) {
        if (annot == null || annot.isEmpty())
            return null;
        try {
            if (annot.getType() == Annot.e_Note)
                return ((Note) annot).getReplyTo();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static PointF getPageViewPoint(PDFViewCtrl pdfViewCtrl, int pageIndex, MotionEvent motionEvent) {
        PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF point = new PointF();
        pdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, point, pageIndex);
        return point;
    }

    public static PointF getPdfPoint(PDFViewCtrl pdfViewCtrl, int pageIndex, MotionEvent motionEvent) {
        PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF pageViewPt = new PointF();
        pdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, pageViewPt, pageIndex);
        PointF point = new PointF();
        pdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, point, pageIndex);
        return point;
    }

    public static int getIconId(String type) {
        int index = TYPES.indexOf(type);
        if (index != -1) {
            return IDS.get(index);
        }
        return R.drawable.rv_panel_annot_not_edit_type;
    }

    public static boolean isSupportDelete(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if (annot == null || annot.isEmpty())
            return false;

        if (GroupManager.getInstance().isGrouped(pdfViewCtrl, annot))
            return GroupManager.getInstance().canDelete(pdfViewCtrl, annot);
        else
            return isSupportDeleteAnnot(annot) && !(isLocked(annot) || isReadOnly(annot));
    }

    public static boolean isSupportDeleteAnnot(Annot annot) {
        try {
            switch (annot.getType()) {
                case Annot.e_Sound:
                    return false;
                default:
                    return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean isSupportEditAnnot(Annot annot) {
        if (annot == null || annot.isEmpty())
            return false;
        try {
            switch (annot.getType()) {
                case Annot.e_Note: {
                    Note note = (Note) annot;
                    if (note.isStateAnnot())
                        return false;
                }

                case Annot.e_Highlight:
                case Annot.e_Underline:
                case Annot.e_Squiggly:
                case Annot.e_StrikeOut:
                case Annot.e_Circle:
                case Annot.e_Square:
                    return !isSupportGroupElement(annot);
                case Annot.e_FreeText:
//                    String intent = ((Markup)annot).getIntent();
//                    return intent == null || "FreeTextTypewriter".equals(intent);
                case Annot.e_Stamp:
                case Annot.e_Caret:
                case Annot.e_Line:
                case Annot.e_Ink:
                case Annot.e_FileAttachment:
                case Annot.e_Screen:
                case Annot.e_Polygon:
                case Annot.e_Redact:
                case Annot.e_PolyLine:
                case Annot.e_Sound:
                    return !isSupportGroupElement(annot);
                default:
                    return false;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Toast mAnnotToast;

    /**
     * Only for annot continue create toast
     */
    public void showAnnotContinueCreateToast(boolean isContinuousCreate) {
        if (mAnnotToast == null) {
            initAnnotToast();
        }
        if (mAnnotToast == null) {
            return;
        }
        String str;
        if (isContinuousCreate) {
            str = AppResource.getString(mContext.getApplicationContext(), R.string.annot_continue_create);
        } else {
            str = AppResource.getString(mContext.getApplicationContext(), R.string.annot_single_create);
        }
        TextView tv = (TextView) mAnnotToast.getView().findViewById(R.id.annot_continue_create_toast_tv);
        int yOffset;
        if (mDisplay.isPad()) {
            yOffset = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_pad) + mDisplay.dp2px(16) * (2 + 1);
        } else {
            yOffset = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_phone) + mDisplay.dp2px(16) * (2 + 1);
        }
        mAnnotToast.setGravity(Gravity.BOTTOM, 0, yOffset);
        tv.setText(str);
        mAnnotToast.show();
    }

    private void initAnnotToast() {
        try {
            mAnnotToast = new Toast(mContext);
            LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View toastlayout = inflate.inflate(R.layout.annot_continue_create_tips, null);
            mAnnotToast.setView(toastlayout);
            mAnnotToast.setDuration(Toast.LENGTH_SHORT);
            int yOffset;
            if (mDisplay.isPad()) {
                yOffset = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_pad) + mDisplay.dp2px(16) * (2 + 1);
            } else {
                yOffset = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_phone) + mDisplay.dp2px(16) * (2 + 1);
            }
            mAnnotToast.setGravity(Gravity.BOTTOM, 0, yOffset);
        } catch (Exception e) {
            mAnnotToast = null;
        }
    }

    public static int getAnnotHandlerType(Annot annot) {
        if (annot == null || annot.isEmpty()) return Annot.e_UnknownType;
        int type = Annot.e_UnknownType;
        try {
            type = annot.getType();
            if (type == Annot.e_FreeText) {
                if (((FreeText) annot).getIntent() == null) {
                    type = AnnotHandler.TYPE_FREETEXT_TEXTBOX; // text box;
                } else if (((FreeText) annot).getIntent().equalsIgnoreCase("FreeTextCallout")) {
                    type = AnnotHandler.TYPE_FREETEXT_CALLOUT; // FreeTextCallout annot handler type
                }
            } else if (type == Annot.e_Widget) {
                Field field = ((Widget) annot).getField();
                if (field != null) {
                    int ft = field.getType();
                    if (ft == Field.e_TypeSignature) {
                        type = AnnotHandler.TYPE_FORMFIELD_SIGNATURE;//signature handle type
                    }
                }
            } else if (type == Annot.e_Screen) {

                String typeString = getScreenTypeString(annot);
                if ("Image".equals(typeString)) {
                    type = AnnotHandler.TYPE_SCREEN_IMAGE; // Screen -Image;
                } else {
                    type = AnnotHandler.TYPE_SCREEN_MULTIMEDIA; // Screen - Multimedia
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return type;
    }

    public static Annot createAnnot(Annot annot) {
        if (annot == null || annot.isEmpty()) return null;
        try {
            return createAnnot(annot, annot.getType());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Annot createAnnot(Annot annot, int type) {
        if (annot == null || annot.isEmpty()) return null;
        Annot object = null;
        switch (type) {
            case Annot.e_Note:
                object = new com.foxit.sdk.pdf.annots.Note(annot);
                break;
            case Annot.e_Highlight:
                object = new com.foxit.sdk.pdf.annots.Highlight(annot);
                break;
            case Annot.e_Underline:
                object = new com.foxit.sdk.pdf.annots.Underline(annot);
                break;
            case Annot.e_StrikeOut:
                object = new com.foxit.sdk.pdf.annots.StrikeOut(annot);
                break;
            case Annot.e_Squiggly:
                object = new com.foxit.sdk.pdf.annots.Squiggly(annot);
                break;
            case Annot.e_Link:
                object = new com.foxit.sdk.pdf.annots.Link(annot);
                break;
            case Annot.e_Circle:
                object = new com.foxit.sdk.pdf.annots.Circle(annot);
                break;
            case Annot.e_Square:
                object = new com.foxit.sdk.pdf.annots.Square(annot);
                break;
            case Annot.e_FreeText:
                object = new com.foxit.sdk.pdf.annots.FreeText(annot);
                break;
            case Annot.e_Line:
                object = new com.foxit.sdk.pdf.annots.Line(annot);
                break;
            case Annot.e_Ink:
                object = new com.foxit.sdk.pdf.annots.Ink(annot);
                break;
            case Annot.e_Caret:
                object = new com.foxit.sdk.pdf.annots.Caret(annot);
                break;
            case Annot.e_Polygon:
                object = new com.foxit.sdk.pdf.annots.Polygon(annot);
                break;
            case Annot.e_PolyLine:
                object = new com.foxit.sdk.pdf.annots.PolyLine(annot);
                break;
            case Annot.e_Stamp:
                object = new com.foxit.sdk.pdf.annots.Stamp(annot);
                break;
            case Annot.e_Popup:
                object = new com.foxit.sdk.pdf.annots.Popup(annot);
                break;
            case Annot.e_PSInk:
                object = new com.foxit.sdk.pdf.annots.PSInk(annot);
                break;
            case Annot.e_FileAttachment:
                object = new com.foxit.sdk.pdf.annots.FileAttachment(annot);
                break;
            case Annot.e_Widget:
                object = new Widget(annot);
                break;
            case Annot.e_Screen:
                object = new Screen(annot);
                break;
            case Annot.e_Redact:
                object = new Redact(annot);
                break;
            case Annot.e_Sound:
                object = new Sound(annot);
                break;
            default:
                try {
                    if (annot.isMarkup())
                        object = new Markup(annot);
                } catch (PDFException e) {
                    e.printStackTrace();
                }
                break;
        }
        return object;
    }

    public static boolean equals(Annot annot, Annot other) {

        try {
            if (annot == null || annot.isEmpty() || other == null || other.isEmpty()) return false;
            if (annot.getIndex() == other.getIndex() && annot.getPage().getIndex() == other.getPage().getIndex()) {
                return true;
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isLocked(Annot annot) {
        try {
            if (annot == null || annot.isEmpty()) return false;
            return (annot.getFlags() & Annot.e_FlagLocked) != 0;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isReadOnly(Annot annot) {
        try {
            if (annot == null || annot.isEmpty()) return false;
            return (annot.getFlags() & Annot.e_FlagReadOnly) != 0;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getAnnotJsonModuleName(Annot annot) {
        String name = "";
        try {
            switch (annot.getType()) {
                case Annot.e_Note:
                    name = "note";
                    break;
                case Annot.e_Highlight:
                    name = "highlight";
                    break;
                case Annot.e_Underline:
                    name = "underline";
                    break;
                case Annot.e_StrikeOut:
                    name = "strikeout";
                    break;
                case Annot.e_Squiggly:
                    name = "squiggly";
                    break;
                case Annot.e_Link:
//                    name = "link";
                    break;
                case Annot.e_Circle:
                    name = "oval";
                    break;
                case Annot.e_Square:
                    name = "rectangle";
                    break;
                case Annot.e_FreeText: {
                    String intent = ((FreeText) annot).getIntent();
                    if (intent == null) {
                        name = "textbox";
                    } else if (intent.equalsIgnoreCase("FreeTextCallout")) {
                        name = "callout";
                    } else if (intent.equalsIgnoreCase("FreeTextTypewriter")) {
                        name = "typewriter";
                    }
                }
                break;
                case Annot.e_Line: {
                    String intent = ((Line) annot).getIntent();
                    if ("LineArrow".equals(intent)) {
                        name = "arrow";
                    } else if ("LineDimension".equals(intent)) {
                        name = "distance";
                    } else {
                        name = "line";
                    }
                }
                break;
                case Annot.e_Ink:
                    name = "pencil";
                    break;
                case Annot.e_Caret:
                    if (isReplaceCaret(annot)) {
                        name = "replace";
                    } else {
                        name = "insert";
                    }
                    break;
                case Annot.e_Polygon: {
                    BorderInfo borderInfo = annot.getBorderInfo();
                    if (borderInfo != null && borderInfo.getStyle() == BorderInfo.e_Cloudy) {
                        name = "cloud";
                    } else {
                        name = "polygon";
                    }
                }
                break;
                case Annot.e_PolyLine:
                    name = "polyline";
                    break;
                case Annot.e_Stamp:
                    name = "stamp";
                    break;
                case Annot.e_Popup:
                    break;
                case Annot.e_PSInk:
                    break;
                case Annot.e_FileAttachment:
                    name = "fileattachment";
                    break;
                case Annot.e_Widget:
                    break;
                case Annot.e_Screen: {
                    String type = getScreenTypeString(annot);
                    if (type.equalsIgnoreCase("Image")) {
                        name = "image";
                    } else if (type.equalsIgnoreCase("Audio")) {
                        name = "audio";
                    } else if (type.equalsIgnoreCase("Video")) {
                        name = "video";
                    }
                }
                break;
                case Annot.e_Redact:
                    name = "redaction";
                    break;
                case Annot.e_Sound:
                    name = "sound";
                    break;
                default:
                    break;
            }
        } catch (PDFException e) {
        }
        return name;
    }

    public static void convertPageViewRectToPdfRect(PDFViewCtrl pdfViewCtrl, Annot annot, RectF pvRect, RectF pdfRect) {
        try {
            com.foxit.sdk.common.fxcrt.RectF _pdfRect = AppUtil.toFxRectF(pvRect);
            PDFPage pdfPage = annot.getPage();
            int pageIndex = pdfPage.getIndex();
            Matrix2D matrix2D = annot.getDisplayMatrix(AppUtil.toMatrix2D(pdfViewCtrl.getDisplayMatrix(pageIndex)));
            Matrix2D pdfMatrix = new Matrix2D();
            pdfMatrix.setReverse(matrix2D);
            pdfMatrix.transformRect(_pdfRect);
            pdfRect.set(AppUtil.toRectF(_pdfRect));
            int flag = annot.getFlags();
//          if ((flag & Annot.e_FlagNoRotate) != 0 || (flag & Annot.e_FlagNoZoom) != 0) {
            RectF _pvRect = new RectF(pvRect);
            pdfViewCtrl.convertPageViewRectToPdfRect(_pvRect, _pvRect, pageIndex);
            int rotation = (pdfViewCtrl.getViewRotation() + pdfPage.getRotation()) % 4;
            float width = Math.abs(pdfRect.width());
            float height = Math.abs(pdfRect.height());
            if ((flag & Annot.e_FlagNoZoom) != 0) {
                RectF annotRect = AppUtil.toRectF(annot.getRect());
                width = Math.abs(annotRect.width());
                height = Math.abs(annotRect.height());
            }

            switch (rotation) {
                case com.foxit.sdk.common.Constants.e_Rotation0:
                    pdfRect.set(_pvRect.left, _pvRect.top, _pvRect.left + width, _pvRect.top - height);
                    break;
                case com.foxit.sdk.common.Constants.e_Rotation90:
                    pdfRect.set(_pvRect.left, _pvRect.bottom, _pvRect.left + width, _pvRect.bottom - height);
                    break;
                case com.foxit.sdk.common.Constants.e_Rotation180:
                    pdfRect.set(_pvRect.right, _pvRect.bottom, _pvRect.right + width, _pvRect.bottom - height);
                    break;
                case com.foxit.sdk.common.Constants.e_Rotation270:
                    pdfRect.set(_pvRect.right, _pvRect.top, _pvRect.right + width, _pvRect.top - height);
                    break;
                default:
            }
//          }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasModuleLicenseRight(int module) {
        try {
            return Library.hasModuleLicenseRight(module);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static ArrayList<Annot> getAnnotsByNMs(PDFPage page, ArrayList<String> nms) {
        ArrayList<Annot> annots = new ArrayList<>();
        for (int i = 0; i < nms.size(); i++) {
            Annot annot = getAnnot(page, nms.get(i));
            if (annot == null || annot.isEmpty())
                continue;
            annots.add(annot);
        }
        return annots;
    }

    public static String getAnnotUniqueID(Annot annot) {
        if (annot == null || annot.isEmpty())
            return "";

        try {
            String uniqueID = annot.getUniqueID();
            if (AppUtil.isEmpty(uniqueID)) {
                if (annot.getDict() != null) {
                    int objNum = annot.getDict().getObjNum();
                    uniqueID = String.valueOf(objNum);
                } else {
                    uniqueID = AppDmUtil.randomUUID(null);
                    annot.setUniqueID(uniqueID);
                }
            }
            return uniqueID;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static int getStandard14Font(DefaultAppearance da, PDFDoc doc) {
        int id = Font.e_StdIDCourier;
        try {
            Font font = da != null ? da.getFont() : null;
            if (font != null && !font.isEmpty())
                id = font.getStandard14Font(doc);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return id;
    }

}
