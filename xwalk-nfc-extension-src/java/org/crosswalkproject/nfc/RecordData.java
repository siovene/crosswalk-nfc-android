package org.crosswalkproject.nfc;

import java.io.UnsupportedEncodingException;
import android.nfc.NdefRecord;

/*
 * The RecordData implementation on the Java side is just a dummy wrapper
 * around Android's NdefRecord. All the parsing and interpreting is done
 * in the JavaScript side.
 */
public class RecordData extends DataObject {
    public byte[] id;
    public byte[] payload;
    public short tnf;
    public byte[] type;

    public RecordData (NdefRecord record)
    {
      this.id = record.getId();
      this.payload = record.getPayload();
      this.tnf = record.getTnf();
      this.type = record.getType();
    }
}
