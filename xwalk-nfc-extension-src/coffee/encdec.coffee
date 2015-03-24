((n) ->
  n.nfc._internal ?= {}
  n.nfc._internal.EncDec =
    bytesToString: `function (bytes) {
      // based on
      // http://ciaranj.blogspot.fr/2007/11/
      // utf8-characters-encoding-in-javascript.html
 
      var result = "";
      var i, c, c1, c2, c3;
      i = c = c1 = c2 = c3 = 0;
 
      // Perform byte-order check.
      if (bytes.length >= 3) {
        if ((bytes[0] & 0xef) == 0xef &&
            (bytes[1] & 0xbb) == 0xbb &&
            (bytes[2] & 0xbf) == 0xbf) {
            // stream has a BOM at the start, skip over
            i = 3;
        }
      }
 
      while (i < bytes.length) {
        c = bytes[i] & 0xff;
 
        if (c < 128) {
          result += String.fromCharCode(c);
          i++;
        } else if ((c > 191) && (c < 224)) {
          if ( i + 1 >= bytes.length ) {
            throw "Un-expected encoding error, " +
                  "UTF-8 stream truncated, or incorrect";
          }
          c2 = bytes[i + 1] & 0xff;
          result += String.fromCharCode( ((c & 31) << 6) | (c2 & 63) );
          i += 2;
        } else {
          if (i + 2 >= bytes.length  || i + 1 >= bytes.length) {
            throw "Un-expected encoding error, " +
                  "UTF-8 stream truncated, or incorrect";
          }
          c2 = bytes[i + 1] & 0xff;
          c3 = bytes[i + 2] & 0xff;
          result += String.fromCharCode(
            ((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
          i += 3;
 
        }
      }
 
      return result;
    }`
    stringToBytes: `function(string) {
      // based on
      // http://ciaranj.blogspot.fr/2007/11/
      // utf8-characters-encoding-in-javascript.html

      var bytes = [];

      for (var n = 0; n < string.length; n++) {
        var c = string.charCodeAt(n);
        if (c < 128) {
          bytes[bytes.length]= c;
        } else if((c > 127) && (c < 2048)) {
          bytes[bytes.length] = (c >> 6) | 192;
          bytes[bytes.length] = (c & 63) | 128;
        } else {
          bytes[bytes.length] = (c >> 12) | 224;
          bytes[bytes.length] = ((c >> 6) & 63) | 128;
          bytes[bytes.length] = (c & 63) | 128;
        }
      }
      return bytes;
    }`
)(navigator)
