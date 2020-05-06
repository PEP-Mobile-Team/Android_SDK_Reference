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
package com.foxit.uiextensions.modules.print;


import android.content.Context;
import android.print.PrintDocumentAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.utils.AppFileUtil;

public class PrintSettingOptions extends UIMatchDialog {

    private PDFViewCtrl mPDFViewCtrl;

    public PrintSettingOptions(Context context, PDFViewCtrl viewCtrl) {
        super(context);

        this.mPDFViewCtrl = viewCtrl;
        createView();
    }

    private View createView() {
        View view = View.inflate(mContext, R.layout.rv_print_setting, null);
        view.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        final CheckBox checkPrintAnnot = (CheckBox) view.findViewById(R.id.rv_switch_print_annot);
        setContentView(view);
        setTitle(mContext.getApplicationContext().getString(R.string.rv_print_setting_title));
        setButton(MatchDialog.DIALOG_OK | MatchDialog.DIALOG_CANCEL);
        setBackButtonVisible(View.GONE);

        setListener(new DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == UIMatchDialog.DIALOG_OK) {
                    UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager();

                    String filename = AppFileUtil.getFileNameWithoutExt(mPDFViewCtrl.getFilePath());
                    PrintDocumentAdapter adapter;
                    if (mPDFViewCtrl.isDynamicXFA()){
                        adapter = new XFAPrintAdapter(mContext, mPDFViewCtrl.getXFADoc(),filename, checkPrintAnnot.isChecked(), null);
                    }else {
                        adapter = new PDFPrintAdapter(mContext, mPDFViewCtrl.getDoc(), filename, checkPrintAnnot.isChecked(), null);
                    }

                    PDFPrint print = new PDFPrint
                            .Builder(uiExtensionsManager.getAttachedActivity(), mPDFViewCtrl.getFilePath())
                            .setAdapter(adapter)
                            .setPageCount(mPDFViewCtrl.getPageCount())
                            .print();
                }
                dismiss();
            }

            @Override
            public void onBackClick() {

            }
        });

        return view;
    }
}
