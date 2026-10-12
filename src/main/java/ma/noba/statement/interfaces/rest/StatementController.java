package ma.noba.statement.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.statement.application.dto.StatementResponse;
import ma.noba.statement.application.service.StatementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/statement")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;

    /** The caller's consolidated financial statement (payments, gift cards, loyalty, referrals). */
    @GetMapping("/me")
    public ResponseEntity<StatementResponse> getMyStatement(
            @AuthenticationPrincipal(expression = "id") UUID userId) {
        return ResponseEntity.ok(statementService.getStatement(userId));
    }
}
