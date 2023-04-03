package com.example.projectboard.repository;

import com.example.projectboard.domain.Article;
import com.example.projectboard.domain.ArticleComment;
import com.example.projectboard.domain.Hashtag;
import com.example.projectboard.domain.UserAccount;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
 * @DataJpaTest 사용시 자동으로 지정된 embedded db 의 데이터베이스를 참조
 * 즉 테스트시 h2 db가 아닌 mysql, oracle등을 사용할때 사용 (직접 테스트 디비를 지정하는 방법)
 * 해당 설정은 yml에서 관리가 가능하기 때문에 여기서는 사용하지 않는다.
 */

/**
 * Auditing 사용 시 SecurityContextHolder에서 사용자 정보를 불러오도록 설정한 이후 문제 발생
 * insert 에서 createdBy 정보를 불러 오지 못함
 * @DataJpaTest 사용한 Jpa 에 대한 슬라이스 테스트로 security context 정보를 불러오지 못하기 때문
 * 기존 @Import(JpaConfig.class) 에서 변경
 */
@Import(JpaRepositoryTest.TestJpaConfig.class) //테스트 시 config 클래스를 인식하지 못하기 때문에 추가
@DisplayName("JPA 연결 테스트")
@ActiveProfiles("testdb")
@DataJpaTest
class JpaRepositoryTest {
    private final ArticleRepository articleRepository;
    private final ArticleCommentRepository articleCommentRepository;
    private final UserAccountRepository userAccountRepository;
    private final HashtagRepository hashtagRepository;


    JpaRepositoryTest(
            @Autowired ArticleRepository articleRepository,
            @Autowired ArticleCommentRepository articleCommentRepository,
            @Autowired UserAccountRepository userAccountRepository,
            @Autowired HashtagRepository hashtagRepository
    ) {
        this.articleRepository = articleRepository;
        this.articleCommentRepository = articleCommentRepository;
        this.userAccountRepository = userAccountRepository;
        this.hashtagRepository = hashtagRepository;
    }

    @DisplayName("insert 테스트")
    @Test
    void givenTestData_whenInserting_whenWorksFine(){
        long previousCount = articleRepository.count();
        UserAccount userAccount = userAccountRepository.save(UserAccount.of("newJyuka","pw",null,null,null));

        Article article = Article.of(userAccount, "new article", "new content");
        article.addHashtags(Set.of(Hashtag.of("spring")));

        Article save = articleRepository.save(article);


        assertThat(articleRepository.count())
                .isEqualTo(previousCount + 1)
                ;

    }

    @DisplayName("update 테스트")
    @Test
    void givenTestData_whenUpdating_thenWorksFine(){
        Article article = articleRepository.findById(1L).orElseThrow();
        System.out.println(article);

        Hashtag updatedHashtag = Hashtag.of("springboot");
        article.clearHashtags();
        article.addHashtags(Set.of(updatedHashtag));

        /**
         * saveAndFlush 가 사용된 이유??
         * @DataJpaTest 의 경우 각 메소드 단위로 롤백
         * Jpa 입장에서 결국 롤백되기 때문에 update 실행 하지 않음
         * update 문을 확인하고자 saveAndFlush 사용
         */
        Article savedArticle = articleRepository.saveAndFlush(article);

        assertThat(savedArticle.getHashtags())
                .hasSize(1)
                .extracting("hashtagName", String.class)
                    .containsExactly(updatedHashtag.getHashtagName())
        ;

    }


    @DisplayName("delete 테스트")
    @Test
    void givenTestData_whenDeleting_thenWorksFine(){
        Article article = articleRepository.findById(1L).orElseThrow();
        long previousArticleCount = articleRepository.count();
        long previousArticleCommentCount = articleCommentRepository.count();
        int deletedCommentSize = article.getArticleComments().size();

        articleRepository.delete(article);

        assertThat(articleRepository.count())
                .isEqualTo(previousArticleCount -1)
        ;

        assertThat(articleCommentRepository.count())
                .isEqualTo(previousArticleCommentCount -deletedCommentSize)
        ;
    }

    @DisplayName("select 테스트")
    @Test
    void givenTestData_whenSelecting_thenWorksFine(){
        List<Article> articles = articleRepository.findAll();

        assertThat(articles)
                .isNotNull()
                .hasSize(123)  // classpath:resources/data.sql 참조
                ;
    }

    @DisplayName("대댓글 조회 테스트")
    @Test
    void givenParentCommentId_whenSelecting_thenReturnsChildComments(){
        //Given

        //When
        Optional<ArticleComment> parentComment = articleCommentRepository.findById(1L);

        //then
        // assertThat.get() : Optional 에 대한 테스트 코드
        assertThat(parentComment).get()
                .hasFieldOrPropertyWithValue("parentCommentId", null) // 필드 검사
                .extracting("childComments", InstanceOfAssertFactories.COLLECTION)
                .hasSize(4)
        ;
    }

    @DisplayName("댓글에 대댓글 삽입 테스트")
    @Test
    void givenParentComment_whenSaving_thenInsertsChildComment() {
        // Given
        ArticleComment parentComment = articleCommentRepository.getReferenceById(1L);
        ArticleComment childComment = ArticleComment.of(
                parentComment.getArticle(),
                parentComment.getUserAccount(),
                "대댓글"
        );

        // When
        parentComment.addChildComment(childComment);
        articleCommentRepository.flush();

        // Then
        assertThat(articleCommentRepository.findById(1L)).get()
                .hasFieldOrPropertyWithValue("parentCommentId", null)
                .extracting("childComments", InstanceOfAssertFactories.COLLECTION)
                    .hasSize(5)
        ;
    }

    @DisplayName("댓글 삭제와 대댓글 전체 연동 삭제 테스트")
    @Test
    void givenArticleCommentHavingChildComments_whenDeletingParentComment_thenDeletesEveryComment() {
        // Given
        ArticleComment parentComment = articleCommentRepository.getReferenceById(1L);
        long previousArticleCommentCount = articleCommentRepository.count();

        // When
        articleCommentRepository.delete(parentComment);

        // Then
        assertThat(articleCommentRepository.count()).isEqualTo(previousArticleCommentCount - 5); // 테스트 댓글 + 대댓글 4개
    }

    @DisplayName("댓글 삭제와 대댓글 전체 연동 삭제 테스트 - 댓글 ID + 유저 ID")
    @Test
    void givenArticleCommentIdHavingChildCommentsAndUserId_whenDeletingParentComment_thenDeletesEveryComment() {
        // Given
        long previousArticleCommentCount = articleCommentRepository.count();

        // When
        articleCommentRepository.deleteByIdAndUserAccount_UserId(1L, "jyuka");

        // Then
        assertThat(articleCommentRepository.count()).isEqualTo(previousArticleCommentCount - 5); // 테스트 댓글 + 대댓글 4개
    }

    @DisplayName("[Querydsl] 전체 hashtag 리스트에서 이름만 조회하기")
    @Test
    void givenNothing_whenQueryingHashtags_thenReturnsHashtagNames() {
        // Given

        // When
        List<String> hashtagNames = hashtagRepository.findAllHashtagNames();

        // Then
        assertThat(hashtagNames).hasSize(19);
    }

    @DisplayName("[Querydsl] hashtag로 페이징된 게시글 검색하기")
    @Test
    void givenHashtagNamesAndPageable_whenQueryingArticles_thenReturnsArticlePage() {
        // Given
        List<String> hashtagNames = List.of("blue", "crimson", "fuscia");
        Pageable pageable = PageRequest.of(0, 5, Sort.by(
                Sort.Order.desc("hashtags.hashtagName"),
                Sort.Order.asc("title")
        ));

        // When
        Page<Article> articlePage = articleRepository.findByHashtagNames(hashtagNames, pageable);

        // Then
        assertThat(articlePage.getContent()).hasSize(pageable.getPageSize());
        assertThat(articlePage.getContent().get(0).getTitle()).isEqualTo("Fusce posuere felis sed lacus.");
        assertThat(articlePage.getContent().get(0).getHashtags())
                .extracting("hashtagName", String.class)
                    .containsExactly("fuscia");
        assertThat(articlePage.getTotalElements()).isEqualTo(17);
        assertThat(articlePage.getTotalPages()).isEqualTo(4);
    }
    @EnableJpaAuditing
    @TestConfiguration
    static class TestJpaConfig{
        @Bean
        AuditorAware<String> auditorAware(){
            return () -> Optional.of("jyuka");
        }
    }
}