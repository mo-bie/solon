package org.noear.solon.extend.validation.annotation;


import org.noear.solon.annotation.XNote;

import java.lang.annotation.*;

/**
 * 不能为null
 *
 * @author noear
 * @since 1.0
 * */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotNull {
    /**
     * param names
     * */
    @XNote("param names")
    String[] value() default {};

    String message() default "";
}
