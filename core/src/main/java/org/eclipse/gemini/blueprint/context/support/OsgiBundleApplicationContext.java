package org.eclipse.gemini.blueprint.context.support;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class OsgiBundleApplicationContext extends AbstractDelegatedExecutionApplicationContext implements DisposableBean {

    private final Set<String> configs = new HashSet<>();

    public OsgiBundleApplicationContext(final String[] config) {
        this.configs.addAll(Arrays.asList(config));
    }

    public OsgiBundleApplicationContext(final String[] config, final ApplicationContext parent) {
        super(parent);
        if (config != null) {
            this.configs.addAll(Arrays.asList(config));
        }
    }

    @Override
    protected void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        super.postProcessBeanFactory(beanFactory);
        //exposing app context as bean for possibility obtain it as ref() in bundles
        beanFactory.registerSingleton("applicationContext", new ApplicationContextBean(this));
        //using xml app context for creating xml bean def reader for possibility  to importing xml context from BeanDSL
        final OsgiBundleXmlApplicationContext xmlApplicationContext = new OsgiBundleXmlApplicationContext(new String[]{});
        xmlApplicationContext.setBundleContext(getBundleContext());
        //and exposing it as bean
        beanFactory.registerSingleton("xmlBeanDefinitionReader", xmlApplicationContext.createXmlBeanDefinitionReader(this));
        configs.forEach(it ->
                        {
                            try {
                                final Class<ApplicationContextInitializer> aClass = (Class<ApplicationContextInitializer>) this.getClassLoader().loadClass(it);
                                aClass.newInstance().initialize(this);
                            } catch (ClassNotFoundException e) {
                                throw new ApplicationContextException("Can't load class " + it, e);
                            } catch (InstantiationException | IllegalAccessException e) {
                                try {
                                    throw new BeanInstantiationException(this.getClassLoader().loadClass(it), "Cant instantiate class " + it, e);
                                } catch (ClassNotFoundException ex) {
                                    throw new ApplicationContextException("Can't load class " + it, ex);
                                }
                            }
                        }
        );
    }

    @Override
    public void setConfigLocations(final String... configLocations) {
        if (configLocations != null) {
            this.configs.addAll(Arrays.asList(configLocations));
        }
    }
}
