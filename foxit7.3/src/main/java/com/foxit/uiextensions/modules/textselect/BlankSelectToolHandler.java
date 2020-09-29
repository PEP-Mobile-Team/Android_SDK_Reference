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
package com.foxit.uiextensions.modules.textselect;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.TextPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotEventListener;
import com.foxit.uiextensions.annots.note.NoteAnnotContent;
import com.foxit.uiextensions.annots.redaction.RedactModule;
import com.foxit.uiextensions.annots.redaction.RedactToolHandler;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.modules.panzoom.PanZoomModule;
import com.foxit.uiextensions.modules.signature.SignatureModule;
import com.foxit.uiextensions.modules.signature.SignatureToolHandler;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIToast;

import java.util.ArrayList;

public class BlankSelectToolHandler implements ToolHandler {

    private PDFViewCtrl mPdfViewCtrl;
    private AnnotMenu mAnnotationMenu;
    private AnnotEventListener mAnnotListener;

    private PointF mMenuPoint;
    private PointF mMenuPdfPoint;
    private RectF mMenuBox;
    private int mCurrentIndex;
    private boolean mIsMenuShow;
    public PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private Context mContext;

    public BlankSelectToolHandler(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        this.mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
        mUiExtensionsManager = uiExtensionsManager;
        mMenuPoint = null;
        mAnnotationMenu = new AnnotMenuImpl(context, mPdfViewCtrl);

        mAnnotListener = new AnnotEventListener() {
            @Override
            public void onAnnotAdded(PDFPage page, Annot annot) {
            }

            @Override
            public void onAnnotWillDelete(PDFPage page, Annot annot) {
            }

            @Override
            public void onAnnotDeleted(PDFPage page, Annot annot) {

            }

            @Override
            public void onAnnotModified(PDFPage page, Annot annot) {
            }

            @Override
            public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {
                if (currentAnnot != null && mIsMenuShow) {
                    mIsMenuShow = false;
                    mAnnotationMenu.dismiss();
                }
            }
        };

        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().registerAnnotEventListener(mAnnotListener);
        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerMenuEventListener(mMenuEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandlerChangedListener(mHandlerChangedListener);
        }
    }

    public void unload() {
        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().unregisterAnnotEventListener(mAnnotListener);
        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterMenuEventListener(mMenuEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandlerChangedListener(mHandlerChangedListener);
        }
    }

    protected AnnotMenu getAnnotationMenu() {
        return mAnnotationMenu;
    }

    @Override
    public String getType() {
        return TH_TYPE_BLANKSELECT;
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
        if (!mPdfViewCtrl.isPageVisible(mCurrentIndex)) return;
        dismissMenu();
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        PointF pointF = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        try {
            if (((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().getCurrentAnnot() != null) {
                ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().setCurrentAnnot(null);
                return false;
            }

            TextSelectToolHandler toolHandler = (TextSelectToolHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (toolHandler != null && toolHandler.getAnnotationMenu().isShowing()) {
                toolHandler.getAnnotationMenu().dismiss();
                return false;
            }

            if (mIsMenuShow) {
                mIsMenuShow = false;
                mAnnotationMenu.dismiss();
                return true;
            }

            mCurrentIndex = pageIndex;
            PointF pointPdfView = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(pointF, pointPdfView, mCurrentIndex);
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
            if (!page.isParsed()) {
                Progressive progressive = page.startParse(PDFPage.e_ParsePageNormal, null, false);
                int state = Progressive.e_ToBeContinued;
                while (state == Progressive.e_ToBeContinued) {
                    state = progressive.resume();
                }
            }
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);

            int index = textPage.getIndexAtPos(pointPdfView.x, pointPdfView.y, 10);
            if (index == -1 && (((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().canAddAnnot() || ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().canAddSignature())) {
                mIsMenuShow = true;
                mMenuPoint = new PointF(pointF.x, pointF.y);
                mMenuPdfPoint = new PointF(mMenuPoint.x, mMenuPoint.y);
                mPdfViewCtrl.convertPageViewPtToPdfPt(mMenuPdfPoint, mMenuPdfPoint, mCurrentIndex);

                mMenuBox = new RectF(pointF.x, pointF.y, pointF.x + 1, pointF.y + 1);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mMenuBox, mMenuBox, mCurrentIndex);

                mAnnotationMenu.setMenuItems(getBlankSelectItems());
                mAnnotationMenu.show(mMenuBox);
                mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
                    @Override
                    public void onAMClick(int btType) {
                        if (btType == AnnotMenu.AM_BT_NOTE) {
                            PDFPage pdfPage = null;
                            try {
                                pdfPage = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
                            } catch (PDFException e1) {
                                e1.printStackTrace();
                            }
                            if (pdfPage == null || pdfPage.isEmpty()) return;
                            PointF p = new PointF(mMenuPdfPoint.x, mMenuPdfPoint.y);
                            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().addAnnot(page, new TextAnnotContent(p, mCurrentIndex), true, null);
                        } else if (btType == AnnotMenu.AM_BT_SIGNATURE) {
                            Module module = ((UIExtensionsManager) mUiExtensionsManager).getModuleByName(Module.MODULE_NAME_PSISIGNATURE);
                            if (module != null) {
                                SignatureToolHandler toolHandler = (SignatureToolHandler) ((SignatureModule) module).getToolHandler();
                                ((UIExtensionsManager) mUiExtensionsManager).setCurrentToolHandler(toolHandler);
                                PointF p = new PointF(mMenuPdfPoint.x, mMenuPdfPoint.y);
                                mPdfViewCtrl.convertPdfPtToPageViewPt(p, p, mCurrentIndex);
                                toolHandler.addSignature(mCurrentIndex, p, true);
                            }
                        } else if (btType == AnnotMenu.AM_BT_REDACT) {
                            if (AppAnnotUtil.hasModuleLicenseRight(Constants.e_ModuleNameRedaction)) {
                                Module module = ((UIExtensionsManager) mUiExtensionsManager).getModuleByName(Module.MODULE_NAME_REDACT);
                                if (module != null) {
                                    RedactToolHandler toolHandler = (RedactToolHandler) ((RedactModule) module).getToolHandler();
                                    ((UIExtensionsManager) mUiExtensionsManager).setCurrentToolHandler(toolHandler);
                                }
                            } else {
                                String message = AppUtil.getMessage(mContext, Constants.e_ErrNoRedactionModuleRight);
                                UIToast.getInstance(mContext).show(message);
                            }
                        }

                        mAnnotationMenu.dismiss();
                        mIsMenuShow = false;
                        mMenuPoint = null;
                    }
                });
                return true;
            }
        } catch (PDFException exception) {
            if (exception.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        if (mIsMenuShow) {
            mIsMenuShow = false;
            mAnnotationMenu.dismiss();
            return true;
        }
        return false;
    }

    @Override
    public boolean isContinueAddAnnot() {
        return false;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
    }

    @Override
    public void onDraw(int i, Canvas canvas) {
        onDrawForAnnotMenu(canvas);
    }

    private ArrayList<Integer> getBlankSelectItems() {
        ArrayList<Integer> items = new ArrayList<Integer>();

        if (((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().canAddAnnot()
                && ((UIExtensionsManager) mUiExtensionsManager).getConfig().modules.getAnnotConfig().isLoadNote()
                && ((UIExtensionsManager) mUiExtensionsManager).getAnnotHandlerByType(Annot.e_Note) != null) {
            items.add(AnnotMenu.AM_BT_NOTE);
        }
        if (((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().canAddSignature()
                && ((UIExtensionsManager) mUiExtensionsManager).getModuleByName(Module.MODULE_NAME_PSISIGNATURE) != null) {
            PanZoomModule panZoomModule = (PanZoomModule) ((UIExtensionsManager) mUiExtensionsManager).getModuleByName(Module.MODULE_NAME_PANZOOM);
            if (panZoomModule == null || !panZoomModule.isInPanZoomMode()) {
                items.add(AnnotMenu.AM_BT_SIGNATURE);
            }
        }

        if (((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().canAddAnnot()
                && ((UIExtensionsManager) mUiExtensionsManager).getConfig().modules.getAnnotConfig().isLoadRedaction()
                && ((UIExtensionsManager) mUiExtensionsManager).getAnnotHandlerByType(Annot.e_Redact) != null) {
            items.add(AnnotMenu.AM_BT_REDACT);
        }
        return items;
    }

    public void dismissMenu() {
        if (mIsMenuShow) {
            mIsMenuShow = false;
            mAnnotationMenu.dismiss();
        }
    }

    private void onDrawForAnnotMenu(Canvas canvas) {
        if (!mPdfViewCtrl.isPageVisible(mCurrentIndex)) {
            return;
        }

        if (!mIsMenuShow) {
            return;
        }

        if (mMenuPoint != null) {
            PointF temp = new PointF(mMenuPdfPoint.x, mMenuPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(mMenuPdfPoint, temp, mCurrentIndex);
            RectF bboxRect = new RectF(temp.x, temp.y, temp.x + 1, temp.y + 1);

            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bboxRect, bboxRect, mCurrentIndex);
            mAnnotationMenu.update(bboxRect);
        }
    }

    private UIExtensionsManager.MenuEventListener mMenuEventListener = new UIExtensionsManager.MenuEventListener() {
        @Override
        public void onTriggerDismissMenu() {
            dismissMenu();
        }
    };

    private UIExtensionsManager.ToolHandlerChangedListener mHandlerChangedListener = new UIExtensionsManager.ToolHandlerChangedListener() {
        @Override
        public void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
            if (currentTool != null && mIsMenuShow) {
                mAnnotationMenu.dismiss();
                mIsMenuShow = false;
            }
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            return onKeyBack();
        }
        return false;
    }

    public boolean onKeyBack() {
        if (mIsMenuShow) {
            mIsMenuShow = false;
            mAnnotationMenu.dismiss();
            return true;
        }
        return false;
    }
}

class TextAnnotContent implements NoteAnnotContent {
    private PointF p = new PointF();
    private int pageIndex;

    public TextAnnotContent(PointF p, int pageIndex) {
        this.p.set(p.x, p.y);
        this.pageIndex = pageIndex;
    }

    @Override
    public int getPageIndex() {
        return pageIndex;
    }

    @Override
    public int getType() {
        return Annot.e_Note;
    }

    @Override
    public String getNM() {
        return null;
    }

    @Override
    public RectF getBBox() {
        return new RectF(p.x, p.y, p.x, p.y);
    }

    @Override
    public int getColor() {
        return 0;
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public float getLineWidth() {
        return 0;
    }

    @Override
    public String getSubject() {
        return null;
    }

    @Override
    public DateTime getModifiedDate() {
        return null;
    }

    @Override
    public String getContents() {
        return null;
    }

    @Override
    public String getIntent() {
        return null;
    }

    @Override
    public String getIcon() {
        return "";
    }

    @Override
    public String getFromType() {
        return Module.MODULE_NAME_SELECTION;
    }

    @Override
    public String getParentNM() {
        return null;
    }
}
