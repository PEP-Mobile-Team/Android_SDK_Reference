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
package com.foxit.uiextensions.modules.doc.docinfo;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.WStringArray;
import com.foxit.sdk.pdf.Metadata;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIMarqueeTextView;

import java.io.File;

/**
 * Class <CODE>DocInfoView</CODE> represents the basic information of pdf file.
 * <p/>
 * This class is used for showing the basic information of pdf file. It offers functions to initialize/show/FilePath Foxit PDF file basic information,
 * and also offers functions for global use.<br>
 * Any application should load Foxit PDF SDK by function {@link DocInfoView#init(String)} before calling any other Foxit PDF SDK functions.
 * When there is a need to show the basic information of pdf file, call function {@link DocInfoView#show()}.
 */
public class DocInfoView {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private boolean mIsPad = false;
    private String mFilePath = null;
    private SummaryInfo mSummaryInfo = null;

    DocInfoView(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mIsPad = AppDisplay.getInstance(context).isPad();
    }

    protected void init(String filePath) {
        setFilePath(filePath);
        mSummaryInfo = new SummaryInfo();
    }

    private void setFilePath(String path) {
        mFilePath = path;
    }

    public void show() {
        if (mSummaryInfo == null)
            return;
        mSummaryInfo.init();
        mSummaryInfo.show();
    }

    abstract class DocInfo {
        protected View mRootLayout = null;
        protected UIMatchDialog mDialog = null;
        protected String mCaption = null;

        abstract void init();

        abstract void show();
    }

    /**
     * Class <CODE>SummaryInfo</CODE> represents the basic information of pdf file.
     * such as: file name, file path, file size and so on.
     * <p/>
     * This class is used for showing the basic information of pdf file. It offers functions to initialize/show/FilePath Foxit PDF file basic information,
     * and also offers functions for global use.<br>
     * Any application should load Foxit PDF SDK by function {@link SummaryInfo#init()} before calling any other Foxit PDF SDK functions.
     * When there is a need to show the basic information of pdf file, call function {@link SummaryInfo#show()}.
     */
    class SummaryInfo extends DocInfo {
        public class DocumentInfo {
            public String mFilePath = null;
            public String mFileName = null;
            public String mAuthor = null;
            public String mSubject = null;
            public String mCreateTime = null;
            public String mModTime = null;
            public long mFileSize = 0;
        }

        SummaryInfo() {
            mCaption = AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info);
        }

        @Override
        void init() {
            String content = null;
            View itemView = null;
            TextView tvContent = null;

            mRootLayout = View.inflate(mContext, R.layout.rv_doc_info, null);
            initPadDimens();

            PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) return;

            DocumentInfo info = getDocumentInfo();
            // file information
            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_title);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_fileinfo));

            // filename
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_name_value);
            tvContent.setText(info.mFileName);

            // file path
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_path_value);
            tvContent.setText(AppUtil.getFileFolder(info.mFilePath));

            // file size
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_size_value);
            tvContent.setText(AppUtil.fileSizeToString(info.mFileSize));

            // author
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_author_value);
            tvContent.setText(info.mAuthor);

            // subject
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_subject_value);
            tvContent.setText(info.mSubject);

            // creation date
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_createdate_value);
            tvContent.setText(info.mCreateTime);

            // modify date
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_moddate_value);
            tvContent.setText(info.mModTime);

            // security information
            LinearLayout lySecurity = (LinearLayout) mRootLayout.findViewById(R.id.rv_doc_info_security);
            lySecurity.setVisibility(mPdfViewCtrl.isDynamicXFA() ? View.INVISIBLE : View.VISIBLE);
            if (mPdfViewCtrl.isDynamicXFA()) {
                ImageView divide0 = mRootLayout.findViewById(R.id.rv_doc_info_tracker_divide0);
                divide0.setVisibility(View.INVISIBLE);

                ImageView divide = mRootLayout.findViewById(R.id.rv_doc_info_tracker_divide);
                divide.setVisibility(View.INVISIBLE);
                return;
            }
            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_security_title);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_security));


            itemView = mRootLayout.findViewById(R.id.rv_doc_info_security);
            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_security_content);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PermissionInfo permInfo = new PermissionInfo();
                    permInfo.init();
                    permInfo.show();
                }
            });

            try {
                switch (doc.getEncryptionType()) {
                    case PDFDoc.e_EncryptPassword:
                        content = AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_security_standard);
                        break;
                    case PDFDoc.e_EncryptCertificate:
                        content = AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_security_pubkey);
                        break;
                    case PDFDoc.e_EncryptRMS:
                        content = AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_security_rms);
                        break;
                    case PDFDoc.e_EncryptCustom:
                        content = AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_security_custom);
                        break;
                    case PDFDoc.e_EncryptCDRM:
                        content = AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_security_cdrm);
                        break;
                    default:
                        if (mFilePath != null &&  mFilePath.toLowerCase().endsWith(".ppdf")) {
                            content = AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_security_rms);
                        } else {
                            content = AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_security_no);
                        }
                        break;
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
            tvContent.setText(content);
        }

        @Override
        void show() {
            mDialog = new UIMatchDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
            mDialog.setTitle(mCaption);
            mDialog.setContentView(mRootLayout);
            if (mIsPad) {
                mDialog.setBackButtonVisible(View.GONE);
            } else {
                mDialog.setBackButtonVisible(View.VISIBLE);
            }
            mDialog.setListener(new MatchDialog.DialogListener() {
                @Override
                public void onResult(long btType) {
                    mDialog.dismiss();
                }

                @Override
                public void onBackClick() {
                }
            });
            mDialog.showDialog(true);
        }

        DocumentInfo getDocumentInfo() {
            DocumentInfo info = new DocumentInfo();
            PDFDoc doc = mPdfViewCtrl.getDoc();
            info.mFilePath = mFilePath;
            if (mFilePath != null) {
                info.mFileName = AppUtil.getFileName(mFilePath);
                File file = new File(mFilePath);
                info.mFileSize = file.length();
            }

            try {
                Metadata metadata = new Metadata(doc);
                WStringArray authorArray = metadata.getValues("Author");
                info.mAuthor = authorArray.getSize() == 0 ? "Foxit SDK" : authorArray.getAt(0);
                WStringArray subArray = metadata.getValues("Subject");
                info.mSubject = subArray.getSize() == 0 ? "Foxit" : subArray.getAt(0);
                info.mCreateTime = AppDmUtil.getLocalDateString(metadata.getCreationDateTime());
                info.mModTime = AppDmUtil.getLocalDateString(metadata.getModifiedDateTime());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return info;
        }

        void initPadDimens() {
            int leftPadding = 0;
            int rightPadding = 0;
            if (mIsPad) {
                leftPadding = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_pad);
                rightPadding = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_right_margin_pad);
            } else {
                leftPadding = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_phone);
                rightPadding = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_right_margin_phone);
            }

            UIMarqueeTextView tvContent;
            // file name
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_name_value);
            tvContent.setPadding(0, 0, leftPadding + rightPadding, 0);

            // file path
            tvContent = (UIMarqueeTextView) mRootLayout.findViewById(R.id.rv_doc_info_fileinfo_path_value);
            tvContent.setPadding(0, 0, leftPadding + rightPadding, 0);

            if (!mIsPad)
                return;

            int arrIDForPadding[] = {
                R.id.rv_doc_info_fileinfo_title,
                R.id.table_row_file_name,
                R.id.table_row_file_path,
                R.id.table_row_file_size,
                R.id.table_row_file_author,
                R.id.table_row_file_subject,
                R.id.table_row_create_date,
                R.id.table_row_modify_date,
                R.id.rv_doc_info_security,
            };

            for (int i = 0; i < arrIDForPadding.length; i++) {
                View view = mRootLayout.findViewById(arrIDForPadding[i]);
                view.setPadding(leftPadding, 0, rightPadding, 0);
            }

            int arrIDForLayout[] = {
                R.id.rv_doc_info_fileinfo_title,
                R.id.rv_doc_info_fileinfo_name,
                R.id.rv_doc_info_fileinfo_name_value,
                R.id.rv_doc_info_fileinfo_path,
                R.id.rv_doc_info_fileinfo_path_value,
                R.id.rv_doc_info_fileinfo_size,
                R.id.rv_doc_info_fileinfo_size_value,
                R.id.rv_doc_info_fileinfo_author,
                R.id.rv_doc_info_fileinfo_author_value,
                R.id.rv_doc_info_fileinfo_subject,
                R.id.rv_doc_info_fileinfo_subject_value,
                R.id.rv_doc_info_fileinfo_createdate,
                R.id.rv_doc_info_fileinfo_createdate_value,
                R.id.rv_doc_info_fileinfo_moddate,
                R.id.rv_doc_info_fileinfo_moddate_value,
                R.id.rv_doc_info_security_title,
                R.id.rv_doc_info_security_content,
            };

            for (int i = 0; i < arrIDForLayout.length; i++) {
                View view = mRootLayout.findViewById(arrIDForLayout[i]);
                view.getLayoutParams().height = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_list_item_height_1l_pad);
            }
        }
    }

    /**
     * Class <CODE>PermissionInfo</CODE> represents the permission information of pdf file.
     * such as: print, modify, fill form, extract and so on.
     * <p/>
     * This class is used for showing the permission information of pdf file. It offers functions to initialize/show Foxit PDF file basic information,
     * and also offers functions for global use.<br>
     * Any application should load Foxit PDF SDK by function {@link PermissionInfo#init()} before calling any other Foxit PDF SDK functions.
     * When there is a need to show the basic information of pdf file, call function {@link PermissionInfo#show()}.
     */
    class PermissionInfo extends DocInfo {

        PermissionInfo() {
            mCaption = AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_permission);
        }

        void init() {
            TextView tvContent = null;
            mRootLayout = View.inflate(mContext.getApplicationContext(), R.layout.rv_doc_info_permissioin, null);

            initPadDimens();

            PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null)
                return;

            // summary
            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_title);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_permission_summary));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_print);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_permission_print));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_fillform);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_permission_fillform));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_annotform);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_permission_annotform));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_assemble);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_permission_assemble));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_modify);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_permission_modify));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_extractaccess);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_permission_extractaccess));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_extract);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_permission_extract));

            tvContent = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_signing);
            tvContent.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_doc_info_permission_signing));

            // on off switch
            TextView tvPrint = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_print_of);
            TextView tvFillForm = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_fillform_of);
            TextView tvAnnotForm = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_annotform_of);
            TextView tvAssemble = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_assemble_of);
            TextView tvModify = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_modify_of);
            TextView tvExtractAccess = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_extractaccess_of);
            TextView tvExtract = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_extract_of);
            TextView tvSigning = (TextView) mRootLayout.findViewById(R.id.rv_doc_info_permission_signing_of);

            String allowed = AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_allowed);
            String notAllowed = AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_notallowed);
            
            tvPrint.setText(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canPrint() ? allowed : notAllowed);
            tvFillForm.setText(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canFillForm() ? allowed : notAllowed);
            tvAnnotForm.setText(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot() ? allowed : notAllowed);
            tvAssemble.setText(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAssemble() ? allowed : notAllowed);
            tvModify.setText(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyContents() ? allowed : notAllowed);
            tvExtractAccess.setText(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canCopyForAssess() ? allowed : notAllowed);
            tvExtract.setText(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canCopy()? allowed : notAllowed);
            boolean canSign = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canSigning();
            tvSigning.setText(canSign ? allowed : notAllowed);
        }

        void initPadDimens() {
            if (!mIsPad)
                return;

            int idArray[] = {
                R.id.rv_doc_info_permission_title,
                R.id.rv_doc_info_permisson_print_rl,
                R.id.rv_doc_info_permission_fillform_rl,
                R.id.rv_doc_info_permission_annotform_rl,
                R.id.rv_doc_info_permission_assemble_rl,
                R.id.rv_doc_info_permission_modify_rl,
                R.id.rv_doc_info_permission_extractaccess_rl,
                R.id.rv_doc_info_permission_extract_rl,
                R.id.rv_doc_info_permission_signing_rl,
            };

            for (int i = 0; i < idArray.length; i++) {
                View view = mRootLayout.findViewById(idArray[i]);
                int leftPadding = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_pad);
                view.setPadding(leftPadding, 0, leftPadding, 0);
                view.getLayoutParams().height = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_list_item_height_1l_pad);
            }
        }

        void show() {
            if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
            Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            if (context == null) return;
            mDialog = new UIMatchDialog(context);
            mDialog.setTitle(mCaption);
            mDialog.setContentView(mRootLayout);
            mDialog.setBackButtonVisible(View.VISIBLE);
            mDialog.setListener(new MatchDialog.DialogListener() {
                @Override
                public void onResult(long btType) {
                }

                @Override
                public void onBackClick() {
                    mDialog.dismiss();
                }
            });
            mDialog.showDialog();
        }
    }
}

