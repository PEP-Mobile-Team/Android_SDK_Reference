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
package com.foxit.uiextensions.modules.signature;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.fxcrt.Matrix2D;
import com.foxit.sdk.pdf.graphics.GraphicsObject;
import com.foxit.sdk.pdf.graphics.ImageObject;
import com.foxit.uiextensions.utils.Event;


class SignaturePSITask extends Task {

    private SignatureEvent mEvent;

    public SignaturePSITask(final SignatureEvent event, final Event.Callback callBack) {
        super(new CallBack() {
            @Override
            public void result(Task task) {
                if(event instanceof SignatureSignEvent && ((SignatureSignEvent) event).mCallBack != null){
                    ((SignatureSignEvent) event).mCallBack.result(event,true);
                }

                if(callBack!= null) {
                    callBack.result(event, true);
                }

            }
        });
        mEvent = event;
    }

    @Override
    protected void execute() {
        if(mEvent instanceof SignatureSignEvent)
        {
            try {
                ImageObject imageObject = ImageObject.create(((SignatureSignEvent) mEvent).mPage.getDocument());
                if (imageObject == null) return;
                imageObject.setBitmap(((SignatureSignEvent) mEvent).mBitmap, null);

                int rotation = (((SignatureSignEvent) mEvent).mPage.getRotation() + ((SignatureSignEvent) mEvent).mViewRotation) % 4;
                float width = Math.abs(((SignatureSignEvent) mEvent).mRect.width());
                float height = Math.abs(((SignatureSignEvent) mEvent).mRect.height());

                Matrix2D matrix2D = new Matrix2D();
                switch (rotation) {
                    case com.foxit.sdk.common.Constants.e_Rotation0:
                        matrix2D.set(width, 0 ,0, height, ((SignatureSignEvent) mEvent).mRect.left, ((SignatureSignEvent) mEvent).mRect.bottom);
                        break;
                    case com.foxit.sdk.common.Constants.e_Rotation90:
                        matrix2D.set(0, height , -width, 0, ((SignatureSignEvent) mEvent).mRect.left + width, ((SignatureSignEvent) mEvent).mRect.bottom);
                        break;
                    case com.foxit.sdk.common.Constants.e_Rotation270:
                        matrix2D.set(0, -height, width, 0, ((SignatureSignEvent) mEvent).mRect.left, ((SignatureSignEvent) mEvent).mRect.bottom + height);
                        break;
                    case com.foxit.sdk.common.Constants.e_Rotation180:
                        matrix2D.set(-width, 0 , 0, -height, ((SignatureSignEvent) mEvent).mRect.left + width, ((SignatureSignEvent) mEvent).mRect.bottom + height);
                        break;
                    default:
                        break;
                }

                imageObject.setMatrix(matrix2D);
                long pos = ((SignatureSignEvent) mEvent).mPage.getLastGraphicsObjectPosition(GraphicsObject.e_TypeAll);
                ((SignatureSignEvent) mEvent).mPage.insertGraphicsObject(pos,imageObject);
                ((SignatureSignEvent) mEvent).mPage.generateContent();
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }
}
