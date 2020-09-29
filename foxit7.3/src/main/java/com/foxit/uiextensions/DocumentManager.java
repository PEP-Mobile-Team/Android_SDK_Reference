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
package com.foxit.uiextensions;

import android.graphics.PointF;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.foxit.sdk.ActionCallback;
import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Library;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.Signature;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.objects.PDFArray;
import com.foxit.sdk.pdf.objects.PDFDictionary;
import com.foxit.sdk.pdf.objects.PDFObject;
import com.foxit.sdk.pdf.objects.PDFStream;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotEventListener;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.IFlattenEventListener;
import com.foxit.uiextensions.annots.IGroupEventListener;
import com.foxit.uiextensions.annots.IImportAnnotsEventListener;
import com.foxit.uiextensions.annots.IRedactionEventListener;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.form.FormFillerUtil;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.annots.multiselect.MultiSelectAnnotHandler;
import com.foxit.uiextensions.config.permissions.PermissionsConfig;
import com.foxit.uiextensions.modules.textselect.BlankSelectToolHandler;
import com.foxit.uiextensions.modules.textselect.TextSelectToolHandler;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.foxit.uiextensions.utils.AppAnnotUtil.ANNOT_SELECT_TOLERANCE;
/**
 * Management for PDF document annotations, pages, permissions, and so on. This implementation is shared with all the modules.
 **/
public class DocumentManager extends AbstractUndo {
    private static final Lock REENTRANT_LOCK = new ReentrantLock();

    Annot mCurAnnot = null;
    private WeakReference<PDFViewCtrl> mPdfViewCtrl;
    private ArrayList<AnnotEventListener> mAnnotEventListenerList = new ArrayList<AnnotEventListener>();
    private ArrayList<IGroupEventListener> mGroupEventListenerList = new ArrayList<IGroupEventListener>();
    private ArrayList<IFlattenEventListener> mFlattenEventListenerList = new ArrayList<IFlattenEventListener>();
    private ArrayList<IRedactionEventListener> mRedactionEventListenerList = new ArrayList<IRedactionEventListener>();
    private ArrayList<IImportAnnotsEventListener> mImportAnnotsListenerList = new ArrayList<>();
    private ActionCallback mActionCallback = null;

    private Boolean isSetActionCallback = Boolean.FALSE;
    private long mUserPermission = 0;
    private int mMDPPermission = 0; // signature permission
    private boolean mIsSign = false;
    private boolean mHasModifyTask = false;
    private boolean mIsOwner = true;

    private boolean mIsDocModified = false;
    private boolean mSingleTap = true;

    public DocumentManager on(PDFViewCtrl ctrl){
        AppUtil.requireNonNull(ctrl);
        if (getPdfViewCtrl() == null){
            mPdfViewCtrl = new WeakReference<>(ctrl);
        }
        return this;
    }

    private PDFViewCtrl getPdfViewCtrl() {
        return mPdfViewCtrl.get();
    }

    public DocumentManager(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = new WeakReference<>(pdfViewCtrl);
    }

    public void setActionCallback(ActionCallback handler) {
        REENTRANT_LOCK.lock();
        if (isSetActionCallback) return;
        mActionCallback = handler;
        isSetActionCallback = Library.setActionCallback(mActionCallback);
        REENTRANT_LOCK.unlock();
    }

    public void reInit() {
        REENTRANT_LOCK.lock();
        isSetActionCallback = false;
        REENTRANT_LOCK.unlock();
    }

    ActionCallback getActionCallback() {
        return mActionCallback;
    }

    public void resetActionCallback() {
        REENTRANT_LOCK.lock();
        isSetActionCallback = Library.setActionCallback(mActionCallback);
        REENTRANT_LOCK.unlock();
    }

    public void setCurrentAnnot(Annot annot) {
        setCurrentAnnot(annot, true);
    }

    public void setCurrentAnnot(Annot annot, boolean reRender) {
        if (mCurAnnot == annot) return;
        Annot lastAnnot = mCurAnnot;
        PDFViewCtrl viewCtrl = getPdfViewCtrl();
        if (viewCtrl == null) return;
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) viewCtrl.getUIExtensionsManager());
        if (annot == null) {
            uiExtensionsManager.startHideToolbarsTimer();
        } else {
            uiExtensionsManager.stopHideToolbarsTimer();
        }

        REENTRANT_LOCK.lock();
        if (mCurAnnot != null && !mCurAnnot.isEmpty()) {
            int type = getAnnotHandlerType(lastAnnot);
            if (uiExtensionsManager.getAnnotHandlerByType(type) != null) {
                uiExtensionsManager.getAnnotHandlerByType(type).onAnnotDeselected(lastAnnot, reRender);
            }
        }
        mCurAnnot = annot;
        if (annot != null && uiExtensionsManager.getAnnotHandlerByType(getAnnotHandlerType(annot)) != null) {
            uiExtensionsManager.getAnnotHandlerByType(getAnnotHandlerType(annot)).onAnnotSelected(annot, reRender);
        } else {
            mSingleTap = true;
        }
        REENTRANT_LOCK.unlock();

        onAnnotChanged(lastAnnot, mCurAnnot);
    }

    public Annot getCurrentAnnot() {
        return mCurAnnot;
    }

    Annot getFocusAnnot() {
        if (mCurAnnot != null && !mCurAnnot.isEmpty()) {
            return mCurAnnot;
        }

        if (mEraseAnnotList.size() > 0) {
            return mEraseAnnotList.get(0);
        }

        return null;
    }

    int getAnnotHandlerType(Annot annot){
        PDFViewCtrl viewCtrl = getPdfViewCtrl();
        if (viewCtrl != null){
            if (mSingleTap
                    && ((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).isLoadAnnotModule(annot)
                    && GroupManager.getInstance().isGrouped(viewCtrl, annot)) {
                return AnnotHandler.TYPE_MULTI_SELECT;
            } else {
                return AppAnnotUtil.getAnnotHandlerType(annot);
            }
        }
        return Annot.e_UnknownType;
    }

    public boolean shouldViewCtrlDraw(Annot annot) {
        try {
            REENTRANT_LOCK.lock();
            if (mCurAnnot != null && !mCurAnnot.isEmpty() && mCurAnnot.getPage().getIndex() == annot.getPage().getIndex()) {
                int type = getAnnotHandlerType(mCurAnnot);
                AnnotHandler annotHandler = ((UIExtensionsManager) getPdfViewCtrl().getUIExtensionsManager()).getAnnotHandlerByType(type);
                if (annotHandler != null) {
                    return annotHandler.shouldViewCtrlDraw(annot);
                }
            }

            //for eraser
            if (annot.getType() != Annot.e_Ink) return true;
            for (int i = 0; i < mEraseAnnotList.size(); i ++) {
                Ink ink =  mEraseAnnotList.get(i);
                if (ink.getPage().getIndex() == annot.getPage().getIndex() &&
                        ink.getIndex() == annot.getIndex()) {
                    return false;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        } finally {
            REENTRANT_LOCK.unlock();
        }
        return true;
    }

    private int getMDPDigitalSignPermissionInDocument(PDFDoc document) throws PDFException {
        PDFDictionary catalog = document.getCatalog();
        if(catalog == null) return 0;
        PDFObject object = catalog.getElement("Perms");
        if (object == null)  return 0;

        PDFDictionary perms = object.getDirectObject().getDict();
        if (perms == null) return 0;

        object = perms.getElement("DocMDP");
        if (object == null) return 0;

        PDFDictionary docMDP = object.getDirectObject().getDict();
        if (docMDP == null) return 0;

        object = docMDP.getElement("Reference");
        if (object == null) return 0;

        PDFArray reference = object.getDirectObject().getArray();
        if (reference == null) return 0;

        for (int i = 0; i < reference.getElementCount(); i++) {
            object = reference.getElement(i);
            if (object == null) return 0;

            PDFDictionary tmpDict = object.getDirectObject().getDict();
            if (tmpDict == null) return 0;

            object = tmpDict.getElement("TransformMethod");
            if (object == null) return 0;

            String transformMethod = object.getDirectObject().getWideString();
            if (!transformMethod.contentEquals("DocMDP")) continue;

            object = tmpDict.getElement("TransformParams");
            if (object == null) return 0;

            PDFDictionary transformParams = object.getDirectObject().getDict();
            if (transformParams == null || transformParams == tmpDict) return 0;

            object = transformParams.getElement("P");
            if (object == null) return 0;

            return object.getDirectObject().getInteger();
        }
        return 0;
    }

    private boolean isDocOpen() {
        if (getPdfViewCtrl() == null || getPdfViewCtrl().getDoc() == null) return false;
        return true;
    }

    public boolean canPrint() {
        if (!isDocOpen()) return false;
        if (mIsOwner) return true;
        if (getPdfViewCtrl().isDynamicXFA()) return true;
        return (mUserPermission & PDFDoc.e_PermPrint) == PDFDoc.e_PermPrint;
    }

    public boolean canPrintHighQuality() {
        if (!isDocOpen()) return false;
        if (mIsOwner) return true;
        return (mUserPermission & PDFDoc.e_PermPrint) == PDFDoc.e_PermPrint ||
                (mUserPermission & PDFDoc.e_PermPrintHigh) == PDFDoc.e_PermPrintHigh;
    }

    public boolean canCopy() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)getPdfViewCtrl().getUIExtensionsManager();
        PermissionsConfig permissionsConfig = uiExtensionsManager.getConfig().permissions;
        if (!permissionsConfig.copyText) return false;

        if (!isDocOpen()) return false;
        if (mIsOwner) return true;

        return (mUserPermission & PDFDoc.e_PermExtract) == PDFDoc.e_PermExtract;
    }

    public boolean canCopyForAssess() {
        if (!isDocOpen()) return false;
        if (mIsOwner) return true;
        return (mUserPermission & PDFDoc.e_PermExtractAccess) == PDFDoc.e_PermExtractAccess
                || (mUserPermission & PDFDoc.e_PermExtract) == PDFDoc.e_PermExtract;
    }

    public boolean canAssemble() {
        if (!isDocOpen()) return false;
        if (!canModifyFile() && !canSaveAsFile())  return false;

        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)getPdfViewCtrl().getUIExtensionsManager();
        if(uiExtensionsManager != null && uiExtensionsManager.getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE) != null) {
            if (mMDPPermission != 0) return false;
            if (mIsSign) return false;
        }
        if (mIsOwner) return true;
        return (mUserPermission & PDFDoc.e_PermAssemble) == PDFDoc.e_PermAssemble
                || (mUserPermission & PDFDoc.e_PermModify) == PDFDoc.e_PermModify;
    }

    public boolean canModifyContents() {
        return canModifyContents(false);
    }

    private boolean canModifyContents(boolean isJudgeAddSignature) {
        if (!isDocOpen()) return false;
        if (!canModifyFile() && !canSaveAsFile()) return false;

        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)getPdfViewCtrl().getUIExtensionsManager();
        if(uiExtensionsManager != null && uiExtensionsManager.getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE)!= null) {
            if (mMDPPermission != 0) return false;
            if (!isJudgeAddSignature && mIsSign) return false;
        }
        if (mIsOwner) return true;
        return (mUserPermission & PDFDoc.e_PermModify) == PDFDoc.e_PermModify;
    }

    public boolean canFillForm() {
        if (!isDocOpen()) return false;
        if (!canModifyFile() && !canSaveAsFile()) return false;

        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)getPdfViewCtrl().getUIExtensionsManager();
        if(uiExtensionsManager != null && uiExtensionsManager.getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE)!= null) {
            if (mMDPPermission == 1) return false;
        }
        if (mIsOwner) return true;
        return (mUserPermission & PDFDoc.e_PermFillForm) == PDFDoc.e_PermFillForm
                || (mUserPermission & PDFDoc.e_PermAnnotForm) == PDFDoc.e_PermAnnotForm
                || (mUserPermission & PDFDoc.e_PermModify) == PDFDoc.e_PermModify;
    }

    public boolean canAddAnnot() {
        if (!isDocOpen()) return false;
        if (getPdfViewCtrl().isDynamicXFA()) return false;
        if (!canModifyFile() && !canSaveAsFile())  return false;

        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)getPdfViewCtrl().getUIExtensionsManager();
        if(uiExtensionsManager != null && uiExtensionsManager.getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE)!= null) {
            if (mMDPPermission == 1 || mMDPPermission == 2) return false;
        }
        if (mIsOwner) return true;
        return (mUserPermission & PDFDoc.e_PermAnnotForm) == PDFDoc.e_PermAnnotForm;
    }

    public boolean canSigning() {
            if (mMDPPermission > 0) return false;
//        if (isSign()) return false;
        if (canAddAnnot() || canFillForm()) return true;
        return false;
    }

    public boolean canAddSignature() {
        if (!isDocOpen()) return false;
        if (getPdfViewCtrl().isDynamicXFA()) return false;
        if (isSign() && !canAddAnnot()) return false;
        if (canSigning() && canModifyContents(true)) return true;
        return false;
    }

    public boolean canModifyForm() {
        return canModifyContents(true) && canAddAnnot();
    }

    public boolean canModifyXFAForm() {
        if (!getPdfViewCtrl().isDynamicXFA()) return false;
        if (isViewSignedDoc) return false;
        return true;
    }

    public boolean canModifyFile() {
        if (!isDocOpen()) return false;
        return true;
    }

    public boolean canSaveAsFile() {
        if (!isDocOpen()) return false;
        return true;
    }

    public boolean isSign() {
        if (!isDocOpen()) return false;
        return mIsSign;
    }

    public boolean isXFA() {
        if (!isDocOpen()) return false;
        try {
            return getPdfViewCtrl().getDoc().isXFA();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    // annot event listener management.
    public void registerAnnotEventListener(AnnotEventListener listener) {
        mAnnotEventListenerList.add(listener);
    }

    public void unregisterAnnotEventListener(AnnotEventListener listener) {
        mAnnotEventListenerList.remove(listener);
    }

    public void onAnnotAdded(PDFPage page, Annot annot) {
        for (AnnotEventListener listener : mAnnotEventListenerList) {
            listener.onAnnotAdded(page, annot);
        }
    }

    public void onAnnotWillDelete(PDFPage page, Annot annot) {
        for (AnnotEventListener listener : mAnnotEventListenerList) {
            listener.onAnnotWillDelete(page, annot);
        }
    }

    public void onAnnotDeleted(PDFPage page, Annot annot) {
        for (AnnotEventListener listener : mAnnotEventListenerList) {
            listener.onAnnotDeleted(page, annot);
        }
    }

    public void onAnnotModified(PDFPage page, Annot annot) {
        for (AnnotEventListener listener : mAnnotEventListenerList) {
            listener.onAnnotModified(page, annot);
        }
    }

    public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {
        for (AnnotEventListener listener : mAnnotEventListenerList) {
            listener.onAnnotChanged(lastAnnot, currentAnnot);
        }
    }

    public void registerFlattenEventListener(IFlattenEventListener listener) {
        mFlattenEventListenerList.add(listener);
    }

    public void unregisterFlattenEventListener(IFlattenEventListener listener) {
        mFlattenEventListenerList.remove(listener);
    }

    public void onAnnotWillFlatten(PDFPage page, Annot annot) {
        for (IFlattenEventListener listener : mFlattenEventListenerList) {
            listener.onAnnotWillFlatten(page, annot);
        }
    }

    public void onAnnotFlattened(PDFPage page, Annot annot) {
        for (IFlattenEventListener listener : mFlattenEventListenerList) {
            listener.onAnnotFlattened(page, annot);
        }
    }

    public void registerGroupEventListener(IGroupEventListener listener) {
        mGroupEventListenerList.add(listener);
    }

    public void unregisterGroupEventListener(IGroupEventListener listener) {
        mGroupEventListenerList.remove(listener);
    }

    public void onAnnotGrouped(PDFPage page, List<Annot> groupAnnots) {
        for (IGroupEventListener listener : mGroupEventListenerList) {
            listener.onAnnotGrouped(page, groupAnnots);
        }
    }

    public void onAnnotUnGrouped(PDFPage page, List<Annot> unGroupAnnots) {
        for (IGroupEventListener listener : mGroupEventListenerList) {
            listener.onAnnotUnGrouped(page, unGroupAnnots);
        }
    }

    public void registerRedactionEventListener(IRedactionEventListener listener) {
        mRedactionEventListenerList.add(listener);
    }

    public void unregisterRedactionEventListener(IRedactionEventListener listener) {
        mRedactionEventListenerList.remove(listener);
    }

    public void onAnnotWillApply(PDFPage page, Annot annot) {
        for (IRedactionEventListener listener : mRedactionEventListenerList) {
            listener.onAnnotWillApply(page, annot);
        }
    }

    public void onAnnotApplied(PDFPage page, Annot annot) {
        for (IRedactionEventListener listener : mRedactionEventListenerList) {
            listener.onAnnotApplied(page, annot);
        }
    }

    public void registerImportedAnnotsEventListener(IImportAnnotsEventListener listener) {
        mImportAnnotsListenerList.add(listener);
    }

    public void unregisterImportedAnnotsEventListener(IImportAnnotsEventListener listener) {
        mImportAnnotsListenerList.remove(listener);
    }

    public void onAnnosImported() {
        for (IImportAnnotsEventListener listener : mImportAnnotsListenerList) {
            listener.onAnnotsImported();
        }
    }

    private List<Ink> mEraseAnnotList = new ArrayList<Ink>();
    public void onAnnotStartEraser(Ink annot) {
        mEraseAnnotList.add(annot);
    }

    public void onAnnotEndEraser(Ink annot) {
        mEraseAnnotList.remove(annot);
    }

    public static  boolean intersects(RectF a, RectF b) {
        return a.left < b.right && b.left < a.right
                && a.top > b.bottom && b.top > a.bottom;
    }

    public ArrayList<Annot> getAnnotsInteractRect(PDFPage page, RectF rect, int type) {
        ArrayList<Annot> annotList = new ArrayList<Annot>(4);
        try {
            int count = page.getAnnotCount();
            Annot annot = null;
            for (int i = 0; i < count; i++) {
                annot = AppAnnotUtil.createAnnot(page.getAnnot(i));
                if (annot == null || (annot.getFlags() & Annot.e_FlagHidden) != 0)
                    continue;

                PDFViewCtrl viewCtrl = getPdfViewCtrl();
                if (viewCtrl != null){
                    int _type = getAnnotHandlerType(annot);
                    AnnotHandler annotHandler = ((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(_type);
                    if (annotHandler != null) {
                        RectF bbox = annotHandler.getAnnotBBox(annot);
                        if (intersects(bbox, rect) && annot.getType() == type) {
                            annotList.add(annot);
                        }
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return annotList;
    }

    public Annot getAnnot(PDFPage page, String nm) {
        if (page == null || page.isEmpty()) return null;

        try {
            int count = page.getAnnotCount();
            Annot annot = null;
            for (int i = 0; i < count; i++) {
                annot = AppAnnotUtil.createAnnot(page.getAnnot(i));
                if (annot == null || annot.isEmpty()) continue;

                if (AppUtil.isEmpty(annot.getUniqueID())) {
                    if (annot.getDict() != null && String.valueOf(annot.getDict().getObjNum()).equals(nm))
                        return annot;
                } else {
                    if (annot.getUniqueID().equals(nm)) return annot;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void addAnnot(PDFPage page, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (page == null || page.isEmpty()) return;

        Annot annot = getAnnot(page, content.getNM());
        if (annot != null && !annot.isEmpty()) {
            modifyAnnot(annot, content, addUndo, result);
            return;
        }
        PDFViewCtrl viewCtrl = getPdfViewCtrl();
        if (viewCtrl != null){
            try {

                UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) viewCtrl.getUIExtensionsManager();
                AnnotHandler annotHandler = uiExtensionsManager.getAnnotHandlerByType(content.getType());
                if (annotHandler != null) {
                    annotHandler.addAnnot(page.getIndex(), content, addUndo, result);
                } else {
                    if (result != null) {
                        result.result(null, false);
                    }
                }
            }catch (PDFException e){
                e.printStackTrace();
            }
        }
    }

    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        try {
            if (annot.getModifiedDateTime() != null && content.getModifiedDate() != null
                    && annot.getModifiedDateTime().equals(content.getModifiedDate())) {
                if (result != null) {
                    result.result(null, true);
                }
                return;
            }

            PDFViewCtrl viewCtrl = getPdfViewCtrl();
            if (viewCtrl != null){
                UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) viewCtrl.getUIExtensionsManager();
                AnnotHandler annotHandler = uiExtensionsManager.getAnnotHandlerByType(getAnnotHandlerType(annot));
                if (annotHandler != null) {
                    annotHandler.modifyAnnot(annot, content, addUndo, result);
                } else {
                    if (result != null) {
                        result.result(null, false);
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void removeAnnot(final Annot annot,boolean isRemoveGroup, boolean addUndo, final Event.Callback result) {
        if (annot == getCurrentAnnot()) {
            setCurrentAnnot(null);
        }

        PDFViewCtrl viewCtrl = getPdfViewCtrl();
        if (viewCtrl != null){
            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) viewCtrl.getUIExtensionsManager();
            AnnotHandler annotHandler;
            if(isRemoveGroup) {
                annotHandler = uiExtensionsManager.getAnnotHandlerByType(getAnnotHandlerType(annot));
            } else {
                annotHandler = uiExtensionsManager.getAnnotHandlerByType(AppAnnotUtil.getAnnotHandlerType(annot));
            }

            if (annotHandler != null) {
                annotHandler.removeAnnot(annot, addUndo, result);
            }
        }
    }

    public void removeAnnot(final Annot annot, boolean addUndo, final Event.Callback result) {
        removeAnnot(annot, true, addUndo, result);
    }

    public void flattenAnnot(final Annot annot, final Event.Callback result) {
        if (annot == getCurrentAnnot()) {
            setCurrentAnnot(null);
        }

        PDFViewCtrl viewCtrl = getPdfViewCtrl();
        if (viewCtrl != null) {
            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) viewCtrl.getUIExtensionsManager();
            AnnotHandler annotHandler = uiExtensionsManager.getAnnotHandlerByType(getAnnotHandlerType(annot));
            if (annotHandler instanceof MultiSelectAnnotHandler) {
                ((MultiSelectAnnotHandler) annotHandler).flattenAnnot(annot, result);
            } else {
                UIAnnotFlatten.flattenAnnot(viewCtrl, annot, result);
            }
        }
    }

    private boolean canTouch(@NonNull Annot annot) throws PDFException {
        int annotType = annot.getType();
        boolean isLocked = (annotType != Annot.e_Widget) && AppAnnotUtil.isLocked(annot);
        boolean isReadOnly = (annotType == Annot.e_Widget) ? FormFillerUtil.isReadOnly(annot) : AppAnnotUtil.isReadOnly(annot);
        boolean canAdd = (annotType == Annot.e_Widget) ? canFillForm() : canAddAnnot();
        if (isLocked || isReadOnly || !canAdd) return false;
        return true;
    }

    //deal with annot
    protected boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        Annot annot = null;
        AnnotHandler annotHandler = null;
        PDFPage page = null;
        int action = motionEvent.getActionMasked();
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    PDFViewCtrl viewCtrl = getPdfViewCtrl();
                    if (viewCtrl != null){
                        annot = getCurrentAnnot();
                        if (annot != null && !annot.isEmpty()) {
                            if (!canTouch(annot)) return false;
                            int type = getAnnotHandlerType(annot);
                            annotHandler = ((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                            if (annotHandler == null) return false;
                            if (annotHandler.onTouchEvent(pageIndex, motionEvent, annot)) {
                                hideSelectorAnnotMenu(viewCtrl);
                                return true;
                            }
                        }
                        PointF pdfPoint = AppAnnotUtil.getPageViewPoint(viewCtrl, pageIndex, motionEvent);

                        page = getPage(pageIndex, false);
                        if (page != null && !page.isEmpty()) {
                            annot = AppAnnotUtil.createAnnot(page.getAnnotAtDevicePoint(AppUtil.toFxPointF(pdfPoint), ANNOT_SELECT_TOLERANCE,
                                    AppUtil.toMatrix2D(viewCtrl.getDisplayMatrix(pageIndex))));
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    annot = getCurrentAnnot();
                    break;
                default:
                    return false;
            }
            PDFViewCtrl viewCtrl = getPdfViewCtrl();
            if (annot != null && !annot.isEmpty() && viewCtrl != null) {
                if (!canTouch(annot)) return false;
                int type = getAnnotHandlerType(annot);
                annotHandler = ((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                if (annotHandler != null && annotHandler.annotCanAnswer(annot)) {
                    hideSelectorAnnotMenu(viewCtrl);
                    return annotHandler.onTouchEvent(pageIndex, motionEvent, annot);
                }
            }
        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    protected boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        Annot annot = null;
        AnnotHandler annotHandler = null;
        PDFPage page = null;
        try {
            PDFViewCtrl viewCtrl = getPdfViewCtrl();
            if (viewCtrl != null){
                boolean annotCanceled = false;
                annot = getCurrentAnnot();
                if (annot != null && !annot.isEmpty()) {
                    int type =getAnnotHandlerType(annot);
                    annotHandler = ((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                    if (annotHandler != null && annotHandler.onLongPress(pageIndex, motionEvent, annot)) {
                        hideSelectorAnnotMenu(viewCtrl);
                        return true;
                    }
                    if (getCurrentAnnot() == null) {
                        annotCanceled = true;
                    }
                }
                PointF pdfPoint = AppAnnotUtil.getPageViewPoint(viewCtrl, pageIndex, motionEvent);
                page = viewCtrl.getDoc().getPage(pageIndex);
                if (page != null && !page.isEmpty()) {
                    annot = AppAnnotUtil.createAnnot(page.getAnnotAtDevicePoint(AppUtil.toFxPointF(pdfPoint), ANNOT_SELECT_TOLERANCE,
                            AppUtil.toMatrix2D(viewCtrl.getDisplayMatrix(pageIndex))));
                }
                if (annot != null && !annot.isEmpty() && AppAnnotUtil.isSupportGroup(annot)) {
                    annot = AppAnnotUtil.createAnnot(((Markup)annot).getGroupHeader());
                }

                if (annot != null && !annot.isEmpty()) {
                    mSingleTap = false;
                    int type = getAnnotHandlerType(annot);
                    annotHandler = ((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                    if (annotHandler != null && annotHandler.annotCanAnswer(annot)) {
                        if (annotHandler.onLongPress(pageIndex, motionEvent, annot)) {
                            hideSelectorAnnotMenu(viewCtrl);
                            return true;
                        }
                    }
                }

                if (annotCanceled) return true;
            }
            return false;
        } catch (PDFException e1) {
            e1.printStackTrace();
        }

        return false;
    }

    protected boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        Annot annot = null;
        AnnotHandler annotHandler = null;
        PDFPage page = null;
        try {
            PDFViewCtrl viewCtrl = getPdfViewCtrl();
            if (viewCtrl != null){
                boolean annotCanceled = false;
                annot = getCurrentAnnot();
                if (annot != null && !annot.isEmpty()) {
                    int type = getAnnotHandlerType(annot);
                    annotHandler = ((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                    if (annotHandler != null && annotHandler.onSingleTapConfirmed(pageIndex, motionEvent, annot)) {
                        hideSelectorAnnotMenu(viewCtrl);
                        return true;
                    }
                    if (getCurrentAnnot() == null) {
                        annotCanceled = true;
                    }
                }
                PointF pdfPoint = AppAnnotUtil.getPageViewPoint(viewCtrl, pageIndex, motionEvent);
                page = viewCtrl.getDoc().getPage(pageIndex);
                if (page != null && !page.isEmpty()) {
                    annot = AppAnnotUtil.createAnnot(page.getAnnotAtDevicePoint(AppUtil.toFxPointF(pdfPoint), ANNOT_SELECT_TOLERANCE,
                            AppUtil.toMatrix2D(viewCtrl.getDisplayMatrix(pageIndex))));
                }
                if (annot != null && !annot.isEmpty() && AppAnnotUtil.isSupportGroup(annot)) {
                    annot = AppAnnotUtil.createAnnot(((Markup)annot).getGroupHeader());
                }

                if (annot != null && !annot.isEmpty()) {
                    mSingleTap = true;
                    int type = getAnnotHandlerType(annot);
                    annotHandler = ((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(type);
                    if (annotHandler != null && annotHandler.annotCanAnswer(annot)) {
                        if (annotHandler.onSingleTapConfirmed(pageIndex, motionEvent, annot)) {
                            hideSelectorAnnotMenu(viewCtrl);
                            return true;
                        }
                    }
                }

                if (annotCanceled) {
                    return true;
                }
            }
            return false;
        } catch (PDFException e1) {
            e1.printStackTrace();
        }

        return false;
    }

    private void hideSelectorAnnotMenu(PDFViewCtrl pdfViewCtrl) {
        BlankSelectToolHandler blankSelectToolHandler = (BlankSelectToolHandler) ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
        if (blankSelectToolHandler != null) {
            blankSelectToolHandler.dismissMenu();
        }

        TextSelectToolHandler textSelectionTool = (TextSelectToolHandler) ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
        if (textSelectionTool != null) {
            textSelectionTool.mAnnotationMenu.dismiss();
        }
    }

    //for Undo&Redo

    @Override
    public void undo() {
        PDFViewCtrl viewCtrl = getPdfViewCtrl();
        if (viewCtrl != null){
            if (((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                ((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
            }

            if (getCurrentAnnot() != null) {
                setCurrentAnnot(null);
            }

            if (haveModifyTasks()) {
                Runnable delayRunnable = new Runnable() {
                    @Override
                    public void run() {
                        undo();
                    }
                };
                viewCtrl.post(delayRunnable);
                return;
            }
        }
        super.undo();
    }

    @Override
    public void redo() {
        PDFViewCtrl viewCtrl = getPdfViewCtrl();
        if (viewCtrl != null){
            if (((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                ((UIExtensionsManager) viewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
            }

            if (getCurrentAnnot() != null) {
                setCurrentAnnot(null);
            }

            if (haveModifyTasks()) {
                Runnable delayRunnable = new Runnable() {
                    @Override
                    public void run() {
                        redo();
                    }
                };
                viewCtrl.post(delayRunnable);
                return;
            }

        }
        super.redo();
    }

    @Override
    protected String getDiskCacheFolder() {
        PDFViewCtrl viewCtrl = getPdfViewCtrl();
        if (viewCtrl == null){
            return "";
        }
        return viewCtrl.getContext().getCacheDir().getParent();
    }

    @Override
    protected boolean haveModifyTasks() {
        return mHasModifyTask;
    }

    public void setHasModifyTask(boolean hasModifyTask) {
        this.mHasModifyTask = hasModifyTask;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mCurAnnot == null || mCurAnnot.isEmpty()) return false;
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setCurrentAnnot(null);
            return true;
        }
        return false;
    }

    public boolean onKeyBack() {
        if (mCurAnnot == null || mCurAnnot.isEmpty()) return false;
        setCurrentAnnot(null);
        return true;
    }

    private boolean isOwner(PDFDoc doc) {
        try {
            if (!doc.isEncrypted()) return true;
            if (PDFDoc.e_PwdOwner == doc.getPasswordType()) {
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }


    private boolean isSign(PDFDoc doc) {
        try {
            int count = doc.getSignatureCount();
            for (int i = 0; i < count; i++) {
                Signature signature = doc.getSignature(i);
                if (signature != null && !signature.isEmpty() && signature.isSigned()) {
                    return true;
                }
            }
        } catch (PDFException e) {

        }
        return false;
    }

    void initDocProperties(final PDFDoc doc) {
        if (doc == null || doc.isEmpty()) return;

        try {
            PDFViewCtrl viewCtrl = getPdfViewCtrl();
            if (viewCtrl != null){
                mIsOwner = viewCtrl.isOwner();
                mUserPermission = viewCtrl.getUserPermission();
                if (isViewSignedDoc) {
                    mUserPermission &= ~PDFDoc.e_PermModify;
                    mUserPermission &= ~PDFDoc.e_PermAnnotForm;
                    mUserPermission &= ~PDFDoc.e_PermFillForm;
                    mUserPermission &= ~PDFDoc.e_PermAssemble;

                    mIsOwner = false;
//                    isViewSignedDoc = false;
                }
                UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)viewCtrl.getUIExtensionsManager();
                if(uiExtensionsManager != null && uiExtensionsManager.getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE)!= null) {
                    mMDPPermission = getMDPDigitalSignPermissionInDocument(doc);
                }

                mIsSign = isSign(doc);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void destroy() {
        mAnnotEventListenerList.clear();
        mAnnotEventListenerList = null;
        mActionCallback = null;
        isSetActionCallback = false;
        mPdfViewCtrl.clear();
    }

    public boolean isDocModified() {
        return mIsDocModified;
    }

    public void setDocModified(boolean isModified) {
        mIsDocModified = isModified;
    }

    //For Multiple select annotations

    private boolean mIsMultipleSelectAnnots = false;

    public boolean isMultipleSelectAnnots() {
        return mIsMultipleSelectAnnots;
    }

    public void setMultipleSelectAnnots(boolean isMultipleSelectAnnots) {
        mIsMultipleSelectAnnots = isMultipleSelectAnnots;
    }

    //update undo/redo items stack

    public void onPageRemoved(boolean success, int index) {
        if(!success) return;
        removeInvalidItems(mRedoItemStack, index);
        removeInvalidItems(mUndoItemStack, index);
    }

    public void onPagesInsert(boolean success, int dstIndex, int[] range) {
        if(!success) return;
        int offsetIndex = 0;
        for (int i = 0; i < range.length / 2; i++) {
            offsetIndex += range[2*i+1];
        }
        updateItemsWithOffset(mRedoItemStack, dstIndex, offsetIndex);
        updateItemsWithOffset(mUndoItemStack, dstIndex, offsetIndex);
    }

    public void onPageMoved(boolean success, int index, int dstIndex) {
        if(!success) return;
        updateItems(mRedoItemStack, index, dstIndex);
        updateItems(mUndoItemStack, index, dstIndex);
    }

    public void removeFlattenUndoItems(int index, String uniqueID) {
        removeFlattenItems(mRedoItemStack, index, uniqueID);
        removeFlattenItems(mUndoItemStack, index, uniqueID);
    }

    public void removeReplyUndoItems(String uniqueID) {
        removeReplyItems(mRedoItemStack, uniqueID);
        removeReplyItems(mUndoItemStack, uniqueID);
    }

    public PDFPage getPage(int pageIndex, boolean reParse) {
        try {
            PDFPage page = getPdfViewCtrl().getDoc().getPage(pageIndex);
            if (page.isEmpty()) return null;
            if (!page.isParsed() || reParse) {
                Progressive progressive = page.startParse(PDFPage.e_ParsePageNormal, null, reParse);
                int state = Progressive.e_ToBeContinued;
                while (state == Progressive.e_ToBeContinued) {
                    state = progressive.resume();
                }

                if (state != Progressive.e_Finished) {
                    return null;
                }
            }
            return page;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isViewSignedDoc = false;

    public void setViewSignedDocFlag(boolean viewSignedDoc) {
        isViewSignedDoc = viewSignedDoc;
    }

    /**
     * Just simply judge a file whether is PDFA or not.
     * @param pdfDoc the specified pdf file.
     * @return true: is pdfa file.
     */
    public boolean simpleCheckPDFA(@NonNull PDFDoc pdfDoc) {
        if (pdfDoc.isEmpty()) {
            return false;
        }
        try {
            PDFDictionary rootDict = pdfDoc.getCatalog();
            if (rootDict == null) {
                return false;
            }
            PDFObject metaDataObj = rootDict.getElement("Metadata");
            if (metaDataObj == null) {
                return false;
            }

            PDFStream metaDataStream = metaDataObj.getStream();
            if (metaDataStream == null) {
                return false;
            }
            int streamSize = metaDataStream.getDataSize(false);
            byte[] data = new byte[streamSize];
            metaDataStream.getData(false, streamSize, data);
            String strStream = new String(data).trim();
            int index = strStream.indexOf("pdfaid:conformance");
            return index != -1;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}
