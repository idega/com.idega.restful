package com.idega.restful.spring.container;

import java.util.logging.Logger;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;

import com.idega.util.ArrayUtil;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;

public class IWSpringComponentProviderFactory extends SpringComponentProviderFactory {

	private static final Logger LOGGER = Logger.getLogger(IWSpringComponentProviderFactory.class.getName());

	private final ConfigurableApplicationContext springContext;

	public IWSpringComponentProviderFactory(ResourceConfig rc, ConfigurableApplicationContext springContext) {
		super(rc, springContext);

		this.springContext = springContext;
		registerSpringBeans(rc);
    }

	protected void registerSpringBeans(final ResourceConfig rc) {
        String[] names = BeanFactoryUtils.beanNamesIncludingAncestors(springContext);
        if (ArrayUtil.isEmpty(names))
        	return;

        for (String name : names) {
            Class<?> type = ClassUtils.getUserClass(springContext.getType(name));
            if (!rc.getClasses().contains(type) && !doRegister(rc, type, name) && springContext.isSingleton(name)) {
            	Object bean = null;
            	try {
            		bean = springContext.getBean(name);
            	} catch (Exception e) {
            		LOGGER.warning("Error getting instance of " + name);
            	}
            	if (AopUtils.isAopProxy(bean)) {
            		Advised proxy = (Advised) bean;
            		type = proxy.getTargetClass();
            		doRegister(rc, type, name);
            	}
            }
        }
    }

	private boolean doRegister(ResourceConfig rc, Class<?> type, String name) {
		 if (ResourceConfig.isProviderClass(type) && !rc.getClasses().contains(type)) {
             LOGGER.info("Registering Spring bean, " + name + ", of type " + type.getName() + " as a provider class");
             rc.getClasses().add(type);
             return true;
         }
		 if (ResourceConfig.isRootResourceClass(type) && !rc.getClasses().contains(type)) {
             LOGGER.info("Registering Spring bean, " + name + ", of type " + type.getName() + " as a root resource class");
             rc.getClasses().add(type);
             return true;
         }

		 return false;
	}

}