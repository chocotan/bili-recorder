package moe.chikalar.recorder.struct;

import moe.chikalar.recorder.struct.Constants.Primitive;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class StructInput extends InputStream {

    public void readObject(Object obj) throws StructException {
        if (obj == null) throw new StructException("Struct objects cannot be null.");
        StructData info = StructUtils.getStructInfo(obj);
        Field[] fields = info.getFields();

        for (Field currentField : fields) {
            //System.out.println("Processing field: " + currentField.getName());
            StructFieldData fieldData = info.getFieldData(currentField.getName());
            if (fieldData == null) {
                throw new StructException("Field Data not found for field: " + currentField.getName());
            }
            int arrayLength = -1;
            boolean lengthedArray = false;
            try {
                if (info.isLenghtedArray(currentField)) {
                    Field f = info.getLenghtedArray(currentField.getName());
                    StructFieldData lengthMarker = info.getFieldData(f.getName());
                    if (lengthMarker.requiresGetterSetter()) {
                        arrayLength = ((Number) lengthMarker.getGetter().invoke(obj, (Object[]) null)).intValue();
                    } else {
                        arrayLength = ((Number) lengthMarker.getField().get(obj)).intValue();
                    }
                    lengthedArray = true;
                }
                // For private and protected fields, use getFieldName or isFieldName
                if (fieldData.requiresGetterSetter()) {
                    Method getter = fieldData.getGetter();
                    Method setter = fieldData.getSetter();

                    if (getter == null || setter == null) {
                        throw new StructException(" getter/setter required for : " + currentField.getName());
                    }

                    if (lengthedArray && arrayLength >= 0) {
                        Object ret = Array.newInstance(currentField.getType().getComponentType(), arrayLength);
                        setter.invoke(obj, new Object[]{ret});
                        if (currentField.getType().getComponentType().isPrimitive() == false) {
                            Object[] array = (Object[]) ret;
                            for (int j = 0; j < arrayLength; j++) {
                                array[j] = currentField.getType().getComponentType().newInstance();
                            }
                        }
                    }
                    if (lengthedArray == false && currentField.getType().isArray()) {
                        if (getter.invoke(obj, (Object[]) null) == null) {
                            throw new StructException("Arrays can not be null :" + currentField.getName());
                        }
                    }
                    readField(fieldData, getter, setter, obj);
                }
                // If public, use directly.
                else {
                    if (lengthedArray && arrayLength >= 0) {
                        Object ret = Array.newInstance(currentField.getType().getComponentType(), arrayLength);
                        currentField.set(obj, ret);
                        if (currentField.getType().getComponentType().isPrimitive() == false) {
                            Object[] array = (Object[]) ret;
                            for (int j = 0; j < arrayLength; j++) {
                                array[j] = currentField.getType().getComponentType().newInstance();
                            }
                        }
                    }
                    if (lengthedArray == false && currentField.getType().isArray()) {
                        if (currentField.get(obj) == null) {
                            throw new StructException("Arrays can not be null. : " + currentField.getName());
                        }
                    }
                    if (lengthedArray == false || (lengthedArray == true && arrayLength >= 0)) {
                        readField(fieldData, null, null, obj);
                    }
                }
            } catch (Exception e) {
                throw new StructException(e);
            }
        }
    }

    public void readField(StructFieldData fieldData, Method getter, Method setter, Object obj)
            throws IOException, InvocationTargetException, InstantiationException, IllegalAccessException, StructException {
        Field field = fieldData.getField();
        if (!field.getType().isArray()) {
            switch (fieldData.getType()) {
                case BOOLEAN:
                    if (setter != null) setter.invoke(obj, new Object[]{readBoolean()});
                    else field.setBoolean(obj, readBoolean());
                    break;

                case BYTE:
                    if (setter != null) setter.invoke(obj, new Object[]{readByte()});
                    else field.setByte(obj, readByte());
                    break;

                case SHORT:
                    if (setter != null) setter.invoke(obj, new Object[]{readShort()});
                    else field.setShort(obj, readShort());
                    break;

                case INT:
                    if (setter != null) setter.invoke(obj, new Object[]{readInt()});
                    else field.setInt(obj, readInt());
                    break;

                case LONG:
                    if (setter != null) setter.invoke(obj, new Object[]{readLong()});
                    else field.setLong(obj, readLong());
                    break;

                case CHAR:
                    if (setter != null) setter.invoke(obj, new Object[]{readChar()});
                    else field.setChar(obj, readChar());
                    break;

                case FLOAT:
                    if (setter != null) setter.invoke(obj, new Object[]{readFloat()});
                    else field.setFloat(obj, readFloat());
                    break;

                case DOUBLE:
                    if (setter != null) setter.invoke(obj, new Object[]{readDouble()});
                    else field.setDouble(obj, readDouble());
                    break;

                default:
                    if (setter != null) {
                        Object object = getter.invoke(obj, (Object[]) null);
                        if (object == null) {
                            if (field.getName().endsWith("CString")) {
                                throw new StructException("CString objects should be initialized :" + field.getName());
                            }
                            object = field.getType().newInstance();
                        }
                        readObject(object);
                        setter.invoke(obj, new Object[]{object});
                    } else handleObject(field, obj);
                    break;
            }
        } else {
            if (getter != null && getter.invoke(obj, (Object[]) null) == null)
                throw new StructException("Arrays can not be null : " + field.getName());
            switch (fieldData.getType()) {
                case BOOLEAN:
                    if (getter != null) readBooleanArray((boolean[]) getter.invoke(obj, (Object[]) null));
                    else readBooleanArray((boolean[]) field.get(obj));
                    break;

                case BYTE:
                    if (getter != null) readByteArray((byte[]) getter.invoke(obj, (Object[]) null));
                    else readByteArray((byte[]) field.get(obj));
                    break;

                case CHAR:
                    if (getter != null) readCharArray((char[]) getter.invoke(obj, (Object[]) null));
                    else readCharArray((char[]) field.get(obj));
                    break;

                case SHORT:
                    if (getter != null) readShortArray((short[]) getter.invoke(obj, (Object[]) null));
                    else readShortArray((short[]) field.get(obj));
                    break;

                case INT:
                    if (getter != null) readIntArray((int[]) getter.invoke(obj, (Object[]) null));
                    else readIntArray((int[]) field.get(obj));
                    break;

                case LONG:
                    if (getter != null) readLongArray((long[]) getter.invoke(obj, (Object[]) null));
                    else readLongArray((long[]) field.get(obj));
                    break;

                case FLOAT:
                    if (getter != null) readFloatArray((float[]) getter.invoke(obj, (Object[]) null));
                    else readFloatArray((float[]) field.get(obj));
                    break;

                case DOUBLE:
                    if (getter != null) readDoubleArray((double[]) getter.invoke(obj, (Object[]) null));
                    else readDoubleArray((double[]) field.get(obj));
                    break;

                default:
                    if (getter != null) readObjectArray((Object[]) getter.invoke(obj, (Object[]) null));
                    else readObjectArray((Object[]) field.get(obj));
                    break;
            }
        }
    }

    /**
     * @param field
     * @param obj
     * @throws IllegalArgumentException
     * @throws StructException
     * @throws IllegalAccessException
     * @throws IOException
     */
    public void handleObject(Field field, Object obj)
            throws IllegalArgumentException, StructException, IOException,
            InstantiationException, IllegalAccessException {
        if (field.get(obj) == null) {
            if (field.getType().getName().endsWith("CString")) {
                throw new StructException("CString objects should be initialized before unpacking :"
                        + field.getName());
            }
            field.set(obj, field.getType().newInstance());
        }
        readObject(field.get(obj));
    }

    public void close() throws IOException {
    }

    public int read() throws IOException {
        return -1;
    }

    protected abstract boolean readBoolean() throws IOException;

    protected abstract byte readByte() throws IOException;

    protected abstract short readShort() throws IOException;

    protected abstract int readInt() throws IOException;

    protected abstract long readLong() throws IOException;

    protected abstract char readChar() throws IOException;

    protected abstract float readFloat() throws IOException;

    protected abstract double readDouble() throws IOException;

    protected abstract void readBooleanArray(boolean buffer[]) throws IOException;

    protected abstract void readByteArray(byte buffer[]) throws IOException;

    protected abstract void readCharArray(char buffer[]) throws IOException;

    protected abstract void readShortArray(short buffer[]) throws IOException;

    protected abstract void readIntArray(int buffer[]) throws IOException;

    protected abstract void readLongArray(long buffer[]) throws IOException;

    protected abstract void readFloatArray(float buffer[]) throws IOException;

    protected abstract void readDoubleArray(double buffer[]) throws IOException;

    protected abstract void readObjectArray(Object objects[]) throws IOException, StructException;

}
