package dev.aegiseye.gateway.api;

import dev.aegiseye.gateway.ingest.EvidenceIngestionRequest;
import dev.aegiseye.gateway.ingest.EvidenceIngestionResponse;
import dev.aegiseye.gateway.ingest.EvidenceIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class EvidenceController {
    private final EvidenceIngestionService ingestionService;

    public EvidenceController(EvidenceIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/api/evidence")
    public ResponseEntity<EvidenceIngestionResponse> ingest(@RequestBody EvidenceIngestionRequest request) {
        EvidenceIngestionResponse response = ingestionService.ingest(request);
        return ResponseEntity.status(201).body(response);
    }
}
