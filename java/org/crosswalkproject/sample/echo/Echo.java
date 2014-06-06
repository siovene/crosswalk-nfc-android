package org.crosswalkproject.sample.echo;

import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

public class Echo extends XWalkExtensionClient {
    // the constructor should have this signature so that Crosswalk
    // can instantiate the extension
    public Echo(String name, String jsApiContent,
                XWalkExtensionContextClient xwalkContext) {
        super(name, jsApiContent, xwalkContext);
    }

    private String echo(String s) {
        return s;
    }

    @Override
    // For asynchronous requests
    public void onMessage(int instanceId, String message) {
        postMessage(instanceId, echo(message));
    }

    @Override
    // For synchronous requests
    public String onSyncMessage(int instanceId, String message) {
        return echo(message);
    }
}

