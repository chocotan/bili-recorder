package struct;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteOrder;

/**
 * StructUnpacker is the default deserializer in Javastruct. It uses 
 * DataStreams (Both Big endian and Little endian)
 * 
 * @author mdakin
 *
 */
public class StructUnpacker extends struct.StructInput {

	DataInput dataInput;

	protected void init( InputStream inStream, ByteOrder order) {
		if ( order == ByteOrder.LITTLE_ENDIAN){
			dataInput = new struct.LEDataInputStream(inStream);
		}
		else {
			dataInput = new DataInputStream( inStream );
		}
	}
	  
    public StructUnpacker(byte[] bufferToUnpack){
        this(new ByteArrayInputStream(bufferToUnpack), ByteOrder.BIG_ENDIAN);
    }
    
    public StructUnpacker(byte[] bufferToUnpack, ByteOrder order){
    	this(new ByteArrayInputStream(bufferToUnpack), order);
    }
    
    public StructUnpacker(InputStream is, ByteOrder order){
    	init(is, order);
    }

    public void unpack(Object objectToUnpack) throws struct.StructException {
        readObject(objectToUnpack);
    }

    @Override
    public void readObject( Object obj) throws struct.StructException {
        if(obj == null)  throw new struct.StructException("Struct objects cannot be null.");
        struct.StructData info = struct.StructUtils.getStructInfo(obj);
        Field[] fields = info.getFields();

        for (Field currentField : fields) {
			//System.out.println("Processing field: " + currentField.getName());
			struct.StructFieldData fieldData = info.getFieldData(currentField.getName());
			if(fieldData == null) {
				throw new struct.StructException("Field Data not found for field: " + currentField.getName());
			}        
            int arrayLength = -1;
            boolean lengthedArray = false;
            try{
            	if(info.isLenghtedArray(currentField)){
            		Field f = info.getLenghtedArray(currentField.getName());
            		struct.StructFieldData lengthMarker = info.getFieldData(f.getName());
            		if (lengthMarker.requiresGetterSetter()) {
            			arrayLength = ((Number)lengthMarker.getGetter().invoke( obj, (Object[])null)).intValue();
            		} else {
            			arrayLength = ((Number)lengthMarker.getField().get(obj)).intValue();
            		}
	       			lengthedArray = true;
            	}
            	// For private and protected fields, use getFieldName or isFieldName
            	if ( fieldData.requiresGetterSetter()){
            		Method getter = fieldData.getGetter();
            		Method setter = fieldData.getSetter();
            		
            		if(getter == null || setter == null){
            			throw new struct.StructException(" getter/setter required for : "+ currentField.getName());
            		}
            		
            		if(lengthedArray && arrayLength >= 0){
            			Object ret = Array.newInstance(currentField.getType().getComponentType(),arrayLength);
            			setter.invoke(obj,new Object[]{ret});
            			if(currentField.getType().getComponentType().isPrimitive() == false){
            				Object[] array = (Object[])ret;
            				for(int j=0; j<arrayLength; j++){
            					array[j] =  currentField.getType().getComponentType().newInstance();
            				}
            			}
            		}
            		if(lengthedArray == false && currentField.getType().isArray()){
            			if(getter.invoke (obj, (Object[])null) == null){
            				throw new struct.StructException("Arrays can not be null :"+ currentField.getName());
            			}
            		}
            		readField(fieldData, getter, setter, obj);
            	}
            	// If public, use directly.
            	else {
            		if(lengthedArray && arrayLength >= 0) {
            			Object ret = Array.newInstance(currentField.getType().getComponentType(),arrayLength);
            			currentField.set(obj,ret);
            			if(currentField.getType().getComponentType().isPrimitive() == false){
            				Object[] array = (Object[])ret;
            				for(int j=0; j<arrayLength; j++){
            					array[j] =  currentField.getType().getComponentType().newInstance();
            				}
            			}
            		}
            		if(lengthedArray == false && currentField.getType().isArray()){
            			if(currentField.get(obj)== null){
            				throw new struct.StructException("Arrays can not be null. : "+ currentField.getName());
            			}
            		}
            		if(lengthedArray == false || (lengthedArray == true && arrayLength >= 0)){
            			readField(fieldData, null, null, obj);
            		}
            	}
            }
            catch (Exception e) {
                 throw new struct.StructException(e);
            }
        }
    }
    
    protected boolean readBoolean() throws IOException {
    	return dataInput.readBoolean();
    }

    protected byte readByte() throws IOException {
    	return dataInput.readByte();
    }

    protected short readShort() throws IOException {
    	return dataInput.readShort();
    }

    protected int readInt() throws IOException {
    	return dataInput.readInt();
    }

    protected long readLong() throws IOException {
    	return dataInput.readLong();
    }

    protected char readChar() throws IOException {
    	return dataInput.readChar();
    }

    protected float readFloat() throws IOException {
    	return dataInput.readFloat();
    }

    protected double readDouble() throws IOException {
    	return dataInput.readDouble();
    }

    protected void readBooleanArray(boolean buffer[]) throws IOException {
    	for ( int i=0; i<buffer.length; i++)
    		buffer[i] = readBoolean();
    }

    protected void readByteArray( byte buffer[] ) throws IOException {
    	dataInput.readFully(buffer);
    }

    protected void readCharArray( char buffer[] ) throws IOException {
    	for ( int i=0; i<buffer.length; i++)
    		buffer[i] = readChar();
    }

    protected void readShortArray( short buffer[] ) throws IOException {
    	for ( int i=0; i<buffer.length; i++)
    		buffer[i] = readShort();
    }

    protected void readIntArray( int buffer[] ) throws IOException {
    	for ( int i=0; i<buffer.length; i++)
    		buffer[i] = readInt();
    }

    protected void readLongArray( long buffer[] ) throws IOException {
    	for ( int i=0; i<buffer.length; i++)
    		buffer[i] = readLong();
    }

    protected void readFloatArray( float buffer[] ) throws IOException {
    	for ( int i=0; i<buffer.length; i++)
    		buffer[i] = readFloat();
    }

    protected void readDoubleArray( double buffer[] ) throws IOException {
    	for ( int i=0; i<buffer.length; i++)
    		buffer[i] = readDouble();
    }

    protected void readObjectArray( Object objects[] ) throws  IOException, struct.StructException {
    	for ( int i=0; i<objects.length; i++)
    		readObject( objects[i] );
    }


}