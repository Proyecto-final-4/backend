package com.backend.backend.shared.crypto;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Static bridge to Spring's {@link ApplicationContext}. Allows components not managed by Spring
 * (such as JPA {@code AttributeConverter}s) to access beans from the container during column
 * conversion.
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }
}
