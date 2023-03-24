package com.example.projectboard.config;

import com.example.projectboard.domain.UserAccount;
import com.example.projectboard.repository.UserAccountRepository;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.annotation.BeforeTestMethod;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;


@Import(SecurityConfig.class)
public class TestSecurityConfig {

    @MockBean
    private UserAccountRepository userAccountRepository;

    // Spring에서 제공하는 테스트 메서드로 스프링 관련(인증) 테스트를 진행할때만 사용 가능
    @BeforeTestMethod
    public void SecuritySetup(){
        given(userAccountRepository.findById(anyString()))
                .willReturn(Optional.of(UserAccount.of(
                        "jyukaTest",
                        "pw",
                        "jyuka-test@mail.com",
                        "jyuka-test",
                        "test memo"
                )));

    }
}
