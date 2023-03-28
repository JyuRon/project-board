package com.example.projectboard.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.core.annotation.Order;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@ToString(callSuper = true) // 상속한 부모 클래스의 정보도 포함한다.
@Table(indexes = {
        @Index(columnList = "content"),
        @Index(columnList = "createdAt"),
        @Index(columnList = "createdBy"),
})
@Entity
public class ArticleComment extends AuditingFields{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(optional = false)
    private Article article; // 게시글 (ID)

    @Setter
    @ManyToOne(optional = false) // 해당 객체에 null 값이 기입될 수 있도록 설정
    @JoinColumn(name = "userId")
    private UserAccount userAccount; // 유저 정보(ID)

//     @ManyToOne(fetch = FetchType.LAZY)
//     private ArticleComment parentComment;
    @Setter
    @Column(updatable = false)
    private Long parentCommentId; // 부모 댓글 ID

    /**
     * 보통 연관관계 테이블이 존재하게 된다면 각각 테이블 내의 외래키를 참조하여 연관관계를 호출한다.
     * 각 테이블마다 외래키 추가하여 사용하게 되면 이는 편리성을 제공하기는 하지만 불필요한 자원을 낭비하게 된다.
     * JPA의 경우 특정한 설정이 없다면 각 테이블마다 외래키를 생성한다.(연관 테이블 생성)
     *
     * 이를 위한 설정으로는 mappedBy 가 존재하며 mappedBy 가 설정된 Entity 에서는 외래키를 생성(소유)하지 않는다.
     * 즉 mappedBy 가 존재하게 되면 외래키가 존재하지 않기 때문에 연관 entity 의 select 외에 update, delete 가 불가능하다.
     *   --> 조회 쿼리의 from 확인 결과 외래키를 소유하는 테이블에서 select 가 일어나며 where 의 경우 외래키를 소유하지 않는 테이블의 키본키를 사용
     *
     * 하지만 이와 같이 자신을 참조하는 mappedBy = "parentCommentId" 의 경우 외래키를 생성하지 않았지만 이미 컬럼값으로 존재하게 된다.
     * 이로 인해 수정, 삭제 또한 가능하게 된다.
     *
     * 대댓글 도메인 안에서 부모, 자식 관계를 설정하는 코드를 추가
     * 자식 댓글의 컬렉션 변화가 쿼리에 반영되게끔 cascading 규칙을 모두 적용
     * 이번엔 단방향 연관관계 설정을 사용해보기로 함 따라서 부모 댓글은 엔티티가 아닌 `Long` id를 직접 표현
     *
     * 또한 자식 댓글을 추가할 수 있는 메소드 추가 제공
     */
    @ToString.Exclude
    @OrderBy("createdAt ASC")
    @OneToMany(mappedBy = "parentCommentId", cascade = CascadeType.ALL)
    private Set<ArticleComment> childComments = new LinkedHashSet<>();

    @Setter
    @Column(nullable = false, length = 500)
    private String content; // 본문


    protected ArticleComment() {
    }

    private ArticleComment(Article article, UserAccount userAccount, Long parentCommentId, String content) {
        this.userAccount = userAccount;
        this.article = article;
        this.parentCommentId = parentCommentId;
        this.content = content;
    }

    public static ArticleComment of(Article article, UserAccount userAccount, String content){
        return new ArticleComment(article, userAccount, null, content);
    }

    public void addChildComment(ArticleComment child){
        child.setParentCommentId(this.getId());
        this.getChildComments().add(child);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof ArticleComment that)) return false;
        return this.getId() != null && this.getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId());
    }
}
