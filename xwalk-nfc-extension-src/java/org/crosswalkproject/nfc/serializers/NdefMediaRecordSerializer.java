package org.crosswalkproject.nfc;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import android.nfc.NdefRecord;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

public class NdefMediaRecordSerializer
    extends NdefRecordSerializer
    implements JsonSerializer
{
    @Override
    public JsonElement serialize(
        final Object obj, final Type typeOfSrc,
        final JsonSerializationContext context)
    {
        NdefRecord record = (NdefRecord) obj;
        JsonObject jsonObject = super.serialize(record, typeOfSrc, context).getAsJsonObject();

        Log.d("XWALK_NFC", "Payload: " + jsonObject.get("payload").getAsString());

        return jsonObject;
    }
}
