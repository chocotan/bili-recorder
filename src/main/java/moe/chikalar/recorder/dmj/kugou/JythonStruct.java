package moe.chikalar.recorder.dmj.kugou;

import lombok.val;
import org.python.core.*;
import org.python.util.PythonInterpreter;

import javax.script.ScriptException;
import java.util.Properties;

public class JythonStruct {
    static {
        Properties props = new Properties();
        props.put("python.import.site", "false");
        Properties preprops = System.getProperties();
        PythonInterpreter.initialize(preprops, props, new String[0]);
    }

    public static byte[] pack(String fmt, long val) throws ScriptException {

        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("import struct\n");
        interpreter.set("fmt", new PyString("!b"));
        interpreter.set("val", new PyInteger(100));
        interpreter.exec("res = struct.pack(fmt, val)");
        PyString result = (PyString) interpreter.get("res");
        return result.toBytes();
    }

    public static int unpackFrom(String fmt, byte[] message, long val) throws ScriptException {

        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("import struct\n");
        interpreter.set("fmt", new PyString("!b"));
        interpreter.set("val", new PyInteger(100));
        interpreter.set("message", new PyByteArray(message));
        interpreter.exec("res = struct.unpack_from(fmt, message, val)");
        PyTuple result = (PyTuple) interpreter.get("res");
        return 0;
    }
}
