package moe.chikalar.recorder.struct;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * Common functions
 *
 */
public class StructUtils {
    private static HashMap<String, StructData> structInfoCache = new HashMap<String, StructData>();

    /**
     * Put class metadata and required info to a cache
     *
     * @param obj Object
     * @throws StructException
     */
    public static synchronized StructData getStructInfo(Object obj) throws StructException {
        StructData info = structInfoCache.get(obj.getClass().getName());
        if(info != null) {
            return info;
        }
         //Annotated member check.
        if(obj.getClass().getAnnotation(StructClass.class) != null){
        	isAccessible(obj);
            Field annotatedFields[] = obj.getClass().getDeclaredFields();
            Field tmpStructFields[] = new Field[annotatedFields.length];
            int annotatedFieldCount = 0;
            for(Field f : annotatedFields){
                StructField sf = f.getAnnotation(StructField.class);
                if(sf != null){
                    int order = sf.order();
                    if(order < 0 || order >= annotatedFields.length) {
                        throw new StructException("Order is illegal for StructField : " + f.getName());
                    }
                    annotatedFieldCount ++ ;
                    tmpStructFields[order] = f;
                }
            }
            Field structFields[] = new Field[annotatedFieldCount];

            for(int i = 0; i<annotatedFieldCount ; i++){
                if(tmpStructFields[i] == null){
                    throw new StructException("Order error for annotated fields! : " + obj.getClass().getName());
                }
                structFields[i] = tmpStructFields[i];
            }
            info = new StructData(structFields, obj.getClass().getDeclaredMethods());
            structInfoCache.put(obj.getClass().getName(), info);
            return info;
        }
        throw new StructException("No struct Annotation found for " + obj.getClass().getName());
    }

    /**
     * is object accessible?
     *
     * @param obj Object
     * @throws StructException
     */
    public static void isAccessible(Object obj) throws StructException {
        int modifiers = obj.getClass().getModifiers();
        if ((modifiers & Modifier.PUBLIC) == 0)
            throw new StructException("Struct operations are only accessible for public classes. Class: " + obj.getClass().getName());
        if ((modifiers & (Modifier.INTERFACE | Modifier.ABSTRACT)) != 0)
            throw new StructException("Struct operations are not accessible for abstract classes and interfaces. Class: " + obj.getClass().getName());
    }

    /**
     * Does this field requires a getter or setter?.
     * @param modifier , modifier mask
     * @return : true if field requires getter - setter. false otherwise
     */
    public static boolean requiresGetterSetter(int modifier){
       return (modifier == 0) || (modifier & (Modifier.PRIVATE | Modifier.PROTECTED)) != 0 ;
   }
}
