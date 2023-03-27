package com.example.projectboard.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@ToString(callSuper = true) // 상속한 부모 클래스의 정보도 포함한다.
@Table(indexes = {
        @Index(columnList = "title"),
        @Index(columnList = "createdAt"),
        @Index(columnList = "createdBy"),
})
@Entity
public class Article extends AuditingFields{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(optional = false) // 해당 객체에 null 값이 기입될 수 있도록 설정
    @JoinColumn(name = "userId")
    private UserAccount userAccount; // 유저 정보(ID)

    @Setter
    @Column(nullable = false)
    private String title; // 제목
    @Setter
    @Column(nullable = false, length = 10000)
    private String content; // 본문

    /**
     * @ManyToMany 사용 시 꼭 필요한 설정은 아니지만 직접 지정을 해주기 위한 설정
     * 실제 소유주 Entity 에서 설정한다.
     *
     * @JoinTable(joinColumns) : 주인 Entity의 column 을 입력한다.
     * @JoinTable(inverseJoinColumns) : 연관관계 Entity의 column 을 입력한다.
     *
     * LinkedHashSet : 순서(Sort)를 유지하기 위함
     *
     * CascadeType.PERSIST : insert 작업 발생 시 수행
     * CascadeType.MERGE : update 작업 발생 시 수행
     */
    @JoinTable(
            name= "article_hashtag",
            joinColumns = @JoinColumn(name = "articleId"),
            inverseJoinColumns = @JoinColumn(name = "hashtagId")
    )
    @ToString.Exclude
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}) // insert, update 시
    private Set<Hashtag> hashtags = new LinkedHashSet<>(); // LinkedHashSet : 순서를 유지하기 위함

    @ToString.Exclude
    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private final Set<ArticleComment> articleComments = new LinkedHashSet<>();


    protected Article() {
    }

    private Article(UserAccount userAccount, String title, String content) {
        this.userAccount = userAccount;
        this.title = title;
        this.content = content;
    }

    public static Article of(UserAccount userAccount, String title, String content) {
        return new Article(userAccount, title, content);
    }

    public void addHashtag(Hashtag hashtag) {
        this.getHashtags().add(hashtag);
    }

    public void addHashtags(Collection<Hashtag> hashtags) {
        this.getHashtags().addAll(hashtags);
    }

    public void clearHashtags() {
        this.getHashtags().clear();
    }

    // id(영속화)가 존재 하지 않다면 모두 다른 객체로 취급
    // id값만을 비교하여 다르면 다른 객체로 판단
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article article)) return false;
        return this.getId() != null && this.getId().equals(article.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId());
    }
}
