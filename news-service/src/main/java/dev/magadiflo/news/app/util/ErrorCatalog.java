package dev.magadiflo.news.app.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCatalog {

    // Errores de validación
    INVALID_PARAMETERS("NEWS_MS_001", "Parámetro de solicitud de fecha no válido"),

    // Errores de negocio
    NEWS_NOT_FOUND("NEWS_MS_201", "Noticia no encontrada"),

    // Errores internos del servidor
    INTERNAL_SERVER_ERROR("NEWS_MS_002", "Error Interno del Servidor");

    private final String code;
    private final String message;
}
