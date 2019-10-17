/******************************************************************************
 * Copyright (c) 2006, 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at 
 * http://www.eclipse.org/legal/epl-v10.html and the Apache License v2.0
 * is available at http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses. 
 *
 * Contributors:
 *   VMware Inc.
 *****************************************************************************/

package org.eclipse.gemini.blueprint.extender.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.gemini.blueprint.context.support.OsgiBundleApplicationContext;
import org.eclipse.gemini.blueprint.extender.support.scanning.ApplicationContextInitializerScanner;
import org.eclipse.gemini.blueprint.util.SpringConfigUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.eclipse.gemini.blueprint.context.DelegatedExecutionOsgiBundleApplicationContext;
import org.eclipse.gemini.blueprint.context.support.OsgiBundleXmlApplicationContext;
import org.eclipse.gemini.blueprint.extender.OsgiApplicationContextCreator;
import org.eclipse.gemini.blueprint.extender.support.scanning.ConfigurationScanner;
import org.eclipse.gemini.blueprint.extender.support.scanning.XmlConfigurationScanner;
import org.eclipse.gemini.blueprint.util.OsgiStringUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Default {@link OsgiApplicationContextCreator} implementation.
 * <p>
 * <p/> Creates an {@link OsgiBundleXmlApplicationContext} instance using the
 * default locations (<tt>Spring-Context</tt> manifest header or
 * <tt>META-INF/spring/*.xml</tt>) if available, null otherwise.
 * <p>
 * <p/> Additionally, this implementation allows custom locations to be
 * specified through {@link ConfigurationScanner} interface.
 *
 * @author Costin Leau
 * @see ConfigurationScanner
 */
public class DefaultOsgiApplicationContextCreator implements OsgiApplicationContextCreator {

    /**
     * logger
     */
    private static final Log log = LogFactory.getLog(DefaultOsgiApplicationContextCreator.class);

    private ConfigurationScanner configurationScanner;

    public DelegatedExecutionOsgiBundleApplicationContext createApplicationContext(final BundleContext bundleContext) {
        final Bundle bundle = bundleContext.getBundle();
        final boolean isUsingApplicationContextInitializer = SpringConfigUtils.isUsingApplicationContextInitializer(bundle);
        final ConfigurationScanner scanner;
        if (this.configurationScanner == null) {
            scanner = isUsingApplicationContextInitializer ? new ApplicationContextInitializerScanner() : new XmlConfigurationScanner();
        } else {
            scanner = this.configurationScanner;
        }
        final ApplicationContextConfiguration config = new ApplicationContextConfiguration(bundle, scanner);
        if (log.isTraceEnabled()) {
            log.trace("Created configuration " + config + " for bundle "
                    + OsgiStringUtils.nullSafeNameAndSymName(bundle));
        }

        // it's not a spring bundle, ignore it
        if (!config.isSpringPoweredBundle()) {
            return null;
        }

        log.info("Discovered configurations " + ObjectUtils.nullSafeToString(config.getConfigurationLocations())
                + " in bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "]");

        final DelegatedExecutionOsgiBundleApplicationContext sdoac = isUsingApplicationContextInitializer ? new OsgiBundleApplicationContext(config.getConfigurationLocations())
                : new OsgiBundleXmlApplicationContext(config.getConfigurationLocations());
        sdoac.setBundleContext(bundleContext);
        sdoac.setPublishContextAsService(config.isPublishContextAsService());
        return sdoac;
    }

    /**
     * Sets the configurationScanner used by this creator.
     *
     * @param configurationScanner The configurationScanner to set.
     */
    public void setConfigurationScanner(ConfigurationScanner configurationScanner) {
        Assert.notNull(configurationScanner);
        this.configurationScanner = configurationScanner;
    }
}