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
package com.foxit.uiextensions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.addon.ConnectedPDF.ClientInfo;
import com.foxit.sdk.addon.xfa.XFADoc;
import com.foxit.sdk.addon.xfa.XFAWidget;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.objects.PDFDictionary;
import com.foxit.sdk.pdf.objects.PDFObject;
import com.foxit.uiextensions.annots.AnnotActionHandler;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.caret.CaretModule;
import com.foxit.uiextensions.annots.circle.CircleModule;
import com.foxit.uiextensions.annots.fileattachment.FileAttachmentModule;
import com.foxit.uiextensions.annots.form.FormFillerModule;
import com.foxit.uiextensions.annots.form.FormFillerToolHandler;
import com.foxit.uiextensions.annots.form.FormNavigationModule;
import com.foxit.uiextensions.annots.freetext.callout.CalloutModule;
import com.foxit.uiextensions.annots.freetext.textbox.TextBoxModule;
import com.foxit.uiextensions.annots.freetext.typewriter.TypewriterModule;
import com.foxit.uiextensions.annots.ink.EraserModule;
import com.foxit.uiextensions.annots.ink.InkModule;
import com.foxit.uiextensions.annots.line.LineModule;
import com.foxit.uiextensions.annots.link.LinkModule;
import com.foxit.uiextensions.annots.multiselect.MultiSelectModule;
import com.foxit.uiextensions.annots.note.NoteModule;
import com.foxit.uiextensions.annots.polygon.PolygonModule;
import com.foxit.uiextensions.annots.polyline.PolyLineModule;
import com.foxit.uiextensions.annots.redaction.RedactModule;
import com.foxit.uiextensions.annots.redaction.RedactToolHandler;
import com.foxit.uiextensions.annots.screen.image.PDFImageModule;
import com.foxit.uiextensions.annots.screen.multimedia.MultimediaModule;
import com.foxit.uiextensions.annots.square.SquareModule;
import com.foxit.uiextensions.annots.stamp.StampModule;
import com.foxit.uiextensions.annots.textmarkup.highlight.HighlightModule;
import com.foxit.uiextensions.annots.textmarkup.squiggly.SquigglyModule;
import com.foxit.uiextensions.annots.textmarkup.strikeout.StrikeoutModule;
import com.foxit.uiextensions.annots.textmarkup.underline.UnderlineModule;
import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.config.modules.annotations.AnnotationsConfig;
import com.foxit.uiextensions.config.permissions.PermissionsManager;
import com.foxit.uiextensions.config.uisettings.UISettingsManager;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFolderSelectDialog;
import com.foxit.uiextensions.controls.menu.IMenuView;
import com.foxit.uiextensions.controls.menu.MoreMenuModule;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.controls.panel.PanelSpec.PanelType;
import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;
import com.foxit.uiextensions.controls.toolbar.IBarsHandler;
import com.foxit.uiextensions.controls.toolbar.impl.BaseBarManager;
import com.foxit.uiextensions.home.local.LocalModule;
import com.foxit.uiextensions.modules.BrightnessModule;
import com.foxit.uiextensions.modules.DocInfoModule;
import com.foxit.uiextensions.modules.OutlineModule;
import com.foxit.uiextensions.modules.PageNavigationModule;
import com.foxit.uiextensions.modules.ReadingBookmarkModule;
import com.foxit.uiextensions.modules.ReflowModule;
import com.foxit.uiextensions.modules.ScreenLockModule;
import com.foxit.uiextensions.modules.SearchModule;
import com.foxit.uiextensions.modules.UndoModule;
import com.foxit.uiextensions.modules.compare.CompareHandler;
import com.foxit.uiextensions.modules.compare.ComparisonModule;
import com.foxit.uiextensions.modules.connectpdf.account.AccountModule;
import com.foxit.uiextensions.modules.crop.CropModule;
import com.foxit.uiextensions.modules.dynamicxfa.DynamicXFAModule;
import com.foxit.uiextensions.modules.dynamicxfa.DynamicXFAWidgetHandler;
import com.foxit.uiextensions.modules.dynamicxfa.IXFAPageEventListener;
import com.foxit.uiextensions.modules.dynamicxfa.IXFAWidgetEventListener;
import com.foxit.uiextensions.modules.panel.IPanelManager;
import com.foxit.uiextensions.modules.panel.PanelManager;
import com.foxit.uiextensions.modules.panel.annot.AnnotPanelModule;
import com.foxit.uiextensions.modules.panel.filespec.FileSpecPanelModule;
import com.foxit.uiextensions.modules.panel.signature.SignaturePanelModule;
import com.foxit.uiextensions.modules.panzoom.PanZoomModule;
import com.foxit.uiextensions.modules.print.IPrintResultCallback;
import com.foxit.uiextensions.modules.print.PDFPrint;
import com.foxit.uiextensions.modules.print.PDFPrintAdapter;
import com.foxit.uiextensions.modules.print.PrintModule;
import com.foxit.uiextensions.modules.print.XFAPrintAdapter;
import com.foxit.uiextensions.modules.signature.SignatureModule;
import com.foxit.uiextensions.modules.signature.SignatureToolHandler;
import com.foxit.uiextensions.modules.textselect.BlankSelectToolHandler;
import com.foxit.uiextensions.modules.textselect.TextSelectModule;
import com.foxit.uiextensions.modules.textselect.TextSelectToolHandler;
import com.foxit.uiextensions.modules.thumbnail.ThumbnailModule;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IMainFrame;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureModule;
import com.foxit.uiextensions.security.standard.PasswordModule;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDevice;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppSystemUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.IResult;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.foxit.sdk.common.Constants.e_ErrCanNotGetUserToken;
import static com.foxit.sdk.common.Constants.e_ErrPassword;
import static com.foxit.sdk.common.Constants.e_ErrSuccess;

/**
 * Class <CODE>UIExtensionsManager</CODE> represents a UI extensions manager.
 * <p>
 * The <CODE>UIExtensionsManager</CODE> class is mainly used for managing the UI extensions which implement {@link IPDFReader} interface, it implements the {@link PDFViewCtrl.UIExtensionsManager}
 * interface that is a listener to listen common interaction events and view event, and will dispatch some events to UI extensions, it also defines functions to manage the UI extensions.
 */
public class UIExtensionsManager implements PDFViewCtrl.UIExtensionsManager, IPDFReader {


    /**
     * Interface used to allow the user to run some code
     * when the current {@link ToolHandler} has changed.
     */
    public interface ToolHandlerChangedListener {
        /**
         * Called when current {@link ToolHandler} has changed.
         *
         * @param oldToolHandler The old tool handler.
         * @param newToolHandler The new tool handler.
         */
        void onToolHandlerChanged(ToolHandler oldToolHandler, ToolHandler newToolHandler);
    }

    /**
     * Interface used to allow the user to run some code
     * when the device configuration changes
     */
    public interface ConfigurationChangedListener {
        /**
         * Called when {@link UIExtensionsManager#onConfigurationChanged(Activity, Configuration)} is called.
         *
         * @param newConfig The new device configuration.
         */
        void onConfigurationChanged(Configuration newConfig);
    }

    /**
     * Interface used to allow the user to run some code
     * when do some operations on menu
     */
    public interface MenuEventListener {
        /**
         * Called when {@link #triggerDismissMenuEvent()} is called.
         */
        void onTriggerDismissMenu();
    }

    private             ILinkEventListener mLinkEventListener = null;
    /**
     * link type: annotation
     */
    public static final int                LINKTYPE_ANNOT     = 0;
    /**
     * link type: text
     */
    public static final int                LINKTYPE_TEXT      = 1;

    /**
     * Class definition for link information
     */
    public static class LinkInfo {

        /**
         * should be one of {@link #LINKTYPE_ANNOT}, {@link #LINKTYPE_TEXT}
         */
        public int    linkType;
        /**
         * Should be link annotation or text link.
         */
        public Object link;
    }

    /**
     * Interface used to allow the user to run some code when do some operations on link
     */
    public interface ILinkEventListener {
        /**
         * Called when tap a link.
         *
         * @param linkInfo The link information of the tapped link.
         * @return Return <code>true</code> to prevent this event from being propagated
         * further, or <code>false</code> to indicate that you have not handled
         * this event and it should continue to be propagated by Foxit.
         */
        boolean onLinkTapped(LinkInfo linkInfo);
    }

    /**
     * Set link event listener.
     *
     * @param listener The specified link event listener.
     */
    public void setLinkEventListener(ILinkEventListener listener) {
        mLinkEventListener = listener;
    }

    /**
     * Interface used to allow the user to run some code
     * when close document and exit the current activity
     */
    public interface OnFinishListener {
        /**
         * Usually called when close document and exit the current activity.
         */
        void onFinish();
    }

    /**
     * Set the {@link OnFinishListener} to be invoked when the document closed and current activity has exited.
     *
     * @param listener the {@link OnFinishListener} to use.
     */
    public void setOnFinishListener(OnFinishListener listener) {
        onFinishListener = listener;
    }

    /**
     * Get link event listener object.
     *
     * @return The link event listener object.
     */
    public ILinkEventListener getLinkEventListener() {
        return mLinkEventListener;
    }

    private ToolHandler                             mCurToolHandler = null;
    private PDFViewCtrl                             mPdfViewCtrl    = null;
    private List<Module>                            mModules        = new ArrayList<Module>();
    private HashMap<String, ToolHandler>            mToolHandlerList;
    private Map<PanelSpec.PanelType, Boolean>       mMapPanelHiddenState;
    private SparseArray<AnnotHandler>               mAnnotHandlerList;
    private ArrayList<ToolHandlerChangedListener>   mHandlerChangedListeners;
    private ArrayList<ILayoutChangeListener>        mLayoutChangedListeners;
    // IXFAPageEventListener
    private ArrayList<IXFAPageEventListener>        mXFAPageEventListeners;
    private ArrayList<IXFAWidgetEventListener>      mXFAWidgetEventListener;
    private ArrayList<ConfigurationChangedListener> mConfigurationChangedListeners;
    private ArrayList<MenuEventListener>            mMenuEventListeners;

    private Activity      mAttachActivity       = null;
    private Context       mContext;
    private ViewGroup     mParent;
    private Config        mConfig;
    private boolean       continueAddAnnot      = false;
    private boolean       mEnableLinkAnnot      = true;
    private boolean       mEnableLinkHighlight  = true;
    private boolean       mEnableFormHighlight  = true;
    private long          mFormHighlightColor   = 0x200066cc;
    private long          mLinkHighlightColor   = 0x16007FFF;
    private int           mSelectHighlightColor = 0xFFACDAED;
    private IPanelManager mPanelManager         = null;

    private DocumentManager documentManager;

    private String  currentFileCachePath  = null;
    private String  mSavePath             = null; // default save path
    private String  mDocPath;
    private String  mProgressMsg          = null;
    private int     mState                = ReadStateConfig.STATE_NORMAL;
    private int     mSaveFlag             = PDFDoc.e_SaveFlagIncremental;
    private boolean bDocClosed            = false;
    private boolean mPasswordError        = false;
    private boolean isSaveDocInCurPath    = false;
    private boolean isCloseDocAfterSaving = false;

    private ArrayList<ILifecycleEventListener> mLifecycleEventList;
    private ArrayList<IStateChangeListener>    mStateChangeEventList;

    private MainFrame            mMainFrame;
    private IBarsHandler         mBaseBarMgr;
    private ProgressDialog       mProgressDlg;
    private OnFinishListener     onFinishListener;
    private BackEventListener    mBackEventListener = null;
    private AlertDialog          mSaveAlertDlg;
    private UIFolderSelectDialog mFolderSelectDialog;

    private              RelativeLayout mActivityLayout;
    private              String         mUserSavePath      = null;
    private              boolean        mIsDocOpened       = false;
    private static final int            TOOL_BAR_SHOW_TIME = 5000;

    // for compare docuemnt
    private boolean mOldFileIsTwoColumnLeft = false;
    private int     mOldPageLayout;
    private boolean mIsCompareDoc           = false;

    /**
     * Instantiates a new UI extensions manager.
     *
     * @param context     A <CODE>Context</CODE> object which species the context.
     * @param pdfViewCtrl A <CODE>PDFViewCtrl</CODE> object which species the PDF view control.
     */
    public UIExtensionsManager(Context context, PDFViewCtrl pdfViewCtrl) {
        init(context, pdfViewCtrl, null);
    }

    /**
     * Instantiates a new UI extensions manager with modules config.
     *
     * @param context     A <CODE>Context</CODE> object which species the context.
     * @param pdfViewCtrl A <CODE>PDFViewCtrl</CODE> object which species the PDF view control.
     * @param config      A <CODE>Config</CODE> object which species a modules loading config,
     *                    if null, UIExtension manager will load all modules by default, and equal to {@link #UIExtensionsManager(Context, PDFViewCtrl)}.
     */
    public UIExtensionsManager(Context context, PDFViewCtrl pdfViewCtrl, Config config) {
        init(context, pdfViewCtrl, config);
    }

    private void init(Context context, PDFViewCtrl pdfViewCtrl, Config config) {
        AppUtil.requireNonNull(pdfViewCtrl, "PDF view control can't be null");
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        documentManager = new DocumentManager(pdfViewCtrl);

        mToolHandlerList = new HashMap<String, ToolHandler>(8);
        mMapPanelHiddenState = new HashMap<PanelSpec.PanelType, Boolean>();
        mAnnotHandlerList = new SparseArray<AnnotHandler>(8);
        mHandlerChangedListeners = new ArrayList<ToolHandlerChangedListener>();
        mLayoutChangedListeners = new ArrayList<ILayoutChangeListener>();
        mXFAPageEventListeners = new ArrayList<IXFAPageEventListener>();
        mXFAWidgetEventListener = new ArrayList<IXFAWidgetEventListener>();
        mConfigurationChangedListeners = new ArrayList<ConfigurationChangedListener>();
        mLifecycleEventList = new ArrayList<ILifecycleEventListener>();
        mStateChangeEventList = new ArrayList<IStateChangeListener>();
        mMenuEventListeners = new ArrayList<MenuEventListener>();
        pdfViewCtrl.registerDocEventListener(mDocEventListener);
        pdfViewCtrl.registerRecoveryEventListener(mRecoveryEventListener);
//        pdfViewCtrl.registerDoubleTapEventListener(mDoubleTapEventListener);
        pdfViewCtrl.registerTouchEventListener(mTouchEventListener);
        pdfViewCtrl.registerPageEventListener(mPageEventListener);

        pdfViewCtrl.setUIExtensionsManager(this);
        registerMenuEventListener(mMenuEventListener);

        if (config == null) {
            mConfig = new Config();
        } else {
            mConfig = config;
        }

        mMainFrame = new MainFrame(mContext, mConfig);
        mBaseBarMgr = new BaseBarManager(mContext, mMainFrame);

        if (mActivityLayout == null) {
            mActivityLayout = new RelativeLayout(mContext);
        } else {
            mActivityLayout.removeAllViews();
            mActivityLayout = new RelativeLayout(mContext);
        }
        mActivityLayout.setId(R.id.rd_main_id);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        mMainFrame.init(this);
        mMainFrame.addDocView(mPdfViewCtrl);
        mActivityLayout.addView(mMainFrame.getContentView(), params);
        mParent = mMainFrame.getContentView();
        mParent.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int newWidth  = right - left;
                int newHeight = bottom - top;
                int oldWidth  = oldRight - oldLeft;
                int oldHeight = oldBottom - oldTop;
                UIExtensionsManager.this.onLayoutChange(v, newWidth, newHeight, oldWidth, oldHeight);
            }
        });

        mPanelManager = mMainFrame.getPanelManager();
        if (mMainFrame.getAttachedActivity() != null) {
            setAttachedActivity(mMainFrame.getAttachedActivity());
        }
        if (mPanelManager == null) {
            mPanelManager = new PanelManager(context, this, mParent, null);
        }

        loadAllModules();

//        if (mConfig.modules.isLoadForm() || mConfig.modules.getAnnotConfig().isLoadStamp()) {
        initActionHandler();
//        }

        pdfViewCtrl.setConnectedPDFEventListener(mConnectedPDFEventListener); // for CDRM Document

        //json config
        PermissionsManager permissionsManager = new PermissionsManager(mContext, pdfViewCtrl);
        permissionsManager.setPermissions();
        UISettingsManager uiSettingsManager = new UISettingsManager(mContext, pdfViewCtrl);
        uiSettingsManager.setUISettings();


    }

    private void initActionHandler() {
        AnnotActionHandler annotActionHandler = (AnnotActionHandler) getDocumentManager().getActionCallback();
        if (annotActionHandler == null) {
            annotActionHandler = new AnnotActionHandler(mContext, mPdfViewCtrl);
        }
        getDocumentManager().setActionCallback(annotActionHandler);
    }

    /**
     * By default, all modules will be loaded..
     */
    private void loadAllModules() {
        if (mConfig.modules.isLoadTextSelection()) {
            //text select module
            TextSelectModule tsModule = new TextSelectModule(mContext, mPdfViewCtrl, this);
            tsModule.loadModule();
        }

        if (mConfig.modules.isLoadAnnotations() || mConfig.modules.isLoadSignature()) {
            registerToolHandler(new BlankSelectToolHandler(mContext, mPdfViewCtrl, this));
        }

        if (mConfig.modules.isLoadAnnotations()) {
            AnnotationsConfig annotConfig = mConfig.modules.getAnnotConfig();

            if (annotConfig.isLoadSquiggly()) {
                //squiggly annotation module
                SquigglyModule sqgModule = new SquigglyModule(mContext, mPdfViewCtrl, this);
                sqgModule.loadModule();
            }

            if (annotConfig.isLoadStrikeout() || annotConfig.isLoadReplaceText()) {
                //strikeout annotation module
                StrikeoutModule stoModule = new StrikeoutModule(mContext, mPdfViewCtrl, this);
                stoModule.loadModule();
            }

            if (annotConfig.isLoadUnderline()) {
                //underline annotation module
                UnderlineModule unlModule = new UnderlineModule(mContext, mPdfViewCtrl, this);
                unlModule.loadModule();
            }

            if (annotConfig.isLoadHighlight()) {
                //highlight annotation module
                HighlightModule hltModule = new HighlightModule(mContext, mPdfViewCtrl, this);
                hltModule.loadModule();
            }

//            if (annotConfig.isLoadNote()) {
            //note annotation module
            NoteModule noteModule = new NoteModule(mContext, mParent, mPdfViewCtrl, this);
            noteModule.loadModule();
//            }

            if (annotConfig.isLoadDrawCircle()) {
                //circle module
                CircleModule circleModule = new CircleModule(mContext, mPdfViewCtrl, this);
                circleModule.loadModule();
            }

            if (annotConfig.isLoadDrawSquare()) {
                //square module
                SquareModule squareModule = new SquareModule(mContext, mPdfViewCtrl, this);
                squareModule.loadModule();
            }

            if (annotConfig.isLoadTypewriter()) {
                //freetext: typewriter
                TypewriterModule typewriterModule = new TypewriterModule(mContext, mPdfViewCtrl, this);
                typewriterModule.loadModule();
            }

            if (annotConfig.isLoadCallout()) {
                // freetext: callout
                CalloutModule calloutModule = new CalloutModule(mContext, mPdfViewCtrl, this);
                calloutModule.loadModule();
            }

            if (annotConfig.isLoadStamp()) {
                //stamp module
                StampModule stampModule = new StampModule(mContext, mParent, mPdfViewCtrl, this);
                stampModule.loadModule();
            }

            if (annotConfig.isLoadInsertText() || annotConfig.isLoadReplaceText()) {
                //Caret module
                CaretModule caretModule = new CaretModule(mContext, mPdfViewCtrl, this);
                caretModule.loadModule();
            }

            if (annotConfig.isLoadDrawPencil() || annotConfig.isLoadEraser()) {
                //ink(pencil) module
                InkModule inkModule = new InkModule(mContext, mPdfViewCtrl, this);
                inkModule.loadModule();
            }

            if (annotConfig.isLoadEraser()) {
                //eraser module
                EraserModule eraserModule = new EraserModule(mContext, mPdfViewCtrl, this);
                eraserModule.loadModule();
            }

            if (annotConfig.isLoadDrawLine() || annotConfig.isLoadDrawArrow() || annotConfig.isLoadDrawDistance()) {
                //Line module
                LineModule lineModule = new LineModule(mContext, mPdfViewCtrl, this);
                lineModule.loadModule();
            }

            if (annotConfig.isLoadFileattach()) {
                //FileAttachment module
                FileAttachmentModule fileAttachmentModule = new FileAttachmentModule(mContext, mPdfViewCtrl, this);
                fileAttachmentModule.loadModule();
            }

            if (annotConfig.isLoadDrawPolygon() || annotConfig.isLoadDrawCloud()) {
                //Polygon module
                PolygonModule polygonModule = new PolygonModule(mContext, mPdfViewCtrl, this);
                polygonModule.loadModule();
            }

            if (annotConfig.isLoadDrawPolyLine()) {
                //PolyLine module
                PolyLineModule polyLineModule = new PolyLineModule(mContext, mPdfViewCtrl, this);
                polyLineModule.loadModule();
            }

            if (annotConfig.isLoadTextbox()) {
                //Textbox module
                TextBoxModule textBoxModule = new TextBoxModule(mContext, mPdfViewCtrl, this);
                textBoxModule.loadModule();
            }

            if (annotConfig.isLoadImage()) {
                // Image module
                PDFImageModule imageModule = new PDFImageModule(mContext, mPdfViewCtrl, this);
                imageModule.loadModule();
            }

            if (annotConfig.isLoadVideo() || annotConfig.isLoadAudio()) {
                // multimedia module
                MultimediaModule imageModule = new MultimediaModule(mContext, mPdfViewCtrl, this);
                imageModule.loadModule();
            }

            if (annotConfig.isLoadRedaction()) {
                //Redact module
                RedactModule redactModule = new RedactModule(mContext, mPdfViewCtrl, this);
                redactModule.loadModule();
            }

            //undo&redo module
            UndoModule undoModule = new UndoModule(mContext, mPdfViewCtrl, this);
            undoModule.loadModule();
        }

        //link module
        LinkModule linkModule = new LinkModule(mContext, mPdfViewCtrl, this);
        linkModule.loadModule();

        if (mConfig.modules.isLoadMultiSelect() && mConfig.modules.isLoadAnnotations()) {
            MultiSelectModule multiSelectModule = new MultiSelectModule(mContext, mParent, mPdfViewCtrl, this);
            multiSelectModule.loadModule();
        }

        if (mConfig.modules.isLoadPageNavigation()) {
            //page navigation module
            PageNavigationModule pageNavigationModule = new PageNavigationModule(mContext, mParent, mPdfViewCtrl, this);
            pageNavigationModule.loadModule();
        }

        if (mConfig.modules.isLoadForm()) {
            //form annotation module
            FormFillerModule formFillerModule = new FormFillerModule(mContext, mParent, mPdfViewCtrl, this);
            formFillerModule.loadModule();
        }

        if (mConfig.modules.isLoadSignature()) {
            //signature module
            SignatureModule signatureModule = new SignatureModule(mContext, mParent, mPdfViewCtrl, this);
            signatureModule.loadModule();

            DigitalSignatureModule dsgModule = new DigitalSignatureModule(mContext, mParent, mPdfViewCtrl, this);
            dsgModule.loadModule();
        }

        if (mConfig.modules.isLoadSearch()) {
            SearchModule searchModule = new SearchModule(mContext, mParent, mPdfViewCtrl, this);
            searchModule.loadModule();
        }

        if (mConfig.modules.isLoadReadingBookmark()) {
            ReadingBookmarkModule readingBookmarkModule = new ReadingBookmarkModule(mContext, mParent, mPdfViewCtrl, this);
            readingBookmarkModule.loadModule();
        }

        if (mConfig.modules.isLoadOutline()) {
            OutlineModule outlineModule = new OutlineModule(mContext, mParent, mPdfViewCtrl, this);
            outlineModule.loadModule();
        }

        if (mConfig.modules.isLoadAnnotations()) {
            //annot panel
            AnnotPanelModule annotPanelModule = new AnnotPanelModule(mContext, mPdfViewCtrl, this);
            annotPanelModule.loadModule();
        }

        if (mConfig.modules.isLoadAttachment()) {
            FileSpecPanelModule fileSpecPanelModule = new FileSpecPanelModule(mContext, mParent, mPdfViewCtrl, this);
            fileSpecPanelModule.loadModule();
        }

        if (mConfig.modules.isLoadThumbnail()) {
            ThumbnailModule thumbnailModule = new ThumbnailModule(mContext, mPdfViewCtrl, this);
            thumbnailModule.loadModule();
        }

        if (mConfig.modules.isLoadFileEncryption()) {
            //password module
            PasswordModule passwordModule = new PasswordModule(mContext, mPdfViewCtrl, this);
            passwordModule.loadModule();
        }

        if (!mConfig.uiSettings.disableFormNavigationBar) {
            //form navigation module
            FormNavigationModule formNavigationModule = new FormNavigationModule(mContext, mParent, this);
            formNavigationModule.loadModule();
        }

        if (mConfig.modules.isLoadSignature()) {
            // signature panel
            SignaturePanelModule signaturePanelModule = new SignaturePanelModule(mContext, mParent, mPdfViewCtrl, this);
            signaturePanelModule.loadModule();
        }

        ReflowModule reflowModule = new ReflowModule(mContext, mParent, mPdfViewCtrl, this);
        reflowModule.loadModule();

        DocInfoModule docInfoModule = new DocInfoModule(mContext, mParent, mPdfViewCtrl, this);
        docInfoModule.loadModule();

        BrightnessModule brightnessModule = new BrightnessModule(mContext, mPdfViewCtrl, this);
        brightnessModule.loadModule();

        ScreenLockModule screenLockModule = new ScreenLockModule(mContext, mPdfViewCtrl);
        screenLockModule.loadModule();

        PrintModule printModule = new PrintModule(mContext, mPdfViewCtrl, this);
        printModule.loadModule();

        MoreMenuModule mMoreMenuModule = new MoreMenuModule(mContext, mParent, mPdfViewCtrl, this);
        mMoreMenuModule.loadModule();

        CropModule cropModule = new CropModule(mContext, mParent, mPdfViewCtrl, this);
        cropModule.loadModule();

        PanZoomModule panZoomModule = new PanZoomModule(mContext, mParent, mPdfViewCtrl, this);
        panZoomModule.loadModule();

        DynamicXFAModule dynamicXFAModule = new DynamicXFAModule(mContext, mParent, mPdfViewCtrl, this);
        dynamicXFAModule.loadModule();

        ComparisonModule comparisonModule = new ComparisonModule(mContext, mParent, mPdfViewCtrl, this);
        comparisonModule.loadModule();
    }

    /**
     * @return the root view
     */
    public ViewGroup getRootView() {
        return mParent;
    }

//    @Override
//    protected void finalize() throws Throwable {
//        super.finalize();
//    }

    /**
     * Register a callback to be invoked when the tool handler changed.
     * <p>
     * Note: This method is only used within RDK
     */
    public void registerToolHandlerChangedListener(ToolHandlerChangedListener listener) {
        mHandlerChangedListeners.add(listener);
    }

    /**
     * Unregister the {@link ToolHandler} changed listener.
     * <p>
     * Note: This method is only used within RDK
     *
     * @param listener a {@link ToolHandlerChangedListener} to use.
     */
    public void unregisterToolHandlerChangedListener(ToolHandlerChangedListener listener) {
        mHandlerChangedListeners.remove(listener);
    }

    private void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
        for (ToolHandlerChangedListener listener : mHandlerChangedListeners) {
            listener.onToolHandlerChanged(lastTool, currentTool);
        }
    }

    /**
     * Register a callback to be invoked when the configuration changed.
     *
     * @param listener a {@link ConfigurationChangedListener} to use
     */
    public void registerConfigurationChangedListener(ConfigurationChangedListener listener) {
        mConfigurationChangedListeners.add(listener);
    }

    /**
     * unregister the specified {@link ConfigurationChangedListener}.
     *
     * @param listener the specified {@link ConfigurationChangedListener}
     */
    public void unregisterConfigurationChangedListener(ConfigurationChangedListener listener) {
        mConfigurationChangedListeners.remove(listener);
    }

    /**
     * Register a xfa page event listener.
     *
     * @param listener An <CODE>IPageEventListener</CODE> object to be registered.
     */
    public void registerXFAPageEventListener(IXFAPageEventListener listener) {
        mXFAPageEventListeners.add(listener);
    }

    /**
     * Unregister a xfa page event listener.
     *
     * @param listener An <CODE>IPageEventListener</CODE> object to be unregistered.
     */
    public void unregisterXFAPageEventListener(IXFAPageEventListener listener) {
        mXFAPageEventListeners.remove(listener);
    }

    /**
     * Called when the specified xfa page has removed.
     */
    public void onXFAPageRemoved(boolean isSuccess, int pageIndex) {
        for (IXFAPageEventListener listener : mXFAPageEventListeners) {
            listener.onPagesRemoved(isSuccess, pageIndex);
        }
    }

    /**
     * Called when a xfa page has added in the specified position.
     */
    public void onXFAPagesInserted(boolean isSuccess, int pageIndex) {
        for (IXFAPageEventListener listener : mXFAPageEventListeners) {
            listener.onPagesInserted(isSuccess, pageIndex);
        }
    }


    /**
     * Register a xfa widget event listener.
     *
     * @param listener An <CODE>IXFAWidgetEventListener</CODE> object to be registered.
     */
    public void registerXFAWidgetEventListener(IXFAWidgetEventListener listener) {
        mXFAWidgetEventListener.add(listener);
    }

    /**
     * Unregister a xfa widget event listener.
     *
     * @param listener An <CODE>IXFAWidgetEventListener</CODE> object to be unregistered.
     */
    public void unregisterXFAWidgetEventListener(IXFAWidgetEventListener listener) {
        mXFAWidgetEventListener.remove(listener);
    }

    /**
     * Called when a {@link XFAWidget} has added.
     */
    public void onXFAWidgetAdded(XFAWidget xfaWidget) {
        for (IXFAWidgetEventListener listener : mXFAWidgetEventListener) {
            listener.onXFAWidgetAdded(xfaWidget);
        }
    }

    /**
     * Called when a {@link XFAWidget} will be removed.
     */
    public void onXFAWidgetWillRemove(XFAWidget xfaWidget) {
        for (IXFAWidgetEventListener listener : mXFAWidgetEventListener) {
            listener.onXFAWidgetWillRemove(xfaWidget);
        }
    }

    /// @cond DEV
    public void registerLayoutChangeListener(ILayoutChangeListener listener) {
        mLayoutChangedListeners.add(listener);
    }

    public void unregisterLayoutChangeListener(ILayoutChangeListener listener) {
        mLayoutChangedListeners.remove(listener);
    }

    private void onLayoutChange(View view, int newWidth, int newHeight, int oldWidth, int oldHeight) {
        for (ILayoutChangeListener listener : mLayoutChangedListeners) {
            listener.onLayoutChange(view, newWidth, newHeight, oldWidth, oldHeight);
        }
    }
    /// @endcond

    /**
     * Set the current tool handler.
     *
     * @param toolHandler A <CODE>ToolHandler</CODE> object which specifies the current tool handler.
     */
    public void setCurrentToolHandler(ToolHandler toolHandler) {
        if (toolHandler == null && mCurToolHandler == null) {
            return;
        }

        boolean canAdd = true;
        if (toolHandler != null) {
            if (toolHandler.getType().equals(ToolHandler.TH_TYPE_SIGNATURE)) {
                canAdd = getDocumentManager().canAddSignature();
            } else {
                // Now, others are all annotation tool handler
                canAdd = getDocumentManager().canAddAnnot();
            }
        }

        if (!canAdd || (toolHandler != null && mCurToolHandler != null && mCurToolHandler.getType().equals(toolHandler.getType()))) {
            return;
        }
        ToolHandler lastToolHandler = mCurToolHandler;
        if (lastToolHandler != null) {
            lastToolHandler.onDeactivate();
        }

        if (toolHandler != null) {
            if (getDocumentManager().getCurrentAnnot() != null) {
                getDocumentManager().setCurrentAnnot(null);
            }
        }

        mCurToolHandler = toolHandler;
        if (mCurToolHandler != null) {
            mCurToolHandler.onActivate();
        }

        changeToolBarState(lastToolHandler, mCurToolHandler);
        onToolHandlerChanged(lastToolHandler, mCurToolHandler);
    }

    /**
     * Get the current tool handler.
     *
     * @return A <CODE>ToolHandler</CODE> object which specifies the current tool handler.
     */
    public ToolHandler getCurrentToolHandler() {
        return mCurToolHandler;
    }

    /**
     * Register the specified {@link ToolHandler} to current UI extensions manager.
     *
     * @param handler A <CODE>ToolHandler</CODE> object to be registered.
     */
    public void registerToolHandler(ToolHandler handler) {
        mToolHandlerList.put(handler.getType(), handler);
    }

    /**
     * Unregister the specified {@link ToolHandler} from current UI extensions manager.
     * <p>
     * Note: This method is only used within RDK
     *
     * @param handler A <CODE>ToolHandler</CODE> object to be unregistered.
     */
    public void unregisterToolHandler(ToolHandler handler) {
        mToolHandlerList.remove(handler.getType());
    }

    /**
     * get the specified {@link ToolHandler} from current UI extensions manager.
     *
     * @param type The tool handler type, refer to function {@link ToolHandler#getType()}.
     * @return A <CODE>ToolHandler</CODE> object with specified type.
     */
    public ToolHandler getToolHandlerByType(String type) {
        return mToolHandlerList.get(type);
    }

    /**
     * Register the specified {@link AnnotHandler} to current UI extensions manager.
     *
     * @param handler A {@link AnnotHandler} to use.
     */
    public void registerAnnotHandler(AnnotHandler handler) {
        mAnnotHandlerList.put(handler.getType(), handler);
    }

    /**
     * Unregister the specified {@link AnnotHandler} from current UI extensions manager.
     *
     * @param handler A {@link AnnotHandler} to use.
     */
    public void unregisterAnnotHandler(AnnotHandler handler) {
        mAnnotHandlerList.remove(handler.getType());
    }

    /**
     * @return Get the current {@link AnnotHandler}
     */
    public AnnotHandler getCurrentAnnotHandler() {
        Annot curAnnot = getDocumentManager().getCurrentAnnot();
        if (curAnnot == null) {
            return null;
        }

        return getAnnotHandlerByType(AppAnnotUtil.getAnnotHandlerType(curAnnot));
    }

    /**
     * Get the specified {@link AnnotHandler} from current UI extensions manager.
     *
     * @param type The type of {@link AnnotHandler}, refer to {@link AnnotHandler#getType()};
     * @return A {@link AnnotHandler} object with specified type.
     */
    public AnnotHandler getAnnotHandlerByType(int type) {
        return mAnnotHandlerList.get(type);
    }

    /**
     * Register the specified module to current UI extensions manager.
     * <p>
     * Note: This method is only used within RDK
     *
     * @param module A <CODE>Module</CODE> object to be registered.
     */
    public void registerModule(Module module) {
        mModules.add(module);
    }

    /**
     * Unregister the specified module from current UI extensions manager.
     * Note: This method is only used within RDK
     *
     * @param module A <CODE>Module</CODE> object to be unregistered.
     */
    public void unregisterModule(Module module) {
        mModules.remove(module);
    }


    /**
     * Get the specified module from current UI extensions manager.
     *
     * @param name The specified module name, refer to {@link Module#getName()}.
     * @return A <CODE>Module</CODE> object with specified module name.
     */
    public Module getModuleByName(String name) {
        for (Module module : mModules) {
            String moduleName = module.getName();
            if (moduleName != null && moduleName.compareTo(name) == 0)
                return module;
        }
        return null;
    }

    /**
     * Whether or not the annotation can be created continuously.
     *
     * @return <CODE>True</CODE> the annotation can be created continuously or otherwise.
     */
    public boolean isContinueAddAnnot() {
        return continueAddAnnot;
    }

    /**
     * Set whether the annotation can be created continuously. The default is false.
     *
     * @param continueAddAnnot whether the annot can be created continuously.
     */
    public void setContinueAddAnnot(boolean continueAddAnnot) {
        this.continueAddAnnot = continueAddAnnot;
        setToolHandlerContinueAddAnnot();
    }

    private void setToolHandlerContinueAddAnnot() {
        for (Map.Entry<String, ToolHandler> entry : mToolHandlerList.entrySet()) {
            entry.getValue().setContinueAddAnnot(continueAddAnnot);
        }
    }

    /**
     * Enable link annotation action event.
     *
     * @param enable True means link annotation action event can be triggered, false for else.
     */
    public void enableLinks(boolean enable) {
        mEnableLinkAnnot = enable;
    }

    /**
     * Check whether link annotation action event can be triggered.
     *
     * @return True means link annotation action event can be triggered, false for else.
     */
    public boolean isLinksEnabled() {
        return mEnableLinkAnnot;
    }

    /**
     * Check whether link highlight can be display.
     *
     * @return True means link highlight can be displayed, false for else.
     */
    public boolean isLinkHighlightEnabled() {
        return mEnableLinkHighlight;
    }

    /**
     * Enable highlight color of link annotation
     *
     * @param enable True means highlight color of link annotation can be displayed, false for else.
     */
    public void enableLinkHighlight(boolean enable) {
        this.mEnableLinkHighlight = enable;
    }


    /**
     * get the highlight color of link annotation.
     *
     * @return the highlight color
     */
    public long getLinkHighlightColor() {
        return mLinkHighlightColor;
    }

    /**
     * Set the highlight color of link annotation.
     *
     * @param color the highlight color to be set
     */
    public void setLinkHighlightColor(long color) {
        this.mLinkHighlightColor = color;
    }

    /**
     * Check whether form highlight can be displayed.
     *
     * @return True means form highlight can be displayed, false for else.
     */
    public boolean isFormHighlightEnable() {
        return mEnableFormHighlight;
    }

    /**
     * Get the highlight color of form field.
     *
     * @return the highlight color of form field.
     */
    public long getFormHighlightColor() {
        return mFormHighlightColor;
    }

    /**
     * Enable the highlight color of form field.
     *
     * @param enable True means highlight color of form field. can be displayed, false for else.
     */
    public void enableFormHighlight(boolean enable) {
        this.mEnableFormHighlight = enable;
    }

    /**
     * set form highlight color
     *
     * @param color the form highlight color to be set
     */
    public void setFormHighlightColor(long color) {
        this.mFormHighlightColor = color;
    }

    /**
     * Set highlight color (including alpha) when select text.
     *
     * @param color The highlight color to be set.
     */
    public void setSelectionHighlightColor(int color) {
        mSelectHighlightColor = color;
    }

    /**
     * Get highlight color (including alpha) when text has selected.
     *
     * @return The highlight color.
     */
    public int getSelectionHighlightColor() {
        return mSelectHighlightColor;
    }

    /**
     * Get current selected text content from text select tool handler.
     *
     * @return The current selected text content.
     */
    public String getCurrentSelectedText() {
        ToolHandler selectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
        if (selectionTool != null) {
            return ((TextSelectToolHandler) selectionTool).getCurrentSelectedText();
        }

        return null;
    }

    /**
     * Register a callback to be invoked when the menu event has triggered.
     */
    public void registerMenuEventListener(MenuEventListener listener) {
        mMenuEventListeners.add(listener);
    }

    /**
     * Unregister the specified {@link MenuEventListener}
     */
    public void unregisterMenuEventListener(MenuEventListener listener) {
        mMenuEventListeners.remove(listener);
    }

    /**
     * Called when menu has dismissed.
     */
    public void triggerDismissMenuEvent() {
        for (MenuEventListener listener : mMenuEventListeners) {
            listener.onTriggerDismissMenu();
        }
    }

    private boolean isSupportFullScreen() {
        return mState == ReadStateConfig.STATE_NORMAL ||
                mState == ReadStateConfig.STATE_REFLOW ||
                mState == ReadStateConfig.STATE_PANZOOM;
    }

    private static final int     MSG_COUNT_DOWN = 121;
    private              boolean isCountDown    = false;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_COUNT_DOWN:
                    if (isSupportFullScreen() && mMainFrame != null) {
//                        mMainFrame.hideToolbars();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public void startHideToolbarsTimer() {
        if (mConfig.uiSettings.fullscreen
                && mMainFrame != null
                && mMainFrame.isToolbarsVisible()
                && isSupportFullScreen()) {
            stopHideToolbarsTimer();
            isCountDown = mHandler.sendEmptyMessageDelayed(MSG_COUNT_DOWN, TOOL_BAR_SHOW_TIME);
        }
    }

    public void stopHideToolbarsTimer() {
        if (isCountDown) {
            mHandler.removeMessages(MSG_COUNT_DOWN);
            isCountDown = false;
        }
    }

    public void resetHideToolbarsTimer() {
        if (mConfig.uiSettings.fullscreen && mMainFrame != null && mMainFrame.isToolbarsVisible()) {
            startHideToolbarsTimer();
        }
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        resetHideToolbarsTimer();
        if (mPdfViewCtrl.isDynamicXFA()) {
            DynamicXFAModule dynamicXFAModule = (DynamicXFAModule) getModuleByName(Module.MODULE_NAME_DYNAMICXFA);
            if (dynamicXFAModule == null) return false;
            DynamicXFAWidgetHandler dynamicXFAWidgetHandler = (DynamicXFAWidgetHandler) dynamicXFAModule.getXFAWidgetHandler();
            if (dynamicXFAWidgetHandler == null) return false;
            return dynamicXFAWidgetHandler.onTouchEvent(pageIndex, motionEvent);
        }

        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_REFLOW) return false;
        PanZoomModule panZoomModule = (PanZoomModule) getModuleByName(Module.MODULE_NAME_PANZOOM);
        if (panZoomModule != null) {
            panZoomModule.onTouchEvent(pageIndex, motionEvent);
        }

        if (motionEvent.getPointerCount() > 1) return false;

        if (mCurToolHandler != null) {
            return mCurToolHandler.onTouchEvent(pageIndex, motionEvent);
        } else {
            //annot handler
            if (getDocumentManager().onTouchEvent(pageIndex, motionEvent)) return true;

            //blank selection tool
            ToolHandler blankSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
            if (blankSelectionTool != null && blankSelectionTool.onTouchEvent(pageIndex, motionEvent))
                return true;

            //text selection tool
            ToolHandler textSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (textSelectionTool != null && textSelectionTool.onTouchEvent(pageIndex, motionEvent))
                return true;
        }
        return false;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return getDocumentManager().shouldViewCtrlDraw(annot);
    }

    @Override
    public Annot getFocusAnnot() {
        if (mPdfViewCtrl == null) return null;
        return getDocumentManager().getFocusAnnot();
    }

    @SuppressLint("WrongCall")
    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mIsCompareDoc) {
            ComparisonModule comparisonModule = (ComparisonModule) getModuleByName(Module.MODULE_NAME_COMPARISON);
            if (comparisonModule == null) return;
            CompareHandler compareHandler = comparisonModule.getCompareHandler();
            if (compareHandler == null) return;
            compareHandler.onDraw(pageIndex, canvas);
            return;
        }

        if (mPdfViewCtrl.isDynamicXFA()) {
            DynamicXFAModule dynamicXFAModule = (DynamicXFAModule) getModuleByName(Module.MODULE_NAME_DYNAMICXFA);
            if (dynamicXFAModule == null) return;
            DynamicXFAWidgetHandler dynamicXFAWidgetHandler = (DynamicXFAWidgetHandler) dynamicXFAModule.getXFAWidgetHandler();
            if (dynamicXFAWidgetHandler == null) return;
            dynamicXFAWidgetHandler.onDraw(pageIndex, canvas);
            return;
        }

        for (ToolHandler handler : mToolHandlerList.values()) {
            handler.onDraw(pageIndex, canvas);
        }

        for (int i = 0; i < mAnnotHandlerList.size(); i++) {
            int          type    = mAnnotHandlerList.keyAt(i);
            AnnotHandler handler = mAnnotHandlerList.get(type);
            if (handler != null) {
                handler.onDraw(pageIndex, canvas);
            }
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        PointF displayViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
        int    pageIndex     = mPdfViewCtrl.getPageIndex(displayViewPt);
        if (mIsCompareDoc) {
            ComparisonModule comparisonModule = (ComparisonModule) getModuleByName(Module.MODULE_NAME_COMPARISON);
            if (comparisonModule == null) return false;
            CompareHandler compareHandler = comparisonModule.getCompareHandler();
            if (compareHandler == null) return false;
            if (compareHandler.onSingleTapConfirmed(pageIndex, motionEvent)) return true;
        }

        if (mPdfViewCtrl.isDynamicXFA()) {
            DynamicXFAModule dynamicXFAModule = (DynamicXFAModule) getModuleByName(Module.MODULE_NAME_DYNAMICXFA);
            if (dynamicXFAModule == null) return false;
            DynamicXFAWidgetHandler dynamicXFAWidgetHandler = (DynamicXFAWidgetHandler) dynamicXFAModule.getXFAWidgetHandler();
            if (dynamicXFAWidgetHandler == null) return false;
//            PointF displayViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
//            int pageIndex = mPdfViewCtrl.getPageIndex(displayViewPt);
            return dynamicXFAWidgetHandler.onSingleTapConfirmed(pageIndex, motionEvent);
        }

        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_REFLOW) return false;
        if (motionEvent.getPointerCount() > 1) return false;
//        PointF displayViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
//        int pageIndex = mPdfViewCtrl.getPageIndex(displayViewPt);


        if (mCurToolHandler != null) {
            return mCurToolHandler.onSingleTapConfirmed(pageIndex, motionEvent);
        } else {
            //annot handler
            if (getDocumentManager().onSingleTapConfirmed(pageIndex, motionEvent)) return true;

            // blank selection tool
            ToolHandler blankSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
            if (blankSelectionTool != null && blankSelectionTool.onSingleTapConfirmed(pageIndex, motionEvent))
                return true;

            //text selection tool
            ToolHandler textSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (textSelectionTool != null && textSelectionTool.onSingleTapConfirmed(pageIndex, motionEvent))
                return true;

            if (getDocumentManager().getCurrentAnnot() != null) {
                getDocumentManager().setCurrentAnnot(null);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        if (mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_REFLOW || mIsCompareDoc)
            return;
        if (motionEvent.getPointerCount() > 1) {
            return;
        }
        PointF displayViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
        int    pageIndex     = mPdfViewCtrl.getPageIndex(displayViewPt);

        if (mCurToolHandler != null) {
            if (mCurToolHandler.onLongPress(pageIndex, motionEvent)) {
                return;
            }
        } else {
            //annot handler
            if (getDocumentManager().onLongPress(pageIndex, motionEvent)) {
                return;
            }

            // blank selection tool
            ToolHandler blankSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
            if (blankSelectionTool != null && blankSelectionTool.onLongPress(pageIndex, motionEvent)) {
                return;
            }

            //text selection tool
            ToolHandler textSelectionTool = getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (textSelectionTool != null && textSelectionTool.onLongPress(pageIndex, motionEvent)) {
                return;
            }
        }
        return;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
    }

    PDFViewCtrl.IDoubleTapEventListener mDoubleTapEventListener = new PDFViewCtrl.IDoubleTapEventListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            if (getDocumentManager().getCurrentAnnot() != null) {
                getDocumentManager().setCurrentAnnot(null);
                return true;
            }
            if (mMainFrame.isToolbarsVisible()) {
                if (mConfig.uiSettings.fullscreen) {
                    mMainFrame.hideToolbars();
                    stopHideToolbarsTimer();
                }
            } else {
                mMainFrame.showToolbars();
                startHideToolbarsTimer();
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent motionEvent) {
            return false;
        }
    };

    private PDFViewCtrl.ITouchEventListener mTouchEventListener = new PDFViewCtrl.ITouchEventListener() {
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }
    };

    PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
            mIsDocOpened = false;
            mSaveFlag = PDFDoc.e_SaveFlagIncremental;
        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (mProgressDlg != null && mProgressDlg.isShowing()) {
                AppDialogManager.getInstance().dismiss(mProgressDlg);
                mProgressDlg = null;
            }

            switch (errCode) {
                case e_ErrSuccess:
                    mIsDocOpened = true;
                    if (isTwoColumnLeft(document)) {
                        mOldPageLayout = mPdfViewCtrl.getPageLayoutMode();
                        mPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_FACING);
                        mOldFileIsTwoColumnLeft = true;
                    } else if (mOldFileIsTwoColumnLeft) {
                        mPdfViewCtrl.setPageLayoutMode(mOldPageLayout);
                    }
                    if (mIsCompareDoc = isCompareDoc()) {
                        mState = ReadStateConfig.STATE_COMPARE;
                    }
                    doOpenAction(document);
                    if (!mPdfViewCtrl.isDynamicXFA()) {
                        getDocumentManager().initDocProperties(document);
                    }
                    setFilePath(mPdfViewCtrl.getFilePath());

                    bDocClosed = false;
                    isSaveDocInCurPath = false;
                    mPasswordError = false;
//                    changeState(mState);
                    //防止每次打开过几秒就变成了全屏
//                    mMainFrame.showToolbars();
                    return;
                case e_ErrPassword:
                    String tips;
                    if (mPasswordError) {
                        tips = AppResource.getString(mContext.getApplicationContext(), R.string.rv_tips_password_error);
                    } else {
                        tips = AppResource.getString(mContext.getApplicationContext(), R.string.rv_tips_password);
                    }
                    final UITextEditDialog uiTextEditDialog = new UITextEditDialog(mMainFrame.getAttachedActivity());
                    uiTextEditDialog.getDialog().setCanceledOnTouchOutside(false);
                    uiTextEditDialog.getInputEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    uiTextEditDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_password_dialog_title));
                    uiTextEditDialog.getPromptTextView().setText(tips);
                    uiTextEditDialog.show();
                    AppUtil.showSoftInput(uiTextEditDialog.getInputEditText());
                    uiTextEditDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            uiTextEditDialog.dismiss();
                            AppUtil.dismissInputSoft(uiTextEditDialog.getInputEditText());
                            String pw = uiTextEditDialog.getInputEditText().getText().toString();
                            mPdfViewCtrl.openDoc(mDocPath, pw.getBytes());
                        }
                    });

                    uiTextEditDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            uiTextEditDialog.dismiss();
                            AppUtil.dismissInputSoft(uiTextEditDialog.getInputEditText());
                            mPasswordError = false;
                            bDocClosed = true;
                            openDocumentFailed();
                        }
                    });

                    uiTextEditDialog.getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                uiTextEditDialog.getDialog().cancel();
                                mPasswordError = false;
                                bDocClosed = true;
                                openDocumentFailed();
                                return true;
                            }
                            return false;
                        }
                    });

                    if (!mPasswordError)
                        mPasswordError = true;
                    return;
                case e_ErrCanNotGetUserToken:
                    AccountModule.getInstance().showLoginDialog(mMainFrame.getAttachedActivity(), new IResult<Void, Void, Void>() {
                        @Override
                        public void onResult(boolean success, Void p1, Void p2, Void p3) {
                            if (success) {
                                mPdfViewCtrl.openDoc(mDocPath, null);
                            } else {
                                bDocClosed = true;
                                openDocumentFailed();
                            }
                        }
                    });
                    return;
                default:
                    bDocClosed = true;
                    String message = AppUtil.getMessage(mContext, errCode);
                    UIToast.getInstance(mContext).show(message);
                    openDocumentFailed();
                    break;
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            if (mProgressDlg != null && mProgressDlg.isShowing()) {
                AppDialogManager.getInstance().dismiss(mProgressDlg);
                mProgressDlg = null;
            }

            bDocClosed = true;
            closeDocumentSucceed();
            documentManager.setViewSignedDocFlag(false);
            if (errCode == e_ErrSuccess && isSaveDocInCurPath) {
                updateThumbnail(mSavePath);
            }
        }

        @Override
        public void onDocWillSave(PDFDoc document) {
        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {
            if (mProgressDlg != null && mProgressDlg.isShowing()) {
                AppDialogManager.getInstance().dismiss(mProgressDlg);
                mProgressDlg = null;
            }

            if (errCode == e_ErrSuccess && !isSaveDocInCurPath) {
                updateThumbnail(mSavePath);
            }

            if (isCloseDocAfterSaving) {
                closeAllDocuments();
            }
        }
    };

    private PDFViewCtrl.IPageEventListener mPageEventListener = new PDFViewCtrl.IPageEventListener() {
        @Override
        public void onPageVisible(int index) {
        }

        @Override
        public void onPageInvisible(int index) {
        }

        @Override
        public void onPageChanged(int oldPageIndex, int curPageIndex) {
        }

        @Override
        public void onPageJumped() {
        }

        @Override
        public void onPagesWillRemove(int[] pageIndexes) {
        }

        @Override
        public void onPageWillMove(int index, int dstIndex) {
        }

        @Override
        public void onPagesWillRotate(int[] pageIndexes, int rotation) {
        }

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {
            mSaveFlag = PDFDoc.e_SaveFlagXRefStream;
        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {
        }

        @Override
        public void onPagesRotated(boolean success, int[] pageIndexes, int rotation) {
        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] pageRanges) {
        }

        @Override
        public void onPagesWillInsert(int dstIndex, int[] pageRanges) {
        }
    };

    PDFViewCtrl.IRecoveryEventListener mRecoveryEventListener = new PDFViewCtrl.IRecoveryEventListener() {

        @Override
        public void onWillRecover() {
            mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_string_recovering);
            showProgressDlg();
            getDocumentManager().mCurAnnot = null;
            getDocumentManager().clearUndoRedo();
            getDocumentManager().reInit();
            BlankSelectToolHandler toolHandler = (BlankSelectToolHandler) getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
            if (toolHandler != null) {
                toolHandler.dismissMenu();
            }

            synchronized (AppFileUtil.getInstance().isOOMHappened) {
                AppFileUtil.getInstance().isOOMHappened = true;
            }
        }

        @Override
        public void onRecovered() {
            if (mProgressDlg != null && mProgressDlg.isShowing()) {
                AppDialogManager.getInstance().dismiss(mProgressDlg);
                mProgressDlg = null;
            }
            if (!mPdfViewCtrl.isDynamicXFA()) {
                getDocumentManager().initDocProperties(mPdfViewCtrl.getDoc());
            }

            initActionHandler();
            synchronized (AppFileUtil.getInstance().isOOMHappened) {
                AppFileUtil.getInstance().isOOMHappened = false;
            }
        }
    };

    /**
     * Set the attached activity.
     * <p>
     * If you want add a Note, FreeText, FileAttachment annotation; you must set the attached activity.
     * <p>
     * If you want to use the function of adding reply or comment to the annotation or about thumbnail,
     * you must set the attached activity and it must be a FragmentActivity.
     *
     * @param activity The attached activity.
     */
    public void setAttachedActivity(Activity activity) {
        mAttachActivity = activity;
    }

    /**
     * Get the attached activity.
     *
     * @return The attached activity.
     */
    public Activity getAttachedActivity() {
        return mAttachActivity;
    }

    /**
     * Return the current value in {@link #setPanelHidden}.
     *
     * @param panelType {@link PanelType#ReadingBookmarks}<br/>
     *                  {@link PanelType#Outline}<br/>
     *                  {@link PanelType#Annotations}<br/>
     *                  {@link PanelType#Attachments}<br/>
     * @return true means the panel is hidden.
     * @see #setPanelHidden(boolean, PanelType)
     */
    public boolean isHiddenPanel(com.foxit.uiextensions.controls.panel.PanelSpec.PanelType panelType) {
        if (panelType == null) {
            return true;
        }

        if (mMapPanelHiddenState.get(panelType) == null) {
            switch (panelType) {
                case ReadingBookmarks:
                    return !mConfig.modules.isLoadReadingBookmark();
                case Outline:
                    return !mConfig.modules.isLoadOutline();
                case Annotations:
                    return !mConfig.modules.isLoadAnnotations();
                case Attachments:
                    return !mConfig.modules.isLoadAttachment();
                case Signatures:
                    return !mConfig.modules.isLoadSignature();
                default:
                    break;
            }
        }
        return mMapPanelHiddenState.get(panelType);
    }

    /**
     * According to the {@link PanelType} control whether to show or hide the panel.
     * <p>
     * It will be work while the annotation module has been loaded.
     *
     * @param isHidden  true means to hidden the panel.
     * @param panelType {@link PanelType#ReadingBookmarks}<br/>
     *                  {@link PanelType#Outline}<br/>
     *                  {@link PanelType#Annotations}<br/>
     *                  {@link PanelType#Attachments}<br/>
     *                  {@link PanelType#Signatures}<br/>
     */
    public void setPanelHidden(boolean isHidden, PanelSpec.PanelType panelType) {
        if (panelType == null || (mMapPanelHiddenState.get(panelType) == null && !isHidden)) {
            return;
        }
        if (mMapPanelHiddenState.get(panelType) != null && mMapPanelHiddenState.get(panelType) == isHidden) {
            return;
        }

        if (isHidden) {
            Module module = getModuleByName(panelType.getModuleName());
            if (module != null) {

                if (module instanceof ReadingBookmarkModule) {
                    ((ReadingBookmarkModule) module).removePanel();
                } else {
                    module.unloadModule();
                    unregisterModule(module);
                }
                mMapPanelHiddenState.put(panelType, isHidden);
            }
        } else {
            switch (panelType) {
                case ReadingBookmarks:
                    if (mConfig.modules.isLoadReadingBookmark()) {
                        ReadingBookmarkModule readingBookmarkModule = (ReadingBookmarkModule) getModuleByName(panelType.getModuleName());
                        readingBookmarkModule.addPanel();
                    }
                    break;
                case Outline:
                    if (mConfig.modules.isLoadOutline()) {
                        OutlineModule outlineModule = new OutlineModule(mContext, mParent, mPdfViewCtrl, this);
                        outlineModule.loadModule();
                        outlineModule.prepareOutlinePanel();
                    }
                    break;
                case Annotations:
                    if (mConfig.modules.isLoadAnnotations()) {
                        AnnotPanelModule annotPanelModule = new AnnotPanelModule(mContext, mPdfViewCtrl, this);
                        annotPanelModule.loadModule();
                        annotPanelModule.prepareAnnotPanel();
                    }
                    break;
                case Attachments:
                    if (mConfig.modules.isLoadAttachment()) {
                        FileSpecPanelModule fileSpecPanelModule = new FileSpecPanelModule(mContext, mParent, mPdfViewCtrl, this);
                        fileSpecPanelModule.loadModule();
                    }
                    break;
                case Signatures:
                    if (mConfig.modules.isLoadSignature()) {
                        SignaturePanelModule signaturePanelModule = new SignaturePanelModule(mContext, mParent, mPdfViewCtrl, this);
                        signaturePanelModule.loadModule();
                    }
                    break;
                default:
                    break;
            }
            mMapPanelHiddenState.put(panelType, isHidden);
        }
    }

    /**
     * Print PDF documents and Static XFA documents(that is xfaDoc.getType() == XFADoc.e_Static)
     * <p>
     * Note: Only when OS version is Kitkat and above (Android API >= 19) the print function can be used
     *
     * @param context      The context to use. it must be instanceof Activity.
     * @param pdfDoc       The {@link PDFDoc} Object, it can not be empty .
     * @param printJobName print job name, it is can be null or empty.
     * @param fileName     The document name which may be shown to the user and
     *                     is the file name if the content it describes is saved as a PDF.
     *                     Cannot be empty.
     * @param callback     print callback {@link IPrintResultCallback}
     */
    public void startPrintJob(Context context, PDFDoc pdfDoc, String printJobName, String fileName, IPrintResultCallback callback) {
        if (pdfDoc == null || pdfDoc.isEmpty()) return;
        new PDFPrint.Builder(context)
                .setAdapter(new PDFPrintAdapter(context, pdfDoc, fileName, callback))
                .setOutputFileName(fileName)
                .setPrintJobName(printJobName)
                .print();
    }

    /**
     * Print Dynamic XFA documents (that is xfaDoc.getType() == XFADoc.e_Dynamic)
     * <p>
     * Note: Only when OS version is Kitkat and above (Android API >= 19) the print function can be used
     *
     * @param context      The context to use. it must be instanceof Activity.
     * @param xfaDoc       The {@link XFADoc} Object, it can not be empty .
     * @param printJobName print job name, it is can be null or empty.
     * @param fileName     The document name which may be shown to the user and
     *                     is the file name if the content it describes is saved as a PDF.
     *                     Cannot be empty.
     * @param callback     print callback {@link IPrintResultCallback}
     */
    public void startPrintJob(Context context, XFADoc xfaDoc, String printJobName, String fileName, IPrintResultCallback callback) {
        if (xfaDoc == null || xfaDoc.isEmpty()) return;
        new PDFPrint.Builder(context)
                .setAdapter(new XFAPrintAdapter(context, xfaDoc, fileName, callback))
                .setOutputFileName(fileName)
                .setPrintJobName(printJobName)
                .print();
    }

    /**
     * @return a {@link IPanelManager}
     */
    public IPanelManager getPanelManager() {
        return mPanelManager;
    }

    UIExtensionsManager.MenuEventListener mMenuEventListener = new UIExtensionsManager.MenuEventListener() {
        @Override
        public void onTriggerDismissMenu() {
            if (getDocumentManager().getCurrentAnnot() != null) {
                getDocumentManager().setCurrentAnnot(null);
            }
        }
    };

    /**
     * Get a property config
     *
     * @return {@link Config}
     */
    public Config getConfig() {
        return mConfig;
    }

    /**
     * Check whether the document can be modified.
     *
     * @return true means The document can be modified
     */
    public boolean canModifyContents() {
        return getDocumentManager().canModifyContents();
    }

    /**
     * Check whether the document can add annotation
     *
     * @return true means The document can add annotation
     */
    public boolean canAddAnnot() {
        return getDocumentManager().canAddAnnot();
    }

    /**
     * Exit the pan zoom mode.
     */
    public void exitPanZoomMode() {
        PanZoomModule module = (PanZoomModule) getModuleByName(Module.MODULE_NAME_PANZOOM);
        if (module != null) {
            module.exit();
        }
    }

    private PDFViewCtrl.IConnectedPDFEventListener mConnectedPDFEventListener = new PDFViewCtrl.IConnectedPDFEventListener() {
        @Override
        public ClientInfo getClientInfo() {
            ClientInfo clientInfo = new ClientInfo();
            clientInfo.setDevice_id(AppDevice.getDeviceId(mAttachActivity));
            clientInfo.setDevice_name(AppDevice.getDeviceName());
            clientInfo.setDevice_model(AppDevice.getDeviceModel());
            clientInfo.setMac_address(AppDevice.getMacAddr(mAttachActivity));
            clientInfo.setOs("Android");
            clientInfo.setProduct_name(AppDevice.PRODUCT_NAME);
            clientInfo.setProduct_vendor(AppDevice.PRODUCT_VENDOR);
            clientInfo.setProduct_version(BuildConfig.VERSION_NAME);
            clientInfo.setProduct_language(AppSystemUtil.getLanguage(mContext));
            return clientInfo;
        }

        @Override
        public String getUserToken(PDFDoc pdfDoc) {
            return AccountModule.getInstance().getUserToken();
        }

        @Override
        public void onACLReceived(String acl, PDFDoc pdfDoc) {
            getDocumentManager().parseACL(acl);
        }
    };

    private void release() {
        BlankSelectToolHandler selectToolHandler = (BlankSelectToolHandler) getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
        if (selectToolHandler != null) {
            selectToolHandler.unload();
            unregisterToolHandler(selectToolHandler);
        }

        for (Module module : mModules) {
            if (module instanceof LocalModule) continue;
            module.unloadModule();
        }

        mMenuEventListeners.clear();
        mLifecycleEventList.clear();
        mStateChangeEventList.clear();
        mXFAPageEventListeners.clear();
        mXFAWidgetEventListener.clear();
        mModules.clear();
        mModules = null;
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterRecoveryEventListener(mRecoveryEventListener);
        mPdfViewCtrl.unregisterDoubleTapEventListener(mDoubleTapEventListener);
        mPdfViewCtrl.unregisterTouchEventListener(mTouchEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        unregisterMenuEventListener(mMenuEventListener);
        getDocumentManager().destroy();

        mXFAPageEventListeners = null;
        mXFAWidgetEventListener = null;
        onFinishListener = null;
        mBackEventListener = null;
        mConnectedPDFEventListener = null;
        mTouchEventListener = null;
        mRecoveryEventListener = null;
        mDoubleTapEventListener = null;
        mPageEventListener = null;
        mDocEventListener = null;
        mMenuEventListener = null;
        mContext = null;
        mActivityLayout.removeAllViews();
        mActivityLayout = null;
        mCurToolHandler = null;
        mConfig = null;
        mPanelManager = null;
        mAttachActivity = null;

        documentManager = null;
        mPdfViewCtrl = null;
        mMainFrame.release();
        mMainFrame = null;
    }

    /**
     * Should be called in {@link Activity#onCreate(Bundle) } or {@link Fragment#onCreate(Bundle)}
     *
     * @see Activity#onCreate(Bundle)
     * @see Fragment#onCreate(Bundle)
     */
    public void onCreate(Activity act, PDFViewCtrl pdfViewCtrl, Bundle bundle) {
        if (mMainFrame.getAttachedActivity() != null && mMainFrame.getAttachedActivity() != act) {
            for (ILifecycleEventListener listener : mLifecycleEventList) {
                listener.onDestroy(act);
            }
        }
        mMainFrame.setAttachedActivity(act);

        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onCreate(act, bundle);
        }
    }

    /**
     * Should be called in {@link Activity#onConfigurationChanged(Configuration)} or {@link Fragment#onConfigurationChanged(Configuration)}
     *
     * @param act       The current activity
     * @param newConfig The new device configuration.
     * @see Activity#onConfigurationChanged(Configuration)
     * @see Fragment#onConfigurationChanged(Configuration)
     */
    public void onConfigurationChanged(Activity act, Configuration newConfig) {
        if (mMainFrame.getAttachedActivity() != act) return;
        if (mFolderSelectDialog != null && mFolderSelectDialog.isShowing()) {
            mFolderSelectDialog.setHeight(mFolderSelectDialog.getDialogHeight());
            mFolderSelectDialog.showDialog();
        }

        for (ConfigurationChangedListener listener : mConfigurationChangedListeners) {
            listener.onConfigurationChanged(newConfig);
        }
    }

    /**
     * Should be called in {@link Activity#onStart()} or {@link Fragment#onStart()}
     *
     * @see Activity#onStart()
     * @see Fragment#onStart()
     */
    public void onStart(Activity act) {
        if (mMainFrame.getAttachedActivity() != act) return;
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onStart(act);
        }
    }

    /**
     * Should be called in {@link Activity#onPause()} or {@link Fragment#onPause()}
     *
     * @see Activity#onPause()
     * @see Fragment#onPause()
     */
    public void onPause(Activity act) {
        if (mMainFrame.getAttachedActivity() != act) return;
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onPause(act);
        }
    }

    /**
     * Should be called in {@link Fragment#onHiddenChanged(boolean)}
     *
     * @param hidden True if the fragment is now hidden, false otherwise.
     * @see Fragment#onHiddenChanged(boolean)
     */
    public void onHiddenChanged(boolean hidden) {
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onHiddenChanged(hidden);
        }
    }

    /**
     * Should be called in {@link Activity#onResume()} or {@link Fragment#onResume()}
     *
     * @see Activity#onResume()
     * @see Fragment#onResume()
     */
    public void onResume(Activity act) {
        if (mMainFrame.getAttachedActivity() != act) return;
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onResume(act);
        }
    }

    /**
     * Should be called in {@link Activity#onStop()} or {@link Fragment#onStop()}
     *
     * @see Activity#onStop()
     * @see Fragment#onStop()
     */
    public void onStop(Activity act) {
        if (mMainFrame.getAttachedActivity() != act) return;
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onStop(act);
        }
    }

    /**
     * Should be called in {@link Activity#onDestroy()} or {@link Fragment#onDestroy()}
     *
     * @see Activity#onDestroy()
     * @see Fragment#onDestroy()
     */
    public void onDestroy(Activity act) {
        if (mMainFrame.getAttachedActivity() != act) return;
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onDestroy(act);
        }
        mMainFrame.setAttachedActivity(null);
        closeAllDocuments();
        release();
        AppDialogManager.getInstance().closeAllDialog();
    }

    /**
     * Receive and handle result from activity
     *
     * @param act         The current activity
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see Activity#onActivityResult(int, int, Intent)
     * @see Fragment#onActivityResult(int, int, Intent)
     */
    public void handleActivityResult(Activity act, int requestCode, int resultCode, Intent data) {
        if (mMainFrame.getAttachedActivity() != act) return;

        mPdfViewCtrl.handleActivityResult(requestCode, resultCode, data);

        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onActivityResult(act, requestCode, resultCode, data);
        }
    }

    /**
     * Open a PDF document from a specified PDF file path.
     *
     * @param path     A PDF file path.
     * @param password A byte array which specifies the password used to load the PDF document content. It can be either user password or owner password.
     *                 If the PDF document is not encrypted by password, just pass an empty string.
     */
    public void openDocument(String path, byte[] password) {
        _resetStatusBeforeOpen();
        mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_string_opening);
        showProgressDlg();

        setFilePath(path);
        mPdfViewCtrl.openDoc(path, password);
    }

    @Override
    public boolean registerLifecycleListener(ILifecycleEventListener listener) {
        mLifecycleEventList.add(listener);
        return true;
    }

    @Override
    public boolean unregisterLifecycleListener(ILifecycleEventListener listener) {
        mLifecycleEventList.remove(listener);
        return true;
    }

    @Override
    public boolean registerStateChangeListener(IStateChangeListener listener) {
        mStateChangeEventList.add(listener);
        return false;
    }

    @Override
    public boolean unregisterStateChangeListener(IStateChangeListener listener) {
        mStateChangeEventList.remove(listener);
        return false;
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public void changeState(int state) {
        int oldState = mState;
        mState = state;
        for (IStateChangeListener listener : mStateChangeEventList) {
            listener.onStateChanged(oldState, state);
        }
        if (mIsDocOpened) {
            startHideToolbarsTimer();
        }
    }

    @Override
    public IMainFrame getMainFrame() {
        return mMainFrame;
    }
    public MainFrame getMainFrame2() {
        return mMainFrame;
    }

    @Override
    public PDFViewCtrl getPDFViewCtrl() {
        return mPdfViewCtrl;
    }

    @Override
    public IBarsHandler getBarManager() {
        return mBaseBarMgr;
    }

    @Override
    public IMultiLineBar getSettingBar() {
        return mMainFrame.getSettingBar();
    }

    /// @cond DEV

    @Override
    public DocumentManager getDocumentManager() {
        return documentManager.on(mPdfViewCtrl);
    }

    /// @endcond

    @Override
    public RelativeLayout getContentView() {
        return mActivityLayout;
    }

    @Override
    public void backToPrevActivity() {
        if (getCurrentToolHandler() != null) {
            setCurrentToolHandler(null);
        }

        if (getDocumentManager() != null && getDocumentManager().getCurrentAnnot() != null) {
            getDocumentManager().setCurrentAnnot(null);
        }

        if (mPdfViewCtrl.isDynamicXFA()) {
            DynamicXFAModule dynamicXFAModule = (DynamicXFAModule) getModuleByName(Module.MODULE_NAME_DYNAMICXFA);
            if (dynamicXFAModule != null && dynamicXFAModule.getCurrentXFAWidget() != null) {
                dynamicXFAModule.setCurrentXFAWidget(null);
            }
        }

        if (mMainFrame.getAttachedActivity() == null) {
            mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_string_closing);
            closeAllDocuments();
            return;
        }

        if (mPdfViewCtrl.getDoc() == null || !getDocumentManager().isDocModified()) {
            mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_string_closing);
            closeAllDocuments();
            return;
        }

        final boolean hideSave = mPdfViewCtrl.isDynamicXFA() ? false : !getDocumentManager().canModifyContents();

        AlertDialog.Builder builder = new AlertDialog.Builder(mMainFrame.getAttachedActivity());
        String[]            items;
        if (hideSave) {
            items = new String[]{
                    AppResource.getString(mContext.getApplicationContext(), R.string.rv_back_save_to_new_file),
                    AppResource.getString(mContext.getApplicationContext(), R.string.rv_back_discard_modify),
            };
        } else {
            items = new String[]{
                    AppResource.getString(mContext.getApplicationContext(), R.string.rv_back_save_to_original_file),
                    AppResource.getString(mContext.getApplicationContext(), R.string.rv_back_save_to_new_file),
                    AppResource.getString(mContext.getApplicationContext(), R.string.rv_back_discard_modify),
            };
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (hideSave) {
                    which += 1;
                }
                switch (which) {
                    case 0: // save
                        isCloseDocAfterSaving = true;
                        mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_string_saving);
                        showProgressDlg();
                        if (mUserSavePath != null && mUserSavePath.length() > 0 && !mUserSavePath.equalsIgnoreCase(mDocPath)) {
                            File userSaveFile    = new File(mUserSavePath);
                            File defaultSaveFile = new File(mDocPath);
                            if (userSaveFile.getParent().equalsIgnoreCase(defaultSaveFile.getParent())) {
                                isSaveDocInCurPath = true;
                                mSavePath = mUserSavePath;
                            } else {
                                isSaveDocInCurPath = false;
                            }
                            mPdfViewCtrl.saveDoc(mUserSavePath, mSaveFlag);
                        } else {
                            isSaveDocInCurPath = true;
                            mPdfViewCtrl.saveDoc(getCacheFile(), mSaveFlag);
                        }
                        break;
                    case 1: // save as
                        mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_string_saving);
                        onSaveAsClicked();
                        break;
                    case 2: // discard modify
                        mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_string_closing);
                        isSaveDocInCurPath = false;
                        closeAllDocuments();
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
                mSaveAlertDlg = null;
            }

            void showInputFileNameDialog(final String fileFolder) {
                String       newFilePath = fileFolder + "/" + AppFileUtil.getFileName(mDocPath);
                final String filePath    = AppFileUtil.getFileDuplicateName(newFilePath);
                final String fileName    = AppFileUtil.getFileNameWithoutExt(filePath);

                final UITextEditDialog rmDialog = new UITextEditDialog(mMainFrame.getAttachedActivity());
                rmDialog.setPattern("[/\\:*?<>|\"\n\t]");
                rmDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_saveas));
                rmDialog.getPromptTextView().setVisibility(View.GONE);
                rmDialog.getInputEditText().setText(fileName);
                rmDialog.getInputEditText().selectAll();
                rmDialog.show();
                AppUtil.showSoftInput(rmDialog.getInputEditText());

                rmDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rmDialog.dismiss();
                        String inputName = rmDialog.getInputEditText().getText().toString();
                        String newPath   = fileFolder + "/" + inputName;
                        newPath += ".pdf";
                        File file = new File(newPath);
                        if (file.exists()) {
                            showAskReplaceDialog(fileFolder, newPath);
                        } else {
                            mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_string_saving);
                            ;
                            showProgressDlg();
                            isCloseDocAfterSaving = true;
                            mSavePath = newPath;
                            mPdfViewCtrl.saveDoc(newPath, mSaveFlag);
                        }
                    }
                });
            }

            void showAskReplaceDialog(final String fileFolder, final String newPath) {
                final UITextEditDialog rmDialog = new UITextEditDialog(mMainFrame.getAttachedActivity());
                rmDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_saveas));
                rmDialog.getPromptTextView().setText(AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_filereplace_warning));
                rmDialog.getInputEditText().setVisibility(View.GONE);
                rmDialog.show();

                rmDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rmDialog.dismiss();
                        mSavePath = newPath;
                        isCloseDocAfterSaving = true;
                        mProgressMsg = mContext.getApplicationContext().getString(R.string.fx_string_saving);
                        ;
                        showProgressDlg();
                        if (newPath.equalsIgnoreCase(mDocPath)) {
                            isSaveDocInCurPath = true;
                            mPdfViewCtrl.saveDoc(getCacheFile(), mSaveFlag);
                        } else {
                            isSaveDocInCurPath = false;
                            mPdfViewCtrl.saveDoc(newPath, mSaveFlag);
                        }
                    }
                });

                rmDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rmDialog.dismiss();
                        showInputFileNameDialog(fileFolder);
                    }
                });
            }

            void onSaveAsClicked() {
                mFolderSelectDialog = new UIFolderSelectDialog(mMainFrame.getAttachedActivity());
                mFolderSelectDialog.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !(pathname.isHidden() || !pathname.canRead()) && !pathname.isFile();
                    }
                });
                mFolderSelectDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_saveas));
                mFolderSelectDialog.setButton(MatchDialog.DIALOG_OK | MatchDialog.DIALOG_CANCEL);
                mFolderSelectDialog.setListener(new MatchDialog.DialogListener() {
                    @Override
                    public void onResult(long btType) {
                        if (btType == MatchDialog.DIALOG_OK) {
                            String fileFolder = mFolderSelectDialog.getCurrentPath();
                            showInputFileNameDialog(fileFolder);
                        }
                        mFolderSelectDialog.dismiss();
                    }

                    @Override
                    public void onBackClick() {
                    }
                });
                mFolderSelectDialog.showDialog();
            }
        });

        mSaveAlertDlg = builder.create();
        mSaveAlertDlg.setCanceledOnTouchOutside(true);
        mSaveAlertDlg.show();
    }

    @Override
    public void setBackEventListener(BackEventListener listener) {
        mBackEventListener = listener;
    }

    @Override
    public BackEventListener getBackEventListener() {
        return mBackEventListener;
    }

    /**
     * Should be call in {@link Activity#onKeyDown(int, KeyEvent)}
     *
     * @param act     The current activity
     * @param keyCode The value in event.getKeyCode().
     * @param event   Description of the key event.
     * @return Return <code>true</code> to prevent this event from being propagated
     * further, or <code>false</code> to indicate that you have not handled
     * this event and it should continue to be propagated.
     * @see Activity#onKeyDown(int, KeyEvent)
     */
    public boolean onKeyDown(Activity act, int keyCode, KeyEvent event) {
        if (mMainFrame.getAttachedActivity() != act) return false;
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (backToNormalState()) return true;

            if (event.getRepeatCount() == 0) {
                backToPrevActivity();
                return true;
            }
        }
        return false;
    }

    /**
     * Show normal view reading state
     *
     * @return True means ic_title_back the normal state
     */
    public boolean backToNormalState() {
        ComparisonModule comparisonModule = (ComparisonModule) getModuleByName(Module.MODULE_NAME_COMPARISON);
        if (comparisonModule != null) {
            comparisonModule.onKeyBack();
        }

        if (mPdfViewCtrl.isDynamicXFA()) {
            DynamicXFAModule dynamicXFAModule = (DynamicXFAModule) getModuleByName(Module.MODULE_NAME_DYNAMICXFA);
            if (dynamicXFAModule != null && dynamicXFAModule.onKeyBack()) {
                changeState(ReadStateConfig.STATE_NORMAL);
                mMainFrame.showToolbars();
                return true;
            }
        }

        PageNavigationModule pageNavigationModule = (PageNavigationModule) getModuleByName(Module.MODULE_NAME_PAGENAV);
        if (pageNavigationModule != null && pageNavigationModule.onKeyBack()) {
            return true;
        }

        SearchModule searchModule = ((SearchModule) getModuleByName(Module.MODULE_NAME_SEARCH));
        if (searchModule != null && searchModule.onKeyBack()) {
            changeState(ReadStateConfig.STATE_NORMAL);
            mMainFrame.showToolbars();
            return true;
        }

        FormFillerModule formFillerModule = (FormFillerModule) getModuleByName(Module.MODULE_NAME_FORMFILLER);
        if (formFillerModule != null && formFillerModule.onKeyBack()) {
            changeState(ReadStateConfig.STATE_NORMAL);
            mMainFrame.showToolbars();
            return true;
        }

        ToolHandler     currentToolHandler = getCurrentToolHandler();
        SignatureModule signature_module   = (SignatureModule) getModuleByName(Module.MODULE_NAME_PSISIGNATURE);
        if (signature_module != null && currentToolHandler instanceof SignatureToolHandler && signature_module.onKeyBack()) {
            changeState(ReadStateConfig.STATE_NORMAL);
            mMainFrame.showToolbars();
            mPdfViewCtrl.invalidate();
            return true;
        }

        FileAttachmentModule fileAttachmentModule = (FileAttachmentModule) getModuleByName(Module.MODULE_NAME_FILEATTACHMENT);
        if (fileAttachmentModule != null && fileAttachmentModule.onKeyBack()) {
            mMainFrame.showToolbars();
            return true;
        }

        FileSpecPanelModule fileSpecPanelModule = (FileSpecPanelModule) getModuleByName(Module.MODULE_NAME_FILE_PANEL);
        if (fileSpecPanelModule != null && fileSpecPanelModule.onKeyBack()) {
            mMainFrame.showToolbars();
            return true;
        }

        SignaturePanelModule signaturePanelModule = (SignaturePanelModule) getModuleByName(Module.MODULE_NAME_SIGNATUREPANEL);
        if (signaturePanelModule != null && signaturePanelModule.onKeyBack()) {
            mMainFrame.showToolbars();
            return true;
        }

        CropModule cropModule = (CropModule) getModuleByName(Module.MODULE_NAME_CROP);
        if (cropModule != null && cropModule.onKeyBack()) {
            mMainFrame.showToolbars();
            return true;
        }

        PanZoomModule panZoomModule = (PanZoomModule) getModuleByName(Module.MODULE_NAME_PANZOOM);
        if (panZoomModule != null && panZoomModule.exit()) {
            mMainFrame.showToolbars();
            return true;
        }

        BlankSelectToolHandler selectToolHandler = (BlankSelectToolHandler) getToolHandlerByType(ToolHandler.TH_TYPE_BLANKSELECT);
        if (selectToolHandler != null && selectToolHandler.onKeyBack()) {
            return true;
        }

        TextSelectModule textSelectModule = (TextSelectModule) getModuleByName(Module.MODULE_NAME_SELECTION);
        if (textSelectModule != null && textSelectModule.onKeyBack()) {
            return true;
        }

        if (getDocumentManager().onKeyBack()) {
            return true;
        }

        if (currentToolHandler != null) {
            setCurrentToolHandler(null);
            return true;
        }

        if (getState() != ReadStateConfig.STATE_NORMAL && getState() != ReadStateConfig.STATE_COMPARE) {
            changeState(ReadStateConfig.STATE_NORMAL);
            return true;
        }

        return false;
    }

    /**
     * Set the file path
     */
    public void setFilePath(String path) {
        mDocPath = path;
        MoreMenuModule module = ((MoreMenuModule) getModuleByName(Module.MODULE_MORE_MENU));
        if (module != null) {
            module.setFilePath(path);
        }
    }

    private void doOpenAction(@NonNull PDFDoc pdfDoc) {
        try {
            pdfDoc.doJSOpenAction();
        } catch (PDFException e) {
        }
    }

    /**
     * Whether show the top tool bar
     *
     * @param isEnabled <CODE>True</CODE> show the top tool bar, or otherwise.
     */
    public void enableTopToolbar(boolean isEnabled) {
        if (mMainFrame != null) {
            mMainFrame.enableTopToolbar(isEnabled);
        }
    }

    /**
     * Whether show the bottom tool bar
     *
     * @param isEnabled <CODE>True</CODE> show the bottom tool bar, or otherwise.
     */
    public void enableBottomToolbar(boolean isEnabled) {
        if (mMainFrame != null) {
            mMainFrame.enableBottomToolbar(isEnabled);
        }
    }

    /**
     * Set the flag to be used when the document has saved
     */
    public void setSaveDocFlag(int flag) {
        mSaveFlag = flag;
    }

    /**
     * Get the flag while saving a document has used
     */
    public int getSaveDocFlag() {
        return mSaveFlag;
    }

    private void openDocumentFailed() {
        if (mMainFrame.getAttachedActivity() != null) {
            if (onFinishListener != null) {
                onFinishListener.onFinish();
            } else {
                mMainFrame.getAttachedActivity().finish();
            }
        }
    }

    private void _resetStatusAfterClose() {
        changeState(ReadStateConfig.STATE_NORMAL);
    }

    private void _resetStatusBeforeOpen() {
//        mMainFrame.showToolbars();
        mState = ReadStateConfig.STATE_NORMAL;
    }

    private void closeAllDocuments() {
        if (!bDocClosed) {
            _closeDocument();
        } else if (mMainFrame.getAttachedActivity() != null) {
            if (onFinishListener != null) {
                onFinishListener.onFinish();
            } else {
                mMainFrame.getAttachedActivity().finish();
            }
        }
    }

    private void showProgressDlg() {
        if (mProgressDlg == null && mMainFrame.getAttachedActivity() != null) {
            mProgressDlg = new ProgressDialog(mMainFrame.getAttachedActivity());
            mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDlg.setCancelable(false);
            mProgressDlg.setIndeterminate(false);
        }

        if (mProgressDlg != null && !mProgressDlg.isShowing()) {
            mProgressDlg.setMessage(mProgressMsg);
            AppDialogManager.getInstance().showAllowManager(mProgressDlg, null);
        }
    }

    private void _closeDocument() {
        showProgressDlg();

        mPdfViewCtrl.closeDoc();
        _resetStatusAfterClose();
        stopHideToolbarsTimer();
        mMainFrame.resetMaskView();
        documentManager.setDocModified(false);
        getDocumentManager().clearUndoRedo();
    }

    private void closeDocumentSucceed() {
        if (mMainFrame != null && mMainFrame.getAttachedActivity() != null) {

            if (onFinishListener != null) {
                onFinishListener.onFinish();
            } else {
                mMainFrame.getAttachedActivity().finish();
            }
        }

        if (isSaveDocInCurPath) {
            if (currentFileCachePath == null) return;
            if (mDocPath.endsWith(".ppdf")) {
                currentFileCachePath = AppFileUtil.replaceFileExtension(currentFileCachePath, ".ppdf");
            }
            File file    = new File(currentFileCachePath);
            File docFile = new File(mDocPath);
            if (file.exists()) {
                docFile.delete();
                if (!file.renameTo(docFile))
                    UIToast.getInstance(mContext.getApplicationContext()).show(mContext.getApplicationContext().getString(R.string.fx_save_file_failed));
            } else {
                UIToast.getInstance(mContext.getApplicationContext()).show(mContext.getApplicationContext().getString(R.string.fx_save_file_failed));
            }
        }
    }

    private void updateThumbnail(String path) {
        LocalModule module = (LocalModule) getModuleByName(Module.MODULE_NAME_LOCAL);
        if (module != null && path != null) {
            module.updateThumbnail(path);
        }
    }

    private String getCacheFile() {
        mSavePath = mDocPath;
        File   file = new File(mDocPath);
        String dir  = file.getParent() + "/";
        while (file.exists()) {
            currentFileCachePath = dir + AppDmUtil.randomUUID(null) + ".pdf";
            file = new File(currentFileCachePath);
        }
        return currentFileCachePath;
    }

    private void changeToolBarState(ToolHandler oldToolHandler, ToolHandler newToolHandler) {
        if (newToolHandler instanceof FormFillerToolHandler || newToolHandler instanceof RedactToolHandler)
            return;

        if (newToolHandler instanceof SignatureToolHandler) {
            triggerDismissMenuEvent();
            mMainFrame.resetAnnotCustomBottomBar();
            mMainFrame.resetAnnotCustomTopBar();
            changeState(ReadStateConfig.STATE_SIGNATURE);
        } else if (newToolHandler != null) {
            changeState(ReadStateConfig.STATE_ANNOTTOOL);
        } else if (getState() == ReadStateConfig.STATE_ANNOTTOOL) {
            changeState(ReadStateConfig.STATE_EDIT);
        }
        if (oldToolHandler instanceof SignatureToolHandler && newToolHandler == null) {
            changeState(ReadStateConfig.STATE_NORMAL);
        }

//        if (!mMainFrame.isToolbarsVisible())
//            mMainFrame.showToolbars();
    }

    /**
     * Set the path where the document will be saved
     */
    public void setSavePath(String savePath) {
        mUserSavePath = savePath;
    }

    /**
     * @return the path where the document has saved
     */
    public String getSavePath() {
        return mUserSavePath;
    }

    /**
     * @return the {@link IMenuView} for interacting with menus
     */
    public IMenuView getMenuView() {
        MoreMenuModule module = ((MoreMenuModule) getModuleByName(Module.MODULE_MORE_MENU));
        if (module == null) return null;
        return module.getMenuView();
    }

    /**
     * Check whether the specified annotation module that is loaded.
     */
    public boolean isLoadAnnotModule(Annot annot) {
        String jsonName = AppAnnotUtil.getAnnotJsonModuleName(annot);
        if (!mConfig.modules.isLoadAnnotations()) return false;
        AnnotationsConfig annotConfig = mConfig.modules.getAnnotConfig();
        switch (jsonName) {
            case "highlight":
                return annotConfig.isLoadHighlight();
            case "underline":
                return annotConfig.isLoadUnderline();
            case "squiggly":
                return annotConfig.isLoadSquiggly();
            case "strikeout":
                return annotConfig.isLoadStrikeout();
            case "insert":
                return annotConfig.isLoadInsertText();
            case "replace":
                return annotConfig.isLoadReplaceText();
            case "line":
                return annotConfig.isLoadDrawLine();
            case "rectangle":
                return annotConfig.isLoadDrawSquare();
            case "oval":
                return annotConfig.isLoadDrawCircle();
            case "arrow":
                return annotConfig.isLoadDrawArrow();
            case "pencil":
                return annotConfig.isLoadDrawPencil();
            case "typewriter":
                return annotConfig.isLoadTypewriter();
            case "textbox":
                return annotConfig.isLoadTextbox();
            case "callout":
                return annotConfig.isLoadCallout();
            case "note":
                return annotConfig.isLoadNote();
            case "stamp":
                return annotConfig.isLoadStamp();
            case "polygon":
                return annotConfig.isLoadDrawPolygon();
            case "cloud":
                return annotConfig.isLoadDrawCloud();
            case "polyline":
                return annotConfig.isLoadDrawPolyLine();
            case "distance":
                return annotConfig.isLoadDrawDistance();
            case "image":
                return annotConfig.isLoadImage();
            case "audio":
                return annotConfig.isLoadAudio();
            case "video":
                return annotConfig.isLoadVideo();
            case "fileattachment":
                return annotConfig.isLoadFileattach();
            case "redaction":
                return annotConfig.isLoadRedaction();
            default:
                return false;
        }
    }

    private boolean isTwoColumnLeft(@NonNull PDFDoc pdfDoc) {
        try {
            PDFDictionary root = pdfDoc.getCatalog();
            if (root.hasKey("PageLayout")) {
                PDFObject pageLayout = root.getElement("PageLayout");
                if (pageLayout.getName().equalsIgnoreCase("TwoColumnLeft")) {
                    return true;
                }
            }
        } catch (PDFException e) {

        }
        return false;
    }

    private boolean isCompareDoc() {
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) return false;
        try {
            PDFDictionary root = mPdfViewCtrl.getDoc().getCatalog();
            if (root != null) {
                boolean bExistPieceInfo = root.hasKey("PieceInfo");
                if (!bExistPieceInfo) return false;
                PDFDictionary pieceInfo = root.getElement("PieceInfo").getDict();
                if (pieceInfo == null) return false;
                return pieceInfo.hasKey("ComparePDF");
            }
        } catch (PDFException e) {

        }
        return false;
    }
}
