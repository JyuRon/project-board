package com.example.projectboard.service;

import com.example.projectboard.domain.Hashtag;
import com.example.projectboard.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class HashtagService {

    private final HashtagRepository hashtagRepository;

    public Set<String> parseHashtagNames(String content) {

        if(content == null){
            return Set.of();
        }

        Pattern pattern = Pattern.compile("#[\\w가-힣]+");
        Matcher matcher = pattern.matcher(content.strip());
        Set<String> result = new HashSet<>();

        while (matcher.find()){
            result.add(matcher.group().replace("#",""));
        }

        return Set.copyOf(result);
    }

    public Set<Hashtag> findHashtagsByNames(Set<String> hashtagNames) {
        return new HashSet<>(hashtagRepository.findByHashtagNameIn(hashtagNames));
    }

    /**
     * 게시글 삭제 시 연관된 해시태그도 같이 삭제되어야 하는게 맞지만
     * 삭제되어야 하는 해시태그가 다른 게시글에서도 사용 될 수 있음으로 이를 체크하는 로직이다.
     */
    public void deleteHashtagWithoutArticles(Long hashtagId) {

        Hashtag hashtag = hashtagRepository.getReferenceById(hashtagId);

        if(hashtag.getArticles().isEmpty()){
            hashtagRepository.delete(hashtag);
        }

    }
}