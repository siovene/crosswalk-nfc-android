((n) ->
  class n.nfc.NfcWatch extends n.nfc.NfcDataObject
    constructor: (@scope, @uuid) ->
      if @scope == undefined
        @scope = ""
)(navigator)
