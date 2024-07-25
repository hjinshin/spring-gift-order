package gift.service;

import gift.common.enums.Role;
import gift.common.enums.SocialLoginType;
import gift.common.properties.KakaoProperties;
import gift.controller.dto.response.TokenResponse;
import gift.model.Member;
import gift.repository.MemberRepository;
import gift.security.TokenProvider;
import org.springframework.stereotype.Service;

@Service
public class OAuthService {

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final KakaoProperties properties;
    private final KakaoApiCaller kakaoApiCaller;

    public OAuthService(MemberRepository memberRepository, TokenProvider tokenProvider, KakaoProperties kakaoProperties, KakaoApiCaller kakaoApiCaller) {
        this.memberRepository = memberRepository;
        this.tokenProvider = tokenProvider;
        this.properties = kakaoProperties;
        this.kakaoApiCaller = kakaoApiCaller;
    }

    public TokenResponse signIn(String code) {
        return signIn(code, properties.redirectUrl());
    }

    public TokenResponse signIn(String code, String redirectUrl) {
        String accessToken = kakaoApiCaller.getKakaoAccessToken(code, redirectUrl);
        String email = kakaoApiCaller.getKakaoMemberInfo(accessToken);
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(new Member(email, "", Role.USER, SocialLoginType.KAKAO)));
        member.checkLoginType(SocialLoginType.KAKAO);
        String token = tokenProvider.generateToken(member.getId(), member.getEmail(), member.getRole());
        return TokenResponse.from(token);
    }
}
