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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.TextPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotEventListener;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContentAbs;
import com.foxit.uiextensions.annots.textmarkup.TextSelector;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.modules.tts.TTSInfo;
import com.foxit.uiextensions.modules.tts.TTSModule;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.UIToast;

import java.util.ArrayList;

public class TextSelectToolHandler implements ToolHandler {
    private static final int HANDLE_AREA = 10;

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    final TextSelector mSelectInfo;
    private int mCurrentIndex;
    private RectF mTmpRect;
    private Paint mPaint;
    private Bitmap mHandlerBitmap;
    boolean mIsEdit;
    public AnnotMenu mAnnotationMenu;
    private ArrayList<Integer> mText;
    private ArrayList<Integer> mTTS;

    private AnnotEventListener mAnnotListener;
    private RectF mMenuBox;

    public TextSelectToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
        mSelectInfo = new TextSelector(pdfViewCtrl);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

        mTmpRect = new RectF();
        mHandlerBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.rv_textselect_handler);
        mAnnotationMenu = new AnnotMenuImpl(context, mPdfViewCtrl);

        mIsEdit = false;
        mText = new ArrayList<Integer>();
        mTTS = new ArrayList<>();

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
                if (currentAnnot != null && mIsEdit) {
                    RectF rectF = new RectF(mSelectInfo.getBbox());
                    mSelectInfo.clear();
                    if (!mPdfViewCtrl.isPageVisible(mCurrentIndex)) return;
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, mCurrentIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
                    RectF rF = AppUtil.calculateRect(rectF, mTmpRect);
                    Rect rect = new Rect();
                    rF.roundOut(rect);
                    getInvalidateRect(rect);
                    mPdfViewCtrl.invalidate(rect);
                    mIsEdit = false;
                    mAnnotationMenu.dismiss();
                }
            }
        };

        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().registerAnnotEventListener(mAnnotListener);
    }

    protected AnnotMenu getAnnotationMenu() {
        return mAnnotationMenu;
    }

    private UIExtensionsManager.ToolHandlerChangedListener mHandlerChangedListener = new UIExtensionsManager.ToolHandlerChangedListener() {
        @Override
        public void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {

            if (currentTool != null && mIsEdit) {
                RectF rectF = new RectF(mSelectInfo.getBbox());
                mSelectInfo.clear();
                mPdfViewCtrl.convertPdfRectToPageViewRect(mSelectInfo.getBbox(), rectF, mCurrentIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
                RectF rF = AppUtil.calculateRect(rectF, mTmpRect);
                Rect rect = new Rect();
                rF.roundOut(rect);
                rect.top -= mHandlerBitmap.getHeight();
                rect.bottom += mHandlerBitmap.getHeight();
                rect.left -= mHandlerBitmap.getWidth() / 2;
                rect.right += mHandlerBitmap.getWidth() / 2;
                mPdfViewCtrl.invalidate(rect);
                mIsEdit = false;
                mAnnotationMenu.dismiss();
            }
        }
    };

    protected UIExtensionsManager.ToolHandlerChangedListener getHandlerChangedListener() {
        return mHandlerChangedListener;
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_TEXTSELECT;
    }


    public String getCurrentSelectedText() {
        return mSelectInfo.getContents();
    }


    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
        RectF rectF = new RectF(mSelectInfo.getBbox());
        mSelectInfo.clear();
        if (!mPdfViewCtrl.isPageVisible(mCurrentIndex)) return;
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, mCurrentIndex);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
        RectF rF = AppUtil.calculateRect(rectF, mTmpRect);
        Rect rect = new Rect();
        rF.roundOut(rect);
        getInvalidateRect(rect);
        mPdfViewCtrl.invalidate(rect);
        mAnnotationMenu.dismiss();
        mIsEdit = false;
    }

    public void uninit() {
        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().unregisterAnnotEventListener(mAnnotListener);
    }

    public void reloadres() {
        mText.clear();
        mTTS.clear();

        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        if (uiExtensionsManager.getDocumentManager().canCopy()) {
            mText.add(AnnotMenu.AM_BT_COPY);
        }
        if (uiExtensionsManager.getDocumentManager().canAddAnnot()
                && uiExtensionsManager.getAnnotHandlerByType(Annot.e_Highlight) != null)
            mText.add(AnnotMenu.AM_BT_HIGHLIGHT);
        if (uiExtensionsManager.getDocumentManager().canAddAnnot()
                && uiExtensionsManager.getAnnotHandlerByType(Annot.e_Underline) != null)
            mText.add(AnnotMenu.AM_BT_UNDERLINE);
        if (uiExtensionsManager.getConfig().modules.getAnnotConfig().isLoadStrikeout()
                && uiExtensionsManager.getDocumentManager().canAddAnnot()
                && uiExtensionsManager.getAnnotHandlerByType(Annot.e_StrikeOut) != null)
            mText.add(AnnotMenu.AM_BT_STRIKEOUT);
        if (uiExtensionsManager.getDocumentManager().canAddAnnot()
                && uiExtensionsManager.getAnnotHandlerByType(Annot.e_Squiggly) != null)
            mText.add(AnnotMenu.AM_BT_SQUIGGLY);
        if (uiExtensionsManager.getConfig().modules.getAnnotConfig().isLoadRedaction()
                && uiExtensionsManager.getDocumentManager().canAddAnnot()
                && uiExtensionsManager.getAnnotHandlerByType(Annot.e_Redact) != null) {
            mText.add(AnnotMenu.AM_BT_REDACT);
        }
        if (uiExtensionsManager.getDocumentManager().canCopy()
                && uiExtensionsManager.getModuleByName(Module.MODULE_NAME_TTS) != null
                && ((TTSModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_TTS)).isSupperTts()) {
            mText.add(AnnotMenu.AM_BT_TTS);
        }

        mTTS.add(AnnotMenu.AM_BT_TTS_STRING);
        mTTS.add(AnnotMenu.AM_BT_TTS_START);
    }

    int[] enSeparatorList = {0, 10, 13, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 58, 59, 60, 61, 62, 63, 64};

    private void findEnWord(int pageIndex, TextSelector info, int index) {
        info.setStart(index);
        info.setEnd(index);
        try {
            PDFPage page = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getPage(pageIndex, false);
            if (page == null || page.isEmpty()) return;
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);

            String charInfo = null;
            for (; info.getStart() >= 0; info.setStart(info.getStart() - 1)) {
                charInfo = textPage.getChars(info.getStart(), 1);
                if (charInfo == null || charInfo.isEmpty()) {
                    info.setStart(info.getStart() + 1);
                    break;
                }
                int i;
                for (i = 0; i < enSeparatorList.length; i++) {
                    if (enSeparatorList[i] == charInfo.charAt(0)) {
                        break;
                    }
                }
                if (i != enSeparatorList.length) {
                    info.setStart(info.getStart() + 1);
                    break;
                }
            }

            if (info.getStart() < 0) {
                info.setStart(0);
            }

            for (; ; info.setEnd(info.getEnd() + 1)) {
                charInfo = textPage.getChars(info.getEnd(), 1);
                if (charInfo == null || charInfo.isEmpty()) {
                    info.setEnd(info.getEnd() - 1);
                    break;
                }
                int i;
                for (i = 0; i < enSeparatorList.length; i++) {
                    if (enSeparatorList[i] == charInfo.charAt(0)) {
                        break;
                    }
                }
                if (i != enSeparatorList.length) {
                    info.setEnd(info.getEnd() - 1);
                    break;
                }
            }
            if (charInfo == null || charInfo.isEmpty()) {
                info.setEnd(info.getEnd() - 1);
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    int[] chPassList = {0, 10, 13, 32};

    private void findChWord(int pageIndex, TextSelector info, int index) {
        info.setStart(index);
        info.setEnd(index);
        info.setStart(info.getStart() - 1);
        info.setEnd(info.getEnd() + 1);
        try {
            PDFPage page = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getPage(pageIndex, false);
            if (page == null || page.isEmpty()) return;
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);

            String charinfo = null;
            charinfo = textPage.getChars(index, 1);

            for (; info.getStart() >= 0; info.setStart(info.getStart() - 1)) {
                charinfo = textPage.getChars(info.getStart(), 1);
                if (charinfo == null || charinfo.isEmpty()) {
                    info.setStart(info.getStart() + 1);
                    break;
                }

                int i;
                for (i = 0; i < chPassList.length; i++) {
                    if (chPassList[i] == charinfo.charAt(0)) {
                        break;
                    }
                }
                if (i != chPassList.length) {
                    info.setStart(info.getStart() + 1);
                    break;
                }
            }
            if (info.getStart() < 0) {
                info.setStart(0);
            }

            for (; info.getEnd() >= 0; info.setEnd(info.getEnd() + 1)) {
                charinfo = textPage.getChars(info.getEnd(), 1);
                if (charinfo == null || charinfo.isEmpty()) {
                    info.setEnd(info.getEnd() - 1);
                    break;
                }

                int i;
                for (i = 0; i < chPassList.length; i++) {
                    if (chPassList[i] == charinfo.charAt(0)) {
                        break;
                    }
                }
                if (i != chPassList.length) {
                    info.setEnd(info.getEnd() - 1);
                    break;
                }
            }
            if (charinfo == null || charinfo.isEmpty()) {
                info.setEnd(info.getEnd() - 1);
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    private int ctrlPoint = 0;


    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF point = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, point, pageIndex);
        int action = motionEvent.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                ctrlPoint = isControlPoint(mCurrentIndex, point);
                if (ctrlPoint != 0) {
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                mAnnotationMenu.dismiss();
                if (ctrlPoint != 0) {
                    OnSelectMove(pageIndex, point, mSelectInfo);
                    final PDFPage page = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getPage(mCurrentIndex, false);
                    mSelectInfo.computeSelected(page, mSelectInfo.getStart(), mSelectInfo.getEnd());
                    invalidateTouch(mCurrentIndex, mSelectInfo);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsEdit) {
                    mText.clear();
                    reloadres();
                    if (mText.size() == 0) return false;
                    mAnnotationMenu.setMenuItems(mText);
                    mMenuBox = new RectF();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mSelectInfo.getBbox(), mMenuBox, mCurrentIndex);
                    mMenuBox.inset(-10, -10);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mMenuBox, mMenuBox, mCurrentIndex);
                    mAnnotationMenu.show(mMenuBox);
                    return false;
                }
                break;
            default:
                break;
        }

        return false;
    }

    @Override
    public boolean onLongPress(final int pageIndex, MotionEvent motionEvent) {
        PointF pointF = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        try {
            final UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
            if (uiExtensionsManager.getDocumentManager().getCurrentAnnot() != null) {
                uiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                return false;
            }

            if (mIsEdit) {
                RectF rectF = new RectF(mSelectInfo.getBbox());
                mSelectInfo.clear();
                mCurrentIndex = pageIndex;
                mPdfViewCtrl.convertPdfRectToPageViewRect(mSelectInfo.getBbox(), rectF, mCurrentIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
                RectF rF = AppUtil.calculateRect(rectF, mTmpRect);
                Rect rect = new Rect();
                rF.roundOut(rect);
                getInvalidateRect(rect);
                mPdfViewCtrl.invalidate(rect);
                mIsEdit = false;
                mAnnotationMenu.dismiss();
                return true;
            }
            if (mAnnotationMenu.isShowing()) {
                mAnnotationMenu.dismiss();
            }

            mCurrentIndex = pageIndex;
            PointF pointPdfView = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(pointF, pointPdfView, mCurrentIndex);
            final PDFPage page = uiExtensionsManager.getDocumentManager().getPage(mCurrentIndex, false);
            if (page == null || page.isEmpty())
                return false;

            final TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);
            int index = textPage.getIndexAtPos(pointPdfView.x, pointPdfView.y, 10);

            if (index == -1) {
                return true;
            }
            String info = textPage.getChars(index, 1);
            if (info.length() == 0) return false;
            if (index >= 0) {
                reloadres();
                if (mText.size() == 0)
                    return false;
                if ((info.charAt(0) >= 65 && info.charAt(0) <= 90) || (info.charAt(0) >= 97 && info.charAt(0) <= 122)) {
                    findEnWord(mCurrentIndex, mSelectInfo, index);
                } else {
                    findChWord(mCurrentIndex, mSelectInfo, index);
                }
                mSelectInfo.computeSelected(page, mSelectInfo.getStart(), mSelectInfo.getEnd());
                invalidateTouch(mCurrentIndex, mSelectInfo);
                mIsEdit = true;
            } else {
                mIsEdit = false;
            }
            if (mSelectInfo.getRectFList().size() == 0) {
                mIsEdit = false;
            }
            if (mIsEdit) {
                mMenuBox = new RectF(mSelectInfo.getBbox());
                mPdfViewCtrl.convertPdfRectToPageViewRect(mMenuBox, mMenuBox, mCurrentIndex);
                mMenuBox.inset(-10, -10);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mMenuBox, mMenuBox, mCurrentIndex);
                mAnnotationMenu.setMenuItems(mText);
                mAnnotationMenu.show(mMenuBox);

                mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
                    @Override
                    public void onAMClick(final int btType) {
                        if (btType == AnnotMenu.AM_BT_COPY) {
                            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setPrimaryClip(ClipData.newPlainText(null, mSelectInfo.getText(page)));
                            AppAnnotUtil.toastAnnotCopy(mContext);
                            RectF rectF = new RectF(mSelectInfo.getBbox());
                            mSelectInfo.clear();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, mCurrentIndex);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
                            RectF rF = AppUtil.calculateRect(rectF, mTmpRect);
                            Rect rect = new Rect();
                            rF.roundOut(rect);
                            getInvalidateRect(rect);
                            mPdfViewCtrl.invalidate(rect);
                            mIsEdit = false;
                            mAnnotationMenu.dismiss();
                            return;
                        }

                        if (btType == AnnotMenu.AM_BT_TTS) {
                            mIsEdit = true;
                            mAnnotationMenu.setMenuItems(mTTS);
                            mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
                                @Override
                                public void onAMClick(int btType) {
                                    mIsEdit = false;
                                    mAnnotationMenu.dismiss();
                                    TTSInfo ttsInfo = new TTSInfo();
                                    ttsInfo.mText = mSelectInfo.getText(page);
                                    ttsInfo.mStart = mSelectInfo.getStart();
                                    RectF rectF = new RectF(mSelectInfo.getBbox());
                                    ttsInfo.mRect = rectF;
                                    ttsInfo.mPageIndex = pageIndex;

                                    ArrayList<RectF> seletRectFList = mSelectInfo.getRectFList();
                                    int rectSize = seletRectFList.size();
                                    for (int i = 0; i < rectSize; i++) {
                                        RectF selectRect = seletRectFList.get(i);
                                        ttsInfo.mRects.add(new RectF(selectRect.left, selectRect.top, selectRect.right, selectRect.bottom));
                                    }

                                    mSelectInfo.clear();
                                    if (mPdfViewCtrl.isPageVisible(mCurrentIndex)) {
                                        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, mCurrentIndex);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
                                        RectF rF = AppUtil.calculateRect(rectF, mTmpRect);
                                        Rect rect = new Rect();
                                        rF.roundOut(rect);
                                        getInvalidateRect(rect);
                                        mPdfViewCtrl.invalidate(rect);
                                    }

                                    TTSModule ttsModule = (TTSModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_TTS);
                                    if (btType == AnnotMenu.AM_BT_TTS_STRING) {
                                        if (!AppUtil.isEmpty(ttsInfo.mText)) {
                                            ttsModule.speakFromTs(ttsInfo);
                                        }

                                    } else if (btType == AnnotMenu.AM_BT_TTS_START) {
                                        if (!AppUtil.isEmpty(ttsInfo.mText)) {
                                            ttsModule.speakFromTp(ttsInfo);
                                        }
                                    }
                                }
                            });

                            mAnnotationMenu.dismiss();
                            mAnnotationMenu.show(mMenuBox);
                            return;
                        }

                        if (btType == AnnotMenu.AM_BT_HIGHLIGHT) {
                            uiExtensionsManager.getDocumentManager().addAnnot(page, new TextMarkupContentAbs() {
                                @Override
                                public TextSelector getTextSelector() {
                                    mSelectInfo.setContents(mSelectInfo.getText(page));
                                    return mSelectInfo;
                                }

                                @Override
                                public int getPageIndex() {
                                    return mCurrentIndex;
                                }

                                @Override
                                public int getType() {
                                    return Annot.e_Highlight;
                                }

                                @Override
                                public String getIntent() {
                                    return null;
                                }

                            }, true, mAddResult);

                        } else if (btType == AnnotMenu.AM_BT_UNDERLINE) {
                            uiExtensionsManager.getDocumentManager().addAnnot(page, new TextMarkupContentAbs() {
                                @Override
                                public TextSelector getTextSelector() {
                                    mSelectInfo.setContents(mSelectInfo.getText(page));
                                    return mSelectInfo;
                                }

                                @Override
                                public int getPageIndex() {
                                    return mCurrentIndex;
                                }

                                @Override
                                public int getType() {
                                    return Annot.e_Underline;
                                }

                                @Override
                                public String getIntent() {
                                    return null;
                                }
                            }, true, mAddResult);

                        } else if (btType == AnnotMenu.AM_BT_STRIKEOUT) {
                            uiExtensionsManager.getDocumentManager().addAnnot(page, new TextMarkupContentAbs() {
                                @Override
                                public TextSelector getTextSelector() {
                                    mSelectInfo.setContents(mSelectInfo.getText(page));
                                    return mSelectInfo;
                                }

                                @Override
                                public int getPageIndex() {
                                    return mCurrentIndex;
                                }

                                @Override
                                public int getType() {
                                    return Annot.e_StrikeOut;
                                }

                                @Override
                                public String getIntent() {
                                    return null;
                                }

                            }, true, mAddResult);

                        } else if (btType == AnnotMenu.AM_BT_SQUIGGLY) {
                            uiExtensionsManager.getDocumentManager().addAnnot(page, new TextMarkupContentAbs() {
                                @Override
                                public TextSelector getTextSelector() {
                                    mSelectInfo.setContents(mSelectInfo.getText(page));
                                    return mSelectInfo;
                                }

                                @Override
                                public int getPageIndex() {
                                    return mCurrentIndex;
                                }

                                @Override
                                public int getType() {
                                    return Annot.e_Squiggly;
                                }

                                @Override
                                public String getIntent() {
                                    return null;
                                }

                            }, true, mAddResult);
                        } else if (btType == AnnotMenu.AM_BT_REDACT) {
                            if (AppAnnotUtil.hasModuleLicenseRight(Constants.e_ModuleNameRedaction)) {
                                uiExtensionsManager.getDocumentManager().addAnnot(page, new TextMarkupContentAbs() {
                                    @Override
                                    public TextSelector getTextSelector() {
                                        mSelectInfo.setContents(mSelectInfo.getText(page));
                                        return mSelectInfo;
                                    }

                                    @Override
                                    public int getPageIndex() {
                                        return mCurrentIndex;
                                    }

                                    @Override
                                    public int getType() {
                                        return Annot.e_Redact;
                                    }

                                    @Override
                                    public String getIntent() {
                                        return null;
                                    }

                                }, true, mAddResult);
                            } else {
                                String message = AppUtil.getMessage(mContext, Constants.e_ErrNoRedactionModuleRight);
                                UIToast.getInstance(mContext).show(message);
                            }
                        }
                        mIsEdit = false;
                        mAnnotationMenu.dismiss();
                    }
                });
            }
        } catch (PDFException exception) {
            if (exception.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        if (mIsEdit) {
            RectF rectF = new RectF(mSelectInfo.getBbox());
            mSelectInfo.clear();
            mPdfViewCtrl.convertPdfRectToPageViewRect(mSelectInfo.getBbox(), rectF, mCurrentIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentIndex);
            RectF rF = AppUtil.calculateRect(rectF, mTmpRect);
            Rect rect = new Rect();
            rF.roundOut(rect);
            getInvalidateRect(rect);
            mPdfViewCtrl.invalidate(rect);
            mIsEdit = false;
            mAnnotationMenu.dismiss();

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isContinueAddAnnot() {
        return false;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
    }

    private Event.Callback mAddResult = new Event.Callback() {
        @Override
        public void result(Event event, boolean success) {
            mSelectInfo.clear();
        }
    };

    protected TextSelector getSelectInfo() {
        return mSelectInfo;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mCurrentIndex != pageIndex) return;
        if (mSelectInfo != null) {
            mPaint.setColor(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getSelectionHighlightColor());
            Rect clipRect = canvas.getClipBounds();
            for (RectF rect : mSelectInfo.getRectFList()) {
                RectF tmp = new RectF(rect);
                mPdfViewCtrl.convertPdfRectToPageViewRect(rect, tmp, mCurrentIndex);
                Rect r = new Rect();
                tmp.round(r);
                if (r.intersect(clipRect)) {
                    canvas.save();
                    canvas.drawRect(r, mPaint);
                    canvas.restore();
                }
            }
            if (mSelectInfo.getRectFList().size() > 0) {
                RectF start = new RectF(mSelectInfo.getRectFList().get(0));
                RectF end = new RectF(mSelectInfo.getRectFList().get(mSelectInfo.getRectFList().size() - 1));

                mPdfViewCtrl.convertPdfRectToPageViewRect(start, start, mCurrentIndex);
                mPdfViewCtrl.convertPdfRectToPageViewRect(end, end, mCurrentIndex);

                canvas.drawBitmap(mHandlerBitmap, start.left - mHandlerBitmap.getWidth(), start.top - mHandlerBitmap.getHeight(), null);
                canvas.drawBitmap(mHandlerBitmap, end.right, end.bottom, null);

                mPaint.setARGB(255, 76, 121, 164);
                canvas.drawLine(start.left, start.top - 1, start.left, start.bottom + 1, mPaint);
                canvas.drawLine(end.right, end.top - 1, end.right, end.bottom + 1, mPaint);
            }
        }

    }

    private boolean OnSelectMove(int pageIndex, PointF point, TextSelector selectInfo) {
        if (selectInfo == null || mCurrentIndex != pageIndex) return false;
        try {
            PDFPage page = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getPage(mCurrentIndex, false);
            if (page == null) return false;
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);
            mPdfViewCtrl.convertPageViewPtToPdfPt(point, point, mCurrentIndex);
            int index = textPage.getIndexAtPos(point.x, point.y, 10);
            if (index < 0) return false;
            if (ctrlPoint == 1) {
                if (index <= selectInfo.getEnd())
                    selectInfo.setStart(index);
            } else if (ctrlPoint == 2) {
                if (index >= selectInfo.getStart())
                    selectInfo.setEnd(index);
            }

        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
                return false;
            }
        }
        return true;
    }

    private void invalidateTouch(int pageIndex, TextSelector selectInfo) {
        if (selectInfo == null) return;
        RectF rectF = new RectF();
        mPdfViewCtrl.convertPdfRectToPageViewRect(mSelectInfo.getBbox(), rectF, pageIndex);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
        RectF rF = AppUtil.calculateRect(rectF, mTmpRect);
        Rect rect = new Rect();
        rF.roundOut(rect);
        getInvalidateRect(rect);
        mPdfViewCtrl.invalidate(rect);
        mTmpRect.set(rectF);
    }

    private int isControlPoint(int pageIndex, PointF point) {
        if (mSelectInfo != null && mSelectInfo.getRectFList().size() > 0) {
            RectF mStart = new RectF(mSelectInfo.getRectFList().get(0));
            RectF mEnd = new RectF(mSelectInfo.getRectFList().get(mSelectInfo.getRectFList().size() - 1));
            mPdfViewCtrl.convertPdfRectToPageViewRect(mStart, mStart, pageIndex);
            mPdfViewCtrl.convertPdfRectToPageViewRect(mEnd, mEnd, pageIndex);

            RectF startHandler = new RectF(mStart.left - mHandlerBitmap.getWidth(), mStart.top - mHandlerBitmap.getHeight(),
                    mStart.left, mStart.top);
            RectF endHandler = new RectF(mEnd.right, mEnd.bottom,
                    mEnd.right + mHandlerBitmap.getWidth(), mEnd.bottom + mHandlerBitmap.getHeight());
            startHandler.inset(-HANDLE_AREA, -HANDLE_AREA);
            endHandler.inset(-HANDLE_AREA, -HANDLE_AREA);
            if (startHandler.contains(point.x, point.y))
                return 1;
            if (endHandler.contains(point.x, point.y))
                return 2;
        }
        return 0;

    }

    public void dismissMenu() {
        if (mIsEdit) {
            RectF rectF = new RectF(mSelectInfo.getBbox());
            mSelectInfo.clear();
            int _pageIndex = mPdfViewCtrl.getCurrentPage();
            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, _pageIndex);
                RectF rF = AppUtil.calculateRect(rectF, mTmpRect);
                Rect rect = new Rect();
                rF.roundOut(rect);
                getInvalidateRect(rect);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, _pageIndex);
                mPdfViewCtrl.invalidate(rect);
            }
            mIsEdit = false;
            mAnnotationMenu.dismiss();
        }
    }

    public void getInvalidateRect(Rect rect) {
        rect.top -= mHandlerBitmap.getHeight();
        rect.bottom += mHandlerBitmap.getHeight();
        rect.left -= mHandlerBitmap.getWidth() / 2;
        rect.right += mHandlerBitmap.getWidth() / 2;
        rect.inset(-20, -20);
    }

    public void onDrawForAnnotMenu(Canvas canvas) {
        if (!mPdfViewCtrl.isPageVisible(mCurrentIndex)) {
            return;
        }
        if (!mIsEdit) {
            return;
        }

        RectF bboxRect = new RectF(mSelectInfo.getBbox());
        mPdfViewCtrl.convertPdfRectToPageViewRect(bboxRect, bboxRect, mCurrentIndex);
        bboxRect.inset(-10, -10);

        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bboxRect, bboxRect, mCurrentIndex);
        mAnnotationMenu.update(bboxRect);
    }

    protected boolean onKeyBack() {
        if (mIsEdit) {
            mIsEdit = false;
            mAnnotationMenu.dismiss();

            RectF rectF = new RectF(mSelectInfo.getBbox());
            mSelectInfo.clear();
            if (!mPdfViewCtrl.isPageVisible(mCurrentIndex))
                return true;
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, mCurrentIndex);
            RectF rF = AppUtil.calculateRect(rectF, mTmpRect);
            Rect rect = new Rect();
            rF.roundOut(rect);
            getInvalidateRect(rect);
            mPdfViewCtrl.invalidate(rect);

            return true;
        }
        return false;
    }
}
