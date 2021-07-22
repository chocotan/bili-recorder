package moe.chikalar.recorder.struct;

import java.lang.reflect.Field;
import java.util.HashMap;

public class Constants {
	
	private static HashMap<String, Primitive> primitiveTypes = new HashMap<String, Primitive>();
	private static HashMap<Character, Primitive> signatures = new HashMap<Character, Primitive>();

	public enum Primitive {
		BOOLEAN("boolean", 'Z', 0),
		BYTE("byte", 'B', 1),
		CHAR("char", 'C', 2),
		SHORT("short", 'S', 3),
		INT("int", 'I', 4),
		LONG("long", 'J', 5),
		FLOAT("float", 'F', 6),
		DOUBLE("double", 'D', 7),
		OBJECT("object", 'O', 8);
		String type;
		char signature;
		int order;
		
		private Primitive(String type, char signature, int order) {
			this.type = type;
			this.signature = signature;
			this.order = order;
		}
	}
	
	static{
		for(Primitive p : Primitive.values()){
			primitiveTypes.put(p.type, p);
			signatures.put(p.signature, p);
		}
	}

	public static final Primitive getPrimitive(Field field){
		if ( !field.getType().isArray() ){
			return getPrimitive(field.getType().getName());
		} else {
			return getPrimitive(field.getType().getName().charAt(1));
		}
	}
	
	public static final Primitive getPrimitive(String name){
		Primitive p = primitiveTypes.get(name);
		return p != null ? p : Primitive.OBJECT;
	}
	
	public static final Primitive getPrimitive(char signature){
		Primitive p = signatures.get(signature);
		return p != null ? p : Primitive.OBJECT;
	}
}
