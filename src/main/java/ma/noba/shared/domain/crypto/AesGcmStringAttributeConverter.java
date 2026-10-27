package ma.noba.shared.domain.crypto;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AesGcmStringAttributeConverter implements AttributeConverter<String, String> {
    @Override public String convertToDatabaseColumn(String attribute) { return AesGcmCrypto.encrypt(attribute); }
    @Override public String convertToEntityAttribute(String dbData) { return AesGcmCrypto.decrypt(dbData); }
}
