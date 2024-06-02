package org.hse.brina.speech.recognition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Класс ConverterToMP3 отвечает за преобразование аудиофайла WAV в MP3-файл с использованием библиотеки FFmpeg.
 */

public class ConverterToMP3 {
    private static final Logger logger = LogManager.getLogger();
    public boolean isFfmpegInstalled = false;

    public void installFFmpeg() {
        String[] command = {"wsl", "sudo", "apt-get", "update", "&&", "wsl", "sudo", "apt-get", "install", "-y", "ffmpeg"};
        executeCommand(command);
    }

    public void convertWAV(String ffmpegPath) {
        Path outputFile = Paths.get("result.mp3");
        if (outputFile.toFile().exists()) {
            try {
                Files.delete(outputFile);
            } catch (IOException e) {
                logger.info("Failed to delete existing file: " + outputFile + " " + e.getMessage());
            }
        }
        String[] command = {"wsl", ffmpegPath, "-i", "record.wav", "-ab", "320k", "result.mp3"};
        executeCommand(command);
    }

    private void executeCommand(String... command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void convertWAVToMP3() {
        String ffmpegPath = getFfmpegPath();
        if (!isFfmpegInstalled) {
            logger.info("FFmpeg is not installed. Installing ...");
            installFFmpeg();
        }
        convertWAV(ffmpegPath);
    }

    public String getFfmpegPath() {
        try {
            Process process = Runtime.getRuntime().exec("wsl which ffmpeg");
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                isFfmpegInstalled = true;
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String ffmpegPath = reader.readLine();
                if (ffmpegPath != null && !ffmpegPath.isEmpty()) {
                    return ffmpegPath;
                } else {
                    logger.error("FFmpeg not found in system.");
                }
            } else {
                isFfmpegInstalled = false;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error getting FFmpeg path: " + e.getMessage());
        }
        return null;
    }

}
