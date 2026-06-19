package com.gymhub.repository;

import com.gymhub.domain.provider.Provider;
import com.gymhub.domain.provider.ProviderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    List<Provider> findByOwnerUserId(Long ownerUserId);

    Optional<Provider> findByLinkedGymId(Long gymId);

    Page<Provider> findByProviderTypeAndActiveTrue(ProviderType providerType, Pageable pageable);

    Page<Provider> findByActiveTrue(Pageable pageable);

    boolean existsByLinkedGymId(Long gymId);
}
