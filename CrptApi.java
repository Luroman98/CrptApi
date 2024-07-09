package org.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;

import java.net.URI;
import java.net.http.HttpClient;


import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
public class CrptApi {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.semaphore = new Semaphore(requestLimit);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()),
                0, timeUnit.toSeconds(1), TimeUnit.SECONDS);
    }

    public static void main( String[] args ) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        Document document = crptApi.new Document();
        String signature = "example_signature";
        crptApi.createDocument("https://ismp.crpt.ru/api/v3/lk/documents/create", document, signature);
    }
    public void createDocument(String apiUrl, Document document, String signature) {
        try {
            if (!semaphore.tryAcquire()) {
                System.err.println("Превышен лимит запросов");
                return;
            }

            String requestBody = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Документ создан");
            } else {
                System.err.println("Ошибка при создании документа. HTTP-статус: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Ошибка при отправке запроса: " + e.getMessage());
        } finally {
            semaphore.release();
        }
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class Document
    {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        @Builder
        public static class Description {
            private String participantInn;
        }

        @Builder
        public static class Product
        {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;
        }
    }

}
