package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.*;
@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    String[] method();
    At[] at();
    boolean cancellable() default false;
    boolean remap() default true;
}
