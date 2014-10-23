package org.crosswalkproject.nfc;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import android.nfc.NdefRecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

public class NdefTextRecordSerializer
    extends NdefRecordSerializer
    implements JsonSerializer
{
    private String[] decodePayload(byte[] payload)
        throws UnsupportedEncodingException
    {
        // encoding, languageCode, text
        String[] result = new String[3];

        int languageCodeLength = payload[0] & 63;

        result[0] = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        result[1] = new String(payload, 1, languageCodeLength, "US-ASCII");
        result[2] = new String(
            payload, languageCodeLength + 1,
            payload.length - languageCodeLength - 1, result[0]);

        return result;
    }


    @Override
    public JsonElement serialize(
        final Object obj, final Type typeOfSrc,
        final JsonSerializationContext context)
    {
        NdefRecord record = (NdefRecord) obj;
        JsonObject jsonObject = super.serialize(record, typeOfSrc, context).getAsJsonObject();
        String[] decodedPayload;

        try {
            decodedPayload = decodePayload(record.getPayload());
        } catch (UnsupportedEncodingException e) {
            return jsonObject;
        }

        jsonObject.addProperty("encoding", decodedPayload[0]);
        jsonObject.addProperty("languageCode", decodedPayload[1]);
        jsonObject.addProperty("text", decodedPayload[2]);

        return jsonObject;
    }
}
