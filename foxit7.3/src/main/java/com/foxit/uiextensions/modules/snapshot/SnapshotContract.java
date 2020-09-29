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
package com.foxit.uiextensions.modules.snapshot;

import android.graphics.Bitmap;

import com.foxit.uiextensions.modules.snapshot.base.BasePresenter;
import com.foxit.uiextensions.modules.snapshot.base.BaseView;

/**
 * This specifies the contract between the view and the presenter.
 *
 */
public interface SnapshotContract {

    interface View extends BaseView<Presenter> {
        void dismiss();
        void showToast(String content);
        Bitmap getBitmap();
    }

    interface Presenter extends BasePresenter {
        void save();
    }
}
