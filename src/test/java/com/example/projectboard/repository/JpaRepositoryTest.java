package com.example.projectboard.repository;

import com.example.projectboard.config.JpaConfig;
import com.example.projectboard.domain.Article;
import com.example.projectboard.domain.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("testdb")
// @DataJpaTest 사용시 자동으로 지정된 embedded db 의 데이터베이스를 참조
// 즉 테스트시 h2 db가 아닌 mysql, oracle등을 사용할때 사용 (직접 테스트 디비를 지정하는 방법)
// 해당 설정은 yml에서 관리가 가능하기 때문에 주석
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JPA 연결 테스트")
@Import(JpaConfig.class) //테스트 시 config 클래스를 인식하지 못하기 때문에 추가
@DataJpaTest
class JpaRepositoryTest {
    private final ArticleRepository articleRepository;
    private final ArticleCommentRepository articleCommentRepository;
    private final UserAccountRepository userAccountRepository;


    public JpaRepositoryTest(
            @Autowired ArticleRepository articleRepository,
            @Autowired ArticleCommentRepository articleCommentRepository,
            @Autowired UserAccountRepository userAccountRepository
    ) {
        this.articleRepository = articleRepository;
        this.articleCommentRepository = articleCommentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @DisplayName("insert 테스트")
    @Test
    void givenTestData_whenInserting_whenWorksFine(){
        long previousCount = articleRepository.count();
        UserAccount userAccount = userAccountRepository.save(UserAccount.of("newJyuka","pw",null,null,null));
        Article article = Article.of(userAccount,"new article","new content", "#spring");

        Article save = articleRepository.save(article);


        assertThat(articleRepository.count())
                .isEqualTo(previousCount + 1)
                ;

    }

    @DisplayName("update 테스트")
    @Test
    void givenTestData_whenUpdating_thenWorksFine(){
        Article article = articleRepository.findById(1L).orElseThrow();
        String updateHashtag = "#springboot";
        article.setHashtag(updateHashtag);

        /**
         * saveAndFlush 가 사용된 이유??
         * @DataJpaTest 의 경우 각 메소드 단위로 롤백
         * Jpa 입장에서 결국 롤백되기 때문에 update 실행 하지 않음
         * update 문을 확인하고자 saveAndFlush 사용
         */
        Article savedArticle = articleRepository.saveAndFlush(article);

        assertThat(savedArticle)
                .hasFieldOrPropertyWithValue("hashtag",updateHashtag)
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
                .hasSize(123)
                ;
    }


}