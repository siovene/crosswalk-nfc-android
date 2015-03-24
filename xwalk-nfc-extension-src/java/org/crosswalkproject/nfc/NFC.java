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
    private Map<String, NdefRecord> ndefRecordMap = new HashMap<String, NdefRecord>();

    // At the moment we simply deal with one tag at a time
    private Tag lastSeenTag = null;

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
                    Log.d(NFC_DEBUG_TAG, "Tag has matching scope: " + w.scope);;

                    ReadEvent readEvent = new ReadEvent(w.uuid);
                    readEvent.scope = w.scope;
                    readEvent.recordData = new RecordData[records.length];

                    Log.d(NFC_DEBUG_TAG, "Tag has # records: " + records.length);


                    for (int i = 0; i < records.length; i++) {
                        readEvent.recordData[i] = new RecordData(records[i]);
                        Log.d(NFC_DEBUG_TAG, "RecordData's payload: " + bytesToString(readEvent.recordData[i].payload));
                    }

                    InternalProtocolMessage response = InternalProtocolMessage.build(
                            request.id,
                            "nfc_tag_discovered",
                            readEvent.toJson(),
                            true);

                    this.lastSeenTag = tag;
                    postMessage(instanceId, response.toJson());
                    Log.d(NFC_DEBUG_TAG, "Posting mesage: " + response.toJson());
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
    // UTIL
    // ------------------------------------------------------------------------

    private static String bytesToString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length);
        Log.d(NFC_DEBUG_TAG, "Found n bytes: " + data.length);
        for (int i = 0; i < data.length; i++) {
            if (data[i] < 0)
                throw new IllegalArgumentException();
            sb.append((char) data[i]);
            Log.d(NFC_DEBUG_TAG, "Appending char: " + (char) data[i]);
        }
        return sb.toString();
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
        Log.d(NFC_DEBUG_TAG, "watch request.args: " + request.args);
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

        Log.d(NFC_DEBUG_TAG, "Adding ipm with args: " + watchFromRequest.toJson());

        InternalProtocolMessage ipm = InternalProtocolMessage.ok(
                request.id, watchFromRequest.toJson());
        instanceMessages.add(ipm);
        return ipm;
    }

    public InternalProtocolMessage nfc_request_clear_watch(
        int instance, InternalProtocolMessage request)
    {
        Log.d(NFC_DEBUG_TAG, "clear_watch request.args: " + request.args);
        Watch watchFromRequest = Watch.fromJson(request.args);
        ArrayList<InternalProtocolMessage> instanceMessages;

        Log.d(NFC_DEBUG_TAG, "Should clear watch " + watchFromRequest.uuid);

        instanceMessages = nfcWatches.get(instance);
        if (instanceMessages == null) {
            // Definitely nothing to clear.
            Log.d(NFC_DEBUG_TAG, "Nothing to clear.");
            return InternalProtocolMessage.fail(
                request.id, "nfc_response_fail");
        }

        Log.d(NFC_DEBUG_TAG, "Iterating watches...");
        Iterator <InternalProtocolMessage> it = instanceMessages.iterator();
        while(it.hasNext()) {
            InternalProtocolMessage ipm = it.next();
            Log.d(NFC_DEBUG_TAG, "ipm.args " + ipm.args);
            Watch w = Watch.fromJson(ipm.args);
            Log.d(NFC_DEBUG_TAG, "Watch " + w.uuid);
            if (w.uuid.equals(watchFromRequest.uuid)) {
                Log.d(NFC_DEBUG_TAG, "Removing watch...");
                it.remove();
                return InternalProtocolMessage.ok(request.id, w.toJson());
            }
        }

        Log.d(NFC_DEBUG_TAG, "Watch not found...");
        return InternalProtocolMessage.fail(request.id, "nfc_response_fail");
    }

    public InternalProtocolMessage nfc_request_write(
        int instance, InternalProtocolMessage request)
    {
        Log.d(NFC_DEBUG_TAG, "write args: " + request.args);

        if (this.lastSeenTag == null) {
            Log.d(NFC_DEBUG_TAG, "We never saw a tag...");
            return InternalProtocolMessage.fail(request.id, "nfc_response_tag_lost");
        }

        JsonParser parser = new JsonParser();
        JsonElement data = parser.parse(request.args);
        JsonArray jsonRecords = data.getAsJsonObject().getAsJsonArray("records");
        NdefRecord[] records = new NdefRecord[jsonRecords.size()];

        for (int i = 0; i < jsonRecords.size(); i++) {
            NdefRecord record = null;
            JsonObject jsonRecord = jsonRecords.get(i).getAsJsonObject();

            // Prepare the type
            JsonArray typeJsonArray = jsonRecord.get("type").getAsJsonArray();
            byte[] typeBytes = new byte[typeJsonArray.size()];
            for (int j = 0; j < typeJsonArray.size(); j++) {
                typeBytes[i] = typeJsonArray.get(j).getAsByte();
            }
            String type = bytesToString(typeBytes);
            Log.d(NFC_DEBUG_TAG, "Got type: " + type);

            // Prepare the payload
            JsonArray payloadJsonArray = jsonRecord.get("encodedPayload").getAsJsonArray();
            byte[] payloadBytes = new byte[payloadJsonArray.size()];
            for (int j = 0; j < payloadJsonArray.size(); j++) {
                payloadBytes[j] = payloadJsonArray.get(j).getAsByte();
                Log.d(NFC_DEBUG_TAG, "Got payload byte: " + payloadBytes[j]);
            }

            if (type.equals("T")) {
                Log.d(NFC_DEBUG_TAG, "Creating text record with payload: " + bytesToString(payloadBytes));
                record = new NdefRecord(
                    NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_TEXT,
                    new byte[0], // TODO: scope
                    payloadBytes);
            }

            records[i] = record;
        }

        if (jsonRecords.size() == 0) {
            Log.d(NFC_DEBUG_TAG, "Creating empty record set...");
            records = new NdefRecord[] {
                new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)
            };
        }

        Log.d(NFC_DEBUG_TAG, "Creating message...");
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(this.lastSeenTag);

        try {
            Log.d(NFC_DEBUG_TAG, "Connecting to ndef...");
            ndef.connect();
            if (ndef.isWritable()) {
                Log.d(NFC_DEBUG_TAG, "ndef is writable");
                int size = message.toByteArray().length;
                if (ndef.getMaxSize() < size) {
                    Log.d(NFC_DEBUG_TAG, "message too large");
                    return InternalProtocolMessage.fail(
                        request.id, "nfc_response_message_too_large");
                } else {
                    Log.d(NFC_DEBUG_TAG, "writing message...");
                    ndef.writeNdefMessage(message);
                    ndef.close();
                    Log.d(NFC_DEBUG_TAG, "done");
                }
            } else {
                Log.d(NFC_DEBUG_TAG, "tag write protected");
                return InternalProtocolMessage.fail(
                    request.id, "nfc_response_tag_write_protected");
            }
        } catch (TagLostException e) {
            Log.d(NFC_DEBUG_TAG, "tag lost");
            return InternalProtocolMessage.fail(request.id, "nfc_response_tag_lost");
        } catch (IOException e) {
            Log.d(NFC_DEBUG_TAG, "io error");
            return InternalProtocolMessage.fail(request.id, "nfc_response_io_error");
        } catch (FormatException e) {
            Log.d(NFC_DEBUG_TAG, "response malformed");
            return InternalProtocolMessage.fail(request.id, "nfc_response_malformed");
        }

        Log.d(NFC_DEBUG_TAG, "all done");
        return InternalProtocolMessage.ok(request.id, null);
    }
}
