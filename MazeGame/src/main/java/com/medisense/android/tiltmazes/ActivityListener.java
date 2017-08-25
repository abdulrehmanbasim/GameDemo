package com.medisense.android.tiltmazes;

import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Created by omer on 8/21/17.
 */

public interface ActivityListener {
    boolean onKeyDown(int keyCode, KeyEvent event);
    boolean onTouchEvent(MotionEvent event);
}
