((n) ->
  class n.nfc.NfcAdapter extends n.nfc.NfcDataObject
    watches: []

    constructor: (@uuid) ->

    watch: (options = {}) ->
      callback = (readEventJson) =>
        for watch in @watches
          if watch.scope == readEventJson.scope
            @onread? (new n.nfc.NdefReadEvent readEventJson)

      new Promise((resolve, reject) =>
        options.scope ?= ""
        response = n.nfc._internal.utils.sendMessage(
          "nfc_request_watch", options, callback)
        watchJson = JSON.parse response.args
        watch = new n.nfc.NfcWatch watchJson.scope, watchJson.uuid
        if response.content == "nfc_response_ok"
          @watches.push watch
          resolve watch.uuid
        else
          reject response.args
      )
      
    clearWatch: (uuid) ->

    write: (records, scope = "") ->
      new Promise((resolve, reject) ->
        response = n.nfc._internal.utils.sendMessage(
          "nfc_request_write", {records: records})
        if response.content == "nfc_response_ok"
          resolve
        else
          reject response.args
      )

    setPushMessage: (data, scope = "") ->

    clearPushMessage: (scope = "") ->
)(navigator)
