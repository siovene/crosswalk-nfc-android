package org.crosswalkproject.nfc;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import android.net.Uri;
import android.nfc.NdefRecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;

public class NdefURIRecordDeserializer
    extends NdefRecordDeserializer
    implements JsonDeserializer<NdefRecord>
{
    @Override
    public NdefRecord deserialize(
        final JsonElement json, final Type typeOfSrc,
        final JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject obj = json.getAsJsonObject();
        JsonElement uriElement = obj.get("uri");

        return NdefRecord.createUri(Uri.parse(uriElement.getAsString()));
    }
}
