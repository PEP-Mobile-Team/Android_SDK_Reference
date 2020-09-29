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
package com.foxit.uiextensions.annots.fileattachment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FileAttachment;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFileSelectDialog;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.io.File;
import java.io.FileFilter;



public class FileAttachmentToolHandler implements ToolHandler {

    private int	mColor;
    private int	mOpacity;
    private int mIconType;
    private String mPath;
    private String attachmentName;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private static final int MAX_ATTACHMENT_FILE_SIZE = 1024*1024*300;
    private boolean mIsContinuousCreate = false;

    public FileAttachmentToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_FILEATTACHMENT;
    }

    @Override
    public void onActivate() {

    }

    @Override
    public void onDeactivate() {

    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean isContinueAddAnnot() {
        return mIsContinuousCreate;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
        mIsContinuousCreate = continueAddAnnot;
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {

        int action = motionEvent.getActionMasked();
        PointF point = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);

        switch (action) {
            case MotionEvent.ACTION_DOWN:

                PDFPage page = null;
                RectF pageRect = new RectF();
                try {
                    page = mPdfViewCtrl.getDoc().getPage(pageIndex);

                    pageRect = new RectF(0, page.getHeight(), page.getWidth(), 0);

                } catch (PDFException e) {
                    e.printStackTrace();
                }
                if (point.x < pageRect.left) {
                    point.x = pageRect.left;
                }

                if (point.x > pageRect.right - 20) {
                    point.x = pageRect.right - 20;
                }

                if (point.y < 24) {
                    point.y = 24;
                }

                if (point.y > pageRect.top) {
                    point.y = pageRect.top;
                }


                showFileSelectDialog(pageIndex, point);
                break;
            default:
                break;
        }

        return true;
    }


    @Override
    public void onDraw(int pageIndex, Canvas canvas) {

    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                return true;
            }
        }
        return false;
    }

    public boolean onKeyBack() {
        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
            return true;
        }
        return false;
    }

    protected void addAnnot(final int pageIndex, final RectF rect, final Event.Callback result) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final FileAttachment annot = (FileAttachment) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FileAttachment, AppUtil.toFxRectF(rect)), Annot.e_FileAttachment);
            final FileAttachmentAddUndoItem undoItem = new FileAttachmentAddUndoItem(mPdfViewCtrl);

            undoItem.mPageIndex = pageIndex;
            undoItem.mNM = AppDmUtil.randomUUID(null);
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mFlags = Annot.e_FlagPrint | Annot.e_FlagNoRotate | Annot.e_FlagNoZoom;
            undoItem.mColor = mColor;
            undoItem.mOpacity = AppDmUtil.opacity100To255(mOpacity) / 255f;
            undoItem.mIconName = FileAttachmentUtil.getIconName(mIconType);
            undoItem.mPath = mPath;
            undoItem.attacheName = attachmentName;
            undoItem.mBBox = new RectF(rect);
            FileAttachmentEvent event = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);

            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            try {
                                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
                                RectF annotRectF = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                                Rect rect = new Rect();
                                annotRectF.roundOut(rect);
                                mPdfViewCtrl.refresh(pageIndex, rect);
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (result != null) {
                        result.result(event, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            if (!mIsContinuousCreate) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private UIFileSelectDialog mfileSelectDialog;
    private void showFileSelectDialog(final int pageIndex, final PointF pointf) {
        if (mfileSelectDialog != null && mfileSelectDialog.isShowing()) return;
        final PointF point = new PointF();
        if (pointf != null) {
            point.set(pointf.x, pointf.y);
        }

        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) {
            return;
        }

        mfileSelectDialog = new UIFileSelectDialog(context);
        mfileSelectDialog.init(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isHidden() || !pathname.canRead()) return false;
                return true;
            }
        }, true);
        mfileSelectDialog.setTitle(context.getApplicationContext().getString(R.string.fx_string_open));
        mfileSelectDialog.setButton(UIMatchDialog.DIALOG_CANCEL | UIMatchDialog.DIALOG_OK);
        mfileSelectDialog.setButtonEnable(false, UIMatchDialog.DIALOG_OK);
        mfileSelectDialog.setListener(new UIMatchDialog.DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == UIMatchDialog.DIALOG_OK) {
                    mPath = mfileSelectDialog.getSelectedFiles().get(0).path;
                    attachmentName = mfileSelectDialog.getSelectedFiles().get(0).name;
                    if (mPath == null || mPath.length() < 1) return;

                    //check file size
                    if (new File(mPath).length() > MAX_ATTACHMENT_FILE_SIZE) {
                        String msg = String.format(AppResource.getString(mContext.getApplicationContext(), R.string.annot_fat_filesizelimit_meg),
                                MAX_ATTACHMENT_FILE_SIZE / (1024 * 1024));
                        Toast toast = Toast.makeText(mContext.getApplicationContext(),
                                msg, Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
                    //add attachment
                    mfileSelectDialog.dismiss();
                    RectF rectF = new RectF(point.x, point.y, point.x + 20, point.y - 20);
                    addAnnot(pageIndex, rectF, null);

                } else if (btType == UIMatchDialog.DIALOG_CANCEL) {
                    mfileSelectDialog.dismiss();
                }
            }

            @Override
            public void onBackClick() {
            }
        });
        mfileSelectDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mfileSelectDialog.dismiss();
                }
                return true;
            }
        });

        mfileSelectDialog.showDialog(false);
    }

    public void setColor(int color) {
        mColor = color;
    }

    public int getColor() {
        return mColor;
    }

    public void setOpacity(int opacity) {
        mOpacity = opacity;
    }

    public int getOpacity() {
        return mOpacity;
    }

    public int getIconType() {
        return mIconType;
    }

    public void setIconType(int iconType) {
        mIconType = iconType;
    }

    protected void onConfigurationChanged(Configuration newConfig){
        if (mfileSelectDialog != null && mfileSelectDialog.isShowing()){
            mfileSelectDialog.setHeight(mfileSelectDialog.getDialogHeight());
            mfileSelectDialog.showDialog(false);
        }
    }

}
