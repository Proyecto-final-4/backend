package com.backend.backend.shared.crypto;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Puente estático hacia el {@link ApplicationContext} de Spring. Permite que componentes no
 * gestionados por Spring (como los {@code AttributeConverter} de JPA) accedan a beans del
 * contenedor durante la conversión de columnas.
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
