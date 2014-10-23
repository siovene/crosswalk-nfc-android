package org.crosswalkproject.nfc;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import android.nfc.NdefRecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;

public class NdefTextRecordDeserializer
    extends NdefRecordDeserializer
    implements JsonDeserializer<NdefRecord>
{
    private NdefRecord createTextRecord(String languageCode, String text)
    throws UnsupportedEncodingException
    {
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = languageCode.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                                           NdefRecord.RTD_TEXT,
                                           new byte[0],
                                           payload);

        return record;
    }


    @Override
    public NdefRecord deserialize(
        final JsonElement json, final Type typeOfSrc,
        final JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject obj = json.getAsJsonObject();
        JsonElement languageCodeElement = obj.get("languageCode");
        JsonElement textElement = obj.get("text");
        String languageCode, text;

        if (languageCodeElement != null)
            languageCode = languageCodeElement.getAsString();
        else
            languageCode = new String("en");

        if (textElement != null)
            text = textElement.getAsString();
        else
            text = new String("");

        Log.d("XWALK_NFC", "Text is " + text);

        // Unfortunately we can't use NdefRecord.createTextRecord because that
        // is only available since API Level 21.
        try {
            NdefRecord record = createTextRecord(languageCode, text);
            return record;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
