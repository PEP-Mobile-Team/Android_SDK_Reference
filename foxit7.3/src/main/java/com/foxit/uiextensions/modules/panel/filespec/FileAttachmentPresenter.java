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
package com.foxit.uiextensions.modules.panel.filespec;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.pdf.FileSpec;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FileAttachment;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.sdk.pdf.objects.PDFDictionary;
import com.foxit.sdk.pdf.objects.PDFNameTree;
import com.foxit.sdk.pdf.objects.PDFObject;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.fileattachment.FileAttachmentUtil;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.modules.panel.bean.FileBean;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppIntentUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        if (mPdfViewCtrl.getDoc() != null) {
            try {
                PDFDictionary catalog = mPdfViewCtrl.getDoc().getCatalog();
                return catalog.hasKey("Names");
            } catch (PDFException e) {
                e.printStackTrace();
            }
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
                            item.setSize(getModifiedDateTimeString(fs) + " " + AppFileUtil.formatFileSize(fs.getFileSize()));
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
                        if (pdfPage.isEmpty()) continue;
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
                                    item.setSize(getModifiedDateTimeString(fileSpec) + " " + AppFileUtil.formatFileSize(fileSpec.getFileSize()));
                                    item.setFlag(FileAttachmentAdapter.FLAG_ANNOT);
                                    item.setDesc(annot.getContent());
                                    item.setUuid(AppAnnotUtil.getAnnotUniqueID(annot));

                                    if (GroupManager.getInstance().isGrouped(mPdfViewCtrl, annot)) {
                                        item.setCanDelete(false);
                                        item.setCanFlatten(false);
                                    } else {
                                        item.setCanDelete(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
                                        item.setCanFlatten(true);
                                    }
                                    item.setCanComment(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
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

    private String getModifiedDateTimeString(FileSpec fileSpec) {
        String date;
        try {
            date = AppDmUtil.getLocalDateString(fileSpec.getModifiedDateTime());
        } catch (PDFException e) {
            date = "unknown";
        }
        return date;
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
            item.setUuid(AppAnnotUtil.getAnnotUniqueID(annot));
            if (GroupManager.getInstance().isGrouped(mPdfViewCtrl, annot)) {
                item.setCanDelete(false);
                item.setCanFlatten(false);
            } else {
                item.setCanDelete(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
                item.setCanFlatten(true);
            }
            item.setCanComment(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));

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
                    if (uuid.equals(AppAnnotUtil.getAnnotUniqueID(a))) {
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
                if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
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

    public void updateByGroup(Annot annot){
        for (FileBean b : list) {
            if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
                if (GroupManager.getInstance().isGrouped(mPdfViewCtrl, annot)) {
                    b.setCanDelete(false);
                    b.setCanFlatten(false);
                } else {
                    b.setCanDelete(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
                    b.setCanFlatten(true);
                }
                b.setCanComment(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
                viewer.success(list);
                break;
            }
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
                if (pdfNameTree.removeObj(list.get(i).getName())) {
                    list.remove(i);
                    if (pdfNameTree.getCount() == 0) {
                        clear = true;
                    }
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
                if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
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

    public void updateByDeleteGroupAnnot(Annot annot) {
        for (FileBean b : list) {
            if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
                b.setCanDelete(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
                b.setCanFlatten(true);
                viewer.success(list);
                break;
            }
        }
    }

    public void deleteByOutside(PDFViewCtrl pdfViewCtrl, Annot annot) {
        try {
            if (annot.getType() == Annot.e_FileAttachment) {
                List<Annot> annots = getAnnotList();
                for (Annot a : annots) {
                    if (a.getPage().getIndex() == annot.getPage().getIndex()
                            && AppAnnotUtil.getAnnotUniqueID(a).equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
                        deleteAnnot(pdfViewCtrl, a);
                        return;
                    }
                }
            }
        } catch (PDFException e){
            e.printStackTrace();
        }
    }

    private void deleteAnnot(PDFViewCtrl pdfViewCtrl, Annot annot){
        if(!annotList.contains(annot)){
            return;
        }

        boolean clear = true;
        int index = 0;
        try {
            index = annot.getPage().getIndex()+1;
            for (FileBean b : list) {
                if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
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

        try {
            final String uniqueID = AppAnnotUtil.getAnnotUniqueID(annot);
            final int pageIndex = annot.getPage().getIndex();
            ((UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager()).getDocumentManager().flattenAnnot(annot, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success){
                        try {
                            for (FileBean b : list) {
                                if (!AppUtil.isBlank(b.getUuid()) && b.getUuid().equals(uniqueID)) {
                                    list.remove(b);
                                    break;
                                }
                            }
                            annotList.remove(annot);

                            boolean clear = true;
                            int index = pageIndex + 1;
                            for (Annot item : annotList) {
                                if (item.getPage().getIndex() == pageIndex) {
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
        }catch (PDFException e){
            e.printStackTrace();
        }
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
                    if (uuid.equals(AppAnnotUtil.getAnnotUniqueID(a))) {
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
                    if (uuid.equals(AppAnnotUtil.getAnnotUniqueID(a))) {
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
