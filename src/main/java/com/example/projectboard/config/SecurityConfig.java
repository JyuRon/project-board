package com.example.projectboard.config;

import com.example.projectboard.dto.UserAccountDto;
import com.example.projectboard.dto.security.BoardPrincipal;
import com.example.projectboard.dto.security.KakaoOAuth2Response;
import com.example.projectboard.repository.UserAccountRepository;
import com.example.projectboard.service.UserAccountService;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig{
    // https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService
    ) throws Exception {
        /**
         * 람다식의 경우 Customizer<?> 의 형태로 감싸져 있어서 사용이 가능하다
         * 람다식이 아닌 체이닝 메소드로 계속 이어 붙이게 된다면 구분을 and()로 하는 등 코드량이 많아지게 된다.
         */
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .mvcMatchers("/api/**").permitAll()
                        .mvcMatchers(
                                HttpMethod.GET,
                                "/",
                                "/articles",
                                "/articles/search-hashtag"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .oauth2Login(oAuth -> oAuth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                )
                .csrf(csrf -> csrf.ignoringAntMatchers("/api/**"))
                .build();
    }

    /**
     * ignore 를 설정하는 순간 스프링 시큐리티의 기본 설정 등 컨텍스트의 관리에서 벗어나게 된다.
     * 이로 인해 css, javascript 에서 동작하는 공격 및 csrf 에 취약하여 권장되지 않는다.
     * SecurityFilterChain 의 permitAll 를 사용하는 것을 권장한다.
     * You are asking Spring Security to ignore org.springframework.boot.autoconfigure.security.servlet.StaticResourceRequest$StaticResourceRequestMatcher@74b7fb89.
     * This is not recommended -- please use permitAll via HttpSecurity#authorizeHttpRequests instead.
     */
//    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // static resource --> css, js
//        return (web) -> web.ignoring().antMatchers("/css");
        return (web) -> web.ignoring().requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }


    /**
     * UserDetailsService Functional Interface 람다식으로 구현
     * @return BoardPrincipal implements UserDetails
     */
    @Bean
    public UserDetailsService userDetailsService(UserAccountService userAccountService){

        return username -> userAccountService.searchUser(username)
                .map(BoardPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다 - username : " + username))
                ;
    }

    /**
     * <p>
     * OAuth 2.0 기술을 이용한 인증 정보를 처리한다.
     * 카카오 인증 방식을 선택.
     *
     * <p>
     * TODO: 카카오 도메인에 결합되어 있는 코드. 확장을 고려하면 별도 인증 처리 서비스 클래스로 분리하는 것이 좋지만, 현재 다른 OAuth 인증 플랫폼을 사용할 예정이 없어 이렇게 마무리한다.
     *
     * @param userAccountService  게시판 서비스의 사용자 계정을 다루는 서비스 로직
     * @param passwordEncoder 패스워드 암호화 도구
     * @return {@link OAuth2UserService} OAuth2 인증 사용자 정보를 읽어들이고 처리하는 서비스 인스턴스 반환
     */
    /**
     * Spring Security Context Holder 에 저장할 유저 정보를 반환한다.
     * @return U extends OAuth2User
     */
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService(
            UserAccountService userAccountService,
            PasswordEncoder passwordEncoder
    ) {
        final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        return userRequest -> {
            OAuth2User oAuth2User = delegate.loadUser(userRequest);

            KakaoOAuth2Response kakaoResponse = KakaoOAuth2Response.from(oAuth2User.getAttributes());
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            String providerId = String.valueOf(kakaoResponse.id());
            String username = registrationId + "_" + providerId; // kakao_123456
            String dummyPassword = passwordEncoder.encode("{bcrypt}" + UUID.randomUUID());

            /**
             * Optional orElse 와 orElseGet 의 차이점
             * https://ysjune.github.io/posts/java/orelsenorelseget/
             * orElse 가 사용되지 않은 이유로는 매개변수가 메소드인 경우 orElse 의 인자로 받을때 한번 실행되게 된다.
             * 즉 메소드를 매개변수로 사용하고 싶다면 orElse 가 아닌 orElseGet 사용이 바람직하다.
             */
            return userAccountService.searchUser(username)
                    .map(BoardPrincipal::from)
                    .orElseGet(() ->
                            BoardPrincipal.from(
                                    userAccountService.saveUser(
                                            username,
                                            dummyPassword,
                                            kakaoResponse.email(),
                                            kakaoResponse.nickname(),
                                            null
                                    )
                            )
                    );
        };

    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
