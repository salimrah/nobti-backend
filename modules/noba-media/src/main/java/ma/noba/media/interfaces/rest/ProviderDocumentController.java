package ma.noba.media.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.media.domain.port.StoragePort;
import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.shared.domain.port.ProviderDocumentPort;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** A provider uploads verification documents; owners and admins can read them. */
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderDocumentController {

    private static final Pattern OBJECT_KEY = Pattern.compile(
            "^(documents|general)/\\d{4}/\\d{2}/[0-9a-fA-F-]{36}\\.[a-z0-9]{2,5}$");

    private final ProviderDocumentPort documents;
    private final ProviderLookupPort providerLookup;
    private final StoragePort storage;

    public record SubmitDocumentRequest(String type, String objectKey, String fileName,
                                        String contentType, long sizeBytes) {
    }

    @GetMapping("/self/documents")
    public ResponseEntity<List<ProviderDocumentPort.DocumentRow>> mine(
            @AuthenticationPrincipal(expression = "id") UUID userId) {
        return ResponseEntity.ok(documents.list(providerId(userId)));
    }

    @PostMapping("/self/documents")
    public ResponseEntity<ProviderDocumentPort.DocumentRow> submit(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestBody SubmitDocumentRequest request) {
        if (request.objectKey() == null || !OBJECT_KEY.matcher(request.objectKey()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Upload the file through /storage/uploads?kind=documents first");
        }
        if (documents.list(providerId(userId)).size() >= 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many documents");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(documents.submit(
                providerId(userId), request.type(), request.objectKey(), request.fileName(),
                request.contentType(), request.sizeBytes()));
    }

    @DeleteMapping("/self/documents/{documentId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal(expression = "id") UUID userId,
                                       @PathVariable UUID documentId) {
        ProviderDocumentPort.DocumentRow document = documents.find(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!document.providerId().equals(providerId(userId)) || !"PENDING".equals(document.status())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only pending documents can be deleted");
        }
        documents.delete(documentId);
        return ResponseEntity.noContent().build();
    }

    /** Owner or administrator can read the stored file. */
    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<byte[]> file(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable UUID documentId) {
        ProviderDocumentPort.DocumentRow document = documents.find(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        boolean owner = false;
        try {
            owner = document.providerId().equals(providerId(principal.id()));
        } catch (ResponseStatusException ignored) {
            // Not a provider — only admins may continue.
        }
        if (!owner && !principal.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        ProviderDocumentPort.DocumentFile file = documents.file(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return storage.load(file.objectKey())
                .map(content -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(content.contentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + file.fileName().replace("\"", "") + "\"")
                        .body(content.content()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    private UUID providerId(UUID userId) {
        return providerLookup.findProviderIdByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
