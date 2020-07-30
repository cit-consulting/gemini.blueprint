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

import org.apache.karaf.bundle.core.BundleStateService;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleApplicationContextListener;
import org.eclipse.gemini.blueprint.util.BundleDelegatingClassLoader;
import org.eclipse.gemini.blueprint.util.OsgiServiceUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class EventActivator implements BundleActivator {

    volatile ServiceRegistration<?> serviceRegistration;

    public void start(final BundleContext bundleContext) {
        Class<?> aClass;
        try {
            aClass = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundleContext.getBundle()).loadClass("org.apache.karaf.bundle.core.BundleStateService");
        } catch (ClassNotFoundException e) {
            aClass = null;
        }
        if (aClass != null) {
            final SpringStateService services = new SpringStateService();
            String[] classes = new String[]{
                    OsgiBundleApplicationContextListener.class.getName(),
                    BundleStateService.class.getName()
            };
            serviceRegistration = bundleContext.registerService(classes, services, null);
        }
    }

    public void stop(final BundleContext context) {
        OsgiServiceUtils.unregisterService(serviceRegistration);
    }

}
