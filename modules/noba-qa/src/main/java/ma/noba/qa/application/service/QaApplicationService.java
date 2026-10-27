package ma.noba.qa.application.service;

import ma.noba.qa.application.dto.AnswerQuestionRequest;
import ma.noba.qa.application.dto.AskQuestionRequest;
import ma.noba.qa.application.dto.QuestionResponse;
import ma.noba.qa.domain.event.QuestionAskedEvent;
import ma.noba.qa.domain.model.ProviderQuestion;
import ma.noba.qa.domain.port.ProviderQuestionRepository;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class QaApplicationService {

    private final ProviderQuestionRepository questionRepository;
    private final EventBus eventBus;

    public QaApplicationService(ProviderQuestionRepository questionRepository, EventBus eventBus) {
        this.questionRepository = questionRepository;
        this.eventBus = eventBus;
    }

    @Transactional
    public QuestionResponse askQuestion(UUID providerId, UUID userId, AskQuestionRequest request) {
        ProviderQuestion question = new ProviderQuestion(providerId, userId, request.question());
        questionRepository.save(question);

        eventBus.publish(new QuestionAskedEvent(question.getId(), providerId, userId));

        return toResponse(question);
    }

    @Transactional
    public QuestionResponse answerQuestion(UUID questionId, AnswerQuestionRequest request) {
        ProviderQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUESTION_NOT_FOUND, "Question not found"));

        if (question.isAnswered()) {
            throw new IllegalStateException("Question has already been answered");
        }

        question.answer(request.answer());
        questionRepository.save(question);

        return toResponse(question);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsForProvider(UUID providerId) {
        return questionRepository.findByProviderIdOrderByCreatedAtDesc(providerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private QuestionResponse toResponse(ProviderQuestion q) {
        return new QuestionResponse(
                q.getId().toString(),
                q.getProviderId().toString(),
                q.getUserId().toString(),
                q.getQuestion(),
                q.getAnswer(),
                q.getAnsweredAt(),
                q.getCreatedAt(),
                q.isAnswered()
        );
    }
}
