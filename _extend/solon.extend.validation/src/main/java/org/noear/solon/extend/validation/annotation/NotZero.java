package org.noear.solon.extend.validation.annotation;


import org.noear.solon.annotation.XNote;

import java.lang.annotation.*;

/**
 * 不能小于min
 *
 * @author noear
 * @since 1.0
 * */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotZero {
    /**
     * param names
     * */
    @XNote("param names")
    String[] value() default {};

    String message() default "";
}
