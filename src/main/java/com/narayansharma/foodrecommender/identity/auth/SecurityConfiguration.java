package com.narayansharma.foodrecommender.identity.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {
	@Bean
	SecurityFilterChain apiSecurityFilterChain(
			HttpSecurity http,
			ApiAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				.formLogin(formLogin -> formLogin.disable())
				.logout(logout -> logout.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
						.requestMatchers("/actuator/**").authenticated()
						.requestMatchers("/v1/**").authenticated()
						.anyRequest().permitAll())
				.build();
	}
}
