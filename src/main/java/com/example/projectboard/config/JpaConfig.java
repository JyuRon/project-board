package com.example.projectboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@EnableJpaAuditing
@Configuration
public class JpaConfig {

    //TODO : 스프링 시큐리티로 인증 기능을 붙이게 될 떄, 수정 예정
    // 각 도메인에 수정자, 생성자를 입력할떄 사용되는 메소드
    @Bean
    public AuditorAware<String> auditorAware(){
        return () -> Optional.of("jyuka");
    }
}
