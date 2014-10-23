package org.crosswalkproject.nfc;

import android.nfc.NdefRecord;
import com.google.gson.JsonElement;

public interface INdefRecordIO {
    /**
     * Converts an NdefRecord to a JSONElement representing it.
     */
    public JsonElement read(NdefRecord record);

    /**
     * Creates an NdefRecord from a JSON strong.
     */
    public NdefRecord write(String recordsJson);
}
