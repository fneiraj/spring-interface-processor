package dev.fneira.interfaceprocessor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/** Enable Interface Processor */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({InterfaceProcessorScannerRegistrar.class})
public @interface EnableInterfaceProcessor {
  /**
   * Base packages to scan for annotated components.
   * Indicates the base packages to scan for annotated components. The value may be a
   * single package name or multiple package names. If specific packages are not defined,
   * scanning will occur from the package of the class that declares this annotation.
   * @return base packages to scan
   */
  String[] basePackages() default {};
}
