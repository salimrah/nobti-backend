package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.PasswordPolicy;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class DefaultPasswordPolicy implements PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    @Override
    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new ValidationException(ErrorCode.PASSWORD_WEAK, "Password must not be empty");
        }
        if (rawPassword.length() < MIN_LENGTH) {
            throw new ValidationException(ErrorCode.PASSWORD_WEAK, "Password must be at least " + MIN_LENGTH + " characters");
        }
        if (rawPassword.length() > MAX_LENGTH) {
            throw new ValidationException(ErrorCode.PASSWORD_WEAK, "Password must not exceed " + MAX_LENGTH + " characters");
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : rawPassword.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUpper) throw new ValidationException(ErrorCode.PASSWORD_WEAK, "Password must contain an uppercase letter");
        if (!hasLower) throw new ValidationException(ErrorCode.PASSWORD_WEAK, "Password must contain a lowercase letter");
        if (!hasDigit) throw new ValidationException(ErrorCode.PASSWORD_WEAK, "Password must contain a digit");
    }
}
