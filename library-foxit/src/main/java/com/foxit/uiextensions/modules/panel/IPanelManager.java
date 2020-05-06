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
package com.foxit.uiextensions.modules.panel;

import android.widget.PopupWindow;

import com.foxit.uiextensions.controls.panel.PanelHost;
import com.foxit.uiextensions.controls.panel.PanelSpec;

/**
 * Through this interface you can control the show/hide of the panel and other related operations.
 */
public interface IPanelManager {
    /**
     * Interface used to allow the creator of a panel to run some code when the
     * panel is shown.
     */
    interface OnShowPanelListener{
        /**
         * This method will be invoked when the panel is shown.
         */
        void onShow();
    }

    /**
     * Get the panel host
     * @return The panel host
     */
    PanelHost getPanel();

    /**
     * Get the panel window
     * @return The panel window
     */
    PopupWindow getPanelWindow();

    /**
     * Display the default or the current panel window.
     */
    void showPanel();

    /**
     * Display the panel window by the specified panel type.
     *
     * @param panelType The panel type, should one of {@link PanelSpec.PanelType#ReadingBookmarks},
     *                  {@link PanelSpec.PanelType#Outline}, {@link PanelSpec.PanelType#Annotations}
     *                  {@link PanelSpec.PanelType#Attachments}
     */
    void showPanel(PanelSpec.PanelType panelType);

    /**
     * Dismiss the panel and its window.
     */
    void hidePanel();

    /**
     * Set a panel listener to be invoked when the panel is shown.
     *
     * @param listener The {@link OnShowPanelListener} to use.
     */
    void setOnShowPanelListener(OnShowPanelListener listener);
}
