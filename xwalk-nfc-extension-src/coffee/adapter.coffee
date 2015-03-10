((n) ->
  class n.nfc.NfcAdapter extends n.nfc.NfcDataObject
    watches: []

    constructor: (@uuid) ->

    watch: (options = {}) ->
      callback = (readEvent) =>
        for watch in @watches
          if watch.scope == readEvent.scope
            @onread? readEvent

      new Promise((resolve, reject) =>
        options.scope ?= ""
        response = n.nfc._internal.utils.sendMessage(
          "nfc_request_watch", options, callback)
        watchJson = JSON.parse response.args
        watch = new n.nfc.NfcWatch(watchJson.scope, watchJson.uuid)
        if response.content == "nfc_response_ok"
          @watches.push watch
          resolve watch.uuid
        else
          reject args
      )
      
    clearWatch: (uuid) ->

    write: (data, scope = "") ->
      new Promise((resolve, reject) ->
        response = n.nfc._internal.utils.sendMessage(
          "nfc_request_write")
        console.log response
      )

    setPushMessage: (data, scope = "") ->

    clearPushMessage: (scope = "") ->
)(navigator)
