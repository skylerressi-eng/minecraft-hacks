package org.spongepowered.asm.mixin;
import java.lang.annotation.*;
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface Mixin {
    Class<?>[] value() default {};
    String[] targets() default {};
    boolean remap() default true;
    int priority() default 1000;
}
