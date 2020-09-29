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
package com.foxit.uiextensions.modules.print;


import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;

import androidx.annotation.NonNull;

import java.io.InputStream;

public class PDFPrint {
    final PrintController printController;

    private PDFPrint(Context context) {
        printController = new PrintController(context);
    }

    private void print() {
        printController.print();
    }

    public static class Builder {
        private PrintController.PrintParmas parmas;

        public Builder(@NonNull Context context){
            parmas = new PrintController.PrintParmas(context);
        }

        public Builder(@NonNull Context context, @NonNull String inputPath) {
            parmas = new PrintController.PrintParmas(context);
            parmas.mInputPath = inputPath;
        }

        public Builder(@NonNull Context context, @NonNull InputStream stream) {
            parmas = new PrintController.PrintParmas(context);
            parmas.mInputStream = stream;
        }

        public Builder setPrintJobName(String printJobName) {
            parmas.mPrintJobName = printJobName;
            return this;
        }

        public Builder setOutputFileName(String fileName) {
            parmas.mOutputFileName = fileName;
            return this;
        }

        public Builder setAdapter(PrintDocumentAdapter adapter) {
            parmas.mAdapter = adapter;
            return this;
        }

        public Builder setPageCount(int pageCount){
            parmas.mPageCount = pageCount;
            return this;
        }

        public Builder setPrintAttributes(PrintAttributes attributes) {
            parmas.mAttributes = attributes;
            return this;
        }

        public PDFPrint create() {
            PDFPrint pdfPrint = new PDFPrint(parmas.mContext);
            parmas.apply(pdfPrint.printController);
            return pdfPrint;
        }

        public PDFPrint print() {
            PDFPrint pdfPrint = create();
            pdfPrint.print();
            return pdfPrint;
        }
    }
}
