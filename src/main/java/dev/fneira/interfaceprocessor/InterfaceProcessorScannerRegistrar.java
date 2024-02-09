package dev.fneira.interfaceprocessor;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/** Registrar for scanning interfaces annotated with {@link InterfaceProcessorHandler}. */
public class InterfaceProcessorScannerRegistrar
    implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

  /** The logger. */
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  /** The environment. */
  private Environment environment;

  /** The resource loader. */
  private ResourceLoader resourceLoader;

  /**
   * Register the bean definitions.
   *
   * @param metadata the metadata
   * @param registry the registry
   */
  @Override
  public void registerBeanDefinitions(
      @Nonnull final AnnotationMetadata metadata, @Nonnull final BeanDefinitionRegistry registry) {
    logger.info("Bean registration started");

    getBasePackages(metadata).stream()
        .map(basePackage -> getScanner().findCandidateComponents(basePackage))
        .flatMap(Set::stream)
        .map(this::getTargetClass)
        .peek(targetClass -> logger.info("Registering bean for " + targetClass))
        .forEach(candidateComponent -> registerBeanForInterface(registry, candidateComponent));

    logger.info("Bean registration completed");
  }

  /**
   * Get the classpath scanner.
   *
   * @return the classpath scanner
   */
  private ClassPathScanningCandidateComponentProvider getScanner() {
    final ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false, this.environment) {
          @Override
          protected boolean isCandidateComponent(final AnnotatedBeanDefinition beanDefinition) {
            if (beanDefinition.getMetadata().isIndependent()) {
              return !beanDefinition.getMetadata().isAnnotation();
            }

            return false;
          }
        };

    scanner.setResourceLoader(this.resourceLoader);
    scanner.addIncludeFilter(new AnnotationTypeFilter(InterfaceProcessorHandler.class));

    return scanner;
  }

  /**
   * Get the base packages.
   *
   * @param importingClassMetadata importing class metadata
   * @return the base packages
   */
  private Set<String> getBasePackages(final AnnotationMetadata importingClassMetadata) {
    final String[] basePackagesValue =
        Optional.ofNullable(
                importingClassMetadata.getAnnotationAttributes(
                    EnableInterfaceProcessor.class.getCanonicalName()))
            .map(attributes -> (String[]) attributes.get("basePackages"))
            .orElse(new String[0]);

    final Set<String> basePackages =
        Stream.of(basePackagesValue).filter(StringUtils::hasText).collect(Collectors.toSet());

    if (basePackages.isEmpty()) {
      final String defaultBasePackage =
          ClassUtils.getPackageName(importingClassMetadata.getClassName());
      logger.info(
          "basePackages value is not present in @EnableInterfaceProxy, using default value: "
              + defaultBasePackage);
      basePackages.add(defaultBasePackage);
    } else {
      logger.info(
          "basePackages value is present in @EnableInterfaceProxy: "
              + Arrays.toString(basePackagesValue));
    }

    return basePackages;
  }

  /**
   * Get the target class.
   *
   * @param candidateComponent the candidate component
   * @return the target class
   */
  private Class<?> getTargetClass(final BeanDefinition candidateComponent) {
    if (candidateComponent instanceof AnnotatedBeanDefinition beanDefinition) {
      final AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
      Assert.isTrue(
          annotationMetadata.isInterface(), "@ProxyHandler can only be specified on an interface");

      return getClass(beanDefinition.getBeanClassName());
    } else {
      throw new RuntimeException("Bean definition is not an instance of AnnotatedBeanDefinition");
    }
  }

  /**
   * Register a bean for the interface.
   *
   * @param registry the registry
   * @param targetClass the target class
   * @param <T> the type of the interface
   */
  private <T> void registerBeanForInterface(
      final BeanDefinitionRegistry registry, final Class<T> targetClass) {
    final BeanDefinition beanDefinition =
        BeanDefinitionBuilder.genericBeanDefinition(InterfaceProcessorFactoryBean.class)
            .addPropertyValue("type", targetClass)
            .addPropertyValue("handler", getHandler(targetClass))
            .getRawBeanDefinition();

    beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, targetClass);
    beanDefinition.setPrimary(true);

    BeanDefinitionReaderUtils.registerBeanDefinition(
        new BeanDefinitionHolder(beanDefinition, targetClass.getSimpleName()), registry);
  }

  /**
   * Get the handler for the interface.
   *
   * @param targetClass the target class
   * @return the handler
   */
  private Class<?> getHandler(final Class<?> targetClass) {
    return Stream.of(targetClass.getAnnotations())
        .filter(
            annotation ->
                annotation.annotationType().isAnnotationPresent(InterfaceProcessorHandler.class))
        .map(
            annotation ->
                annotation.annotationType().getAnnotation(InterfaceProcessorHandler.class).value())
        .findFirst()
        .orElseThrow();
  }

  /**
   * Get the class.
   *
   * @param className the class name
   * @return the class
   */
  private Class<?> getClass(final String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set the environment.
   *
   * @param environment the environment
   */
  @Override
  public void setEnvironment(@Nonnull final Environment environment) {
    this.environment = environment;
  }

  /**
   * Set the resource loader.
   *
   * @param resourceLoader the resource loader
   */
  @Override
  public void setResourceLoader(@Nonnull final ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }
}
