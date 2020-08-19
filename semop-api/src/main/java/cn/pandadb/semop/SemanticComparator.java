package cn.pandadb.semop;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface SemanticComparator {
    String name();

    DomainType[] domains();

    double threshold() default 0.7;
}
