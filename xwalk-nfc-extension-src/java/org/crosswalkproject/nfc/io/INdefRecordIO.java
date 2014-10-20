package org.crosswalkproject.nfc;

import android.nfc.NdefRecord;
import com.google.gson.JsonElement;

public interface INdefRecordIO {
    /**
     * Reads an NdefRecord and returns a JSONElement representing it.
     */
    public JsonElement read(NdefRecord record);
}
