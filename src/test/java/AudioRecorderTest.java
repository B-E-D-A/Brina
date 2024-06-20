import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hse.brina.speech.recognition.AudioRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Класс AudioRecorderTest - unit тесты, проверяющие работу методов класса AudioRecorder на корректность
 */

class AudioRecorderTest {

    private static final Logger logger = LogManager.getLogger();
    private AudioRecorder audioRecorder;

    @BeforeEach
    void setUp() {
        audioRecorder = new AudioRecorder();
    }

    @AfterEach
    void tearDown() {
        audioRecorder.stopRecording();
        Path recordFile = Paths.get("record.wav");
        if (Files.exists(recordFile)) {
            try {
                Files.delete(recordFile);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    @Test
    void testStartAndStopRecording() {
        audioRecorder.startRecording();
        try {
            Thread.sleep(20);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        audioRecorder.stopRecording();
        Path recordFile = Paths.get("record.wav");
        Assertions.assertTrue(Files.exists(recordFile), "Record file should exist");
        Assertions.assertTrue(recordFile.toFile().length() > 0, "Record file should not be empty");
    }

    @Test
    void testStartRecordingWithExistingFile() {
        Path recordFile = Paths.get("record.wav");
        try {
            Files.createFile(recordFile);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        audioRecorder.startRecording();
        try {
            Thread.sleep(20);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        audioRecorder.stopRecording();
        Assertions.assertTrue(Files.exists(recordFile), "Record file should exist");
        Assertions.assertTrue(recordFile.toFile().length() > 0, "Record file should not be empty");
    }

    @Test
    void testStopRecordingWithoutStarting() {
        audioRecorder.stopRecording();
        Assertions.assertFalse(Files.exists(Paths.get("record.wav")), "Record file should not exist");
    }
}
