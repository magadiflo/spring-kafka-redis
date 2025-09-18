package dev.magadiflo.news.app.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCatalog {

    INVALID_PARAMETERS("NEWS_MS_001", "Parámetro de solicitud de fecha no válido"),
    INTERVAL_SERVER_ERROR("NEWS_MS_002", "Error Interno del Servidor");

    private final String code;
    private final String message;
}
