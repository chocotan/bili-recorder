package struct;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

public class JavaStruct {
	
	public static final byte[] pack(Object o) throws struct.StructException {
		return pack(o, ByteOrder.BIG_ENDIAN);
	}

	public static final byte[] pack(Object o, ByteOrder order) throws struct.StructException {
		struct.StructPacker packer = new struct.StructPacker(order);
		return packer.pack(o);
	}

	public static struct.StructPacker getPacker(OutputStream os, ByteOrder order){
		return new struct.StructPacker(os, order);
	}
	
	public static final void unpack(Object o, byte[] buffer) throws struct.StructException {
		unpack(o, buffer, ByteOrder.BIG_ENDIAN);
	}

	public static final void unpack(Object o, byte[] buffer, ByteOrder order) throws struct.StructException {
		StructUnpacker unpacker = new StructUnpacker( buffer, order);
		unpacker.unpack(o);
	}
	
	public static StructUnpacker getUnpacker(InputStream is, ByteOrder order){
		return new StructUnpacker(is, order);
	}
}
