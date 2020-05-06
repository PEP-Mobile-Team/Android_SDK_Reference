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

import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.ink.EraserUndoItem;
import com.foxit.uiextensions.annots.ink.InkUndoItem;
import com.foxit.uiextensions.annots.multiselect.MultiSelectUndoItem;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Management for the undo items, handle the events which will affect undo implementation.
 **/
public abstract class AbstractUndo {
	private static final int	HISTORY_COUNT			= 512;
	private static final String HISTORY_CACHE_FILE	= "history.dat";

	ArrayList<IUndoItem> mUndoItemStack;
	ArrayList<IUndoItem> mRedoItemStack;

	AbstractUndo() {
		mUndoItemStack = new ArrayList<IUndoItem>();
		mRedoItemStack = new ArrayList<IUndoItem>();
	}

    void removeFlattenItems(ArrayList<IUndoItem> list, int index, String uniqueID) {
        ArrayList<IUndoItem> flattenList = new ArrayList<>();
		ArrayList<EraserUndoItem> eraserList = new ArrayList<>();
        for (IUndoItem undoItem : list) {
            if (undoItem instanceof AnnotUndoItem) {
                AnnotUndoItem item = (AnnotUndoItem) undoItem;
                if (item instanceof MultiSelectUndoItem) {
                    if (item.mPageIndex == index && ((MultiSelectUndoItem) item).isSelected(uniqueID)) {
                        flattenList.add(item);
                    }
                	continue;
				}
                if (item.mPageIndex == index && item.mNM != null && item.mNM.equals(uniqueID)) {
                    flattenList.add(item);
                }

				if (undoItem instanceof EraserUndoItem){
					EraserUndoItem eraserItem = (EraserUndoItem) undoItem;
					eraserList.add(eraserItem);
				}
            }
        }

		for (EraserUndoItem eraserItem: eraserList){
			ArrayList<InkUndoItem> inkUndoItems = eraserItem.getUndoItems();

			Iterator<InkUndoItem> iterator = inkUndoItems.iterator();
			while (iterator.hasNext()){
				InkUndoItem inkUndoItem = iterator.next();
				if (inkUndoItem.mPageIndex == index
						&& inkUndoItem.mNM != null
						&& inkUndoItem.mNM.equals(uniqueID)){
					iterator.remove();
					if (inkUndoItems.isEmpty()){
						flattenList.add(eraserItem);
					}
				}
			}
		}

		for (IUndoItem undoItem : flattenList) {
			list.remove(undoItem);
		}
        flattenList.clear();
        eraserList.clear();
    }

	void removeInvalidItems(ArrayList<IUndoItem> list, int index){
		ArrayList<IUndoItem> invalidList = new ArrayList<>();
		for(IUndoItem undoItem: list){
			if(undoItem instanceof AnnotUndoItem){
				AnnotUndoItem item = (AnnotUndoItem) undoItem;
				if(item.mPageIndex == index){
					invalidList.add(item);
				} else if (item.mPageIndex > index){
					item.mPageIndex -= 1;
				}
			}
		}
		for(IUndoItem undoItem: invalidList){
			list.remove(undoItem);
		}
		invalidList.clear();
	}

	void updateItemsWithOffset(ArrayList<IUndoItem> list, int index, int offset){
		for(IUndoItem undoItem: list) {
			if(undoItem instanceof AnnotUndoItem) {
				if(((AnnotUndoItem) undoItem).mPageIndex >= index){
					((AnnotUndoItem) undoItem).mPageIndex += offset;
				}
			}
		}
	}

	void updateItems(ArrayList<IUndoItem> list,int index,int dstIndex){
		for(IUndoItem undoItem: list) {
			if(undoItem instanceof AnnotUndoItem) {
				if (index < dstIndex) {
					if (((AnnotUndoItem) undoItem).mPageIndex <= dstIndex && ((AnnotUndoItem) undoItem).mPageIndex > index) {
						((AnnotUndoItem) undoItem).mPageIndex -= 1;
					} else if (((AnnotUndoItem) undoItem).mPageIndex == index) {
						((AnnotUndoItem) undoItem).mPageIndex = dstIndex;
					}

				} else {
					if (((AnnotUndoItem) undoItem).mPageIndex >= dstIndex && ((AnnotUndoItem) undoItem).mPageIndex < index) {
						((AnnotUndoItem) undoItem).mPageIndex +=1;
					} else if (((AnnotUndoItem) undoItem).mPageIndex == index) {
						((AnnotUndoItem) undoItem).mPageIndex = dstIndex;
					}
				}
			}
		}
	}

	public void	addUndoItem(IUndoItem undoItem) {
		undoItemWillAdd(undoItem);

		mUndoItemStack.add(undoItem);
		mRedoItemStack.clear();
		writeHistoryCache();

		undoItemAdded(undoItem);
	}

	public boolean	canUndo() {
		return mUndoItemStack.size() > 0;
	}

	public boolean	canRedo() {
		return mRedoItemStack.size() > 0;
	}

	public void	undo() {
		if (mUndoItemStack.size() == 0 || haveModifyTasks()) return;
		IUndoItem item = mUndoItemStack.get(mUndoItemStack.size() - 1);
		willUndo(item);

		item.undo();
		mUndoItemStack.remove(item);
		mRedoItemStack.add(item);

		undoFinished(item);
	}

	public void	redo() {
		if (mRedoItemStack.size() == 0 || haveModifyTasks()) return;
		IUndoItem item = mRedoItemStack.get(mRedoItemStack.size() - 1);
		willRedo(item);

		item.redo();
		mRedoItemStack.remove(item);
		mUndoItemStack.add(item);

		redoFinished(item);
	}

	public void clearUndoRedo() {
		willClearUndo();

		mUndoItemStack.clear();
		mRedoItemStack.clear();
		deleteHistoryCacheFile();

		clearUndoFinished();
	}

	private void readHistoryCache(File file, ArrayList<IUndoItem> historyStack) {
        if (file.exists()) {
        	try {
        		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        		try {
        			while (true) {
        				IUndoItem item = (IUndoItem)ois.readObject();
        				if (item != null) {
        					historyStack.add(item);
        				} else {
        					break;
        				}
        			}
        		} catch (ClassNotFoundException e) {
        			e.printStackTrace();
        		} catch (EOFException e) {
        			e.printStackTrace();
				}
        		ois.close();
        	} catch (StreamCorruptedException e) {
        		e.printStackTrace();
        	} catch (FileNotFoundException e) {
        		e.printStackTrace();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }
	}

	protected boolean writeHistoryCache() {
		int undoCount = mUndoItemStack.size();
		if (undoCount < HISTORY_COUNT * 2)
			return true;

        int count = 0;
    	ArrayList<IUndoItem> historyStack = new ArrayList<IUndoItem>();
		String filePath = getHistoryCachePath();
        File file = new File(filePath);

        readHistoryCache(file, historyStack);
        if (file.exists()) {
        	boolean ret = file.delete();
			if (!ret) return false;
        }

		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        	count = historyStack.size();
        	for (int i = 0; i < count; i ++) {
	        	IUndoItem item = historyStack.get(i);
        		oos.writeObject(item);
        	}
	        for (int i = 0; i < HISTORY_COUNT; i ++) {
	        	IUndoItem item = mUndoItemStack.get(i);
	        	oos.writeObject(item);
	        }
	        IUndoItem nullItem = null;// TODO: 2016/12/30
			oos.writeObject(nullItem);
	        oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

        for (int i = HISTORY_COUNT - 1; i >= 0; i --) {
        	mUndoItemStack.remove(i);
        }
        return true;
	}

	protected void onDocumentOpened() {
		deleteHistoryCacheFile();
	}

	protected void onDocumentClosed() {
		deleteHistoryCacheFile();
	}

	protected boolean deleteHistoryCacheFile() {
		String filePath = getHistoryCachePath();
		File file = new File(filePath);
		return file.delete();
	}

	protected String getHistoryCachePath() {
		String path = getDiskCacheFolder();
		return path.concat("/" + HISTORY_CACHE_FILE);
	}

	protected abstract String getDiskCacheFolder();
	protected abstract boolean			haveModifyTasks();



	//For Undo&Redo

	ArrayList<IUndoEventListener>			mUndoEventListeners = new ArrayList<IUndoEventListener>();
	public interface IUndoEventListener {
		void		itemWillAdd(DocumentManager dm, IUndoItem item);
		void		itemAdded(DocumentManager dm, IUndoItem item);
		void		willUndo(DocumentManager dm, IUndoItem item);
		void		undoFinished(DocumentManager dm, IUndoItem item);
		void		willRedo(DocumentManager dm, IUndoItem item);
		void		redoFinished(DocumentManager dm, IUndoItem item);
		void		willClearUndo(DocumentManager dm);
		void		clearUndoFinished(DocumentManager dm);
	}

	public void registerUndoEventListener(IUndoEventListener listener) {
		mUndoEventListeners.add(listener);
	}

	public void unregisterUndoEventListener(IUndoEventListener listener) {
		mUndoEventListeners.remove(listener);
	}

	private void undoItemWillAdd(IUndoItem item) {
		for (IUndoEventListener listener : mUndoEventListeners) {
			listener.itemWillAdd((DocumentManager)this, item);
		}
	}

	private void undoItemAdded(IUndoItem item) {
		for (IUndoEventListener listener : mUndoEventListeners) {
			listener.itemAdded((DocumentManager)this, item);
		}
	}

	private void willUndo(IUndoItem item) {
		for (IUndoEventListener listener : mUndoEventListeners) {
			listener.willUndo((DocumentManager)this, item);
		}
	}

	private void undoFinished(IUndoItem item) {
		for (IUndoEventListener listener : mUndoEventListeners) {
			listener.undoFinished((DocumentManager)this, item);
		}
	}

	private void willRedo(IUndoItem item) {
		for (IUndoEventListener listener : mUndoEventListeners) {
			listener.willRedo((DocumentManager)this, item);
		}
	}

	private void redoFinished(IUndoItem item) {
		for (IUndoEventListener listener : mUndoEventListeners) {
			listener.redoFinished((DocumentManager)this, item);
		}
	}

	private void willClearUndo() {
		for (IUndoEventListener listener : mUndoEventListeners) {
			listener.willClearUndo((DocumentManager)this);
		}
	}

	private void clearUndoFinished() {
		for (IUndoEventListener listener : mUndoEventListeners) {
			listener.clearUndoFinished((DocumentManager)this);
		}
	}
}
