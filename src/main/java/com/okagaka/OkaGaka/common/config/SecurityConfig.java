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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy; // 세션 관리 정책 설정 시 필요
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 비밀번호 암호화 시 필요
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 암호화 시 필요
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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


    // HTTP 요청에 대한 보안 규칙 정의
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                // 요청에 대한 인가 (Authorization) 규칙 설정
                .authorizeHttpRequests(authorize -> authorize
                       // 로그인 없이 접근을 허용할 경로
                        .requestMatchers(
                                "/",
                                "/stomp_test.html",
                                "/api/signup/**",
                                "/api/auth/login",
                                "/api/stt/*",
                                "/ws-location/**"
//                                "/api/groups/**",
                        ).permitAll() // 위의 경로들은 인증 없이 모두 허용합니다.

                        // 그 외 모든 요청은 인증(로그인)된 사용자만 접근 허용
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

//                // 1.2. CSRF (Cross-Site Request Forgery) 보호 설정
//                //      REST API 개발 시 일반적으로 비활성화합니다.
//                //      토큰(JWT 등) 기반 인증을 사용하는 경우 세션 기반의 CSRF 보호는 불필요합니다.
//
//
//                // 1.3. 폼 로그인 설정
//                //      기본 스프링 시큐리티 로그인 폼을 사용하거나 커스텀 로그인 페이지를 사용할 때 설정합니다.
//                .formLogin(formLogin -> formLogin
//                        // .loginPage("/custom-login")       // 커스텀 로그인 페이지의 URL을 지정합니다. (기본은 /login)
//                        // .loginProcessingUrl("/authenticate") // 로그인 폼이 제출될 URL을 지정합니다. (기본은 /login)
//                        // .defaultSuccessUrl("/", true)    // 로그인 성공 시 리다이렉트될 기본 URL (항상 지정된 URL로 이동)
//                        // .failureUrl("/login?error")      // 로그인 실패 시 이동할 URL
//                        .permitAll() // 로그인 관련 페이지에 대한 접근을 모두 허용합니다.
//                )
//
//                // 1.4. 로그아웃 설정
//                .logout(logout -> logout
//                        .logoutUrl("/logout")              // 로그아웃을 처리할 URL을 지정합니다. (기본은 /logout)
//                        .logoutSuccessUrl("/login?logout") // 로그아웃 성공 시 리다이렉트될 URL
//                        .invalidateHttpSession(true)       // 세션 무효화
//                        .deleteCookies("JSESSIONID")       // 쿠키 삭제 (세션 기반인 경우)
//                        .permitAll() // 로그아웃 관련 페이지에 대한 접근을 모두 허용합니다.
//                )
//
//                // 1.5. 세션 관리 설정 (선택 사항, JWT 등 토큰 기반 인증 시 유용)
//                //      STATELESS: 서버가 세션을 생성하거나 사용하지 않습니다. (JWT 등 무상태 API에 적합)
//                //      IF_REQUIRED: 필요할 때만 세션을 생성합니다. (기본값)
//                //      ALWAYS: 항상 세션을 생성합니다.
//                //      NEVER: 세션을 생성하지 않지만, 기존 세션은 사용합니다.
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 예시: 기본값 사용
//                )

//        // 1.6. 예외 처리 설정 (인증/인가 실패 시 동작 정의)
//        // .exceptionHandling(exceptions -> exceptions
//        //     .authenticationEntryPoint(new CustomAuthenticationEntryPoint()) // 인증되지 않은 사용자가 보호된 리소스 접근 시
//        //     .accessDeniedHandler(new CustomAccessDeniedHandler())         // 인증은 되었지만 권한이 없는 사용자가 접근 시
//        // )
//
//        // 1.7. 기타 보안 헤더 설정 (기본값으로 대부분 활성화되어 있음)
//        // .headers(headers -> headers
//        //     .frameOptions(frameOptions -> frameOptions.sameOrigin()) // X-Frame-Options (Clickjacking 방어)
//        // );
//        ; // HttpSecurity 설정 체인 종료

        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
        );

        return http.build(); // SecurityFilterChain 빌드
    }


//    // 비밀번호 인코더
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        // BCryptPasswordEncoder는 비밀번호를 안전하게 해싱하는 강력한 알고리즘입니다.
//        return new BCryptPasswordEncoder();
//    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

}
