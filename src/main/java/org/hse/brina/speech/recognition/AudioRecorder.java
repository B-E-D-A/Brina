package org.hse.brina.speech.recognition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Класс AudioRecorder отвечает за запись звука с устройства ввода звука по умолчанию с помощью Java Sound API
 * и сохранение записи в файл "record.wav" в текущем каталоге.
 */

public class AudioRecorder {

    private static final Logger logger = LogManager.getLogger();
    private TargetDataLine targetLine;
    private Thread audioRecorderThread;

    public AudioRecorder() {
        try {
            AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            DataLine.Info dataInfo = new DataLine.Info(TargetDataLine.class, format);
            targetLine = (TargetDataLine) AudioSystem.getLine(dataInfo);
            targetLine.open();
        } catch (Exception e) {
            logger.error("Recorder is not created");
        }
    }

    public void startRecording() {
        Path outputFile = Paths.get("record.wav");
        if (outputFile.toFile().exists()) {
            try {
                Files.delete(outputFile);
            } catch (IOException e) {
                logger.info("Failed to delete existing file: " + outputFile + " " + e.getMessage());
            }
        }
        if (targetLine == null) return;
        logger.info("Start recording");
        targetLine.start();
        audioRecorderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                AudioInputStream recordingStream = new AudioInputStream(targetLine);
                File outputFile = new File("record.wav");
                try {
                    AudioSystem.write(recordingStream, AudioFileFormat.Type.WAVE, outputFile);
                } catch (Exception e) {
                    logger.info(e.getMessage());
                }
            }
        });
        audioRecorderThread.setDaemon(true);
        audioRecorderThread.start();
        logger.info("Rеcording is going");
    }

    public void stopRecording() {
        if (targetLine == null) return;
        targetLine.stop();
        targetLine.flush();
        targetLine.close();
        if (audioRecorderThread == null) return;
        logger.info("Stopped recording");
    }
}
