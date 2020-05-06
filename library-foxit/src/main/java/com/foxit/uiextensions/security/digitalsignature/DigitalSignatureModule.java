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
package com.foxit.uiextensions.security.digitalsignature;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.security.certificate.CertificateSupport;
import com.foxit.uiextensions.utils.AppSQLite;
import com.foxit.uiextensions.utils.OnPageEventListener;

import java.io.File;
import java.util.ArrayList;

public class DigitalSignatureModule implements Module {

	private DigitalSignatureSecurityHandler mSecurityHandler;

	private DigitalSignatureAnnotHandler mAnnotHandler;

	public DigitalSignatureUtil getDigitalSignatureUtil() {
		return mDigitalSignatureUtil;
	}

	private DigitalSignatureUtil mDigitalSignatureUtil;
	private CertificateSupport mSupport;
    private Context mContext;
	private ViewGroup mParent;
	private PDFViewCtrl mPdfViewCtrl;
	private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

	private static final String DB_TABLE_DSG_PFX 			= "_pfx_dsg_cert";
	private static final String PUBLISHER 				= "publisher";
	private static final String ISSUER 					= "issuer";
	private static final String SERIALNUMBER			= "serial_number";
	private static final String FILEPATH				= "file_path";
	private static final String CHANGEFILEPATH				= "file_change_path";
	private static final String FILENAME				= "file_name";
	private static final String PASSWORD				= "password";

	public DigitalSignatureModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
		this.mContext = context;
		this.mParent = parent;
		this.mPdfViewCtrl = pdfViewCtrl;
		mUiExtensionsManager = uiExtensionsManager;
	}

	@Override
	public String getName() {
		return Module.MODULE_NAME_DIGITALSIGNATURE;
	}


	public AnnotHandler getAnnotHandler() {
		return mAnnotHandler;
	}

    @Override
	public boolean loadModule() {
		if (!AppSQLite.getInstance(mContext).isDBOpened()) {
			AppSQLite.getInstance(mContext).openDB();
		}
		mSupport = new CertificateSupport(mContext);
		mSecurityHandler = new DigitalSignatureSecurityHandler(mContext,mPdfViewCtrl, mSupport);
		mAnnotHandler = new DigitalSignatureAnnotHandler(mContext,mParent, mPdfViewCtrl, mSecurityHandler);

		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
			((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mAnnotHandler);
			((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigurationChangedListener);
			((UIExtensionsManager) mUiExtensionsManager).registerLayoutChangeListener(mLayoutChangeListener);
			((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
		}

		mDigitalSignatureUtil = new DigitalSignatureUtil(mContext, mPdfViewCtrl);
		initDBTableForDSG();

		mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
		mPdfViewCtrl.registerRecoveryEventListener(recoveryEventListener);
		mPdfViewCtrl.registerPageEventListener(mPageEventListener);

		//for signature sign operation.
		mDocPathChangeListener = new DigitalSignatureModule.DocPathChangeListener() {
			@Override
			public void onDocPathChange(String newPath) {
				((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setFilePath(newPath);
			}
		};

		return true;
	}


	@Override
	public boolean unloadModule() {
		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
			((UIExtensionsManager) mUiExtensionsManager).unregisterLayoutChangeListener(mLayoutChangeListener);
		}
		mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
		mPdfViewCtrl.unregisterRecoveryEventListener(recoveryEventListener);
		mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
		mDocPathChangeListener = null;
		mDrawEventListener = null;
		recoveryEventListener = null;
		mPageEventListener = null;
        return true;
	}


	private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


		@Override
		public void onDraw(int pageIndex, Canvas canvas) {
			if(mAnnotHandler!= null)
				mAnnotHandler.onDrawForControls(canvas);
		}
	};

	private DocPathChangeListener mDocPathChangeListener = null;

	public void setDocPathChangeListener(DocPathChangeListener listener) {
		mDocPathChangeListener = listener;
	}

	public DocPathChangeListener getDocPathChangeListener() {
		return mDocPathChangeListener;
	}

	public interface DocPathChangeListener {
		void onDocPathChange(String newPath);
	}

	private void initDBTableForDSG() {
		if (!AppSQLite.getInstance(mContext).isTableExist(DB_TABLE_DSG_PFX)) {
			ArrayList<AppSQLite.FieldInfo> fieldInfos  = new ArrayList<AppSQLite.FieldInfo>();
			fieldInfos.add(new AppSQLite.FieldInfo(SERIALNUMBER, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(ISSUER, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(PUBLISHER, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(FILEPATH, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(CHANGEFILEPATH, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(FILENAME, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(PASSWORD, AppSQLite.KEY_TYPE_VARCHAR));
			AppSQLite.getInstance(mContext).createTable(DB_TABLE_DSG_PFX, fieldInfos);
		}
		String filePath = mContext.getFilesDir() + "/DSGCert";
		File file = new File(filePath);
		if(!file.exists()) {
			file.mkdirs();
		}
	}

	PDFViewCtrl.IRecoveryEventListener recoveryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
		@Override
		public void onWillRecover() {

		}

		@Override
		public void onRecovered() {

		}
	};

	private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
		@Override
		public void onConfigurationChanged(Configuration newConfig) {
			mSecurityHandler.onConfigurationChanged(newConfig);
		}
	};

	private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
		@Override
		public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
			mAnnotHandler.onLayoutChange(v, newWidth, newHeight, oldWidth, oldHeight);
		}
	};

	private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener(){

		@Override
		public void onPagesRotated(boolean success, int[] pageIndexes, int rotation) {
            mAnnotHandler.onPagesRotated(success, pageIndexes, rotation);
		}
	};

	public DigitalSignatureSecurityHandler getSecurityHandler() {
		return mSecurityHandler;
	}
}
