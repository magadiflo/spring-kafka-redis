package dev.magadiflo.news.app.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public static final String TOPIC_NAME = "news-topic";
    public static final String DATE_FORMAT = "^\\d{4}-\\d{2}-\\d{2}$";
    public static final String DATE_NOT_BLANK_MESSAGE = "El parámetro de solicitud de fecha no puede estar vacío o nulo";
    public static final String DATE_PATTERN_MESSAGE = "La fecha debe estar en el formato yyyy-MM-dd";
    public static final String DATA_FOUND_MESSAGE = "Datos encontrados";
    public static final String DATA_NOT_FOUND_MESSAGE = "La noticia solicitada aún no está disponible. Su solicitud está siendo procesada, intente nuevamente en unos segundos";
}
