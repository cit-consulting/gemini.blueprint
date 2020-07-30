/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.gemini.blueprint.extender.internal.activator.karaf;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleApplicationContextEvent;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleApplicationContextListener;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleContextFailedEvent;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleContextRefreshedEvent;
import org.eclipse.gemini.blueprint.extender.event.BootstrappingDependenciesEvent;
import org.eclipse.gemini.blueprint.extender.event.BootstrappingDependencyEvent;
import org.eclipse.gemini.blueprint.service.importer.OsgiServiceDependency;
import org.eclipse.gemini.blueprint.service.importer.event.OsgiServiceDependencyEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpringStateService
        implements OsgiBundleApplicationContextListener, BundleListener, BundleStateService {

    private static final Logger LOG = LoggerFactory.getLogger(SpringStateService.class);

    private final Map<Long, OsgiBundleApplicationContextEvent> states;

    public SpringStateService() {
        this.states = new ConcurrentHashMap<>();
    }

    public String getName() {
        return"Gemini Blueprint";
    }

    public BundleState getState(final Bundle bundle) {
        final OsgiBundleApplicationContextEvent event = states.get(bundle.getBundleId());
        final BundleState state = mapEventToState(event);
        return (bundle.getState() != Bundle.ACTIVE) ? BundleState.Unknown : state;
    }

    public String getDiag(final Bundle bundle) {
        final OsgiBundleApplicationContextEvent event = states.get(bundle.getBundleId());
        if (event == null) {
            return null;
        }

        final StringBuilder message = new StringBuilder();
        final Date date = new Date(event.getTimestamp());
        final SimpleDateFormat df = new SimpleDateFormat();
        message.append(df.format(date)).append("\n");
        if (event instanceof BootstrappingDependencyEvent) {
            message.append(getServiceInfo((BootstrappingDependencyEvent) event));
        }else if(event instanceof BootstrappingDependenciesEvent){
            message.append(getServiceInfo((BootstrappingDependenciesEvent) event));
        }
        final Throwable ex = getException(event);
        if (ex != null) {
            message.append("Exception: \n");
            addMessages(message, ex);
        }
        return message.toString();
    }

    private String getServiceInfo(final BootstrappingDependencyEvent event) {
        final OsgiServiceDependencyEvent depEvent = event.getDependencyEvent();
        if (depEvent == null || depEvent.getServiceDependency() == null) {
            return "";
        }
        final OsgiServiceDependency dep = depEvent.getServiceDependency();
        return String.format("Bean %s is waiting for OSGi service with filter %s",
                dep.getBeanName(),
                dep.getServiceFilter());
    }

    private String getServiceInfo(final BootstrappingDependenciesEvent event) {
        final StringBuilder builder = new StringBuilder();
        OsgiServiceDependency dep;
        for (OsgiServiceDependencyEvent dependencyEvent : event.getDependencyEvents()) {
            if (dependencyEvent == null || dependencyEvent.getServiceDependency() == null) {
                continue;
            }
            dep = dependencyEvent.getServiceDependency();
            builder.append(String.format("Bean %s is waiting for OSGi service with filter %s",
                    dep.getBeanName(),
                    dep.getServiceFilter()));
        }
        return builder.toString();
    }

    private void addMessages(final StringBuilder message, final Throwable ex) {
        if (ex != null) {
            message.append(ex.getMessage());
            message.append("\n");
            final StringWriter errorWriter = new StringWriter();
            ex.printStackTrace(new PrintWriter(errorWriter));
            message.append(errorWriter);
            message.append("\n");
        }
    }

    private Throwable getException(final OsgiBundleApplicationContextEvent event) {
        if (!(event instanceof OsgiBundleContextFailedEvent)) {
            return null;
        }
        return ((OsgiBundleContextFailedEvent) event).getFailureCause();
    }

    public void onOsgiApplicationEvent(final OsgiBundleApplicationContextEvent event) {
        if (LOG.isDebugEnabled()) {
            final BundleState state = mapEventToState(event);
            LOG.debug("Spring app state changed to " + state + " for bundle " + event.getBundle().getBundleId());
        }
        states.put(event.getBundle().getBundleId(), event);
    }

    private BundleState mapEventToState(final OsgiBundleApplicationContextEvent event) {
        if (event == null) {
            return BundleState.Unknown;
        } else if (event instanceof BootstrappingDependencyEvent || event instanceof BootstrappingDependenciesEvent) {
            return BundleState.Waiting;
        } else if (event instanceof OsgiBundleContextFailedEvent) {
            return BundleState.Failure;
        } else if (event instanceof OsgiBundleContextRefreshedEvent) {
            return BundleState.Active;
        } else {
            return BundleState.Unknown;
        }
    }

    public void bundleChanged(final BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED) {
            states.remove(event.getBundle().getBundleId());
        }
    }

}