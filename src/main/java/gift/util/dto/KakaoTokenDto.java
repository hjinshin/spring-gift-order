package gift.util.dto;

public record KakaoTokenDto(
        String access_token,
        String token_type,
        String refresh_token,
        String expires_in,
        String scope,
        String refresh_token_expires_in
) {
}
