package dev.fneira.interfaceprocessor;

import jakarta.annotation.Nonnull;
import java.util.Objects;
import java.util.logging.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

/** Factory bean for creating a proxy for an interface. */
public class InterfaceProcessorFactoryBean
    implements FactoryBean<Object>, InitializingBean, BeanFactoryAware {

  /** The logger. */
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  /** The bean factory. */
  private BeanFactory beanFactory;

  /** The type of the interface to proxy. */
  private Class<?> type;

  /** The handler for the proxy. */
  private Class<? extends MethodInterceptor> handler;

  /**
   * Create a proxy for the interface.
   *
   * @return the proxy
   * @param <T> the type of the interface
   */
  @SuppressWarnings("unchecked")
  <T> T createProxyForInterface() {
    logger.info("Creating proxy for interface: " + type);
    return (T) Enhancer.create(type, beanFactory.getBean(handler));
  }

  /** After properties set, check that the required properties are set. */
  @Override
  public void afterPropertiesSet() {
    Objects.requireNonNull(type, "Property 'type' is required");
    Objects.requireNonNull(handler, "Property 'handler' is required");
  }

  /**
   * Get the object.
   *
   * @return the object
   */
  @Override
  public Object getObject() {
    return createProxyForInterface();
  }

  /**
   * Get the type of the object.
   *
   * @return the type of the object
   */
  @Override
  public Class<?> getObjectType() {
    return type;
  }

  /**
   * Set the bean factory.
   *
   * @param beanFactory the bean factory
   * @throws BeansException if an error occurs
   */
  @Override
  public void setBeanFactory(@Nonnull final BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  /**
   * Set the type of the interface to proxy.
   *
   * @param type the type of the interface to proxy
   */
  public void setType(final Class<?> type) {
    this.type = type;
  }

  /**
   * Set the handler for the proxy.
   *
   * @param handler the handler for the proxy
   */
  public void setHandler(final Class<? extends MethodInterceptor> handler) {
    this.handler = handler;
  }
}
