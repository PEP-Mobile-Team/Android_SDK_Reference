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
package com.foxit.uiextensions.annots.line;

import android.content.Context;
import android.graphics.Canvas;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.config.modules.annotations.AnnotationsConfig;
import com.foxit.uiextensions.config.uisettings.annotations.annots.DistanceConfig;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;

import java.util.ArrayList;


public class LineModule implements Module {
	protected LineUtil mUtil;
	protected LineToolHandler mLineToolHandler;
	protected LineToolHandler mArrowToolHandler;
	protected LineToolHandler mDistanceToolHandler;
	protected LineAnnotHandler mLineAnnotHandler;

	Context mContext;
	PDFViewCtrl mPdfViewCtrl;
	private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
	public LineModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
		mContext = context;
		mPdfViewCtrl = pdfViewCtrl;
		mUiExtensionsManager = uiExtensionsManager;
	}

	@Override
	public String getName() {
		return Module.MODULE_NAME_LINE;
	}

	@Override
	public boolean loadModule() {
		mUtil = new LineUtil(mContext, this);
		mLineAnnotHandler = new LineAnnotHandler(mContext, mPdfViewCtrl, mUtil);

		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
			Config config = ((UIExtensionsManager) mUiExtensionsManager).getConfig();
			AnnotationsConfig annotConfig = config.modules.getAnnotConfig();

			// tool line
			if (annotConfig.isLoadDrawLine()) {
				mLineToolHandler = new LineToolHandler(mContext, mPdfViewCtrl, mUtil, LineConstants.INTENT_LINE_DEFAULT);
				mLineAnnotHandler.mRealAnnotHandler.initialize(LineConstants.INTENT_LINE_DEFAULT);
				mLineToolHandler.mAnnotHandler = mLineAnnotHandler.mRealAnnotHandler;
				mLineAnnotHandler.setAnnotMenu(LineConstants.INTENT_LINE_DEFAULT, new AnnotMenuImpl(mContext, mPdfViewCtrl));
				mLineAnnotHandler.setPropertyBar(LineConstants.INTENT_LINE_DEFAULT, new PropertyBarImpl(mContext, mPdfViewCtrl));
				((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mLineToolHandler);
				mLineToolHandler.setColor(config.uiSettings.annotations.line.color);
				mLineToolHandler.setThickness(config.uiSettings.annotations.line.thickness);
				mLineToolHandler.setOpacity((int) (config.uiSettings.annotations.line.opacity * 100));
				mLineToolHandler.initUiElements();
			}

			// arrow line
			if (annotConfig.isLoadDrawArrow()) {
				mArrowToolHandler = new LineToolHandler(mContext, mPdfViewCtrl, mUtil, LineConstants.INTENT_LINE_ARROW);
				mLineAnnotHandler.mRealAnnotHandler.initialize(LineConstants.INTENT_LINE_ARROW);
				mArrowToolHandler.mAnnotHandler = mLineAnnotHandler.mRealAnnotHandler;
				mLineAnnotHandler.setAnnotMenu(LineConstants.INTENT_LINE_ARROW, new AnnotMenuImpl(mContext, mPdfViewCtrl));
				mLineAnnotHandler.setPropertyBar(LineConstants.INTENT_LINE_ARROW, new PropertyBarImpl(mContext, mPdfViewCtrl));
				((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mArrowToolHandler);
				mArrowToolHandler.setColor(config.uiSettings.annotations.arrow.color);
				mArrowToolHandler.setThickness(config.uiSettings.annotations.arrow.thickness);
				mArrowToolHandler.setOpacity((int) (config.uiSettings.annotations.arrow.opacity * 100));
				mArrowToolHandler.initUiElements();
			}

			if (annotConfig.isLoadDrawDistance()) {
				mDistanceToolHandler = new LineToolHandler(mContext, mPdfViewCtrl, mUtil, LineConstants.INTENT_LINE_DIMENSION);
				mLineAnnotHandler.mRealAnnotHandler.initialize(LineConstants.INTENT_LINE_DIMENSION);
				mDistanceToolHandler.mAnnotHandler = mLineAnnotHandler.mRealAnnotHandler;
				mLineAnnotHandler.setAnnotMenu(LineConstants.INTENT_LINE_DIMENSION, new AnnotMenuImpl(mContext, mPdfViewCtrl));
				mLineAnnotHandler.setPropertyBar(LineConstants.INTENT_LINE_DIMENSION, new PropertyBarImpl(mContext, mPdfViewCtrl));
				((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mDistanceToolHandler);

				DistanceConfig distance = config.uiSettings.annotations.distance;
				mDistanceToolHandler.setColor(distance.color);
				mDistanceToolHandler.setOpacity((int) (distance.opacity * 100));
				mDistanceToolHandler.setThickness(distance.thickness);
				ArrayList<String> unitStrings = fakeDate();
				if (unitStrings.contains(distance.scaleFromUnit)){
					mDistanceToolHandler.setScaleFromUnitIndex(unitStrings.indexOf(distance.scaleFromUnit));
				}
				if (unitStrings.contains(distance.scaleToUnit)){
					mDistanceToolHandler.setScaleToUnitIndex(unitStrings.indexOf(distance.scaleToUnit));
				}
				mDistanceToolHandler.setScaleFromValue(distance.scaleFromValue);
				mDistanceToolHandler.setScaleToValue(distance.scaleToValue);

				mDistanceToolHandler.initUiElements();
			}

            ((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mLineAnnotHandler);
			((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
		}
		mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
		mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);

		return true;
	}

	private ArrayList<String> fakeDate() {
		ArrayList<String> list = new ArrayList<>();
		for (DistanceMeasurement rest : DistanceMeasurement.values()) {
			list.add(rest.getName());
		}
		return list;
	}

	@Override
	public boolean unloadModule() {
		mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
		mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {

			if(mArrowToolHandler != null){
				((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mArrowToolHandler);
				mArrowToolHandler.uninitUiElements();
			}
			if (mLineToolHandler != null){
				((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mLineToolHandler);
				mLineToolHandler.uninitUiElements();
			}

			if (mDistanceToolHandler != null){
				((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mDistanceToolHandler);
				mDistanceToolHandler.uninitUiElements();
			}
			((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mLineAnnotHandler);
		}
		return true;
	}

	public AnnotHandler getAnnotHandler() {
		return mLineAnnotHandler;
	}

	public ToolHandler getLineToolHandler() {
		return mLineToolHandler;
	}

	public ToolHandler getArrowToolHandler() {
		return mArrowToolHandler;
	}

	public LineToolHandler getDistanceToolHandler() {
		return mDistanceToolHandler;
	}

	private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

		@Override
		public void onDraw(int pageIndex, Canvas canvas) {
			mLineAnnotHandler.onDrawForControls(canvas);
		}
	};

	PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
		@Override
		public void onWillRecover() {
		}

		@Override
		public void onRecovered() {
		}
	};
}
