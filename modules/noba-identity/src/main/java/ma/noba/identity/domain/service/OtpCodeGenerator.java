package ma.noba.identity.domain.service;


public interface OtpCodeGenerator<T> {
    int CODE_LENGTH = 6;

    T generate(int digits);

    default T generate() {
        return generate(CODE_LENGTH);
    }
}



