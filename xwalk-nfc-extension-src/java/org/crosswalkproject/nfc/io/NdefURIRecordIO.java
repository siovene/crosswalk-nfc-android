package org.crosswalkproject.nfc;

import android.nfc.NdefRecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class NdefURIRecordIO implements INdefRecordIO {
    public JsonElement read(NdefRecord record) {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(NdefRecord.class, new NdefURIRecordSerializer());
        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();
        return gson.toJsonTree(record);
    }

    public NdefRecord write(String jsonRecord) {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(NdefRecord.class, new NdefURIRecordDeserializer());
        final Gson gson = gsonBuilder.create();
        return gson.fromJson(jsonRecord, NdefRecord.class);
    }
}
