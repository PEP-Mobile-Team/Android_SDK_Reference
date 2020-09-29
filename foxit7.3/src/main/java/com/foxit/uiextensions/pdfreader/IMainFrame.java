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
package com.foxit.uiextensions.pdfreader;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.modules.panel.IPanelManager;

/**
 * Interface definition for main frame of the complete pdf reader. It provides an entrance to access the UI of reader. For example,
 * getTopToolbar/getBottomToolbar will provide caller the interface for top bar and bottom bar of pdf view control. The SDK user
 * is able to modify or customize the reader UI through these interfaces.
 */
public interface IMainFrame {
    Activity getAttachedActivity();

    void setAttachedActivity(Activity act);

    Context getContext();

    RelativeLayout getContentView();

    void showToolbars();

    void hideToolbars();

    boolean isToolbarsVisible();

    BaseBar getTopToolbar();

    BaseBar getBottomToolbar();

    IPanelManager getPanelManager();

    PropertyBar getPropertyBar();

    IMultiLineBar getSettingBar();

    void hideSettingBar();

    BaseBar getEditDoneBar();

    BaseBar getFormBar();

    MoreTools getMoreToolsBar();

    BaseBar getToolSetBar();

    void showMaskView();

    void hideMaskView();

    boolean isMaskViewShowing();

    void enableTopToolbar(boolean isEnabled);
    void enableBottomToolbar(boolean isEnabled);

    boolean addSubViewToTopBar(View subView, int index, LinearLayout.LayoutParams params);

    boolean removeSubViewFromTopBar(View subView);

    public Animation getTopbarShowAnimation();

    public Animation getBottombarShowAnimation();

    public Animation getTopbarHideAnimation();

    public Animation getBottombarHideAnimation();
}