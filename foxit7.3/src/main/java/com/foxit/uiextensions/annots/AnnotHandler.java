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
package com.foxit.uiextensions.annots;

import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;

/**
 * Interface that defines a annotation handler that edit annotation.
 */
public interface AnnotHandler extends PDFViewCtrl.IDrawEventListener {
    /** The type of textbox annotation handler*/
    int TYPE_FREETEXT_TEXTBOX = 100;
    /** The type of callout annotation handler*/
    int TYPE_FREETEXT_CALLOUT = 101;
    /** The type of signature annotation handler*/
    int TYPE_FORMFIELD_SIGNATURE = 102;
    /** The type of image annotation handler*/
    int TYPE_SCREEN_IMAGE = 201;
    /** The type of multimedia annotation handler*/
    int TYPE_SCREEN_MULTIMEDIA = 202;

    /** The type of multiselect handler*/
    int TYPE_MULTI_SELECT = 301;
    /**
     * Get the type of a {@link AnnotHandler}.
     *
     * usually, we use annotation`s type(such as {@link Annot#e_Note}) as its type.
     *
     * Specifically, using {@link #TYPE_FREETEXT_CALLOUT} as the type of Callout annotation handler,
     * {@link #TYPE_FREETEXT_TEXTBOX} as the type of Textbox annotation handler and
     * {@link Annot#e_FreeText} as the type of Typewriter annotation handler.
     *
     * using {@link #TYPE_FORMFIELD_SIGNATURE} as the type of signature annotation handler and
     * {@link Annot#e_Widget} as the type of Form annotation handler.
     *
     * using {@link #TYPE_SCREEN_IMAGE} as the type of Image annotation handler,
     * {@link #TYPE_SCREEN_MULTIMEDIA} as the type of Multimedia annotation handler
     *
     * @return Return the type of a {@link AnnotHandler}.
     */
    int getType();

    /** Whether the specified annotation can be answered.*/
    boolean annotCanAnswer(Annot annot);

    /** Return the bbox of the specified annotation.*/
    RectF getAnnotBBox(Annot annot);

    /** Whether hit the specified on the position.*/
    boolean isHitAnnot(Annot annot, PointF point);

    /**
     * Called when the specified annotation is selected.
     * @param annot The selected annotation
     * @param reRender whether re-render the selected annotation.
     */
    void onAnnotSelected(Annot annot, boolean reRender);

    /**
     * Called when the current selected annotation lost focus.
     * @param annot The current selected annotation
     * @param reRender whether re-render the selected annotation.
     */
    void onAnnotDeselected(Annot annot, boolean reRender);

    /**
     * Add annotation to the specified page.
     *
     * @param pageIndex The page where add the annotation
     * @param content a {@link AnnotContent} to use
     * @param addUndo whether can be do redo or undo operation.
     * @param result The callback used to allow the user to run some code when add annotation
     */
    void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, com.foxit.uiextensions.utils.Event.Callback result);

    /**
     * Modify the specified annotation by using a {@link AnnotContent}
     * @param annot The specified annotation which will be modified.
     * @param content a {@link AnnotContent} to use
     * @param addUndo whether can be do redo or undo operation.
     * @param result The callback used to allow the user to run some code when add annotation
     */
    void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, com.foxit.uiextensions.utils.Event.Callback result);

    /**
     * Remove the specified annotation from a page.
     * @param annot The specified annotation which will be removed.
     * @param addUndo whether can be do redo or undo operation.
     * @param result The callback used to allow the user to run some code when add annotation
     */
    void removeAnnot(Annot annot, boolean addUndo, com.foxit.uiextensions.utils.Event.Callback result);

    /**
     * Called when {@link PDFViewCtrl.UIExtensionsManager#onTouchEvent(int, MotionEvent)} is called
     *
     * @see PDFViewCtrl.UIExtensionsManager#onTouchEvent(int, MotionEvent)
     */
    boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot);

    /**
     * Called when {@link PDFViewCtrl.UIExtensionsManager#onLongPress(MotionEvent)} is called.
     *
     * @see PDFViewCtrl.UIExtensionsManager#onLongPress(MotionEvent)
     */
    boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot);

    /**
     * Called when {@link PDFViewCtrl.UIExtensionsManager#onSingleTapConfirmed(MotionEvent)} is called.
     *
     * @see PDFViewCtrl.UIExtensionsManager#onSingleTapConfirmed(MotionEvent)
     */
    boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot);

    /**
     * Called when {@link PDFViewCtrl.UIExtensionsManager#shouldViewCtrlDraw(Annot)} is called
     *
     * @see PDFViewCtrl.UIExtensionsManager#shouldViewCtrlDraw(Annot)
     */
    boolean shouldViewCtrlDraw(Annot annot);
}
