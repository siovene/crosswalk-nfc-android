package org.crosswalkproject.nfc;

// xwalk
import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

// Android
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.util.Log;

// Other
import com.google.gson.Gson;


public class NFC extends XWalkExtensionClient {
    private Gson gson = new Gson();
    private Context androidContext;
    private NfcAdapter nfcAdapter;

    private class InternalProtocolMessage {
        private String action;
        private String eventHandler;
    }

    // the constructor should have this signature so that Crosswalk
    // can instantiate the extension
    public NFC(String name, String jsApiContent,
                XWalkExtensionContextClient xwalkContext) {
        super(name, jsApiContent, xwalkContext);

        this.androidContext = xwalkContext.getContext();
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this.androidContext);
    }

    private String powerOnOff() {
        this.androidContext.startActivity(new Intent(
                Settings.ACTION_WIRELESS_SETTINGS));
        return "NFC settings window showed.";
    }

    private String powerStatus() {
        boolean enabled = this.nfcAdapter.isEnabled();
        if(enabled)
            return "ON";
        return "OFF";
    }

    private String subscribe(InternalProtocolMessage ipm) {
        if(ipm.event.equals("onpoweron")) {

        }
    }

    private String runAction(String message) {
        InternalProtocolMessage ipm = gson.fromJson(
            message, InternalProtocolMessage.class);

        if(ipm.action.equals("powerOn") || ipm.action.equals("powerOff")) {
            return this.powerOnOff();
        }

        if(ipm.action.equals("powerStatus")) {
            return this.powerStatus();
        }

        if(ipm.action.equals("subscribe")) {
            return this.subscribe(ipm);
        }

        return "Invalid action.";
    }

    @Override
    public void onMessage(int instanceId, String message) {
        postMessage(instanceId, this.runAction(message));
    }

    @Override
    public String onSyncMessage(int instanceId, String message) {
        return this.runAction(message);
    }
}

