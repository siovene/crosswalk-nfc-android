package org.crosswalkproject.nfc;

import android.nfc.NdefRecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class NdefTextRecordIO implements INdefRecordIO {
    public JsonElement read(NdefRecord record) {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(NdefRecord.class, new NdefTextRecordSerializer());
        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();
        return gson.toJsonTree(record);
    }
}
