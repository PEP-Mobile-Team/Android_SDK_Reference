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
package com.foxit.uiextensions.annots.note;

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

class NoteUtil {
    public static final float TA_BEZIER = 0.5522847498308f;

    //Get the path data according to different note icon type.
    public static Path getPathStringByType(String iconName, RectF rect) {
        int type = getIconByIconName(iconName);
        switch (type) {
            case NoteConstants.TA_ICON_UNKNOWN:
                return GetPathDataComment(rect);
            case NoteConstants.TA_ICON_COMMENT:
                return GetPathDataComment(rect);
            case NoteConstants.TA_ICON_KEY:
                return GetPathDataKey(rect);
            case NoteConstants.TA_ICON_NOTE:
                return GetPathDataNote(rect);
            case NoteConstants.TA_ICON_HELP:
                return GetPathDataHelp(rect);
            case NoteConstants.TA_ICON_NEWPARAGRAPH:
                return GetPathDataNewParagraph(rect);
            case NoteConstants.TA_ICON_PARAGRAPH:
                return GetPathDataParagraph(rect);
            case NoteConstants.TA_ICON_INSERT:
                return GetPathDataInsert(rect);
            default:
        }
        return null;
    }

    public static Path GetPathDataComment(RectF rect) {
        Path path = new Path();
        float l = rect.left;
        float b = rect.bottom;
        float r = rect.right;
        float t = rect.top;
        float w = r - l;
        float h = t - b;

        path.moveTo(l + w / 15.0f, t - h / 6.0f);
        path.cubicTo(l + w / 15.0f, t - h / 6.0f + TA_BEZIER * (h / 6.0f - h / 10.0f),
                l + w * 2 / 15.0f - TA_BEZIER * w / 15.0f, t - h / 10.0f,
                l + w * 2 / 15.0f, t - h / 10.0f);
        path.lineTo(r - w * 2 / 15.0f, t - h / 10.0f);
        path.cubicTo(r - w * 2 / 15.0f + TA_BEZIER * w / 15.0f, t - h / 10.0f,
                r - w / 15.0f, t - h / 6 + TA_BEZIER * (h / 6.0f - h / 10.0f),
                r - w / 15.0f, t - h / 6.0f);
        path.lineTo(r - w / 15.0f, b + h / 3.0f);
        path.cubicTo(r - w / 15.0f, b + h * 4 / 15.0f + TA_BEZIER * h / 15.0f,
                r - w * 2 / 15.0f + TA_BEZIER * w / 15.0f, b + h * 4 / 15.0f,
                r - w * 2 / 15.0f, b + h * 4 / 15.0f);
        path.lineTo(l + w * 5 / 15.0f, b + h * 4 / 15.0f);

        path.cubicTo(l + w * 5 / 15.0f, b + h * 2 / 15 + TA_BEZIER * h * 2 / 15.0f,
                l + w * 5 / 15.0f - TA_BEZIER * w * 2 / 15.0f, b + h * 2 / 15.0f,
                l + w * 6 / 30.0f, b + h * 2 / 15.0f);
        path.cubicTo(l + w * 7 / 30.0f + TA_BEZIER * w / 30.0f, b + h * 2 / 15.0f,
                l + w * 7 / 30.0f, b + h * 2 / 15.0f + TA_BEZIER * h * 2 / 15.0f,
                l + w * 7 / 30.0f, b + h * 4 / 15.0f);

        path.lineTo(l + w * 2 / 15.0f, b + h * 4 / 15.0f);
        path.cubicTo(l + w * 2 / 15.0f - TA_BEZIER * w / 15.0f, b + h * 4 / 15.0f,
                l + w / 15.0f, b + h / 3.0f - TA_BEZIER * h / 15.0f,
                l + w / 15.0f, b + h / 3.0f);

        path.lineTo(l + w / 15.0f, t - h / 6.0f);

        path.moveTo(l + w * 2 / 15.0f, t - h * 8 / 30.0f);
        path.lineTo(r - w * 2 / 15.0f, t - h * 8 / 30.0f);
        path.moveTo(l + w * 2 / 15, t - h * 25 / 60.0f);
        path.lineTo(r - w * 2 / 15.0f, t - h * 25 / 60.0f);
        path.moveTo(l + w * 2 / 15.0f, t - h * 17 / 30.0f);
        path.lineTo(r - w * 4 / 15.0f, t - h * 17 / 30.0f);
        return path;

    }

    public static Path GetPathDataKey(RectF rect) {
        Path path = new Path();
        float l = rect.left;
        float b = rect.bottom;
        float r = rect.right;
        float t = rect.top;
        float w = r - l;
        float h = t - b;
        float k = -h / w;

        PointF tail = new PointF(0, 0);
        PointF center = new PointF(0, 0);
        tail.x = l + w * 0.9f;
        tail.y = k * (tail.x - r) + b;
        center.x = l + w * 0.15f;
        center.y = k * (center.x - r) + b;
        path.moveTo(tail.x + w / 30.0f, -w / 30.0f / k + tail.y);
        path.lineTo(tail.x + w / 30.0f - w * 0.18f, -k * w * 0.18f - w / 30 / k + tail.y);
        path.lineTo(tail.x + w / 30 - w * 0.18f + w * 0.07f, -w * 0.07f / k - k * w * 0.18f - w / 30 / k + tail.y);
        path.lineTo(tail.x + w / 30 - w * 0.18f - w / 20 + w * 0.07f, -w * 0.07f / k - k * w / 20 - k * w * 0.18f - w
                / 30 / k + tail.y);
        path.lineTo(tail.x + w / 30 - w * 0.18f - w / 20, -k * w / 20 - k * w * 0.18f - w / 30 / k + tail.y);
        path.lineTo(tail.x + w / 30 - w * 0.18f - w / 20 - w / 15, -k * w / 15 - k * w / 20 - k * w * 0.18f - w / 30
                / k + tail.y);
        path.lineTo(tail.x + w / 30 - w * 0.18f - w / 20 - w / 15 + w * 0.07f, -w * 0.07f / k - k * w / 15 - k * w / 20
                - k * w * 0.18f - w / 30 / k + tail.y);
        path.lineTo(tail.x + w / 30 - w * 0.18f - w / 20 - w / 15 - w / 20 + w * 0.07f, -w * 0.07f / k + -k * w / 20
                + -k * w / 15 - k * w / 20 - k * w * 0.18f - w / 30 / k + tail.y);
        path.lineTo(tail.x + w / 30 - w * 0.18f - w / 20 - w / 15 - w / 20, -k * w / 20 + -k * w / 15 - k * w / 20 - k
                * w * 0.18f - w / 30 / k + tail.y);
        path.lineTo(tail.x + w / 30 - w * 0.45f, -k * w * 0.45f - w / 30 / k + tail.y);
        path.cubicTo(tail.x + w / 30 - w * 0.45f + w * 0.2f, -w * 0.4f / k - k * w * 0.45f - w / 30 / k + tail.y,
                center.x + w * 0.2f, -w * 0.1f / k + center.y,
                center.x, center.y);
        path.cubicTo(center.x - w / 60.0f, -k * w / 60 + center.y,
                center.x - w / 60, -k * w / 60 + center.y,
                center.x, center.y);
        path.cubicTo(center.x - w * 0.22f, w * 0.35f / k + center.y - h * 0.05f,
                tail.x - w / 30 - w * 0.45f - w * 0.18f, w * 0.05f / k - k * w * 0.45f + w / 30 / k + tail.y - h
                        * 0.05f,
                tail.x - w / 30.0f - w * 0.45f, -k * w * 0.45f + w / 30.0f / k + tail.y);
        path.lineTo(tail.x - w / 30.0f, w / 30.0f / k + tail.y);
        path.lineTo(tail.x + w / 30, -w / 30 / k + tail.y);
        path.moveTo(center.x + w * 0.08f, k * w * 0.08f + center.y);
        path.cubicTo(center.x + w * 0.08f + w * 0.1f, -w * 0.1f / k + k * w * 0.08f + center.y,
                center.x + w * 0.22f + w * 0.1f, k * w * 0.22f + center.y - w * 0.1f / k,
                center.x + w * 0.22f, k * w * 0.22f + center.y);
        path.cubicTo(center.x + w * 0.22f - w * 0.1f, w * 0.1f / k + k * w * 0.22f + center.y,
                center.x + w * 0.08f - w * 0.1f, w * 0.1f / k + k * w * 0.08f + center.y,
                center.x + w * 0.08f, k * w * 0.08f + center.y);
        return path;
    }

    public static Path GetPathDataNote(RectF rect) {
        Path path = new Path();
        float l = rect.left;
        float b = rect.bottom;
        float r = rect.right;
        float t = rect.top;
        float w = r - l;
        float h = t - b;

        path.moveTo(r - w * 3 / 10.0f, b + h / 15.0f);
        path.lineTo(l + w * 7 / 10.0f, b + h * 4 / 15.0f);
        path.lineTo(r - w / 10.0f, b + h * 4 / 15.0f);
        path.lineTo(r - w / 10.0f, t - h / 15.0f);
        path.lineTo(l + w / 10.0f, t - h / 15.0f);
        path.lineTo(l + w / 10.0f, b + h / 15.0f);
        path.lineTo(r - w * 3 / 10.0f, b + h / 15.0f);
        path.lineTo(r - w / 10.0f, b + h * 4 / 15.0f);
        path.lineTo(r - w * 3 / 10.0f, b + h / 15.0f);
        path.lineTo(r - w * 3 / 10.0f, b + h * 4 / 15.0f);
        path.lineTo(r - w / 10.0f, b + h * 4 / 15.0f);
        path.moveTo(l + w / 5.0f, t - h * 4 / 15.0f);
        path.lineTo(r - w / 5.0f, t - h * 4 / 15.0f);
        path.moveTo(l + w / 5.0f, t - h * 7 / 15.0f);
        path.lineTo(r - w / 5.0f, t - h * 7 / 15.0f);
        path.moveTo(l + w / 5.0f, t - h * 10 / 15.0f);
        path.lineTo(r - w * 3 / 10.0f, t - h * 10 / 15.0f);
        return path;
    }

    public static Path GetPathDataHelp(RectF rect) {
        Path path = new Path();
        float l = rect.left;
        float b = rect.bottom;
        float r = rect.right;
        float t = rect.top;
        float w = r - l;
        float h = t - b;

        path.moveTo(l + w / 60.0f, b + h / 2.0f);
        path.cubicTo(l + w / 60.0f, b + h / 2.0f + TA_BEZIER * (h / 60.0f - h / 2.0f),
                l + w / 2.0f - TA_BEZIER * (w / 2.0f - w / 60.0f), b + h / 60.0f,
                l + w / 2.0f, b + h / 60.0f);
        path.cubicTo(l + w / 2.0f + TA_BEZIER * w * 29 / 60.0f, b + h / 60.0f,
                r - w / 60.0f, b + h / 2.0f + TA_BEZIER * (h / 60.0f - h / 2.0f),
                r - w / 60.0f, b + h / 2.0f);
        path.cubicTo(r - w / 60.0f, b + h / 2.0f + TA_BEZIER * h * 29 / 60.0f,
                l + w / 2.0f + TA_BEZIER * w * 29 / 60.0f, t - h / 60.0f,
                l + w / 2.0f, t - h / 60.0f);
        path.cubicTo(l + w / 2.0f - TA_BEZIER * w * 29 / 60.0f, t - h / 60.0f,
                l + w / 60.0f, b + h / 2.0f + TA_BEZIER * h * 29 / 60.0f,
                l + w / 60.0f, b + h / 2.0f);

        path.moveTo(l + w * 0.27f, t - h * 0.36f);
        path.cubicTo(l + w * 0.27f, t - h * 0.36f + TA_BEZIER * h * 0.23f,
                l + w * 0.5f - TA_BEZIER * w * 0.23f, b + h * 0.87f,
                l + w * 0.5f, b + h * 0.87f);
        path.cubicTo(l + w * 0.5f + TA_BEZIER * w * 0.23f, b + h * 0.87f,
                r - w * 0.27f, t - h * 0.36f + TA_BEZIER * h * 0.23f,
                r - w * 0.27f, t - h * 0.36f);
        path.cubicTo(r - w * 0.27f - w * 0.08f * 0.2f, t - h * 0.36f - h * 0.15f * 0.7f,
                r - w * 0.35f + w * 0.08f * 0.2f, t - h * 0.51f + h * 0.15f * 0.2f,
                r - w * 0.35f, t - h * 0.51f);
        path.cubicTo(r - w * 0.35f - w * 0.1f * 0.5f, t - h * 0.51f - h * 0.15f * 0.3f,
                r - w * 0.45f - w * 0.1f * 0.5f, t - h * 0.68f + h * 0.15f * 0.5f,
                r - w * 0.45f, t - h * 0.68f);
        path.lineTo(r - w * 0.45f, b + h * 0.30f);
        path.cubicTo(r - w * 0.45f, b + h * 0.30f + w * 0.1f * 0.7f,
                r - w * 0.55f, b + h * 0.30f + w * 0.1f * 0.7f,
                r - w * 0.55f, b + h * 0.30f);
        path.lineTo(r - w * 0.55f, t - h * 0.66f);
        path.cubicTo(r - w * 0.55f - w * 0.1f * 0.05f, t - h * 0.66f + h * 0.18f * 0.5f,
                r - w * 0.45f - w * 0.1f * 0.05f, t - h * 0.48f - h * 0.18f * 0.3f,
                r - w * 0.45f, t - h * 0.48f);
        path.cubicTo(r - w * 0.45f + w * 0.08f * 0.2f, t - h * 0.48f + h * 0.18f * 0.2f,
                r - w * 0.37f - w * 0.08f * 0.2f, t - h * 0.36f - h * 0.18f * 0.7f,
                r - w * 0.37f, t - h * 0.36f);
        path.cubicTo(r - w * 0.37f, t - h * 0.36f + TA_BEZIER * h * 0.13f,
                l + w * 0.5f + TA_BEZIER * w * 0.13f, b + h * 0.77f,
                l + w * 0.5f, b + h * 0.77f);
        path.cubicTo(l + w * 0.5f - TA_BEZIER * w * 0.13f, b + h * 0.77f,
                l + w * 0.37f, t - h * 0.36f + TA_BEZIER * h * 0.13f,
                l + w * 0.37f, t - h * 0.36f);
        path.cubicTo(l + w * 0.37f, t - h * 0.36f + w * 0.1f * 0.6f,
                l + w * 0.27f, t - h * 0.36f + w * 0.1f * 0.6f,
                l + w * 0.27f, t - h * 0.36f);

        path.moveTo(r - w * 0.56f, b + h * 0.13f);
        path.cubicTo(r - w * 0.56f, b + h * 0.13f + TA_BEZIER * h * 0.055f,
                r - w * 0.505f - TA_BEZIER * w * 0.095f, b + h * 0.185f,
                r - w * 0.505f, b + h * 0.185f);
        path.cubicTo(r - w * 0.505f + TA_BEZIER * w * 0.065f, b + h * 0.185f,
                r - w * 0.44f, b + h * 0.13f + TA_BEZIER * h * 0.055f,
                r - w * 0.44f, b + h * 0.13f);
        path.cubicTo(r - w * 0.44f, b + h * 0.13f - TA_BEZIER * h * 0.055f,
                r - w * 0.505f + TA_BEZIER * w * 0.065f, b + h * 0.075f,
                r - w * 0.505f, b + h * 0.075f);
        path.cubicTo(r - w * 0.505f - TA_BEZIER * w * 0.065f, b + h * 0.075f,
                r - w * 0.56f, b + h * 0.13f - TA_BEZIER * h * 0.055f,
                r - w * 0.56f, b + h * 0.13f);
        return path;
    }

    public static Path GetPathDataNewParagraph(RectF rect) {
        Path path = new Path();
        float l = rect.left;
        float b = rect.bottom;
        float r = rect.right;
        float t = rect.top;
        float w = r - l;
        float h = t - b;

        path.moveTo(l + w / 2.0f, t - h / 20.0f);
        path.lineTo(l + w / 10.0f, t - h / 2.0f);
        path.lineTo(r - w / 10.0f, t - h / 2.0f);
        path.lineTo(l + w / 2.0f, t - h / 20.0f);

        path.moveTo(l + w * 0.12f, t - h * 17 / 30.0f);
        path.lineTo(l + w * 0.12f, b + h / 10.0f);
        path.lineTo(l + w * 0.22f, b + h / 10.0f);
        path.lineTo(l + w * 0.22f, t - h * 17 / 30.0f + w * 0.14f);

        path.lineTo(l + w * 0.38f, b + h / 10.0f);
        path.lineTo(l + w * 0.48f, b + h / 10.0f);
        path.lineTo(l + w * 0.48f, t - h * 17 / 30.0f);
        path.lineTo(l + w * 0.38f, t - h * 17 / 30.0f);
        path.lineTo(l + w * 0.38f, b - w * 0.24f);
        path.lineTo(l + w * 0.22f, t - h * 17 / 30.0f);
        path.lineTo(l + w * 0.12f, t - h * 17 / 30.0f);
        path.moveTo(l + w * 0.6f, b + h / 10.0f);
        path.lineTo(l + w * 0.7f, b + h / 10.0f);
        path.lineTo(l + w * 0.7f, b + h / 10.0f + h / 7.0f);
        path.cubicTo(l + w * 0.97f, b + h / 10.0f + h / 7.0f,
                l + w * 0.97f, t - h * 17 / 30.0f,
                l + w * 0.7f, t - h * 17 / 30.0f);
        path.lineTo(l + w * 0.6f, t - h * 17 / 30.0f);
        path.lineTo(l + w * 0.6f, b + h / 10.0f);
        path.moveTo(l + w * 0.7f, b + h / 7 + h * 0.18f);
        path.cubicTo(l + w * 0.85f, b + h / 7 + h * 0.18f,
                l + w * 0.85f, t - h * 17 / 30.0f - h * 0.08f,
                l + w * 0.7f, t - h * 17 / 30.0f - h * 0.08f);
        path.lineTo(l + w * 0.7f, b + h / 7 + h * 0.18f);
        return path;
    }

    public static Path GetPathDataParagraph(RectF rect) {
        Path path = new Path();
        float l = rect.left;
        float b = rect.bottom;
        float r = rect.right;
        float t = rect.top;
        float w = r - l;
        float h = t - b;

        path.moveTo(l + w / 2.0f, t - h / 15.0f);
        path.lineTo(l + w * 0.7f, t - h / 15.0f);
        path.lineTo(l + w * 0.7f, b + h / 15.0f);
        path.lineTo(l + w * 0.634f, b + h / 15.0f);
        path.lineTo(l + w * 0.634f, t - h * 2 / 15.0f);
        path.lineTo(l + w * 0.566f, t - h * 2 / 15.0f);
        path.lineTo(l + w * 0.566f, b + h / 15.0f);
        path.lineTo(l + w / 2.0f, b + h / 15.0f);
        path.lineTo(l + w / 2.0f, t - h / 15.0f - h * 0.4f);
        path.cubicTo(l + w * 0.2f, t - h / 15.0f - h * 0.4f,
                l + w * 0.2f, t - h / 15.0f,
                l + w / 2.0f, t - h / 15.0f);
        return path;
    }

    public static Path GetPathDataInsert(RectF rect) {
        Path path = new Path();
        float l = rect.left;
        float b = rect.bottom;
        float r = rect.right;
        float t = rect.top;
        float w = r - l;
        float h = t - b;

        path.moveTo(l + w / 10, b + h / 10);
        path.lineTo(l + w / 2, t - h * 2 / 15);
        path.lineTo(r - w / 10, b + h / 10);
        path.lineTo(l + w / 10, b + h / 10);
        return path;
    }

    public static String getIconNameByType(int type) {
        String iconName = "Comment";
        switch (type) {
            case NoteConstants.TA_ICON_COMMENT:
                iconName = "Comment";
                break;
            case NoteConstants.TA_ICON_KEY:
                iconName = "Key";
                break;
            case NoteConstants.TA_ICON_NOTE:
                iconName = "Note";
                break;
            case NoteConstants.TA_ICON_HELP:
                iconName = "Help";
                break;
            case NoteConstants.TA_ICON_NEWPARAGRAPH:
                iconName = "NewParagraph";
                break;
            case NoteConstants.TA_ICON_PARAGRAPH:
                iconName = "Paragraph";
                break;
            case NoteConstants.TA_ICON_INSERT:
                iconName = "Insert";
                break;
            default:
                break;
        }
        return iconName;
    }

    public static int getIconByIconName(String iconName) {
        if (iconName == null) return NoteConstants.TA_ICON_COMMENT;
        if (iconName.equalsIgnoreCase("Comment")) return NoteConstants.TA_ICON_COMMENT;
        if (iconName.equalsIgnoreCase("Key")) return NoteConstants.TA_ICON_KEY;
        if (iconName.equalsIgnoreCase("Note")) return NoteConstants.TA_ICON_NOTE;
        if (iconName.equalsIgnoreCase("Help")) return NoteConstants.TA_ICON_HELP;
        if (iconName.equalsIgnoreCase("NewParagraph")) return NoteConstants.TA_ICON_NEWPARAGRAPH;
        if (iconName.equalsIgnoreCase("Paragraph")) return NoteConstants.TA_ICON_PARAGRAPH;
        if (iconName.equalsIgnoreCase("Insert")) return NoteConstants.TA_ICON_INSERT;
        return NoteConstants.TA_ICON_COMMENT;
    }

}
