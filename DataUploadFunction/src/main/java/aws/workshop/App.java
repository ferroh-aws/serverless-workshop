package aws.workshop;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
        LOGGER.debug("Iniciando ejecución");
        // Iteramos sobre los registros.
        try {
            for (final S3EventNotification.S3EventNotificationRecord record: input.getRecords()) {
                LOGGER.debug("Procesando archivo " + record.getS3().getObject().getKey());
                processFile(record.getS3().getBucket().getName(), record.getS3().getObject().getKey());
            }
        } catch (final IOException ex) {
            LOGGER.error("Error processing file", ex);
            throw new RuntimeException("Error processing file", ex);
        }
        return null;
    }

    /**
     * Procesa el archivo cargado en el bucket de S3.
     * @param bucket Nombre del bucket de S3.
     * @param key    Llave del objeto a procesar.
     */
    private void processFile(final String bucket, final String key) throws IOException {
        LOGGER.debug("Procesando archivo " + bucket + key);
        final String fileName = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + ".csv";
        final String uploadFileName = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + ".csv";
        // Procesamos los archivos, recordemos que DynamoDB soporta batch.
        Files.deleteIfExists(Path.of(fileName));
        Files.deleteIfExists(Path.of(uploadFileName));
    }

    /**
     * Escribe las lineas en el cache al archivo temporal.
     *
     * @param writer     Escritor al archivo temporal.
     * @param linesCache Cache de líneas.
     * @param accounts   Cuentas.
     * @throws IOException En caso de errores al escribir el archivo.
     */
    private void writeLines(BufferedWriter writer, List<String[]> linesCache, Set<String> accounts) throws IOException {
        tokenizeAccounts(accounts);
        for (final String[] cachedFields: linesCache) {
            writer.append(cachedFields[0])
                    .append(',')
                    .append(accountsTokens.get(cachedFields[1]))
                    .append(',')
                    .append(cachedFields[2])
                    .append(',')
                    .append(cachedFields[3])
                    .append(',')
                    .append(cachedFields[4])
                    .append('\n');
        }
        accounts.clear();
        linesCache.clear();
    }

    /**
     * Función para tokenizar las cuentas. Esta función se asegura que la cuenta se encuentre en el cache.
     *
     * @param accounts Set con las cuentas a tokenizar.
     * @return Mapa con la cuenta y su token.
     */
    private void tokenizeAccounts(final Set<String> accounts) {
        // Necesitamos implementar como obtener el token de las cuentas y como almacenarlo en DynamoDB.
        // Notas importantes, BatchGetItem soporta un máximo de 100 items, y BatchWriteItem soporta un máximo de 25.
    }
}
