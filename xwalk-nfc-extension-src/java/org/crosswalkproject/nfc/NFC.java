package org.crosswalkproject.nfc;

// xwalk
import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

// Android
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.util.Log;

// Java
import java.util.HashMap;
import java.util.Map;

// Other
import com.google.gson.Gson;

public class NFC extends XWalkExtensionClient implements NFCGlobals {
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

    private class ForegroundDispatcher implements Runnable {
        private volatile NFC nfc;
        private volatile boolean enable;

        public ForegroundDispatcher(NFC nfc, boolean enable) {
            this.nfc = nfc;
            this.enable = enable;
        }

        public void run() {
            if (this.enable) {
                Log.d(NFC_DEBUG_TAG, "Enabling foreground dispatch...");
                nfc.nfcAdapter.enableForegroundDispatch(
                    nfc.activity, nfc.singleTopPendingIntent, nfc.tagFilters, null);
            } else {
                Log.d(NFC_DEBUG_TAG, "Disabling foreground dispatch...");
                nfc.nfcAdapter.disableForegroundDispatch(nfc.activity);
            }
        }
    }

    private static final String NFC_EXTRA_ADAPTER_STATE = "android.nfc.extra.ADAPTER_STATE";
    private static final int NFC_STATE_OFF = 1;
    private static final int NFC_STATE_TURNING_ON = 2;
    private static final int NFC_STATE_ON = 3;
    private static final int NFC_STATE_TURNING_OFF = 4;

    private Gson gson = new Gson();
    private XWalkExtensionContextClient xwalkContext;
    private Context androidContext;
    private Activity activity;
    private NfcAdapter nfcAdapter;
    private boolean nfcEnabled;

    private PendingIntent singleTopPendingIntent;
    private IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
    private IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
    private IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
    private IntentFilter[] tagFilters = new IntentFilter[] {
        this.tagDetected,
        this.ndefDetected,
        this.techDetected
    };

    // State changed
    private BroadcastReceiver nfcStateChangeBroadcastReceiver;
    private static IntentFilter nfcStateChangeIntentFilter = new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED");
    private Map<Integer, InternalProtocolMessage> nfcStateChangeSubscribers = new HashMap<Integer, InternalProtocolMessage>();

    // Ndef discovered
    private static IntentFilter nfcNdefDiscoveredIntentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

    // Ndef subscribers
    private Map<Integer, InternalProtocolMessage> nfcTagDiscoveredSubscribers = new HashMap<Integer, InternalProtocolMessage>();


    // the constructor should have this signature so that Crosswalk
    // can instantiate the extension
    public NFC(String name, String jsApiContent,
                XWalkExtensionContextClient xwalkContext) {
        super(name, jsApiContent, xwalkContext);

        this.xwalkContext = xwalkContext;
        this.androidContext = this.xwalkContext.getContext();
        this.activity = this.xwalkContext.getActivity();
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this.androidContext);

        this.nfcStateChangeBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(NFC_EXTRA_ADAPTER_STATE, -1);
                detectNfcStateChanges(state);
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
        Intent intent = new Intent(this.activity, this.activity.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.singleTopPendingIntent = PendingIntent.getActivity(this.activity, 0, intent, 0);
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

    private void processIntent() {
        Log.d(NFC_DEBUG_TAG, "Processing intent...");

        Intent intent = this.activity.getIntent();
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

    @Override
    public void onResume() {
        Log.d(NFC_DEBUG_TAG, "Enabling foreground dispatch...");
        this.activity.runOnUiThread(new ForegroundDispatcher(this, true));
    }

    @Override
    public void onPause() {
        Log.d(NFC_DEBUG_TAG, "Disabling foreground dispatch...");
        this.activity.runOnUiThread(new ForegroundDispatcher(this, false));
    }

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

