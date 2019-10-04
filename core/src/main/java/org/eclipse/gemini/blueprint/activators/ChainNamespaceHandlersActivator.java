package org.eclipse.gemini.blueprint.activators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.gemini.blueprint.util.OsgiPlatformDetector;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.springframework.util.ClassUtils;

public class ChainNamespaceHandlersActivator implements BundleActivator {

    protected final Log log = LogFactory.getLog(getClass());

    private static final boolean BLUEPRINT_AVAILABLE =
            ClassUtils.isPresent("org.osgi.service.blueprint.container.BlueprintContainer", ChainNamespaceHandlersActivator.class
                    .getClassLoader());

    private final BundleActivator[] CHAIN;

    public ChainNamespaceHandlersActivator() {
        final NamespaceHandlerActivator activateCustomNamespaceHandling = new NamespaceHandlerActivator();
        final NamespaceHandlerActivator activateBlueprintspecificNamespaceHandling = new BlueprintNamespaceHandlerActivator();

        if (OsgiPlatformDetector.isR42()) {
            if (BLUEPRINT_AVAILABLE) {
                log.info("Blueprint API detected; enabling Blueprint Container functionality");
                CHAIN = new BundleActivator[] {
                        activateCustomNamespaceHandling,
                        activateBlueprintspecificNamespaceHandling
                };
            }
            else {
                log.warn("Blueprint API not found; disabling Blueprint Container functionality");
                CHAIN = new BundleActivator[] {
                        activateCustomNamespaceHandling
                };
            }
        } else {
            log.warn("Pre-4.2 OSGi platform detected; disabling Blueprint Container functionality");
            CHAIN = new BundleActivator[] {
                    activateCustomNamespaceHandling
            };
        }
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        for (int i = 0; i < CHAIN.length; i++) {
            CHAIN[i].start(context);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        for (int i = CHAIN.length - 1; i >= 0; i--) {
            CHAIN[i].stop(context);
        }
    }
}
