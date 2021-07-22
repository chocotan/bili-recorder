package moe.chikalar.recorder.struct;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteOrder;

public class StructPacker extends StructOutput {
	private ByteArrayOutputStream bos;
	protected DataOutput dataOutput;

	protected void init(OutputStream outStream, ByteOrder order) {
		if (order == ByteOrder.LITTLE_ENDIAN) {
			dataOutput = new LEDataOutputStream(outStream);
		} else {
			dataOutput = new DataOutputStream(outStream);
		}
	}
	
    public StructPacker(){
        this(new ByteArrayOutputStream(), ByteOrder.BIG_ENDIAN);
    }

    public StructPacker(ByteOrder order){
    	this(new ByteArrayOutputStream(), order);
    }

	public StructPacker(OutputStream os, ByteOrder order){
        init(os, order);
        bos = (ByteArrayOutputStream)os;
	}

    public byte[] pack(Object objectToPack) throws StructException {
        writeObject(objectToPack);
        return bos.toByteArray();
    }

	public void writeBoolean(boolean value) throws IOException {
		dataOutput.writeBoolean(value);
	}

	public void writeByte(byte value) throws IOException {
		dataOutput.writeByte(value);
	}

	public void writeShort(short value) throws IOException {
		dataOutput.writeShort(value);
	}

	public void writeInt(int value) throws IOException {
		dataOutput.writeInt(value);
	}

	public void writeLong(long value) throws IOException {
		dataOutput.writeLong(value);
	}

	public void writeChar(char value) throws IOException {
		dataOutput.writeChar(value);
	}

	public void writeFloat(float value) throws IOException {
		dataOutput.writeFloat(value);
	}

	public void writeDouble(double value) throws IOException {
		dataOutput.writeDouble(value);
	}

	public void writeBooleanArray(boolean buffer[], int len) throws IOException {
		if (len == -1 || len > buffer.length)
			len = buffer.length;
		for (int i = 0; i < len; i++)
			dataOutput.writeBoolean(buffer[i]);
	}

	public void writeByteArray(byte buffer[], int len) throws IOException {
		if (len == 0) {
			return;
		}
		if (len == -1 || len > buffer.length)
			len = buffer.length;
		dataOutput.write(buffer, 0, len);
	}

	public void writeCharArray(char buffer[], int len) throws IOException {
		if (len == -1 || len > buffer.length)
			len = buffer.length;
		for (int i = 0; i < len; i++)
			dataOutput.writeChar(buffer[i]);
	}

	public void writeShortArray(short buffer[], int len) throws IOException {
		if (len == -1 || len > buffer.length)
			len = buffer.length;
		for (int i = 0; i < len; i++)
			dataOutput.writeShort(buffer[i]);
	}

	public void writeIntArray(int buffer[], int len) throws IOException {
		if (len == -1 || len > buffer.length)
			len = buffer.length;
		for (int i = 0; i < len; i++)
			dataOutput.writeInt(buffer[i]);
	}

	public void writeLongArray(long buffer[], int len) throws IOException {
		if (len == -1 || len > buffer.length)
			len = buffer.length;
		for (int i = 0; i < len; i++)
			dataOutput.writeLong(buffer[i]);
	}

	public void writeFloatArray(float buffer[], int len) throws IOException {
		if (len == -1 || len > buffer.length)
			len = buffer.length;
		for (int i = 0; i < len; i++)
			dataOutput.writeFloat(buffer[i]);
	}

	public void writeDoubleArray(double buffer[], int len) throws IOException {
		if (len == -1 || len > buffer.length)
			len = buffer.length;
		for (int i = 0; i < len; i++)
			dataOutput.writeDouble(buffer[i]);
	}

	public void writeObjectArray(Object buffer[], int len) throws IOException,
			IllegalAccessException, InvocationTargetException, StructException {
		if (buffer == null || len == 0)
			return;
		if (len == -1 || len > buffer.length)
			len = buffer.length;
		for (int i = 0; i < len; i++)
			writeObject(buffer[i]);
	}


}