package org.crosswalkproject.nfc;

import com.google.gson.Gson;

public class MessageEvent extends DataObject {
    public String scope;
    public MessageData[] messageData;

    public MessageEvent() {
        super();
    }

    public MessageEvent(String uuid) {
        super(uuid);
    }
}
