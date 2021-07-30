# DataReplicationFunction

Esta función lambda está implementada utilizando [Corretto Java 11](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/what-is-corretto-11.html) y [Gradle](https://gradle.org/) como herramienta de construcción.

## Estructura

- gradle - Wrapper de Dradle.
- src/main/java - Fuentes de Java 11.
- src/main/resources - Recursos utilizados en la Lambda.
- build.gradle.kts - Script de construcción utilizando Kotlin como lenguaje de definición.
- gradlew - Wrapper de Gradle.
- gradlew.bat - Wrapper de Gradle.
- README.md - Este archivo
- settings.gradle.kts - Archivo de configuración

## Librerías

- [AWS SDK for Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html) - Software Developer Kit, este kit de desarrollo es modular por lo que en esta función incluiremos las librerias de Amazon DynamoDB y Amazon Simple Storage Service.
- [AWS Lambda for Java](https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html) - Esta librería contiene todo lo necesario para construir una función Lambda en Java.

## Ejercicio

El objetivo de esta función es obtener el archivo cargado en el bucket de S3 de carga, tokenizar las cuentas y cargar el nuevo archivo a la capa RAW del lago de datos.

## Código

En el ejercicio se incluye el archivo ```aws.workshop.App.java``` que contiene el esqueleto del ejercicio.

Comencemos por los paquetes importados:
```java
// Estos paquetes nos ayudan a definir la estructura de datos de entrada y salida de la Lambda
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
// Paquetes de bitácoras estándar
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Paquetes para utilizar el cliente de DynamoDB del SDK de Java v2
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
// Paquetes para utilizar el cliente de S3 del SDK de Java v2
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

// Paquetes de Java para escribir en archivos, adicionalmente la clase UUID para generar identificadores únicos
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
```

Ahora revisemos la firma de la clase.
```java
/**
 * Esta función será invocada a través de eventos generados por el bucket de Amazon Simple Storage Service que fue
 * configurado en el template. Para definir la función debemos implementar la interfaz
 * <code>{@link com.amazonaws.services.lambda.runtime.RequestHandler}</code> y definir la entrada de la función como
 * <code>{@link com.amazonaws.services.lambda.runtime.events.S3Event}</code> así como la salida como
 * <code>{@link String}</code>.
 */
public class App implements RequestHandler<S3Event, String> {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    /** Nombre del bucket de destino. */
    private static final String DESTINATION_BUCKET = System.getenv("DESTINATION_BUCKET");
    /** Limite de items en peticiones de DynamoDB batch. */
    private static final int ITEM_LIMIT = 100;
    /** Nombre de la tabla de DynamoDB donde se encuentra la bóveda de tokens. */
    private static final String TOKEN_VAULT_TABLE = System.getenv("TOKEN_VAULT_TABLE");
    /** Cliente de Amazon DynamoDB. */
    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    /** Cliente de Amazon Simple Storage Service. */
    private static final S3Client s3Client = S3Client.create();
    /** Mapa utilizado como cache. */
    private final Map<String, String> accountsTokens = Collections.synchronizedMap(new HashMap<>());

    /**
     * Handles a Lambda Function request
     *
     * @param input   The Lambda Function input
     * @param context The Lambda execution environment context object.
     * @return The Lambda Function output
     */
    @Override
    public String handleRequest(final S3Event input, final Context context) {
        return "Ok";
    }
}
```

Puntos importantes para el ejercicio:

1. Recibiremos los objetos cargados a S3 en un arreglo el cual tendremos que iterar:
```java
for (final S3EventNotification.S3EventNotificationRecord record: input.getRecords()) {
    
}
```
2. Necesitamos almacenar los archivo en el directorio temporal de la Lambda:
```java
final String fileName = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + ".csv";
final String uploadFileName = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + ".csv";
```
3. Es importante eliminar los archivos en cada registro:
```java
Files.deleteIfExists(Path.of(fileName));
Files.deleteIfExists(Path.of(uploadFileName));
```
4. Para mayor información en los límites de las funciones Lambda puedes revisarlo aquí [AWS Lambda Limits](https://docs.aws.amazon.com/lambda/latest/dg/gettingstarted-limits.html)

---
**NOTA**

Puedes obtener la solución al ejercicio en el archivo App.solution

---