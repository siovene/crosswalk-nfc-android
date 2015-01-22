package org.crosswalkproject.nfc;

import com.google.gson.Gson;

public class TagRegistration extends DataObject {
  public String scope;

  public static TagRegistration fromJson(String json) {
      return new Gson().fromJson(json, TagRegistration.class);
  }
}
