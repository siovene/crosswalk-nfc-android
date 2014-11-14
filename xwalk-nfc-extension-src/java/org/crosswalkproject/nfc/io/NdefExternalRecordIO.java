package org.crosswalkproject.nfc;

import android.nfc.NdefRecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class NdefExternalRecordIO implements INdefRecordIO {
    public JsonElement read(NdefRecord record) {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(NdefRecord.class, new NdefExternalRecordSerializer());
        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();
        return gson.toJsonTree(record);
    }

    public NdefRecord write(String jsonRecord) {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(NdefRecord.class, new NdefExternalRecordDeserializer());
        final Gson gson = gsonBuilder.create();
        return gson.fromJson(jsonRecord, NdefRecord.class);
    }
}
