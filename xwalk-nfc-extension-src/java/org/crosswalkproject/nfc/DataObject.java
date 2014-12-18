package org.crosswalkproject.nfc;

import java.util.UUID;

import com.google.gson.Gson;

public class DataObject {
    public String uuid;

    public DataObject()
    {
        this.uuid = UUID.randomUUID().toString();
    }

    public DataObject(String uuid)
    {
        this.uuid = uuid;
    }

    public String toJson()
    {
        return new Gson().toJson(this);
    }
}
