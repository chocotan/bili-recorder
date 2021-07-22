package moe.chikalar.recorder.struct;

import java.io.Serializable;

/**
 * FixedString simulates C Strings (char arrays)
 *
 */
@StructClass
public class CString implements Serializable {

	private static final long serialVersionUID = -3393948411351663341L;
    @StructField(order=0)
    private byte[] buffer = null;

    public CString(int len) {
        buffer = new byte[len];
    }

    public CString(String str, int len) {
        buffer = new byte[len];
        copyData(str.getBytes(), len);
    }

	public CString(byte[] data, int len) {
		buffer = new byte[len];
		copyData(data, len);
	}

    public CString(String str, char fillChar, int len) {
        if(str == null) str = "";
        
        buffer = new byte[len];
        
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte)fillChar;
        }
        copyData(str.getBytes(), len);
    }

	public CString(byte[] data, char fillChar, int len) {
		buffer = new byte[len];

		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = (byte)fillChar;
		}
		copyData(data, len);
	}

	private void copyData(byte[] data, int len) {
		if ( data.length < len)
			System.arraycopy(data, 0, buffer, 0, data.length);
		else
			System.arraycopy(data, 0, buffer, 0, len);
	}

    public boolean equals(Object obj) {
        CString str = (CString)obj;

        if ( str.toString().equals(this.toString()) )
            return true;

        return false;
    }

    public void setString(String str) {
        System.arraycopy(str.getBytes(), 0, buffer, 0, str.getBytes().length);
    }

	/**
	 * Returns as Java string. It trims the buffer
	 * @return
	 */
    public String toString() {
        return new String(buffer).trim();
    }

    public String asCString() {
        int i;
        // Find the null
        for (i = 0;(i < buffer.length) && (buffer[i] != 0); i++);
        String str = new String(buffer, 0, i);
        return str;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }
}
