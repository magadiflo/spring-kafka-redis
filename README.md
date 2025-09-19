# [Comunicación asíncrona entre microservicios usando SpringBoot 3 con Kafka, Redis y Docker](https://www.youtube.com/watch?v=kIc3ORaZM-I)

---

## Arquitectura del proyecto

La solución se compone de dos microservicios que se comunican de manera asíncrona mediante `Kafka`, y que utilizan un
`Redis` compartido como `sistema de caché`.

### 1. News Service

- Expone una `API REST` para que los clientes consulten noticias.
- Flujo:
    - Revisa primero si la noticia solicitada se encuentra en `Redis`.
    - Si existe en caché → responde inmediatamente al cliente.
    - Si no existe en caché → produce un mensaje en `Kafka (news-topic)` indicando que debe obtenerse esa noticia.

### 2. Worker Service

- Está suscrito al topic `news-topic`.
- Flujo:
    - Escucha el mensaje enviado por el `news-service`.
    - Verifica en `Redis` si ya existe la noticia.
    - Si no está → consulta la API externa `Mediastack`.
    - Guarda la respuesta obtenida en `Redis`, quedando disponible para futuras consultas.

#### 3. Redis (único y compartido)

- Ambos servicios se conectan a la misma instancia de `Redis`, que actúa como caché centralizada.
- Permite que:
    - El `news-service` pueda responder rápidamente si la noticia ya fue procesada.
    - El `worker-service` guarde los resultados para que luego el `news-service` los entregue a los clientes.

![01.png](assets/01.png)

### Nota sobre la integración con Redis

En este proyecto, ambos microservicios se conectan a la misma instancia de `Redis`, pero cada uno utiliza un cliente
diferente para propósitos de práctica y aprendizaje:

- `News Service`. Se conecta a `Redis` utilizando la dependencia `spring-boot-starter-data-redis-reactive`, la cual
  internamente emplea el cliente `Lettuce`. Esta elección está alineada con el contenido del curso principal, ya que se
  centra en trabajar con `Spring Data Redis` en su variante reactiva.


- `Worker Service`. En este caso `no se usa` `Spring Data Redis Reactive`. En su lugar, se integra `Redis` a través de
  `redisson-spring-boot-starter`, aprovechando el cliente `Redisson`. Esta decisión se tomó como parte de la práctica
  de un curso previo de `Redis`, lo que permite explorar un enfoque alternativo de conexión y manejo de datos en
  `Redis`.

De esta forma, aunque ambos microservicios comparten la misma instancia de `Redis` como sistema de caché centralizado,
cada uno lo hace con un cliente distinto, lo que enriquece el aprendizaje y la comparación entre enfoques.

## Creando proyecto: [news-service](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.5.5&packaging=jar&jvmVersion=21&groupId=dev.magadiflo&artifactId=news-service&name=news-service&description=Demo%20project%20for%20Spring%20Boot&packageName=dev.magadiflo.news.app&dependencies=webflux,lombok,data-redis-reactive,kafka,validation)

Creamos el proyecto `news-service` desde spring initializr con las siguientes dependencias.

````xml
<!--Spring Boot 3.5.5-->
<!--Java 21-->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
````

> `Nota`: usamos `spring-boot-starter-data-redis-reactive` porque `WebFlux` está basado en el paradigma reactivo. Esto
> garantiza operaciones no bloqueantes también en el acceso a `Redis`.

## Configuración de News Service

Definimos las configuraciones básicas para que nuestro `news-service` pueda conectarse a `Kafka` y a `Redis`.

````yml
server:
  port: 8080
  error:
    include-message: always

spring:
  application:
    name: news-service
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
````

Explicación:

- `spring.kafka.bootstrap-servers`: indica la dirección de los `brokers de Kafka`.
- `spring.data.redis`: define las credenciales de acceso a `Redis`.
- El uso de variables de entorno (`${...}`) permite mayor portabilidad entre entornos (desarrollo, staging, producción).

## Configuración de Producer y Topic en Kafka

Para que el `News Service` pueda publicar mensajes en `Kafka`, es necesario configurar un `Producer` y definir el
`Topic` donde se enviarán dichos mensajes.

A continuación, se muestran las clases de configuración:

### Configuración del Producer

En esta clase se define el `ProducerFactory` y el `KafkaTemplate`.

- El `ProducerFactory` se encarga de crear instancias de productores con las propiedades definidas.
- El `KafkaTemplate` es el componente que abstrae y simplifica el envío de mensajes a `Kafka`.

````java

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Propiedades del Producer
    public Map<String, Object> producerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    // Crea el ProducerFactory
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(this.producerConfig());
    }

    // Define el KafkaTemplate para enviar mensajes
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
````

### Definiendo constantes

En esta clase de utilidad creamos las constantes que estaremos usando en nuestra aplicación.

````java

@UtilityClass
public class Constants {
    public static final String TOPIC_NAME = "news-topic";
    public static final String DATE_FORMAT = "^\\d{4}-\\d{2}-\\d{2}$";
    public static final String DATE_NOT_BLANK_MESSAGE = "El parámetro de solicitud de fecha no puede estar vacío o nulo";
    public static final String DATE_PATTERN_MESSAGE = "La fecha debe estar en el formato yyyy-MM-dd";
    public static final String DATA_FOUND_MESSAGE = "Datos encontrados";
    public static final String DATA_NOT_FOUND_MESSAGE = "La noticia solicitada para la fecha [%s] aún no está disponible. Por favor, intente nuevamente en unos momentos";
}
````

### Configuración del Topic

En esta clase se define el topic `news-topic`, el cual será creado automáticamente al iniciar la aplicación si no
existe en el cluster de `Kafka`.

````java

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic generateTopic() {
        return TopicBuilder.name(Constants.TOPIC_NAME).build();
    }
}
````

> 📌 `Nota`: Esta configuración es suficiente para entornos de desarrollo o pruebas. En entornos productivos, es
> recomendable especificar explícitamente el `número de particiones` y `réplicas` para garantizar escalabilidad y
> tolerancia a fallos.

## Configuración de Redis en news-service

En el `News Service`, se utiliza `Spring Data Redis Reactive` junto con el cliente `Lettuce` para interactuar con
`Redis` de manera reactiva.

Esto permite realizar operaciones no bloqueantes, algo muy útil en aplicaciones basadas en `Spring WebFlux`.

````java

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    // Configuración de conexión
    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        var redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(Objects.requireNonNull(this.redisHost));
        redisStandaloneConfiguration.setPort(Objects.requireNonNull(this.redisPort));
        redisStandaloneConfiguration.setPassword(Objects.requireNonNull(this.redisPassword));
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    // Configuración del template reactivo con serializadores
    @Bean
    public ReactiveRedisOperations<String, Object> reactiveRedisOperations(ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class); // Para serializar el value
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer()); // Para serializar la key
        RedisSerializationContext<String, Object> context = builder
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();
        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }
}
````

Explicación paso a paso

1. `Propiedades externas`
    - `spring.data.redis.host` → Dirección del servidor Redis.
    - `spring.data.redis.port` → Puerto de Redis.
    - `spring.data.redis.password` → Contraseña (si está configurada en Redis).

   Estas propiedades se inyectan desde el `application.yml`.

2. `ReactiveRedisConnectionFactory`
    - Se crea una instancia de `RedisStandaloneConfiguration` para indicar `host`, `puerto` y `password`.
    - Se usa `LettuceConnectionFactory`, que es el driver por defecto recomendado para `Redis` en entornos reactivos.
    - Este `ConnectionFactory` será la encargada de abrir conexiones reactivas hacia Redis.

3. `ReactiveRedisOperations`
    - Se construye un `ReactiveRedisTemplate`, que es el componente principal para interactuar con Redis de forma
      reactiva.
    - Para serializar las `keys`, se usa `StringRedisSerializer`.
    - Para serializar los `values` y objetos más complejos, se usa `Jackson2JsonRedisSerializer<Object>`.
    - También se configuran las serializaciones de `hashKey` y `hashValue` para soportar estructuras de tipo `Hash`
      en `Redis`.

En pocas palabras:

- `Keys` → guardadas como String.
- `Values` → guardados en formato JSON (gracias a `Jackson`).

## Creación del DAO para acceder a Redis

Para mantener una separación clara entre la lógica de negocio y el acceso a datos, se implementa un
`DAO (Data Access Object)` que encapsula las operaciones contra `Redis`. De esta forma, la aplicación puede
interactuar con la caché sin acoplarse directamente a la `API` de `ReactiveRedisOperations`.

### Interfaz NewsDao

La interfaz define las operaciones disponibles para acceder a las noticias en `Redis`. En este caso, únicamente se
consulta si existe una noticia asociada a una fecha específica.

````java
public interface NewsDao {
    Mono<Object> getNews(String date);
}
````

- `Mono<Object>` → dado que estamos en un contexto reactivo, el método devuelve un `Mono`, que representa un valor
  asíncrono (puede contener la noticia o estar vacío si no existe en `Redis`).
- `String date` → se usa la fecha como clave en `Redis`. En un caso real, podría considerarse un identificador más
  rico (ej. `news:2025-09-16`) para evitar colisiones.

### Implementación NewsDaoImpl

La implementación utiliza el `ReactiveRedisOperations` configurado previamente para interactuar con `Redis`.

````java

@RequiredArgsConstructor
@Repository
public class NewsDaoImpl implements NewsDao {

    private final ReactiveRedisOperations<String, Object> reactiveRedisOperations;

    @Override
    public Mono<Object> getNews(String date) {
        return this.reactiveRedisOperations.opsForValue().get(date);
    }
}
````

- `@Repository` → marca la clase como componente de acceso a datos.
- `@RequiredArgsConstructor (Lombok)` → genera automáticamente un constructor con los argumentos final, en este caso
  `reactiveRedisOperations`.
- `opsForValue().get(date)` → obtiene el valor almacenado en `Redis` para la clave proporcionada (la fecha).

## Servicio de Noticias (`NewsService`)

El servicio se encarga de:

1. Consultar `Redis` para ver si la noticia solicitada ya está disponible.
2. Si la encuentra `(cache HIT)` → devolverla al cliente.
3. Si no existe `(cache MISS)` → publicar un mensaje en `Kafka` para que el `worker-service` procese la solicitud y
   obtenga la noticia desde la API externa.

### Interfaz

````java
public interface NewsService {
    Mono<Object> getNews(String date);

    Mono<Void> publishToMessageBroker(String date);
}
````

- `getNews(String date)`: consulta si existe la noticia en `Redis`, y si no, dispara el flujo de publicación a `Kafka`.
- `publishToMessageBroker(String date)`: envía un mensaje al `topic` de `Kafka` con la fecha solicitada.

> ⚠️ `Nota`: por ahora el retorno es `Mono<Object>`, pero cuando tengamos el DTO definido conviene tipar la respuesta
> `(Mono<NewsDto>)` para trabajar de manera más segura.

### Implementación

````java

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsServiceImpl implements NewsService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NewsDao newsDao;

    @Override
    public Mono<Object> getNews(String date) {
        return this.newsDao.getNews(date)
                .doOnNext(value -> log.info("Cache HIT - Obteniendo desde Redis para fecha: {}", value))
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Cache MISS - Publicando fecha {} en Kafka", date);
                    return this.publishToMessageBroker(date);
                }));
    }

    @Override
    public Mono<Void> publishToMessageBroker(String date) {
        Message<String> message = MessageBuilder
                .withPayload(date)
                .setHeader(KafkaHeaders.TOPIC, Constants.TOPIC_NAME)
                .build();
        return Mono.fromFuture(() -> this.kafkaTemplate.send(message))
                .then();
    }
}
````

Explicación del flujo

1. `newsDao.getNews(date)` → busca la noticia en Redis.
2. Si existe:
    - Se loguea `Cache HIT`.
    - Se devuelve directamente el valor encontrado.
3. Si no existe:
    - Se loguea Cache MISS.
    - Se invoca `publishToMessageBroker(date)`, que construye un mensaje y lo envía a `Kafka`.

## 🧭 Catálogo de errores de negocio en APIs (`ErrorCatalog`)

En arquitecturas modernas de backend, especialmente en APIs REST, es común complementar los códigos HTTP estándar
con un `catálogo de errores de negocio`. Este catálogo permite identificar con precisión el origen del error, facilitar
la trazabilidad en observabilidad, y ofrecer mensajes claros y consistentes a los consumidores de la API.

### 🎯 Propósito del catálogo

El `enum` `ErrorCatalog` centraliza los errores que pueden ocurrir en la lógica de negocio o en validaciones
específicas. Cada entrada del catálogo contiene:

- `code`: Identificador único del error, siguiendo una convención definida por el equipo (e.g. `NEWS_MS_001`).
- `message`: Descripción legible del error, útil para mostrar al cliente o registrar en logs.

Esto permite desacoplar los errores técnicos del protocolo HTTP de los errores funcionales del dominio.

### 🧱 Ejemplo de implementación

````java

@Getter
@RequiredArgsConstructor
public enum ErrorCatalog {

    INVALID_PARAMETERS("NEWS_MS_001", "Parámetro de solicitud de fecha no válido"),
    INTERVAL_SERVER_ERROR("NEWS_MS_002", "Error Interno del Servidor");

    private final String code;
    private final String message;
}
````

### 🔍 Diferencias entre errores HTTP y errores de negocio

| Aspecto                        | Código HTTP (`400`, `500`, etc.) | Código de catálogo (`NEWS_MS_001`)             |
|--------------------------------|----------------------------------|------------------------------------------------|
| Propósito                      | Indicar tipo de error técnico    | Identificar error específico de negocio        |
| Granularidad                   | Limitada                         | Detallada y extensible                         |
| Trazabilidad en observabilidad | Difícil de rastrear sin contexto | Fácil de rastrear en logs, dashboards, alertas |
| Mensaje para el cliente        | Genérico                         | Personalizados y claros                        |
| Mantenibilidad                 | No versionable                   | Versionable y documentable                     |

### 📦 Ejemplo de respuesta en API

Esta estructura puede acompañar un código `HTTP 400 Bad Request`, pero el code interno permite identificar el error
exacto en dashboards, logs o alertas.

````json
{
  "code": "NEWS_MS_001",
  "message": "Parámetro de solicitud de fecha no válido"
}
````

### 🛠️ Buenas prácticas de diseño

- `Convención de códigos`: Usa prefijos por módulo (NEWS_MS, USER_MS, etc.) y numeración secuencial.
- `Centralización`: Mantén el catálogo en un único enum o agrúpalo por dominio si crece demasiado.
- `Versionado`: Documenta los cambios en el catálogo para evitar rupturas en clientes.
- `Integración con observabilidad`: Expón el code en logs estructurados, trazas y métricas.
- `Mensajes legibles`: Evita mensajes técnicos crípticos, prioriza claridad para el consumidor.

## DTOs de Respuesta

Para garantizar que la API tenga un contrato consistente tanto en respuestas exitosas como en errores, definimos los
siguientes DTOs:

### 1. Tipos de Error (ErrorType)

- `FUNCTIONAL` → Por ejemplo, cuando un parámetro no es válido o una noticia solicitada no existe.
- `SYSTEM` → Por ejemplo, fallas en la comunicación con `Kafka`, `Redis`, o errores internos inesperados.

````java
public enum ErrorType {
    FUNCTIONAL, // Errores relacionados con reglas de negocio
    SYSTEM      // Errores relacionados con el sistema o infraestructura
}
````

### 2. Respuesta exitosa (DataResponse)

````java
public record DataResponse<T>(String message,
                              Boolean status,
                              @JsonInclude(JsonInclude.Include.NON_NULL)
                              T data) {
}
````

- `message` → mensaje genérico para el cliente (ej: "Operación exitosa").
- `status` → indica si la operación fue correcta (true/false).
- `data` → el contenido real de la respuesta (puede ser un DTO, lista, etc.).
- ⚠️ `Si data es null`, no se incluye en el JSON gracias a `@JsonInclude(JsonInclude.Include.NON_NULL)`.

### 3. Respuesta de error (ErrorResponse)

````java
public record ErrorResponse(String code,
                            String message,
                            ErrorType errorType,
                            List<String> details,
                            LocalDateTime timestamp) {
}
````

- `code` → código de error definido en el catálogo (`NEWS_MS_001`, `NEWS_MS_002`, etc.).
- `message` → descripción breve y clara del error.
- `errorType` → indica si es un error `FUNCTIONAL` o `SYSTEM`.
- `details` → lista de detalles adicionales (ej. campos inválidos).
- `timestamp` → momento exacto en que ocurrió el error.

### ✅ Beneficios de este diseño

- Estandariza las respuestas, tanto exitosas como de error.
- Facilita el consumo de la API, ya que el cliente sabe siempre qué campos esperar.
- Permite extender fácilmente la estructura (ej. agregar un campo de requestId en el futuro para trazabilidad).

## Excepciones personalizadas

En la aplicación definimos excepciones propias para representar errores de negocio de forma clara y controlada. Esto
permite separar los errores funcionales de los errores técnicos, y facilitar el manejo centralizado de excepciones en
la capa de controladores.

### 1. Excepción específica: `NewsNotFoundException`

````java
public class NewsNotFoundException extends RuntimeException {
    public NewsNotFoundException(String date) {
        super(Constants.DATA_NOT_FOUND_MESSAGE.formatted(date));
    }
}
````

- Extiende de `RuntimeException`, lo que la hace no chequeada `(unchecked)`.
- Se lanza cuando una noticia no existe ni en `Redis` ni en la `API externa`.
- Usa el mensaje predefinido en `Constants.DATA_NOT_FOUND_MESSAGE`, formateado con la fecha solicitada.

### 2. Fábrica de excepciones: `ApplicationExceptions`

````java

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationExceptions {
    public static <T> Mono<T> newsNotFound(String date) {
        return Mono.error(() -> new NewsNotFoundException(date));
    }
}
````

- Clase utilitaria y estática (constructor privado) para centralizar la creación de errores en formato reactivo
  `(Mono.error)`.
- `newsNotFound(String date)` retorna un `Mono.error` que emite la excepción `NewsNotFoundException`.

### ✅ Beneficios de este enfoque

- Separa las excepciones de negocio (`NewsNotFound`) de los errores técnicos.
- Estandariza la forma en que se generan errores reactivos (`ApplicationExceptions`).
- Facilita el futuro manejo global con un `@RestControllerAdvice`, devolviendo un `ErrorResponse` consistente al
  cliente.

## Lanzando excepción en servicio `NewsServiceImpl`

Cuando una noticia no se encuentra en `Redis`, ocurren dos acciones encadenadas:

1. Se publica un mensaje en el topic de `Kafka`, para que el `worker-service` procese la solicitud.
2. Se lanza una excepción `NewsNotFoundException`, que posteriormente será capturada por nuestro handler global de
   errores y devuelta al cliente en un formato consistente (`ErrorResponse`).

De esta forma:

- El cliente recibe una respuesta inmediata, sin quedar bloqueado esperando a `Kafka`.
- El `worker-service` se encarga en segundo plano de consultar la API externa y guardar el resultado en `Redis` para
  futuras peticiones.

````java

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsServiceImpl implements NewsService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NewsDao newsDao;

    @Override
    public Mono<Object> getNews(String date) {
        return this.newsDao.getNews(date)
                .doOnNext(value -> log.info("Cache HIT - Obteniendo desde Redis para fecha: {}", value))
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Cache MISS - Publicando fecha {} en Kafka", date);
                    return this.publishToMessageBroker(date) // Cuando termine la publicación → lanzamos excepción
                            .then(ApplicationExceptions.newsNotFound(date));
                }));
    }

    @Override
    public Mono<Void> publishToMessageBroker(String date) {
        Message<String> message = MessageBuilder
                .withPayload(date)
                .setHeader(KafkaHeaders.TOPIC, Constants.TOPIC_NAME)
                .build();
        return Mono.fromFuture(() -> this.kafkaTemplate.send(message))
                .then();
    }
}
````

### 🔎 Puntos clave

- `Mono.defer(...)`: garantiza que la publicación en `Kafka` y la excepción solo se ejecuten si el flujo viene vacío
  (cache miss).
- `.then(ApplicationExceptions.newsNotFound(date))`: asegura que la excepción se dispare después de publicar el mensaje.
- Patrón `Cache-Aside + Event-driven`:
    - Si la noticia existe → se devuelve directamente desde Redis.
    - Si no existe → se dispara el flujo asíncrono y el cliente recibe un mensaje claro de que la solicitud está en
      proceso.

## Crea controlador `NewsController`

El controlador expone el endpoint para consultar noticias. Utiliza `NewsService` y retorna un `DataResponse` tipado
para mantener consistencia en las respuestas.

````java

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public Mono<ResponseEntity<DataResponse<Object>>> getNews(@NotBlank(message = Constants.DATE_NOT_BLANK_MESSAGE)
                                                              @Pattern(regexp = Constants.DATE_FORMAT, message = Constants.DATE_PATTERN_MESSAGE)
                                                              @RequestParam(required = false) String date) {
        return this.newsService.getNews(date)
                .map(data -> ResponseEntity.ok(new DataResponse<>(Constants.DATA_FOUND_MESSAGE, Boolean.TRUE, data)));
    }
}
````

🔎 Puntos clave

- `Validación de parámetros`
    - Aunque el parámetro `date` es obligatorio desde el punto de vista funcional, se define con `required = false`.
    - Esto permite que `Bean Validation` (`@NotBlank`, `@Pattern`) maneje la validación en lugar de que `Spring WebFlux`
      lo rechace automáticamente.
        - Si usáramos `required = true` (valor por defecto), cuando no enviemos el parámetro date en el request,
          `Spring` lanzaría una excepción antes de que nuestras validaciones con `Bean Validation` pudieran aplicarse.
        - De esta forma tenemos control total sobre los mensajes de error y aseguramos que todas las respuestas se
          devuelvan en un formato consistente (`ErrorResponse`).


- `Respuesta uniforme`. El uso de `DataResponse<T>` estandariza la salida.
    - Si existe la noticia en Redis → se devuelve con `status = true` y data poblado.
    - Si no existe → se lanza `NewsNotFoundException`, que más adelante manejaremos en un handler global de excepciones
      para devolver un `ErrorResponse`.
