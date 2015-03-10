((n) ->
  n.nfc._internal ?= {}
  n.nfc._internal.TNFMap =
    TNF_EMPTY: 0x0
    TNF_WELL_KNOWN: 0x01
    TNF_MIME_MEDIA: 0x02
    TNF_ABSOLUTE_URL: 0x03
    TNF_EXTERNAL_TYPE: 0x04
    TNF_UNKNOWN: 0x05
    TNF_UNCHANGED: 0x06
    TNF_RESERVED: 0x07

  n.nfc.TNF = [
    "Empty",
    "Well-known",
    "Media-type",
    "AbsoluteURI",
    "External",
    "Unknown",
    "Unchanged",
    "Reserved"
  ]
)(navigator)
