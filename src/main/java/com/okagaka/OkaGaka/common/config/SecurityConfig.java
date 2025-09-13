package com.okagaka.OkaGaka.common.config;

import com.google.api.JwtLocationOrBuilder;
import com.okagaka.OkaGaka.common.security.CustomUserDetails;
import com.okagaka.OkaGaka.common.security.CustomUserDetailsService;
import com.okagaka.OkaGaka.common.security.JwtAuthenticationFilter;
import com.okagaka.OkaGaka.common.security.JwtTokenProvider;
import com.okagaka.OkaGaka.common.exception.CustomAccessDeniedHandler;
import com.okagaka.OkaGaka.common.exception.CustomAuthenticationEntryPoint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy; // 세션 관리 정책 설정 시 필요
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 비밀번호 암호화 시 필요
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 암호화 시 필요
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${FRONTEND_ORIGIN}")
    private String frontendOrigin;

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(CustomAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler,
                          CustomUserDetailsService userDetailsService,
                          JwtTokenProvider jwtTokenProvider)
    {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        // 가장 먼저 웹소켓 경로를 허용
                        // 모든 permitAll 경로를 하나의 requestMatchers 블록에 통합
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/",
                                "/stomp_test.html",
                                "/vehicle-websocket-test.html",
                                "/vehicle-websocket-test2.html",
                                "vehicle2.html",
                                "/api/signup/**",
                                "/api/auth/login",
                                "/api/user-face-embedding-images",
//                                "/api/stt/*",
                                "/api/vehicles/*/location",
//                                "/sockjs-node/**",
                                "/ws-location/**",
//                                "/ws-location/*",
//                                "/ws-location/*/*",
//                                "/ws-location/info",
                                "/test.html",
                                "/webjars/**",
                                "/stomp_test2.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // JWT 인증 필터 등록 (WebSocket 경로 제외 처리)
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000", frontendOrigin)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
            // 웹소켓 CORS 설정 추가
            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new MappingJackson2HttpMessageConverter());
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 비밀번호 인코더
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder는 비밀번호를 안전하게 해싱하는 강력한 알고리즘입니다.
        return new BCryptPasswordEncoder();
    }



//
////                // 1.2. CSRF (Cross-Site Request Forgery) 보호 설정
////                //      REST API 개발 시 일반적으로 비활성화합니다.
////                //      토큰(JWT 등) 기반 인증을 사용하는 경우 세션 기반의 CSRF 보호는 불필요합니다.
////
////
////                // 1.3. 폼 로그인 설정
////                //      기본 스프링 시큐리티 로그인 폼을 사용하거나 커스텀 로그인 페이지를 사용할 때 설정합니다.
////                .formLogin(formLogin -> formLogin
////                        // .loginPage("/custom-login")       // 커스텀 로그인 페이지의 URL을 지정합니다. (기본은 /login)
////                        // .loginProcessingUrl("/authenticate") // 로그인 폼이 제출될 URL을 지정합니다. (기본은 /login)
////                        // .defaultSuccessUrl("/", true)    // 로그인 성공 시 리다이렉트될 기본 URL (항상 지정된 URL로 이동)
////                        // .failureUrl("/login?error")      // 로그인 실패 시 이동할 URL
////                        .permitAll() // 로그인 관련 페이지에 대한 접근을 모두 허용합니다.
////                )
////
////                // 1.4. 로그아웃 설정
////                .logout(logout -> logout
////                        .logoutUrl("/logout")              // 로그아웃을 처리할 URL을 지정합니다. (기본은 /logout)
////                        .logoutSuccessUrl("/login?logout") // 로그아웃 성공 시 리다이렉트될 URL
////                        .invalidateHttpSession(true)       // 세션 무효화
////                        .deleteCookies("JSESSIONID")       // 쿠키 삭제 (세션 기반인 경우)
////                        .permitAll() // 로그아웃 관련 페이지에 대한 접근을 모두 허용합니다.
////                )
////
////                // 1.5. 세션 관리 설정 (선택 사항, JWT 등 토큰 기반 인증 시 유용)
////                //      STATELESS: 서버가 세션을 생성하거나 사용하지 않습니다. (JWT 등 무상태 API에 적합)
////                //      IF_REQUIRED: 필요할 때만 세션을 생성합니다. (기본값)
////                //      ALWAYS: 항상 세션을 생성합니다.
////                //      NEVER: 세션을 생성하지 않지만, 기존 세션은 사용합니다.
////                .sessionManagement(session -> session
////                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 예시: 기본값 사용
////                )
//
////        // 1.6. 예외 처리 설정 (인증/인가 실패 시 동작 정의)
////        // .exceptionHandling(exceptions -> exceptions
////        //     .authenticationEntryPoint(new CustomAuthenticationEntryPoint()) // 인증되지 않은 사용자가 보호된 리소스 접근 시
////        //     .accessDeniedHandler(new CustomAccessDeniedHandler())         // 인증은 되었지만 권한이 없는 사용자가 접근 시
////        // )
////
////        // 1.7. 기타 보안 헤더 설정 (기본값으로 대부분 활성화되어 있음)
////        // .headers(headers -> headers
////        //     .frameOptions(frameOptions -> frameOptions.sameOrigin()) // X-Frame-Options (Clickjacking 방어)
////        // );
////        ; // HttpSecurity 설정 체인 종료
//
//        http.addFilterBefore(
//                new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
////                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
//                UsernamePasswordAuthenticationFilter.class);
//
//
//        return http.build(); // SecurityFilterChain 빌드
//    }
//
//

//
////    @Bean
////    public CorsConfigurationSource corsConfigurationSource() {
////        CorsConfiguration configuration = new CorsConfiguration();
////        configuration.setAllowedOrigins(List.of(
////                "http://localhost:3000",
////                frontendOrigin
////        )); // 프론트 주소
////        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
////        configuration.setAllowedHeaders(List.of("*"));
////        configuration.setAllowCredentials(true); // 인증 필요한 경우 true
////
////        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
////        source.registerCorsConfiguration("/**", configuration);
////        return source;
////    }
//
//    @Bean
//    public WebMvcConfigurer corsConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(CorsRegistry registry) {
//                registry.addMapping("/**")
//                        .allowedOrigins("http://localhost:3000", frontendOrigin)
//                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                        .allowedHeaders("*")
//                        .allowCredentials(true)
//                        .maxAge(3600);
//            }
//        };
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
//        return configuration.getAuthenticationManager();
//    }



}
