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

El objetivo de esta función es obtener los registros generados por Amazon DynamoDB a partir del Amazon DynamoDB Stream y generar un archivo separado por comas el cual será colocado en nuestra capa cruda del lago de datos, adicionalmente, este deberá ser encriptado con otra llave CMK diferente a la configurada por defecto.

## Código

En el ejercicio se incluye el archivo ```aws.workshop.App.java``` que contiene el esqueleto del ejercicio.

Comencemos por los paquetes importados:
```java
// Estos paquetes nos ayudan a definir la estructura de datos de entrada y salida de la Lambda
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
// Paquetes de bitácoras estándar
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Paquetes para utilizar el cliente de S3 del SDK de Java v2
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

// Paquetes de Java para escribir en archivos, adicionalmente la clase UUID para generar identificadores únicos
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
```

Ahora revisemos la firma de la clase.
```java
/**
 * Implementación de la interfaz <code>{@link RequestHandler}</code> con entrada de tipo
 * <code>{@link DynamodbEvent}</code> y salida de tipo <code>{@link String}</code>.
 */
public class App implements RequestHandler<DynamodbEvent, String> {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    /** Nombre del bucket de destino. */
    private static final String DESTINATION_BUCKET = System.getenv("DESTINATION_BUCKET");
    /** Identificador de llave para datos sensibles. */
    private static final String SENSIBLE_KEY_ID = System.getenv("SENSIBLE_KEY_ID");
    /** Cliente de Amazon Simple Storage Service. */
    private static final S3Client s3Client = S3Client.create();

    /**
     * Handles a Lambda Function request
     *
     * @param input   The Lambda Function input
     * @param context The Lambda execution environment context object.
     * @return The Lambda Function output
     */
    @Override
    public String handleRequest(final DynamodbEvent input, final Context context) {
        return "Ok";
    }
}
```

Puntos importantes para el ejercicio:

1. Recibiremos los registros en un arreglo el cual tendremos que iterar:
```java
for (final DynamodbEvent.DynamodbStreamRecord record : input.getRecords()) {
    // Procesar registro
}
```
2. Necesitamos almacenar el archivo en el directorio temporal de la Lambda:
```java
final String fileName = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + ".csv";
```
---
**NOTA**

Puedes obtener la solución al ejercicio en el archivo App.solution

---