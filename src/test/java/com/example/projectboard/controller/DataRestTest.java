package com.example.projectboard.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@WebMvcTest // RestRepository Auto Configuration 을 불러오지 못해 에러가 발생하여 사용하지 않는다.
@Disabled("spring data rest 통합테스트는 불필요하므로 제외시킴")
@DisplayName("Data Rest Test")
@AutoConfigureMockMvc
@Transactional // 테스트에서 사용되는 것은 모두 롤백, repository test에는 @DataJpaTest가 사용되었으며 @Transactional 이 붙어 있음
@SpringBootTest
public class DataRestTest {

    private final MockMvc mvc;

    public DataRestTest(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @DisplayName("[api] 게시글 리스트 조회 ")
    @Test
    void givenNothing_whenRequestArticles_thenReturnsArticlesJsonResponse () throws Exception {
        mvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.valueOf("application/hal+json")))
                .andDo(print())
                ;
    }

    @DisplayName("[api] 게시글 단건 조회 ")
    @Test
    void givenNothing_whenRequestArticle_thenReturnsArticleJsonResponse () throws Exception {
        mvc.perform(get("/api/articles/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.valueOf("application/hal+json")))
                .andDo(print())
        ;
    }

    @DisplayName("[api] 댓글 리스트 조회 ")
    @Test
    void givenNothing_whenRequestArticleComments_thenReturnsArticleCommentsJsonResponse () throws Exception {
        mvc.perform(get("/api/articleComments"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.valueOf("application/hal+json")))
                .andDo(print())
        ;
    }

    @DisplayName("[api] 댓글 단건 조회 ")
    @Test
    void givenNothing_whenRequestArticleComment_thenReturnsArticleCommentJsonResponse () throws Exception {
        mvc.perform(get("/api/articleComments/1 "))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.valueOf("application/hal+json")))
                .andDo(print())
        ;
    }
}
