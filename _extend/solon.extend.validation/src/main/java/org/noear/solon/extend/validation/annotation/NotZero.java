package org.noear.solon.extend.validation.annotation;


import org.noear.solon.annotation.XNote;

import java.lang.annotation.*;

/**
 * 不能小于min
 * */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotZero {
    /**
     * param names
     * */
    @XNote("param names")
    String[] names();
}