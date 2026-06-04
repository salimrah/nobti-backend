package ma.noba.provider.infra;

import ma.noba.provider.domain.model.ProviderSearchView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProviderSearchViewRepository extends JpaRepository<ProviderSearchView, UUID> {

    @Query("SELECT p FROM ProviderSearchView p WHERE p.isActive = true AND (:category IS NULL OR p.category = :category)")
    Page<ProviderSearchView> findByCategory(String category, Pageable pageable);

    @Query("SELECT p FROM ProviderSearchView p WHERE p.isActive = true AND p.ratingAverage >= :minRating")
    List<ProviderSearchView> findTopRated(double minRating);
}
