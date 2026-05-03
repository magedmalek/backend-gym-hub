# GymHub Backend — Update Implementation Plan

> **Generated:** Phase 0 Repository Audit  
> **Scope:** Dashboard hardening → Customer App APIs  
> **Cash only. No online payment. No fingerprint. No marketplace.**

---

## 1. Repository Audit — Current State

### 1.1 Module Inventory

| Module | Package | Status |
|---|---|---|
| Identity & Auth | `domain/user`, `security`, `service/AuthService` | ✅ Complete |
| Gym Management | `domain/gym`, `service/GymManagementService` | ⚠️ Partial (settings incomplete) |
| Employee Management | `domain/employee`, `service/EmployeeManagementService` | ⚠️ No caller-from-JWT resolution |
| Service Catalog | `domain/gymservice`, `service/ServiceCatalogService` | ✅ Functional |
| Packages | `domain/gympackage`, `service/PackageService` | ✅ Functional |
| Customers | `domain/customer`, `service/CustomerService` | ⚠️ Boolean status only, no lifecycle |
| Subscriptions | `domain/subscription`, `service/SubscriptionService` | ⚠️ Missing FROZEN/SUSPENDED/COMPLETED, no history |
| Payments | `domain/subscription/Payment` | ⚠️ Minimal — no cash ledger, no receipt numbers |
| Attendance | `domain/attendance`, `service/AttendanceService` | ⚠️ No lifecycle eligibility enforcement |
| Invitations | `domain/invitation`, `service/InvitationService` | ⚠️ No status, no expiry, no lifecycle |
| Extra Services | `domain/extraservice`, `service/ExtraServiceSaleService` | ✅ Functional (independent txn) |
| Sessions/Bookings | — | ❌ Missing |
| Cash Ledger | — | ❌ Missing |
| Customer App APIs | — | ❌ Missing |
| Notifications | — | ❌ Missing |
| Support Tickets | — | ❌ Missing |
| Ratings | — | ❌ Missing |
| Offers/Popups | — | ❌ Missing |

---

### 1.2 Current Entities

| Entity | File | Key Fields | Gaps |
|---|---|---|---|
| `User` | `domain/user/User.java` | id, firstName, lastName, email, phone, password, roles(Set), activeContext, active | None major |
| `Gym` | `domain/gym/Gym.java` | id, name, owner(User), status(GymStatus) | None major |
| `GymSettings` | `domain/gym/GymSettings.java` | allowPartialPayment, activationPolicy, enabledEntranceMethods | **Missing 12 settings fields** (gracePeriod, freeze, booking, waitlist…) |
| `Employee` | `domain/employee/Employee.java` | user, gym, jobTitle, permissions(Set), active | No `status` enum, only boolean |
| `Customer` | `domain/customer/Customer.java` | user, gym, memberCode, active(boolean) | **No lifecycle status enum** |
| `Subscription` | `domain/subscription/Subscription.java` | customer, gymPackage, totalPrice, paidAmount, soldBy, activatedBy, status | **Missing FROZEN, SUSPENDED, COMPLETED**; no history |
| `Payment` | `domain/subscription/Payment.java` | subscription, amount, receivedBy, notes | **No receipt number, no movementType, no session link** |
| `GymPackage` | `domain/gympackage/GymPackage.java` | name, durationDays, price, includedServices, maxInvitations | None major |
| `GymService` | `domain/gymservice/GymService.java` | name, canBeIncludedInPackage, canBeSoldIndependently | None major |
| `Attendance` | `domain/attendance/Attendance.java` | customer, gym, type, entranceMethod, subscription, recordedBy | No lifecycle check |
| `Invitation` | `domain/invitation/Invitation.java` | host, subscription, guestUser, gym, recordedBy, usedAt | **No status enum, no expiryDate, no allowedVisits** |
| `ExtraServiceTransaction` | `domain/extraservice/ExtraServiceTransaction.java` | customer, gym, service, amount, soldBy | No cash movement link |
| `ServiceUsage` | `domain/extraservice/ServiceUsage.java` | customer, subscription, service, recordedBy | None |

---

### 1.3 Current Enums

| Enum | Values | Gaps |
|---|---|---|
| `UserRole` | SERVICE_PROVIDER, EMPLOYEE, CUSTOMER | None |
| `GymStatus` | ACTIVE, INACTIVE, SUSPENDED | None |
| `ActivationPolicy` | IMMEDIATE, MANUAL | **Missing AFTER_FULL_PAYMENT, DEFERRED_START_ALLOWED** |
| `EntranceMethod` | (existing values) | Review |
| `AttendanceType` | MEMBER_VISIT, GUEST_VISIT | None |
| `SubscriptionStatus` | PENDING_PAYMENT, PENDING_ACTIVATION, ACTIVE, EXPIRED, CANCELLED | **Missing FROZEN, SUSPENDED, COMPLETED** |
| `ServiceStatus` | (existing values) | None |
| `EmployeePermission` | SELL_SUBSCRIPTION, ACTIVATE_SUBSCRIPTION, REGISTER_ATTENDANCE, REGISTER_INVITATION, SELL_EXTRA_SERVICE, MANAGE_SERVICES, MANAGE_PACKAGES, MANAGE_CUSTOMERS, ADMIN | **Missing CLOSE_CASH_DAY, FREEZE_SUBSCRIPTION, MANUAL_ADJUSTMENT, MANAGE_EMPLOYEES** |

---

### 1.4 Current Controllers

| Controller | Route Prefix | Gaps |
|---|---|---|
| `AuthController` | `/api/v1/auth` | ✅ Clean — uses `@AuthenticationPrincipal User` |
| `GymController` | `/api/v1/gyms` | ✅ Clean — uses `@AuthenticationPrincipal User` |
| `EmployeeController` | `/api/v1/gyms/{gymId}/employees` | ⚠️ No caller auth check for who can add/modify employees |
| `ServiceController` | `/api/v1/gyms/{gymId}/services` | ⚠️ No employee auth check |
| `PackageController` | `/api/v1/gyms/{gymId}/packages` | ⚠️ No employee auth check |
| `CustomerController` | `/api/v1/gyms/{gymId}/customers` | ⚠️ No employee auth check |
| `SubscriptionController` | `/api/v1/gyms/{gymId}/subscriptions` | 🚨 **Takes `sellerEmployeeId` / `employeeId` from request param** |
| `AttendanceController` | `/api/v1/gyms/{gymId}/attendance` | 🚨 **Takes `employeeId` from request param** |
| `InvitationController` | `/api/v1/gyms/{gymId}/invitations` | 🚨 **Takes `employeeId` from request param** |
| `ExtraServiceController` | `/api/v1/gyms/{gymId}/extra-services` | 🚨 **Takes `employeeId` from request param** |

---

### 1.5 Current Services

| Service | File | Gaps |
|---|---|---|
| `AuthService` | `service/AuthService.java` | ✅ Correct |
| `GymManagementService` | `service/GymManagementService.java` | ✅ Correct (owner-based auth) |
| `EmployeeManagementService` | `service/EmployeeManagementService.java` | `findByUserAndGym` exists but not used for caller auth |
| `ServiceCatalogService` | `service/ServiceCatalogService.java` | No caller auth |
| `PackageService` | `service/PackageService.java` | No caller auth |
| `CustomerService` | `service/CustomerService.java` | No caller auth |
| `SubscriptionService` | `service/SubscriptionService.java` | Uses `sellerEmployeeId` from request — **security gap** |
| `AttendanceService` | `service/AttendanceService.java` | Uses `employeeId` from request — **security gap** |
| `InvitationService` | `service/InvitationService.java` | Uses `employeeId` from request — **security gap** |
| `ExtraServiceSaleService` | `service/ExtraServiceSaleService.java` | Uses `employeeId` from request — **security gap** |

---

### 1.6 Current Repositories

| Repository | Key Query Methods |
|---|---|
| `UserRepository` | findByEmail, existsByEmail |
| `GymRepository` | findByOwnerId |
| `GymSettingsRepository` | findByGymId |
| `EmployeeRepository` | findByGymId, findByUserIdAndGymId, existsByUserIdAndGymId |
| `GymServiceRepository` | findByGymId |
| `GymPackageRepository` | findByGymId |
| `CustomerRepository` | findByGymId, searchByGymId, findByMemberCode, existsByUserIdAndGymId |
| `SubscriptionRepository` | findByGymId, findByCustomerId, findByIdAndGymId, findByCustomerIdAndStatus |
| `PaymentRepository` | (basic) |
| `AttendanceRepository` | findByGymIdOrderByVisitedAtDesc, findByCustomerIdOrderByVisitedAtDesc |
| `InvitationRepository` | findByHostId, findByGymId, existsBySubscriptionIdAndGuestUserId |
| `ExtraServiceTransactionRepository` | findByCustomerIdOrderBySoldAtDesc, findByGymIdOrderBySoldAtDesc |
| `ServiceUsageRepository` | findByCustomerIdOrderByUsedAtDesc |

---

### 1.7 Security / JWT Approach

- **JWT filter** (`JwtAuthenticationFilter`): loads the actual `User` JPA entity from DB; sets it as principal
- **Security principal**: `@AuthenticationPrincipal User currentUser` works in all controllers
- **SecurityConfig**: stateless, BCrypt, public: `/api/v1/auth/**`, Swagger
- **UserDetailsServiceImpl**: still used by DaoAuthenticationProvider during login
- **Farmer-style pattern**: ✅ correctly implemented

**Critical Gap**: While the JWT filter correctly sets the authenticated `User`, the service layer for subscription/attendance/invitation/extra-service operations ignores it entirely — they accept `employeeId` as a request parameter from the frontend. Any authenticated user can pass any employeeId and execute privileged operations as that employee.

---

### 1.8 Current Permission Approach

- `Employee.hasPermission(EmployeePermission p)` — checks if employee has specific permission OR ADMIN
- **Gap**: No centralized service to resolve "calling user → acting employee → permission check" in one place
- **Gap**: All sensitive service methods take raw `Long employeeId` instead of resolving from JWT

---

### 1.9 Current Response / Error Handling

| Component | Implementation |
|---|---|
| Error response | `ErrorResponse` (timestamp, status, error, validationErrors map) |
| Global handler | `GlobalExceptionHandler` — handles ResourceNotFoundException, BusinessException, DuplicateResourceException, UnauthorizedException, AccessDeniedException, BadCredentialsException, MethodArgumentNotValidException |
| Paged response | `PagedResponse<T>` wrapping Spring `Page<T>` |
| Auth response | `AuthResponse` (accessToken, refreshToken, userId, email, fullName, roles, activeContext) |

---

### 1.10 Current Documentation

| File | Status |
|---|---|
| `README.md` | ✅ Comprehensive dev guide |
| `.env.example` | ✅ Complete |
| Swagger/OpenAPI | ✅ SpringDoc configured, accessible at `/swagger-ui.html` |
| `GymHub_API_Guide.html` | ❌ Not present |

---

### 1.11 Current Tests

| File | Content |
|---|---|
| `GymHubApplicationTests.java` | Empty context load test only |

**All business rules, security rules, and lifecycle rules are completely untested.**

---

## 2. Gap Analysis Summary

### 🔴 Critical Gaps (Security)

1. **Employee impersonation vulnerability**: Subscription, Attendance, Invitation, ExtraService controllers accept `employeeId` as a URL/query param. A malicious frontend can pass any employee ID and execute privileged operations as that employee.
2. **No centralized authorization service**: No single place that says "can this JWT user perform action X in gym Y?"
3. **Cross-gym access not checked at controller level**: Service layer checks gym ownership per-entity but there's no unified guard.
4. **Customers can call dashboard endpoints**: No role check prevents a CUSTOMER-role JWT user from calling `/api/v1/gyms/{gymId}/subscriptions/sell`.

### 🟡 Functional Gaps

5. **GymSettings incomplete**: Missing 12 operational settings fields (freeze, grace period, booking, waitlist, etc.)
6. **Customer has no lifecycle status**: Only `boolean active` — no LEAD/TRIAL/FROZEN/SUSPENDED/BLOCKED
7. **Subscription missing statuses**: FROZEN, SUSPENDED, COMPLETED not present
8. **No lifecycle history**: No audit trail of status changes for customers or subscriptions
9. **No subscription lifecycle operations**: freeze/unfreeze/extend/suspend/cancel/reactivate/manual-adjust
10. **Payment is not a cash ledger**: No receipt number, no movementType, no daily closing concept
11. **No `CashMovement` entity**: No unified cash tracking across subscriptions and extra services
12. **Invitation has no status or lifecycle**: No CREATED/USED/EXPIRED/CANCELLED, no expiryDate, no allowedVisitsCount
13. **ActivationPolicy enum incomplete**: Missing AFTER_FULL_PAYMENT, DEFERRED_START_ALLOWED
14. **EmployeePermission incomplete**: Missing CLOSE_CASH_DAY, FREEZE_SUBSCRIPTION, MANUAL_ADJUSTMENT, MANAGE_EMPLOYEES

### 🔵 Missing Modules

15. **ServiceSession** — not implemented
16. **ServiceBooking** — not implemented
17. **Customer App APIs** (`/api/v1/customer/*`) — not implemented
18. **SubscriptionRequest** — not implemented
19. **Notifications** — not implemented
20. **Support Tickets** — not implemented
21. **Ratings** — not implemented
22. **Offers/Popups** — not implemented

---

## 3. Implementation Plan — Phase by Phase

---

### PHASE 1 — Dashboard Security & Authorization Fix

**Goal**: Eliminate `employeeId` from request parameters for all sensitive dashboard ops. Resolve the acting user from JWT.

#### New File: `GymAccessService.java`

```
service/GymAccessService.java
```

Responsibilities:
- `resolveActingEmployee(User currentUser, Long gymId)` — finds Employee record for this user in this gym, throws if not found
- `assertPermission(Employee emp, EmployeePermission perm)` — checks permission, throws UnauthorizedException if missing
- `assertOwnerOrEmployee(User user, Long gymId, EmployeePermission perm)` — owner bypasses permission check
- `resolveOwnerOrAdmin(User user, Long gymId)` — only owner or ADMIN employee can call
- `assertIsOwner(User user, Long gymId)` — strict owner check

#### Files to Modify

| File | Change |
|---|---|
| `SubscriptionController` | Remove `@RequestParam Long sellerEmployeeId/employeeId`; add `@AuthenticationPrincipal User` |
| `SubscriptionService.sellSubscription` | Accept `User currentUser, Long gymId` instead of `Long sellerEmployeeId` |
| `SubscriptionService.addPayment` | Resolve employee from JWT |
| `SubscriptionService.activateSubscription` | Resolve employee from JWT |
| `AttendanceController` | Remove `@RequestParam Long employeeId`; add `@AuthenticationPrincipal User` |
| `AttendanceService.recordAttendance` | Accept `User currentUser` |
| `InvitationController` | Remove `@RequestParam Long employeeId`; add `@AuthenticationPrincipal User` |
| `InvitationService.useInvitation` | Accept `User currentUser` |
| `ExtraServiceController` | Remove `@RequestParam Long employeeId`; add `@AuthenticationPrincipal User` |
| `ExtraServiceSaleService.sellExtraService` | Accept `User currentUser` |
| `EmployeeController` | Add `@AuthenticationPrincipal User` — only owner/ADMIN can add/modify/disable employees |
| `ServiceController` | Add `@AuthenticationPrincipal User` — check MANAGE_SERVICES permission |
| `PackageController` | Add `@AuthenticationPrincipal User` — check MANAGE_PACKAGES permission |
| `CustomerController` | Add `@AuthenticationPrincipal User` — check MANAGE_CUSTOMERS permission |

#### New Permissions to Add to `EmployeePermission` enum

```java
MANAGE_EMPLOYEES,       // Can add/edit/deactivate employees
CLOSE_CASH_DAY,         // Can perform daily cash closing
FREEZE_SUBSCRIPTION,    // Can freeze/unfreeze subscriptions
MANUAL_ADJUSTMENT       // Can perform manual subscription date/amount adjustments
```

#### New Tests (Phase 1)

- `DashboardAuthorizationTest` — integration tests:
  - Employee without SELL_SUBSCRIPTION cannot sell
  - Employee from different gym cannot act
  - Customer JWT cannot call dashboard endpoint
  - Owner can act inside own gym without explicit employee permission
  - Inactive employee is blocked
  - Frontend-provided employeeId in deprecated request body is ignored

---

### PHASE 2 — Gym Settings / Rules Engine

**Goal**: Extend `GymSettings` to cover all configurable operational policies.

#### Files to Modify

| File | Change |
|---|---|
| `GymSettings.java` | Add 12 new fields (see below) |
| `ActivationPolicy.java` | Add `AFTER_FULL_PAYMENT`, `DEFERRED_START_ALLOWED` |
| `GymSettingsRequest.java` | Add all new fields with validation |
| `GymManagementService.updateSettings` | Map new fields |
| `GymManagementService.createGym` | Set sensible defaults for all new settings |

#### New `GymSettings` Fields

```java
int gracePeriodDays                       // default 0
boolean allowSubscriptionFreeze           // default false
int maxFreezeDaysPerSubscriptionDefault   // default 30
boolean allowManualSubscriptionAdjustment // default false
boolean requireReasonForManualAdjustment  // default true
boolean allowGuestInvitations             // default true
int defaultInvitationExpiryDays           // default 30
boolean allowGuestRepeatVisitDefault      // default false
boolean allowBookingCancellation          // default true
int bookingCancellationCutoffHours        // default 2
boolean allowWaitlist                     // default true
boolean allowManualAttendanceOverride     // default false
```

#### Settings Enforcement

- `SubscriptionService.sellSubscription` — enforce `allowPartialPayment` from settings
- `AttendanceService.recordAttendance` — enforce `allowedEntranceMethods` from settings
- `InvitationService.useInvitation` — enforce `allowGuestInvitations`, `defaultInvitationExpiryDays`

#### New Tests (Phase 2)

- Partial payment blocked when `allowPartialPayment = false`
- IMMEDIATE policy auto-activates on full payment
- MANUAL policy does NOT auto-activate
- Invitation blocked when `allowGuestInvitations = false`

---

### PHASE 3 — Customer & Subscription Lifecycle

**Goal**: Full lifecycle statuses, history audit trail, and lifecycle operations.

#### New Enums

```
domain/customer/CustomerStatus.java
  → LEAD, TRIAL, ACTIVE, FROZEN, SUSPENDED, EXPIRED, BLOCKED
```

Add to `SubscriptionStatus.java`:
```java
FROZEN, SUSPENDED, COMPLETED
```

#### New Entities

```
domain/customer/CustomerStatusHistory.java
  → customerId, oldStatus, newStatus, changedByUserId, reason, changedAt

domain/subscription/SubscriptionStatusHistory.java
  → subscriptionId, oldStatus, newStatus, changedByUserId, reason, changedAt
```

#### Files to Modify

| File | Change |
|---|---|
| `Customer.java` | Replace `boolean active` with `CustomerStatus status` |
| `SubscriptionStatus.java` | Add FROZEN, SUSPENDED, COMPLETED |
| `CustomerRepository.java` | Add queries by status |
| `SubscriptionRepository.java` | Add queries by status |
| `CustomerService.java` | Replace `toggleStatus` with `updateStatus(CustomerStatus)` |
| `SubscriptionService.java` | Add freeze/unfreeze/extend/suspend/reactivate/cancel/manualAdjust |
| `AttendanceService.java` | Block FROZEN/SUSPENDED/EXPIRED/BLOCKED customers |

#### New Service Methods in `SubscriptionService`

```java
freezeSubscription(Long gymId, Long subId, FreezeRequest, User caller)
unfreezeSubscription(Long gymId, Long subId, User caller)
extendSubscription(Long gymId, Long subId, ExtendRequest, User caller)
suspendSubscription(Long gymId, Long subId, String reason, User caller)
reactivateSubscription(Long gymId, Long subId, User caller)
cancelSubscription(Long gymId, Long subId, String reason, User caller)
manualAdjustment(Long gymId, Long subId, ManualAdjustmentRequest, User caller)
getHistory(Long gymId, Long subId)
```

#### New Endpoints (SubscriptionController)

```
POST /api/v1/gyms/{gymId}/subscriptions/{id}/freeze
POST /api/v1/gyms/{gymId}/subscriptions/{id}/unfreeze
POST /api/v1/gyms/{gymId}/subscriptions/{id}/extend
POST /api/v1/gyms/{gymId}/subscriptions/{id}/suspend
POST /api/v1/gyms/{gymId}/subscriptions/{id}/reactivate
POST /api/v1/gyms/{gymId}/subscriptions/{id}/cancel
POST /api/v1/gyms/{gymId}/subscriptions/{id}/manual-adjustment
GET  /api/v1/gyms/{gymId}/subscriptions/{id}/history
```

#### New Tests (Phase 3)

- Freeze with past date rejected
- Freeze days > remaining balance rejected
- End date recalculated after unfreeze
- Manual adjustment requires reason when configured
- Attendance blocked for FROZEN subscription
- Customer BLOCKED status blocks all dashboard operations

---

### PHASE 4 — Cash-Only Financial Ledger

**Goal**: Traceable cash ledger for all financial transactions. CASH ONLY. No online payment.

#### New Enums

```
domain/cash/CashMovementType.java
  → SUBSCRIPTION_PAYMENT, EXTRA_SERVICE_PAYMENT, MANUAL_CORRECTION, REFUND
```

#### New Entities

```
domain/cash/CashMovement.java
  → id, gym, customer(nullable), subscription(nullable),
    extraServiceTransaction(nullable), amount, currency,
    movementType(CashMovementType), collectedBy(User),
    receiptNumber, notes, timestamp, closingId(nullable)

domain/cash/CashDayClosing.java
  → id, gym, closingDate, totalAmount, closedBy(User),
    closedAt, notes, movementCount
```

#### Files to Modify

| File | Change |
|---|---|
| `SubscriptionService.addPaymentInternal` | Also create `CashMovement` with type SUBSCRIPTION_PAYMENT |
| `ExtraServiceSaleService.sellExtraService` | Also create `CashMovement` with type EXTRA_SERVICE_PAYMENT |

#### New Repositories

```
CashMovementRepository.java
  → findByGymId, findByGymIdAndTimestampBetween, findByCustomerId, findByClosingId

CashDayClosingRepository.java
  → findByGymIdOrderByClosingDateDesc, findByGymIdAndClosingDate
```

#### New Service: `CashLedgerService.java`

```java
getMovements(Long gymId, Pageable)
getDailySummary(Long gymId, LocalDate date)
closeDay(Long gymId, LocalDate date, String notes, User caller)
getClosings(Long gymId, Pageable)
getCustomerMovements(Long gymId, Long customerId, Pageable)
addManualCorrection(Long gymId, ManualCorrectionRequest, User caller) // requires ADMIN
```

#### New Endpoints (`CashController.java`)

```
GET  /api/v1/gyms/{gymId}/cash/movements
GET  /api/v1/gyms/{gymId}/cash/daily-summary?date=YYYY-MM-DD
POST /api/v1/gyms/{gymId}/cash/close-day
GET  /api/v1/gyms/{gymId}/cash/closings
GET  /api/v1/gyms/{gymId}/cash/customers/{customerId}
```

#### Receipt Number Generation

Auto-generate per gym: `GH-{gymId}-{YYYYMMDD}-{sequence}`

#### New Tests (Phase 4)

- Cash movement created for subscription payment
- Cash movement created for extra service sale
- Closed day cannot be modified
- Cash close requires CLOSE_CASH_DAY permission
- Manual correction requires ADMIN permission and reason

---

### PHASE 5 — Services, Sessions, Bookings, Waitlist

**Goal**: Schedule-based service sessions and customer booking.

#### New Enums

```
domain/booking/BookingStatus.java
  → CONFIRMED, WAITLISTED, CANCELLED, COMPLETED, NO_SHOW
```

#### New Entities

```
domain/session/ServiceSession.java
  → id, gymService, gym, instructor(Employee, nullable),
    date, startTime, endTime, capacity, bookingCutoffHours,
    status(SCHEDULED/CANCELLED/COMPLETED)

domain/booking/ServiceBooking.java
  → id, serviceSession, customer, subscription(nullable),
    status(BookingStatus), bookedAt, cancelledAt, cancelReason,
    attendedAt, notes
```

#### New Repositories

```
ServiceSessionRepository.java
ServiceBookingRepository.java
```

#### New Service: `SessionBookingService.java`

```java
createSession(Long gymId, CreateSessionRequest, User caller)
getSessions(Long gymId, LocalDate from, LocalDate to, Pageable)
bookSession(Long gymId, Long sessionId, Long customerId, User caller) // dashboard
cancelBooking(Long gymId, Long bookingId, String reason, User caller)
markAttended(Long gymId, Long bookingId, User caller)
markNoShow(Long gymId, Long bookingId, User caller)
```

#### New Endpoints (`SessionController.java`)

```
POST /api/v1/gyms/{gymId}/service-sessions
GET  /api/v1/gyms/{gymId}/service-sessions
POST /api/v1/gyms/{gymId}/service-sessions/{sessionId}/bookings
POST /api/v1/gyms/{gymId}/bookings/{bookingId}/cancel
POST /api/v1/gyms/{gymId}/bookings/{bookingId}/attend
POST /api/v1/gyms/{gymId}/bookings/{bookingId}/no-show
```

#### Booking Rules

- Cannot book in the past
- Cannot exceed capacity → if `allowWaitlist=true`, status=WAITLISTED; else reject
- Cancellation promotes first waitlisted customer
- Duplicate booking rejected
- SUSPENDED/BLOCKED/EXPIRED customers cannot book
- Customer must have active subscription including the service OR paid extra service

#### New Tests (Phase 5)

- Past session booking rejected
- Over-capacity: waitlist when allowed, reject when disabled
- Cancellation promotes waitlisted customer
- Duplicate booking rejected
- Blocked customer cannot book

---

### PHASE 6 — Invitation Completion

**Goal**: Add full invitation lifecycle — status, expiry, multi-visit slots.

#### New Enum

```
domain/invitation/InvitationStatus.java
  → CREATED, USED, EXPIRED, CANCELLED
```

#### Files to Modify

| File | Change |
|---|---|
| `Invitation.java` | Add: status, expiryDate, allowedVisitsCount, usedVisitsCount, cancelReason |
| `InvitationService.java` | Support pre-creation (status=CREATED), usage tracking, expiry check, cancellation |
| `InvitationRepository.java` | Add: findByStatus, findExpiring |

#### Invitation Lifecycle

1. **Create invitation** (dashboard): status=CREATED, expiryDate = now + gymSettings.defaultInvitationExpiryDays
2. **Use invitation**: usedVisitsCount++; if usedVisitsCount >= allowedVisitsCount → status=USED
3. **Cancel invitation**: status=CANCELLED, cancelReason required
4. **Expiry sweep** (scheduled or on-demand): CREATED invitations past expiryDate → status=EXPIRED

#### New Endpoints

```
POST /api/v1/gyms/{gymId}/invitations/{id}/cancel
GET  /api/v1/gyms/{gymId}/invitations/{id}/history
GET  /api/v1/gyms/{gymId}/invitations?status=CREATED
```

#### New Tests (Phase 6)

- Expired invitation cannot be used
- Cancelled invitation cannot be used
- Visit count exceeded rejected
- Cancellation requires reason

---

### PHASE 7 — Customer App API Layer

**Goal**: Separate read/request APIs for the customer perspective. No dashboard data exposed.

#### Security Rules

- All `/api/v1/customer/**` endpoints resolve customer from JWT, never from request body/param
- CustomerApp endpoints are blocked for SERVICE_PROVIDER and EMPLOYEE roles (customer-only zone)
- No write operations that bypass gym approval

#### New Controller: `CustomerAppController.java`

Endpoints are listed in the spec. Split into logical groups:

| Group | Controller | Routes |
|---|---|---|
| Profile | `CustomerProfileController` | `/api/v1/customer/profile` GET/PUT |
| Gym Discovery | `CustomerGymController` | `/api/v1/customer/gyms/**` |
| Subscriptions | `CustomerSubscriptionController` | `/api/v1/customer/subscriptions/**`, `/subscription-requests/**` |
| Attendance | `CustomerAttendanceController` | `/api/v1/customer/attendance/**`, `/entry-code` |
| Invitations | `CustomerInvitationController` | `/api/v1/customer/invitations/**` |
| Sessions/Bookings | `CustomerBookingController` | `/api/v1/customer/service-sessions/**`, `/bookings/**` |
| Offers | `CustomerOfferController` | `/api/v1/customer/offers/**` |
| Notifications | `CustomerNotificationController` | `/api/v1/customer/notifications/**` |
| Support Tickets | `CustomerSupportController` | `/api/v1/customer/support-tickets/**` |
| Ratings | `CustomerRatingController` | `/api/v1/customer/bookings/{id}/rating`, `/ratings/**` |
| Home | `CustomerHomeController` | `/api/v1/customer/home` |

#### New Entities for Customer App

```
domain/customer/SubscriptionRequest.java
  → customer, gym, gymPackage, status(PENDING/ACCEPTED/REJECTED/CANCELLED),
    requestedAt, processedAt, processedBy(Employee), notes

domain/notification/Notification.java
  → user, gymId(nullable), title, body, type, read, createdAt

domain/support/SupportTicket.java
  → customer, gym, subject, description, status(OPEN/IN_PROGRESS/CLOSED),
    createdAt, closedAt, closedBy

domain/rating/Rating.java
  → booking(ServiceBooking), customer, score(1-5), comment, createdAt

domain/offer/Offer.java
  → gym(nullable for platform-wide), title, description, imageUrl,
    targetRole, startAt, endAt, active
    
domain/offer/OfferView.java
  → offer, user, viewedAt, dismissed
```

---

## 4. Files/Modules Summary — What to Create vs Modify

### New Files to Create

```
# Phase 1
service/GymAccessService.java

# Phase 2
(GymSettings fields added inline, ActivationPolicy enum extended)

# Phase 3
domain/customer/CustomerStatus.java
domain/customer/CustomerStatusHistory.java
domain/subscription/SubscriptionStatusHistory.java
repository/CustomerStatusHistoryRepository.java
repository/SubscriptionStatusHistoryRepository.java
dto/request/FreezeSubscriptionRequest.java
dto/request/ExtendSubscriptionRequest.java
dto/request/ManualAdjustmentRequest.java
dto/response/SubscriptionHistoryResponse.java

# Phase 4
domain/cash/CashMovementType.java
domain/cash/CashMovement.java
domain/cash/CashDayClosing.java
repository/CashMovementRepository.java
repository/CashDayClosingRepository.java
service/CashLedgerService.java
controller/CashController.java
dto/request/CloseDayRequest.java
dto/response/CashMovementResponse.java
dto/response/CashDailySummaryResponse.java

# Phase 5
domain/session/ServiceSession.java
domain/session/SessionStatus.java
domain/booking/ServiceBooking.java
domain/booking/BookingStatus.java
repository/ServiceSessionRepository.java
repository/ServiceBookingRepository.java
service/SessionBookingService.java
controller/SessionController.java
dto/request/CreateSessionRequest.java
dto/request/BookSessionRequest.java
dto/response/SessionResponse.java
dto/response/BookingResponse.java

# Phase 6
domain/invitation/InvitationStatus.java
(Invitation.java modified inline)
dto/response/InvitationResponse.java

# Phase 7
domain/customer/SubscriptionRequest.java
domain/customer/SubscriptionRequestStatus.java
domain/notification/Notification.java
domain/support/SupportTicket.java
domain/support/SupportTicketStatus.java
domain/rating/Rating.java
domain/offer/Offer.java
domain/offer/OfferView.java
repository/* (7 new repositories)
service/CustomerAppService.java
service/NotificationService.java
service/SupportTicketService.java
service/RatingService.java
service/OfferService.java
controller/customer/* (11 new controllers)
dto/request/customer/* (multiple)
dto/response/customer/* (multiple)
```

### Files to Modify

```
# Phase 1
security/JwtAuthenticationFilter.java  (no change needed — already correct)
controller/SubscriptionController.java
controller/AttendanceController.java
controller/InvitationController.java
controller/ExtraServiceController.java
controller/EmployeeController.java
controller/ServiceController.java
controller/PackageController.java
controller/CustomerController.java
service/SubscriptionService.java
service/AttendanceService.java
service/InvitationService.java
service/ExtraServiceSaleService.java
service/EmployeeManagementService.java
domain/employee/EmployeePermission.java  (add new permissions)

# Phase 2
domain/gym/GymSettings.java
domain/gym/ActivationPolicy.java
dto/request/GymSettingsRequest.java
service/GymManagementService.java
service/SubscriptionService.java  (enforce settings)
service/InvitationService.java    (enforce settings)

# Phase 3
domain/customer/Customer.java
domain/subscription/SubscriptionStatus.java
service/CustomerService.java
service/SubscriptionService.java
service/AttendanceService.java
repository/CustomerRepository.java
repository/SubscriptionRepository.java

# Phase 4
service/SubscriptionService.java
service/ExtraServiceSaleService.java

# Phase 6
domain/invitation/Invitation.java
service/InvitationService.java
repository/InvitationRepository.java
```

---

## 5. Test Plan Summary

| Phase | Test Class | Key Scenarios |
|---|---|---|
| 1 | `DashboardAuthorizationTest` | Employee impersonation blocked, cross-gym blocked, customer blocked, owner allowed, inactive employee blocked |
| 2 | `GymSettingsEnforcementTest` | Partial payment settings, activation policy variants, invitation settings |
| 3 | `SubscriptionLifecycleTest` | Freeze rules, unfreeze date recalc, extend, cancel, suspend, attendance eligibility |
| 4 | `CashLedgerTest` | Movement created per payment, closing authorization, closed-day immutability |
| 5 | `SessionBookingTest` | Past booking rejected, capacity/waitlist, duplicate booking, cancellation promotion |
| 6 | `InvitationLifecycleTest` | Expiry, cancel, visit count |
| 7 | `CustomerAppTest` | Self-data only, no dashboard access, subscription request lifecycle, entry code eligibility |

---

## 6. Execution Order

```
Phase 0  ← YOU ARE HERE (Audit + Plan complete)
Phase 1  ← NEXT: Security fix (highest priority — all other phases depend on this)
Phase 2  ← Settings engine
Phase 3  ← Lifecycle
Phase 4  ← Cash ledger
Phase 5  ← Sessions & bookings
Phase 6  ← Invitation completion
Phase 7  ← Customer App APIs
```

**Do NOT start Phase 7 before Phase 1–6 are stable.**

---

## 7. Architecture Constraints

- ❌ No online payment
- ❌ No fingerprint devices
- ❌ No marketplace, PT, nutritionist modules
- ❌ No Phase 2/3 platform features
- ✅ Cash only — CASH is the only payment method in all code
- ✅ Gym controls subscription activation — customer requests, gym approves
- ✅ Customer APIs never trust customerId from request — resolve from JWT
- ✅ Dashboard APIs and Customer APIs are permission-separated
- ✅ Preserve all existing working business logic
- ✅ Follow existing package/DTO/service/controller/exception patterns
