package com.monsoon.seedflowplus.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/approvals").hasRole("ADMIN")
                        .requestMatchers("/api/v1/statistics/billing/revenue/**").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers("/api/v1/statistics/sales-rep").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers("/api/v1/statistics/admin").hasRole("ADMIN")
                        .requestMatchers("/api/v1/statistics/by-employee").hasRole("ADMIN")
                        .requestMatchers("/api/v1/statistics/by-client").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers("/api/v1/statistics/by-variety").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers("/api/v1/statistics/ranking").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v2/quotations").hasRole("SALES_REP")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v2/quotations/*/revise").hasRole("SALES_REP")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v2/quotations/*/cancel").hasRole("SALES_REP")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v2/contracts").hasRole("SALES_REP")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v2/contracts/*/revise").hasRole("SALES_REP")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v2/contracts/*/cancel").hasRole("SALES_REP")
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())));

        return http.build();
    }
}
