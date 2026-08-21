package org.sopt.buddys.global.security.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserInfo(
    String sub,
    String email,
    @JsonProperty("email_verified") Boolean emailVerified,
    String name,
    String picture
) {}
