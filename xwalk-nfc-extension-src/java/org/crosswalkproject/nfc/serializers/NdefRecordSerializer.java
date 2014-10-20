package org.crosswalkproject.nfc;

import java.lang.reflect.Type;

import android.nfc.NdefRecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

public class NdefRecordSerializer implements JsonSerializer {
    @Override
    public JsonElement serialize(
        final Object obj, final Type typeOfSrc,
        final JsonSerializationContext context)
    {
        final JsonObject jsonObject = new JsonObject();
        final NdefRecord record = (NdefRecord) obj;

        jsonObject.addProperty("id", new String(record.getId()));
        jsonObject.addProperty("payload", new String(record.getPayload()));
        jsonObject.addProperty("tnf", record.getTnf());
        jsonObject.addProperty("type", new String(record.getType()));

        return jsonObject;
    }
}
