package dev.aegiseye.gateway.api;

import dev.aegiseye.gateway.ledger.ChainVerificationRequest;
import dev.aegiseye.gateway.ledger.ChainVerificationResponse;
import dev.aegiseye.gateway.ledger.LedgerRecordResponse;
import dev.aegiseye.gateway.ledger.LedgerService;
import dev.aegiseye.gateway.ledger.StreamSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {
    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/streams")
    public List<StreamSummary> streams() {
        return ledgerService.listStreams();
    }

    @GetMapping("/records")
    public List<LedgerRecordResponse> records(
            @RequestParam String deviceId,
            @RequestParam String streamId
    ) {
        return ledgerService.listRecords(deviceId, streamId);
    }

    @PostMapping("/verify")
    public ChainVerificationResponse verify(@RequestBody ChainVerificationRequest request) {
        return ledgerService.verify(request);
    }
}
