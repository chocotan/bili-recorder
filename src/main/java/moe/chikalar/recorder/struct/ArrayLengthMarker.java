package moe.chikalar.recorder.struct;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD })
public @interface ArrayLengthMarker {
	String fieldName();
}
