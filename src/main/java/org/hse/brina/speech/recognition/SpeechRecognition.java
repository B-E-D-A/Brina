package org.hse.brina.speech.recognition;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.*;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.protobuf.ByteString;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class SpeechRecognition {

    private static final Logger logger = LogManager.getLogger();

    private SpeechSettings downloadSettings() throws IOException {
        Dotenv dotenv = Dotenv.configure().directory("./.env").load();

        String projectId = dotenv.get("PROJECT_ID");
        String privateKeyId = dotenv.get("PRIVATE_KEY_ID");
        String privateKey = dotenv.get("PRIVATE_KEY");
        String clientEmail = dotenv.get("CLIENT_EMAIL");
        String clientId = dotenv.get("CLIENT_ID");
        String clientCertUrl = dotenv.get("CLIENT_CERT_URL");

        String credentialsJson = String.format("{" +
                "\"type\": \"service_account\"," +
                "\"project_id\": \"%s\"," +
                "\"private_key_id\": \"%s\"," +
                "\"private_key\": \"%s\"," +
                "\"client_email\": \"%s\"," +
                "\"client_id\": \"%s\"," +
                "\"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\"," +
                "\"token_uri\": \"https://oauth2.googleapis.com/token\"," +
                "\"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\"," +
                "\"client_x509_cert_url\":  \"%s\"" +
                "}", projectId, privateKeyId, privateKey, clientEmail, clientId, clientCertUrl);

        Path tempDir = Files.createTempDirectory("temp");
        Path tempCredentialsFile = Paths.get(tempDir.toString(), "credentials.json");
        Files.write(tempCredentialsFile, credentialsJson.getBytes());

        return SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(new FileInputStream(tempCredentialsFile.toString()))))
                .setEndpoint("speech.googleapis.com:443")
                .build();
    }

    public String translateAudioToText(String path) {
        StringBuilder textResult = new StringBuilder();
        try {
            SpeechSettings settings = downloadSettings();

            try (SpeechClient speechClient = SpeechClient.create(settings)) {
                ByteString audioBytes = ByteString.readFrom(new FileInputStream(path));
                RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

                String[] filenameParts = path.split("\\.");
                String audioExtension = filenameParts[filenameParts.length - 1];
                AudioEncoding encoding;
                if (audioExtension.equals("mp3")) {
                    encoding = AudioEncoding.MP3;
                } else {
                    encoding = AudioEncoding.LINEAR16;
                }

                RecognitionConfig config = RecognitionConfig.newBuilder().setEncoding(encoding).setSampleRateHertz(16000).setLanguageCode("ru-RU").build();
                RecognizeResponse response = speechClient.recognize(config, audio);
                List<SpeechRecognitionResult> results = response.getResultsList();

                for (SpeechRecognitionResult result : results) {
                    SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                    textResult.append(alternative.getTranscript());
                }
            } catch (Exception e) {
                logger.error("Errors while speech to text recognition occurred\n");
            }
        } catch (Exception e) {
            logger.error("Errors while downloading api info to SpeechRecognition client\n");
        }
        return textResult.toString();
    }
}
