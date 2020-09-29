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
package com.foxit.uiextensions.annots.multiselect;


import android.graphics.Matrix;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.sdk.pdf.annots.QuadPointsArray;
import com.foxit.sdk.pdf.annots.Redact;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;

class MultiSelectUtils {

    static int getMoveState(ArrayList<Annot> annots, boolean canEdit) {
        if (annots.size() == 0 || !canEdit) return MultiSelectConstants.STATE_NONE;
        int count_none = 0;
        int count_move = 0;
        int count_drag_move = 0;
        for (int i = 0; i < annots.size(); i++) {
            if (getState(annots.get(i)) == MultiSelectConstants.STATE_NONE) {
                count_none++;
            } else if (getState(annots.get(i)) == MultiSelectConstants.STATE_MOVE) {
                count_move++;
            } else {//STATE_DRAG_MOVE
                count_drag_move++;
            }
        }

        int state;
        if (count_none > 0) {
            state = MultiSelectConstants.STATE_NONE;
        } else if (count_none == 0 && count_move > 0) {
            state = MultiSelectConstants.STATE_MOVE;
        } else { // (count_move == 0 && count_none == 0 && count_drag_move > 0)
            state = MultiSelectConstants.STATE_DRAG_MOVE;
        }
        return state;
    }

    private static int getState(Annot annot) {
        int annotType = AppAnnotUtil.getAnnotHandlerType(annot);
        int state = MultiSelectConstants.STATE_NONE;
        switch (annotType) {
            case Annot.e_Line://Arrow,Distance
            case Annot.e_Ink:
            case Annot.e_Square:
            case Annot.e_Circle:
            case Annot.e_PolyLine:
            case Annot.e_Polygon:
            case AnnotHandler.TYPE_FREETEXT_CALLOUT:
            case AnnotHandler.TYPE_FREETEXT_TEXTBOX:
                state = MultiSelectConstants.STATE_DRAG_MOVE;
                break;
            case Annot.e_Redact:
                Redact redact = (Redact) annot;
                try {
                    QuadPointsArray quadPointsArray = redact.getQuadPoints();
                    if (quadPointsArray.getSize() > 0) {
                        state = MultiSelectConstants.STATE_NONE;
                    } else {
                        state = MultiSelectConstants.STATE_DRAG_MOVE;
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
                break;
            case Annot.e_Stamp:
            case AnnotHandler.TYPE_SCREEN_IMAGE://Image
            case AnnotHandler.TYPE_SCREEN_MULTIMEDIA://Video,audio
            case Annot.e_Note:
            case Annot.e_FreeText://Typewriter
            case Annot.e_FileAttachment:
                state = MultiSelectConstants.STATE_MOVE;
                break;
            default://Annot.e_Highlight,Annot.e_Underline,Annot.e_StrikeOut,Annot.e_Squiggly,Annot.e_Caret(Insert/Replace)
                state = MultiSelectConstants.STATE_NONE;
                break;
        }

        return state;
    }

    static RectF getGroupRectF(PDFViewCtrl pdfViewCtrl, Annot annot) {
        RectF rectF = new RectF();
        try {
            if (AppAnnotUtil.isSupportAnnotGroup(annot) && AppAnnotUtil.isGrouped(annot)) {
                int pageIndex = annot.getPage().getIndex();
                Matrix matrix = pdfViewCtrl.getDisplayMatrix(pageIndex);

                MarkupArray markupArray = ((Markup) annot).getGroupElements();
                long arrSize = markupArray.getSize();
                for (long i = 0; i < arrSize; i++) {
                    Annot groupAnnot = AppAnnotUtil.createAnnot(markupArray.getAt(i));

                    UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
                    if (uiExtensionsManager.isLoadAnnotModule(groupAnnot)) {
                        RectF pvRect = AppUtil.toRectF(groupAnnot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                        if (rectF.isEmpty()) {
                            rectF = new RectF(pvRect);
                        } else {
                            rectF.union(pvRect);
                        }
                    }
                }

                if (!rectF.isEmpty())
                    pdfViewCtrl.convertPageViewRectToPdfRect(rectF, rectF, pageIndex);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return rectF;
    }

    static void normalize(RectF rectF) {
        if (rectF.left > rectF.right) {
            float tmp = rectF.left;
            rectF.left = rectF.right;
            rectF.right = tmp;
        }

        if (rectF.top > rectF.bottom) {
            float tmp = rectF.top;
            rectF.top = rectF.bottom;
            rectF.bottom = tmp;
        }

        if (rectF.left == rectF.right) rectF.right += 1;
        if (rectF.top == rectF.bottom) rectF.bottom += 1;
    }

    static void normalize(PDFViewCtrl viewCtrl, int pageIndex, RectF rectF, float dxy) {
        if ((int) rectF.left < dxy) {
            rectF.left = dxy;
        }
        if ((int) rectF.top < dxy) {
            rectF.top = dxy;
        }

        if ((int) rectF.right > viewCtrl.getPageViewWidth(pageIndex) - dxy) {
            rectF.right = viewCtrl.getPageViewWidth(pageIndex) - dxy;
        }
        if ((int) rectF.bottom > viewCtrl.getPageViewHeight(pageIndex) - dxy) {
            rectF.bottom = viewCtrl.getPageViewHeight(pageIndex) - dxy;
        }
    }

    static boolean isGroupSupportReply(ArrayList<Annot> annots) {
        boolean isSupportReply = false;
        int selectedSize = annots.size();
        for (int i = 0; i < selectedSize; i++) {
            isSupportReply = isAnnotSupportReply(annots.get(i));
            if (!isSupportReply)
                break;
        }
        return isSupportReply;
    }


    static boolean isAnnotSupportReply(Annot annot) {
        int annotType = Annot.e_UnknownType;
        try {
            annotType = annot.getType();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return Annot.e_Note == annotType
                || Annot.e_Stamp == annotType
                || Annot.e_Line == annotType
                || Annot.e_Square == annotType
                || Annot.e_Circle == annotType
                || Annot.e_Polygon == annotType
                || Annot.e_PolyLine == annotType
                || Annot.e_Ink == annotType;
    }

}
