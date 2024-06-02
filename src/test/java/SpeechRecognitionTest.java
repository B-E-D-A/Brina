import com.google.cloud.speech.v1.SpeechSettings;
import org.hse.brina.speech.recognition.SpeechRecognition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class SpeechRecognitionTest {

    private SpeechRecognition speechRecognizer;

    public static String cleanPunctuation(String input) {
        String cleanedString = input.replaceAll("\\p{Punct}", "");
        return cleanedString.replaceAll("\\n", " ");
    }

    @BeforeEach
    void setUp() {
        speechRecognizer = new SpeechRecognition();
    }

    @Test
    void testDownloadSettings() throws IOException {
        SpeechSettings settings = speechRecognizer.downloadSettings();
        Assertions.assertNotNull(settings);
        Assertions.assertNotNull(settings.getCredentialsProvider());
        Assertions.assertEquals("speech.googleapis.com:443", settings.getEndpoint());
    }

    @Test
    void testTranslateAudioToText() throws IOException {
        String result = speechRecognizer.translateAudioToText("src/test/resources/test.mp3");
        String expectedResult = cleanPunctuation("Утёс\n" + "Ночевала тучка золотая\n" + "На груди утёса великана;\n" + "Утром в путь она умчалась рано,\n" + "По лазури весело играя;\n" + "Но остался влажный след в морщине\n" + "Старого утёса. Одиноко\n" + "Он стоит, задумался глубоко,\n" + "И тихонько плачет он в пустыне.");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResult.toLowerCase(), result.toLowerCase());
    }

    @Test
    void testTranslateNonExistentAudioToText() {
        String result = speechRecognizer.translateAudioToText("src/test/resources/non-existent.mp3");
        String expectedResult = "Файл не существует";
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResult, result);
    }
}
