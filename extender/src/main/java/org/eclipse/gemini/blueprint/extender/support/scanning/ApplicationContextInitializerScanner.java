package org.eclipse.gemini.blueprint.extender.support.scanning;

import org.eclipse.gemini.blueprint.util.SpringConfigUtils;
import org.osgi.framework.Bundle;

public class ApplicationContextInitializerScanner implements ConfigurationScanner {
    @Override
    public String[] getConfigurations(final Bundle bundle) {
        return new String[]{
                SpringConfigUtils.getApplicationContextInitializerClass(bundle)
        };
    }
}
