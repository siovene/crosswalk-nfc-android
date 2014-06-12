package org.crosswalkproject.nfc;

import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

public class NFC extends XWalkExtensionClient {
    // the constructor should have this signature so that Crosswalk
    // can instantiate the extension
    public NFC(String name, String jsApiContent,
                XWalkExtensionContextClient xwalkContext) {
        super(name, jsApiContent, xwalkContext);
    }

    @Override
    public void onMessage(int instanceId, String message) {
        postMessage(instanceId, "");
    }

    @Override
    public String onSyncMessage(int instanceId, String message) {
        return "";
    }
}

