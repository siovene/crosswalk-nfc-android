package org.crosswalkproject.nfc;

import com.google.gson.Gson;

public class Watch extends DataObject {
  public String scope;

  public static Watch fromJson(String json) {
      Watch w = new Gson().fromJson(json, Watch.class);
      if (w.scope == null) {
          w.scope = new String();
      }
      return w;
  }
}
