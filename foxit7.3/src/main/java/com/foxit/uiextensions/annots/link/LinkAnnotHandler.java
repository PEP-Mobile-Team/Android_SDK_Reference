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
package com.foxit.uiextensions.annots.link;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.FileSpec;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.actions.Action;
import com.foxit.sdk.pdf.actions.Destination;
import com.foxit.sdk.pdf.actions.GotoAction;
import com.foxit.sdk.pdf.actions.LaunchAction;
import com.foxit.sdk.pdf.actions.RemoteGotoAction;
import com.foxit.sdk.pdf.actions.URIAction;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Link;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppIntentUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.OnPageEventListener;

import java.io.File;
import java.util.ArrayList;

class LinkAnnotHandler implements AnnotHandler {
    protected Context mContext;
    protected boolean isDocClosed = false;
    private Paint mPaint;
    private final int mType;
    private PDFViewCtrl mPdfViewCtrl;
    private Destination mDestination;

    class LinkInfo {
        int pageIndex;
        ArrayList<Link> links;
    }

    protected LinkInfo mLinkInfo;

    LinkAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mPaint = new Paint();
//        mPaint.setARGB(0x16, 0x0, 0x7F, 0xFF);
        mType = Annot.e_Link;
        mLinkInfo = new LinkInfo();
        mLinkInfo.pageIndex = -1;
        mLinkInfo.links = new ArrayList<>();
    }

    private boolean isLoadLink(int pageIndex) {
        return mLinkInfo.pageIndex == pageIndex;
    }

    protected PDFViewCtrl.IPageEventListener getPageEventListener(){
        return mPageEventListener;
    }

    private OnPageEventListener mPageEventListener = new OnPageEventListener() {
        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {
            if (!success || mLinkInfo.pageIndex == -1 || index == dstIndex)
                return;

            if (mLinkInfo.pageIndex < Math.min(index, dstIndex) || mLinkInfo.pageIndex > Math.max(index, dstIndex))
                return;

            if (mLinkInfo.pageIndex == index) {
                mLinkInfo.pageIndex = dstIndex;
                return;
            }

            if (index > dstIndex) {
                mLinkInfo.pageIndex += 1;
            } else {
                mLinkInfo.pageIndex -= 1;
            }

        }

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {
            if (!success || mLinkInfo.pageIndex == -1)
                return;

            int count = pageIndexes.length;
            for(int i = 0; i < count; i++) {
                if (mLinkInfo.pageIndex == pageIndexes[i]) {
                    mLinkInfo.pageIndex = -1;
                    break;
                }
            }
            if (mLinkInfo.pageIndex == -1) {
                mLinkInfo.links.clear();
            }
            else
                mLinkInfo.pageIndex -= count;
        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
            if (!success || mLinkInfo.pageIndex == -1)
                return;
            if (mLinkInfo.pageIndex > dstIndex) {
                for (int i = 0; i < range.length / 2; i++) {
                    mLinkInfo.pageIndex += range[2 * i + 1];
                }
            }
        }
    };

    protected Destination getDestination() {
        return mDestination;
    }

    protected void setDestination(Destination mDestination) {
        this.mDestination = mDestination;
    }

    protected void reloadLinks(int pageIndex) {
        try {
            if (mPdfViewCtrl.getDoc() == null) return;
            clear();
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            int count = page.getAnnotCount();
            mLinkInfo.pageIndex = pageIndex;
            Annot annot = null;
            for (int i = 0; i < count; i++) {
                annot = page.getAnnot(i);
                if (annot == null || annot.isEmpty()) continue;
                if (annot.getType() == Annot.e_Link) {
                    mLinkInfo.links.add((Link) AppAnnotUtil.createAnnot(annot, Annot.e_Link));
                }
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getType() {
        return mType;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        try {
            if (annot.getType() == mType) {
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            return AppUtil.toRectF(annot.getRect());
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        return getAnnotBBox(annot).contains(point.x, point.y);
    }

    @Override
    public void onAnnotSelected(Annot annot, boolean reRender) {

    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {

    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {

    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (isDocClosed || mPdfViewCtrl.isDynamicXFA()) return;
        if (!((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).isLinkHighlightEnabled())return;

        if (!isLoadLink(pageIndex)) {
            reloadLinks(pageIndex);
        }

        if (mLinkInfo.links.size() == 0) return;

        canvas.save();
        Rect clipRect = canvas.getClipBounds();
        Rect rect = new Rect();
        try {
            int count = mLinkInfo.links.size();
            mPaint.setColor((int) ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getLinkHighlightColor());

            for (int i = 0; i < count; i++) {
                Annot annot = mLinkInfo.links.get(i);
                RectF rectF = AppUtil.toRectF(annot.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                rectF.round(rect);
                if (rect.intersect(clipRect)) {
                    canvas.drawRect(rect, mPaint);
                }
            }
            canvas.restore();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).isLinksEnabled())
            return false;

        if (!mPdfViewCtrl.isPageVisible(pageIndex) || mPdfViewCtrl.getUIExtensionsManager() == null) return false;
        UIExtensionsManager.ILinkEventListener ILinkEventListener = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getLinkEventListener();
        if (ILinkEventListener != null) {
            UIExtensionsManager.LinkInfo emLinkInfo = new UIExtensionsManager.LinkInfo();
            emLinkInfo.link = annot;
            emLinkInfo.linkType = UIExtensionsManager.LINKTYPE_ANNOT;
            if (ILinkEventListener.onLinkTapped(emLinkInfo)) return true;
        }

        try {
            Action annotAction = ((Link) annot).getAction();
            if (annotAction == null || annotAction.isEmpty()) {
                return false;
            }
            int type = annotAction.getType();
            switch (type) {
                case Action.e_TypeGoto: {
                    GotoAction gotoAction = new GotoAction(annotAction);
                    Destination destination = gotoAction.getDestination();
                    if (destination == null || destination.isEmpty()){
                        mPdfViewCtrl.gotoPage(0, 0, 0);
                    }else{
                        PointF destPt = AppUtil.getDestinationPoint(mPdfViewCtrl, destination);
                        PointF devicePt = new PointF();
                        int _pageIndex = destination.getPageIndex(mPdfViewCtrl.getDoc());
                        if (!mPdfViewCtrl.convertPdfPtToPageViewPt(destPt, devicePt, _pageIndex)) {
                            devicePt.set(0, 0);
                        }
                        mPdfViewCtrl.gotoPage(_pageIndex, devicePt.x, devicePt.y);
                    }
                    break;
                }
                case Action.e_TypeURI: {
                    Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                    if (context == null) return false;
                    URIAction uriAction = new URIAction(annotAction);
                    String uri = uriAction.getURI();
                    if (uri.toLowerCase().startsWith("mailto:")) {
                        AppUtil.mailTo((Activity) context, uri);
                    } else {
                        AppUtil.openUrl((Activity) context, uri);
                    }
                    break;
                }
                case Action.e_TypeLaunch:
                case Action.e_TypeGoToR: {
                    FileSpec fileSpec;
                    RemoteGotoAction remoteGotoAction = null;
                    if (type == Action.e_TypeGoToR) {
                        remoteGotoAction = new RemoteGotoAction(annotAction);
                        fileSpec = remoteGotoAction.getFileSpec();
                    } else {
                        LaunchAction launchAction = new LaunchAction(annotAction);
                        fileSpec = launchAction.getFileSpec();
                    }

                    if (fileSpec == null || fileSpec.isEmpty()) return false;
                    String fileName = fileSpec.getFileName();
                    String path = mPdfViewCtrl.getFilePath();
                    if (path != null && path.lastIndexOf("/") > 0) {
                        path = path.substring(0, path.lastIndexOf("/") + 1);
                        path += fileName;
                        File file = new File(path);
                        if (file.exists() && file.isFile()) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).exitPanZoomMode();
                            if (path.length() > 4) {
                                String subfix = path.substring(path.length() - 4);
                                if (subfix.equalsIgnoreCase(".pdf")) {
                                    mPdfViewCtrl.cancelAllTask();
                                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().clearUndoRedo();
                                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(false);

                                    boolean isNullDestination = false;
                                    if (type == Action.e_TypeGoToR) {
                                        Destination destination = remoteGotoAction.getDestination();
                                        if (destination == null || destination.isEmpty()){
                                            isNullDestination = true;
                                        }
                                        setDestination(destination);
                                    }
                                    mPdfViewCtrl.openDoc(path, null);
                                    return !isNullDestination;
                                }
                            }
                            Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                            if (context == null) return false;
                            AppIntentUtil.openFile((Activity) context, path);
                            return true;
                        }
                        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                        if (context == null) return false;
                        AppUtil.alert((Activity) context,
                                AppResource.getString(mContext.getApplicationContext(), R.string.annot_link_alert_title),
                                AppResource.getString(mContext.getApplicationContext(), R.string.annot_link_alert_prompt),
                                AppUtil.ALERT_OK);
                    }
                    return true;
                }
                case Action.e_TypeUnknown:
                    return false;
                default:
            }

        } catch (PDFException e1) {
            if (e1.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            e1.printStackTrace();
            return true;
        }

        return true;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }

    protected void clear() {
        synchronized (mLinkInfo) {
            mLinkInfo.pageIndex = -1;
            mLinkInfo.links.clear();
        }
    }
}
