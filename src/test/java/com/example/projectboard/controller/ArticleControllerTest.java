package com.example.projectboard.controller;

import com.example.projectboard.config.SecurityConfig;
import com.example.projectboard.domain.type.SearchType;
import com.example.projectboard.dto.ArticleWithCommentsDto;
import com.example.projectboard.dto.UserAccountDto;
import com.example.projectboard.service.ArticleService;
import com.example.projectboard.service.PaginationService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Import(SecurityConfig.class) // // Spring Security 추가 이후 permit all 설정과 관련된 정보를 불러오지 못하여 에러 발생, 이를 해결
@DisplayName("View 컨트롤러 - 게시글")
@WebMvcTest(ArticleController.class) // Controller Test 기법, 특정 클래스 지정 없을 경우 모든 controller 호ㅇ
class ArticleControllerTest {

    private final MockMvc mvc;

    @MockBean // 생성자 지원을 하지 않음
    private ArticleService articleService;

    @MockBean
    private PaginationService paginationService;

    // 실제 코드에서는 @Autowired를 생략이 가능하지만 테스트 코드의 경우 명시해줘야함
    public ArticleControllerTest(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @DisplayName("[view][GET} 게시글 리스트 (게시판) 페이지 - 정상 호출")
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

    @DisplayName("[view][GET} 게시글 상세 페이지 - 정상 호출")
    @Test
    void givenNothing_whenRequestingArticleView_thenReturnsArticleView() throws Exception {
        // given
        Long articleId = 1L;
        given(articleService.getArticle(articleId)).willReturn(createArticleWithCommentDto());

        // when & then
        mvc.perform(get("/articles/" + articleId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("articles/detail"))
                .andExpect(model().attributeExists("article"))
                .andExpect(model().attributeExists("articleComments"))
        ;

        then(articleService).should().getArticle(articleId);
    }

    @Disabled("구현 중")
    @DisplayName("[view][GET} 게시글 검색 전용 페이지 - 정상 호출")
    @Test
    void givenNothing_whenRequestingArticleSearchView_thenReturnsArticleSearchView() throws Exception {
        // given

        // when & then
        mvc.perform(get("/articles/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("articles/search"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        ;
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
