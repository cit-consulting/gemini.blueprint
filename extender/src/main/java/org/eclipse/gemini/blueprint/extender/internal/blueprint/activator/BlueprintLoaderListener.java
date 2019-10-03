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

package org.eclipse.gemini.blueprint.extender.internal.blueprint.activator;

import org.eclipse.gemini.blueprint.extender.OsgiApplicationContextCreator;
import org.eclipse.gemini.blueprint.extender.internal.activator.*;
import org.eclipse.gemini.blueprint.extender.internal.blueprint.activator.support.BlueprintContainerConfig;
import org.eclipse.gemini.blueprint.extender.internal.blueprint.activator.support.BlueprintContainerCreator;
import org.eclipse.gemini.blueprint.extender.internal.blueprint.event.EventAdminDispatcher;
import org.eclipse.gemini.blueprint.extender.internal.support.ExtenderConfiguration;
import org.eclipse.gemini.blueprint.extender.support.ApplicationContextConfiguration;
import org.eclipse.gemini.blueprint.util.BlueprintConfigUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * RFC124 extension to the Spring DM extender.
 * 
 * @author Costin Leau
 */
public class BlueprintLoaderListener extends ContextLoaderListener {
    private volatile BlueprintListenerManager listenerManager;
    private volatile BlueprintContainerProcessor contextProcessor;
	private volatile TypeCompatibilityChecker typeChecker;
    private ListenerServiceActivator listenerServiceActivator;

    public BlueprintLoaderListener(ExtenderConfiguration extenderConfiguration, ListenerServiceActivator listenerServiceActivator) {
        super(extenderConfiguration);
        this.listenerServiceActivator = listenerServiceActivator;
    }

    @Override
	public void start(BundleContext context) throws Exception {
		this.listenerManager = new BlueprintListenerManager(context);
        EventAdminDispatcher dispatcher = new EventAdminDispatcher(context);
        Bundle bundle = context.getBundle();
		this.contextProcessor = new BlueprintContainerProcessor(dispatcher, listenerManager, bundle);
		this.typeChecker = new BlueprintTypeCompatibilityChecker(bundle);
        this.listenerServiceActivator.getMulticaster().addApplicationListener(this.contextProcessor);
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		listenerManager.destroy();
	}

	@Override
	protected ApplicationContextConfigurationFactory createContextConfigFactory() {
		return new ApplicationContextConfigurationFactory() {

			public ApplicationContextConfiguration createConfiguration(Bundle bundle) {
				return new BlueprintContainerConfig(bundle);
			}
		};
	}

    /**
     * Always use the {@link BlueprintContainerCreator}, never the configured creator.
     * Rationale: Backwards compatibility. Both DM and Blueprint extenders are available simultaneously,
     * however Blueprint extender support is new and must not be broken by existing configurations. Otherwise, existing
     * users would have to make their creators aware of the difference between blueprint and dm containers.
     */
    @Override
    protected OsgiApplicationContextCreator getOsgiApplicationContextCreator() {
        return new BlueprintContainerCreator();
    }

    @Override
	protected OsgiContextProcessor createContextProcessor() {
		return contextProcessor;
	}

	@Override
	protected TypeCompatibilityChecker getTypeCompatibilityChecker() {
		return typeChecker;
	}

	@Override
	protected String getManagedBundleExtenderVersionHeader() {
		return BlueprintConfigUtils.EXTENDER_VERSION;
	}
}