((n) ->
  class n.nfc.NdefRecordData extends n.nfc.NfcDataObject
    _internal: {}

    constructor: () ->
      switch arguments.length
        when 1
          json = arguments[0]
          @uuid = json.uuid
          @_internal[x] = json[x] for x in ['id', 'payload', 'tnf', 'type']

        when 2
          type = arguments[0]
          content = arguments[1]

          @_internal.id = "" # TODO: scope
          switch type
            when "text"
              lang = 'en' # TODO: support languages?
              encoded = n.nfc._internal.EncDec.stringToBytes lang + content
              encoded.unshift lang.length

              @_internal.tnf = n.nfc._internal.TNFMap.TNF_WELL_KNOWN
              @_internal.type = n.nfc._internal.RTDMap.RTD_TEXT
              @_internal.payload = encoded

    url: ->
      if @_internal.tnf == n.nfc._internal.TNFMap.TNF_WELL_KNOWN &&
         @_internal.type[0] == n.nfc._internal.RTDMap.RTD_URI[0]
        protocols = [
          "http://www.", "https://www.", "http://", "https://", "tel:",
          "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://",
          "sftp://", "smb://", "nfs://", "ftp://", "dav://", "news:",
          "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:", "sips:",
          "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://",
          "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
          "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"
        ]

        p = @_internal.payload
        prefix = protocols[p[0]]
        if !prefix
          prefix = ""

        prefix + n.nfc._internal.EncDec.bytesToString p.slice 1

    json: ->
      try
        JSON.parse @text()
      catch e
        return

    text: ->
      if @_internal.tnf == n.nfc._internal.TNFMap.TNF_WELL_KNOWN &&
         @_internal.type[0] == n.nfc._internal.RTDMap.RTD_TEXT[0]
        p = @_internal.payload
        languageCodeLength = p[0]
        languageCode = p.slice 1, 1 + languageCodeLength
        n.nfc._internal.EncDec.bytesToString p.slice languageCodeLength + 1
)(navigator)
