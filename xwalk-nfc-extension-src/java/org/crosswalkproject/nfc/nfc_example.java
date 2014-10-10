// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.crosswalkproject.nfc;

import android.content.Intent;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

import org.xwalk.app.XWalkRuntimeActivityBase;

public class nfc_example extends XWalkRuntimeActivityBase implements NFCGlobals {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRemoteDebugging(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Passdown the key-up event to runtime view.
        if (getRuntimeView() != null &&
                getRuntimeView().onKeyUp(keyCode, event)) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void didTryLoadRuntimeView(View runtimeView) {
        if (runtimeView != null) {
            setContentView(runtimeView);
            getRuntimeView().loadAppFromUrl("file:///android_asset/www/index.html");
        } else {
            TextView msgText = new TextView(this);
            msgText.setText("Crosswalk failed to initialize.");
            msgText.setTextSize(36);
            msgText.setTextColor(Color.BLACK);
            setContentView(msgText);
        }
    }

    private void enterFullscreen() {
        if (VERSION.SDK_INT >= VERSION_CODES.KITKAT &&
                ((getWindow().getAttributes().flags &
                        WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0)) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public void setIsFullscreen(boolean isFullscreen) {
        if (isFullscreen) {
            enterFullscreen();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
                Log.d(NFC_DEBUG_TAG, "Process NDEF discovered action");
        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
                Log.d(NFC_DEBUG_TAG, "Process TAG discovered action");
        } else  if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
                Log.d(NFC_DEBUG_TAG, "Process TECH discovered action");
        } else {
                Log.d(NFC_DEBUG_TAG, "Ignore action " + intent.getAction());
        }
    }
}
