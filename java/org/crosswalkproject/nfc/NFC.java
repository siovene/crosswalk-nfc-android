package org.crosswalkproject.nfc;

import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

import android.util.Log;
import com.google.gson.Gson;


public class NFC extends XWalkExtensionClient {
    private Gson gson = new Gson();
    private class InternalProtocolMessage {
        private String action;
    }

    // the constructor should have this signature so that Crosswalk
    // can instantiate the extension
    public NFC(String name, String jsApiContent,
                XWalkExtensionContextClient xwalkContext) {
        super(name, jsApiContent, xwalkContext);
    }

    private String test() {
        return "Test works";
    }

    private String runAction(String message) {
        InternalProtocolMessage ipm = gson.fromJson(
            message, InternalProtocolMessage.class);

        if(ipm.action.equals("test")) {
            return test();
        }

        return "Error: invalid action.";
    }

    @Override
    public void onMessage(int instanceId, String message) {
        postMessage(instanceId, runAction(message));
    }

    @Override
    public String onSyncMessage(int instanceId, String message) {
        return runAction(message);
    }
}

