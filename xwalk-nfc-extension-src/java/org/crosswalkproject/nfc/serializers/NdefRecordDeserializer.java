package org.crosswalkproject.nfc;

import java.lang.reflect.Type;

import android.nfc.NdefRecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;

public class NdefRecordDeserializer implements JsonDeserializer<NdefRecord> {
    @Override
    public NdefRecord deserialize(
        final JsonElement json, final Type typeOfSrc,
        final JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject obj = json.getAsJsonObject();
        short tnf = obj.get("tnf").getAsShort();
        byte[] type = obj.get("type").getAsString().getBytes();

        byte[] id;

        try {
            id = obj.get("id").getAsString().getBytes();
        } catch (NullPointerException e) {
            id = null;
        }

        byte[] payload = obj.get("payload").getAsString().getBytes();

        final NdefRecord record = new NdefRecord(tnf, type, id, payload);
        return record;
    }
}
