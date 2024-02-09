package dev.fneira.interfaceprocessor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.cglib.proxy.MethodInterceptor;

/** Annotation to define the handler for the interface processor. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InterfaceProcessorHandler {

  /**
   * The handler class for the interface processor.
   *
   * @return the handler class for the interface processor
   */
  Class<? extends MethodInterceptor> value();
}
