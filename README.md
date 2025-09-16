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
