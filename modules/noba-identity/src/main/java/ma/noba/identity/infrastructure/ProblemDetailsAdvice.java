package ma.noba.identity.infrastructure;


import ma.noba.identity.domain.exception.TooManyRequestsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ProblemDetailsAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail badRequest(Exception e) {
        var p = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        p.setDetail(e.getMessage());
        return p;
    }

    @ExceptionHandler(TooManyRequestsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ProblemDetail tooMany(Exception e) {
        var p = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        p.setDetail(e.getMessage());
        return p;
    }

}
