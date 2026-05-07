package com.gymhub.service;

import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.user.User;
import com.gymhub.domain.user.UserRole;
import com.gymhub.dto.request.CreateEmployeeRequest;
import com.gymhub.dto.response.EmployeeResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.DuplicateResourceException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.GymRepository;
import com.gymhub.repository.EmployeeRepository;
import com.gymhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeManagementService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final GymManagementService gymService;
    private final GymAccessService gymAccessService;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int OWNER_HIERARCHY_LEVEL = 100;
    private static final int ADMIN_HIERARCHY_LEVEL = 50;

    @Transactional
    public EmployeeResponse createEmployee(Long gymId, CreateEmployeeRequest request, User currentUser) {
        // Only gym owner or ADMIN employee can add employees
        gymAccessService.assertOwnerOrAdmin(currentUser, gymId);
        Gym gym = gymService.findGymOrThrow(gymId);

        User user = resolveOrCreateUser(request);

        if (employeeRepository.existsByUserIdAndGymId(user.getId(), gymId)) {
            throw new DuplicateResourceException("User is already an employee of this gym");
        }

        // Ensure EMPLOYEE role
        if (!user.getRoles().contains(UserRole.EMPLOYEE)) {
            user.addRole(UserRole.EMPLOYEE);
            userRepository.save(user);
        }

        Set<EmployeePermission> perms = request.getPermissions() != null
                ? request.getPermissions() : new java.util.HashSet<>();

        int hierarchyLevel = perms.contains(EmployeePermission.ADMIN) ? ADMIN_HIERARCHY_LEVEL : 1;

        Employee employee = Employee.builder()
                .user(user)
                .gym(gym)
                .jobTitle(request.getJobTitle())
                .salary(request.getSalary())
                .salaryCurrency(request.getSalaryCurrency() != null ? request.getSalaryCurrency() : "EGP")
                .notes(request.getNotes())
                .permissions(perms)
                .hierarchyLevel(hierarchyLevel)
                .active(true)
                .build();

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getEmployees(Long gymId, User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return employeeRepository.findByGymId(gymId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long gymId, Long employeeId, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        Employee emp = findOrThrow(employeeId);
        if (!emp.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("Employee not found in this gym");
        }
        return toResponse(emp);
    }

    @Transactional
    public EmployeeResponse updatePermissions(Long gymId, Long employeeId,
                                               Set<EmployeePermission> permissions, User currentUser) {
        Employee actor = gymAccessService.assertOwnerOrAdmin(currentUser, gymId);
        Employee target = findOrThrow(employeeId);
        if (!target.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("Employee not found in this gym");
        }
        assertCanManage(actor, target, gymId, currentUser);
        target.setPermissions(permissions);
        return toResponse(employeeRepository.save(target));
    }

    @Transactional
    public void toggleStatus(Long gymId, Long employeeId, boolean active, User currentUser) {
        Employee actor = gymAccessService.assertOwnerOrAdmin(currentUser, gymId);
        Employee target = findOrThrow(employeeId);
        if (!target.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("Employee not found in this gym");
        }
        assertCanManage(actor, target, gymId, currentUser);
        target.setActive(active);
        employeeRepository.save(target);
    }

    /**
     * Enforces that the acting employee/owner can only manage employees at a strictly
     * lower hierarchy level, and that the gym owner can never be deactivated or edited.
     */
    private void assertCanManage(Employee actor, Employee target, Long gymId, User currentUser) {
        // Protect the gym owner — they can never be managed from the dashboard
        boolean targetIsOwner = gymRepository.findById(gymId)
                .map(g -> g.getOwner().getId().equals(target.getUser().getId()))
                .orElse(false);
        if (targetIsOwner) {
            throw new UnauthorizedException("The gym owner cannot be deactivated or modified from the dashboard");
        }

        // If actor is the gym owner (no employee record or OWNER_HIERARCHY_LEVEL), allow all
        if (actor == null) return; // actor is the gym owner with no employee record

        int actorLevel = actor.getHierarchyLevel();
        int targetLevel = target.getHierarchyLevel();

        if (actorLevel <= targetLevel) {
            throw new UnauthorizedException(
                    "You can only manage employees with a lower hierarchy level than your own");
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public Employee findOrThrow(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
    }

    public Employee findByUserAndGym(Long userId, Long gymId) {
        return employeeRepository.findByUserIdAndGymId(userId, gymId)
                .orElseThrow(() -> new BusinessException("Employee not found for this user/gym combination"));
    }

    private User resolveOrCreateUser(CreateEmployeeRequest request) {
        // Try to find by email first
        if (request.getUserEmail() != null) {
            return userRepository.findByEmail(request.getUserEmail())
                    .orElseGet(() -> createMinimalUser(request));
        }
        throw new BusinessException("User email is required");
    }

    private User createMinimalUser(CreateEmployeeRequest request) {
        if (request.getFirstName() == null || request.getPassword() == null) {
            throw new BusinessException("New employee user requires firstName, lastName and password");
        }
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName() != null ? request.getLastName() : "")
                .email(request.getUserEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .build();
        user.addRole(UserRole.EMPLOYEE);
        return userRepository.save(user);
    }

    private EmployeeResponse toResponse(Employee emp) {
        return EmployeeResponse.builder()
                .id(emp.getId())
                .userId(emp.getUser().getId())
                .fullName(emp.getUser().getFullName())
                .email(emp.getUser().getEmail())
                .phone(emp.getUser().getPhone())
                .profileImageUrl(emp.getUser().getProfileImageUrl())
                .gymId(emp.getGym().getId())
                .gymName(emp.getGym().getName())
                .jobTitle(emp.getJobTitle())
                .salary(emp.getSalary())
                .salaryCurrency(emp.getSalaryCurrency())
                .permissions(emp.getPermissions())
                .hierarchyLevel(emp.getHierarchyLevel())
                .active(emp.isActive())
                .createdAt(emp.getCreatedAt())
                .build();
    }
}
