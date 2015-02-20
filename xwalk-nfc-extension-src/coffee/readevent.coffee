((n) ->
  class n.nfc.NdefReadEvent extends n.nfc.NfcDataObject
    constructor: (json) ->
      @passive = json.passive
      @writeable = json.writeable
      @data = (new n.nfc.NdefRecordData x for x in json.recordData)
)(navigator)
