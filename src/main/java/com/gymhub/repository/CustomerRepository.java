package com.gymhub.repository;

import com.gymhub.domain.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUserIdAndGymId(Long userId, Long gymId);

    Optional<Customer> findByMemberCode(String memberCode);

    boolean existsByUserIdAndGymId(Long userId, Long gymId);

    boolean existsByMemberCode(String memberCode);

    Page<Customer> findByGymId(Long gymId, Pageable pageable);

    long countByGymId(Long gymId);

    long countByGymIdAndActiveTrue(Long gymId);

    List<Customer> findByUserId(Long userId);

    @Query("SELECT c FROM Customer c " +
           "JOIN c.user u " +
           "WHERE c.gym.id = :gymId " +
           "AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "  OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%',:q,'%')) " +
           "  OR LOWER(u.email)     LIKE LOWER(CONCAT('%',:q,'%')) " +
           "  OR c.memberCode       LIKE CONCAT('%',:q,'%'))")
    Page<Customer> searchByGymId(@Param("gymId") Long gymId,
                                  @Param("q") String query,
                                  Pageable pageable);
}
