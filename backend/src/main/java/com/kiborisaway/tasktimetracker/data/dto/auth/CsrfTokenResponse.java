package com.kiborisaway.tasktimetracker.data.dto.auth;

public record CsrfTokenResponse(String token, String headerName) {
}
