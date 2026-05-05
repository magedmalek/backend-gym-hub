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
    private final PasswordEncoder passwordEncoder;

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

        Employee employee = Employee.builder()
                .user(user)
                .gym(gym)
                .jobTitle(request.getJobTitle())
                .salary(request.getSalary())
                .salaryCurrency(request.getSalaryCurrency() != null ? request.getSalaryCurrency() : "EGP")
                .notes(request.getNotes())
                .active(true)
                .build();

        if (request.getPermissions() != null) {
            employee.setPermissions(request.getPermissions());
        }

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
        gymAccessService.assertOwnerOrAdmin(currentUser, gymId);
        Employee emp = findOrThrow(employeeId);
        if (!emp.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("Employee not found in this gym");
        }
        emp.setPermissions(permissions);
        return toResponse(employeeRepository.save(emp));
    }

    @Transactional
    public void toggleStatus(Long gymId, Long employeeId, boolean active, User currentUser) {
        gymAccessService.assertOwnerOrAdmin(currentUser, gymId);
        Employee emp = findOrThrow(employeeId);
        if (!emp.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("Employee not found in this gym");
        }
        emp.setActive(active);
        employeeRepository.save(emp);
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
                .active(emp.isActive())
                .createdAt(emp.getCreatedAt())
                .build();
    }
}
