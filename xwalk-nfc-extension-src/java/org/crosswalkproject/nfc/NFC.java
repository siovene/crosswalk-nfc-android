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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Other
import com.google.gson.Gson;
import com.google.gson.*;

public class NFC extends XWalkExtensionClient implements NFCGlobals {
    private class InternalProtocolMessage {
        private String id;
        private String content;
        private String args;
        private boolean persistent;

        public InternalProtocolMessage(String id, String content, String args, boolean persistent) {
            this.id = id;
            this.content = content;
            this.args = args;
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

    private String buildIPM(String id, String content, String args, boolean persistent) {
        InternalProtocolMessage msg = new InternalProtocolMessage(id, content, args, persistent);
        return gson.toJson(msg);
    }

    private String failIPM(String id, String msg) {
        return buildIPM(id, "nfc_status_fail", msg, false);
    }

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

                postMessage(instanceId, buildIPM(request.id, nfc_state, null, true));
            }
        }
    }

    private void onTagDiscovered(Tag tag) {
        for (Map.Entry<Integer, InternalProtocolMessage> entry : this.nfcTagDiscoveredSubscribers.entrySet()) {
            Integer instanceId = entry.getKey();
            InternalProtocolMessage request = entry.getValue();

            String uuid = new String(tag.getId());
            if (uuid.isEmpty())
                uuid = UUID.randomUUID().toString();
            nfcTagMap.put(uuid, tag);

            postMessage(instanceId, buildIPM(request.id, "nfc_tag_discovered", uuid, true));
        }
    }


    // ------------------------------------------------------------------------
    // API
    // ------------------------------------------------------------------------

    private String powerOnOff(InternalProtocolMessage request) {
        this.androidContext.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        return buildIPM(request.id, "nfc_settings_shown", null, false);
    }

    private String powerState(InternalProtocolMessage request) {
        boolean enabled = this.nfcAdapter.isEnabled();
        String responseContent = "off";
        if(enabled)
            responseContent = "on";

        return buildIPM(request.id, responseContent, null, false);
    }

    private String subscribePowerStateChanged(int instanceId, InternalProtocolMessage request) {
        this.nfcStateChangeSubscribers.put((Integer) instanceId, request);
        return buildIPM(request.id, "nfc_status_ok", null, false);
    }

    private String unsubscribePowerStateChanged(int instanceId, InternalProtocolMessage request) {
        this.nfcStateChangeSubscribers.remove((Integer) instanceId);
        return buildIPM(request.id, "nfc_status_ok", null, false);
    }

    private String subscribeTagDiscovered(int instanceId, InternalProtocolMessage request) {
        this.nfcTagDiscoveredSubscribers.put((Integer) instanceId, request);
        return buildIPM(request.id, "nfc_status_ok", null, true);
    }

    private String tagReadNDEF(int instanceId, InternalProtocolMessage request) {
        String uuid = request.args;
        Tag tag = nfcTagMap.get(uuid);
        Ndef ndef = Ndef.get(tag);
        JsonObject jsonMessage = new JsonObject();

        if (ndef == null) {
            Log.d(NFC_DEBUG_TAG, "NDEF is not supported by this Tag");
            return buildIPM(request.id, "nfc_status_error", "nfc_ndef_not_supported", false);
        }

        NdefMessage message = ndef.getCachedNdefMessage();
        NdefRecord[] records = message.getRecords();
        JsonArray jsonRecords = new JsonArray();

        for (int i = 0; i < records.length; i++) {
            NdefRecord record = records[i];
            ndefRecordMap.put(uuid, record);

            JsonObject jsonRecord = null;

            // TODO: perform NdefMessage IO.
            jsonMessage = new JsonObject();

            switch (record.getTnf()) {
            case NdefRecord.TNF_EMPTY:
                break;

            case NdefRecord.TNF_WELL_KNOWN:
                String type = new String(record.getType());
                if (type.toLowerCase().equals("t")) {
                    jsonRecord = new NdefRecordIO().read(record, new NdefTextRecordSerializer()).getAsJsonObject();
                } else if (type.toLowerCase().equals("u")) {
                    jsonRecord = new NdefRecordIO().read(record, new NdefURIRecordSerializer()).getAsJsonObject();
                }
                break;

            case NdefRecord.TNF_MIME_MEDIA:
                jsonRecord = new NdefRecordIO().read(record, new NdefMediaRecordSerializer()).getAsJsonObject();
                break;

            case NdefRecord.TNF_ABSOLUTE_URI:
                jsonRecord = new NdefRecordIO().read(record, new NdefAbsoluteURIRecordSerializer()).getAsJsonObject();
                break;

            case NdefRecord.TNF_EXTERNAL_TYPE:
                jsonRecord = new NdefRecordIO().read(record, new NdefExternalRecordSerializer()).getAsJsonObject();
                break;

            default:
                jsonRecord = new JsonObject();
            };

            jsonRecord.addProperty("uuid", uuid);
            jsonRecords.add(jsonRecord);
        }

        jsonMessage.add("records", jsonRecords);

        return buildIPM(request.id, "nfc_status_ok", gson.toJson(jsonMessage), false);
    }

    private String tagWriteNDEF(int instanceId, InternalProtocolMessage request) {
        JsonParser parser = new JsonParser();
        JsonElement data = parser.parse(request.args);
        JsonArray jsonRecords = data.getAsJsonObject().getAsJsonArray("records");
        NdefRecord[] records = new NdefRecord[jsonRecords.size()];

        for (int i = 0; i < jsonRecords.size(); i++) {
            JsonObject jsonRecord = jsonRecords.get(i).getAsJsonObject();
            int tnf = jsonRecord.get("tnf").getAsInt();
            String type = jsonRecord.get("type").getAsString();
            NdefRecord record = null;

            switch (tnf) {
            case NdefRecord.TNF_EMPTY:
                // Skip it
                break;

            case NdefRecord.TNF_WELL_KNOWN:
                if (type.toLowerCase().equals("t")) {
                    try {
                        record = new NdefRecordIO().write(gson.toJson(jsonRecord), new NdefTextRecordDeserializer());
                    } catch (JsonParseException e) {
                        return failIPM(request.id, "Invalid JSON");
                    }
                } else if (type.toLowerCase().equals("u")) {
                    try {
                        record = new NdefRecordIO().write(gson.toJson(jsonRecord), new NdefURIRecordDeserializer());
                    } catch (JsonParseException e) {
                        return failIPM(request.id, "Invalid JSON");
                    }
                }
                break;

            case NdefRecord.TNF_MIME_MEDIA:
                record = new NdefRecordIO().write(gson.toJson(jsonRecord), new NdefMediaRecordDeserializer());
                break;

            case NdefRecord.TNF_ABSOLUTE_URI:
                record = new NdefRecordIO().write(gson.toJson(jsonRecord), new NdefAbsoluteURIRecordDeserializer());
                break;

            case NdefRecord.TNF_EXTERNAL_TYPE:
                record = new NdefRecordIO().write(gson.toJson(jsonRecord), new NdefExternalRecordDeserializer());
                break;

            default:
                return failIPM(request.id, "Invalid type");
            }

            records[i] = record;
        }


        NdefMessage message = new NdefMessage(records);
        Tag tag = nfcTagMap.get(data.getAsJsonObject().get("_uuid").getAsString());
        Ndef ndef = Ndef.get(tag);

        try {
            ndef.connect();
            if (ndef.isWritable()) {
                int size = message.toByteArray().length;
                if (ndef.getMaxSize() < size) {
                    return failIPM(
                        request.id,
                        "Message content is too large. " +
                        "Tag capacity: " + ndef.getMaxSize() +
                        ", message size: " + size);
                } else {
                    ndef.writeNdefMessage(message);
                    ndef.close();
                }
            } else {
                return failIPM(request.id, "Tag is read only");
            }
        } catch (TagLostException e) {
            return failIPM(request.id, "Tag lost");
        } catch (IOException e) {
            return failIPM(request.id, "I/O error: " + e.getMessage());
        } catch (FormatException e) {
            return failIPM(request.id, "Malformed message");
        }

        return buildIPM(request.id, "nfc_status_ok", null, false);
    }

    private String ndefRecordGetPayload(int instanceId, InternalProtocolMessage request) {
        String uuid = request.args;
        NdefRecord record = ndefRecordMap.get(uuid);
        String payload;

        try {
            payload = new String(record.getPayload(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return failIPM(request.id, e.toString());
        }

        return buildIPM(request.id, "nfc_status_ok", gson.toJson(payload), false);
    }

    private String runAction(int instanceId, String requestJson) {
        InternalProtocolMessage request = gson.fromJson(requestJson, InternalProtocolMessage.class);
        String response = null;

        if(request.content.equals("nfc_set_power_on") || request.content.equals("nfc_set_power_off")) {
            response = this.powerOnOff(request);
        }

        else if(request.content.equals("nfc_get_power_state")) {
            response = this.powerState(request);
        }

        else if(request.content.equals("nfc_subscribe_power_state_changed")) {
            response = this.subscribePowerStateChanged(instanceId, request);
        }

        else if(request.content.equals("nfc_unsubscribe_power_state_changed")) {
            response = this.unsubscribePowerStateChanged(instanceId, request);
        }

        else if(request.content.equals("nfc_subscribe_tag_discovered")) {
            response = this.subscribeTagDiscovered(instanceId, request);
        }

        else if(request.content.equals("nfc_tag_read_ndef")) {
            response = this.tagReadNDEF(instanceId, request);
        }

        else if(request.content.equals("nfc_tag_write_ndef")) {
            response = this.tagWriteNDEF(instanceId, request);
        }

        else if(request.content.equals("nfc_ndefrecord_get_payload")) {
            response = this.ndefRecordGetPayload(instanceId, request);
        }


        if (response == null) {
            response = buildIPM(
                request.id, "nfc_invalid_action", request.content, false);
        }

        return response;
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
        this.activity.runOnUiThread(new ForegroundDispatcher(this, true));
    }

    @Override
    public void onPause() {
        this.activity.runOnUiThread(new ForegroundDispatcher(this, false));
    }

    public void onNewIntent(Intent intent) {
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            onTagDiscovered(tag);
        }
    }
}
