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
package com.foxit.uiextensions.modules.panel.signature;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextUtils;
import android.widget.PopupWindow;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.addon.xfa.XFAPage;
import com.foxit.sdk.addon.xfa.XFAWidget;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.Signature;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.form.FormFillerUtil;
import com.foxit.uiextensions.modules.dynamicxfa.DynamicXFAModule;
import com.foxit.uiextensions.modules.panel.bean.SignatureBean;
import com.foxit.uiextensions.modules.signature.SignatureDataUtil;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureAnnotHandler;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureModule;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureSecurityHandler;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.IResult;
import com.foxit.uiextensions.utils.UIToast;

import java.util.ArrayList;
import java.util.HashMap;

public class SignaturePresenter {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private DigitalSignatureSecurityHandler mDSSecurityHandler;
    private ISignedVersionCallBack mSignedVersionCallback;
    public SignaturePresenter(Context context, PDFViewCtrl pdfViewCtrl, ISignedVersionCallBack callBack) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mSignedVersionCallback = callBack;
        DigitalSignatureModule module = (DigitalSignatureModule) ((UIExtensionsManager)(mPdfViewCtrl.getUIExtensionsManager())).getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE);
        if (module == null) return;
        mDSSecurityHandler = module.getSecurityHandler();
    }

    void loadSignatures(IResult<ArrayList<SignatureBean>, Object, Object> result) {
        SearchSignatureTask task = new SearchSignatureTask(mPdfViewCtrl, result);
        mPdfViewCtrl.addTask(task);
    }

    void view(int position, int pageIndex, String uuid) {
        SignatureInfo signatureInfo = getSignatureInfo(position, pageIndex, uuid);
        if (signatureInfo != null) {
            try {
                int[] byteRanges = new int[4];
                signatureInfo.signature.getByteRangeArray(byteRanges);
                long fileLength = mPdfViewCtrl.getDoc().getFileSize();
                int pos = byteRanges[2] + byteRanges[3];

                if (fileLength < pos) {
                    UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_security_failed_to_view));
                } else if (fileLength == pos) {
                    UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_panel_signature_view_current_version));
                } else {
                    if (mPdfViewCtrl.getFilePath() == null) {
                        UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_unknown_error));
                        return;
                    }
                    onPrepare();
                    PDFDoc pdfDoc = signatureInfo.signature.getSignedVersionDocument(mPdfViewCtrl.getFilePath());
                    if (pdfDoc.isEmpty()) return;
                    int error = pdfDoc.load(null);
                    if (error != Constants.e_ErrSuccess) {
                        onFinish();
                        String message = AppUtil.getMessage(mContext, error);
                        UIToast.getInstance(mContext).show(message);
                        return;
                    }

                    onOpen(pdfDoc, AppUtil.getFileName(mPdfViewCtrl.getFilePath()) + " - Signed Version");
//                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setViewSignedDocFlag(true);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else {
            UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_unknown_error));
        }
    }

    void verify(int position, int pageIndex, String uuid) {
        SignatureInfo signatureInfo = getSignatureInfo(position, pageIndex, uuid);
        if (signatureInfo != null && mDSSecurityHandler != null) {
            mDSSecurityHandler.verifySignature(signatureInfo.signature);
        } else {
            UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_unknown_error));
        }
    }

    void gotoPage(int position, final int pageIndex, String uuid, final boolean isShowSignList) {
        final SignatureInfo signatureInfo = getSignatureInfo(position, pageIndex, uuid);
        if (signatureInfo != null) {
            Task.CallBack callBack = new Task.CallBack() {
                @Override
                public void result(Task task) {
                    PopupWindow popupWindow = getPopupWindow();
                    if (popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }

                    if (signatureInfo.rect == null) {
                        UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_unknown_error));
                        return;
                    }
                    RectF rect = signatureInfo.rect;
                    RectF rectPageView = new RectF();

                    //Covert rect from the PDF coordinate system to the page view coordinate system,
                    // and show the annotation to the middle of the screen as possible.
                    if (mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rectPageView, pageIndex)) {
                        float devX = rectPageView.left - (mPdfViewCtrl.getWidth() - rectPageView.width()) / 2;
                        float devY = rectPageView.top - (mPdfViewCtrl.getHeight() - rectPageView.height()) / 2;
                        mPdfViewCtrl.gotoPage(pageIndex, devX, devY);
                    } else {
                        mPdfViewCtrl.gotoPage(pageIndex, new PointF(rect.left, rect.top));
                    }

                    if (mPdfViewCtrl.isDynamicXFA()) {
                        DynamicXFAModule xfaModule = (DynamicXFAModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DYNAMICXFA);
                        if (xfaModule != null) {
                            xfaModule.setCurrentXFAWidget(signatureInfo.xfaWidget);
                        }
                        return;
                    }

                    if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                    }

                    boolean canAdd = signatureInfo.isSigned || !signatureInfo.isReadOnly
                            && (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canFillForm());
                    if (canAdd) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(signatureInfo.widget);
                    }

                    if (isShowSignList && canAdd) {
                        HashMap<String, Object> map = SignatureDataUtil.getRecentData(mContext);
                        if (map != null && !TextUtils.isEmpty((String) map.get("dsgPath")) && map.get("rect") != null && map.get("bitmap") != null) {
                            DigitalSignatureAnnotHandler handler = (DigitalSignatureAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(AnnotHandler.TYPE_FORMFIELD_SIGNATURE);
                            if (handler != null) {
                                handler.showSignList();
                            }
                        }
                    }

                }
            };

            mPdfViewCtrl.addTask(new Task(callBack) {
                @Override
                protected void execute() {

                }
            });
        } else {
            UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_unknown_error));
        }
    }

    static class SignatureInfo {
        Signature signature;
        Widget widget;
        XFAWidget xfaWidget;
        int pageIndex;
        RectF rect;
        boolean isReadOnly;
        boolean isSigned;
    }

    private SignatureInfo getSignatureInfo(int position, int pageIndex, String uuid) {
        try {
            if (mPdfViewCtrl.isDynamicXFA()) {
                XFAPage page = mPdfViewCtrl.getXFADoc().getPage(pageIndex);
                XFAWidget widget = page.getWidgetByFullName(uuid);
                if (!widget.isEmpty() && widget.getType() == XFAWidget.e_WidgetTypeSignature) {
                    SignatureInfo signatureInfo = new SignatureInfo();
                    signatureInfo.signature = widget.getSignature();
                    signatureInfo.xfaWidget = widget;
                    signatureInfo.pageIndex = pageIndex;
                    signatureInfo.rect = AppUtil.toRectF(widget.getRect());
                    signatureInfo.isReadOnly = false;
                    signatureInfo.isSigned = signatureInfo.signature.isSigned();
                    return signatureInfo;
                }
            } else {
                int count = mPdfViewCtrl.getDoc().getSignatureCount();
                for (int i = 0; i < count; i++) {
                    Signature signature = mPdfViewCtrl.getDoc().getSignature(i);
                    if (!signature.isEmpty()) {

                        Widget widget = signature.getControl(0).getWidget();
                        if (!widget.isEmpty() && widget.getDict().getObjNum() == Integer.parseInt(uuid)) {
                            SignatureInfo signatureInfo = new SignatureInfo();
                            signatureInfo.signature = signature;
                            signatureInfo.pageIndex = pageIndex;
                            signatureInfo.isSigned = signature.isSigned();
                            signatureInfo.widget = widget;
                            signatureInfo.rect = AppUtil.toRectF(widget.getRect());
                            signatureInfo.isReadOnly = FormFillerUtil.isReadOnly(widget);
                            return signatureInfo;
                        }
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }

    private PopupWindow getPopupWindow() {
        SignaturePanelModule module = (SignaturePanelModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_SIGNATUREPANEL);
        if (module != null) {
            return module.getPopupWindow();
        }
        return null;
    }

    private class SearchSignatureTask extends Task {

        PDFViewCtrl mPdfViewCtrl;
        boolean mRet;
        int signedVersion = 1;
        ArrayList<SignatureBean> mList = new ArrayList<>();
        SearchSignatureTask(PDFViewCtrl pdfViewCtrl, final IResult<ArrayList<SignatureBean>, Object, Object> listener) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    listener.onResult(((SearchSignatureTask) task).mRet, ((SearchSignatureTask) task).mList, null, null);
                }
            });
            mPdfViewCtrl = pdfViewCtrl;
        }

        @Override
        protected void execute() {
            if (mPdfViewCtrl.isDynamicXFA()) {
                mRet = searchSignaturesInXFADoc();
            } else {
                mRet = searchSignatures();
            }
        }

        private boolean searchSignatures() {
            try {
                signedVersion = 1;
                int count = mPdfViewCtrl.getDoc().getSignatureCount();
                for (int i = 0; i < count; i++) {
                    Signature signature = mPdfViewCtrl.getDoc().getSignature(i);
                    if (!signature.isEmpty()) {
                        SignatureBean bean = createSignatureBean(signature);

                        Widget widget = signature.getControl(0).getWidget();
                        if (!widget.isEmpty()) {
                            bean.setReadOnly(FormFillerUtil.isReadOnly(widget));
                            bean.setPageIndex(widget.getPage().getIndex());
                            String uuid = String.valueOf(widget.getDict().getObjNum());
                            bean.setUuid(uuid);
                            mList.add(bean);
                        }
                    }
                }
                return true;
            } catch (PDFException e) {
            }
            return false;
        }

        private boolean searchSignaturesInXFADoc() {
            try {
                int pageCount = mPdfViewCtrl.getXFADoc().getPageCount();
                for (int pi = 0; pi < pageCount; pi ++) {
                    XFAPage page = mPdfViewCtrl.getXFADoc().getPage(pi);
                    int widgetCount = page.getWidgetCount();
                    for (int wi = 0; wi < widgetCount; wi ++) {
                        XFAWidget widget = page.getWidget(wi);
                        if (!widget.isEmpty() && widget.getType() == XFAWidget.e_WidgetTypeSignature) {
                            Signature signature = widget.getSignature();
                            if (signature.isEmpty()) continue;
                            SignatureBean bean = createSignatureBean(signature);
                            bean.setPageIndex(widget.getXFAPage().getIndex());
                            bean.setReadOnly(false);
                            bean.setUuid(widget.getName(XFAWidget.e_WidgetNameTypeFullName));
                            mList.add(bean);
                        }
                    }
                }
                return true;
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return false;
        }

        private SignatureBean createSignatureBean(Signature signature) throws PDFException {
            SignatureBean bean = new SignatureBean();
            bean.setFlag(SignatureAdapter.FLAG_SIGNATURE);
            bean.setSigned(signature.isSigned());
            if (bean.isSigned()) {
                String signer = signature.getKeyValue(Signature.e_KeyNameSigner);
                if (TextUtils.isEmpty(signer)) {
                    signer = AppUtil.getEntryName(signature.getCertificateInfo("Subject"), "CN=");
                }
                bean.setSigner(signer);
                bean.setDate(AppDmUtil.getLocalDateString(signature.getSignTime()));
                bean.setSignedIndex(signedVersion ++);
            } else {
                bean.setSigner(signature.getName());
            }
            return bean;
        }
    }

    public interface ISignedVersionCallBack {
        void onPrepare();
        void onOpen(PDFDoc pdfDoc, String fileName);
        void onFinish();
    }

    private void onPrepare() {
        if (mSignedVersionCallback != null) {
            mSignedVersionCallback.onPrepare();
        }
    }

    private void onOpen(PDFDoc pdfDoc, String fileName) {
        if (mSignedVersionCallback != null) {
            mSignedVersionCallback.onOpen(pdfDoc, fileName);
        }
    }

    private void onFinish() {
        if (mSignedVersionCallback != null) {
            mSignedVersionCallback.onFinish();
        }
    }
}
