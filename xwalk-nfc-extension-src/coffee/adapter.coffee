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
      new Promise((resolve, reject) =>
        if uuid == null
          do reject

        response = n.nfc._internal.utils.sendMessage(
          "nfc_request_clear_watch", {uuid:uuid})
        if response.content == "nfc_response_ok"
          @watches = (x for x in @watches when x.uuid != uuid)
          do resolve
        else
          do reject
      )

    write: (records, scope = "") ->
      new Promise((resolve, reject) ->
        recordDataList =
          (new n.nfc.NdefRecordData x.type, x.content for x in records)
        requestRecords = (
          {
            type: x._internal.type
            encodedPayload: x._internal.payload
          } for x in recordDataList
        )
        response = n.nfc._internal.utils.sendMessage(
          "nfc_request_write", {records: requestRecords})
        if response.content == "nfc_response_ok"
          do resolve
        else
          reject response.args
      )

    setPushMessage: (data, scope = "") ->

    clearPushMessage: (scope = "") ->
)(navigator)
