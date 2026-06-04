package ma.noba.shared.domain.model;

public record OAuth2UserInfo(
        String id,
        String name,
        String email,
        String pictureUrl
) {}
