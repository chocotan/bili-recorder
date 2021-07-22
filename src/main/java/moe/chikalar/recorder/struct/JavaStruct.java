package moe.chikalar.recorder.struct;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

public class JavaStruct {
	
	public static final byte[] pack(Object o) throws StructException {
		return pack(o, ByteOrder.BIG_ENDIAN);
	}

	public static final byte[] pack(Object o, ByteOrder order) throws StructException {
		StructPacker packer = new StructPacker(order);
		return packer.pack(o);
	}

	public static StructPacker getPacker(OutputStream os, ByteOrder order){
		return new StructPacker(os, order);
	}
	
	public static final void unpack(Object o, byte[] buffer) throws StructException {
		unpack(o, buffer, ByteOrder.BIG_ENDIAN);
	}

	public static final void unpack(Object o, byte[] buffer, ByteOrder order) throws StructException {
		StructUnpacker unpacker = new StructUnpacker( buffer, order);
		unpacker.unpack(o);
	}
	
	public static StructUnpacker getUnpacker(InputStream is, ByteOrder order){
		return new StructUnpacker(is, order);
	}
}
