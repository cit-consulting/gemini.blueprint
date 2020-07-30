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

package org.eclipse.gemini.blueprint.extender.internal.boot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.gemini.blueprint.extender.internal.activator.ContextLoaderListener;
import org.eclipse.gemini.blueprint.extender.internal.activator.JavaBeansCacheActivator;
import org.eclipse.gemini.blueprint.extender.internal.activator.ListenerServiceActivator;
import org.eclipse.gemini.blueprint.extender.internal.activator.LoggingActivator;
import org.eclipse.gemini.blueprint.extender.internal.activator.karaf.EventActivator;
import org.eclipse.gemini.blueprint.extender.internal.blueprint.activator.BlueprintLoaderListener;
import org.eclipse.gemini.blueprint.extender.internal.support.ExtenderConfiguration;
import org.eclipse.gemini.blueprint.util.OsgiPlatformDetector;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Bundle activator that simply the lifecycle callbacks to other activators.
 *
 * @author Costin Leau
 */
public final class ChainActivator implements BundleActivator {
    private final Log log = LogFactory.getLog(getClass());

    private static final String CHAIN_ACTIVATOR_PID = "org.eclipse.gemini.extender";
    private static final String LISTEN_BLUEPRINT_BUNDLES = "listen.blueprint.bundles";
    private static final String LISTEN_SPRING_BUNDLES = "listen.spring.bundles";

    private final Properties configuration = new Properties();


    private static final boolean BLUEPRINT_AVAILABLE =
            ClassUtils.isPresent("org.osgi.service.blueprint.container.BlueprintContainer", ChainActivator.class
                    .getClassLoader());

    private final BundleActivator[] CHAIN;

    public ChainActivator() {

        configuration.put(LISTEN_BLUEPRINT_BUNDLES, "true");
        configuration.put(LISTEN_SPRING_BUNDLES, "true");

        final LoggingActivator logStatus = new LoggingActivator();
        final JavaBeansCacheActivator activateJavaBeansCache = new JavaBeansCacheActivator();
        final ExtenderConfiguration initializeExtenderConfiguration = new ExtenderConfiguration();
        final ListenerServiceActivator activateListeners = new ListenerServiceActivator(initializeExtenderConfiguration);
        final ContextLoaderListener listenForSpringDmBundles = new ContextLoaderListener(initializeExtenderConfiguration);
        final BlueprintLoaderListener listenForBlueprintBundles = new BlueprintLoaderListener(initializeExtenderConfiguration, activateListeners);
        final EventActivator springEventActivator = new EventActivator();

        if (OsgiPlatformDetector.isR42()) {
            if (BLUEPRINT_AVAILABLE) {
                log.info("Blueprint API detected; enabling Blueprint Container functionality");
                CHAIN = new BundleActivator[]{
                        logStatus,
                        activateJavaBeansCache,
                        initializeExtenderConfiguration,
                        activateListeners,
                        listenForSpringDmBundles,
                        springEventActivator,
                        listenForBlueprintBundles
                };
            } else {
                log.warn("Blueprint API not found; disabling Blueprint Container functionality");
                CHAIN = new BundleActivator[]{
                        logStatus,
                        activateJavaBeansCache,
                        initializeExtenderConfiguration,
                        activateListeners,
                        listenForSpringDmBundles,
                        springEventActivator
                };
            }
        } else {
            log.warn("Pre-4.2 OSGi platform detected; disabling Blueprint Container functionality");
            CHAIN = new BundleActivator[]{
                    logStatus,
                    activateJavaBeansCache,
                    initializeExtenderConfiguration,
                    activateListeners,
                    listenForSpringDmBundles,
                    springEventActivator
            };
        }
    }

    private void updateConfiguration(final BundleContext context) throws IOException {
        final ConfigurationAdmin service = context.getService(context.getServiceReference(ConfigurationAdmin.class));
        final Configuration configuration = service.getConfiguration(CHAIN_ACTIVATOR_PID);
        if (configuration == null) {
            return;
        }
        final Enumeration<String> keys = configuration.getProperties().keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            this.configuration.put(key, configuration.getProperties().get(key));
        }
    }

    public void start(final BundleContext context) throws Exception {
        updateConfiguration(context);

        for (BundleActivator bundleActivator : CHAIN) {
            if (bundleActivator instanceof BlueprintLoaderListener) {
                if (Boolean.parseBoolean((String) configuration.get(LISTEN_BLUEPRINT_BUNDLES))) {
                    bundleActivator.start(context);
                } else {
                    log.debug("Ignoring Blueprint Container functionality due configuration");
                }
            } else if (bundleActivator instanceof ContextLoaderListener) {
                if (Boolean.parseBoolean((String) configuration.get(LISTEN_SPRING_BUNDLES))) {
                    bundleActivator.start(context);
                } else {
                    log.debug("Ignoring Spring Container functionality due configuration");
                }
            } else if (bundleActivator instanceof EventActivator) {
                if (Boolean.parseBoolean((String) configuration.get(LISTEN_SPRING_BUNDLES))) {
                    bundleActivator.start(context);
                } else {
                    log.debug("Ignoring Spring Container functionality due configuration");
                }
            } else {
                bundleActivator.start(context);
            }
        }
    }

    public void stop(final BundleContext context) throws Exception {
        for (int i = CHAIN.length - 1; i >= 0; i--) {
            if (CHAIN[i] instanceof BlueprintLoaderListener) {
                if (Boolean.parseBoolean((String) configuration.get(LISTEN_BLUEPRINT_BUNDLES))) {
                    CHAIN[i].stop(context);
                }
            } else if (CHAIN[i] instanceof ContextLoaderListener) {
                if (Boolean.parseBoolean((String) configuration.get(LISTEN_SPRING_BUNDLES))) {
                    CHAIN[i].stop(context);
                }
            } else if (CHAIN[i] instanceof EventActivator) {
                if (Boolean.parseBoolean((String) configuration.get(LISTEN_SPRING_BUNDLES))) {
                    CHAIN[i].stop(context);
                }
            } else {
                CHAIN[i].stop(context);
            }
        }
    }
}
