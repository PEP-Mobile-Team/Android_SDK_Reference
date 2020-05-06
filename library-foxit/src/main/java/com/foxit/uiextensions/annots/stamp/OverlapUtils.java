package com.foxit.uiextensions.annots.stamp;

import android.graphics.PointF;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.utils.AppUtil;

/**
 * 锚点偏移工具类
 * wuk
 * 2018/9/16
 */
public class OverlapUtils {
    private static final float hight = (float) 20;//pdf中,锚点的固定高度大约是20
    /**
     * 校验当前点生成的锚点是否在pdf内,防止自动限制在pdf页影响后边的偏移校验
     * @param rectPDF 整页PDF的rectf框
     * @param pointF 屏幕点击的点,转换为pdf层
     * @return 处理完成的pdf层point
     */
    public static PointF checkPoint(RectF rectPDF, PointF pointF) {
        float f = hight/2;
        if (rectPDF.top-f<pointF.y){
            pointF.set(pointF.x,rectPDF.top-f);
        }
        if (rectPDF.bottom+f>pointF.y){
            pointF.set(pointF.x,rectPDF.bottom+f);
        }
        if (rectPDF.left+f>pointF.x){
            pointF.set(rectPDF.left+f,pointF.y);
        }
        if (rectPDF.right-f<pointF.x){
            pointF.set(rectPDF.right-f,pointF.y);
        }
        return pointF;
    }

    /**
     * 获取偏移之后的点
     * @param rect 重合锚点的rect
     * @param pdfPage 当前pdf页
     * @param rectPDF 整页PDF的rectf框
     * @param i 递归次数(为了方便递归,此处传0即可)
     * @return 偏移后的点
     */
    public static PointF getOverlapCenterPontF(RectF rect , PDFPage pdfPage, RectF rectPDF, int i) {
        float  hightOverlap = hight * 3 / 2;
        PointF p            = new PointF(rect.centerX(),rect.bottom-hight- i*hightOverlap);
        if (!checkOverlap(p,pdfPage,rectPDF)){
            return p;
        }else{
            float fx1  = p.x + hightOverlap;
            while(fx1 < rect.right+5/2*hight+i*hightOverlap){
                p.set(fx1,p.y);
                if (!checkOverlap(p,pdfPage,rectPDF)){
                    return p;
                }else{
                    fx1 = fx1+hightOverlap;
                }
            }
            float fy1  = p.y + hightOverlap;
            while(fy1 <= rect.top+5/2*hight+i*hightOverlap){
                p.set(p.x,fy1);
                if (!checkOverlap(p,pdfPage,rectPDF)){
                    return p;
                }else{
                    fy1 = fy1+hightOverlap;
                }
            }
            fx1 = p.x - hightOverlap;
            while(fx1 > rect.left-(5/2*hight+i*hightOverlap)){
                p.set(fx1 ,p.y);
                if (!checkOverlap(p,pdfPage,rectPDF)){
                    return p;
                }else{
                    fx1 = fx1-hightOverlap;
                }
            }
            fy1  = p.y - hightOverlap;
            while(fy1 > rect.bottom-(5/2*hight+ i * hightOverlap)){
                p.set(p.x,fy1);
                if (!checkOverlap(p,pdfPage,rectPDF)){
                    return p;
                }else{
                    fy1 = fy1-hightOverlap;
                }
            }
            fx1  = p.x + hightOverlap;
            while(fx1 < rect.centerX()){
                p.set(fx1 , p.y);
                if (!checkOverlap(p,pdfPage,rectPDF)){
                    return p;
                }else{
                    fx1 = fx1+hightOverlap;
                }
            }
        }
        i++;
        return getOverlapCenterPontF(rect,pdfPage, rectPDF, i);
    }

    /**
     * 校验传入的点(pdf点),新建锚点后,是否与当前页面其他的锚点重叠,是否超出边界
     * @return
     * @param pointF
     */
    private static boolean checkOverlap(PointF pointF, PDFPage pdfPage, RectF rectPDF){
        int size = 0;
        try {
            size = pdfPage.getAnnotCount();
            if (size > 0) {
                Annot annot;
                float rectTop;
                float rectBottom;
                float rectLeft;
                float rectRight;
                if ((pointF.x>rectPDF.right-hight/2)||(pointF.x<rectPDF.left+hight/2)||(pointF.y>rectPDF.top-hight/2)||(pointF.y<rectPDF.bottom+hight/2)){
                    //超出边界
                    return true;
                }
                for (int i = 0; i < size; i++) {
                    annot = pdfPage.getAnnot(i);
                    int type = annot.getType();
                    if (Annot.e_Line == type || Annot.e_Ink == type || Annot.e_Highlight == type){
                        continue;
                    }
                    com.foxit.sdk.common.fxcrt.RectF rect1 = annot.getRect();
                    RectF                            rect2 = AppUtil.toRectF(rect1);
                    rectTop = rect2.top + hight / 2;
                    rectBottom = rect2.bottom - hight / 2;
                    rectLeft = rect2.left - hight / 2;
                    rectRight = rect2.right + hight / 2;
                    if (rectLeft <= pointF.x && rectRight >= pointF.x && rectTop >= pointF.y && rectBottom <= pointF.y) {
                        return true;
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取重叠锚点的rectf
     * @param pointF  新增或移动后的锚点中心点
     * @param pdfPage 当前页的pdfPage
     * @param size 当前页的锚点数量
     * @return
     */
    public static RectF getOverlappingRect(PointF pointF, PDFPage pdfPage, int size) {
        for (int i = 0; i < size; i++) {
            Annot annot;
            float rectTop;
            float rectBottom;
            float rectLeft;
            float rectRight;
            try {
                annot = pdfPage.getAnnot(i);
                int type = annot.getType();
                if (Annot.e_Line == type || Annot.e_Ink == type || Annot.e_Highlight == type){
                    continue;
                }
                com.foxit.sdk.common.fxcrt.RectF rect1 = annot.getRect();
                RectF                            rect  = AppUtil.toRectF(rect1);
                rectTop = rect.top+hight/2;
                rectBottom = rect.bottom-hight/2;
                rectLeft = rect.left-hight/2;
                rectRight = rect.right+hight/2;
                if(rectLeft<=pointF.x &&rectRight>=pointF.x &&rectTop>=pointF.y&&rectBottom<=pointF.y){
                    return rect;
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}