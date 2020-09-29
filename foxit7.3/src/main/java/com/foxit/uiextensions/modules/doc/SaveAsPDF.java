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
package com.foxit.uiextensions.modules.doc;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.common.fxcrt.PauseCallback;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.concurrent.Callable;

import io.reactivex.Single;

public class SaveAsPDF {

    public static Single<Boolean> doSaveAsPDF(final PDFDoc srcDoc, final String destPath, final int saveFlags, final PauseCallback pause) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return doSaveAsPDFImpl(srcDoc, destPath, saveFlags, pause);
            }
        });
    }

    public static Single<Boolean> doSaveAsPDF(final String srcPath, final String destPath, final int saveFlags) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return doSaveAsPDFImpl(srcPath, null, destPath, saveFlags, null);
            }
        });
    }

    public static Single<Boolean> doSaveAsPDF(final String srcPath, final byte[] password, final String destPath, final int saveFlags, final PauseCallback pause) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return doSaveAsPDFImpl(srcPath, password, destPath, saveFlags, pause);
            }
        });
    }

    public static boolean doSaveAsPDFImpl(final String srcPath, byte[] password, String destPath, int saveFlags, PauseCallback pause) {
        try {
            if (AppUtil.isEmpty(srcPath) || AppUtil.isEmpty(destPath))
                return false;

            PDFDoc doc = new PDFDoc(srcPath);
            doc.load(password);
            return doSaveAsPDFImpl(doc, destPath, saveFlags, pause);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean doSaveAsPDFImpl(PDFDoc srcDoc, String destPath, int saveFlags, PauseCallback pause) {
        try {
            if (srcDoc == null || srcDoc.isEmpty() || AppUtil.isEmpty(destPath))
                return false;
            Progressive progressive = srcDoc.startSaveAs(destPath, saveFlags, pause);
            int state = Progressive.e_ToBeContinued;
            while (state == Progressive.e_ToBeContinued) {
                state = progressive.resume();
            }
            return state == Progressive.e_Finished;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

}
