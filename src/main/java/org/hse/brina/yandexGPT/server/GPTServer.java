package org.hse.brina.yandexGPT.server;

import io.github.cdimascio.dotenv.Dotenv;
import org.hse.brina.Config;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GPTServer {
    public static String getGPTProcessing(String query, String text) throws URISyntaxException, IOException, InterruptedException {
        String json = constructRequest(query, text);
        Dotenv dotenv = Dotenv.configure().directory("./.env").load();
        String apiKey = dotenv.get("GPT_API_KEY");
        HttpRequest request = HttpRequest.newBuilder().uri(new URI("https://llm.api.cloud.yandex.net/foundationModels/v1/completion")).POST(HttpRequest.BodyPublishers.ofString(json)).header("Content-Type", "application/json").header("Authorization", "Api-key " + apiKey).build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();
        int responseBegin = responseBody.indexOf("message");
        responseBegin = responseBody.indexOf("text", responseBegin);
        responseBegin = responseBody.indexOf(":", responseBegin) + 2;
        int responseEnd = responseBody.indexOf("\"", responseBegin);

        return responseBody.substring(responseBegin, responseEnd);
    }

    @NotNull
    private static String constructRequest(String query, String text) throws IOException {
        String requestJson = Files.readString(Paths.get(Config.getProjectPath() + "/src/main/resources/org/hse/brina/integrations/yandex-gpt-request.json"));
        return requestJson.replace("[QUERY]", query).replace("[TEXT]", text);
    }
}

