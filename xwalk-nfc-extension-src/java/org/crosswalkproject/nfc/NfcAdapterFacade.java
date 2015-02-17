package org.crosswalkproject.nfc;

import com.google.gson.Gson;

public class NfcAdapterFacade extends DataObject {
  public static NfcAdapterFacade fromJson(String json) {
      return new Gson().fromJson(json, NfcAdapterFacade.class);
  }
}
