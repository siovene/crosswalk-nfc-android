package org.crosswalkproject.nfc;

import java.lang.reflect.Type;

import android.nfc.NdefRecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

public class NdefExternalRecordSerializer
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

        return jsonObject;
    }
}
