package com.example.projectboard.service;

import com.example.projectboard.domain.Article;
import com.example.projectboard.domain.Hashtag;
import com.example.projectboard.domain.UserAccount;
import com.example.projectboard.domain.constant.SearchType;
import com.example.projectboard.dto.ArticleDto;
import com.example.projectboard.dto.ArticleWithCommentsDto;
import com.example.projectboard.repository.ArticleRepository;
import com.example.projectboard.repository.HashtagRepository;
import com.example.projectboard.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserAccountRepository userAccountRepository;
    private final HashtagService hashtagService;
    private final HashtagRepository hashtagRepository;

    @Transactional(readOnly = true)
    public Page<ArticleDto> searchArticles(SearchType searchType, String searchKeyword, Pageable pageable) {
        if(searchKeyword == null || searchKeyword.isBlank()){
            return articleRepository.findAll(pageable).map(ArticleDto::from);
        }

        return switch (searchType){
            case TITLE -> articleRepository.findByTitleContaining(searchKeyword, pageable).map(ArticleDto::from);
            case CONTENT -> articleRepository.findByContentContaining(searchKeyword, pageable).map(ArticleDto::from);
            case ID -> articleRepository.findByUserAccount_UserIdContaining(searchKeyword, pageable).map(ArticleDto::from);
            case NICKNAME -> articleRepository.findByUserAccount_NicknameContaining(searchKeyword, pageable).map(ArticleDto::from);
            case HASHTAG -> articleRepository.findByHashtagNames(
                            Arrays.stream(searchKeyword.split(" ")).toList(),
                            pageable
                    )
                    .map(ArticleDto::from);
        };
    }

    @Transactional(readOnly = true)
    public ArticleWithCommentsDto getArticleWithComments(long articleId) {
        return articleRepository.findById(articleId)
                .map(ArticleWithCommentsDto::from)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다 - articleId: " + articleId))
                ;
    }

    @Transactional(readOnly = true)
    public ArticleDto getArticle(Long articleId) {
        return articleRepository.findById(articleId)
                .map(ArticleDto::from)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 없습니다 - articleId: " + articleId));
    }

    public void saveArticle(ArticleDto dto){
        UserAccount userAccount = userAccountRepository.getReferenceById(dto.userAccountDto().userId());

        Set<Hashtag> hashtags = renewHashtagsFromContent(dto.content());

        Article article = dto.toEntity(userAccount);

        // Set의 경우 중복되는 값의 경우 변경되지 않는다. 이러한 이유로 addAll 이 가능하다.
        article.addHashtags(hashtags);

        articleRepository.save(article);
    }


    public void  updateArticle(Long articleId, ArticleDto dto) {

        try{
            /**
             * 기존에는 findById 를 사용하여 select 쿼리를 한번 호출하고 save 하는 과정을 거침
             * getReferenceById 의 경우 Article Id 가 존재하는 가정하에 실제 테이블을 조회하는 대신 프록시 객체만 가져옴
             * 프록시 객체만 있는 경우 ID 값을 제외한 나머지 값을 사용하기 전까지는 실제 DB 에 액세스 하지 않기 때문에 SELECT 쿼리가 날아가지 않음
             * https://bcp0109.tistory.com/325
             */
            Article article = articleRepository.getReferenceById(articleId);
            UserAccount userAccount = userAccountRepository.getReferenceById(dto.userAccountDto().userId());

            // 게시글 등록자와 수정 요청할 사용자가 동일한지 파악
            if(article.getUserAccount().equals(userAccount)){
                if(dto.title() != null){
                    article.setTitle(dto.title());
                }

                if(dto.content() != null){
                    article.setContent(dto.content());
                }

                // 기존에 게시글이 가지고 있던 해시태그 Entity 의 id 를 추출한다.
                Set<Long> hashtagIds = article.getHashtags().stream()
                        .map(Hashtag::getId)
                        .collect(Collectors.toUnmodifiableSet());

                // 게시글이 기존에 가지고 있던 해시태그들의 정보를 지운다.
                article.clearHashtags();
                articleRepository.flush();

                // 기존에 가지고 있던 해시태그들이 사용되는 곳이 없다면 해시태그 Entity 를 삭제한다.
                hashtagIds.forEach(hashtagService::deleteHashtagWithoutArticles);

                // 게시글 등록시 해시태그 등록을 하던 방식과 동일하다.
                Set<Hashtag> hashtags = renewHashtagsFromContent(dto.content());
                article.addHashtags(hashtags);

                /**
                 * @Transactional 의해 영속성 컨텍스트는 article 의 변화를 감지하여 스스로 update 쿼리를 호출
                 * 그로 인해 save 메소드 생략 가능
                 *
                 */
                // articleRepository.save(dto.toEntity());
            }

        }catch (EntityNotFoundException e){
            log.warn("게시글 업데이트 실패. 게시글을 수정하는데 필요한 정보를 찾을 수 없습니다. - {}", e.getLocalizedMessage());
        }
    }

    public void deleteArticle(long articleId, String userId) {

        Article article = articleRepository.getReferenceById(articleId);

        // 기존에 게시글이 가지고 있던 해시태그 Entity 의 id 를 추출한다.
        Set<Long> hashtagIds = article.getHashtags().stream()
                .map(Hashtag::getId)
                .collect(Collectors.toUnmodifiableSet());

        // 게시글이 기존에 가지고 있던 해시태그들의 정보를 지운다.
        articleRepository.deleteByIdAndUserAccount_UserId(articleId, userId);
        articleRepository.flush();

        // 기존에 가지고 있던 해시태그들이 사용되는 곳이 없다면 해시태그 Entity 를 삭제한다.
        hashtagIds.forEach(hashtagService::deleteHashtagWithoutArticles);

    }

    public long getArticleCount() {
        return articleRepository.count();
    }

    @Transactional(readOnly = true)
    public Page<ArticleDto> searchArticlesViaHashtag(String hashtagName, Pageable pageable) {
        if(hashtagName == null || hashtagName.isBlank()){
            return Page.empty(pageable);
        }

        return articleRepository.findByHashtagNames(List.of(hashtagName),pageable).map(ArticleDto::from);
    }

    public List<String> getHashtags() {
        // TODO: hashtagService 로의 이동을 고려해보자.
        return hashtagRepository.findAllHashtagNames();
    }

    /**
     * 1. 게시글을 파싱하여 해시태그들을 추출한다.
     * 2. 추출한 해시태그들 중 이미 DB에 값이 존재하는지 파악한다.
     * 3. DB에 존재하지 않는 해시태그의 경우 추가하여 리턴한다.
     */
    private Set<Hashtag> renewHashtagsFromContent(String content) {
 
        // 본문 내용을 파싱하여 해시태그를 추출한다.
        Set<String> hashtagNamesInContent = hashtagService.parseHashtagNames(content);
        
        // 추출된 해시태그 중 DB에 이미 저장되어 있는 해시태그 Entity 들을 호출한다.
        Set<Hashtag> hashtags = hashtagService.findHashtagsByNames(hashtagNamesInContent);

        // 추출된 해시태그가 기존 DB에 저장된 값인지를 판단하기 위한 Set 생성
        Set<String> existingHashtagNames = hashtags.stream()
                .map(Hashtag::getHashtagName)
                .collect(Collectors.toUnmodifiableSet());

        // 추출된 해시태그가 DB에 존재 하지 않다면 해시태그 Entity Set 에 추가한다.
        hashtagNamesInContent.forEach(newHashtagName ->{
            if(!existingHashtagNames.contains(newHashtagName)){
                hashtags.add(Hashtag.of(newHashtagName));
            }
        });

        return hashtags;
    }
}
