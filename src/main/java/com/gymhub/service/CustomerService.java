package com.gymhub.service;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.user.User;
import com.gymhub.domain.user.UserRole;
import com.gymhub.dto.request.CreateCustomerRequest;
import com.gymhub.dto.response.CustomerResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.DuplicateResourceException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UserAlreadyExistsException;
import com.gymhub.repository.CustomerRepository;
import com.gymhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final GymManagementService gymManagementService;
    private final GymAccessService gymAccessService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CustomerResponse createCustomer(Long gymId, CreateCustomerRequest request, User currentUser) {
        gymAccessService.resolveActingEmployee(currentUser, gymId, EmployeePermission.MANAGE_CUSTOMERS);
        Gym gym = gymManagementService.findGymOrThrow(gymId);

        // Phone-based duplicate check: if a user with this phone already exists on the
        // platform but is NOT the one we are explicitly linking, block the creation.
        if (request.getExistingUserId() == null && request.getPhone() != null) {
            userRepository.findByPhone(request.getPhone()).ifPresent(existing -> {
                // Existing user found — dashboard cannot silently link or duplicate them.
                // The user must request linking via the customer app.
                throw new UserAlreadyExistsException(request.getPhone());
            });
        }

        User user = resolveOrCreateUser(request);

        if (customerRepository.existsByUserIdAndGymId(user.getId(), gymId)) {
            throw new DuplicateResourceException("User is already a customer of this gym");
        }

        String memberCode = request.getMemberCode() != null
                ? request.getMemberCode()
                : generateMemberCode(gymId);

        if (customerRepository.existsByMemberCode(memberCode)) {
            throw new DuplicateResourceException("Member code already in use: " + memberCode);
        }

        // Ensure CUSTOMER role
        if (!user.getRoles().contains(UserRole.CUSTOMER)) {
            user.addRole(UserRole.CUSTOMER);
            userRepository.save(user);
        }

        Customer customer = Customer.builder()
                .user(user)
                .gym(gym)
                .memberCode(memberCode)
                .active(true)
                .build();

        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(Long gymId, User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return customerRepository.findByGymId(gymId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchCustomers(Long gymId, String query,
                                                   User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return customerRepository.searchByGymId(gymId, query, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long gymId, Long customerId, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        Customer c = findOrThrow(customerId);
        assertBelongsToGym(c, gymId);
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getByMemberCode(Long gymId, String memberCode, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        Customer c = customerRepository.findByMemberCode(memberCode)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for code: " + memberCode));
        return toResponse(c);
    }

    @Transactional
    public void toggleStatus(Long gymId, Long customerId, boolean active, User currentUser) {
        gymAccessService.resolveActingEmployee(currentUser, gymId, EmployeePermission.MANAGE_CUSTOMERS);
        Customer c = findOrThrow(customerId);
        assertBelongsToGym(c, gymId);
        c.setActive(active);
        customerRepository.save(c);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public Customer findOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private void assertBelongsToGym(Customer c, Long gymId) {
        if (!c.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("Customer not found in this gym");
        }
    }

    private User resolveOrCreateUser(CreateCustomerRequest request) {
        if (request.getExistingUserId() != null) {
            return userRepository.findById(request.getExistingUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getExistingUserId()));
        }

        if (request.getEmail() == null) {
            throw new BusinessException("Email is required when creating a new customer user");
        }

        return userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    if (request.getPassword() == null) {
                        throw new BusinessException("Password is required for a new user");
                    }
                    User user = User.builder()
                            .firstName(request.getFirstName() != null ? request.getFirstName() : "")
                            .lastName(request.getLastName() != null ? request.getLastName() : "")
                            .email(request.getEmail())
                            .phone(request.getPhone())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .active(true)
                            .build();
                    user.addRole(UserRole.CUSTOMER);
                    return userRepository.save(user);
                });
    }

    /**
     * Creates a minimal guest user account (no gym, no subscription link).
     * Used when an invited guest has no existing account.
     */
    @Transactional
    public User createGuestUser(String firstName, String lastName, String email, String phone) {
        if (email != null) {
            return userRepository.findByEmail(email).orElseGet(() -> buildGuestUser(firstName, lastName, email, phone));
        }
        return buildGuestUser(firstName, lastName, email, phone);
    }

    private User buildGuestUser(String firstName, String lastName, String email, String phone) {
        User user = User.builder()
                .firstName(firstName != null ? firstName : "Guest")
                .lastName(lastName != null ? lastName : "")
                .email(email != null ? email : "guest+" + UUID.randomUUID() + "@gymhub.local")
                .phone(phone)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .active(true)
                .build();
        // No role assigned — guest account only
        return userRepository.save(user);
    }

    private String generateMemberCode(Long gymId) {
        return "GH" + gymId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .userId(c.getUser().getId())
                .fullName(c.getUser().getFullName())
                .email(c.getUser().getEmail())
                .phone(c.getUser().getPhone())
                .profileImageUrl(c.getUser().getProfileImageUrl())
                .gymId(c.getGym().getId())
                .gymName(c.getGym().getName())
                .memberCode(c.getMemberCode())
                .active(c.isActive())
                .joinedAt(c.getJoinedAt())
                .build();
    }
}
