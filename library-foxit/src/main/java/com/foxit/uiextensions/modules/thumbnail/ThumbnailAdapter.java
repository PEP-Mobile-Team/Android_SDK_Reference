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
package com.foxit.uiextensions.modules.thumbnail;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.common.Range;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFileSelectDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFolderSelectDialog;
import com.foxit.uiextensions.home.local.LocalModule;
import com.foxit.uiextensions.modules.DocInfoModule;
import com.foxit.uiextensions.modules.thumbnail.createpage.CreatePageBean;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import androidx.recyclerview.widget.RecyclerView;

public class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.ThumbViewHolder> implements ThumbnailItemTouchCallback.ItemTouchAdapter {
    final ArrayList<ThumbnailItem> mThumbnailList;
    final ArrayList<ThumbnailItem> mSelectedList;
    final ArrayList<ThumbnailItem> mCacheList;
    final private ArrayList<DrawThumbnailTask> mTaskList;
    private final PDFViewCtrl mPDFViewCtrl;
    private static final int REMOVE_PAGE = 0;
    private static final int ROTATE_PAGE_CW = 1;
    private static final int ROTATE_PAGE_ACW = 2;
    private static final int INSERT_FROM_DOCUMENT = 3;
    private static final int EXPORT_PAGE = 4;
    private static final int COPY_PAGES = 5;
    private static final int INSERT_PAGES = 6;
    private boolean isEditing = false;

    private final ThumbnailSupport mSupport;
    private int mTasksMax;
    private int mBitmapsMax;
    private int mCurrentPage;

    private ThumbnailAdapterCallback callback;
    private boolean flag;
    private int index;

    public ThumbnailAdapter(ThumbnailSupport support) {
        mSupport = support;
        mPDFViewCtrl = support.getPDFView();
        mCurrentPage = mPDFViewCtrl.getCurrentPage();
        mThumbnailList = new ArrayList<>();
        mSelectedList = new ArrayList<>();
        mCacheList = new ArrayList<>();
        mTaskList = new ArrayList<>();
        mCacheList.clear();
        mSelectedList.clear();
        mThumbnailList.clear();
        for (int i = 0; i < mPDFViewCtrl.getPageCount(); i++) {
            this.mThumbnailList.add(i, new ThumbnailItem(i, mSupport.getThumbnailBackgroundSize(), mPDFViewCtrl));
        }
        this.callback = support;
        this.flag = false;
    }

    public void prepareOnClickAdd(){
        flag = false;
    }

    public int getEditPosition() {
        if (flag){
            flag = false;
            return index;
        }

        if (mSelectedList == null || mSelectedList.size() <= 0) {
            return mThumbnailList.size();
        } else {
            int index = -1;
            for (ThumbnailItem item : mSelectedList) {
                if (item.getIndex() > index) {
                    index = item.getIndex();
                }
            }
            return index + 1;
        }
    }

    public void setCacheSize(int tasksMax, int bitmapsMax) {
        mTasksMax = tasksMax;
        mBitmapsMax = bitmapsMax;
    }

    public boolean isSelectedAll() {
        return mThumbnailList.size() == mSelectedList.size() && mSelectedList.size() != 0;
    }

    public void selectAll(boolean isSelect) {
        mSelectedList.clear();
        for (int i = 0; i < mThumbnailList.size(); i++) {
            ThumbnailItem thumbnailItem = mThumbnailList.get(i);
            updateSelectListInfo(thumbnailItem, isSelect);
        }
        notifyDataSetChanged();
    }

    private void addTask(DrawThumbnailTask task) {
        synchronized (mTaskList) {
            if (mTaskList.size() >= mTasksMax) {
                DrawThumbnailTask oldTask = null;
                int position = task.getThumbnailItem().getIndex();
                for (DrawThumbnailTask thumbnailTask : mTaskList) {
                    if (!mSupport.isThumbnailItemVisible(thumbnailTask.getThumbnailItem())) {
                        if(oldTask == null){
                            oldTask = thumbnailTask;
                        }else{
                            if((Math.abs(oldTask.getThumbnailItem().getIndex() - position)) < Math.abs(thumbnailTask.getThumbnailItem().getIndex() - position))
                                oldTask = thumbnailTask;
                        }
                        break;
                    }
                }
                if (oldTask == null) {
                    oldTask = mTaskList.get(0);
                }

                mPDFViewCtrl.removeTask(oldTask);
                mTaskList.remove(oldTask);
                oldTask.getThumbnailItem().resetRending(false);
            }
            mTaskList.add(task);
            mPDFViewCtrl.addTask(task);
        }
    }

    private void removeTask(DrawThumbnailTask task) {
        synchronized (mTaskList) {
            mTaskList.remove(task);
        }
    }

    public void updateCacheListInfo(ThumbnailItem value, boolean add) {
        if (add) {
            if (mCacheList.contains(value))
                return;
            if (mCacheList.size() >= mBitmapsMax) {
                ThumbnailItem item = mCacheList.get(0);
                item.setBitmap(null);
                mCacheList.remove(0);
            }
            mCacheList.add(value);
        } else {
            if (mCacheList.contains(value)) {
                mCacheList.remove(value);
                value.setBitmap(null);
            }
        }
    }

    public int getSelectedItemCount() {
        return mSelectedList.size();
    }

    public void updateSelectListInfo(ThumbnailItem item, boolean select) {
        if (select) {
            if (!mSelectedList.contains(item))
                mSelectedList.add(item);
            item.setSelected(true);
        } else {
            if (mSelectedList.contains(item))
                mSelectedList.remove(item);
            item.setSelected(false);
        }
    }

    @Override
    public ThumbViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.thumbnail_view, parent, false);
        return new ThumbViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ThumbViewHolder holder, int position) {
        ThumbnailItem thumbnailItem = mThumbnailList.get(position);
        holder.drawThumbnail(thumbnailItem, position);
    }

    @Override
    public int getItemCount() {
        return mThumbnailList.size();
    }

    public ThumbnailItem getThumbnailItem(int position) {
        if (position < 0 || position > getItemCount())
            return null;
        return mThumbnailList.get(position);
    }

    private void swap(int dst, int src) {
        Collections.swap(mThumbnailList, dst, src);
    }

    @Override
    public void onMove(int fromPosition, int toPosition) {
        if (((UIExtensionsManager)mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager().isXFA()) {
            UIToast.getInstance(mSupport.getContext()).show(mSupport.getContext().getApplicationContext().getString(R.string.xfa_not_support_move_toast));
            return;
        }
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                swap(i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                swap(i, i - 1);
            }
        }
        movePage(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void clear() {
        if (mSelectedList != null) {
            for (ThumbnailItem info : mSelectedList) {
                info.setSelected(false);
            }
            mSelectedList.clear();
        }
        if (mCacheList != null) {
            for (ThumbnailItem item : mCacheList) {
                item.setBitmap(null);
            }
            mCacheList.clear();
        }

//        for (ThumbnailItem item : mThumbnailList) {
//            item.closePage();
//        }

        if (mThumbnailList != null) {
            mThumbnailList.clear();
        }
    }

    public void removeSelectedPages() {
        if (mSelectedList.size() == 0)
            return;
        mSupport.getProgressDialog().setTips(AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.rv_page_delete));
        mSupport.getProgressDialog().show();

        final ArrayList<ThumbnailItem> tmpSelectList = new ArrayList<>();
        for (ThumbnailItem item : mSelectedList) {
            tmpSelectList.add(item);
        }
        doRemovePages(tmpSelectList, new EditThumbnailCallback() {
            @Override
            public void result(boolean success) {
                tmpSelectList.clear();
                mSupport.getProgressDialog().dismiss();
                notifyDataSetChanged();
            }
        });
    }

    //remove items form all item list and cache list but not selected item list,remove pages from doc.
    private void doRemovePages(final ArrayList<ThumbnailItem> itemList, final EditThumbnailCallback callback) {
        for (ThumbnailItem item : itemList) {
            if (item.isRendering()) {
                if(callback != null) {
                    callback.result(true);
                }
                return;
            }
        }
        EditThumbnailTask editThumbnailTask = new EditThumbnailTask(REMOVE_PAGE, itemList, callback);
        mPDFViewCtrl.addTask(editThumbnailTask);
    }

    private void doRotatePages(final ArrayList<ThumbnailItem> itemList, final boolean isClockWise, final EditThumbnailCallback callback) {
        for (ThumbnailItem item : itemList) {
            if (item.isRendering()) {
                if(callback != null) {
                    callback.result(true);
                }
                return;
            }
        }
        EditThumbnailTask editThumbnailTask = new EditThumbnailTask(isClockWise ? ROTATE_PAGE_CW : ROTATE_PAGE_ACW, itemList, callback);
        mPDFViewCtrl.addTask(editThumbnailTask);
    }

    public void copyPages(PDFDoc pdfdoc){
        final ArrayList<Integer> ranges = new ArrayList<>();
        for (ThumbnailItem item : mSelectedList) {
            if (item.isRendering()) return;
            ranges.add(item.getIndex());
        }
        Collections.sort(ranges);
        int[] range = new int[mSelectedList.size()*2];
        int index =0;
        for(int i=0;i<ranges.size()*2;i+=2)
        {
            range[i] = ranges.get(index);
            range[i+1] = 1;
            index++;
        }

        final FxProgressDialog progressDialog = mSupport.getProgressDialog();
        progressDialog.setTips(AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.fx_string_copying));
        progressDialog.show();
        doCopyPages(range,pdfdoc, new EditThumbnailCallback() {
            @Override
            public void result(boolean success) {
                progressDialog.dismiss();
                notifyDataSetChanged();
            }
        });
    }

    private void importDocument(final int dstIndex, final String filepath, String password) {
        try {
            final PDFDoc doc = new PDFDoc(filepath);
            if (password == null) {
                doc.load(null );
            } else {
                doc.load(password.getBytes());
            }

            final FxProgressDialog progressDialog = mSupport.getProgressDialog();
            progressDialog.setTips(AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.rv_page_import));
            int[] ranges = {0, doc.getPageCount()};
            doImportPages(dstIndex, ranges, doc, new EditThumbnailCallback() {
                @Override
                public void result(boolean success) {
                    doc.delete();
                    progressDialog.dismiss();
                    notifyDataSetChanged();
                }
            });
            progressDialog.show();
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrPassword) {
                String tips;
                if (password != null && password.trim().length() > 0) {
                    tips = AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.rv_tips_password_error);
                } else {
                    tips = AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.rv_tips_password);
                }
                final UITextEditDialog uiTextEditDialog = new UITextEditDialog(mSupport.getActivity());
                uiTextEditDialog.getDialog().setCanceledOnTouchOutside(false);
                uiTextEditDialog.getInputEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                uiTextEditDialog.setTitle(AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.rv_password_dialog_title));
                uiTextEditDialog.getPromptTextView().setText(tips);
                uiTextEditDialog.show();
                uiTextEditDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        uiTextEditDialog.dismiss();
                        InputMethodManager inputManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        String pw = uiTextEditDialog.getInputEditText().getText().toString();
                        importDocument(dstIndex, filepath, pw);
                    }
                });

                uiTextEditDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        uiTextEditDialog.dismiss();
                        InputMethodManager inputManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                });

                uiTextEditDialog.getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            uiTextEditDialog.getDialog().cancel();
                            return true;
                        }
                        return false;
                    }
                });
                uiTextEditDialog.show();
            } else {
                UIToast.getInstance(mSupport.getContext()).show(mSupport.getContext().getApplicationContext().getString(R.string.rv_page_import_error));
            }
        }
    }

    private void doImportPages(int dstIndex, int[] pageRanges, final PDFDoc srcDoc, EditThumbnailCallback callback) {
        EditThumbnailTask editThumbnailTask = new EditThumbnailTask(dstIndex, pageRanges, srcDoc, callback);
        mPDFViewCtrl.addTask(editThumbnailTask);
    }

    private void doInsertPages(int dstIndex, int[] pageRanges, final CreatePageBean pageBean, EditThumbnailCallback callback){
        EditThumbnailTask editThumbnailTask = new EditThumbnailTask(dstIndex, pageRanges, pageBean, callback);
        mPDFViewCtrl.addTask(editThumbnailTask);
    }

    private void doCopyPages(int[] pageRanges, final PDFDoc srcDoc, EditThumbnailCallback callback) {
        EditThumbnailTask editThumbnailTask = new EditThumbnailTask(pageRanges, srcDoc, callback);
        mPDFViewCtrl.addTask(editThumbnailTask);
    }

    private void doExtractPages(ArrayList<ThumbnailItem> itemList, final String path) {
        final FxProgressDialog progressDialog = mSupport.getProgressDialog();
        progressDialog.setTips(AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.rv_page_extract));
        progressDialog.show();
        EditThumbnailTask editThumbnailTask = new EditThumbnailTask(itemList, path, new EditThumbnailCallback() {
            @Override
            public void result(boolean success) {
                progressDialog.dismiss();
                if (!success) {
                    UIToast.getInstance(mSupport.getContext()).show(AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.rv_page_extract_error));
                }

                LocalModule module = (LocalModule) ((UIExtensionsManager)mPDFViewCtrl.getUIExtensionsManager())
                        .getModuleByName(Module.MODULE_NAME_LOCAL);
                if (module != null && path != null) {
                    module.updateThumbnail(path);
                }
                notifyDataSetChanged();
            }
        });
        mPDFViewCtrl.addTask(editThumbnailTask);
    }

    private void showInputFileNameDialog(final String fileFolder) {
        final String filePath = AppFileUtil.getFileDuplicateName("");
        final String fileName = AppFileUtil.getFileNameWithoutExt(filePath);

        final UITextEditDialog rmDialog = new UITextEditDialog(mSupport.getActivity());
        rmDialog.setPattern("[/\\:*?<>|\"\n\t]");
        rmDialog.setTitle(AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.fx_string_extract_to));
        rmDialog.getPromptTextView().setVisibility(View.GONE);
        rmDialog.getInputEditText().setText(fileName);
        rmDialog.getInputEditText().selectAll();
        rmDialog.show();
        AppUtil.showSoftInput(rmDialog.getInputEditText());

        rmDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rmDialog.dismiss();
                String inputName = rmDialog.getInputEditText().getText().toString();
                String newPath = fileFolder + "/" + inputName;
                newPath += ".pdf";
                File file = new File(newPath);
                if (file.exists()) {
                    Module module = ((UIExtensionsManager)mPDFViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DOCINFO);
                    if (module == null) {
                        UIToast.getInstance(mSupport.getContext()).show(mSupport.getContext().getApplicationContext().getString(R.string.docinfo_module_not_regisetered_toast));
                        return;
                    }
                    String currentPath = ((DocInfoModule)module).getFilePath();
                    if(!newPath.contentEquals(currentPath))
                        showAskReplaceDialog(fileFolder, newPath);
                    else
                    {
                        UIToast.getInstance(mSupport.getContext())
                                .show(mSupport.getContext().getApplicationContext().getString(R.string.cinflicted_with_current_doc_toast));
                        showInputFileNameDialog(fileFolder);
                    }

                } else {
                    doExtractPages(mSelectedList, newPath);
                }
            }
        });
    }

    private void showAskReplaceDialog(final String fileFolder, final String newPath) {
        final UITextEditDialog rmDialog = new UITextEditDialog(mSupport.getActivity());
        rmDialog.setTitle(AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.fx_string_extract_to));
        rmDialog.getPromptTextView().setText(AppResource.getString(mSupport.getContext().getApplicationContext(),R.string.fx_string_filereplace_warning));
        rmDialog.getInputEditText().setVisibility(View.GONE);
        rmDialog.show();

        rmDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rmDialog.dismiss();
                File file = new File(newPath);
                if (file.delete()) {
                    doExtractPages(mSelectedList, newPath);
                } else {
                    UIToast.getInstance(mSupport.getContext())
                            .show(mSupport.getContext().getApplicationContext().getString(R.string.the_file_can_not_replace_toast, file.getPath()));
                }
            }
        });

        rmDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rmDialog.dismiss();
                showInputFileNameDialog(fileFolder);
            }
        });
    }

    void rotateSelectedPages() {
        if (mSelectedList.size() == 0) return;
        mSupport.getProgressDialog().setTips(AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.rv_page_rotate_cw));
        mSupport.getProgressDialog().show();
        doRotatePages(mSelectedList, true, new EditThumbnailCallback() {
            @Override
            public void result(boolean success) {
                mSupport.getProgressDialog().dismiss();
                notifyDataSetChanged();
            }
        });
    }


    //remove page from adapter and pdf doc
    private void removePage(int position) {
        if (mThumbnailList.size() <= 1) {
            mSupport.showTipsDlg(ThumbnailSupport.REMOVE_ALL_PAGES_TIP);
            return;
        }
        final ThumbnailItem item = mThumbnailList.get(position);
        //remove page from doc;
        final ArrayList<ThumbnailItem> itemArrayList = new ArrayList<ThumbnailItem>();
        itemArrayList.add(item);
        doRemovePages(itemArrayList, new EditThumbnailCallback() {
            @Override
            public void result(boolean success) {
                itemArrayList.clear();
                notifyDataSetChanged();
            }
        });
    }

    private void rotatePage(final int position, boolean isClockWise) {
        final ArrayList<ThumbnailItem> itemArrayList = new ArrayList<ThumbnailItem>();
        ThumbnailItem item = mThumbnailList.get(position);
        itemArrayList.add(item);
        doRotatePages(itemArrayList, isClockWise, new EditThumbnailCallback() {
            @Override
            public void result(boolean success) {
                itemArrayList.clear();
                notifyDataSetChanged();
            }
        });
    }

    private void movePage(int fromPosition, int toPosition) {
        if (mPDFViewCtrl.movePage(fromPosition, toPosition)) {
            ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
        }
    }

    void importPages(final int dstIndex) {
        UIFileSelectDialog dialog = mSupport.getFileSelectDialog();
        dialog.setFileClickedListener(new UIFileSelectDialog.OnFileClickedListener() {
            @Override
            public void onFileClicked(String filepath) {
                importDocument(dstIndex, filepath, null);
            }
        });
        dialog.setHeight(dialog.getDialogHeight());
        dialog.showDialog();
    }

    //import pages form sd-card special file
    public void importPagesFromSpecialFile(final int dstIndex){
        UIFileSelectDialog dialog = mSupport.getFileSelectDialog();
        dialog.setFileClickedListener(new UIFileSelectDialog.OnFileClickedListener() {
            @Override
            public void onFileClicked(String filepath) {
                importDocument(dstIndex, filepath, null);
            }
        });
        dialog.setHeight(dialog.getDialogHeight());
        dialog.showDialog();
    }

    public void insertPage(int index, CreatePageBean pageBean){
        final FxProgressDialog progressDialog = mSupport.getProgressDialog();
        progressDialog.setTips(AppResource.getString(mSupport.getContext().getApplicationContext(), R.string.fx_string_processing));
        int[] ranges = {index, pageBean.getPageCounts()};
        doInsertPages(index, ranges, pageBean, new EditThumbnailCallback() {
            @Override
            public void result(boolean success) {
                progressDialog.dismiss();
                notifyDataSetChanged();
            }
        });
        progressDialog.show();
    }

    private float[] getImageWidthHeight(float pageWidth, float pageHeight, String path){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        float outWidth = options.outWidth;
        float outHeigh = options.outHeight;
        float rate = outWidth/outHeigh;
        float pageRate = pageWidth/pageHeight;
        if (rate > pageRate){
            return new float[]{pageWidth,pageHeight/rate};
        }else{
            return new float[]{pageHeight*rate,pageHeight};
        }
    }

    public int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public boolean importPagesFromDCIM(final int dstIndex,String imagePath) {
        return mPDFViewCtrl.addImagePage(dstIndex, imagePath);
    }

    public boolean importPagesFromCamera(final int dstIndex,String imagePath) {
        return mPDFViewCtrl.addImagePage(dstIndex, imagePath);
    }

    void extractPages() {
        final UIFolderSelectDialog dialog = mSupport.getFolderSelectDialog();
        dialog.setListener(new MatchDialog.DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == MatchDialog.DIALOG_OK) {
                    String fileFolder = dialog.getCurrentPath();
                    showInputFileNameDialog(fileFolder);
                }
                dialog.dismiss();
            }

            @Override
            public void onBackClick() {
            }
        });
        dialog.setHeight(dialog.getDialogHeight());
        dialog.showDialog();
        dialog.notifyDataSetChanged();
    }

    class EditThumbnailTask extends Task {
        private final int mType;
        private final ArrayList<ThumbnailItem> tmpItemLists;
        private int mInsertPosition;
        private int[] mImportRanges;
        private PDFDoc mPDFDoc;
        private CreatePageBean mPageBean;
        private String mExtractPath;
        private boolean mSuccess = false;

        EditThumbnailTask(int type, ArrayList<ThumbnailItem> itemLists, final EditThumbnailCallback callback) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    if (callback != null) {
                        callback.result(((EditThumbnailTask) task).mSuccess);
                    }
                }
            });
            mType = type;
            tmpItemLists = itemLists;
        }

        EditThumbnailTask(int position, int[] ranges, PDFDoc doc, final EditThumbnailCallback callback) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    if (callback != null) {
                        callback.result(((EditThumbnailTask) task).mSuccess);
                    }
                }
            });
            mType = INSERT_FROM_DOCUMENT;
            tmpItemLists = null;
            mInsertPosition = position;
            mImportRanges = ranges;
            mPDFDoc = doc;
        }

        EditThumbnailTask(int[] ranges, PDFDoc doc, final EditThumbnailCallback callback) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    if (callback != null) {
                        callback.result(((EditThumbnailTask) task).mSuccess);
                    }
                }
            });
            mType = COPY_PAGES;
            tmpItemLists = null;
            mInsertPosition = ranges[ranges.length-2]+1;
            mImportRanges = ranges;
            mPDFDoc = doc;
        }

        EditThumbnailTask(int position, int[] ranges, CreatePageBean pageBean, final EditThumbnailCallback callback) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    if (callback != null) {
                        callback.result(((EditThumbnailTask) task).mSuccess);
                    }
                }
            });
            mType = INSERT_PAGES;
            tmpItemLists = null;
            mInsertPosition = position;
            mImportRanges = ranges;
            mPageBean = pageBean;
        }

        EditThumbnailTask(ArrayList<ThumbnailItem> itemLists, String path, final EditThumbnailCallback callback) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    if (callback != null) {
                        callback.result(((EditThumbnailTask) task).mSuccess);
                    }
                }
            });
            mType = EXPORT_PAGE;
            tmpItemLists = itemLists;
            mExtractPath = path;
        }

        @Override
        protected void execute() {
            isEditing = true;
            switch (mType) {
                case REMOVE_PAGE:
                case ROTATE_PAGE_CW:
                case ROTATE_PAGE_ACW:
                    editSelectedPages();
                    break;
                case INSERT_FROM_DOCUMENT:
                    insertPages();
                    break;
                case EXPORT_PAGE:
                    extractPages();
                    break;
                case COPY_PAGES:
                    copyPages();
                    break;
                case INSERT_PAGES:
                    createPages();
                    break;
                default://not support now
                    break;
            }
            isEditing = false;
            if (mSuccess) {
                ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            }
        }

        private void editSelectedPages() {
            int[] pageIndexes = new int[tmpItemLists.size()];
            for (int i = 0; i < tmpItemLists.size(); i++)
                pageIndexes[i] = tmpItemLists.get(i).getIndex();
            if (mType == REMOVE_PAGE)
                mSuccess = mPDFViewCtrl.removePages(pageIndexes);
            for (ThumbnailItem item : tmpItemLists) {
                switch (mType) {
                    case ROTATE_PAGE_CW: {
                        int rotation = item.getRotation();
                        mSuccess = item.setRotation(rotation < 3 ? rotation + 1 : 3 - rotation);
                    }
                    break;
                    case ROTATE_PAGE_ACW: {
                        int rotation = item.getRotation();
                        mSuccess = item.setRotation(rotation > 0 ? rotation - 1 : 3 + rotation);
                    }
                    break;
                    default:
                        break;
                }
            }
            mCurrentPage = Math.min(mCurrentPage, getItemCount() - 1);
        }

        private void insertPages() {
            mSuccess = mPDFViewCtrl.insertPages(mInsertPosition, PDFDoc.e_ImportFlagNormal, null, mPDFDoc, mImportRanges);
        }

        private void createPages() {
            float width = mPageBean.getWidth();
            float height = mPageBean.getHeight();
            if (Constants.e_Rotation90 == mPageBean.getPageDirection() ||
                    Constants.e_Rotation270 == mPageBean.getPageDirection()){
                width = mPageBean.getHeight();
                height = mPageBean.getWidth();
            }
            mSuccess = mPDFViewCtrl.insertPages(mInsertPosition,
                    width,
                    height,
                    mPageBean.getPageStyle(),
                    mPageBean.getPageColor(),
                    Constants.e_Rotation0,
                    mPageBean.getPageCounts());
        }

        private void copyPages(){
            mSuccess = mPDFViewCtrl.insertPages(mInsertPosition, PDFDoc.e_ImportFlagNormal, null, mPDFDoc, mImportRanges);
        }


        private void extractPages() {
            try {
                PDFDoc doc = new PDFDoc();
                Collections.sort(tmpItemLists);

                ArrayList<Boolean> extractFlagList = new ArrayList<>();
                for (int i = 0; i < mPDFViewCtrl.getPageCount(); i++) {
                    extractFlagList.add(false);
                }

                for (ThumbnailItem item : tmpItemLists) {
                    int index = item.getIndex();
                    extractFlagList.set(index, true);

                }
                ArrayList<Integer> rangeList = new ArrayList<>();

                int lastIndex = -1;
                int count = 0;
                for (int i = 0; i < extractFlagList.size(); i++) {
                    if (extractFlagList.get(i)) {
                        if (lastIndex == -1)
                            lastIndex = i;
                        count++;
                    } else {
                        if (lastIndex == -1) {
                            count = 0;
                            continue;
                        } else {
                            rangeList.add(lastIndex);
                            rangeList.add(count);
                            lastIndex = -1;
                            count = 0;
                        }
                    }
                }
                if (lastIndex != -1) {
                    rangeList.add(lastIndex);
                    rangeList.add(count);
                }

                Range ranges = new Range();
//                int ranges[] = new int[rangeList.size()];
                for (int i = 0; i < rangeList.size(); i+= 2) {
//                    ranges[i] = rangeList.get(i);
                    ranges.addSegment(rangeList.get(i), rangeList.get(i) + rangeList.get(i + 1) - 1, Range.e_All);
                }

                doc.startImportPages(doc.getPageCount(), mPDFViewCtrl.getDoc(), PDFDoc.e_ImportFlagShareStream, null, ranges, null);

                Progressive progressive = doc.startSaveAs(mExtractPath, PDFDoc.e_SaveFlagIncremental, null);
                int state = Progressive.e_ToBeContinued;
                while (state == Progressive.e_ToBeContinued) {
                    state = progressive.resume();
                }
//                progressive.delete();
                doc.delete();
                mSuccess = (state == Progressive.e_Finished);
            } catch (PDFException e) {
                mSuccess = false;
            }
        }
    }

    interface EditThumbnailCallback {
        void result(boolean success);
    }

    public class ThumbViewHolder extends RecyclerView.ViewHolder {
        private final TextView mIndexView;
        private final ImageView mImageView;
        private final ImageView mRemoveView;
        private final ImageView mRotateAcwView;
        private final ImageView mRotateCwView;
        private final ImageView mInsertLeftView;
        private final ImageView mInsertRightView;
        private final LinearLayout mLeftEditViewLayout;
        private final LinearLayout mRightEditViewLayout;
        private final ImageView mSelectView;
        protected Bitmap mThumbnailBitmap;

        public ThumbViewHolder(View itemView) {
            super(itemView);
            mIndexView = (TextView) itemView.findViewById(R.id.item_text);
            mImageView = (ImageView) itemView.findViewById(R.id.item_image);
            mSelectView = (ImageView) itemView.findViewById(R.id.thumbnail_select_view);

            mRemoveView = (ImageView) itemView.findViewById(R.id.thumbnail_delete_self);
            mRotateAcwView = (ImageView) itemView.findViewById(R.id.thumbnail_rotate_acw);
            mRotateCwView = (ImageView) itemView.findViewById(R.id.thumbnail_rotate_cw);

            mInsertLeftView = (ImageView) itemView.findViewById(R.id.thumbnail_insert_left);
            mInsertRightView = (ImageView) itemView.findViewById(R.id.thumbnail_insert_right);

            mLeftEditViewLayout = (LinearLayout) itemView.findViewById(R.id.thumbnail_edit_left_layout);
            mRightEditViewLayout = (LinearLayout) itemView.findViewById(R.id.thumbnail_edit_right_layout);

        }

        public void updateImageView() {
            mImageView.setImageBitmap(getThumbnailBitmap());
            mImageView.invalidate();
        }

        public Bitmap getThumbnailBitmap() {
            if (mThumbnailBitmap == null)
                mThumbnailBitmap = Bitmap.createBitmap(mSupport.getThumbnailBackgroundSize().x, mSupport.getThumbnailBackgroundSize().y, Bitmap.Config.RGB_565);
            return mThumbnailBitmap;
        }

        public boolean inEditView(int x, int y) {
            int[] location = {0, 0};
            if (mLeftEditViewLayout.getVisibility() == View.VISIBLE) {
                mLeftEditViewLayout.getLocationOnScreen(location);
                Rect rect = new Rect(location[0], location[1], location[0] + mLeftEditViewLayout.getWidth(), location[1] + mLeftEditViewLayout.getHeight());
                if (rect.contains(x, y))
                    return true;
            }
            if (mRightEditViewLayout.getVisibility() == View.VISIBLE) {
                mRightEditViewLayout.getLocationOnScreen(location);
                Rect rect = new Rect(location[0], location[1], location[0] + mRightEditViewLayout.getWidth(), location[1] + mRightEditViewLayout.getHeight());
                if (rect.contains(x, y))
                    return true;
            }
            return false;
        }

        protected void blank(ThumbnailItem item) {
            Bitmap bitmap = getThumbnailBitmap();
            bitmap.eraseColor(mSupport.getContext().getResources().getColor(R.color.ux_color_thumbnail_textview_background));
            mImageView.setImageBitmap(bitmap);
            mImageView.invalidate();
        }

        public void changeLeftEditView(final int position, boolean withAnimation) {
            final ThumbnailItem item = mThumbnailList.get(position);
            if (mSupport.isEditMode() && item.editViewFlag == ThumbnailItem.EDIT_LEFT_VIEW) {
                if (mLeftEditViewLayout.getVisibility() == View.VISIBLE)
                    return;
                ViewGroup.LayoutParams layoutParams = mRotateCwView.getLayoutParams();
                layoutParams.width = mSupport.getThumbnailBackgroundSize().x / 3;
                layoutParams.height = layoutParams.width;
                mRotateCwView.setLayoutParams(layoutParams);
                mRotateAcwView.setLayoutParams(layoutParams);
                mRemoveView.setLayoutParams(layoutParams);
                mRotateCwView.setPadding(5, 0, 5, 0);
                mRotateAcwView.setPadding(5, 0, 5, 0);
                mRemoveView.setPadding(5, 0, 5, 0);
                mRotateCwView.requestLayout();
                mRotateAcwView.requestLayout();
                mRemoveView.requestLayout();
                mRotateAcwView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rotatePage(position, false);
                    }
                });

                mRotateCwView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rotatePage(position, true);
                    }
                });

                mRemoveView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        item.editViewFlag = ThumbnailItem.EDIT_NO_VIEW;
                        removePage(position);
                    }
                });
                if (withAnimation) {
                    TranslateAnimation showAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1.0f,
                            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                            0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
                    showAnimation.setDuration(500);
                    mLeftEditViewLayout.startAnimation(showAnimation);
                }
                mLeftEditViewLayout.setVisibility(View.VISIBLE);
            } else {
                if (mLeftEditViewLayout.getVisibility() == View.GONE)
                    return;
                if (withAnimation) {
                    TranslateAnimation goneAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                            0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
                            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                            0.0f);
                    goneAnimation.setDuration(500);
                    mLeftEditViewLayout.startAnimation(goneAnimation);
                }
                mLeftEditViewLayout.setVisibility(View.GONE);
            }
        }

        public void changeRightEditView(final int position, boolean withAnimation) {
            final ThumbnailItem item = mThumbnailList.get(position);
            if (mSupport.isEditMode() && item.editViewFlag == ThumbnailItem.EDIT_RIGHT_VIEW) {
                if (mRightEditViewLayout.getVisibility() == View.VISIBLE)
                    return;

                ViewGroup.LayoutParams layoutParams = mInsertLeftView.getLayoutParams();
                layoutParams.width = mSupport.getThumbnailBackgroundSize().x / 3;
                layoutParams.height = layoutParams.width;
                mInsertLeftView.setLayoutParams(layoutParams);
                mInsertRightView.setLayoutParams(layoutParams);
                mInsertRightView.setPadding(5, 0, 5, 0);
                mInsertLeftView.setPadding(5, 0, 5, 0);
                mInsertLeftView.requestLayout();
                mInsertRightView.requestLayout();

                mInsertLeftView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //importPages(position);
                        index = position;
                        flag = true;
                        callback.insertImage();
                    }
                });

                mInsertRightView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //importPages(position + 1);
                        index = position+1;
                        flag =true;
                        callback.insertImage();
                    }
                });
                if (withAnimation) {
                    TranslateAnimation showAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f,
                            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                            0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
                    showAnimation.setDuration(500);
                    mRightEditViewLayout.startAnimation(showAnimation);
                }
                mRightEditViewLayout.setVisibility(View.VISIBLE);
            } else {
                if (mRightEditViewLayout.getVisibility() == View.GONE)
                    return;
                if (withAnimation) {
                    TranslateAnimation showAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                            Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF,
                            0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
                    showAnimation.setDuration(500);
                    mRightEditViewLayout.startAnimation(showAnimation);
                }
                mRightEditViewLayout.setVisibility(View.GONE);
            }
        }

        public void changeSelectView(boolean show) {
            if (!mSupport.isEditMode()) {
                mSelectView.setVisibility(View.GONE);
                return;
            }
            ViewGroup.LayoutParams layoutParams = mSelectView.getLayoutParams();
            layoutParams.height = mSupport.getThumbnailBackgroundSize().x / 5;
            layoutParams.width = layoutParams.height;
            mSelectView.setLayoutParams(layoutParams);
            mSelectView.requestLayout();
            mSelectView.setVisibility(View.VISIBLE);
            if (show) {
                mSelectView.setImageDrawable(mSupport.getContext().getResources().getDrawable(R.drawable.thumbnail_select_true));
            } else {
                mSelectView.setImageDrawable(mSupport.getContext().getResources().getDrawable(R.drawable.thumbnail_select_normal));
            }
        }

        public void drawThumbnail(final ThumbnailItem item, int position) {
            final int index = isEditing ? position : item.getIndex();
            changeLeftEditView(index, false);
            changeRightEditView(index, false);
            changeSelectView(item.isSelected());
            if (mCurrentPage == index) {
                mIndexView.setBackgroundDrawable(mSupport.getContext().getResources().getDrawable(R.drawable.thumbnail_textview_background_current));
            } else {
                mIndexView.setBackgroundDrawable(mSupport.getContext().getResources().getDrawable(R.drawable.thumbnail_textview_background_normal));
            }
            mIndexView.setText(String.format("%d", index + 1));
            if (item.getBitmap() != null && !item.needRecompute()) {
                mImageView.setImageBitmap(item.getBitmap());
                mImageView.invalidate();
            } else {
                blank(item);
                if (item.isRendering() && !item.needRecompute()) {
                    return;
                }
                if (!isEditing) {
                    final DrawThumbnailTask drawThumbnailTask = new DrawThumbnailTask(item, new DrawThumbnailCallback() {
                        @Override
                        public void result(ThumbnailItem item, DrawThumbnailTask task, Bitmap bitmap) {
                            ThumbViewHolder viewHolder = mSupport.getViewHolderByItem(item);
                            if (bitmap == null || viewHolder == null)
                                return;
                            Bitmap vhBitmap = viewHolder.getThumbnailBitmap();
                            Canvas canvas = new Canvas(vhBitmap);
                            canvas.drawBitmap(bitmap, null, item.getRect(), new Paint());
                            item.setBitmap(Bitmap.createBitmap(vhBitmap));

                            drawWrapperData(canvas, index);

                            viewHolder.updateImageView();
                            updateCacheListInfo(item, true);
                            removeTask(task);
                        }
                    });
                    addTask(drawThumbnailTask);
                }
            }
        }
    }

    private void drawWrapperData(Canvas canvas, int pageIndex) {
        try {
            DocumentManager documentManager = ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager();
            if (mPDFViewCtrl.getDoc().isCDRM() &&
                    !documentManager.isAccessPage(pageIndex)) {
                TextPaint textPaint = new TextPaint();
                textPaint.setColor(Color.RED);
                textPaint.setTextSize(14);
                StaticLayout layout = new StaticLayout(documentManager.getWrapperContent(), textPaint,
                        mSupport.getThumbnailBackgroundSize().x - 10, Layout.Alignment.ALIGN_CENTER,
                        1.5f, 0, false);

                canvas.save();
                canvas.translate(10, mSupport.getThumbnailBackgroundSize().y / 2);
                layout.draw(canvas);
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }
}

