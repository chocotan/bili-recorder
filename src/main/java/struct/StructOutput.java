package struct;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class StructOutput extends OutputStream {

	@Override
	public void write(int arg0) throws IOException {
	}
	
	public void writeObject(Object obj) throws struct.StructException {
        if(obj == null) throw new struct.StructException("Struct classes cant be null. ");
        StructData info = StructUtils.getStructInfo(obj);

        boolean lengthedArray = false;
        int arrayLength = 0;

        for (Field currentField : info.getFields()) {
            //System.out.println("Processing field: " + currentField.getName());
            StructFieldData fieldData = info.getFieldData(currentField.getName());
            if(fieldData == null) {
                throw new struct.StructException("Field Data not found for field: " + currentField.getName());
            }
            lengthedArray = false; 
            arrayLength = 0;
            try{
                if(fieldData.isArrayLengthMarker()){
                    if (fieldData.requiresGetterSetter()) {
                        arrayLength = ((Number)fieldData.getGetter().invoke( obj, (Object[])null)).intValue();
                    } else {
                        arrayLength = ((Number)fieldData.getField().get(obj)).intValue();
                    }
                    lengthedArray = true;
                }
                if ( fieldData.requiresGetterSetter()){
                    if(lengthedArray && arrayLength >= 0){
                        writeField(fieldData, fieldData.getGetter(), obj, arrayLength);
                    }
                    else writeField(fieldData, fieldData.getGetter(), obj, -1);
                }
                // Field is public. Access directly.
                else {
                    if(lengthedArray && arrayLength >= 0){
                            writeField(fieldData, null, obj, arrayLength);
                    }
                    // Array is null if Length is negative.
                    else {
                        writeField(fieldData, null, obj, -1);
                    }
                }
            }
            catch (Exception e) {
                throw new struct.StructException(e);
            }
        }
    }
	
	/**
	 * Write a fields value. Field can be an primitive, array or another object.
	 */
	public void writeField( StructFieldData fieldData, Method getter, Object obj, int len )
	            throws IllegalAccessException, IOException, InvocationTargetException, struct.StructException {
		Field field = fieldData.getField();
		if ( !field.getType().isArray() )
		{
			switch(fieldData.getType()) {
			case BOOLEAN: 
				if(getter != null) writeBoolean((Boolean)getter.invoke(obj, (Object[])null));
				else writeBoolean(field.getBoolean(obj));
				break;
				
			case BYTE:
				if(getter != null) writeByte((Byte)getter.invoke(obj, (Object[])null));
				else writeByte(field.getByte(obj));
				break;
				
			case SHORT:
				if(getter != null) writeShort((Short)getter.invoke(obj, (Object[])null));
				else writeShort(field.getShort(obj));
				break;
				
			case INT: 
				if(getter != null) writeInt((Integer)getter.invoke(obj, (Object[])null));
				else writeInt(field.getInt(obj));
				break;
				
			case LONG: 
				long longValue;
				if(getter != null) longValue = (Long)getter.invoke(obj, (Object[])null);
				else longValue = field.getLong(obj);
				writeLong(longValue);
				break;
				
			case CHAR:
				if(getter != null) writeChar((Character)getter.invoke(obj, (Object[])null));
				else writeChar(field.getChar(obj));
				break;
				
			case FLOAT: 
				if(getter != null) writeFloat((Float)getter.invoke(obj, (Object[])null));
				else writeFloat(field.getFloat(obj));
				break;
				
			case DOUBLE:
				if(getter != null) writeDouble((Double)getter.invoke(obj, (Object[])null));
				else writeDouble(field.getDouble(obj));
				break;
				
			// Object	
			default : 
				if(getter != null) handleObject(field, getter.invoke(obj, (Object[])null));
				else handleObject(field, obj);
				break;
			}
		} else {
			switch(fieldData.getType()) {
			case BOOLEAN:
				if(getter != null) writeBooleanArray((boolean[])getter.invoke(obj, (Object[])null), len);
				else writeBooleanArray((boolean[]) field.get(obj), len);
				break;
				
			case BYTE: 
				if(getter != null) writeByteArray((byte[])getter.invoke(obj, (Object[])null), len);
				else writeByteArray((byte[]) field.get(obj), len);
				break;
				
			case CHAR: 
				if(getter != null) writeCharArray((char[])getter.invoke(obj, (Object[])null), len);
				else writeCharArray((char[]) field.get(obj), len);
				break;
				
			case SHORT: 
				if(getter != null) writeShortArray((short[])getter.invoke(obj, (Object[])null), len);
				else writeShortArray((short[]) field.get(obj), len);
				break;
				
			case INT: 
				if(getter != null) writeIntArray((int[])getter.invoke(obj, (Object[])null), len);
				else writeIntArray((int[]) field.get(obj), len);
				break;
				
			case LONG: 
				if(getter != null) writeLongArray((long[])getter.invoke(obj, (Object[])null), len);
				else writeLongArray((long[]) field.get(obj), len);
				break;
				
			case FLOAT: 
				if(getter != null) writeFloatArray((float[])getter.invoke(obj, (Object[])null), len);
				else writeFloatArray((float[]) field.get(obj), len);
				break;
				
			case DOUBLE:
				if(getter != null) writeDoubleArray((double[])getter.invoke(obj, (Object[])null), len);
				else writeDoubleArray((double[]) field.get(obj), len);
				break;
				
			default: 
				if(getter != null) writeObjectArray((Object[])getter.invoke(obj, (Object[])null), len);
				else writeObjectArray((Object[]) field.get(obj), len);
				break;
			}
		}
	}
	
	public void handleObject(Field field, Object obj)
			throws IllegalArgumentException, struct.StructException,
			IllegalAccessException, IOException {
		writeObject(field.get(obj));
	}
	
	public abstract void writeBoolean(boolean value) throws IOException;

	public abstract void writeByte(byte value) throws IOException ;

	public abstract void writeShort(short value) throws IOException;

	public abstract void writeInt(int value) throws IOException;
	
	public abstract void writeLong(long value) throws IOException ;

	public abstract void writeChar(char value) throws IOException ;
	
	public abstract void writeFloat(float value) throws IOException;
	
	public abstract void writeDouble(double value) throws IOException;

	public abstract void writeBooleanArray(boolean buffer[], int len) throws IOException;
	
	public abstract void writeByteArray(byte buffer[], int len) throws IOException ;
	
	public abstract void writeCharArray(char buffer[], int len) throws IOException;

	public abstract void writeShortArray(short buffer[], int len) throws IOException;

	public abstract void writeIntArray(int buffer[], int len) throws IOException;

	public abstract void writeLongArray(long buffer[], int len) throws IOException;

	public abstract void writeFloatArray(float buffer[], int len) throws IOException;

	public abstract void writeDoubleArray(double buffer[], int len) throws IOException ;

	public abstract void writeObjectArray(Object buffer[], int len) throws IOException,
			IllegalAccessException, InvocationTargetException, struct.StructException;
}