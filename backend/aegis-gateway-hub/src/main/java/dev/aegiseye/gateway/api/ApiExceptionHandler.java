package dev.aegiseye.gateway.api;

import dev.aegiseye.gateway.ingest.InvalidEvidenceException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InvalidEvidenceException.class)
    public ResponseEntity<ProblemDetail> invalidEvidence(InvalidEvidenceException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid evidence record");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ProblemDetail> duplicateRecord(DuplicateKeyException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Evidence record already exists for this stream sequence or payload hash"
        );
        problem.setTitle("Duplicate evidence record");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
