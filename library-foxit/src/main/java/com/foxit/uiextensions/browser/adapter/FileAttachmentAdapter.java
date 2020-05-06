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
package com.foxit.uiextensions.browser.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Environment;
import android.os.Handler;
import android.text.Selection;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.browser.adapter.viewholder.PageFlagViewHolder;
import com.foxit.uiextensions.browser.adapter.viewholder.SuperViewHolder;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UISaveAsDialog;
import com.foxit.uiextensions.modules.panel.bean.BaseBean;
import com.foxit.uiextensions.modules.panel.bean.FileBean;
import com.foxit.uiextensions.modules.panel.filespec.FileAttachmentPresenter;
import com.foxit.uiextensions.modules.panel.filespec.FileSpecModuleCallback;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import static com.foxit.uiextensions.controls.dialog.fileselect.UISaveAsDialog.ISaveAsOnOKClickCallBack;


public class FileAttachmentAdapter extends SuperAdapter implements FileAttachmentPresenter.FileAttachmentViewer {

    private final static String TAG = FileAttachmentAdapter.class.getSimpleName();

    public final static int FLAG_NORMAL = 0;
    public final static int FLAG_TAG = 1;
    public final static int FLAG_ANNOT = 2;

    private List<FileBean> mFileList;

    private FileAttachmentPresenter presenter;
    private FileSpecModuleCallback callback;
    private UISaveAsDialog saveAsDialog;

    private PDFViewCtrl mPdfViewCtrl;

    private int index = -1;

    public FileAttachmentAdapter(Context context, List list, PDFViewCtrl pdfViewCtrl,FileSpecModuleCallback callback) {
        super(context);
        this.mFileList = list;
        this.callback = callback;
        this.mPdfViewCtrl = pdfViewCtrl;
        presenter = new FileAttachmentPresenter(context, pdfViewCtrl,this);
    }

    public void initPDFNameTree(boolean reInit){
        presenter.initPDFNameTree(reInit);
    }

    public void reset(){
        index =-1;
    }

    public int getIndex() {
        return index;
    }

    /**
     * init data, load all pdf-name-tree item
     */
    public void init(boolean isLoadAnnotation) {
        presenter.searchFileAttachment(isLoadAnnotation);
    }

    public void reInit(){
        presenter.reInitPDFNameTree();
    }

    /**
     *  add a file spec to pdfdoc nametree
     * @param name file name
     * @param file file path
     */
    public void add(String name, String file){
        presenter.add(name, file);
        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
    }

    public void add(Annot annot){
        presenter.add(annot);
        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
    }

    private void delete(int count,int start, int end){
        presenter.delete(count,start,end);
        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
    }

    //delete annot
    public void delete(Annot annot){
        if(!presenter.getAnnotList().contains(annot)){
            return;
        }
        presenter.delete(mPdfViewCtrl, annot);
    }

    public void deleteByOutside(Annot annot){
        try {
            List<Annot> annots = presenter.getAnnotList();
            for (Annot a : annots){
                if (a.getPage().getIndex() == annot.getPage().getIndex() && a.getUniqueID().equals(annot.getUniqueID())){
                    presenter.deleteByOutside(mPdfViewCtrl, a);
                    return;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void delete(int index){
        try {
            for (Annot a : presenter.getAnnotList()) {
                if (a.getUniqueID().equals(mFileList.get(index).getUuid())){
                    delete(a);
                    break;
                }
            }
        }catch (PDFException e){
            e.printStackTrace();
        }
    }

    private void flatten(int index){
        try {
            for (Annot annot : presenter.getAnnotList()) {
                if (annot.getUniqueID().equals(mFileList.get(index).getUuid())){
                    presenter.flatten(mPdfViewCtrl, annot);
                    break;
                }
            }
        }catch (PDFException e){
            e.printStackTrace();
        }
    }

    public void updateByOutside(Annot annot){
        presenter.updateByOutside(annot);
        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
    }

    private void setDesc(int index,String content){
        presenter.update(index,content);
        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
    }

    private void save(int index,String path){
        presenter.save(mPdfViewCtrl,index, path);
    }

    private void open(int index,String path){
        presenter.open(mPdfViewCtrl,index, path);
    }

    public List<FileBean> getList() {
        return mFileList;
    }

    public void setList(List<FileBean> list) {
        this.mFileList = list;
    }

    @Override
    public void notifyUpdateData() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SuperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SuperViewHolder viewHolder;
        switch (viewType) {
            case FLAG_NORMAL:
                viewHolder = new ItemViewHolder(inflateLayout(getContext(), parent, R.layout.panel_item_fileattachment));
                break;
            case FLAG_TAG:
                viewHolder = new PageFlagViewHolder(inflateLayout(getContext(), parent, R.layout.panel_item_fileattachment_flag));
                break;
            default:
                viewHolder = new ItemViewHolder(inflateLayout(getContext(), parent, R.layout.panel_item_fileattachment));
                break;
        }

        return viewHolder;
    }

    private View inflateLayout(Context context, ViewGroup parent, int layoutId) {
        return LayoutInflater.from(context).inflate(layoutId, parent, false);
    }


    @Override
    public int getItemCount() {
        return mFileList.size();
    }

    @Override
    public FileBean getDataItem(int position) {
        return mFileList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return mFileList.get(position).getFlag();
    }


    @Override
    public void success(ArrayList<FileBean> list) {
        this.mFileList = list;
        this.index = -1;
        notifyDataSetChanged();
        if (this.mFileList.size()>=1){
            callback.success();
        }
        if (this.mFileList.size() == 0){
            callback.fail();
        }
    }

    @Override
    public void fail(int rct, Object o) {

    }

    @Override
    public void openPrepare() {
        callback.onDocOpenPrepare();
    }

    @Override
    public void openStart(String path,String filename) {
        callback.onDocOpenStart(path,filename);
    }

    @Override
    public void openFinished() {
        callback.onDocOpenFinished();
    }

    class ItemViewHolder extends SuperViewHolder {

        private ImageView icon;
        private ImageView more;
        private TextView title;
        private TextView date_size;
        private TextView desc;
        private View view;
        private View container;

        private TextView more_save;
        private TextView more_desc;
        private TextView more_flatten;
        private TextView more_delete;



        public ItemViewHolder(View viewHolder) {
            super(viewHolder);
            container = viewHolder.findViewById(R.id.panel_attachment_container);
            icon = (ImageView) viewHolder.findViewById(R.id.panel_item_fileattachment_icon);
            more = (ImageView) viewHolder.findViewById(R.id.panel_item_fileattachment_more);
            title = (TextView) viewHolder.findViewById(R.id.panel_item_fileattachment_title);
            date_size = (TextView) viewHolder.findViewById(R.id.panel_item_fileattachment_date_size);
            desc = (TextView) viewHolder.findViewById(R.id.panel_item_fileattachment_desc);
            more.setOnClickListener(this);
            view = viewHolder.findViewById(R.id.more_view);
            more_save = (TextView) viewHolder.findViewById(R.id.panel_more_tv_save);
            more_desc = (TextView) viewHolder.findViewById(R.id.panel_more_tv_desc);
            more_flatten = (TextView) viewHolder.findViewById(R.id.panel_more_tv_flatten);
            more_delete = (TextView) viewHolder.findViewById(R.id.panel_more_tv_delete);

            more_save.setOnClickListener(this);
            more_delete.setOnClickListener(this);
            more_desc.setOnClickListener(this);
            more_flatten.setOnClickListener(this);
            container.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.panel_item_fileattachment_more) {
                int temp = index;
                index = getAdapterPosition();
                notifyItemChanged(temp);
                notifyItemChanged(index);
            } else if (v.getId() == R.id.panel_more_tv_desc){
                ((LinearLayout) v.getParent()).setVisibility(View.GONE);

                final UITextEditDialog textDialog = new UITextEditDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
                textDialog.getPromptTextView().setVisibility(View.GONE);
                textDialog.getInputEditText().setText(desc.getText());
                textDialog.getInputEditText().setMaxLines(6);
                Spannable text = textDialog.getInputEditText().getText();
                if (text != null) {
                    Selection.setSelection(text, text.length());
                }
                textDialog.setTitle(context.getApplicationContext().getString(R.string.rv_panel_edit_desc));
                textDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppKeyboardUtil.hideInputMethodWindow(textDialog.getInputEditText().getContext(),textDialog.getInputEditText());
                        textDialog.dismiss();
                    }
                });
                textDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AppKeyboardUtil.hideInputMethodWindow(textDialog.getInputEditText().getContext(),textDialog.getInputEditText());
                        textDialog.dismiss();
                        setDesc(getAdapterPosition(),textDialog.getInputEditText().getText().toString());

                    }
                });
                textDialog.show();
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        AppKeyboardUtil.showInputMethodWindow(textDialog.getInputEditText().getContext(),textDialog.getInputEditText());
                    }
                });
            } else if (v.getId() == R.id.panel_more_tv_delete){
                ((LinearLayout) v.getParent()).setVisibility(View.GONE);

                if (mFileList.get(getAdapterPosition()).getFlag() == FLAG_ANNOT){
                    delete(getAdapterPosition());
                    return;
                }
                delete(1,getAdapterPosition(),getAdapterPosition());
            } else if (v.getId() == R.id.panel_more_tv_save){
                ((LinearLayout) v.getParent()).setVisibility(View.GONE);

                String fileName = mFileList.get(getAdapterPosition()).getTitle();
                if (fileName == null || fileName.trim().length() < 1) {
                    AlertDialog dialog = new AlertDialog.Builder(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity()).
                            setMessage(getContext().getApplicationContext().getString(R.string.save_failed_by_incorrect_file_name)).
                            setPositiveButton(getContext().getApplicationContext().getString(R.string.fx_string_ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                    return;
                }
                String suffix = fileName.substring(fileName.lastIndexOf(".")+1);
                saveAsDialog = new UISaveAsDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity(), mFileList.get(getAdapterPosition()).getTitle(), suffix, new ISaveAsOnOKClickCallBack() {
                    @Override
                    public void onOkClick(final String newFilePath) {
                        save(getAdapterPosition(),newFilePath);
                    }
                    @Override
                    public void onCancelClick() {

                    }
                });
                saveAsDialog.showDialog();
            } else if (v.getId() == R.id.panel_attachment_container){
                if (index != -1){
                    int temp = index;
                    reset();
                    notifyItemChanged(temp);
                    return;
                }

                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                String tempPath = Environment.getExternalStorageDirectory() + "/FoxitSDK/AttaTmp/";
                open(getAdapterPosition(),tempPath);
            } else if (v.getId() == R.id.panel_more_tv_flatten){
                if (mFileList.get(getAdapterPosition()).getFlag() == FLAG_ANNOT){
                    flatten(getAdapterPosition());
                }
            }
        }

        @Override
        public void bind(BaseBean data) {
            FileBean item = (FileBean) data;
            icon.setImageResource(getIconResource(item.getTitle()));
            title.setText(item.getTitle());
            date_size.setText(item.getSize());
            desc.setText(item.getDesc());
            if (!AppUtil.isBlank(item.getDesc())) {
                desc.setVisibility(View.VISIBLE);
            }else {
                desc.setVisibility(View.GONE);
            }
            if(getAdapterPosition() != index) {
                view.setVisibility(View.GONE);
            }else {
                view.setVisibility(View.VISIBLE);
            }

            //3. when copy access.. == not allow: all can not save
            boolean enable = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canCopyForAssess() &&
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canCopy();
            more_save.setVisibility(enable ? View.VISIBLE : View.GONE);

            if (item.getFlag() == FLAG_ANNOT){
                //2.when comment ==not allow:annotation should not show more view! but fileattachment can!
                boolean canEdit = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot();
                more.setEnabled(canEdit);
                more.setClickable(canEdit);

                more.setVisibility(View.VISIBLE);
                more_desc.setVisibility(item.canDelete() ? View.VISIBLE : View.GONE);//lock & readonly flag
                more_flatten.setVisibility(View.VISIBLE);
                //4.when copy access or extract == not allow: annot can save,but attachment can not;
                more_save.setVisibility(View.VISIBLE);
                more_delete.setVisibility(canEdit && item.canDelete() ? View.VISIBLE : View.GONE);
            }else if (item.getFlag() == FLAG_NORMAL){
                //1.when modify ==not allow:fileattachment should not show more view! but annotation can!
                boolean modifyEnable = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyContents();
                more_desc.setVisibility(modifyEnable ? View.VISIBLE : View.GONE);
                more_flatten.setVisibility(View.GONE);
                more_delete.setVisibility(modifyEnable ? View.VISIBLE : View.GONE);

                boolean moreEnable = enable || modifyEnable;
                more.setVisibility(moreEnable ? View.VISIBLE:View.GONE);
            }
        }

        private int getIconResource(String filename) {

            String ext = "";
            if (filename != null) {
                ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            }

            if (ext.equals("pdf")) {
                return R.drawable.fb_file_pdf;
            } else if (ext.equals("ofd")) {
                return R.drawable.fb_file_ofd;
            } else if (ext.equals("ppdf")) {
                return R.drawable.fb_file_ppdf;
            } else if (ext.equals("png")) {
                return R.drawable.fb_file_png;
            } else if (ext.equals("jpg")) {
                return R.drawable.fb_file_jpg;
            } else if (ext.equals("doc")) {
                return R.drawable.fb_file_doc;
            } else if (ext.equals("txt")) {
                return R.drawable.fb_file_txt;
            } else if (ext.equals("xls")) {
                return R.drawable.fb_file_xls;
            } else if (ext.equals("ppt")) {
                return R.drawable.fb_file_ppt;
            } else {
                return R.drawable.fb_file_other;
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig){
        if (saveAsDialog != null && saveAsDialog.isShowing()){
            saveAsDialog.setHeight(saveAsDialog.getDialogHeight());
            saveAsDialog.showDialog();
        }
    }

    public void clearItems() {
        mFileList.clear();
        notifyDataSetChanged();
    }
}
