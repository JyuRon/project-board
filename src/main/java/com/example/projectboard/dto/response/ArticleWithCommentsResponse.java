package com.example.projectboard.dto.response;

import com.example.projectboard.dto.ArticleCommentDto;
import com.example.projectboard.dto.ArticleWithCommentsDto;
import com.example.projectboard.dto.HashtagDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public record ArticleWithCommentsResponse(
        Long id,
        String title,
        String content,
        Set<String> hashtags,
        LocalDateTime createdAt,
        String email,
        String nickname,
        String userId,
        Set<ArticleCommentResponse> articleCommentsResponse
){

    public static ArticleWithCommentsResponse of(Long id, String title, String content, Set<String> hashtags, LocalDateTime createdAt, String email, String nickname, String userId, Set<ArticleCommentResponse> articleCommentResponses) {
        return new ArticleWithCommentsResponse(id, title, content, hashtags, createdAt, email, nickname, userId, articleCommentResponses);
    }

    public static ArticleWithCommentsResponse from(ArticleWithCommentsDto dto) {
        String nickname = dto.userAccountDto().nickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = dto.userAccountDto().userId();
        }

        return new ArticleWithCommentsResponse(
                dto.id(),
                dto.title(),
                dto.content(),
                dto.hashtagDtos().stream()
                        .map(HashtagDto::hashtagName)
                        .collect(Collectors.toUnmodifiableSet()),
                dto.createdAt(),
                dto.userAccountDto().email(),
                nickname,
                dto.userAccountDto().userId(),
                organizeChildComments(dto.articleCommentDtos())
        );
    }

    private static Set<ArticleCommentResponse> organizeChildComments(Set<ArticleCommentDto> dtos){

        // Map 의 key, value 를 각각 id, 하나의 response 객체로 구성
        Map<Long, ArticleCommentResponse> map = dtos.stream()
                .map(ArticleCommentResponse::from)
                .collect(Collectors.toMap(ArticleCommentResponse::id, Function.identity()))
                ;

        // 부모 댓글이 있는 경우 자기 자신을 부모댓글의 childComments 에 추가
        map.values().stream()
                .filter(ArticleCommentResponse::hasParentComment)
                .forEach(comment -> {
                    ArticleCommentResponse parentComment = map.get(comment.parentCommentId());
                    parentComment.childComments().add(comment);
                });

        // 이미 부모 댓글에 자식 댓글을 Set 형태로 가지고 있다.
        // 부모 댓글이 없는 것을 걸러내어 return
        return map.values().stream()
                .filter(comment -> !comment.hasParentComment())
                .collect(Collectors.toCollection(()->
                            new TreeSet<>(Comparator
                                    .comparing(ArticleCommentResponse::createdAt).reversed()
                                    .thenComparingLong(ArticleCommentResponse::id)
                            )
                        ));

    }
}
