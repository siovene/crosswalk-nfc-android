((n) ->
  class n.nfc.NdefRecordData extends n.nfc.NfcDataObject
    _internal: {}

    constructor: (json) ->
      @uuid = json.uuid
      @_internal[x] = json[x] for x in ['id', 'payload', 'tnf', 'type']

    contentType: ""
    url: ->
    arrayBuffer: ->
    blob: ->
    json: ->
    text: ->
      p = @_internal.payload
      languageCodeLength = p[0] & 0x1F # 5 bits
      languageCode = p.slice 1, 1 + languageCodeLength
      n.nfc._internal.EncDec.bytesToString p.slice languageCodeLength + 1
)(navigator)
