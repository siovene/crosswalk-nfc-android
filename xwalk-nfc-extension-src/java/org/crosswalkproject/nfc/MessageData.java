package org.crosswalkproject.nfc;

import java.io.UnsupportedEncodingException;
import android.nfc.NdefRecord;

public class MessageData extends DataObject {
    public String contentType;
    public String text;

    private static String payloadAsText(byte[] payload)
        throws UnsupportedEncodingException
    {
        int languageCodeLength = payload[0] & 63;
        String encoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
        return new String(
            payload, languageCodeLength + 1,
            payload.length - languageCodeLength - 1, encoding);
    }

    public final static MessageData fromNdefRecord(NdefRecord record)
    {
        MessageData md = new MessageData();

        switch (record.getTnf()) {
            case NdefRecord.TNF_EMPTY:
                // Do nothing
                break;

            case NdefRecord.TNF_WELL_KNOWN:
                String type = new String(record.getType());
                if (type.toLowerCase().equals("t")) {
                    md.contentType = "text";
                    try {
                        md.text = MessageData.payloadAsText(record.getPayload());
                    } catch (UnsupportedEncodingException e) {
                        md.text = "*** Unable to decode text ***";
                    }
                }

                break;
        }

        return md;
    }
}
