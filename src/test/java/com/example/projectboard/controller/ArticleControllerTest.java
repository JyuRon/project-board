package com.example.projectboard.controller;

import com.example.projectboard.config.SecurityConfig;
import com.example.projectboard.config.TestSecurityConfig;
import com.example.projectboard.domain.constant.FormStatus;
import com.example.projectboard.domain.constant.SearchType;
import com.example.projectboard.dto.ArticleDto;
import com.example.projectboard.dto.ArticleWithCommentsDto;
import com.example.projectboard.dto.UserAccountDto;
import com.example.projectboard.dto.request.ArticleRequest;
import com.example.projectboard.response.ArticleResponse;
import com.example.projectboard.service.ArticleService;
import com.example.projectboard.service.PaginationService;
import com.example.projectboard.util.FormDataEncoder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest 의 경우 Controller 테스트를 진행함과 동시에 Spring Security Layer 까지 동작하게 된다.
 * 이로 인해 @Import(SecurityConfig.class) 를 추가 하지 않으면 permit 설정을 불러오지 못해 모든 테스트 동작이 에러가 발생하게 된다.
 * 이후 인증 기능을 구현하게 되면서 permitAll 설정을 바꿔줄 필요가 있었으며 이 과정에서 UserDetailsService 가 호출되는 동작이 발생하게 된다.
 * 즉 모든 인증,인가가 필요한 작업에 UserDetailsService - userAccountRepository.findById 를 호출하게 되며 이를 위한 Mock 작업이 필요
 * 이러한 이유로 @Import(SecurityConfig.class) --> @Import(TestSecurityConfig.class) 로 변경
 */
@Import({TestSecurityConfig.class, FormDataEncoder.class})
@DisplayName("View 컨트롤러 - 게시글")
@WebMvcTest(ArticleController.class) // Controller Test 기법, 특정 클래스 지정 없을 경우 모든 controller 호출
class ArticleControllerTest {

    private final MockMvc mvc;
    private final FormDataEncoder formDataEncoder;

    @MockBean // 생성자 지원을 하지 않음
    private ArticleService articleService;

    @MockBean
    private PaginationService paginationService;

    // 실제 코드에서는 @Autowired를 생략이 가능하지만 테스트 코드의 경우 명시해줘야함
    public ArticleControllerTest(
            @Autowired MockMvc mvc,
            @Autowired FormDataEncoder formDataEncoder
    ) {
        this.mvc = mvc;
        this.formDataEncoder = formDataEncoder;
    }

    @WithMockUser // 인증정보를 구분할 필요가 없을때, 인증이 되었다고 signal 을 보내는 것과 동일한 효과
    @DisplayName("[view][GET} 게시글 리스트 (게시판) 페이지 - 정상 호출, 인증된 사용자 ")
    @Test
    void givenNothing_whenRequestingArticlesView_thenReturnsArticlesView() throws Exception {
        // given
        // 서비스 로직의 동작 유무를 파악하는것이 아님
        // ArgumentMatchers 를 사용하게 된다면 모든 매개변수가 ArgumentMatchers 를 사용해야 한다.
        given(articleService.searchArticles(eq(null), eq(null), any(Pageable.class))).willReturn(Page.empty());
        given(paginationService.getPaginationBarNumbers(anyInt(),anyInt())).willReturn(List.of(0,1,2,3,4));

        // when & then
        mvc.perform(get("/articles"))
                .andExpect(status().isOk())
                //.andExpect(content().contentType(MediaType.TEXT_HTML))     // 정확히 일치해야함
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML)) // 호환되는 타입까지 매칭 : text/html;charset=UTF-8
                .andExpect(view().name("articles/index"))
                .andExpect(model().attributeExists("articles"))
                .andExpect(model().attributeExists("paginationBarNumbers"))
                .andExpect(model().attributeExists("searchTypes"))
                ;
        then(articleService).should().searchArticles(eq(null), eq(null), any(Pageable.class));
        then(paginationService).should().getPaginationBarNumbers(anyInt(),anyInt());
    }

    @DisplayName("[view][GET] 게시글 페이지 - 인증 없을 땐 로그인 페이지로 이동")
    @Test
    void givenNothing_whenRequestingArticlePage_thenRedirectsToLoginPage() throws Exception {
        //Given
        long articleId = 1L;

        //When & Then
        mvc.perform(get("/articles/" + articleId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"))
        ;

        then(articleService).shouldHaveNoInteractions();

    }

    @DisplayName("[view][GET} 게시글 리스트 (게시판) 페이지 - 검색어와 함께 호출")
    @Test
    void givenSearchKeyword_whenSearchingArticlesView_thenReturnsArticlesView() throws Exception {
        // given
        // 서비스 로직의 동작 유무를 파악하는것이 아님
        // ArgumentMatchers 를 사용하게 된다면 모든 매개변수가 ArgumentMatchers 를 사용해야 한다.
        SearchType searchType = SearchType.TITLE;
        String searchValue = "title";
        given(articleService.searchArticles(eq(searchType),eq(searchValue) , any(Pageable.class))).willReturn(Page.empty());
        given(paginationService.getPaginationBarNumbers(anyInt(),anyInt())).willReturn(List.of(0,1,2,3,4));

        // when & then
        mvc.perform(
                get("/articles")
                        .queryParam("searchType",searchType.name())
                        .queryParam("searchValue",searchValue)
                )
                .andExpect(status().isOk())
                //.andExpect(content().contentType(MediaType.TEXT_HTML))     // 정확히 일치해야함
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML)) // 호환되는 타입까지 매칭 : text/html;charset=UTF-8
                .andExpect(view().name("articles/index"))
                .andExpect(model().attributeExists("articles"))
                .andExpect(model().attributeExists("searchTypes"))
        ;
        then(articleService).should().searchArticles(eq(searchType), eq(searchValue), any(Pageable.class));
        then(paginationService).should().getPaginationBarNumbers(anyInt(),anyInt());
    }

    @DisplayName("[view][GET] 게시글 리스트 (게시판) 페이지 - 페이징, 정렬 기능")
    @Test
    void givenPagingAndSortingParams_whenSearchingArticlePage_thenReturnsArticlesPage() throws Exception {
        //Given
        String sortName = "title";
        String direction = "desc";
        int pageNumber = 0;
        int pageSize = 5;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.desc(sortName)));
        List<Integer> barNumbers = List.of(1,2,3,4,5);

        given(articleService.searchArticles(null, null, pageable))
                .willReturn(Page.empty());
        given(paginationService.getPaginationBarNumbers(pageable.getPageNumber(), Page.empty().getTotalPages()))
                .willReturn(barNumbers);

        //When & then
        mvc.perform(
                get("/articles")
                        .queryParam("page", String.valueOf(pageNumber))
                        .queryParam("size", String.valueOf(pageSize))
                        .queryParam("sort", sortName + "," + direction)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML)) // 호환되는 타입까지 매칭 : text/html;charset=UTF-8
                .andExpect(view().name("articles/index"))
                .andExpect(model().attributeExists("articles"))
                .andExpect(model().attribute("paginationBarNumbers", barNumbers))
        ;

        then(articleService).should().searchArticles(null, null, pageable);
        then(paginationService).should().getPaginationBarNumbers(pageable.getPageNumber(), Page.empty().getTotalPages());
    }

    @WithMockUser
    @DisplayName("[view][GET} 게시글 상세 페이지 - 정상 호출")
    @Test
    void givenNothing_whenRequestingArticleView_thenReturnsArticleView() throws Exception {
        // given
        Long articleId = 1L;
        Long totalCount = 1L;

        given(articleService.getArticleWithComments(articleId)).willReturn(createArticleWithCommentDto());
        given(articleService.getArticleCount()).willReturn(totalCount);

        // when & then
        mvc.perform(get("/articles/" + articleId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("articles/detail"))
                .andExpect(model().attributeExists("article"))
                .andExpect(model().attributeExists("articleComments"))
                .andExpect(model().attribute("totalCount", totalCount));
        ;

        then(articleService).should().getArticleWithComments(articleId);
        then(articleService).should().getArticleCount();
    }


    @DisplayName("[view][GET} 게시글 해시태그 검색 페이지 - 정상 호출")
    @Test
    void givenNothing_whenRequestingArticleHashtagSearchView_thenReturnsArticleHashtagSearchView() throws Exception {
        // given
        List<String> hashtags = List.of("#java","#spring","#boot");
        given(articleService.searchArticlesViaHashtag(eq(null), any(Pageable.class)))
                .willReturn(Page.empty());
        given(paginationService.getPaginationBarNumbers(anyInt(), anyInt()))
                .willReturn(List.of(1,2,3,4,5));
        given(articleService.getHashtags())
                .willReturn(hashtags);


        // when & then
        mvc.perform(get("/articles/search-hashtag"))
                .andExpect(status().isOk())
                .andExpect(view().name("articles/search-hashtag"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(model().attribute("articles",Page.empty()))
                .andExpect(model().attribute("hashtags",hashtags))
                .andExpect(model().attributeExists("paginationBarNumbers"))
                .andExpect(model().attribute("searchType", SearchType.HASHTAG))
        ;

        then(articleService).should().searchArticlesViaHashtag(eq(null),any(Pageable.class));
        then(paginationService).should().getPaginationBarNumbers(anyInt(),anyInt());
        then(articleService).should().getHashtags();
    }

    @DisplayName("[view][GET} 게시글 해시태그 검색 페이지 - 정상 호출, 해시태그 입력")
    @Test
    void givenHashtag_whenRequestingArticleSearchHashtagView_thenReturnsArticleSearchHashtagView() throws Exception {
        // given
        String hashtag = "#java";
        List<String> hashtags = List.of("#java","#spring","#boot");
        given(articleService.searchArticlesViaHashtag(eq(hashtag), any(Pageable.class)))
                .willReturn(Page.empty());
        given(paginationService.getPaginationBarNumbers(anyInt(), anyInt()))
                .willReturn(List.of(1,2,3,4,5));
        given(articleService.getHashtags())
                .willReturn(hashtags);

        // when & then
        mvc.perform(
                get("/articles/search-hashtag")
                        .queryParam("searchValue",hashtag)
                )
                .andExpect(status().isOk())
                .andExpect(view().name("articles/search-hashtag"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(model().attribute("articles",Page.empty()))
                .andExpect(model().attribute("hashtags",hashtags))
                .andExpect(model().attributeExists("paginationBarNumbers"))
                .andExpect(model().attribute("searchType", SearchType.HASHTAG))
        ;

        then(articleService).should().searchArticlesViaHashtag(eq(hashtag),any(Pageable.class));
        then(paginationService).should().getPaginationBarNumbers(anyInt(),anyInt());
        then(articleService).should().getHashtags();
    }

    @WithMockUser
    @DisplayName("[view][GET] 새 게시글 작성 페이지")
    @Test
    void givenNothing_whenRequesting_thenReturnsNewArticlePage() throws Exception {
        // Given

        // When & Then
        mvc.perform(get("/articles/form"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("articles/form"))
                .andExpect(model().attribute("formStatus", FormStatus.CREATE));
    }

    /**
     * post().with(user(BoardPrincipal.of())) 사용하는 다른 방식 또한 존재한다.
     * SecurityConfig에서 지정한 UserDetailsService 를 사용하게 하는 어노테이션
     * 단 DB(mock 데이터 포함) 에 사용자 정보가 존재하여야 한다.
     *
     * userDetailsServiceBeanName : UserDetailServices 빈을 직접 지정할 수 있다., 생략 가능
     * setupBefore : 테스트를 실행하기 직전에 이 setUp을 마쳐라!!!
     */
    @WithUserDetails(
            value = "jyukaTest",
            userDetailsServiceBeanName = "userDetailsService",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
    )
    @DisplayName("[view][POST] 새 게시글 등록 - 정상 호출")
    @Test
    void givenNewArticleInfo_whenRequesting_thenSavesNewArticle() throws Exception {
        // Given
        ArticleRequest articleRequest = ArticleRequest.of("new title", "new content", "#new");
        willDoNothing().given(articleService).saveArticle(any(ArticleDto.class));

        // When & Then
        mvc.perform(
                        post("/articles/form")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .content(formDataEncoder.encode(articleRequest))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/articles"))
                .andExpect(redirectedUrl("/articles"));
        then(articleService).should().saveArticle(any(ArticleDto.class));
    }

    @DisplayName("[view][POST] 게시글 수정 페이지 - 인증 없을 땐 로그인 페이지로 이동")
    @Test
    void givenNothing_whenRequesting_thenRedirectsToLoginPage() throws Exception {
        //Given
        long articleId = 1L;

        //When & Then
        mvc.perform(get("/articles/" + articleId + "/form"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"))
        ;

        then(articleService).shouldHaveNoInteractions();
    }

    @WithMockUser
    @DisplayName("[view][GET] 게시글 수정 페이지 - 정상 호출, 인증된 사용자")
    @Test
    void givenNothing_whenRequesting_thenReturnsUpdatedArticlePage() throws Exception {
        // Given
        long articleId = 1L;
        ArticleDto dto = createArticleDto();
        given(articleService.getArticle(articleId)).willReturn(dto);

        // When & Then
        mvc.perform(get("/articles/" + articleId + "/form"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("articles/form"))
                .andExpect(model().attribute("article", ArticleResponse.from(dto)))
                .andExpect(model().attribute("formStatus", FormStatus.UPDATE));
        then(articleService).should().getArticle(articleId);
    }



    @WithUserDetails(
            value = "jyukaTest",
            userDetailsServiceBeanName = "userDetailsService",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
    )
    @DisplayName("[view][POST] 게시글 수정 - 정상 호출, 인증된 사용자")
    @Test
    void givenUpdatedArticleInfo_whenRequesting_thenUpdatesNewArticle() throws Exception {
        // Given
        long articleId = 1L;
        ArticleRequest articleRequest = ArticleRequest.of("new title", "new content", "#new");
        willDoNothing().given(articleService).updateArticle(eq(articleId), any(ArticleDto.class));

        // When & Then
        mvc.perform(
                        post("/articles/" + articleId + "/form")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .content(formDataEncoder.encode(articleRequest))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/articles/" + articleId))
                .andExpect(redirectedUrl("/articles/" + articleId));
        then(articleService).should().updateArticle(eq(articleId), any(ArticleDto.class));
    }

    @WithUserDetails(
            value = "jyukaTest",
            userDetailsServiceBeanName = "userDetailsService",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
    )
    @DisplayName("[view][POST] 게시글 삭제 - 정상 호출")
    @Test
    void givenArticleIdToDelete_whenRequesting_thenDeletesArticle() throws Exception {
        // Given
        long articleId = 1L;
        String userId = "jyukaTest";
        willDoNothing().given(articleService).deleteArticle(articleId, userId);

        // When & Then
        mvc.perform(
                        post("/articles/" + articleId + "/delete")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/articles"))
                .andExpect(redirectedUrl("/articles"));
        then(articleService).should().deleteArticle(articleId, userId );
    }


    private ArticleDto createArticleDto() {
        return ArticleDto.of(
                createUserAccountDto(),
                "title",
                "content",
                "#java"
        );
    }

    private ArticleWithCommentsDto createArticleWithCommentDto(){
        return ArticleWithCommentsDto.of(
                1L,
                createUserAccountDto(),
                Set.of(),
                "title",
                "content",
                "#java",
                LocalDateTime.now(),
                "jyuka",
                LocalDateTime.now(),
                "jyuka"
        );
    }

    private UserAccountDto createUserAccountDto(){
        return UserAccountDto.of(
                "jyuka",
                "pw",
                "jyuka@mail.com",
                "jyuka",
                "memo",
                LocalDateTime.now(),
                "jyuka",
                LocalDateTime.now(),
                "jyuka"
        );
    }

}
