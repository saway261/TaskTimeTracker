package com.kiborisaway.tasktimetracker.support;

import java.time.Clock;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@code @WebMvcTest} スライスは {@code SecurityConfig} を読み込まないため、
 * {@code @AuthenticationPrincipal} の解決に必要な {@link AuthenticationPrincipalArgumentResolver}
 * や、常駐フィルタ（{@code AbsoluteSessionTimeoutFilter} 等）が必要とする {@link Clock}
 * がスライス内に存在しない。各Controllerテストへ {@code @Import} して補う。
 */
@TestConfiguration
public class WebMvcTestSecuritySupportConfig implements WebMvcConfigurer {

  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new AuthenticationPrincipalArgumentResolver());
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("Content-Type", "X-CSRF-TOKEN")
        .allowCredentials(true);
  }
}
