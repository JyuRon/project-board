package com.example.projectboard.domain;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@ToString(callSuper = true) // 상속한 부모 클래스의 정보도 포함한다.
@Table(indexes = {
        @Index(columnList = "title"),
        @Index(columnList = "hashtag"),
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
    @Setter
    private String hashtag; // 해시태그


    @ToString.Exclude
    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private final Set<ArticleComment> articleComments = new LinkedHashSet<>();


    protected Article() {
    }

    private Article(UserAccount userAccount, String title, String content, String hashtag) {
        this.userAccount = userAccount;
        this.title = title;
        this.content = content;
        this.hashtag = hashtag;
    }

    public static Article of(UserAccount userAccount, String title, String content, String hashtag) {
        return new Article(userAccount, title, content, hashtag);
    }

    // id(영속화)가 존재 하지 않다면 모두 다른 객체로 취급
    // id값만을 비교하여 다르면 다른 객체로 판단
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article article)) return false;
        return id != null && id.equals(article.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
