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
package com.foxit.uiextensions.annots.ink;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;


public class InkModule implements Module {
	protected InkAnnotUtil mUtil;
	protected InkAnnotHandler mAnnotHandler;
	protected InkToolHandler mToolHandler;
	private Context mContext;
	private PDFViewCtrl mPdfViewCtrl;
	private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

	public InkModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
		mContext = context;
		mPdfViewCtrl = pdfViewCtrl;
		mUiExtensionsManager = uiExtensionsManager;
	}

	@Override
	public String getName() {
		return Module.MODULE_NAME_INK;
	}
	
	@Override
	public boolean loadModule() {
		mUtil = new InkAnnotUtil();

		mToolHandler = new InkToolHandler(mContext, mPdfViewCtrl, mUtil);
		mAnnotHandler = new InkAnnotHandler(mContext, mPdfViewCtrl,mUiExtensionsManager, mToolHandler, mUtil);
		mToolHandler.mAnnotHandler = mAnnotHandler;
		mAnnotHandler.setAnnotMenu(new AnnotMenuImpl(mContext, mPdfViewCtrl));
		mAnnotHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl));

		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
			((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);
			((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mAnnotHandler);
			((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
			((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigureChangeListener);
		}
		mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
		mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
		return true;
	}

	@Override
	public boolean unloadModule() {
		mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
		mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
		mToolHandler.uninitUiElements();

		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
			((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);
			((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
			((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigureChangeListener);
		}
		return true;
	}

	public AnnotHandler getAnnotHandler() {
		return mAnnotHandler;
	}

	public ToolHandler getToolHandler() {
		return mToolHandler;
	}

	PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
		@Override
		public void onWillRecover() {
			if (mAnnotHandler.getAnnotMenu() != null && mAnnotHandler.getAnnotMenu().isShowing()) {
				mAnnotHandler.getAnnotMenu().dismiss();
			}

			if (mAnnotHandler.getPropertyBar() != null && mAnnotHandler.getPropertyBar().isShowing()) {
				mAnnotHandler.getPropertyBar().dismiss();
			}
		}

		@Override
		public void onRecovered() {
		}
	};

	private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


		@Override
		public void onDraw(int pageIndex, Canvas canvas) {
			mAnnotHandler.onDrawForControls(canvas);
		}
	};

	UIExtensionsManager.ConfigurationChangedListener mConfigureChangeListener = new UIExtensionsManager.ConfigurationChangedListener() {
		@Override
		public void onConfigurationChanged(Configuration newConfig) {
			mToolHandler.onConfigurationChanged(newConfig);
		}
	};
}
