package ma.noba.provider.domain.port;

import ma.noba.provider.domain.model.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Provider-neutral search contract. Implemented by Postgres full-text (default) and Meilisearch.
 * A different engine (Algolia, Typesense) is just another adapter behind this port.
 */
public interface ProviderSearchPort {

    Page<Provider> search(String query, Pageable pageable);
}
