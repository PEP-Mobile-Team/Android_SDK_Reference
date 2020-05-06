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
package com.foxit.uiextensions;

import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;

/**
 * Interface that defines a tool to add annotation or select text.
 */
public interface ToolHandler extends PDFViewCtrl.IDrawEventListener {
    // Tool handler type
    /** Tool Handler type: select text.*/
    String TH_TYPE_TEXTSELECT = "TextSelect Tool";
    /** Tool Handler type: a tool to add annotation or signature on blank position.*/
    String TH_TYPE_BLANKSELECT = "BlankSelect Tool";

    //Annot tool
    /** Tool Handler type: highlight.*/
    String TH_TYPE_HIGHLIGHT = "Highlight Tool";
    /** Tool Handler type: underline.*/
    String TH_TYPE_UNDERLINE = "Underline Tool";
    /** Tool Handler type: strikeout.*/
    String TH_TYPE_STRIKEOUT = "Strikeout Tool";
    /** Tool Handler type: squiggly.*/
    String TH_TYPE_SQUIGGLY = "Squiggly Tool";
    /** Tool Handler type: note.*/
    String TH_TYPE_NOTE = "Note Tool";
    /** Tool Handler type: circle.*/
    String TH_TYPE_CIRCLE = "Circle Tool";
    /** Tool Handler type: square.*/
    String TH_TYPE_SQUARE = "Square Tool";
    /** Tool Handler type: typewriter.*/
    String TH_TYPE_TYPEWRITER = "Typewriter Tool";
    /** Tool Handler type: textbox.*/
    String TH_TYPE_TEXTBOX = "Textbox Tool";
    /** Tool Handler type: insert text.*/
    String TH_TYPR_INSERTTEXT = "InsetText Tool";
    /** Tool Handler type: callout.*/
    String TH_TYPE_CALLOUT = "Callout Tool";
    /** Tool Handler type: replace.*/
    String TH_TYPE_REPLACE = "Replace Tool";
    /** Tool Handler type: ink.*/
    String TH_TYPE_INK = "Ink Tool";
    /** Tool Handler type: eraser.*/
    String TH_TYPE_ERASER = "Eraser Tool";
    /** Tool Handler type: stamp.*/
    String TH_TYPE_STAMP = "Stamp Tool";
    /** Tool Handler type: line.*/
    String TH_TYPE_LINE = "Line Tool";
    /** Tool Handler type: arrow.*/
    String TH_TYPE_ARROW = "Arrow Tool";
    /** Tool Handler type: distance.*/
    String TH_TYPE_DISTANCE = "Distance Tool";
    /** Tool Handler type: form filler.*/
    String TH_TYPE_FORMFILLER = "FormFiller Tool";
    /** Tool Handler type: file attachment.*/
    String TH_TYPE_FILEATTACHMENT = "FileAttachment Tool";
    /** Tool Handler type: signature.*/
    String TH_TYPE_SIGNATURE = "Signature Tool";
    /** Tool Handler type: image.*/
    String TH_TYPE_PDFIMAGE= "PDFImage Tool";
    /** Tool Handler type: audio.*/
    String TH_TYPE_SCREEN_AUDIO= "Audio Tool";
    /** Tool Handler type: video.*/
    String TH_TYPE_SCREEN_VIDEO= "Video Tool";
    /** Tool Handler type: polygon.*/
    String TH_TYPE_POLYGON = "polygon Tool";
    /** Tool Handler type: polygon cloud.*/
    String TH_TYPE_POLYGONCLOUD = "polygon cloud Tool";
    /** Tool Handler type: polyline.*/
    String TH_TYPE_POLYLINE = "polyline Tool";
    /** Tool Handler type: select multiple annotations.*/
    String TH_TYPE_SELECT_ANNOTATIONS = "Select Annotations Tool";
    /** Tool Handler type: redact.*/
    String TH_TYPE_REDACT = "Redact Tool";
    /** @return the type of the tool handler, refer to {@code TH_TYPE_} constants such as: {@link #TH_TYPE_TEXTSELECT}, {@link #TH_TYPE_HIGHLIGHT}.
     */
    String getType();

    /**
     * Called when a tool handler is selected as the current tool handler.
     */
    void onActivate();

    /**
     * Called when the current tool handler is changed.
     */
    void onDeactivate();

    /**
     * Called when {@link PDFViewCtrl.UIExtensionsManager#onTouchEvent(int, MotionEvent)} is called
     *
     * @param pageIndex The page index.Valid range: from 0 to (<CODE>count</CODE>-1).
     *                  <CODE>count</CODE> is the page count.
     * @param motionEvent A <CODE>MotionEvent</CODE> object which species the event.
     *
     * @return {@link PDFViewCtrl.UIExtensionsManager#onTouchEvent(int, MotionEvent)}
     */
    boolean onTouchEvent(int pageIndex, MotionEvent motionEvent);

    /**
     * Called when {@link PDFViewCtrl.UIExtensionsManager#onLongPress(MotionEvent)} is called.
     *
     * @param pageIndex The page index.Valid range: from 0 to (<CODE>count</CODE>-1).
     *                  <CODE>count</CODE> is the page count.
     * @param motionEvent A <CODE>MotionEvent</CODE> object which species the event.
     *
     * @return {@link PDFViewCtrl.UIExtensionsManager#onLongPress(MotionEvent)}
     */
    boolean onLongPress(int pageIndex, MotionEvent motionEvent);

    /**
     * Called when {@link PDFViewCtrl.UIExtensionsManager#onSingleTapConfirmed(MotionEvent)} is called.
     *
     * @param pageIndex The page index.Valid range: from 0 to (<CODE>count</CODE>-1).
     *                  <CODE>count</CODE> is the page count.
     * @param motionEvent A <CODE>MotionEvent</CODE> object which species the event.
     *
     * @return {@link PDFViewCtrl.UIExtensionsManager#onSingleTapConfirmed(MotionEvent)}
     */
    boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent);

    /**
     *@return whether or not the annotation can be created continuously.
     */
    boolean isContinueAddAnnot();

    /**
     * Set whether the annot can be created continuously. The default is false.
     *
     * @param continueAddAnnot whether the annotation can be created continuously.
     */
    void setContinueAddAnnot(boolean continueAddAnnot);
}
