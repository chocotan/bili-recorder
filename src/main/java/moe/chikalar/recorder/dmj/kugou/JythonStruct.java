package moe.chikalar.recorder.dmj.kugou;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.python.core.*;
import org.python.modules.struct;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.Properties;

public class JythonStruct {
    static Logger logger = LoggerFactory.getLogger(JythonStruct.class);
    static {
        try {
            Properties props = new Properties();
            props.put("python.import.site", "false");
            Properties preprops = System.getProperties();
            PythonInterpreter.initialize(preprops, props, new String[0]);
        } catch (Throwable e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    public static byte[] pack(String fmt, long val) throws ScriptException {
//        PythonInterpreter interpreter = new PythonInterpreter();
//        interpreter.exec("import struct\n");
//        interpreter.set("fmt", new PyString(fmt));
//        interpreter.set("val", new PyLong(val));
//        interpreter.exec("res = struct.pack(fmt, val)");
//        PyString result = (PyString) interpreter.get("res");
//        return result.toBytes();

        PyObject[] objs = new PyObject[]{
                new PyString(fmt),
                new PyLong(val)
        };
        PyString pack = struct.pack(objs);
        return pack.toBytes();
    }

    public static Integer unpackFrom(String fmt, byte[] message, long val) throws ScriptException {

//        PythonInterpreter interpreter = new PythonInterpreter();
//        interpreter.exec("import struct\n");
//        interpreter.set("fmt", new PyString(fmt));
//        interpreter.set("val", new PyLong(val));
//        interpreter.set("message", new PyByteArray(message));
//        interpreter.exec("res = struct.unpack_from(fmt, message, val)");
//        PyTuple result = (PyTuple) interpreter.get("res");
//        return (Integer) result.get(0);
        return (Integer) struct.unpack_from(new PyString(fmt).asString(), new PyByteArray(message), new PyLong(val).asInt()).get(0);
    }


}
