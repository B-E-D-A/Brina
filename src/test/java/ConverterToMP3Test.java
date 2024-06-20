import org.hse.brina.speech.recognition.ConverterToMP3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Класс ConverterToMP3Test - unit тесты, проверяющие работу методов класса ConverterToMP3 на корректность
 */

class ConverterToMP3Test {

    private ConverterToMP3 converterToMP3;

    @BeforeEach
    void setUp() {
        converterToMP3 = new ConverterToMP3();
    }

    @AfterEach
    void tearDown() {
        Path resultMP3 = Paths.get("result.mp3");
        if (Files.exists(resultMP3)) {
            try {
                Files.delete(resultMP3);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Test
    void testInstallFFmpeg() {
        String ffmpegPath = converterToMP3.getFfmpegPath();
        Assertions.assertNotNull(ffmpegPath);
        Assertions.assertTrue(converterToMP3.isFfmpegInstalled);
    }

    @Test
    void testConvertWAVToMP3MultipleTimes() {
        converterToMP3.convertWAVToMP3();
        Path resultMP3 = Paths.get("result.mp3");
        Assertions.assertTrue(Files.exists(resultMP3));
        converterToMP3.convertWAVToMP3();
        Assertions.assertTrue(Files.exists(resultMP3));
    }
}
