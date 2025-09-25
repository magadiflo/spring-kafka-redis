package dev.magadiflo.worker.app.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String TOPIC_NEWS = "news-topic";
    public static final String GROUP_ID_NEWS_TOPIC = "news-consumer-group";
    public static final String DATA_NOT_FOUND_MESSAGE = "La noticia solicitada para la fecha [%s] no existe en el servicio externo";

}
