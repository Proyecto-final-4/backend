package com.backend.backend.user.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or type as restricted to users with the {@code ADMIN} role.
 *
 * <p>This is a meta-annotation placeholder designed to be backed by Spring Security's
 * {@code @PreAuthorize("hasRole('ADMIN')")} in a future security PR. Once {@code
 * spring-boot-starter-security} is added and {@code @EnableMethodSecurity} is configured, compose
 * this annotation with {@code @PreAuthorize} to activate enforcement.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminOnly {}
