package org.crosswalkproject.nfc;

import android.nfc.NdefRecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;

public class NdefRecordIO {
    public JsonElement read(NdefRecord record, JsonSerializer serializer) {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(NdefRecord.class, serializer);
        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();

        return gson.toJsonTree(record);
    }

    public NdefRecord write(String jsonRecord, JsonDeserializer deserializer) {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(NdefRecord.class, deserializer);
        final Gson gson = gsonBuilder.create();
        return gson.fromJson(jsonRecord, NdefRecord.class);
    }
}
