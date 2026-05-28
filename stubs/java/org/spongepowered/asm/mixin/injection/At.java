package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.*;
@Target({}) @Retention(RetentionPolicy.RUNTIME)
public @interface At {
    String value();
    String target() default "";
    boolean remap() default true;
}
