package com.kiborisaway.tasktimetracker.support;

import com.kiborisaway.tasktimetracker.publicid.PublicIdCodec;
import com.kiborisaway.tasktimetracker.publicid.jackson.PublicIdJacksonConfig;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@code @WebMvcTest} スライスは {@code SecurityConfig} を読み込まないため、
 * {@code @AuthenticationPrincipal} の解決に必要な {@link AuthenticationPrincipalArgumentResolver}
 * や、常駐フィルタ（{@code AbsoluteSessionTimeoutFilter} 等）が必要とする {@link Clock}
 * がスライス内に存在しない。各Controllerテストへ {@code @Import} して補う。
 *
 * <p>公開IDの {@link PublicIdCodec} と {@link PublicIdJacksonConfig} も同様にスライス外である。
 * {@code Converter} 自体はスライスに含まれるが、その依存である Codec は含まれないため、
 * ここで取り込む。全Controllerテストがこの設定を {@code @Import} しているので、
 * 1箇所の追加で全体へ行き渡る。
 */
@TestConfiguration
@Import({PublicIdCodec.class, PublicIdJacksonConfig.class})
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
