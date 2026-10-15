package com.taskman.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Session-based authentication for the SPA.
 * <ul>
 * <li>Logging in stores the security context in the HTTP session; the browser keeps an
 * HttpOnly session cookie that scripts cannot read.</li>
 * <li>CSRF: the token is sent in a readable {@code XSRF-TOKEN} cookie and must come back
 * in the {@code X-XSRF-TOKEN} header, which Angular's HttpClient does automatically.</li>
 * <li>Security errors (401, 403) are handed to the MVC exception handlers, so they are
 * RFC 9457 problem details like every other error.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
			SecurityContextRepository securityContextRepository,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
		http.authorizeHttpRequests(requests -> requests
			.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login")
			.permitAll()
			.requestMatchers("/api/**")
			.authenticated()
			.anyRequest()
			.permitAll())
			.csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
			.securityContext(context -> context.securityContextRepository(securityContextRepository))
			.exceptionHandling(errors -> errors
				.authenticationEntryPoint((request, response, ex) -> exceptionResolver.resolveException(request,
						response, null, ex))
				.accessDeniedHandler(
						(request, response, ex) -> exceptionResolver.resolveException(request, response, null, ex)))
			.logout(logout -> logout.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			// An API: no login page, no browser basic-auth popup, no "redirect back after login".
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.requestCache(cache -> cache.requestCache(new NullRequestCache()));
		return http.build();
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		return CookieCsrfTokenRepository.withHttpOnlyFalse();
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	/**
	 * Applied by the login and register endpoints: a new session id (prevents session
	 * fixation) and a new CSRF token for the authenticated session.
	 */
	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
		return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),
				new CsrfAuthenticationStrategy(csrfTokenRepository)));
	}

	/** BCrypt by default; the "{bcrypt}" prefix in the hash allows changing algorithms later. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(UserPrincipalService users, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

}
