package bridge;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * An annotation for accessing the hidden <code>synthetic</code> access modifier
 */
@Documented
@Target({ TYPE, CONSTRUCTOR, METHOD, FIELD })
public @interface Synthetic {

}
