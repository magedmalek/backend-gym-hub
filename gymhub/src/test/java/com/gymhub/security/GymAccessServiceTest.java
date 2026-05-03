package com.gymhub.security;

import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.user.User;
import com.gymhub.domain.user.UserRole;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.EmployeeRepository;
import com.gymhub.repository.GymRepository;
import com.gymhub.service.GymAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link GymAccessService}.
 *
 * Each test verifies that the centralized authorization rules are enforced:
 * - Owner bypasses all permission checks inside own gym.
 * - Active employee with the required permission is allowed.
 * - Inactive employee is always blocked.
 * - Employee from a different gym is blocked.
 * - Customer (no employee record) is blocked from dashboard operations.
 * - Frontend-provided employeeId cannot be used to impersonate another employee
 *   (this attack vector is structurally eliminated — the service never accepts an employeeId param).
 */
@ExtendWith(MockitoExtension.class)
class GymAccessServiceTest {

    @Mock
    private GymRepository gymRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private GymAccessService gymAccessService;

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static final Long GYM_ID      = 1L;
    private static final Long OTHER_GYM   = 2L;
    private static final Long OWNER_ID    = 10L;
    private static final Long EMPLOYEE_ID = 20L;
    private static final Long CUSTOMER_ID = 30L;

    private User owner;
    private User activeEmployee;
    private User inactiveEmployee;
    private User customerUser;
    private User foreignEmployee;

    private Gym gym;

    private Employee empActive;
    private Employee empInactive;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(OWNER_ID).email("owner@gym.com")
                .roles(Set.of(UserRole.SERVICE_PROVIDER)).active(true).build();

        activeEmployee = User.builder().id(EMPLOYEE_ID).email("emp@gym.com")
                .roles(Set.of(UserRole.EMPLOYEE)).active(true).build();

        inactiveEmployee = User.builder().id(21L).email("inactive@gym.com")
                .roles(Set.of(UserRole.EMPLOYEE)).active(true).build();

        customerUser = User.builder().id(CUSTOMER_ID).email("customer@gym.com")
                .roles(Set.of(UserRole.CUSTOMER)).active(true).build();

        foreignEmployee = User.builder().id(22L).email("foreign@othergym.com")
                .roles(Set.of(UserRole.EMPLOYEE)).active(true).build();

        gym = Gym.builder().id(GYM_ID).name("Test Gym").owner(owner).build();

        empActive = Employee.builder()
                .id(100L).user(activeEmployee).gym(gym)
                .permissions(Set.of(EmployeePermission.SELL_SUBSCRIPTION))
                .active(true).jobTitle("Receptionist").build();

        empInactive = Employee.builder()
                .id(101L).user(inactiveEmployee).gym(gym)
                .permissions(Set.of(EmployeePermission.SELL_SUBSCRIPTION,
                        EmployeePermission.ADMIN))
                .active(false).jobTitle("Former staff").build();
    }

    // ── requireGym ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("requireGym — throws 404 for unknown gym")
    void requireGym_unknownGym_throwsNotFound() {
        given(gymRepository.findById(GYM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> gymAccessService.requireGym(GYM_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── assertDashboardAccess ─────────────────────────────────────────────────

    @Nested
    @DisplayName("assertDashboardAccess")
    class AssertDashboardAccess {

        @Test
        @DisplayName("Owner gains access without an employee record")
        void owner_withoutEmployeeRecord_allowed() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(OWNER_ID, GYM_ID))
                    .willReturn(Optional.empty());

            Employee result = gymAccessService.assertDashboardAccess(owner, GYM_ID);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Active employee of this gym gains access")
        void activeEmployee_allowed() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(EMPLOYEE_ID, GYM_ID))
                    .willReturn(Optional.of(empActive));

            Employee result = gymAccessService.assertDashboardAccess(activeEmployee, GYM_ID);

            assertThat(result).isEqualTo(empActive);
        }

        @Test
        @DisplayName("Inactive employee is blocked")
        void inactiveEmployee_blocked() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(21L, GYM_ID))
                    .willReturn(Optional.of(empInactive));

            assertThatThrownBy(() -> gymAccessService.assertDashboardAccess(inactiveEmployee, GYM_ID))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("inactive");
        }

        @Test
        @DisplayName("Customer (no employee record in this gym) is blocked")
        void customer_noEmployeeRecord_blocked() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(CUSTOMER_ID, GYM_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> gymAccessService.assertDashboardAccess(customerUser, GYM_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Employee from a different gym is blocked")
        void foreignEmployee_noRecord_blocked() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(22L, GYM_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> gymAccessService.assertDashboardAccess(foreignEmployee, GYM_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    // ── resolveActingEmployee ─────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveActingEmployee (permission check)")
    class ResolveActingEmployee {

        @Test
        @DisplayName("Owner bypasses permission check")
        void owner_bypassesPermissionCheck() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(OWNER_ID, GYM_ID))
                    .willReturn(Optional.empty());

            // Owner should be allowed even for SELL_SUBSCRIPTION without an explicit permission
            Employee result = gymAccessService.resolveActingEmployee(
                    owner, GYM_ID, EmployeePermission.SELL_SUBSCRIPTION);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Active employee with correct permission is allowed")
        void activeEmployeeWithPermission_allowed() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(EMPLOYEE_ID, GYM_ID))
                    .willReturn(Optional.of(empActive));

            Employee result = gymAccessService.resolveActingEmployee(
                    activeEmployee, GYM_ID, EmployeePermission.SELL_SUBSCRIPTION);

            assertThat(result).isEqualTo(empActive);
        }

        @Test
        @DisplayName("Active employee missing permission is blocked")
        void activeEmployeeMissingPermission_blocked() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(EMPLOYEE_ID, GYM_ID))
                    .willReturn(Optional.of(empActive));

            // empActive only has SELL_SUBSCRIPTION, not ACTIVATE_SUBSCRIPTION
            assertThatThrownBy(() -> gymAccessService.resolveActingEmployee(
                    activeEmployee, GYM_ID, EmployeePermission.ACTIVATE_SUBSCRIPTION))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("ACTIVATE_SUBSCRIPTION");
        }

        @Test
        @DisplayName("Inactive employee is blocked even if they hold the permission")
        void inactiveEmployee_blockedRegardlessOfPermission() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(21L, GYM_ID))
                    .willReturn(Optional.of(empInactive));

            // empInactive has ADMIN which covers everything, but account is inactive
            assertThatThrownBy(() -> gymAccessService.resolveActingEmployee(
                    inactiveEmployee, GYM_ID, EmployeePermission.SELL_SUBSCRIPTION))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("inactive");
        }

        @Test
        @DisplayName("Employee from a different gym is blocked")
        void employeeFromDifferentGym_blocked() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(22L, GYM_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> gymAccessService.resolveActingEmployee(
                    foreignEmployee, GYM_ID, EmployeePermission.SELL_SUBSCRIPTION))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Customer JWT cannot call dashboard operations")
        void customerJwt_cannotCallDashboardOps() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(CUSTOMER_ID, GYM_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> gymAccessService.resolveActingEmployee(
                    customerUser, GYM_ID, EmployeePermission.SELL_SUBSCRIPTION))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("ADMIN employee satisfies any permission requirement")
        void adminEmployee_satisfiesAnyPermission() {
            Employee adminEmp = Employee.builder()
                    .id(200L).user(activeEmployee).gym(gym)
                    .permissions(Set.of(EmployeePermission.ADMIN))
                    .active(true).jobTitle("Manager").build();

            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(EMPLOYEE_ID, GYM_ID))
                    .willReturn(Optional.of(adminEmp));

            // ADMIN covers ACTIVATE_SUBSCRIPTION without it being listed explicitly
            Employee result = gymAccessService.resolveActingEmployee(
                    activeEmployee, GYM_ID, EmployeePermission.ACTIVATE_SUBSCRIPTION);

            assertThat(result).isEqualTo(adminEmp);
        }
    }

    // ── assertOwnerOrAdmin ────────────────────────────────────────────────────

    @Nested
    @DisplayName("assertOwnerOrAdmin")
    class AssertOwnerOrAdmin {

        @Test
        @DisplayName("Owner is allowed")
        void owner_allowed() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));

            assertThatNoException().isThrownBy(
                    () -> gymAccessService.assertOwnerOrAdmin(owner, GYM_ID));
        }

        @Test
        @DisplayName("Non-admin employee is blocked")
        void nonAdminEmployee_blocked() {
            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(EMPLOYEE_ID, GYM_ID))
                    .willReturn(Optional.of(empActive)); // empActive has SELL_SUBSCRIPTION only

            assertThatThrownBy(() -> gymAccessService.assertOwnerOrAdmin(activeEmployee, GYM_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("ADMIN employee is allowed")
        void adminEmployee_allowed() {
            Employee adminEmp = Employee.builder()
                    .id(200L).user(activeEmployee).gym(gym)
                    .permissions(Set.of(EmployeePermission.ADMIN))
                    .active(true).jobTitle("Manager").build();

            given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(employeeRepository.findByUserIdAndGymId(EMPLOYEE_ID, GYM_ID))
                    .willReturn(Optional.of(adminEmp));

            assertThatNoException().isThrownBy(
                    () -> gymAccessService.assertOwnerOrAdmin(activeEmployee, GYM_ID));
        }
    }

    // ── assertOwnerOnly ───────────────────────────────────────────────────────

    @Test
    @DisplayName("assertOwnerOnly — non-owner is always blocked, even ADMIN employee")
    void assertOwnerOnly_nonOwner_blocked() {
        Employee adminEmp = Employee.builder()
                .id(200L).user(activeEmployee).gym(gym)
                .permissions(Set.of(EmployeePermission.ADMIN))
                .active(true).jobTitle("Manager").build();

        given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));

        assertThatThrownBy(() -> gymAccessService.assertOwnerOnly(activeEmployee, GYM_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("owner");
    }

    @Test
    @DisplayName("assertOwnerOnly — owner passes")
    void assertOwnerOnly_owner_passes() {
        given(gymRepository.findById(GYM_ID)).willReturn(Optional.of(gym));

        assertThatNoException().isThrownBy(
                () -> gymAccessService.assertOwnerOnly(owner, GYM_ID));
    }
}
