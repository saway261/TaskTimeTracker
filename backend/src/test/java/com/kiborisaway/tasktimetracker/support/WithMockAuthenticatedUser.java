package com.kiborisaway.tasktimetracker.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * テストにおいて {@link com.kiborisaway.tasktimetracker.security.AuthenticatedUser} をPrincipalとして
 * SecurityContextへ設定するアノテーションです。 {@code @WithMockUser} は {@code User} 型のPrincipalを注入するため、
 * {@code @AuthenticationPrincipal AuthenticatedUser} の解決に失敗します。このアノテーションはその代替です。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithMockAuthenticatedUserSecurityContextFactory.class)
public @interface WithMockAuthenticatedUser {

  int userId() default 1;

  String email() default "user1@example.com";

  boolean passwordChangeRequired() default false;

  boolean emailVerified() default true;
}
