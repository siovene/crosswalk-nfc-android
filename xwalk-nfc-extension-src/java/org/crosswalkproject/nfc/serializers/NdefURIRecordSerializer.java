package org.crosswalkproject.nfc;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import android.nfc.NdefRecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

public class NdefURIRecordSerializer
    extends NdefRecordSerializer
    implements JsonSerializer
{
    private String decodePayload(byte[] payload)
        throws UnsupportedEncodingException
    {
        final String[] prefixes = {
            "",
            "http://www.",
            "https://www.",
            "http://",
            "https://",
            "tel:",
            "mailto:",
            "ftp://anonymous:anonymous@",
            "ftp://ftp.",
            "ftps://"
        };

        return prefixes[payload[0]] + new String(payload, 1, payload.length - 1);
    }


    @Override
    public JsonElement serialize(
        final Object obj, final Type typeOfSrc,
        final JsonSerializationContext context)
    {
        NdefRecord record = (NdefRecord) obj;
        JsonObject jsonObject = super.serialize(record, typeOfSrc, context).getAsJsonObject();
        String decodedPayload;

        try {
            decodedPayload = decodePayload(record.getPayload());
        } catch (UnsupportedEncodingException e) {
            return jsonObject;
        }

        jsonObject.addProperty("uri", decodedPayload);

        return jsonObject;
    }
}
