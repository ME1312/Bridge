package bridge;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * An annotation for editing class properties
 */
@Target(ElementType.TYPE)
public @interface Adopt {

    /**
     * Removes existing class metadata when <code>true</code>
     *
     * @return Clean Status
     */
    boolean clean() default false;

    /**
     * Overwrites the <code>extends</code> clause
     *
     * @implNote Compatible super constructor(s) must exist within the specified class for them to be usable
     * @return Parent Class
     */
    Class<?> parent() default Bridges.class;

    /**
     * Appends to the <code>implements</code> clause
     *
     * @return Parent Interfaces
     */
    Class<?>[] interfaces() default {};

    /**
     * Adds generic type data to the class definition
     *
     * @return Signature String
     */
    String signature() default "";
}
