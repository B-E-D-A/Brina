package org.hse.brina.speech.recognition;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.*;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.protobuf.ByteString;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.Config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * SpeechRecognition - класс, который позволяет преобразовывать аудиофайлы в текст с помощью Google Cloud API.
 */
public class SpeechRecognition {

    private static final Logger logger = LogManager.getLogger();

    private SpeechSettings downloadSettings() throws IOException {
        Path credentialsFilePath = Paths.get(Config.getProjectPath() + "/src/main/resources/org/hse/brina/integrations/google-credentials.json");
        String credentialsJson = Files.readString(credentialsFilePath);
        if (credentialsJson.contains("[")) {
            Dotenv dotenv = Dotenv.configure().directory("./.env").load();
            String projectId = dotenv.get("PROJECT_ID");
            String privateKeyId = dotenv.get("PRIVATE_KEY_ID");
            String privateKey = dotenv.get("PRIVATE_KEY");
            String clientEmail = dotenv.get("CLIENT_EMAIL");
            String clientId = dotenv.get("CLIENT_ID");
            String clientCertUrl = dotenv.get("CLIENT_CERT_URL");
            if (projectId != null && privateKeyId != null && privateKey != null && clientEmail != null && clientId != null && clientCertUrl != null) {
                credentialsJson = credentialsJson.replace("[PROJECT_ID]", projectId).replace("[PRIVATE_KEY_ID]", privateKeyId).replace("[PRIVATE_KEY]", privateKey).replace("[CLIENT_EMAIL]", clientEmail).replace("[CLIENT_ID]", clientId).replace("[CLIENT_CERT_URL]", clientCertUrl);
            } else {
                logger.error("Not all dotenv values are loaded");
            }
        }
        try {
            Files.write(credentialsFilePath, credentialsJson.getBytes());
        } catch (IOException e) {
            System.err.println("Error while writing in file: " + e.getMessage());
        }
        return SpeechSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(new FileInputStream(credentialsFilePath.toString())))).setEndpoint("speech.googleapis.com:443").build();
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
