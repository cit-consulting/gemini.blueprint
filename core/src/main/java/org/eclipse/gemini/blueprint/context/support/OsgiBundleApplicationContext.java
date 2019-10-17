package org.eclipse.gemini.blueprint.context.support;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;

import java.util.Arrays;

public class OsgiBundleApplicationContext extends AbstractDelegatedExecutionApplicationContext implements DisposableBean {

    private final String[] config;

    public OsgiBundleApplicationContext(final String[] config) {
        this.config = config;
    }

    public OsgiBundleApplicationContext(final String[] config, final ApplicationContext parent) {
        super(parent);
        this.config = config;
    }

    @Override
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Arrays.stream(config).forEach(it ->
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
}
