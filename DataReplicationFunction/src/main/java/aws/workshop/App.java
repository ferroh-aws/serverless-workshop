package aws.workshop;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

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
        LOGGER.debug("Procesando registros");
        return "Ok";
    }
}
