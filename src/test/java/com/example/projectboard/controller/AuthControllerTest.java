package com.example.projectboard.controller;

import com.example.projectboard.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@Import(SecurityConfig.class) // // Spring Security 추가 이후 permit all 설정과 관련된 정보를 불러오지 못하여 에러 발생, 이를 해결
@WebMvcTest
@DisplayName("View 컨트롤러 - 인증")
public class AuthControllerTest {
    private final MockMvc mvc;

    // 실제 코드에서는 @Autowired를 생략이 가능하지만 테스트 코드의 경우 명시해줘야함
    public AuthControllerTest(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @DisplayName("[view][GET} 로그인 페이지 - 정상 호출")
    @Test
    void givenNothing_whenTryingToLogin_thenReturnsLoginPage() throws Exception {
        // given

        // when & then
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML)) // 호환되는 타입까지 매칭 : text/html;charset=UTF-8
        ;
    }
}