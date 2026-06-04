package ma.noba.config;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DeprecatedApi {
    String sunset() default "";
    String migration() default "";
    String deprecationDate() default "";
}
