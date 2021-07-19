package struct;

@SuppressWarnings("serial")
public class StructException extends Exception {

	public StructException() {
        super();
    }

    public StructException(String arg0) {
        super(arg0);
    }

    public StructException(Throwable arg0) {
        super(arg0);
    }

    public StructException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
}
