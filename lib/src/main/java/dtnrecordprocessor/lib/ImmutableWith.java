package dtnrecordprocessor.lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface ImmutableWith {

    String className() default "";

}
