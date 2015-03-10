package org.crosswalkproject.nfc;

import com.google.gson.Gson;

public class ReadEvent extends DataObject {
    public String scope;
    public RecordData[] recordData;

    public ReadEvent() {
        super();
    }

    public ReadEvent(String uuid) {
        super(uuid);
    }
}
