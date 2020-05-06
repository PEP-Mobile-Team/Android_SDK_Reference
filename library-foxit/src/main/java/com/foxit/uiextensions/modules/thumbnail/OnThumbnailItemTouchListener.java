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
package com.foxit.uiextensions.modules.thumbnail;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;

public abstract class OnThumbnailItemTouchListener implements RecyclerView.OnItemTouchListener {
    private final GestureDetectorCompat mGestureDetector;
    private final RecyclerView recyclerView;

    public OnThumbnailItemTouchListener(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        mGestureDetector = new GestureDetectorCompat(recyclerView.getContext(), new ItemTouchHelperGestureListener());
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    private class ItemTouchHelperGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (child != null) {
                RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
                return ((ThumbnailAdapter.ThumbViewHolder) vh).inEditView((int) e.getRawX(), (int) e.getRawY()) || OnThumbnailItemTouchListener.this.onItemClick(vh);
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            View child1 = recyclerView.findChildViewUnder(e1.getX(), e1.getY());
            View child2 = recyclerView.findChildViewUnder(e2.getX(), e2.getY());
            if (child1 == null || child1 != child2) {
                return false;
            }
            RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child1);
            int verticalMinDistance = 20;
            int minVelocity = 0;
            if (e1.getX() - e2.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {
                return OnThumbnailItemTouchListener.this.onToLeftFling(vh);
            } else if (e2.getX() - e1.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {
                return OnThumbnailItemTouchListener.this.onToRightFling(vh);
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (child != null) {
                RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
                OnThumbnailItemTouchListener.this.onLongPress(vh);
            }
        }
    }

    abstract public void onLongPress(RecyclerView.ViewHolder vh);

    abstract public boolean onItemClick(RecyclerView.ViewHolder vh);

    abstract boolean onToRightFling(RecyclerView.ViewHolder vh);

    abstract boolean onToLeftFling(RecyclerView.ViewHolder vh);
}
