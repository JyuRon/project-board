package com.example.projectboard.repository;

import com.example.projectboard.domain.Article;
import com.example.projectboard.domain.QArticle;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.StringExpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface ArticleRepository extends
        JpaRepository<Article,Long>,
        QuerydslPredicateExecutor<Article>, // 완전히 일치하는 검색이 가능(대소문자 구분 안함)
        QuerydslBinderCustomizer<QArticle>  // web 검색시 모든 단어를 입력하지 않기 때문에 이를 위한 커스텀 적용
{

    // 참고 url : https://jaime-note.tistory.com/80v
    // QuerydslBinderCustomizer 오버라이드
    @Override
    default void customize(QuerydslBindings bindings, QArticle root){

        // 리스트에 등록되지 않은 필드를 검색하지 않는 옵션
        bindings.excludeUnlistedProperties(true);

        // 검색하고자 하는 필드를 추가
        bindings.including(root.title, root.hashtag, root.createdAt, root.createdBy, root.content);


        // 검색 조건 상세 설정(커스텀)
        bindings.bind(root.title).first(StringExpression::containsIgnoreCase);
        bindings.bind(root.content).first(StringExpression::containsIgnoreCase);
        bindings.bind(root.hashtag).first(StringExpression::containsIgnoreCase);
        bindings.bind(root.createdAt).first(DateTimeExpression::eq);
        bindings.bind(root.createdBy).first(StringExpression::containsIgnoreCase);

    }
}
