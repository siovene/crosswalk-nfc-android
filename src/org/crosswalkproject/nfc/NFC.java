package org.crosswalkproject.nfc;

// xwalk
import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

// Android
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.util.Log;

// Java
import java.util.HashMap;
import java.util.Map;

// Other
import com.google.gson.Gson;


public class NFC extends XWalkExtensionClient {
    private class InternalProtocolMessage {
        private String id;
        private String content;
        private boolean persistent;

        public InternalProtocolMessage(String id, String content, boolean persistent) {
            this.id = id;
            this.content = content;
            this.persistent = persistent;
        }
    }

    private static final String NFC_DEBUG_TAG = "XWALK_NFC";
    private static final String NFC_EXTRA_ADAPTER_STATE = "android.nfc.extra.ADAPTER_STATE";
    private static final int NFC_STATE_OFF = 1;
    private static final int NFC_STATE_TURNING_ON = 2;
    private static final int NFC_STATE_ON = 3;
    private static final int NFC_STATE_TURNING_OFF = 4;

    private Gson gson = new Gson();
    private Context androidContext;
    private NfcAdapter nfcAdapter;
    private boolean nfcEnabled;

    // State changed
    private BroadcastReceiver nfcStateChangeBroadcastReceiver;
    private static IntentFilter nfcStateChangeIntentFilter = new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED");
    private Map<Integer, InternalProtocolMessage> nfcStateChangeSubscribers = new HashMap<Integer, InternalProtocolMessage>();

    // Tag discovered
    private BroadcastReceiver nfcTagDiscoveredBroadcastReceiver;
    private static IntentFilter nfcTagDiscoveredIntentFilter = new IntentFilter("android.nfc.action.ACTION_TAG_DISCOVERED");
    private Map<Integer, InternalProtocolMessage> nfcTagDiscoveredSubscribers = new HashMap<Integer, InternalProtocolMessage>();


    // the constructor should have this signature so that Crosswalk
    // can instantiate the extension
    public NFC(String name, String jsApiContent,
                XWalkExtensionContextClient xwalkContext) {
        super(name, jsApiContent, xwalkContext);

        this.androidContext = xwalkContext.getContext();
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this.androidContext);

        this.nfcStateChangeBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(NFC_EXTRA_ADAPTER_STATE, -1);
                detectNfcStateChanges(state);
            }
        };

        this.nfcTagDiscoveredBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onTagDiscovered();
            }
        };


        detectInitialNfcState();
        startDetectingNfcStateChanges();
        startDetectingTagDiscoveries();
    }


    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    private void startDetectingNfcStateChanges() {
        this.androidContext.registerReceiver(
            nfcStateChangeBroadcastReceiver,
            this.nfcStateChangeIntentFilter);
    }

    private void startDetectingTagDiscoveries() {
        this.androidContext.registerReceiver(
            nfcTagDiscoveredBroadcastReceiver,
            this.nfcTagDiscoveredIntentFilter);
    }


    private void detectInitialNfcState() {
        this.nfcEnabled = this.nfcAdapter.isEnabled();
        Log.d(NFC_DEBUG_TAG, "Initial NFC state: " + (this.nfcEnabled ? "enabled" : "disabled"));
    }

    private void detectNfcStateChanges(int state) {
        Log.d(NFC_DEBUG_TAG, "Detect NFC state changes while previously " + (this.nfcEnabled ? "enabled" : "disabled"));
        boolean enabled = (state == NFC_STATE_ON) || (state == NFC_STATE_TURNING_ON);

        Log.d(NFC_DEBUG_TAG, "this.nfcEnabled = " + this.nfcEnabled + ", enabled = " + enabled);
        if(this.nfcEnabled != enabled) {
            Log.d(NFC_DEBUG_TAG, "NFC state change detected; NFC is now " + (enabled ? "enabled" : "disabled"));
            this.nfcEnabled = enabled;
            Log.d(NFC_DEBUG_TAG, "New value of this.nfcEnabled = " + this.nfcEnabled);
            String nfc_state = enabled ? "nfc_state_on" : "nfc_state_off";

            for (Map.Entry<Integer, InternalProtocolMessage> entry : this.nfcStateChangeSubscribers.entrySet()) {
                Integer instanceId = entry.getKey();
                InternalProtocolMessage request = entry.getValue();

                InternalProtocolMessage response = new InternalProtocolMessage(request.id, nfc_state, true);
                postMessage(instanceId, gson.toJson(response));
                Log.d(NFC_DEBUG_TAG, gson.toJson(response));
            }
        }
    }

    private void onTagDiscovered() {
        Log.d(NFC_DEBUG_TAG, "Tag discovered");

        for (Map.Entry<Integer, InternalProtocolMessage> entry : this.nfcTagDiscoveredSubscribers.entrySet()) {
            Integer instanceId = entry.getKey();
            InternalProtocolMessage request = entry.getValue();

            InternalProtocolMessage response = new InternalProtocolMessage(request.id, "ok", true);
            postMessage(instanceId, gson.toJson(response));
            Log.d(NFC_DEBUG_TAG, gson.toJson(response));
        }
    }


    // ------------------------------------------------------------------------
    // API
    // ------------------------------------------------------------------------

    private String powerOnOff(InternalProtocolMessage request) {
        this.androidContext.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        InternalProtocolMessage response = new InternalProtocolMessage(request.id, "nfc_settings_shown.", false);
        return gson.toJson(response);
    }

    private String powerState(InternalProtocolMessage request) {
        boolean enabled = this.nfcAdapter.isEnabled();
        String responseContent = "off";
        if(enabled)
            responseContent = "on";

        InternalProtocolMessage response = new InternalProtocolMessage(request.id, responseContent, false);
        return gson.toJson(response);
    }

    private String subscribePowerStateChanged(int instanceId, InternalProtocolMessage request) {
        this.nfcStateChangeSubscribers.put((Integer) instanceId, request);
        InternalProtocolMessage response = new InternalProtocolMessage(request.id, "ok", false);
        return gson.toJson(response);
    }

    private String unsubscribePowerStateChanged(int instanceId, InternalProtocolMessage request) {
        this.nfcStateChangeSubscribers.remove((Integer) instanceId);
        InternalProtocolMessage response = new InternalProtocolMessage(request.id, "ok", false);
        return gson.toJson(response);
    }

    private String subscribeTagDiscovered(int instanceId, InternalProtocolMessage request) {
        this.nfcTagDiscoveredSubscribers.put((Integer) instanceId, request);
        InternalProtocolMessage response = new InternalProtocolMessage(request.id, "ok", false);
        return gson.toJson(response);
    }

    private String runAction(int instanceId, String requestJson) {
        InternalProtocolMessage request = gson.fromJson(requestJson, InternalProtocolMessage.class);

        Log.d(NFC_DEBUG_TAG, "Received request to run action: " + request.content);

        if(request.content.equals("nfc_set_power_on") || request.content.equals("nfc_set_power_off")) {
            return this.powerOnOff(request);
        }

        if(request.content.equals("nfc_get_power_state")) {
            return this.powerState(request);
        }

        if(request.content.equals("nfc_subscribe_power_state_changed")) {
            return this.subscribePowerStateChanged(instanceId, request);
        }

        if(request.content.equals("nfc_unsubscribe_power_state_changed")) {
            return this.unsubscribePowerStateChanged(instanceId, request);
        }

        if(request.content.equals("nfc_subscribe_tag_discovered")) {
            return this.subscribeTagDiscovered(instanceId, request);
        }

        InternalProtocolMessage response = new InternalProtocolMessage(request.id, "nfc_invalid_action", false);
        return gson.toJson(response);
    }

    @Override
    public void onMessage(int instanceId, String message) {
        postMessage(instanceId, this.runAction(instanceId, message));
    }

    @Override
    public String onSyncMessage(int instanceId, String message) {
        return this.runAction(instanceId, message);
    }
}

