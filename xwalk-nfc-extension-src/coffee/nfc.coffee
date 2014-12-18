((n) ->
  n.nfc =
    NFC:
      requestTagRegistration: (options = {}) ->
        new Promise((resolve, reject) ->
          try
            callback = (messageEvent) ->
              uuid = messageEvent.uuid
              for registration in n.nfc._internal.tagRegistrations
                if registration.uuid == messageEvent.uuid
                  registration.onmessage messageEvent

            response = n.nfc._internal.utils.sendMessage(
              "nfc_request_tag_registration", options, callback)
            responseJson = JSON.parse response

            if (responseJson.content == "nfc_response_ok")
              registrationJson = JSON.parse responseJson.args
              registration = new n.nfc.NfcTagRegistration registrationJson.uuid
              n.nfc._internal.tagRegistrations.push(registration)
              resolve registration
            else
              reject
          catch e
            reject e
        )

      requestPeerRegistration: ->


  n.nfc._internal =
    tagRegistrations: []


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
)(navigator)
