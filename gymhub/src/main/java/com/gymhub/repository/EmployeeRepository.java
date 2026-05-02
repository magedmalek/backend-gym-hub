package com.gymhub.repository;

import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Page<Employee> findByGymId(Long gymId, Pageable pageable);

    List<Employee> findByGymIdAndActive(Long gymId, boolean active);

    Optional<Employee> findByUserIdAndGymId(Long userId, Long gymId);

    boolean existsByUserIdAndGymId(Long userId, Long gymId);

    @Query("SELECT e FROM Employee e JOIN e.permissions p " +
           "WHERE e.gym.id = :gymId AND p = :permission AND e.active = true")
    List<Employee> findByGymIdAndPermission(@Param("gymId") Long gymId,
                                            @Param("permission") EmployeePermission permission);
}
