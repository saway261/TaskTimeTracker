package com.kiborisaway.tasktimetracker.config;

import com.kiborisaway.tasktimetracker.security.JsonAccessDeniedHandler;
import com.kiborisaway.tasktimetracker.security.JsonAuthenticationEntryPoint;
import com.kiborisaway.tasktimetracker.security.PasswordChangeRequiredAuthorizationManager;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SecurityConfig {

  private static final int BCRYPT_STRENGTH = 12;

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JsonAuthenticationEntryPoint authenticationEntryPoint,
      JsonAccessDeniedHandler accessDeniedHandler,
      SecurityContextRepository securityContextRepository,
      PasswordChangeRequiredAuthorizationManager passwordChangeRequiredAuthorizationManager)
      throws Exception {
    HttpSessionCsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();
    CsrfTokenRequestAttributeHandler csrfTokenRequestHandler =
        new CsrfTokenRequestAttributeHandler();

    http
        .cors(cors -> {
        })
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository)
            .csrfTokenRequestHandler(csrfTokenRequestHandler))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/auth/csrf").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login")
            .access(passwordChangeRequiredAuthorizationManager
                ::authorizePublicAuthenticationEndpoint)
            .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
            .requestMatchers(HttpMethod.PUT, "/auth/password").authenticated()
            .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
            // パスワード変更成功時に全セッションを失効させるため、古いPrincipalの
            // passwordChangeRequiredが変更後も残り続けることはありません。
            .anyRequest().access(passwordChangeRequiredAuthorizationManager))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler))
        .securityContext(context -> context
            .securityContextRepository(securityContextRepository)
            .requireExplicitSave(true))
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
        .httpBasic(basic -> basic.disable());

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
  CookieSerializer cookieSerializer() {
    DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
    cookieSerializer.setCookieName("JSESSIONID");
    cookieSerializer.setCookiePath("/api");
    cookieSerializer.setSameSite("Lax");
    return cookieSerializer;
  }

}
