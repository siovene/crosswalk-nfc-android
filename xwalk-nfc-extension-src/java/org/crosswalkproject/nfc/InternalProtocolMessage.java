package org.crosswalkproject.nfc;

import com.google.gson.Gson;

public class InternalProtocolMessage {
    public String id;
    public String content;
    public String args;
    public boolean persistent;

    public InternalProtocolMessage(
        String id, String content, String args, boolean persistent)
    {
        this.id = id;
        this.content = content;
        this.args = args;
        this.persistent = persistent;
    }

    public String toJson()
    {
        return new Gson().toJson(this);
    }

    public static InternalProtocolMessage build(
        String id, String content, String args, boolean persistent)
    {
        return new InternalProtocolMessage(id, content, args, persistent);
    }

    public static InternalProtocolMessage fail(String id, String msg)
    {
        return InternalProtocolMessage.build(
                id, "nfc_response_fail", msg, false);
    }

    public static InternalProtocolMessage ok(String id, String msg) {
        return InternalProtocolMessage.build(
                id, "nfc_response_ok", msg, false);
    }
}
