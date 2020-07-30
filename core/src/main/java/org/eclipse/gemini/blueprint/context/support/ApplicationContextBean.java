package org.eclipse.gemini.blueprint.context.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;

public class ApplicationContextBean implements FactoryBean<ApplicationContext> {

    private final ApplicationContext applicationContext;

    ApplicationContextBean(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public ApplicationContext getObject() throws Exception {
        return applicationContext;
    }

    @Override
    public Class<?> getObjectType() {
        return applicationContext.getClass();
    }
}
