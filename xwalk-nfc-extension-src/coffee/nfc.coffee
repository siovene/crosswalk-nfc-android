((n) ->
  extension.setMessageListener((message) ->
    messageJson = JSON.parse message
    cb = n.nfc._internal.callbacks.functions[messageJson.id]

    if (cb?)
      argsJson = JSON.parse messageJson.args
      cb argsJson
      if (!messageJson.persistent)
        delete n.nfc._internal.callbacks[messageJson.id]
    else
      console.error "Callback not found"
  )

  n.nfc =
    NFC:
      findAdapters: ->
        new Promise((resolve, reject) ->
          response = n.nfc._internal.utils.sendMessage(
            "nfc_request_find_adapters")
          adapters = JSON.parse response.args
          if response.content == "nfc_response_ok"
            resolve (new n.nfc.NfcAdapter(x.uuid) for x in adapters)
          else
            reject()
        )
)(navigator)
