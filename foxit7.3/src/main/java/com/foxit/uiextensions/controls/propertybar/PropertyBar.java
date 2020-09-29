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
package com.foxit.uiextensions.controls.propertybar;

import android.graphics.Color;
import android.graphics.RectF;
import android.view.View;

import com.foxit.sdk.common.Constants;
import com.foxit.uiextensions.annots.note.NoteConstants;


public interface PropertyBar {
    public interface PropertyChangeListener {
        public void onValueChanged(long property, int value);

        public void onValueChanged(long property, float value);

        public void onValueChanged(long property, String value);
    }

    public interface DismissListener {
        public void onDismiss();
    }

    public interface UpdateViewListener {
        public void onUpdate(long property, int value);
    }

    public static final long PROPERTY_UNKNOWN = 0x00000000L;
    public static final long PROPERTY_COLOR = 0x00000001L;
    public static final long PROPERTY_OPACITY = 0x00000002L;
    public static final long PROPERTY_LINEWIDTH = 0x00000004L;
    public static final long PROPERTY_FONTNAME = 0x00000008L;
    public static final long PROPERTY_FONTSIZE = 0x00000010L;
    public static final long PROPERTY_LINE_STYLE = 0x00000020L;
    public static final long PROPERTY_ANNOT_TYPE = 0x00000040L;
    public static final long PROPERTY_SELF_COLOR = 0x00000080L;
    public static final long PROPERTY_SCALE_PERCENT = 0x00000100L;
    public static final long PROPERTY_SCALE_SWITCH = 0x00000200L;
    public static final long PROPERTY_ROTATION = 0x00000400L;
    public static final long PROPERTY_EDIT_TEXT = 0x00000800L;
    public static final long PROPERTY_ALL = 0x000003FFL;

    long PROPERTY_DISTANCE = 0x00001000L;
    long PROPERTY_DISTANCE_TIP = 0x00001002L;
    long PROPERTY_DISTANCE_VALUE = 0x00001004L;
    long PROPERTY_DISTANCE_TIP_VALUE = 0x0001008L;
    long PROPERTY_DISTANCE_DISPLAY = 0x00002000L;

    public static final long PROPERTY_FILEATTACHMENT = 0x0010000000L;
    public static final long PROPERTY_IMAGE = 0x0020000000L;
    public static final long PROPERTY_REDACT = 0x0040000000L;
    public static final long PROPERTY_OPTIONS = 0x0080000000L;
    public static final long PROPERTY_TTS_SPEED_RATES = 0x00100000000L;

    public static final int ARROW_NONE = 0;
    public static final int ARROW_LEFT = 1;
    public static final int ARROW_TOP = 2;
    public static final int ARROW_RIGHT = 3;
    public static final int ARROW_BOTTOM = 4;
    public static final int ARROW_CENTER = 5;

    int[] PB_COLORS_TOOL_GROUP_1 = new int[]{Color.argb(255, 254, 102, 51), Color.argb(255, 255, 0, 255),
            Color.argb(255, 255, 204, 0), Color.argb(255, 0, 255, 255),
            Color.argb(255, 0, 255, 0), Color.argb(255, 255, 255, 0),
            Color.argb(255, 255, 0, 0), Color.argb(255, 153, 51, 153),
            Color.argb(255, 204, 153, 255), Color.argb(255, 255, 153, 204),
            Color.argb(255, 0, 0, 255), Color.argb(255, 102, 204, 51),
            Color.argb(255, 0, 0, 0), Color.argb(255, 51, 51, 51),
            Color.argb(255, 102, 102, 102), Color.argb(255, 153, 153, 153),
            Color.argb(255, 204, 204, 204), Color.argb(255, 255, 255, 255)};

    int[] PB_COLORS_TOOL_GROUP_2 = new int[]{Color.argb(255, 204, 102, 102), Color.argb(255, 153, 51, 51),
            Color.argb(255, 111, 50, 46), Color.argb(255, 51, 102, 102),
            Color.argb(255, 102, 102, 51), Color.argb(255, 51, 102, 0),
            Color.argb(255, 102, 51, 102), Color.argb(255, 51, 0, 102),
            Color.argb(255, 0, 0, 102), Color.argb(255, 51, 51, 102),
            Color.argb(255, 102, 102, 0), Color.argb(255, 102, 51, 51),
            Color.argb(255, 0, 0, 0), Color.argb(255, 51, 51, 51),
            Color.argb(255, 102, 102, 102), Color.argb(255, 153, 153, 153),
            Color.argb(255, 204, 204, 204), Color.argb(255, 255, 255, 255)};

    public static final int[] PB_COLORS_HIGHLIGHT = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_UNDERLINE = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_SQUIGGLY = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_STRIKEOUT = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_CARET = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_TYPEWRITER = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_TEXTBOX = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_CALLOUT = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_LINE = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_CIRCLE = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_SQUARE = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_ARROW = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_PENCIL = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_TEXT = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_FILEATTACHMENT = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_POLYGON = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_POLYLINE = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_NOTE = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_REDACT = PB_COLORS_TOOL_GROUP_1;
    public static final int[] PB_COLORS_FORM = PB_COLORS_TOOL_GROUP_1;

    public static final int[] PB_COLORS_SIGN = PB_COLORS_TOOL_GROUP_2;

    public static final int[] PB_OPACITYS = new int[]{25, 50, 75, 100};
    public static final int[] PB_ROTAIIONS = new int[]{Constants.e_Rotation0, Constants.e_Rotation90, Constants.e_Rotation180, Constants.e_Rotation270};
    public static final float PB_FONTSIZE_DEFAULT = 24.0f;
    public static final float[] PB_FONTSIZES = new float[]{6.0f, 8.0f, 10.0f, 12.0f, 18.0f, PB_FONTSIZE_DEFAULT, 36.0f, 48.0f, 64.0f, 72.0f};

    public static final int[] ICONTYPES = new int[]{NoteConstants.TA_ICON_COMMENT, NoteConstants.TA_ICON_KEY, NoteConstants.TA_ICON_NOTE, NoteConstants.TA_ICON_HELP, NoteConstants.TA_ICON_NEWPARAGRAPH, NoteConstants.TA_ICON_PARAGRAPH, NoteConstants.TA_ICON_INSERT};

    public static final float PB_ALPHA = 0.6f;

    public void setColors(int[] colors);

    public void setProperty(long property, int value);

    public void setProperty(long property, float value);

    public void setProperty(long property, float[] values);

    public void setProperty(long property, String value);

    public void setPropertyTitle(long property, String tabTitle, String itemTitle);

    public void setArrowVisible(boolean visible);

    public void setPhoneFullScreen(boolean fullScreen);

    public void reset(long items);

    public void reset(long items, boolean clearCustomProperty);

    public void setTopTitleVisible(boolean visible);

    public void addTab(String title, int index);

    public void addTab(String topTitle, int resid_img, String title, int index);

    public int getItemIndex(long item);

    public void addCustomItem(long item, View itemView, int tabIndex, int index);

    public void addContentView(View contentView);

    public View getContentView();

    public int getCurrentTabIndex();

    public void setCurrentTab(int currentTab);

    public void show(RectF rectF, boolean showMask);

    public void show(View parent, RectF rectF, boolean showMask);

    public void update(RectF rectF);

    public void update(View parent, RectF rectF);

    public void dismiss();

    public boolean isShowing();

    public PropertyChangeListener getPropertyChangeListener();

    public void setPropertyChangeListener(PropertyChangeListener listener);

    public void setDismissListener(DismissListener dismissListener);

    void scaleFromUnit(int index);

    void scaleFromValue(int value);

    void scaleToUnit(int index);

    void scaleToValue(int value);

    void setDistanceScale(String[] distanceScale);

    void setEditable(boolean canEdit);

    void requestLayout();
}
