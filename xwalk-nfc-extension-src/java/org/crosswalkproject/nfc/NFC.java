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
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.provider.Settings;
import android.util.Log;

// Java
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Other
import com.google.gson.Gson;
import com.google.gson.*;

public class NFC extends XWalkExtensionClient implements NFCGlobals {
    private class ForegroundDispatcher implements Runnable {
        private volatile NFC nfc;
        private volatile boolean enable;

        public ForegroundDispatcher(NFC nfc, boolean enable) {
            this.nfc = nfc;
            this.enable = enable;
        }

        public void run() {
            if (this.enable) {
                nfc.nfcAdapter.enableForegroundDispatch(
                    nfc.activity, nfc.singleTopPendingIntent, nfc.tagFilters, null);
            } else {
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

    // Data with UUID
    private NfcAdapterFacade nfcAdapterFacade;
    private Map<Integer, ArrayList<InternalProtocolMessage>> nfcWatches = new HashMap<Integer, ArrayList<InternalProtocolMessage>>();
    private Map<String, Tag> nfcTagMap = new HashMap<String, Tag>();
    private Map<String, NdefRecord> ndefRecordMap = new HashMap<String, NdefRecord>();


    // the constructor should have this signature so that Crosswalk
    // can instantiate the extension
    public NFC(String name, String jsApiContent,
                XWalkExtensionContextClient xwalkContext) {
        super(name, jsApiContent, xwalkContext);

        this.xwalkContext = xwalkContext;
        this.androidContext = this.xwalkContext.getContext();
        this.activity = this.xwalkContext.getActivity();
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this.androidContext);
        this.nfcAdapterFacade = new NfcAdapterFacade();

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
    }

    private void detectNfcStateChanges(int state) {
        boolean enabled = (state == NFC_STATE_ON) || (state == NFC_STATE_TURNING_ON);

        if(this.nfcEnabled != enabled) {
            this.nfcEnabled = enabled;
            String nfc_state = enabled ? "nfc_state_on" : "nfc_state_off";

            for (Map.Entry<Integer, InternalProtocolMessage> entry : this.nfcStateChangeSubscribers.entrySet()) {
                Integer instanceId = entry.getKey();
                InternalProtocolMessage request = entry.getValue();

                postMessage(
                    instanceId,
                    InternalProtocolMessage.build(
                        request.id,
                        nfc_state,
                        null,
                        true).toJson());
            }
        }
    }

    private void onTagDiscovered(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        NdefMessage message = ndef.getCachedNdefMessage();
        NdefRecord[] records = message.getRecords();
        byte[] tagId = tag.getId();

        for (Map.Entry<Integer, ArrayList<InternalProtocolMessage>> entry : this.nfcWatches.entrySet()) {
            Integer instanceId = entry.getKey();
            ArrayList <InternalProtocolMessage> instanceMessages = entry.getValue();
            Iterator <InternalProtocolMessage> it = instanceMessages.iterator();

            while(it.hasNext()) {
                InternalProtocolMessage request = it.next();

                Watch w = Watch.fromJson(request.args);

                if (w.scope == null || w.scope.equals("") || w.scope.equals(tagId)) {
                    ReadEvent readEvent = new ReadEvent(w.uuid);
                    readEvent.scope = w.scope;
                    readEvent.recordData = new RecordData[records.length];

                    for (int i = 0; i < records.length; i++) {
                        readEvent.recordData[i] = new RecordData(records[i]);
                    }

                    InternalProtocolMessage response = InternalProtocolMessage.build(
                            request.id,
                            "nfc_tag_discovered",
                            readEvent.toJson(),
                            true);

                    postMessage(instanceId, response.toJson());
                }
            }
        }
    }

    private String runAction(int instance, String request)
    {
        InternalProtocolMessage response =
            new ActionRunner(this).run(instance, request);
        return gson.toJson(response);
    }

    @Override
    public void onMessage(int instanceId, String message)
    {
        postMessage(instanceId, this.runAction(instanceId, message));
    }

    @Override
    public String onSyncMessage(int instanceId, String message)
    {
        return this.runAction(instanceId, message);
    }

    @Override
    public void onResume()
    {
        this.activity.runOnUiThread(new ForegroundDispatcher(this, true));
    }

    @Override
    public void onPause()
    {
        this.activity.runOnUiThread(new ForegroundDispatcher(this, false));
    }

    public void onNewIntent(Intent intent)
    {
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            onTagDiscovered(tag);
        }
    }


    // ------------------------------------------------------------------------
    // API
    // ------------------------------------------------------------------------

    public InternalProtocolMessage nfc_request_find_adapters(
        int instance, InternalProtocolMessage request)
    {
        NfcAdapterFacade[] adapters = {this.nfcAdapterFacade};
        InternalProtocolMessage ipm = InternalProtocolMessage.ok(
                request.id, this.gson.toJson(adapters));
        return ipm;
    }

    public InternalProtocolMessage nfc_request_watch(
        int instance, InternalProtocolMessage request)
    {
        Watch watchFromRequest = Watch.fromJson(request.args);

        // Only allow one watch per scope, from the same instance.
        ArrayList<InternalProtocolMessage> instanceMessages;

        instanceMessages = nfcWatches.get(instance);
        if (instanceMessages != null) {
            Iterator <InternalProtocolMessage> it = instanceMessages.iterator();
            while(it.hasNext()) {
                InternalProtocolMessage ipm = it.next();
                Watch w = Watch.fromJson(ipm.args);
                if (w.scope.equals(watchFromRequest.scope)) {
                    return InternalProtocolMessage.ok(request.id, w.toJson());
                }
            }
        } else {
            instanceMessages = new ArrayList<InternalProtocolMessage>();
            this.nfcWatches.put(instance, instanceMessages);
        }


        InternalProtocolMessage ipm = InternalProtocolMessage.ok(
                request.id, watchFromRequest.toJson());
        instanceMessages.add(request);
        return ipm;
    }
}
