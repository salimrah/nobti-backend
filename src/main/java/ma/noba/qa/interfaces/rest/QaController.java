package ma.noba.qa.interfaces.rest;

import jakarta.validation.Valid;
import ma.noba.qa.application.dto.AnswerQuestionRequest;
import ma.noba.qa.application.dto.AskQuestionRequest;
import ma.noba.qa.application.dto.QuestionResponse;
import ma.noba.qa.application.service.QaApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class QaController {

    private final QaApplicationService qaService;

    public QaController(QaApplicationService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/providers/{id}/questions")
    public ResponseEntity<QuestionResponse> askQuestion(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @PathVariable("id") UUID providerId,
            @Valid @RequestBody AskQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qaService.askQuestion(providerId, userId, request));
    }

    @PostMapping("/questions/{id}/answer")
    public ResponseEntity<QuestionResponse> answerQuestion(
            @PathVariable("id") UUID questionId,
            @Valid @RequestBody AnswerQuestionRequest request) {
        return ResponseEntity.ok(qaService.answerQuestion(questionId, request));
    }

    @GetMapping("/providers/{id}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(
            @PathVariable("id") UUID providerId) {
        return ResponseEntity.ok(qaService.getQuestionsForProvider(providerId));
    }
}
