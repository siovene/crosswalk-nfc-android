package org.crosswalkproject.nfc;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import android.nfc.NdefRecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;

public class NdefMediaRecordDeserializer
    extends NdefRecordDeserializer
    implements JsonDeserializer<NdefRecord>
{
    private NdefRecord createMime(String type, String content) throws UnsupportedEncodingException {
        return new NdefRecord(
            NdefRecord.TNF_MIME_MEDIA, type.getBytes("US-ASCII"),
            null, content.getBytes());
    }

    @Override
    public NdefRecord deserialize(
        final JsonElement json, final Type typeOfSrc,
        final JsonDeserializationContext context)
        throws JsonParseException
    {
        JsonObject obj = json.getAsJsonObject();
        JsonElement typeElement = obj.get("type");
        JsonElement contentElement = obj.get("content");
        String type, content;

        type = typeElement.getAsString();
        content = contentElement.getAsString();

        // Unfortunately we can't use NdefRecord.createMime because that
        // is only available since API Level 16.
        try {
            return createMime(type, content);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
