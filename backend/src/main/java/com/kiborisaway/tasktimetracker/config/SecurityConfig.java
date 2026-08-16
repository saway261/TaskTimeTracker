package com.kiborisaway.tasktimetracker.config;

import com.kiborisaway.tasktimetracker.security.AbsoluteSessionTimeoutFilter;
import com.kiborisaway.tasktimetracker.security.JsonAccessDeniedHandler;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.security.RestrictedAccountAuthorizationManager;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  private static final int BCRYPT_STRENGTH = 12;

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JsonAuthenticationEntryPoint authenticationEntryPoint,
      JsonAccessDeniedHandler accessDeniedHandler,
      SecurityContextRepository securityContextRepository,
      AbsoluteSessionTimeoutFilter absoluteSessionTimeoutFilter,
      RestrictedAccountAuthorizationManager restrictedAccountAuthorizationManager)
      throws Exception {
    HttpSessionCsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();
    CsrfTokenRequestAttributeHandler csrfTokenRequestHandler =
        new CsrfTokenRequestAttributeHandler();

    http
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository)
            .csrfTokenRequestHandler(csrfTokenRequestHandler))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/auth/csrf").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login")
            .access(restrictedAccountAuthorizationManager
                ::authorizePublicAuthenticationEndpoint)
            .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
            .requestMatchers(HttpMethod.PUT, "/auth/password").authenticated()
            .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
            .requestMatchers(HttpMethod.POST, "/auth/email-verifications").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/email-verifications/resend")
            .authenticated()
            .requestMatchers(HttpMethod.PUT, "/auth/email").authenticated()
            .requestMatchers(HttpMethod.POST, "/auth/email-changes").permitAll()
            // パスワード変更・メールアドレス確認完了時に全セッションを失効させるため、古いPrincipalの
            // passwordChangeRequired/emailVerifiedが変更後も残り続けることはありません。
            .anyRequest().access(restrictedAccountAuthorizationManager))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler))
        .securityContext(context -> context
            .securityContextRepository(securityContextRepository)
            .requireExplicitSave(true))
        .addFilterAfter(absoluteSessionTimeoutFilter, SecurityContextHolderFilter.class)
        .logout(logout -> logout
            .logoutUrl("/auth/logout")
            .invalidateHttpSession(true)
            .clearAuthentication(true)
            .deleteCookies("JSESSIONID")
            .logoutSuccessHandler((request, response, authentication) -> {
              if (authentication == null) {
                authenticationEntryPoint.commence(request, response,
                    new InsufficientAuthenticationException("authentication required"));
                return;
              }
              response.setStatus(204);
            }))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .headers(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    Map<String, PasswordEncoder> encoders = new LinkedHashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder(BCRYPT_STRENGTH));
    return new DelegatingPasswordEncoder("bcrypt", encoders);
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  CookieSerializer cookieSerializer(
      @Value("${server.servlet.session.cookie.secure:true}") boolean secure,
      @Value("${server.servlet.session.cookie.max-age:30d}") Duration maxAge) {
    DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
    cookieSerializer.setCookieName("JSESSIONID");
    cookieSerializer.setCookiePath("/api");
    cookieSerializer.setSameSite("Lax");
    cookieSerializer.setUseHttpOnlyCookie(true);
    cookieSerializer.setUseSecureCookie(secure);
    cookieSerializer.setCookieMaxAge(Math.toIntExact(maxAge.toSeconds()));
    return cookieSerializer;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins:http://localhost:5173}") String origins) {
    List<String> allowedOrigins = Arrays.stream(origins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toList();
    if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
      throw new IllegalArgumentException(
          "app.cors.allowed-origins must contain exact origins and cannot contain '*'");
    }

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(Duration.ofHours(1));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

}
