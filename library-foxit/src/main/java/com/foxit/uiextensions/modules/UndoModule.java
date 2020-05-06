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
package com.foxit.uiextensions.modules;

import android.content.Context;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Note;
import com.foxit.uiextensions.AbstractUndo;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.IUndoItem;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.DefaultAnnotHandler;
import com.foxit.uiextensions.annots.IFlattenEventListener;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.OnPageEventListener;

import java.util.ArrayList;

/** Support undo/redo for annotation operations.(add/edit/delete..etc).*/
public class UndoModule implements Module {
	private Context mContext;
	private DefaultAnnotHandler	mDefAnnotHandler;

	private PDFViewCtrl mPdfViewCtrl;
	private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

	private IBaseItem mUndoButton;
	private IBaseItem mRedoButton;

	public UndoModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
		mContext = context;
		mPdfViewCtrl = pdfViewCtrl;
		mUiExtensionsManager = uiExtensionsManager;
	}

	@Override
	public String getName() {
		return MODULE_NAME_UNDO;
	}

	@Override
	public boolean loadModule() {
		mDefAnnotHandler = new DefaultAnnotHandler(mContext, mPdfViewCtrl);
		mPdfViewCtrl.registerPageEventListener(mPageEventListener);

		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
			((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mDefAnnotHandler);
			((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
			((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerFlattenEventListener(mFlattenEventListener);
			addUndo();
		}
		return true;
	}

	private void addUndo(){
		UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;

		mUndoButton = new BaseItemImpl(mContext);
		mUndoButton.setTag(ToolbarItemConfig.ITEM_BAR_UNDO);
		mUndoButton.setImageResource(R.drawable.annot_undo_pressed);
		mUndoButton.setEnable(false);
		uiExtensionsManager.getMainFrame().getEditDoneBar().addView(mUndoButton, BaseBar.TB_Position.Position_LT);

		mRedoButton = new BaseItemImpl(mContext);
		mRedoButton.setTag(ToolbarItemConfig.ITEM_BAR_REDO);
		mRedoButton.setImageResource(R.drawable.annot_redo_pressed);
		mRedoButton.setEnable(false);
		uiExtensionsManager.getMainFrame().getEditDoneBar().addView(mRedoButton, BaseBar.TB_Position.Position_LT);

		mUndoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AppUtil.isFastDoubleClick())
					return;
				undo();
			}
		});

		mRedoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (AppUtil.isFastDoubleClick())
					return;
				redo();
			}
		});

		uiExtensionsManager.getDocumentManager().registerUndoEventListener(mUndoEventListener);
		uiExtensionsManager.registerStateChangeListener(mStateChangeListener);
	}


	private AbstractUndo.IUndoEventListener mUndoEventListener = new AbstractUndo.IUndoEventListener() {
		@Override
		public void itemWillAdd(DocumentManager dm, IUndoItem item) {
		}

		@Override
		public void itemAdded(DocumentManager dm, IUndoItem item) {
			changeButtonStatus();
		}

		@Override
		public void willUndo(DocumentManager dm, IUndoItem item) {
		}

		@Override
		public void undoFinished(DocumentManager dm, IUndoItem item) {
			changeButtonStatus();
		}

		@Override
		public void willRedo(DocumentManager dm, IUndoItem item) {
		}

		@Override
		public void redoFinished(DocumentManager dm, IUndoItem item) {
			changeButtonStatus();
		}

		@Override
		public void willClearUndo(DocumentManager dm) {
		}

		@Override
		public void clearUndoFinished(DocumentManager dm) {
			changeButtonStatus();
		}
	};

	private IStateChangeListener mStateChangeListener = new IStateChangeListener() {
		@Override
		public void onStateChanged(int oldState, int newState) {
			changeButtonStatus();
		}
	};

	private void changeButtonStatus() {
		DocumentManager dm = ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager();
		if (dm.canUndo()) {
			mUndoButton.setImageResource(R.drawable.annot_undo_enabled);
			mUndoButton.setEnable(true);
		} else {
			mUndoButton.setImageResource(R.drawable.annot_undo_pressed);
			mUndoButton.setEnable(false);
		}
		if (dm.canRedo()) {
			mRedoButton.setImageResource(R.drawable.annot_redo_enabled);
			mRedoButton.setEnable(true);
		} else {
			mRedoButton.setImageResource(R.drawable.annot_redo_pressed);
			mRedoButton.setEnable(false);
		}
		if (((UIExtensionsManager) mUiExtensionsManager).getCurrentToolHandler() != null
				&& ((UIExtensionsManager) mUiExtensionsManager).getCurrentToolHandler().getType().equals(ToolHandler.TH_TYPE_INK)) {
			mUndoButton.getContentView().setVisibility(View.INVISIBLE);
			mRedoButton.getContentView().setVisibility(View.INVISIBLE);
		} else {
			mUndoButton.getContentView().setVisibility(View.VISIBLE);
			mRedoButton.getContentView().setVisibility(View.VISIBLE);
		}
	}

	public AnnotHandler getAnnotHandler() {
		return mDefAnnotHandler;
	}

	private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener(){
		@Override
		public void onPageMoved(boolean success, int index, int dstIndex) {
			((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().onPageMoved(success,index,dstIndex);
		}

		@Override
		public void onPagesRemoved(boolean success, int[] pageIndexes) {
			for(int i = 0; i < pageIndexes.length; i++)
				((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().onPageRemoved(success,pageIndexes[i] - i);
		}

		@Override
		public void onPagesInserted(boolean success, int dstIndex, int[] range) {
			((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().onPagesInsert(success, dstIndex, range);
		}
	};

    private ArrayList<String> mReplyLists = new ArrayList<>();

    private void getAnnotReplys(Annot annot) {
        try {
            if (annot.isMarkup()) {
                Markup markup = new Markup(annot);
                int count = markup.getReplyCount();

                if (count > 0) {
                    for (int i = 0; i < count; i++) {
                        Note note = markup.getReply(i);
                        if (note.getReplyCount() > 0) {
                            getAnnotReplys(note);
                        }
                        mReplyLists.add(note.getUniqueID());
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

	private IFlattenEventListener mFlattenEventListener = new IFlattenEventListener() {
		@Override
		public void onAnnotWillFlatten(PDFPage page, Annot annot) {
			try {
				mReplyLists.clear();
				getAnnotReplys(annot);
				mReplyLists.add(annot.getUniqueID());
				for (String uniqueId : mReplyLists) {
					((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().onAnnotWillFlatten(page.getIndex(), uniqueId);
				}
			} catch (PDFException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onAnnotFlattened(PDFPage page, Annot annot) {
			changeButtonStatus();
		}
	};

	@Override
	public boolean unloadModule() {
		mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
			((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mDefAnnotHandler);

			((UIExtensionsManager) mUiExtensionsManager).unregisterStateChangeListener(mStateChangeListener);
			((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterFlattenEventListener(mFlattenEventListener);
			((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterUndoEventListener(mUndoEventListener);
		}
		return true;
	}

	private void undo() {
		if (((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().canUndo()) {
			((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().undo();
		}
	}

	private void redo() {
		if (((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().canRedo()) {
			((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().redo();
		}
	}
}
