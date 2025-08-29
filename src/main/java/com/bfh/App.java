package com.bfh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    ApplicationRunner runner(RestTemplate restTemplate) {
        return args -> {
            System.out.println("✅ App started!");

            // STEP 1: Call generateWebhook API
            String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            Map<String, String> body = Map.of(
                    "name", "Your Name",
                    "regNo", "REG12347",   // 🔹 put your real regNo here
                    "email", "your.email@example.com"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<GenerateResp> response =
                    restTemplate.postForEntity(url, request, GenerateResp.class);

            GenerateResp resp = response.getBody();
            if (resp == null || resp.webhook == null || resp.accessToken == null) {
                throw new RuntimeException("❌ Failed to get webhook and token!");
            }

            System.out.println("✅ Got webhook: " + resp.webhook);
            System.out.println("✅ Got accessToken: " + resp.accessToken);

            // STEP 2: Your SQL query (Q2 since regNo last two digits are even)
            String finalQuery = """
            SELECT 
                e1.EMP_ID,
                e1.FIRST_NAME,
                e1.LAST_NAME,
                d.DEPARTMENT_NAME,
                COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT
            FROM EMPLOYEE e1
            JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID
            LEFT JOIN EMPLOYEE e2 
                ON e1.DEPARTMENT = e2.DEPARTMENT
               AND e2.DOB > e1.DOB
            GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME
            ORDER BY e1.EMP_ID DESC;
            """;

            // STEP 3: Submit the SQL query
            HttpHeaders submitHeaders = new HttpHeaders();
            submitHeaders.setContentType(MediaType.APPLICATION_JSON);
            submitHeaders.set("Authorization", resp.accessToken); // JWT token

            Map<String, String> finalBody = Map.of("finalQuery", finalQuery);

            HttpEntity<Map<String, String>> submitRequest = new HttpEntity<>(finalBody, submitHeaders);

            ResponseEntity<String> submitResp =
                    restTemplate.postForEntity(resp.webhook, submitRequest, String.class);

            System.out.println("✅ Submission response: " + submitResp.getBody());
        };
    }

    // Helper class to parse response JSON
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GenerateResp {
        public String webhook;
        public String accessToken;
    }
}
