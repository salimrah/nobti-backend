package ma.noba.shared;

import ma.noba.shared.domain.model.ApiKeyPermissions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyPermissionsTest {

    @Test
    @DisplayName("Blank or null permissions default to read-only (least privilege)")
    void defaultsToReadOnly() {
        assertThat(ApiKeyPermissions.parse(null)).containsExactly("read");
        assertThat(ApiKeyPermissions.parse("  ")).containsExactly("read");
        assertThat(ApiKeyPermissions.parse(",,")).containsExactly("read");
    }

    @Test
    @DisplayName("Permissions are parsed, trimmed and lowercased")
    void parsesList() {
        Set<String> perms = ApiKeyPermissions.parse(" Read , WRITE ");
        assertThat(perms).containsExactly("read", "write");
    }

    @Test
    @DisplayName("Read-only keys cannot write; write keys can read and write")
    void readWriteRules() {
        Set<String> readOnly = ApiKeyPermissions.parse("read");
        assertThat(ApiKeyPermissions.allowsRead(readOnly)).isTrue();
        assertThat(ApiKeyPermissions.allowsWrite(readOnly)).isFalse();

        Set<String> readWrite = ApiKeyPermissions.parse("read,write");
        assertThat(ApiKeyPermissions.allowsRead(readWrite)).isTrue();
        assertThat(ApiKeyPermissions.allowsWrite(readWrite)).isTrue();
    }

    @Test
    @DisplayName("Wildcard grants everything")
    void wildcard() {
        Set<String> perms = ApiKeyPermissions.parse("*");
        assertThat(ApiKeyPermissions.allowsRead(perms)).isTrue();
        assertThat(ApiKeyPermissions.allowsWrite(perms)).isTrue();
    }

    @Test
    @DisplayName("Only GET/HEAD/OPTIONS/TRACE are read-safe")
    void readSafeMethods() {
        assertThat(ApiKeyPermissions.isReadSafe("GET")).isTrue();
        assertThat(ApiKeyPermissions.isReadSafe("head")).isTrue();
        assertThat(ApiKeyPermissions.isReadSafe("OPTIONS")).isTrue();
        assertThat(ApiKeyPermissions.isReadSafe("POST")).isFalse();
        assertThat(ApiKeyPermissions.isReadSafe("DELETE")).isFalse();
        assertThat(ApiKeyPermissions.isReadSafe(null)).isTrue();
    }
}
