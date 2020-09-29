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
import com.foxit.sdk.addon.optimization.ImageSettings;
import com.foxit.sdk.addon.optimization.MonoImageSettings;
import com.foxit.sdk.addon.optimization.Optimizer;
import com.foxit.sdk.addon.optimization.OptimizerSettings;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.concurrent.Callable;

import io.reactivex.Single;

public class OptimizerPDF {

    public static Single<Boolean> doOptimizerPDF(final PDFDoc doc, final int imageQuality, final int monoImageQuality) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return doOptimizerPDFImpl(doc, imageQuality, monoImageQuality);
            }
        });
    }

    public static Single<Boolean> doOptimizerPDF(final String path, final int imageQuality, final int monoImageQuality) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return doOptimizerPDFImpl(path, null, imageQuality, monoImageQuality);
            }
        });
    }

    public static Single<Boolean> doOptimizerPDF(final String path, final byte[] password, final int imageQuality, final int monoImageQuality) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return doOptimizerPDFImpl(path, password, imageQuality, monoImageQuality);
            }
        });
    }

    public static boolean doOptimizerPDFImpl(String path, final byte[] password, int imageQuality, int monoImageQuality) {
        try {
            if (AppUtil.isEmpty(path))
                return false;
            PDFDoc doc = new PDFDoc(path);
            doc.load(password);
            return doOptimizerPDFImpl(doc, imageQuality, monoImageQuality);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean doOptimizerPDFImpl(PDFDoc doc, int imageQuality, int monoImageQuality) {
        try {
            if (doc == null || doc.isEmpty())
                return false;
            OptimizerSettings optimizerSettings = new OptimizerSettings();

            ImageSettings imageSettings = new ImageSettings();
            imageSettings.setQuality(imageQuality);
            imageSettings.setCompressionMode(ImageSettings.e_ImageCompressjpeg);
            optimizerSettings.setColorGrayImageSettings(imageSettings);

            MonoImageSettings monoImageSettings = new MonoImageSettings();
            monoImageSettings.setQuality(monoImageQuality);
            monoImageSettings.setCompressionMode(MonoImageSettings.e_ImageCompressjpeg2);
            optimizerSettings.setMonoImageSettings(monoImageSettings);

            optimizerSettings.setOptimizerOptions(OptimizerSettings.e_OptimizerCompressImages);
            Progressive progressive = Optimizer.optimize(doc, optimizerSettings, null);
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
