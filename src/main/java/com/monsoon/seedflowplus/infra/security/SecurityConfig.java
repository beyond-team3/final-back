package com.monsoon.seedflowplus.infra.security;

import com.monsoon.seedflowplus.core.common.support.error.RestAccessDeniedHandler;
import com.monsoon.seedflowplus.core.common.support.error.RestAuthenticationEntryPoint;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(
            JwtTokenProvider jwtTokenProvider,
            @org.springframework.context.annotation.Lazy UserDetailsService userDetailsService,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(EndpointRequest.to("health", "prometheus")).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/clients/*/crops").hasRole("SALES_REP")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/accounts/clients/crops/*").hasRole("SALES_REP")
                        .requestMatchers("/api/v1/accounts/clients/register", "/api/v1/accounts/employees/register",
                                "/api/v1/accounts/users/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/accounts/clients/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/accounts/employees/*").hasRole("ADMIN")
                        .requestMatchers("/api/v1/notes/**").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/clients/*/trade-summary")
                        .hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers("/api/v1/scoring/**").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers("/api/v1/statistics/billing/revenue/**").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers("/api/v2/statistics/billing/revenue/**").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/statistics/sales-rep").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/statistics/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/statistics/by-employee").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/statistics/by-client").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/statistics/by-variety").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/statistics/ranking").hasAnyRole("SALES_REP", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v2/quotations").hasRole("SALES_REP")
                        .requestMatchers(HttpMethod.POST, "/api/v2/quotations/*/revise").hasRole("SALES_REP")
                        .requestMatchers(HttpMethod.PATCH, "/api/v2/quotations/*/cancel").hasRole("SALES_REP")
                        .requestMatchers(HttpMethod.POST, "/api/v2/contracts").hasRole("SALES_REP")
                        .requestMatchers(HttpMethod.POST, "/api/v2/contracts/*/revise").hasRole("SALES_REP")
                        .requestMatchers(HttpMethod.PATCH, "/api/v2/contracts/*/cancel").hasRole("SALES_REP")
                        .requestMatchers(HttpMethod.POST, "/api/v1/approvals").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/invoices/contracts/*/manual-draft").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/calendar/**").hasRole("SALES_REP")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "https://monsoonseed.com",
                "https://www.monsoonseed.com",
                "http://localhost:5173",
                "http://localhost:8000",
                "http://localhost:30090"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
