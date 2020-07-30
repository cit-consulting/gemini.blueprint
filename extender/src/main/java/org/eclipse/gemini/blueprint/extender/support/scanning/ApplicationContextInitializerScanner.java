package org.eclipse.gemini.blueprint.extender.support.scanning;

import org.osgi.framework.Bundle;
import java.util.Enumeration;
import static org.eclipse.gemini.blueprint.util.SpringConfigUtils.INITIALIZER_HEADER_DELIMITER;
import static org.eclipse.gemini.blueprint.util.SpringConfigUtils.SPRING_CONTEXT_INITIALIZER_HEADER;

public class ApplicationContextInitializerScanner implements ConfigurationScanner {
    @Override
    public String[] getConfigurations(final Bundle bundle) {
        final Enumeration<String> keys = bundle.getHeaders().keys();
        while (keys.hasMoreElements()) {
            if (keys.nextElement().equals(SPRING_CONTEXT_INITIALIZER_HEADER)) {
                return bundle.getHeaders().get(SPRING_CONTEXT_INITIALIZER_HEADER).split(INITIALIZER_HEADER_DELIMITER);
            }
        }
        return null;
    }
}
