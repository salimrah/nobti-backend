package ma.noba.provider.domain.port;

import ma.noba.provider.domain.model.Provider;

import java.util.UUID;

/**
 * Keeps an external search index in sync with the provider catalogue. Postgres does not need it
 * (queries are live); a Meilisearch/Algolia adapter does.
 */
public interface ProviderSearchIndexPort {

    void index(Provider provider);

    void remove(UUID providerId);

    void reindexAll();
}
