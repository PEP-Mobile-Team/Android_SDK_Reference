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
package com.foxit.uiextensions.modules.panel.filespec;

import android.app.Activity;
import android.content.Context;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.pdf.FileSpec;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FileAttachment;
import com.foxit.sdk.pdf.objects.PDFDictionary;
import com.foxit.sdk.pdf.objects.PDFNameTree;
import com.foxit.sdk.pdf.objects.PDFObject;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.fileattachment.FileAttachmentUtil;
import com.foxit.uiextensions.browser.adapter.FileAttachmentAdapter;
import com.foxit.uiextensions.modules.panel.bean.FileBean;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppIntentUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.io.File;
import java.util.ArrayList;

public class FileAttachmentPresenter {

    private FileAttachmentViewer viewer;

    private PDFViewCtrl mPdfViewCtrl;

    private PDFNameTree pdfNameTree;

    private ArrayList<FileBean> list;

    private ArrayList<Annot> annotList;

    private Context mContext;

    public FileAttachmentPresenter(Context context, PDFViewCtrl pdfViewCtrl, FileAttachmentViewer viewer) {
        this.viewer = viewer;
        list = new ArrayList<>();
        annotList = new ArrayList<>();
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
    }

    private boolean hasNameTree() {
        try {
            PDFDictionary catalog = mPdfViewCtrl.getDoc().getCatalog();
            return catalog.hasKey("Names");
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void initPDFNameTree(boolean reInit){
        if ((this.pdfNameTree == null || reInit)) {
            pdfNameTree = null;
            if (hasNameTree()) {
                createNameTree();
            }
        }
    }

    public void reInitPDFNameTree(){
        if (hasNameTree()) {
            createNameTree();
        }
    }

    private void createNameTree() {
        try {
            this.pdfNameTree = new PDFNameTree(mPdfViewCtrl.getDoc(), PDFNameTree.e_EmbeddedFiles);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Annot> getAnnotList() {
        return annotList;
    }

    public void searchFileAttachment(boolean isLoadAnnotation){
        SearchFileAttachmentTask task = new SearchFileAttachmentTask(mContext, mPdfViewCtrl, pdfNameTree, isLoadAnnotation, new OnSearchEndListener() {
            @Override
            public void onResult(boolean success, ArrayList<FileBean> list1, ArrayList<Annot> list2) {
                list.clear();
                annotList.clear();
                if (list1 != null) list.addAll(list1);
                if (list2 != null) annotList.addAll(list2);

                viewer.success(list);
            }
        });
        mPdfViewCtrl.addTask(task);
    }

    private class SearchFileAttachmentTask extends Task{

        private PDFViewCtrl pdfViewCtrl;
        private PDFNameTree nameTree;
        private ArrayList<FileBean> mList;
        private ArrayList<Annot> mAnnotList;
        private boolean isLoadAnnotation;
        private Context mSearchContext;

        private SearchFileAttachmentTask(Context context, PDFViewCtrl viewCtrl, PDFNameTree pdfNameTree, boolean isLoadAnnotation, final OnSearchEndListener onSearchEndListener) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    SearchFileAttachmentTask task1 = (SearchFileAttachmentTask) task;
                    onSearchEndListener.onResult(true, task1.mList,task1.mAnnotList);
                }
            });
            this.pdfViewCtrl = viewCtrl;
            this.nameTree = pdfNameTree;
            this.isLoadAnnotation = isLoadAnnotation;
            this.mSearchContext = context;
        }

        @Override
        protected void execute() {
            if (mList == null)
                mList = new ArrayList<>();
            if (mAnnotList == null)
                mAnnotList = new ArrayList<>();

            if (nameTree != null) {
                try {
                    int nOrgCount = nameTree.getCount();
                    if (nOrgCount > 0) {
                        FileBean fb = new FileBean();
                        fb.setFlag(FileAttachmentAdapter.FLAG_TAG);
                        fb.setTag(mSearchContext.getApplicationContext().getString(R.string.rv_panel_attachment_label));
                        mList.add(fb);
                    }
                    for (int o = 0; o < nOrgCount; o++) {
                        String name = nameTree.getName(o);
                        PDFObject object = nameTree.getObj(name);
                        FileBean item = new FileBean();
                        FileSpec fs = new FileSpec(pdfViewCtrl.getDoc(), object);
                        if (!fs.isEmpty()) {
                            item.setName(name);
                            item.setTitle(fs.getFileName());
                            item.setSize(AppDmUtil.getLocalDateString(fs.getModifiedDateTime()) + " " + AppFileUtil.formatFileSize(fs.getFileSize()));
                            item.setFlag(FileAttachmentAdapter.FLAG_NORMAL);
                            item.setDesc(fs.getDescription());
                            mList.add(item);
                        }
                    }

                } catch (PDFException e) {
                    e.printStackTrace();
                }

            }
            //load annot
            if (isLoadAnnotation) {
                try {
                    int pagecount = pdfViewCtrl.getDoc().getPageCount();
                    for (int i = 0; i < pagecount; i++) {
                        PDFPage pdfPage = pdfViewCtrl.getDoc().getPage(i);
                        int annotcount = pdfPage.getAnnotCount();
                        int count = 0;
                        for (int j = 0; j < annotcount; j++) {
                            Annot annot = AppAnnotUtil.createAnnot(pdfPage.getAnnot(j));
                            if (annot != null && annot.getType() == Annot.e_FileAttachment) {
                                count += 1;
                                FileSpec fileSpec = ((FileAttachment) annot).getFileSpec();
                                if (!fileSpec.isEmpty()) {
                                    FileBean item = new FileBean();
                                    item.setTitle(fileSpec.getFileName());
                                    item.setName(fileSpec.getFileName());
                                    item.setSize(AppDmUtil.getLocalDateString(fileSpec.getModifiedDateTime()) + " " + AppFileUtil.formatFileSize(fileSpec.getFileSize()));
                                    item.setFlag(FileAttachmentAdapter.FLAG_ANNOT);
                                    item.setDesc(annot.getContent());
                                    item.setUuid(annot.getUniqueID());
                                    item.setCanDelete(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
                                    mList.add(item);
                                    mAnnotList.add(annot);
                                }
                            }
                        }
                        if (count > 0) {
                            FileBean fb = new FileBean();
                            fb.setFlag(FileAttachmentAdapter.FLAG_TAG);
                            int pageIndex = i+1;
                            fb.setTag(mSearchContext.getApplicationContext().getString(R.string.attachment_page_tab, pageIndex));
                            mList.add(mList.size() - count, fb);
                        }
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private interface OnSearchEndListener {
        void onResult(boolean success, ArrayList<FileBean> list, ArrayList<Annot> annotList);
    }

    private String rename(String name) throws PDFException {
        if (pdfNameTree == null) return "";
        if (!pdfNameTree.hasName(name))
            return name;
        int lastIndex = name.lastIndexOf('.');
        if (lastIndex == -1){
            lastIndex = name.length()-1;
        }
        String oldName = name.substring(0, lastIndex);
        String copyName = oldName + "-Copy";
        name = name.replace(oldName, copyName);
        return rename(name);
    }

    public void add(String name, String path) {
         if (pdfNameTree == null) {
             createNameTree();
             if (pdfNameTree == null) return;
         }
        try {
            name = rename(name);
            int index = 0;
            boolean insert = false;
            for (FileBean b : list) {
                index += 1;
                if (b.getFlag() == FileAttachmentAdapter.FLAG_NORMAL) {
                    insert = true;
                    break;
                }
            }

            FileSpec pNewFile = new FileSpec(mPdfViewCtrl.getDoc());
            pNewFile.setFileName(name);
            pNewFile.embed(path);
            pNewFile.setCreationDateTime(AppDmUtil.currentDateToDocumentDate());
            pNewFile.setModifiedDateTime(AppDmUtil.javaDateToDocumentDate(new File(path).lastModified()));
            PDFDictionary dict = pNewFile.getDict();
            pdfNameTree.add(pNewFile.getFileName(), dict);

            FileBean item = new FileBean();
            item.setTitle(pNewFile.getFileName());
            item.setName(pNewFile.getFileName());
            item.setSize(AppDmUtil.getLocalDateString(pNewFile.getModifiedDateTime()) + " " + AppFileUtil.formatFileSize(pNewFile.getFileSize()));
            item.setFlag(FileAttachmentAdapter.FLAG_NORMAL);
            item.setDesc(pNewFile.getDescription());

            if (!insert) {
                FileBean fb = new FileBean();
                fb.setFlag(FileAttachmentAdapter.FLAG_TAG);
                fb.setTag(mContext.getApplicationContext().getString(R.string.rv_panel_attachment_label));
                list.add(fb);
                list.add(item);
            } else {
                list.add(index + pdfNameTree.getCount() - 2, item);
            }


        } catch (PDFException e) {
            e.printStackTrace();
        }

        viewer.success(list);

    }

    public void add(Annot annot) {
        annotList.add(annot);
        try {
            int pageIndex = annot.getPage().getIndex()+1;
            int index = 0;
            boolean insert = false;
            for (FileBean b : list) {
                index += 1;
                if (b.getFlag() == FileAttachmentAdapter.FLAG_TAG && b.getTag().endsWith("" + pageIndex)) {
                    insert = true;
                    break;
                }
            }

            FileSpec pNewFile = ((FileAttachment) annot).getFileSpec();
            FileBean item = new FileBean();
            item.setTitle(pNewFile.getFileName());
            item.setName(pNewFile.getFileName());
            item.setSize(AppDmUtil.getLocalDateString(pNewFile.getModifiedDateTime()) + " " + AppFileUtil.formatFileSize(pNewFile.getFileSize()));
            item.setFlag(FileAttachmentAdapter.FLAG_ANNOT);
            item.setDesc(annot.getContent());
            item.setUuid(annot.getUniqueID());
            item.setCanDelete(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));

            if (!insert) {
                FileBean fb = new FileBean();
                fb.setFlag(FileAttachmentAdapter.FLAG_TAG);
                fb.setTag(mContext.getApplicationContext().getString(R.string.attachment_page_tab, pageIndex));
                list.add(fb);
                list.add(item);
            } else {
                list.add(index, item);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        viewer.success(list);
    }

    public void update(final int index, final String content) {
        try {
            //update file panel file
            if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_NORMAL) {
                if (pdfNameTree == null) return;
                String name = list.get(index).getName();
                PDFObject obj = pdfNameTree.getObj(name);
                FileSpec pNewFile = new FileSpec(mPdfViewCtrl.getDoc(), obj);
                pNewFile.setDescription(content);

                pdfNameTree.setObj(name, pNewFile.getDict());
                list.get(index).setDesc(content);

                viewer.success(list);
            } else if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_ANNOT) {
                //update annot file
                FileBean i = list.get(index);
                String uuid = i.getUuid();
                for (Annot a : annotList) {
                    if (uuid.equals(a.getUniqueID())) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().
                                modifyAnnot(a, new UIAnnotReply.CommentContent(a, content), true, new Event.Callback() {
                            @Override
                            public void result(Event event, boolean success) {
                              if (success){
                                  list.get(index).setDesc(content);
                                  viewer.success(list);
                              }
                            }
                        });
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void updateByOutside(Annot annot){
        try {
            for (FileBean b : list) {
                if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(annot.getUniqueID())) {
                    if (!b.getDesc().equals(annot.getContent())){
                        b.setDesc(annot.getContent());
                        viewer.success(list);
                    }
                    break;
                }
            }
        } catch (PDFException e){
            e.printStackTrace();
        }
    }

    private void clearTag(String content){
        for (FileBean item : list){
            if (item.getFlag()==FileAttachmentAdapter.FLAG_TAG && content.equals(item.getTag())){
                list.remove(item);
                break;
            }
        }
    }

    public void delete(int count, int start, int end) {
        if (count == 0) {
            viewer.fail(FileAttachmentViewer.DELETE, null);
        }
        boolean clear = false;
        for (int i = start; i <= end; i++) {
            FileBean bean = list.get(i);
            if (bean.getFlag() == FileAttachmentAdapter.FLAG_TAG) {
                //list.remove(i);
                continue;
            }
            try {
                if (pdfNameTree == null) return;
                pdfNameTree.removeObj(list.get(i).getTitle());
                list.remove(i);
                if (pdfNameTree.getCount() == 0){
                    clear = true;
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
        if (clear){
            clearTag(mContext.getApplicationContext().getString(R.string.rv_panel_attachment_label));
        }
        viewer.success(list);
    }

    public void delete(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if(!annotList.contains(annot)){
            return;
        }
        boolean clear = true;
        int index = 0;
        try {
            index = annot.getPage().getIndex()+1;
            for (FileBean b : list) {
                if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(annot.getUniqueID())) {
                    list.remove(b);
                    break;
                }
            }
            annotList.remove(annot);
            ((UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager()).getDocumentManager().removeAnnot(annot, true, null);

            for (Annot item: annotList){
                if(item.getPage().getIndex() == annot.getPage().getIndex()) {
                    clear = false;
                    break;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        if (clear){
            clearTag(mContext.getApplicationContext().getString(R.string.attachment_page_tab, index));
        }

        viewer.success(list);
    }

    public void deleteByOutside(PDFViewCtrl pdfViewCtrl, Annot annot) {
        if(!annotList.contains(annot)){
            return;
        }
        boolean clear = true;
        int index = 0;
        try {
            index = annot.getPage().getIndex()+1;
            for (FileBean b : list) {
                if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(annot.getUniqueID())) {
                    list.remove(b);
                    break;
                }
            }
            annotList.remove(annot);
            for (Annot item: annotList){
                if(item.getPage().getIndex() == annot.getPage().getIndex()) {
                    clear = false;
                    break;
                }
            }
            ((UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
        } catch (PDFException e) {
            e.printStackTrace();
        }

        if (clear){
            clearTag(mContext.getApplicationContext().getString(R.string.attachment_page_tab, index));
        }

        viewer.success(list);
    }

    public void flatten(PDFViewCtrl pdfViewCtrl, final Annot annot) {
        if (!annotList.contains(annot)) {
            return;
        }

        UIAnnotFlatten.flattenAnnot(pdfViewCtrl, annot, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success){
                    try {
                        for (FileBean b : list) {
                            if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(annot.getUniqueID())) {
                                list.remove(b);
                                break;
                            }
                        }
                        annotList.remove(annot);

                        boolean clear = true;
                        int index = annot.getPage().getIndex() + 1;
                        for (Annot item : annotList) {
                            if (item.getPage().getIndex() == annot.getPage().getIndex()) {
                                clear = false;
                                break;
                            }
                        }

                        if (clear) {
                            clearTag(mContext.getApplicationContext().getString(R.string.attachment_page_tab, index));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
                viewer.success(list);
            }
        });
    }

    public void save(final PDFViewCtrl pdfViewCtrl, final int index, final String path) {
        if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_NORMAL) {
            try {
                if (pdfNameTree == null) return;
                FileSpec fileSpec = new FileSpec(mPdfViewCtrl.getDoc(), pdfNameTree.getObj(list.get(index).getName()));
                FileAttachmentUtil.saveAttachment(pdfViewCtrl, path, fileSpec, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                    }

                });

            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_ANNOT) {
            try {
                FileBean i = list.get(index);
                String uuid = i.getUuid();
                for (Annot a : annotList) {
                    if (uuid.equals(a.getUniqueID())) {
                        FileSpec fileSpec = ((FileAttachment) a).getFileSpec();
                        FileAttachmentUtil.saveAttachment(pdfViewCtrl, path, fileSpec, new Event.Callback() {
                            @Override
                            public void result(Event event, boolean success) {
                            }

                        });

                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean endsWith(String str, String suffix, boolean ignoreCase) {
        if (str != null && suffix != null) {
            if (suffix.length() > str.length()) {
                return false;
            } else {
                int strOffset = str.length() - suffix.length();
                return str.regionMatches(ignoreCase, strOffset, suffix, 0, suffix.length());
            }
        } else {
            return str == null && suffix == null;
        }
    }

    public void open(final PDFViewCtrl pdfViewCtrl, final int index, String p) {
//        path = path  + UUID.randomUUID().toString().split("-")[0] + ".pdf";
        viewer.openPrepare();
        final String path = p + list.get(index).getTitle();
        if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_NORMAL) {
            try {
                if (pdfNameTree == null) return;
                final FileSpec fileSpec = new FileSpec(mPdfViewCtrl.getDoc(), pdfNameTree.getObj(list.get(index).getName()));
                FileAttachmentUtil.saveAttachment(pdfViewCtrl, path, fileSpec, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            String ExpName = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                            if (ExpName.equals("pdf") || ExpName.equals("ppdf")) {
                                viewer.openStart(path, list.get(index).getTitle());
                            } else {
                                viewer.openFinished();
                                if (pdfViewCtrl.getUIExtensionsManager() == null) return;
                                Context context = ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                                if (context == null) return;
                                AppIntentUtil.openFile((Activity) context, path);
                            }
                        }
                    }

                });

            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else if (list.get(index).getFlag() == FileAttachmentAdapter.FLAG_ANNOT) {
            try {
                FileBean i = list.get(index);
                String uuid = i.getUuid();
                for (Annot a : annotList) {
                    if (uuid.equals(a.getUniqueID())) {
                        FileSpec fileSpec = ((FileAttachment) a).getFileSpec();
                        FileAttachmentUtil.saveAttachment(pdfViewCtrl, path, fileSpec, new Event.Callback() {
                            @Override
                            public void result(Event event, boolean success) {
                                if (success) {
                                    String ExpName = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                                    if (ExpName.equals("pdf")) {
                                        viewer.openStart(path, list.get(index).getTitle());
                                    } else {
                                        viewer.openFinished();
                                        if (pdfViewCtrl.getUIExtensionsManager() == null) return;
                                        Context context = ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                                        if (context == null) return;
                                        AppIntentUtil.openFile((Activity) context, path);
                                    }
                                }
                            }

                        });

                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * viewer
     */
    public interface FileAttachmentViewer {
        int LOAD = 1;
        int DELETE = 2;
        int RENAME = 3;
        int CLEAR = 4;

        void success(ArrayList<FileBean> list);

        void fail(int rct, Object o);

        void openPrepare();

        void openStart(String path, String name);

        void openFinished();
    }
}
