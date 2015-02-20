((n) ->
  class n.nfc.NdefRecordData extends n.nfc.NfcDataObject
    _internal: {}

    constructor: (json) ->
      @_internal[x] = json[x] for x in ['id', 'payload', 'tnf', 'type']

    contentType: ""
    url: ->
    arrayBuffer: ->
    blob: ->
    json: ->
    text: ->
)(navigator)
