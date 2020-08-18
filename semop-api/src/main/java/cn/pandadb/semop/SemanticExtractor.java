package cn.pandadb.semop;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface SemanticExtractor {
    String name();

    DomainType domain();
}
