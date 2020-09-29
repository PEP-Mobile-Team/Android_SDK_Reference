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
package com.foxit.uiextensions.home.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppStorageManager;

import java.io.File;
import java.util.List;

interface IPathCtl {
    void setPath(String path);
    void setPathChangedListener(PathView.pathChangedListener listener);
    View getContentView();

    String getCurPath();
}

class PathItem extends BaseItemImpl {
    public PathItem(Context context, String text, int imgRes) {
        super(context, text, imgRes, RELATION_RIGNT);
        this.setTextSize(AppDisplay.getInstance(context).px2dp(context.getResources().getDimension(R.dimen.ux_text_height_menu)));
        setInterval(6);
        mTextView.setSingleLine(true);
        mTextView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    }

    public TextView getTextView() {
        return mTextView;
    }
}

public class PathView implements IPathCtl {
    public interface pathChangedListener {
        void onPathChanged(String newPath);
    }
    private String mPath;
    private String mParentPath;
    private String mParentText;
    private String mDestText;
    private boolean mIsRoot;

    private LinearLayout mRootLayout;
    private PathItem mParentItem;
    private PathItem mDestItem;
    private Context mContext;
    private pathChangedListener mPathChangedListener;

    public PathView(Context context) {
        mContext = context;
        mRootLayout = new LinearLayout(context);
        mParentItem = new PathItem(context, "pathctl_back", R.drawable.pathctl_back);
        mDestItem = new PathItem(context, "pathctl_dest", R.drawable.pathctl_dest);
        mParentItem.setTextColorResource(R.color.hm_pathclt_parent_selector);
        mRootLayout.setOrientation(LinearLayout.HORIZONTAL);
        mRootLayout.addView(mParentItem.getContentView());
        mRootLayout.addView(mDestItem.getContentView());
        if (!AppDisplay.getInstance(context).isPad()) {
            mRootLayout.setPadding(AppDisplay.getInstance(context).dp2px(16), 0, AppDisplay.getInstance(context).dp2px(6), 0);
        } else {
            mRootLayout.setPadding(AppDisplay.getInstance(context).dp2px(24), 0, 0, 0);
        }
    }

    @Override
    public void setPath(String path) {
        if (path == null) {
            mRootLayout.setVisibility(View.INVISIBLE);
            return;
        } else {
            mRootLayout.setVisibility(View.VISIBLE);
        }
        mPath = path;
        if (checkRoot(path)) {
            mDestText = "";
            mParentPath = path;
            getRootText(path);
        } else {
            analysisPath(path);
        }
        if (mDestText != null) {
            mDestItem.setText(mDestText);
        }
        if (mParentText != null) {
            mParentItem.setText(mParentText);
        }
        if (mDestItem.getText() == null || "".equals(mDestItem.getText())) {
            mDestItem.getContentView().setVisibility(View.INVISIBLE);
        } else {
            mDestItem.getContentView().setVisibility(View.VISIBLE);
        }
        if (mParentItem.getText() == null || "".equals(mParentItem.getText())) {
            mParentItem.getContentView().setVisibility(View.INVISIBLE);
        } else {
            mParentItem.getContentView().setVisibility(View.VISIBLE);
        }
        resetTextMaxWidth();
    }

    @Override
    public void setPathChangedListener(pathChangedListener listener) {
        if (listener == null || mParentItem == null) {
            return;
        }
        mPathChangedListener = listener;
        mParentItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRoot) {
                    mPathChangedListener.onPathChanged(null);
                    mRootLayout.setVisibility(View.INVISIBLE);
                } else {
                    mPathChangedListener.onPathChanged(mParentPath);
                }
            }
        });
    }

    @Override
    public View getContentView() {
        return mRootLayout;
    }

    @Override
    public String getCurPath() {
        return mPath;
    }

    private boolean checkRoot(String Path) {
        List<String> list = AppStorageManager.getInstance(mContext).getVolumePaths();
        if (list.contains(Path)) {
            mIsRoot = true;
            return true;
        } else {
            mIsRoot = false;
            return false;
        }
    }

    private void analysisPath(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String[] hierarchies = path.split(File.separator);
        if (hierarchies != null && hierarchies.length > 2) {
            mParentText = hierarchies[hierarchies.length - 2];
            mDestText = hierarchies[hierarchies.length - 1];
        } else if (hierarchies.length == 2) {
            mParentText = hierarchies[hierarchies.length - 1];
            mDestText = "";
        } else {
            mParentText = "";
            mDestText = "";
        }
        mParentPath = path.substring(0, path.lastIndexOf(File.separator));
    }

    private void getRootText(String path) {
        String[] hierarchies = path.split(File.separator);
        if (hierarchies.length > 0) {
            mParentText = hierarchies[hierarchies.length - 1];
        }
    }

    private void resetTextMaxWidth() {
        if (!AppDisplay.getInstance(mContext).isPad()) {
            mParentItem.getTextView().setMaxWidth(AppDisplay.getInstance(mContext).getRawScreenWidth() / 3);
        } else {
            mParentItem.getTextView().setMaxWidth(AppDisplay.getInstance(mContext).getRawScreenWidth() / 6);
        }
    }
}
