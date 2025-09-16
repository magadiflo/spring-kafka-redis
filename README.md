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

## Creando proyecto: [news-service](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.5.5&packaging=jar&jvmVersion=21&groupId=dev.magadiflo&artifactId=news-service&name=news-service&description=Demo%20project%20for%20Spring%20Boot&packageName=dev.magadiflo.news.app&dependencies=webflux,lombok,data-redis-reactive,kafka)

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
    public static final String DATA_NOT_FOUND_MESSAGE = "La noticia solicitada aún no está disponible. Su solicitud está siendo procesada, intente nuevamente en unos segundos";
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

