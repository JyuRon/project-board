package com.example.projectboard.config;

import com.example.projectboard.dto.security.BoardPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@EnableJpaAuditing
@Configuration
public class JpaConfig {

    // 각 도메인에 수정자, 생성자를 입력할떄 사용되는 메소드
    @Bean
    public AuditorAware<String> auditorAware(){
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal) //UserDetailService 에서 구현한 구현체가 Object 형태로 리턴
//                .map(x -> (BoardPrincipal) x)
                .map(BoardPrincipal.class::cast)
                .map(BoardPrincipal::getUsername)
                ;
    }
}
