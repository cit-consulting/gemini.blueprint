package org.eclipse.gemini.blueprint.util;

import org.osgi.framework.Bundle;

import java.util.Enumeration;

public final class SpringConfigUtils {

    public static final String SPRING_CONTEXT_INITIALIZER_HEADER = "Spring-Context-Initializer";

    public static boolean isUsingApplicationContextInitializer(final Bundle bundle) {
        final Enumeration<String> keys = bundle.getHeaders().keys();
        while (keys.hasMoreElements()) {
            if (keys.nextElement().equals(SPRING_CONTEXT_INITIALIZER_HEADER)) {
                return true;
            }
        }
        return false;
    }

    public static String getApplicationContextInitializerClass(final Bundle bundle) {
        final Enumeration<String> keys = bundle.getHeaders().keys();
        while (keys.hasMoreElements()) {
            if (keys.nextElement().equals(SPRING_CONTEXT_INITIALIZER_HEADER)) {
                return bundle.getHeaders().get(SPRING_CONTEXT_INITIALIZER_HEADER);
            }
        }
        return null;
    }


}
