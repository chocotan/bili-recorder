package struct;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * This class keeps class metadata for faster access
 *
 */
public class StructData {
	Field[] fields = null;
	Method[] methods = null;
	HashMap<String, Field> lengthedArrayFields = new HashMap<String, Field>();
	HashMap<String, struct.StructFieldData> fieldDataMap = new HashMap<String, struct.StructFieldData>();
	static int ACCEPTED_MODIFIERS =  Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;

	/**
	 * Analyse the class and find getter - setter methods.
	 * 
	 * @param fields
	 * @param methods
	 */
	public StructData(Field[] fields, Method[] methods) throws struct.StructException {
		this.fields = fields;
		this.methods = methods;
		
		for (Field field : fields) {
			// check the fields modifier type. if it's not accepted, throw an  exception.
			if (!((field.getModifiers() & ~ACCEPTED_MODIFIERS) == 0 
				&& (field.getModifiers() | ACCEPTED_MODIFIERS) != 0)) {
				throw new struct.StructException("Field type should be public, private or protected : " + field.getName());
			}
			struct.StructFieldData fieldData = new struct.StructFieldData(field);
			
			// find the members whose lengths are given in another field.
			struct.ArrayLengthMarker lengthMarker = field.getAnnotation(struct.ArrayLengthMarker.class);
			if (lengthMarker != null) {
				fieldData.setArrayLengthMarker(true);
				int i= 0;
				for(; i<fields.length ;i++){
					if (lengthMarker.fieldName().equals(fields[i].getName())) {
						lengthedArrayFields.put(fields[i].getName(), field);
						break;
					}
				}
				if (i == fields.length){
					throw new struct.StructException("Lenght Marker Fields target is not found: " + lengthMarker.fieldName());
				}
			}
			// If required, Find getter and setter methods, put into Maps for faster access later.
			if (struct.StructUtils.requiresGetterSetter(field.getModifiers())) {
				fieldData.setGetter(getGetterName(methods, field));
				fieldData.setSetter(getSetterName(methods, field));
				fieldData.setRequiresGetterSetter(true);
			}
			fieldData.setType( struct.Constants.getPrimitive(field) );
			
			fieldDataMap.put(field.getName(),fieldData);
		}
	}
	
	public struct.StructFieldData getFieldData(String fieldName){
		return fieldDataMap.get(fieldName);
	}

	/**
	 * Is there a getter for this field?
	 * 
	 * @param methods
	 * @param field
	 * @return
	 */
	private static final Method getGetterName(Method[] methods, Field field) throws struct.StructException {
		String getterName = "get" + field.getName();
		String booleanGetterName = "is" + field.getName();
		for (Method method : methods) {
			if (method.getName().equalsIgnoreCase(getterName)) {
				return method;
			}
		}
		if (field.getType().getName().equals("boolean")) {
			for (Method method : methods) {
				if (method.getName().equalsIgnoreCase(booleanGetterName)) {
					return method;
				}
			}
		}
		throw new struct.StructException("The field needs a getter method, but none supplied. Field: " + field.getName());
	}

	/**
	 * Find setter for a given member.
	 * @param methods : methods
	 * @param field : field
	 * @return : the setter method, if exists, else null.
	 */
	private static final Method getSetterName(Method[] methods, Field field) throws struct.StructException {
		String setterName = "set" + field.getName();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equalsIgnoreCase(setterName)) {
				return methods[i];
			}
		}
		throw new struct.StructException("The field needs a setter method, but none supplied. Field: " + field.getName());
	}
    
	public Field[] getFields() {
		return fields;
	}

	public Method[] getMethods() {
		return methods;
	}

	public boolean isLenghtedArray(Field field) {
		return (lengthedArrayFields.get(field.getName()) != null);
	}

	public Field getLenghtedArray(String fieldName) {
		return lengthedArrayFields.get(fieldName);
	}
}