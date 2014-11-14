package org.crosswalkproject.nfc;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import android.nfc.NdefRecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;

public class NdefExternalRecordDeserializer implements JsonDeserializer<NdefRecord> {
    @Override
    public NdefRecord deserialize(
        final JsonElement json, final Type typeOfSrc,
        final JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject obj = json.getAsJsonObject();

        JsonElement typeElement = obj.get("type");
        JsonElement payloadElement = obj.get("payload");
        String type, payload;

        type = typeElement.getAsString();
        payload = payloadElement.getAsString();

        try {
            return new NdefRecord(
                NdefRecord.TNF_EXTERNAL_TYPE, type.getBytes("US-ASCII"),
                null, payload.getBytes());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
