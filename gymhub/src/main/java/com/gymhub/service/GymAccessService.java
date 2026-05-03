package com.gymhub.service;

import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.user.User;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.EmployeeRepository;
import com.gymhub.repository.GymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized authorization service for all dashboard operations.
 *
 * <p>Security model:
 * <ul>
 *   <li>The gym <b>owner</b> has full access inside their gym — no employee record required,
 *       no explicit permission check.</li>
 *   <li>An <b>active employee</b> of the gym can act when they hold the required permission
 *       (or ADMIN which supersedes every other permission).</li>
 *   <li>An <b>inactive employee</b> is always blocked.</li>
 *   <li>Users who are neither the owner nor an employee of the gym are always blocked.</li>
 *   <li><b>Customers</b> are implicitly blocked — they have no employee record.</li>
 * </ul>
 *
 * <p><strong>employeeId must NEVER be accepted from the frontend</strong> as the acting
 * identity. Always call one of these methods with the JWT-resolved {@link User}.
 */
@Service
@RequiredArgsConstructor
public class GymAccessService {

    private final GymRepository gymRepository;
    private final EmployeeRepository employeeRepository;

    // ── Core resolution ───────────────────────────────────────────────────────

    /**
     * Load and verify the gym, throwing 404 if it does not exist.
     */
    @Transactional(readOnly = true)
    public Gym requireGym(Long gymId) {
        return gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym", gymId));
    }

    /**
     * Check whether {@code user} is the owner of gym {@code gymId}.
     */
    @Transactional(readOnly = true)
    public boolean isOwner(User user, Long gymId) {
        return gymRepository.findById(gymId)
                .map(g -> g.getOwner().getId().equals(user.getId()))
                .orElse(false);
    }

    // ── Dashboard access assertions ───────────────────────────────────────────

    /**
     * Assert that the user is either the gym owner or an active employee of the gym.
     * Used for read-only dashboard endpoints that should not be visible to customers.
     *
     * @return the Employee record if one exists, or {@code null} if the user is the owner
     *         without an employee record
     */
    @Transactional(readOnly = true)
    public Employee assertDashboardAccess(User user, Long gymId) {
        Gym gym = requireGym(gymId);
        if (gym.getOwner().getId().equals(user.getId())) {
            // Owner: return their employee record if it exists (may be null)
            return employeeRepository.findByUserIdAndGymId(user.getId(), gymId).orElse(null);
        }
        Employee emp = employeeRepository.findByUserIdAndGymId(user.getId(), gymId)
                .orElseThrow(() -> new UnauthorizedException(
                        "You do not have access to this gym's dashboard"));
        if (!emp.isActive()) {
            throw new UnauthorizedException("Your employee account is inactive");
        }
        return emp;
    }

    /**
     * Resolve the acting employee for a sensitive write operation.
     * <p>
     * Rules:
     * <ul>
     *   <li>Owner → all permissions granted; returns their Employee record (or null if
     *       they have no explicit employee entry in this gym).</li>
     *   <li>Non-owner → must be an <b>active</b> employee of this gym and hold
     *       {@code requiredPermission}.</li>
     * </ul>
     *
     * @param user               the JWT-authenticated user
     * @param gymId              the gym being operated on
     * @param requiredPermission the permission that non-owner employees must hold
     * @return the acting {@link Employee} record (may be {@code null} for owner with no record)
     * @throws UnauthorizedException if the user lacks access or permission
     */
    @Transactional(readOnly = true)
    public Employee resolveActingEmployee(User user, Long gymId, EmployeePermission requiredPermission) {
        Gym gym = requireGym(gymId);

        // ── Owner path: full access, no permission check needed ───────────────
        if (gym.getOwner().getId().equals(user.getId())) {
            return employeeRepository.findByUserIdAndGymId(user.getId(), gymId).orElse(null);
        }

        // ── Employee path: must be active and have the required permission ─────
        Employee emp = employeeRepository.findByUserIdAndGymId(user.getId(), gymId)
                .orElseThrow(() -> new UnauthorizedException(
                        "You are not an employee of this gym"));

        if (!emp.isActive()) {
            throw new UnauthorizedException("Your employee account is inactive");
        }

        if (!emp.hasPermission(requiredPermission)) {
            throw new UnauthorizedException(
                    "Missing required permission: " + requiredPermission.name());
        }

        return emp;
    }

    /**
     * Assert that the acting user is the gym owner or an ADMIN employee.
     * Used for sensitive management operations (employee management, settings, etc.).
     *
     * @return the acting {@link Employee} if an admin employee, or {@code null} if the owner
     */
    @Transactional(readOnly = true)
    public Employee assertOwnerOrAdmin(User user, Long gymId) {
        Gym gym = requireGym(gymId);

        if (gym.getOwner().getId().equals(user.getId())) {
            return employeeRepository.findByUserIdAndGymId(user.getId(), gymId).orElse(null);
        }

        Employee emp = employeeRepository.findByUserIdAndGymId(user.getId(), gymId)
                .orElseThrow(() -> new UnauthorizedException(
                        "You are not an employee of this gym"));

        if (!emp.isActive()) {
            throw new UnauthorizedException("Your employee account is inactive");
        }

        if (!emp.hasPermission(EmployeePermission.ADMIN)) {
            throw new UnauthorizedException(
                    "Only the gym owner or an ADMIN employee can perform this action");
        }

        return emp;
    }

    /**
     * Assert that the user is strictly the gym owner.
     * Used for operations that only the owner can perform (e.g., changing gym ownership data).
     */
    @Transactional(readOnly = true)
    public void assertOwnerOnly(User user, Long gymId) {
        Gym gym = requireGym(gymId);
        if (!gym.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the gym owner can perform this action");
        }
    }
}
