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
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

/**
 * Interface definition for a callback to be invoked when the lifecycle of a UI extension executing.
 */
public interface ILifecycleEventListener {
    /**
     * Should be called in {@link Activity#onCreate(Bundle) } or {@link Fragment#onCreate(Bundle)}
     * @see Activity#onCreate(Bundle)
     * @see Fragment#onCreate(Bundle)
     */
    void onCreate(Activity act, Bundle savedInstanceState);

    /**
     * Should be called in {@link Activity#onStart()} or {@link Fragment#onStart()}
     * @see Activity#onStart()
     * @see Fragment#onStart()
     */
    void onStart(Activity act);

    /**
     * Should be called in {@link Activity#onPause()} or {@link Fragment#onPause()}
     * @see Activity#onPause()
     * @see Fragment#onPause()
     */
    void onPause(Activity act);

    /**
     * Should be called in {@link Activity#onResume()} or {@link Fragment#onResume()}
     * @see Activity#onResume()
     * @see Fragment#onResume()
     */
    void onResume(Activity act);

    /**
     * Should be called in {@link Activity#onStop()} or {@link Fragment#onStop()}
     * @see Activity#onStop()
     * @see Fragment#onStop()
     */
    void onStop(Activity act);

    /**
     * Should be called in {@link Activity#onDestroy()} or {@link Fragment#onDestroy()}
     * @see Activity#onDestroy()
     * @see Fragment#onDestroy()
     */
    void onDestroy(Activity act);

    /**
     * Should be called in {@link Activity#onSaveInstanceState(Bundle)}, {@link Activity#onSaveInstanceState(Bundle, PersistableBundle)}
     * or {@link Fragment#onSaveInstanceState(Bundle)}
     * @see Activity#onSaveInstanceState(Bundle)
     * @see Activity#onSaveInstanceState(Bundle, PersistableBundle)
     * @see Fragment#onSaveInstanceState(Bundle)
     */
    void onSaveInstanceState(Activity act, Bundle bundle);

    /**
     * Should be called in {@link Fragment#onHiddenChanged(boolean)}
     * @see Fragment#onHiddenChanged(boolean)
     */
    void onHiddenChanged(boolean hidden);

    /**
     * Receive and handle result from activity
     *
     * @see Activity#onActivityResult(int, int, Intent)
     * @see Fragment#onActivityResult(int, int, Intent)
     */
    void onActivityResult(Activity act, int requestCode, int resultCode, Intent data);
}
