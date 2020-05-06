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
package com.foxit.uiextensions.annots;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Line;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.IAnnotTaskResult;
import com.foxit.uiextensions.annots.common.UIAnnotFrame;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class DefaultAnnotHandler extends AbstractAnnotHandler {
	protected ArrayList<Integer> mMenuText;

	public DefaultAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
		super(context, pdfViewCtrl, Annot.e_UnknownType);
		mMenuText = new ArrayList<Integer>();
	}

	@Override
	protected AbstractToolHandler getToolHandler() {
		return null;
	}

	@Override
	public boolean annotCanAnswer(Annot annot) {
		return AppAnnotUtil.isSupportEditAnnot(annot);
	}

	@Override
	public void onAnnotSelected(final Annot annot, boolean reRender) {
		super.onAnnotSelected(annot, reRender);
	}

	@Override
	public void onAnnotDeselected(Annot annot, boolean reRender) {
		if (!mIsModified) {
			super.onAnnotDeselected(annot, reRender);
		}
	}

	@Override
	public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {
		int action = e.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			return super.onTouchEvent(pageIndex, e, annot);
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			PointF point = new PointF(e.getX(), e.getY());
			mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
			try {

				if (mTouchCaptured && pageIndex == annot.getPage().getIndex()
						&& annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
					if (action == MotionEvent.ACTION_UP|| action == MotionEvent.ACTION_CANCEL) {
						mTouchCaptured = false;
						mDownPt.set(0, 0);
						mLastPt.set(0, 0);
						mOp = UIAnnotFrame.OP_DEFAULT;
						mCtl = UIAnnotFrame.CTL_NONE;
					}
					return true;
				}
			} catch (PDFException e1) {
				e1.printStackTrace();
			}
			break;
			default:
				break;
		}
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
		return super.onSingleTapConfirmed(pageIndex, motionEvent, annot);
	}

	@Override
	public boolean shouldViewCtrlDraw(Annot annot) {
		return true;
	}

	@Override
	public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
		return super.onLongPress(pageIndex, motionEvent, annot);
	}

	@Override
	public void onDraw(int pageIndex, Canvas canvas) {
		Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
		if (annot == null) return;
		try {
			if (mSelectedAnnot == annot && annot.getPage().getIndex() == pageIndex) {
				RectF bbox = AppUtil.toRectF(annot.getRect());
				mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
				RectF mapBounds = UIAnnotFrame.mapBounds(mPdfViewCtrl, pageIndex, annot, mOp, mCtl,
						mLastPt.x - mDownPt.x, mLastPt.y - mDownPt.y);

				if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
					int color = (int)annot.getBorderColor() | 0xFF000000;
					int opacity = (int)(((Markup)annot).getOpacity() * 255f);
					UIAnnotFrame.getInstance(mContext).drawFrame(canvas, mapBounds, color, opacity);
				}
			}
		} catch (PDFException e) {

		}

	}

	@Override
	public void addAnnot(int pageIndex, final AnnotContent content, boolean addUndo, final Event.Callback result) {
		try {
			PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
			final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(content.getType(), AppUtil.toFxRectF(content.getBBox())), content.getType());
			DefaultAnnotAddUndoItem undoItem = new DefaultAnnotAddUndoItem(this, mPdfViewCtrl);
			undoItem.mPageIndex = pageIndex;
			undoItem.mNM = content.getNM();
			undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
			undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
			undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
			undoItem.mFlags = Annot.e_FlagPrint;
			undoItem.mColor = content.getColor();
			undoItem.mOpacity = content.getOpacity() / 255f;
			undoItem.mBBox = new RectF(content.getBBox());
			undoItem.mIntent = content.getIntent();
			undoItem.mLineWidth = content.getLineWidth();
			undoItem.mType = content.getType();

			addAnnot(pageIndex, annot, undoItem, addUndo,result);
		} catch (PDFException e) {
			e.printStackTrace();
		}
	}

	protected void addAnnot(int pageIndex, Annot annot, DefaultAnnotAddUndoItem undoItem, boolean addUndo, final Event.Callback result) {
		final DefaultAnnotEvent event = new DefaultAnnotEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
		if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
			if (result != null) {
				result.result(event, true);
			}
			return;
		}
		handleAddAnnot(pageIndex, annot, event, addUndo, new IAnnotTaskResult<PDFPage, Annot, Void>() {
			@Override
			public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
				if (result != null) {
					result.result(event, success);
				}
			}
		});
	}

	@Override
	public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
		DefaultAnnotModifyUndoItem undoItem = new DefaultAnnotModifyUndoItem(this, mPdfViewCtrl);
		undoItem.setOldValue(annot);
		undoItem.setCurrentValue(content);
		modifyAnnot(annot, undoItem, false, addUndo, true, result);
	}


	protected void modifyAnnot(Annot annot, DefaultAnnotUndoItem undoItem, boolean useOldValue, boolean addUndo, boolean reRender, final Event.Callback result) {
		DefaultAnnotEvent modifyEvent = new DefaultAnnotEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, annot, mPdfViewCtrl);
		modifyEvent.useOldValue = useOldValue;
		handleModifyAnnot(annot, modifyEvent, addUndo, reRender,
				new IAnnotTaskResult<PDFPage, Annot, Void>() {
			@Override
			public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
				if (result != null) {
					result.result(null, success);
				}
			}
		});
	}

	@Override
	public void removeAnnot(Annot annot, boolean addUndo, final Event.Callback result) {
		DefaultAnnotDeleteUndoItem undoItem = new DefaultAnnotDeleteUndoItem(this, mPdfViewCtrl);
		undoItem.setCurrentValue(annot);
		removeAnnot(annot, undoItem, addUndo, result);
	}

	protected void removeAnnot(Annot annot, DefaultAnnotDeleteUndoItem undoItem, boolean addUndo, final Event.Callback result) {

		DefaultAnnotEvent deleteEvent = new DefaultAnnotEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, annot, mPdfViewCtrl);
		if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
		    try {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(annot.getPage(), annot);
                if (result != null) {
                    result.result(deleteEvent, true);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return;
		}
		handleRemoveAnnot(annot, deleteEvent, addUndo,
				new IAnnotTaskResult<PDFPage, Void, Void>() {
					@Override
					public void onResult(boolean success, PDFPage p1, Void p2, Void p3) {
						if (result != null) {
							result.result(null, success);
						}
					}
				});
	}

	@Override
	protected ArrayList<Path> generatePathData(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot) {
		return new ArrayList<Path>();
	}

	@Override
	protected void transformAnnot(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, Matrix matrix) {
	}

	@Override
	protected void resetStatus() {
		mBackRect = null;
		mSelectedAnnot = null;
		mIsModified = false;
	}

	@Override
	protected void showPopupMenu() {
		Annot curAnnot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
		if (curAnnot == null) return;
		try {
			if (!AppAnnotUtil.isSupportReply(curAnnot))
                return;

			reloadPopupMenuString();
			mAnnotMenu.setMenuItems(mMenuText);
			RectF bbox = AppUtil.toRectF(curAnnot.getRect());
			int pageIndex = curAnnot.getPage().getIndex();
			mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
			mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
			mAnnotMenu.show(bbox);
			mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
				@Override
				public void onAMClick(int flag) {
					if (mSelectedAnnot == null) return;
					if (flag == AnnotMenu.AM_BT_COMMENT) { // comment
						((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
						UIAnnotReply.showComments(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mSelectedAnnot);
					} else if (flag == AnnotMenu.AM_BT_REPLY) { // reply
						((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
						UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mSelectedAnnot);
					}
				}
			});
		} catch (PDFException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void dismissPopupMenu() {
		mAnnotMenu.dismiss();
	}

	@Override
	protected void showPropertyBar(long curProperty) {
		Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
		if (annot != null) {
			mPropertyBar.setPropertyChangeListener(this);
			mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, getColor());
			mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, getColor());
			mPropertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, getThickness());
			mPropertyBar.reset(getSupportedProperties());

			try {
				RectF bbox = AppUtil.toRectF(annot.getRect());
				int pageIndex = annot.getPage().getIndex();
				mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
				mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
				RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), bbox);
				mPropertyBar.show(rectF, false);
			} catch (PDFException e) {
				e.printStackTrace();
			}


		}
	}

	@Override
	protected void hidePropertyBar() {
		if (mPropertyBar.isShowing()) {
			mPropertyBar.dismiss();
		}
	}

	@Override
	protected long getSupportedProperties() {
		return PropertyBar.PROPERTY_COLOR;
	}

	@Override
	protected void setPropertyBarProperties(PropertyBar propertyBar) {
		propertyBar.setProperty(PropertyBar.PROPERTY_COLOR, getColor());
		if (AppDisplay.getInstance(mContext).isPad())
			propertyBar.setArrowVisible(true);
	}

	@Override
	public void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint, Annot annot) {
		super.setPaintProperty(pdfViewCtrl, pageIndex, paint, annot);
	}

	protected void onLanguageChanged() {
		reloadPopupMenuString();
	}

	protected void reloadPopupMenuString() {
		mMenuText.clear();
		mMenuText.add(AnnotMenu.AM_BT_COMMENT);
		if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
			mMenuText.add(AnnotMenu.AM_BT_REPLY);
		}
	}
}

class DefaultAnnotEvent extends EditAnnotEvent {
	public DefaultAnnotEvent(int eventType, DefaultAnnotUndoItem undoItem, Annot annot, PDFViewCtrl pdfViewCtrl) {
		mType = eventType;
		mUndoItem = undoItem;
		mAnnot = annot;
		mPdfViewCtrl = pdfViewCtrl;
	}

	@Override
	public boolean add() {
		if (mAnnot == null) {
			return false;
		}

		try {
			if (mAnnot.getType() != mUndoItem.mType) {
				return false;
			}

			mAnnot.setBorderColor(mUndoItem.mColor);
			((Markup) (mAnnot)).setOpacity(mUndoItem.mOpacity);
			if (mUndoItem.mContents == null) {
				mUndoItem.mContents = "";
			}
			mAnnot.setContent(mUndoItem.mContents);
			mAnnot.setFlags(mUndoItem.mFlags);
			if (mUndoItem.mCreationDate != null && AppDmUtil.isValidDateTime(mUndoItem.mCreationDate)) {
				((Markup) (mAnnot)).setCreationDateTime(mUndoItem.mCreationDate);
			}

			if (mUndoItem.mModifiedDate != null && AppDmUtil.isValidDateTime(mUndoItem.mModifiedDate)) {
				mAnnot.setModifiedDateTime(mUndoItem.mModifiedDate);
			}

			if (mUndoItem.mAuthor != null) {
				((Markup) (mAnnot)).setTitle(mUndoItem.mAuthor);
			}

			if (mUndoItem.mIntent != null) {
				((Markup) (mAnnot)).setIntent(mUndoItem.mIntent);
			}

			if (mUndoItem.mSubject != null) {
				((Markup) (mAnnot)).setSubject(mUndoItem.mSubject);
			}

			BorderInfo borderInfo = new BorderInfo();
			borderInfo.setWidth(mUndoItem.mLineWidth);
			mAnnot.setBorderInfo(borderInfo);

			mAnnot.setFlags(mUndoItem.mFlags);
			mAnnot.setUniqueID(mUndoItem.mNM);
			mAnnot.resetAppearanceStream();
			return true;
		} catch (PDFException e) {
			if (e.getLastError() == Constants.e_ErrOutOfMemory) {
				mPdfViewCtrl.recoverForOOM();
			}
		}
		return false;
	}

	@Override
	public boolean modify() {
		if (mAnnot == null) {
			return false;
		}
		Markup annot = (Markup) mAnnot;
		try {
			if (mAnnot.getType() != mUndoItem.mType) {
				return false;
			}
			if (mUndoItem.mModifiedDate != null) {
				annot.setModifiedDateTime(mUndoItem.mModifiedDate);
			}
			if (!useOldValue) {
				annot.setBorderColor(mUndoItem.mColor);
				annot.setOpacity(mUndoItem.mOpacity);
				BorderInfo borderInfo = new BorderInfo();
				borderInfo.setWidth(mUndoItem.mLineWidth);
				annot.setBorderInfo(borderInfo);
				if (mUndoItem.mContents == null) {
					mUndoItem.mContents = "";
				}
				annot.setContent(mUndoItem.mContents);
			} else {
				annot.setBorderColor(mUndoItem.mOldColor);
				annot.setOpacity(mUndoItem.mOldOpacity);
				BorderInfo borderInfo = new BorderInfo();
				borderInfo.setWidth(mUndoItem.mOldLineWidth);
				annot.setBorderInfo(borderInfo);
				if (mUndoItem.mOldContents == null) {
					mUndoItem.mOldContents = "";
				}
				annot.setContent(mUndoItem.mOldContents);
			}

			annot.resetAppearanceStream();
			return true;
		} catch (PDFException e) {
			if (e.getLastError() == Constants.e_ErrOutOfMemory) {
				mPdfViewCtrl.recoverForOOM();
			}
		}
		return false;
	}

	@Override
	public boolean delete() {
		if (mAnnot == null) {
			return false;
		}

		try {
			if (mAnnot.getType() != mUndoItem.mType) {
				return false;
			}
			((Markup)mAnnot).removeAllReplies();
			PDFPage page = mAnnot.getPage();
			page.removeAnnot(mAnnot);
			return true;
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return false;
	}
}

abstract class DefaultAnnotUndoItem extends AnnotUndoItem {
	DefaultAnnotHandler mAnnotHandler;
	public DefaultAnnotUndoItem(DefaultAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
		mAnnotHandler = annotHandler;
		mPdfViewCtrl = pdfViewCtrl;
	}
}

class DefaultAnnotAddUndoItem extends DefaultAnnotUndoItem {
	public DefaultAnnotAddUndoItem(DefaultAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
		super(annotHandler, pdfViewCtrl);
	}

	@Override
	public boolean undo() {
		DefaultAnnotDeleteUndoItem undoItem = new DefaultAnnotDeleteUndoItem(mAnnotHandler, mPdfViewCtrl);
		undoItem.mNM = mNM;
		undoItem.mPageIndex = mPageIndex;
		try {
			PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
			Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
			if (annot == null) {
				return false;
			}
			if (annot.getType() != mType) {
				return false;
			}
			mAnnotHandler.removeAnnot(annot, undoItem, false, null);
			return true;
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean redo() {
		try {
			PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
			Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(mType, AppUtil.toFxRectF(mBBox)), mType);

			mAnnotHandler.addAnnot(mPageIndex, annot, this, false, null);
			return true;
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return false;
	}
}

class DefaultAnnotModifyUndoItem extends DefaultAnnotUndoItem {


	public DefaultAnnotModifyUndoItem(DefaultAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
		super(annotHandler, pdfViewCtrl);
	}

	@Override
	public boolean undo() {
		return modifyAnnot(true);
	}

	@Override
	public boolean redo() {
		return modifyAnnot(false);
	}

	private boolean modifyAnnot(boolean userOldValue) {
		try {
			PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
			Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
			if (annot == null) {
				return false;
			}
			if (annot.getType() != mType) {
				return false;
			}

			mAnnotHandler.modifyAnnot((Line) annot, this, userOldValue, false, true, null);
			return true;
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return false;
	}
}

class DefaultAnnotDeleteUndoItem extends DefaultAnnotUndoItem {

	public DefaultAnnotDeleteUndoItem(DefaultAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
		super(annotHandler, pdfViewCtrl);
	}

	@Override
	public boolean undo(Event.Callback callback) {
		try {
			PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
			Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(mType, AppUtil.toFxRectF(mBBox)), mType);
			DefaultAnnotAddUndoItem undoItem = new DefaultAnnotAddUndoItem(mAnnotHandler, mPdfViewCtrl);
			undoItem.mNM = mNM;
			undoItem.mPageIndex = mPageIndex;
			undoItem.mAuthor = mAuthor;
			undoItem.mFlags = mFlags;
			undoItem.mSubject = mSubject;
			undoItem.mCreationDate = mCreationDate;
			undoItem.mModifiedDate = mModifiedDate;
			undoItem.mColor = mColor;
			undoItem.mOpacity = mOpacity;
			undoItem.mLineWidth = mLineWidth;
			undoItem.mIntent = mIntent;
			undoItem.mContents = mContents;

			mAnnotHandler.addAnnot(mPageIndex, annot, undoItem, false, callback);
			return true;
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean redo(Event.Callback callback) {
		try {
			PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
			Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
			if (annot == null) {
				return false;
			}
			if (annot.getType() != mType) {
				return false;
			}
			mAnnotHandler.removeAnnot(annot, this, false, callback);
			return true;
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean undo() {
		return undo(null);
	}

	@Override
	public boolean redo() {
		return redo(null);
	}
}