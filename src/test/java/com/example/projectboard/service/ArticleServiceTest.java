package com.example.projectboard.service;

import com.example.projectboard.domain.Article;
import com.example.projectboard.domain.type.SearchType;
import com.example.projectboard.dto.ArticleDto;
import com.example.projectboard.dto.ArticleUpdateDto;
import com.example.projectboard.repository.ArticleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("비즈니스 로직 - 게시글")
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @InjectMocks // Mock 을 주입 받을 필드에 설정, 테스트 대상
    private ArticleService sut;

    @Mock
    private ArticleRepository articleRepository;

    @DisplayName("게시글을 검색하면 게시글 리스트를 반환한다.")
    @Test
    void givenSearchParameters_whenSearchingArticles_thenReturnsArticleList(){
        // given

        // when
        Page<ArticleDto> articles = sut.searchArticles(SearchType.TITLE,"search keyword");

        // then
        assertThat(articles).isNotNull();
    }

    @DisplayName("게시글을 조회하면 게시글을 반환한다.")
    @Test
    void givenArticleId_whenSearchingArticle_thenReturnsArticle(){
        // given

        // when
        ArticleDto articles = sut.searchArticles(1L);

        // then
        assertThat(articles).isNotNull();
    }

    @DisplayName("게시글 정보를 입력하면 게시글을 생성한다.")
    @Test
    void givenArticleInfo_whenSavingArticle_thenSavesArticle(){
        //Given
        ArticleDto articleDto = ArticleDto.of(LocalDateTime.now(), "Jyuka", "title", "content","#java");

        // mock 생성 과정
        // return 값이 존재하지 않는 void 형일 경우 앞의 메소드를 사용하여 명시하여 준다.
        // willDoNothing().given(articleRepository).save(any(Article.class));
        given(articleRepository.save(any(Article.class))).willReturn(null);


        //When
        sut.saveArticle(articleDto);

        //then
        // 실제로 해당 메소드가 호출 되었는지 검사
        then(articleRepository).should().save(any(Article.class));
    }

    @DisplayName("게시글의 ID와 수정정보를 입력하면 게시글을 생성한다.")
    @Test
    void givenArticleIdAndModifiedInfo_whenUpdatingArticle_thenUpdateArticle(){
        //Given
        ArticleUpdateDto dto = ArticleUpdateDto.of("title", "content","#java");

        // mock 생성 과정
        // return 값이 존재하지 않는 void 형일 경우 앞의 메소드를 사용하여 명시하여 준다.
        // willDoNothing().given(articleRepository).save(any(Article.class));
        given(articleRepository.save(any(Article.class))).willReturn(null);


        //When
        sut.updateArticle(1L, dto);

        //then
        // 실제로 해당 메소드가 호출 되었는지 검사
        then(articleRepository).should().save(any(Article.class));
    }


    @DisplayName("게시글의 ID를 입력하면 게시글을 삭제한다.")
    @Test
    void givenArticleId_whenDeletingArticle_thenDeletesArticle(){
        //Given
        ArticleUpdateDto dto = ArticleUpdateDto.of("title", "content","#java");

        // mock 생성 과정
        // return 값이 존재하지 않는 void 형일 경우 앞의 메소드를 사용하여 명시하여 준다.
         willDoNothing().given(articleRepository).delete(any(Article.class));


        //When
        sut.deleteArticle(1L);

        //then
        // 실제로 해당 메소드가 호출 되었는지 검사
        then(articleRepository).should().delete(any(Article.class));
    }

}