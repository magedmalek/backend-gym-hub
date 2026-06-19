# GymHub Backend — Phase 2 Implementation Plan

> **Generated:** 2026-06-03  
> **Branch:** GymHub-DEV  
> **Auditor:** Repository audit before Phase 2 extension begins  

---

## Mandatory Statements

- **Phase 1 backend is the production baseline.** All existing business logic must remain functional.
- Existing **Gym, GymPackage, GymService, Customer, Employee, Subscription, Payment, Freeze, Invitation, FamilyMembership, Attendance, and ExtraService** flows must remain working.
- New Phase 2 logic must be **additive and backward-compatible**.
- Existing **cash payment behavior** must remain valid. `PaymentMethod.CASH` must not be removed or renamed.
- Online payment is **structurally prepared only**, not integrated. No real gateway.
- All new endpoints must use **`/api/v1`**.
- **Internal implementation labels** (Phase 1, Phase 2, Sprint, Backend Phase, etc.) must **not** be exposed in customer-facing or provider-facing API responses.
- **Provider dashboard APIs must be channel-agnostic** and reusable by mobile, web, and desktop clients.
- **Platform configuration foundations** must stay limited to approved business needs only.

---

## 1. Existing Phase 1 Architecture Summary

### Tech Stack
| Component | Version / Detail |
|-----------|-----------------|
| Java | 17 |
| Spring Boot | 3.5.4 |
| Spring Security | JWT stateless |
| Spring Data JPA | ddl-auto: update (NO Flyway/Liquibase) |
| Database | MySQL |
| Lombok | Yes |
| Validation | Jakarta Bean Validation |
| OpenAPI | SpringDoc (Swagger UI at `/swagger-ui.html`) |
| Images | Cloudinary config present |
| Main package | `com.gymhub` |
| API prefix | `/api/v1` |

### Package Layout
```
com.gymhub
├── config/              CloudinaryConfig, SecurityConfig
├── controller/          14 controllers (dashboard + customer app)
├── domain/
│   ├── attendance/      Attendance, AttendanceType
│   ├── customer/        Customer, GymLinkRequest, GymLinkRequestStatus
│   ├── employee/        Employee, EmployeePermission (13 values)
│   ├── extraservice/    ExtraServiceTransaction, ServiceUsage
│   ├── family/          FamilyMembership
│   ├── freeze/          SubscriptionFreeze, FreezeStatus
│   ├── gym/             Gym, GymSettings, GymStatus, ActivationPolicy, EntranceMethod
│   ├── gympackage/      GymPackage
│   ├── gymservice/      GymService, ServiceStatus
│   ├── invitation/      Invitation
│   ├── subscription/    Subscription, Payment, PaymentMethod, SubscriptionStatus, ActivationType
│   └── user/            User, UserRole
├── dto/
│   ├── request/         15 request DTOs
│   └── response/        15 response DTOs
├── exception/           GlobalExceptionHandler, custom exceptions
├── repository/          13 repositories
├── security/            JwtAuthenticationFilter, JwtService, UserDetailsServiceImpl
└── service/             12 services (including GymAccessService)
```

### Security Architecture
- JWT filter loads `User` JPA entity from DB as principal
- `@AuthenticationPrincipal User currentUser` pattern in all controllers
- `GymAccessService` is the central gym-permission service (already implemented)
- Stateless session — no server-side session state

---

## 2. Existing Entities to Preserve

| Entity | Key Contract | Do Not Change |
|--------|-------------|---------------|
| `User` | id, email (unique+required), phone, password, roles (Set\<UserRole\>), activeContext | email uniqueness, UserRole values |
| `Gym` | id, name, owner (User), status | route prefix `/api/v1/gyms` |
| `GymSettings` | 1:1 with Gym, operational policies | existing field names |
| `Employee` | User+Gym junction, permissions (Set\<EmployeePermission\>), hierarchyLevel | permission enum values |
| `Customer` | User+Gym junction, memberCode | gym-specific customer concept |
| `GymPackage` | durationDays, bonusDays, price, includedServices, freezeAllowanceDays, maxInvitations | all fields |
| `GymService` | name, canBeIncludedInPackage, canBeSoldIndependently | all fields |
| `Subscription` | Customer+Package+Gym, paidAmount, status, activationType | status enum values |
| `Payment` | subscription, amount, receivedBy, method (CASH) | PaymentMethod.CASH |
| `SubscriptionFreeze` | startDate, endDate, status, registeredBy | FreezeStatus values |
| `FamilyMembership` | mainCustomer, subCustomer, subscription, relationType | — |
| `Invitation` | host, subscription, guestUser, gym, recordedBy | — |
| `ExtraServiceTransaction` | customer, gym, service, amount, soldBy | — |
| `ServiceUsage` | customer, subscription, service, recordedBy | — |
| `GymLinkRequest` | user, gym, status | GymLinkRequestStatus values |

---

## 3. Existing APIs to Preserve

| Controller | Routes | Status |
|------------|--------|--------|
| `AuthController` | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` | ✅ Keep as-is |
| `GymController` | `/api/v1/gyms` CRUD | ✅ Keep as-is |
| `EmployeeController` | `/api/v1/gyms/{gymId}/employees` CRUD | ✅ Keep as-is |
| `ServiceController` | `/api/v1/gyms/{gymId}/services` CRUD | ✅ Keep as-is |
| `PackageController` | `/api/v1/gyms/{gymId}/packages` CRUD | ✅ Keep as-is |
| `CustomerController` | `/api/v1/gyms/{gymId}/customers` CRUD | ✅ Keep as-is |
| `SubscriptionController` | `/api/v1/gyms/{gymId}/subscriptions` sell/pay/activate/freeze/lifecycle | ✅ Keep as-is |
| `AttendanceController` | `/api/v1/gyms/{gymId}/attendance` | ✅ Keep as-is |
| `InvitationController` | `/api/v1/gyms/{gymId}/invitations` | ✅ Keep as-is |
| `ExtraServiceController` | `/api/v1/gyms/{gymId}/extra-services` | ✅ Keep as-is |
| `FreezeController` | `/api/v1/gyms/{gymId}/subscriptions/{id}/freezes` | ✅ Keep as-is |
| `FamilyMembershipController` | `/api/v1/gyms/{gymId}/subscriptions/{id}/sub-users` | ✅ Keep as-is |
| `CustomerAppController` | `/api/v1/customer/**` | ✅ Keep — extend only |
| `HealthController` | `/api/v1/health` | ✅ Keep as-is |

---

## 4. Existing Services to Reuse

| Service | Reuse Pattern |
|---------|--------------|
| `GymAccessService` | **MANDATORY** for all gym-scoped operations. Methods: `resolveActingEmployee`, `assertPermission`, `assertOwnerOrEmployee`, `assertIsOwner` |
| `AuthService` | Reuse for any auth flows — do not duplicate JWT logic |
| `GymService` (GymManagementService) | Reuse gym resolution. Add `ensureProviderForGym()` bridge |
| `CustomerService` | Reuse customer lookup for gym-specific context |
| `SubscriptionService` | Phase 1 gym subscription logic — do NOT touch for provider subscriptions |
| `FreezeService` | Phase 1 freeze logic — do NOT interfere with `ProviderServiceFreeze` |

---

## 5. Existing Permission Model

```
EmployeePermission (13 values):
  SELL_SUBSCRIPTION, ACTIVATE_SUBSCRIPTION, REGISTER_ATTENDANCE,
  REGISTER_INVITATION, SELL_EXTRA_SERVICE, MANAGE_SERVICES,
  MANAGE_PACKAGES, MANAGE_CUSTOMERS, MANAGE_EMPLOYEES,
  FREEZE_SUBSCRIPTION, MANUAL_ADJUSTMENT, CLOSE_CASH_DAY, ADMIN
```

**New permissions needed for Phase 2** (additive — do NOT remove existing):
```
MANAGE_SPECIALIST_RELATIONSHIPS   // gym side of specialist relationship
MANAGE_TRAINING_PATTERNS          // gym training pattern CRUD
MANAGE_EQUIPMENT                  // gym equipment QR management
```

**Hierarchy:** Owner (level 100) bypasses all permission checks. ADMIN (level 50) gets all permissions.

---

## 6. Existing Customer Model Limitations

- `Customer` is a **gym-specific junction** (User + Gym). A platform user can be a customer of multiple gyms.
- `Customer` has no lifecycle status enum — only `boolean active`.
- Phase 2 must **not** require a `Customer` (gym-specific) record for provider subscriptions when no gym context exists.
- A platform user can subscribe to a specialist independently of any gym.
- The `User` entity is the universal identity; `Customer` is a gym-scoped relationship.

---

## 7. Existing Subscription / Payment / Freeze Logic

### Subscription Flow (Phase 1 — MUST NOT CHANGE)
1. Sell: `POST .../subscriptions/sell` → creates `Subscription` with `PENDING_PAYMENT` or `PENDING_ACTIVATION`
2. Pay: `POST .../subscriptions/{id}/pay` → creates `Payment` (CASH), updates `paidAmount`
3. Activate: `POST .../subscriptions/{id}/activate` → sets status `ACTIVE`, sets `startDate`/`endDate`
4. Freeze: via `FreezeController` → creates `SubscriptionFreeze`, extends `endDate` on unfreeze
5. Cancel/Expire: lifecycle status transitions

### Payment (Phase 1)
- `PaymentMethod.CASH` only
- `Payment` entity linked to `Subscription`
- `receivedBy` is the acting `Employee`

### Phase 2 Provider Subscription
- **Separate entity**: `ProviderServiceSubscription` — does NOT touch `Subscription`
- **Separate freeze**: `ProviderServiceFreeze` — does NOT touch `SubscriptionFreeze`
- **Separate payments**: `ProviderPaymentTransaction` — does NOT touch `Payment`

---

## 8. New Modules to Add

Listed in implementation priority order:

| # | Module | Package | Key Entities |
|---|--------|---------|-------------|
| 1 | Auth Update | `domain/user` (extend) | LoginRequest update, phone login |
| 2 | Provider Foundation | `domain/provider` | Provider, ProviderType, ProviderStatus |
| 3 | Specialist Profile | `domain/specialist` | SpecialistProfile, SpecialistServiceAxisSelection, SpecialistEnabledTool |
| 4 | Gym-Specialist Relationship | `domain/relationship` | GymSpecialistRelationship |
| 5 | Provider Services & Packages | `domain/provider` | ProviderService, ProviderPackage, ProviderPackageSnapshot |
| 6 | Provider Subscription & Payment | `domain/provider` | ProviderServiceSubscription, ProviderPaymentTransaction |
| 7 | Customer Invitation & Onboarding | `domain/provider` | ProviderCustomerInvitation |
| 8 | Privacy & Data Sharing | `domain/privacy` | CustomerDataShare |
| 9 | Specialist Dashboard | (service layer) | DashboardSummary (computed DTO) |
| 10 | Exercise Library | `domain/training` | Exercise, TrainingTemplate |
| 11 | Gym Training Patterns | `domain/training` | GymTrainingPattern, PatternDay, PatternExercise |
| 12 | Training Plans | `domain/training` | TrainingPlan, TrainingPlanDay, TrainingPlanDayExercise |
| 13 | Workout Calendar & Day Status | `domain/training` | TrainingPlanDayStatusHistory |
| 14 | Workout Execution | `domain/training` | WorkoutSession, WorkoutSessionExerciseLog, WorkoutExecutionSet |
| 15 | Nutrition Plans | `domain/nutrition` | NutritionPlan, NutritionPlanDay, NutritionPlanMeal, NutritionPlanMealItem, MealAlternative, Recipe |
| 16 | Meal Logs, Water, Food Prefs, Training Profile | `domain/nutrition`, `domain/training` | MealLog, WaterLog, CustomerFoodPreference, CustomerTrainingProfile |
| 17 | Daily Summary & Timeline | (computed service) | DailyActivitySummary (DTO) |
| 18 | Supplements & Reminders | `domain/health` | SupplementSchedule, CustomerReminder |
| 19 | Appointments & Availability | `domain/appointment` | ProviderAvailability, Appointment |
| 20 | Customer Tool Access | `domain/access` | CustomerToolAccess, PlatformToolConfiguration |
| 21 | Upgrade Pricing | `domain/access` | PlatformFeaturePricing, CustomerToolUpgradeSubscription |
| 22 | Body Measurements & Progress | `domain/progress` | BodyMeasurement |
| 23 | Gym Equipment QR | `domain/gym` (extend) | GymEquipment, EquipmentExerciseLink |
| 24 | Workout Sharing | `domain/training` | WorkoutShare |
| 25 | Ratings & Performance | `domain/rating` | RatingReview |
| 26 | Customer Analytics | `domain/analytics` | (computed DTOs, no new entities) |
| 27 | Requests Center | `domain/request` | ProviderRequest |
| 28 | Complaints, Renewal, Freeze | `domain/request` | ProviderServiceCancellation, ProviderServiceFreeze, Complaint |
| 29 | Audit Log | `domain/audit` | AuditLog |

---

## 9. Entity Extension Plan

### User (extend — additive only)
```java
// No structural change needed. Phone uniqueness enforced at service level
// for new registrations (existing data may have duplicates)
// LoginRequest: add loginIdentifier field (supports email or phone)
```

### PaymentMethod (extend — additive only)
```java
// ADD values, never remove CASH:
CASH,                    // existing — MUST remain
ONLINE_PAYMENT_READY,   // structural only — no gateway
EXTERNAL_PAYMENT,
PAYMENT_AT_GYM,
PAYMENT_TO_SPECIALIST,
WALLET_FUTURE
```

### EmployeePermission (extend — additive only)
```java
// ADD:
MANAGE_SPECIALIST_RELATIONSHIPS,
MANAGE_TRAINING_PATTERNS,
MANAGE_EQUIPMENT
```

### New Enums (net-new)
```
ProviderType:              GYM, SPECIALIST
ProviderStatus:            PENDING, ACTIVE, SUSPENDED, INACTIVE, SOFT_DELETED
SpecialistStatus:          DRAFT, PENDING_REVIEW, ACTIVE, INACTIVE, SUSPENDED
SpecialistSpecialization:  ONLINE_COACH, NUTRITION_SPECIALIST, PERSONAL_TRAINER, REHAB_OR_MOBILITY_SPECIALIST, OTHER
ServiceAxis:               NUTRITION, WORKOUT_PLAN, TRAINING_EXECUTION, MIXED
RelationshipType:          ENTITY_CONTRACT, EMPLOYMENT
RelationshipStatus:        REQUESTED, ACCEPTED, REJECTED, ACTIVE, SUSPENDED, TERMINATED, CANCELLED
RequestInitiatorType:      GYM, SPECIALIST
CommercialOwner:           GYM, SPECIALIST, PLATFORM, CONFIGURED
GymPatternPolicy:          NOT_APPLICABLE, RECOMMENDED_ONLY, MANDATORY_APPLIES, CONTRACT_CONFIGURED
ProviderServiceCategory:   TRAINING, NUTRITION, ONLINE_COACHING, PERSONAL_TRAINING, CONSULTATION, MIXED_PACKAGE, OTHER
ProviderSubscriptionStatus: PENDING, ACTIVE, EXPIRED, CANCELLED, SUSPENDED, READ_ONLY
ProviderPaymentStatus:     PENDING, PAID, FAILED, CANCELLED, WAITING_CONFIRMATION, REFUNDED_FUTURE
ProviderActivationStatus:  NOT_ACTIVE, WAITING_PAYMENT, WAITING_PAYMENT_CONFIRMATION, WAITING_ACTIVATION, ACTIVE, REJECTED, CANCELLED, EXPIRED
ProviderSubscriptionSource: GYM, SPECIALIST, PLATFORM_DIRECT
ProviderCustomerInvitationStatus: PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED, LINKED
DataShareCategory:         WORKOUT_PLANS, WORKOUT_EXECUTION, NUTRITION_PLANS, MEAL_LOGS, WATER_LOGS, SUPPLEMENTS, MEASUREMENTS, PROGRESS_PHOTOS, REPORTS, NOTES, APPOINTMENTS, FOOD_PREFERENCES, TRAINING_PROFILE, DAILY_SUMMARY, TIMELINE
DataShareStatus:           REQUESTED, ACTIVE, PARTIALLY_APPROVED, REJECTED, REVOKED, EXPIRED
MuscleGroup:               CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, ABS, CORE, GLUTES, QUADS, HAMSTRINGS, CALVES, FULL_BODY, CARDIO, MOBILITY, STRETCHING, OTHER
ExerciseType:              STRENGTH, CARDIO, WARM_UP, STRETCHING, MOBILITY, FUNCTIONAL, BODYWEIGHT, MACHINE, FREE_WEIGHT, OTHER
ExerciseApprovalStatus:    APPROVED, PENDING_APPROVAL, REJECTED, ARCHIVED
TrainingTemplateType:      PPL, ARNOLD_SPLIT, UPPER_LOWER, FULL_BODY, CUSTOM
GymTrainingPatternControlType: MANDATORY, RECOMMENDED
TrainingPlanSource:        CUSTOMER, SPECIALIST, GYM, PLATFORM
TrainingPlanStatus:        DRAFT, ACTIVE, COMPLETED, CANCELLED, ARCHIVED, READ_ONLY
ExerciseTimingType:        WARM_UP, MAIN, COOL_DOWN, STRETCHING
ReschedulePolicy:          SHIFT_REMAINING_PLAN, KEEP_REMAINING_PLAN_AS_IS, MANUAL_RESCHEDULE_ONLY
MissedDayPolicy:           MARK_SKIPPED_AND_CONTINUE, SHIFT_REMAINING_PLAN, ASK_CUSTOMER_OR_SPECIALIST, MANUAL_DECISION_REQUIRED
TrainingPlanDayStatus:     PLANNED, NOT_STARTED, IN_PROGRESS, COMPLETED, SKIPPED, POSTPONED, CANCELLED
WorkoutSessionStatus:      NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED
ExecutionSetType:          NORMAL, DROP_SET, SUPERSET, TIMED, CARDIO, BODYWEIGHT
ExecutionEntrySource:      CUSTOMER, SPECIALIST_PT, GYM_EMPLOYEE
MealLogStatus:             COMMITTED, REPLACED, NOT_COMMITTED, SKIPPED, NEEDS_ALTERNATIVE, NOT_LOGGED
WaterLogStatus:            ON_TARGET, BELOW_TARGET, ABOVE_TARGET, NOT_LOGGED
SupplementTimingType:      FIXED_TIME, AFTER_MEAL, BEFORE_MEAL, BEFORE_SLEEP, AFTER_WORKOUT, BEFORE_WORKOUT, CUSTOM_EVENT
ReminderType:              MEAL, SUPPLEMENT, SLEEP, WORKOUT, WATER, MEASUREMENT, PLAN_CHANGE, CUSTOM
ReminderStatus:            PENDING, SENT, CANCELLED, DISABLED, FAILED, DONE, SNOOZED
AppointmentType:           PT_SESSION, TRAINING_EXECUTION, NUTRITION_FOLLOW_UP, CONSULTATION, PLAN_REVIEW, MEASUREMENT, PLAN_CHANGE
AppointmentStatus:         PENDING_APPROVAL, SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW, RESCHEDULED
ToolAccessState:           OPEN, LOCKED, TRIAL, LIMITED, READ_ONLY, HIDDEN, UPGRADE_AVAILABLE
ToolAccessSource:          FREE_BASIC, FREE_TRIAL, PLATFORM_DIRECT_SUBSCRIPTION, SERVICE_BASED_ACCESS, TOOL_UPGRADE_ACCESS
EquipmentStatus:           ACTIVE, OUT_OF_SERVICE, MAINTENANCE, REMOVED
EquipmentCategory:         CARDIO, STRENGTH_MACHINE, FREE_WEIGHT, CABLE_MACHINE, BODYWEIGHT, FUNCTIONAL, OTHER
WorkoutShareType:          IMAGE_SUMMARY, PUBLIC_WEB_LINK, INTERNAL_SPECIALIST_SHARE
WorkoutShareVisibility:    PRIVATE, PUBLIC_LIMITED, SPECIALIST_ONLY
RatingStatus:              PENDING, PUBLISHED, HIDDEN, REPORTED
RequestType:               SUBSCRIPTION_REQUEST, RENEWAL_REQUEST, CANCELLATION_REQUEST, UPGRADE_REQUEST, MEAL_ALTERNATIVE_REQUEST, PLAN_MODIFICATION_REQUEST, DATA_SHARING_REQUEST, GYM_LINK_REQUEST, CUSTOMER_INVITATION, APPOINTMENT_RESCHEDULE, COMPLAINT
RequestStatus:             PENDING, APPROVED, REJECTED, CANCELLED, EXPIRED, COMPLETED
ComplaintType:             SPECIALIST, SESSION, SERVICE, GYM, PAYMENT, NO_SHOW, PRIVACY, FOLLOW_UP, DATA_PROBLEM, OTHER
ComplaintStatus:           NEW, UNDER_REVIEW, NEEDS_RESPONSE, RESOLVED, REJECTED, CLOSED
ProviderServiceFreezeStatus: PENDING, APPROVED, REJECTED, ACTIVE, COMPLETED, CANCELLED
ProviderServiceCancellationStatus: PENDING, APPROVED, REJECTED, COMPLETED
```

---

## 10. Migration Risk

| Risk | Severity | Mitigation |
|------|---------|------------|
| `PaymentMethod` enum extension | LOW | Additive only. `CASH` remains first value. JPA `EnumType.STRING` — safe. |
| `EmployeePermission` extension | LOW | Additive only. Existing records unaffected. |
| Phone uniqueness constraint | MEDIUM | Do NOT add DB-level unique constraint to `User.phone` — enforce at service level for new registrations only. Existing data may have nulls/duplicates. |
| `ddl-auto: update` schema drift | MEDIUM | New entities auto-create tables. Existing tables only get new nullable columns added. Never remove columns. |
| Provider ↔ Gym bridge | LOW | `ensureProviderForGym()` is lazy/additive — creates Provider record only when needed. Does not affect Gym records. |
| `ProviderServiceSubscription` vs `Subscription` | LOW | Completely separate entity. No FK relationship between them. |
| `ProviderServiceFreeze` vs `SubscriptionFreeze` | LOW | Completely separate entity. Existing `FreezeController` unaffected. |
| Training plan activation archives old plan | LOW | Status change is additive to `TrainingPlanStatus`. No destructive data change. |

---

## 11. Security Impact

### New Public Endpoints (must add to SecurityConfig)
```java
"/api/v1/public/provider-invitations/**"    // invitation token lookup
"/api/v1/public/workout-shares/**"          // public workout share
"/api/v1/equipment/qr/**"                   // QR scan (authenticated in practice, but may be public)
"/api/v1/exercises"                         // exercise library browse (read-only public)
"/api/v1/exercises/**"
"/api/v1/training-templates"
"/api/v1/providers/*/reviews"              // public provider reviews
"/api/v1/providers/*/rating-summary"       // public rating summary
```

### New Permission Checks
- All gym-scoped Phase 2 endpoints use `GymAccessService` (existing pattern)
- Specialist operations resolve from `@AuthenticationPrincipal User` → `Provider` → `SpecialistProfile`
- Customer operations resolve from JWT only — never from request params
- Data sharing enforced at service layer before returning any private data

### Sensitive Data Protection
- Progress photos require consent check
- Measurements require consent check outside active service scope
- Meal/water/supplement logs: specialist sees only within active service or with valid share
- Workout execution: PT cannot modify customer-created entries

---

## 12. API Impact

### New Controller Files (net-new)
```
controller/ProviderController.java
controller/SpecialistController.java
controller/GymSpecialistRelationshipController.java
controller/ProviderPackageController.java
controller/ProviderSubscriptionController.java
controller/ProviderInvitationController.java
controller/PrivacyController.java
controller/ExerciseController.java
controller/TrainingTemplateController.java
controller/GymTrainingPatternController.java
controller/TrainingPlanController.java
controller/WorkoutController.java
controller/NutritionPlanController.java
controller/MealLogController.java
controller/WaterLogController.java
controller/FoodPreferenceController.java
controller/TrainingProfileController.java
controller/DailySummaryController.java
controller/SupplementController.java
controller/AppointmentController.java
controller/CustomerToolAccessController.java
controller/ToolUpgradeController.java
controller/BodyMeasurementController.java
controller/GymEquipmentController.java
controller/WorkoutShareController.java
controller/RatingController.java
controller/AnalyticsController.java
controller/RequestCenterController.java
controller/ComplaintController.java
```

### Complete New Endpoint List (by section)

#### Auth (Section 2)
```
POST /api/v1/auth/register            [existing — update LoginRequest only]
POST /api/v1/auth/login               [existing — add phone login support]
```

#### Provider (Section 3)
```
GET  /api/v1/providers/me
GET  /api/v1/providers/{providerId}
GET  /api/v1/providers
PUT  /api/v1/providers/me
```

#### Specialist Profile (Section 4)
```
POST /api/v1/specialists/me/profile
GET  /api/v1/specialists/me/profile
PUT  /api/v1/specialists/me/profile
GET  /api/v1/specialists/{specialistId}/profile
PUT  /api/v1/specialists/me/service-axes
GET  /api/v1/specialists/me/service-axes
PUT  /api/v1/specialists/me/enabled-tools
GET  /api/v1/specialists/me/enabled-tools
```

#### Gym-Specialist Relationships (Section 5)
```
POST /api/v1/gyms/{gymId}/specialist-relationships/requests
POST /api/v1/specialists/me/gym-relationships/requests
GET  /api/v1/gyms/{gymId}/specialist-relationships
GET  /api/v1/specialists/me/gym-relationships
POST /api/v1/specialist-relationships/{id}/accept
POST /api/v1/specialist-relationships/{id}/reject
POST /api/v1/specialist-relationships/{id}/suspend
POST /api/v1/specialist-relationships/{id}/terminate
PUT  /api/v1/specialist-relationships/{id}/policy
```

#### Provider Services & Packages (Section 6)
```
POST   /api/v1/providers/me/services
GET    /api/v1/providers/me/services
PUT    /api/v1/providers/me/services/{serviceId}
DELETE /api/v1/providers/me/services/{serviceId}
POST   /api/v1/providers/me/packages
GET    /api/v1/providers/me/packages
GET    /api/v1/providers/{providerId}/packages
GET    /api/v1/provider-packages/{packageId}
PUT    /api/v1/providers/me/packages/{packageId}
DELETE /api/v1/providers/me/packages/{packageId}
```

#### Provider Subscriptions & Payment (Section 7)
```
POST /api/v1/customer/provider-packages/{packageId}/subscribe
GET  /api/v1/customer/provider-subscriptions
GET  /api/v1/customer/provider-subscriptions/{id}
GET  /api/v1/customer/provider-subscriptions/{id}/payment-history
GET  /api/v1/providers/me/subscriptions
GET  /api/v1/providers/me/payments/waiting-confirmation
POST /api/v1/providers/me/subscriptions/{id}/confirm-payment
POST /api/v1/providers/me/subscriptions/{id}/activate
```

#### Customer Invitations (Section 8)
```
POST /api/v1/providers/me/customer-invitations
GET  /api/v1/providers/me/customer-invitations
POST /api/v1/providers/me/customer-invitations/{id}/cancel
GET  /api/v1/customer/provider-invitations
POST /api/v1/customer/provider-invitations/{id}/accept
POST /api/v1/customer/provider-invitations/{id}/reject
GET  /api/v1/public/provider-invitations/{token}
```

#### Privacy & Data Sharing (Section 9)
```
GET  /api/v1/customer/privacy/data-shares
GET  /api/v1/customer/privacy/data-share-requests
POST /api/v1/customer/privacy/data-share-requests/{id}/approve
POST /api/v1/customer/privacy/data-share-requests/{id}/reject
PUT  /api/v1/customer/privacy/data-shares/{id}/categories
POST /api/v1/customer/privacy/data-shares/{id}/revoke
POST /api/v1/customer/privacy/share-with-provider
POST /api/v1/specialists/me/customers/{customerUserId}/data-share-requests
GET  /api/v1/specialists/me/customers/{customerUserId}/data-sharing-status
```

#### Specialist Dashboard (Section 10)
```
GET /api/v1/specialists/me/dashboard-summary
GET /api/v1/specialists/me/customers
GET /api/v1/specialists/me/customers/{customerUserId}
GET /api/v1/specialists/me/customers/{customerUserId}/timeline
GET /api/v1/specialists/me/today
GET /api/v1/specialists/me/follow-ups
GET /api/v1/specialists/me/pending-actions
GET /api/v1/specialists/me/context-options
```

#### Exercise Library (Section 11)
```
GET  /api/v1/exercises
GET  /api/v1/exercises/{id}
GET  /api/v1/exercises/by-muscle/{muscleGroup}
GET  /api/v1/exercises/search
POST /api/v1/exercises/proposals
GET  /api/v1/training-templates
GET  /api/v1/training-templates/{id}
POST /api/v1/providers/me/training-templates
PUT  /api/v1/providers/me/training-templates/{id}
DELETE /api/v1/providers/me/training-templates/{id}
```

#### Gym Training Patterns (Section 12)
```
POST   /api/v1/gyms/{gymId}/training-patterns
GET    /api/v1/gyms/{gymId}/training-patterns
PUT    /api/v1/gyms/{gymId}/training-patterns/{patternId}
DELETE /api/v1/gyms/{gymId}/training-patterns/{patternId}
GET    /api/v1/specialists/me/available-gym-patterns
```

#### Training Plans (Section 13)
```
POST /api/v1/specialists/me/customers/{customerUserId}/training-plans
GET  /api/v1/specialists/me/customers/{customerUserId}/training-plans
GET  /api/v1/customer/training-plans
GET  /api/v1/customer/training-plans/active
POST /api/v1/customer/training-plans
GET  /api/v1/training-plans/{planId}
PUT  /api/v1/training-plans/{planId}
POST /api/v1/training-plans/{planId}/activate
POST /api/v1/training-plans/{planId}/archive
```

#### Workout Calendar & Day Status (Section 14)
```
POST /api/v1/customer/training-days/{dayId}/postpone
POST /api/v1/customer/training-days/{dayId}/skip
POST /api/v1/specialists/me/training-days/{dayId}/reschedule
POST /api/v1/training-plans/{planId}/recalculate-schedule
GET  /api/v1/customer/calendar/month
GET  /api/v1/customer/calendar/week
GET  /api/v1/customer/calendar/day
```

#### Workout Execution (Section 15)
```
POST /api/v1/customer/workouts/days/{planDayId}/start
POST /api/v1/customer/workouts/sessions/{sessionId}/complete
GET  /api/v1/customer/workouts/sessions/{sessionId}
POST /api/v1/customer/workouts/exercise-logs/{exerciseLogId}/sets
POST /api/v1/customer/workouts/exercise-logs/{exerciseLogId}/sets/use-suggested
POST /api/v1/customer/workouts/exercise-logs/{exerciseLogId}/extra-set
PUT  /api/v1/customer/workouts/sessions/{sessionId}/edit-completed
POST /api/v1/specialists/me/workout-sessions/{sessionId}/exercise-logs/{exerciseLogId}/sets
GET  /api/v1/specialists/me/workout-sessions/{sessionId}/pt-contribution
GET  /api/v1/customer/progress/exercises/{exerciseId}
GET  /api/v1/customer/progress/exercise-weight-history
GET  /api/v1/specialists/me/customers/{customerUserId}/progress/exercise-weight-history
```

#### Nutrition Plans (Section 16)
```
POST /api/v1/specialists/me/customers/{customerUserId}/nutrition-plans
GET  /api/v1/specialists/me/customers/{customerUserId}/nutrition-plans
GET  /api/v1/customer/nutrition-plans
GET  /api/v1/customer/nutrition-plans/active
POST /api/v1/customer/nutrition-plans
POST /api/v1/customer/nutrition/meals/{mealId}/request-alternative
GET  /api/v1/nutrition-plans/{planId}
PUT  /api/v1/nutrition-plans/{planId}
POST /api/v1/nutrition-plans/{planId}/activate
POST /api/v1/nutrition-plans/{planId}/archive
```

#### Meal Logs, Water, Food Prefs, Training Profile (Section 17)
```
POST /api/v1/customer/nutrition/meals/{mealId}/log
GET  /api/v1/customer/nutrition/meal-logs
GET  /api/v1/specialists/me/customers/{customerUserId}/nutrition/meal-logs
POST /api/v1/customer/water-logs
GET  /api/v1/customer/water-logs
GET  /api/v1/customer/water-logs/today
GET  /api/v1/specialists/me/customers/{customerUserId}/water-logs
GET  /api/v1/customer/food-preferences
PUT  /api/v1/customer/food-preferences
GET  /api/v1/specialists/me/customers/{customerUserId}/food-preferences
GET  /api/v1/customer/training-profile
PUT  /api/v1/customer/training-profile
GET  /api/v1/specialists/me/customers/{customerUserId}/training-profile
GET  /api/v1/customer/daily-summary
GET  /api/v1/customer/today-summary
GET  /api/v1/specialists/me/customers/{customerUserId}/daily-summary
GET  /api/v1/specialists/me/customers/{customerUserId}/timeline
```

#### Supplements & Reminders (Section 18)
```
POST /api/v1/specialists/me/customers/{customerUserId}/supplement-schedules
GET  /api/v1/specialists/me/customers/{customerUserId}/supplement-schedules
GET  /api/v1/customer/supplement-schedules
PUT  /api/v1/customer/supplement-schedules/{id}/enable
PUT  /api/v1/customer/supplement-schedules/{id}/disable
GET  /api/v1/customer/reminders
POST /api/v1/customer/reminders/{id}/done
POST /api/v1/customer/reminders/{id}/snooze
PUT  /api/v1/customer/notification-preferences
```

#### Appointments (Section 19)
```
POST /api/v1/providers/me/availability
GET  /api/v1/providers/me/availability
PUT  /api/v1/providers/me/availability/{id}
GET  /api/v1/providers/me/appointments
POST /api/v1/providers/me/appointments/{id}/accept
POST /api/v1/providers/me/appointments/{id}/reject
POST /api/v1/providers/me/appointments/{id}/reschedule
POST /api/v1/providers/me/appointments/{id}/complete
POST /api/v1/providers/me/appointments/{id}/no-show
GET  /api/v1/customer/appointments
POST /api/v1/customer/providers/{providerId}/appointments/request
POST /api/v1/customer/appointments/{id}/cancel
POST /api/v1/customer/appointments/{id}/reschedule-request
GET  /api/v1/customer/providers/{providerId}/available-slots
```

#### Customer Tool Access (Section 20)
```
GET /api/v1/customer/my-services
GET /api/v1/customer/my-services/{subscriptionId}
GET /api/v1/customer/tool-access
GET /api/v1/customer/my-plan-progress
GET /api/v1/customer/read-only-history
```

#### Tool Upgrades (Section 21)
```
POST /api/v1/customer/tool-upgrades/price-preview
POST /api/v1/customer/tool-upgrades/subscribe
GET  /api/v1/customer/tool-upgrades
```

#### Body Measurements (Section 22)
```
POST   /api/v1/customer/measurements
GET    /api/v1/customer/measurements
GET    /api/v1/customer/measurements/latest
PUT    /api/v1/customer/measurements/{id}
DELETE /api/v1/customer/measurements/{id}
GET    /api/v1/specialists/me/customers/{customerUserId}/measurements
POST   /api/v1/specialists/me/customers/{customerUserId}/measurements
GET    /api/v1/customer/progress/summary
GET    /api/v1/customer/progress/weight-chart
GET    /api/v1/customer/progress/waist-chart
GET    /api/v1/customer/progress/exercise-weight-chart
```

#### Gym Equipment QR (Section 23)
```
POST   /api/v1/gyms/{gymId}/equipment
GET    /api/v1/gyms/{gymId}/equipment
GET    /api/v1/gyms/{gymId}/equipment/{equipmentId}
PUT    /api/v1/gyms/{gymId}/equipment/{equipmentId}
DELETE /api/v1/gyms/{gymId}/equipment/{equipmentId}
PUT    /api/v1/gyms/{gymId}/equipment/{equipmentId}/status
POST   /api/v1/gyms/{gymId}/equipment/{equipmentId}/exercises
DELETE /api/v1/gyms/{gymId}/equipment/{equipmentId}/exercises/{exerciseId}
GET    /api/v1/gyms/{gymId}/equipment/{equipmentId}/qr-print-data
GET    /api/v1/equipment/qr/{qrCodeValue}
```

#### Workout Sharing (Section 24)
```
POST /api/v1/customer/workouts/sessions/{sessionId}/share/image-summary
POST /api/v1/customer/workouts/sessions/{sessionId}/share/web-link
POST /api/v1/customer/workouts/sessions/{sessionId}/share/specialist
POST /api/v1/customer/workout-shares/{shareId}/revoke
GET  /api/v1/public/workout-shares/{token}
GET  /api/v1/specialists/me/shared-workouts
```

#### Ratings (Section 25)
```
POST /api/v1/customer/reviews
GET  /api/v1/providers/{providerId}/reviews
GET  /api/v1/providers/{providerId}/rating-summary
GET  /api/v1/provider-services/{serviceId}/rating-summary
GET  /api/v1/specialists/me/performance-summary
```

#### Analytics (Section 26)
```
GET /api/v1/customer/analytics/calories
GET /api/v1/customer/analytics/training-calendar
GET /api/v1/customer/analytics/muscle-groups
GET /api/v1/customer/analytics/exercise-types
GET /api/v1/customer/analytics/measurements
GET /api/v1/customer/analytics/adherence
GET /api/v1/customer/analytics/day-summary
```

#### Requests Center (Section 27)
```
GET  /api/v1/providers/me/requests
GET  /api/v1/customer/requests
POST /api/v1/providers/me/requests/{requestId}/approve
POST /api/v1/providers/me/requests/{requestId}/reject
POST /api/v1/customer/requests/{requestId}/cancel
```

#### Complaints, Renewal, Freeze (Section 28)
```
GET  /api/v1/customer/provider-subscriptions/{id}/renewal-options
POST /api/v1/customer/provider-subscriptions/{id}/renew
GET  /api/v1/customer/provider-subscriptions/{id}/cancellation-policy
POST /api/v1/customer/provider-subscriptions/{id}/cancel-request
GET  /api/v1/customer/provider-subscriptions/{id}/freeze-eligibility
POST /api/v1/customer/provider-subscriptions/{id}/freeze-request
POST /api/v1/providers/me/provider-service-freezes/{freezeId}/approve
POST /api/v1/providers/me/provider-service-freezes/{freezeId}/reject
POST /api/v1/customer/complaints
GET  /api/v1/customer/complaints
GET  /api/v1/providers/me/complaints
```

---

## 13. Tests to Add

### Critical Business Rule Tests (minimum required)

| # | Test Class | Scenario |
|---|-----------|----------|
| 1 | `ApplicationContextTest` | Context loads with all Phase 2 beans |
| 2 | `Phase1CompatibilityTest` | Existing subscription sell/pay/activate still works |
| 3 | `Phase1CashPaymentTest` | Cash payment flow unchanged |
| 4 | `Phase1GymPackageTest` | GymPackage/GymService APIs still respond |
| 5 | `AuthLoginTest` | Email login still works |
| 6 | `AuthLoginTest` | Phone login works when phone exists |
| 7 | `AuthLoginTest` | Duplicate email blocked on register |
| 8 | `AuthLoginTest` | Duplicate phone blocked for new users when phone provided |
| 9 | `ProviderFoundationTest` | Provider created without breaking Gym entity |
| 10 | `SpecialistProfileTest` | Specialist profile created under SERVICE_PROVIDER role |
| 11 | `ServiceAxisToolsTest` | NUTRITION axis auto-enables nutrition tools |
| 12 | `ServiceAxisToolsTest` | WORKOUT_PLAN axis auto-enables workout tools |
| 13 | `GymSpecialistRelationshipTest` | Gym can request relationship |
| 14 | `GymSpecialistRelationshipTest` | Specialist can request relationship |
| 15 | `GymSpecialistRelationshipTest` | Accept/reject by correct party |
| 16 | `EmploymentRelationshipTest` | Employment applies gym ownership rules |
| 17 | `EntityContractTest` | Entity contract preserves both-party independence |
| 18 | `ProviderPackageSnapshotTest` | Snapshot created at subscription time |
| 19 | `ProviderPaymentTest` | Cash confirmation requires payment owner |
| 20 | `ProviderPaymentTest` | Customer cannot self-confirm cash payment |
| 21 | `CustomerInvitationTest` | Invitation does not duplicate existing platform user |
| 22 | `CustomerInvitationTest` | Existing platform customer must explicitly approve link |
| 23 | `DataSharingTest` | Approve/reject/revoke works correctly |
| 24 | `SpecialistAccessTest` | Specialist cannot access unrelated customer data |
| 25 | `CustomerAccessTest` | Customer cannot access another customer's data |
| 26 | `SuggestedValuesTest` | Fills next set only (not all sets) |
| 27 | `WorkoutExecutionTest` | Extra set is marked as extra |
| 28 | `WorkoutExecutionTest` | PT cannot log after session completed |
| 29 | `WorkoutExecutionTest` | PT cannot edit customer-created set |
| 30 | `WorkoutExecutionTest` | Completed session edit requires reason |
| 31 | `TrainingDayTest` | Postpone/skip/reschedule works per policy |
| 32 | `CalendarTest` | Month view returns icon-level data only |
| 33 | `CalendarTest` | Week view returns short titles |
| 34 | `CalendarTest` | Day view returns full details |
| 35 | `MealLogTest` | All MealLogStatus values accepted |
| 36 | `WaterLogTest` | Daily summary returns correct ON_TARGET/BELOW/ABOVE |
| 37 | `FoodPreferenceTest` | Specialist sees only within active nutrition service |
| 38 | `TrainingProfileTest` | Protected from unrelated specialist access |
| 39 | `SupplementReminderTest` | Meal-linked reminder uses actual logged time, fallback to planned |
| 40 | `ToolUpgradeTest` | 20-day provider + 10-day platform direct pricing works correctly |
| 41 | `MeasurementTest` | Measurements protected from unrelated providers |
| 42 | `EquipmentQrTest` | QR scan returns safe data only |
| 43 | `WorkoutShareTest` | Public share exposes only safe fields (no phone, email, notes) |
| 44 | `RatingTest` | Provider/service/session ratings stored separately |
| 45 | `RatingTest` | Last 6 months rating filter works |
| 46 | `DailySummaryTest` | Summary includes nutrition/workout/water/supplements/appointments |
| 47 | `RequestCenterTest` | Approve/reject actions are audited |
| 48 | `ProviderFreezeTest` | Provider service freeze does not affect Phase 1 SubscriptionFreeze |
| 49 | `InternalLabelTest` | No internal phase/sprint labels in any API response field |
| 50 | `ChannelAgnosticTest` | Provider dashboard APIs return structured data usable by mobile/web/desktop |

---

## 14. Assumptions

1. `ddl-auto: update` continues — no Flyway/Liquibase introduced.
2. `UserRole.SPECIALIST` is NOT added. Specialists use `SERVICE_PROVIDER` role + `Provider.providerType = SPECIALIST`.
3. No real payment gateway is integrated. Online payment path is structural only.
4. No actual push notification provider is integrated. `CustomerReminder` records are persisted; sending is deferred.
5. Exercise library starts with foundation entities only. No full exercise seed required at this stage.
6. Cloudinary is the image upload mechanism (already configured).
7. `bigDecimal` is used for all monetary values; `LocalDate` for dates; `LocalDateTime` for timestamps.
8. All new enums use `@Enumerated(EnumType.STRING)`.
9. All list endpoints use pagination (`PagedResponse<T>`).
10. `AuditLog` is a new entity — no existing audit mechanism was found.
11. The `ToolCode` concept is implemented as a `String` constant class or enum (not a DB entity).
12. "Platform direct" pricing is backend-only configuration — no admin UI.
13. Branch support within Gym is deferred (exists as nullable FK on some entities).

---

## 15. Blockers

| Blocker | Impact | Resolution |
|---------|--------|------------|
| Phone uniqueness in existing data | Cannot safely add DB UNIQUE on User.phone | Enforce at service layer for new registrations only |
| No existing `AuditLog` entity | Audit trail must be built from scratch | Implement in Section 29 |
| No notification delivery mechanism | CustomerReminder can be persisted but not sent | Implement record creation; mark delivery as deferred |
| No payment gateway | ProviderSubscription online path is structural only | `ONLINE_PAYMENT_READY` enum value exists; no gateway code |
| Exercise seed data | Full exercise library is large | Implement foundation; optionally seed 5–10 sample exercises |
| `CustomerAppController` already exists | Must extend, not replace | Audit existing endpoints before adding new ones |

---

## 16. Backward Compatibility Rules

1. **Never remove** any existing entity, field, enum value, controller, or service method.
2. **Never rename** existing database column names — JPA derives them from field names.
3. **Never change** existing API route paths or HTTP methods.
4. **Never change** existing request/response DTO field names used by Phase 1 frontend.
5. `PaymentMethod.CASH` must remain the first and default value.
6. `SubscriptionStatus` existing values (PENDING_PAYMENT, PENDING_ACTIVATION, ACTIVE, EXPIRED, CANCELLED) must remain.
7. `UserRole` existing values (SERVICE_PROVIDER, EMPLOYEE, CUSTOMER) must remain.
8. `FreezeStatus` existing values must remain.
9. `GymAccessService` method signatures must not change — extend via new overloads if needed.
10. Phase 2 subscription/payment/freeze entities are fully separate — no shared FK with Phase 1 entities.

---

## 17. Execution Checklist

### Section 1 — Audit & Plan ✅ (this document)

### Section 2 — Auth Update
- [ ] Update `LoginRequest`: add `loginIdentifier` field; accept email or phone
- [ ] Update `AuthService.login()`: detect `@` → email path; else → phone path
- [ ] Update `UserRepository`: add `findByPhone()`
- [ ] Service-level phone duplicate check in `AuthService.register()`
- [ ] Test: email login, phone login, duplicate email blocked, duplicate phone blocked

### Section 3 — Provider Foundation
- [ ] Create `domain/provider/Provider.java`
- [ ] Create `domain/provider/ProviderType.java`
- [ ] Create `domain/provider/ProviderStatus.java`
- [ ] Create `repository/ProviderRepository.java`
- [ ] Create `service/ProviderService.java` (includes `ensureProviderForGym()`)
- [ ] Create `controller/ProviderController.java`
- [ ] Create request/response DTOs
- [ ] Add permissions to `EmployeePermission` enum (3 new values)
- [ ] Test: Provider CRUD, Gym bridge

### Section 4 — Specialist Profile & Service Axes
- [ ] Create `domain/specialist/SpecialistProfile.java`
- [ ] Create `domain/specialist/SpecialistServiceAxisSelection.java`
- [ ] Create `domain/specialist/SpecialistEnabledTool.java`
- [ ] Create enums: `SpecialistStatus`, `SpecialistSpecialization`, `ServiceAxis`
- [ ] Create `ToolCode` constants
- [ ] Implement auto-enable logic for tools based on selected axes
- [ ] Create `service/SpecialistService.java`
- [ ] Create `controller/SpecialistController.java`
- [ ] Test: axis selection → tool auto-enable, disable with warning

### Section 5 — Gym-Specialist Relationships
- [ ] Create `domain/relationship/GymSpecialistRelationship.java`
- [ ] Create enums: `RelationshipType`, `RelationshipStatus`, `RequestInitiatorType`, `CommercialOwner`, `GymPatternPolicy`
- [ ] Create `service/GymSpecialistRelationshipService.java`
- [ ] Create `controller/GymSpecialistRelationshipController.java`
- [ ] Enforce `GymAccessService` for gym-side operations
- [ ] Test: request/accept/reject, employment ownership rules, entity contract independence

### Section 6 — Provider Services & Packages
- [ ] Create `domain/provider/ProviderService.java`
- [ ] Create `domain/provider/ProviderPackage.java`
- [ ] Create `domain/provider/ProviderPackageSnapshot.java`
- [ ] Create enums: `ProviderServiceCategory`
- [ ] Create `service/ProviderPackageService.java`
- [ ] Create `controller/ProviderPackageController.java`

### Section 7 — Provider Subscriptions & Payment
- [ ] Create `domain/provider/ProviderServiceSubscription.java`
- [ ] Create `domain/provider/ProviderPaymentTransaction.java`
- [ ] Create enums: `ProviderSubscriptionStatus`, `ProviderPaymentStatus`, `ProviderActivationStatus`, `ProviderSubscriptionSource`
- [ ] Add new `PaymentMethod` values
- [ ] Create `service/ProviderSubscriptionService.java`
- [ ] Create `controller/ProviderSubscriptionController.java`

### Section 8 — Customer Invitations
- [ ] Create `domain/provider/ProviderCustomerInvitation.java`
- [ ] Create `service/ProviderInvitationService.java`
- [ ] Create `controller/ProviderInvitationController.java`
- [ ] Add public endpoint to `SecurityConfig`
- [ ] Test: no duplicate user, existing user must approve

### Section 9 — Privacy & Data Sharing
- [ ] Create `domain/privacy/CustomerDataShare.java`
- [ ] Create enums: `DataShareStatus`, `DataShareCategory`
- [ ] Create `service/DataSharingService.java`
- [ ] Create `controller/PrivacyController.java`
- [ ] Integrate sharing checks into specialist customer-data APIs
- [ ] Test: approve/reject/revoke, specialist blocked from unrelated customer

### Section 10 — Specialist Dashboard
- [ ] Create `service/SpecialistDashboardService.java`
- [ ] Implement `DashboardSummaryResponse` DTO
- [ ] Implement `CustomerCardResponse` with all fields
- [ ] Add filter/sort/pagination
- [ ] Create `controller/SpecialistDashboardController.java`

### Section 11 — Exercise Library
- [ ] Create `domain/training/Exercise.java`
- [ ] Create `domain/training/TrainingTemplate.java`
- [ ] Create enums: `MuscleGroup`, `ExerciseType`, `ExerciseApprovalStatus`, `TrainingTemplateType`
- [ ] Create `service/ExerciseService.java`
- [ ] Create `controller/ExerciseController.java`
- [ ] Add public exercise browse to `SecurityConfig`

### Section 12 — Gym Training Patterns
- [ ] Create `domain/training/GymTrainingPattern.java`
- [ ] Create `domain/training/GymTrainingPatternDay.java`
- [ ] Create `domain/training/GymTrainingPatternExercise.java`
- [ ] Create `service/GymTrainingPatternService.java`
- [ ] Create `controller/GymTrainingPatternController.java`
- [ ] Use `GymAccessService` + `MANAGE_TRAINING_PATTERNS`

### Section 13 — Training Plans
- [ ] Create `domain/training/TrainingPlan.java`
- [ ] Create `domain/training/TrainingPlanDay.java`
- [ ] Create `domain/training/TrainingPlanDayExercise.java`
- [ ] Create enums: `TrainingPlanSource`, `TrainingPlanStatus`, `ExerciseTimingType`, `ReschedulePolicy`, `MissedDayPolicy`
- [ ] Create `service/TrainingPlanService.java`
- [ ] Create `controller/TrainingPlanController.java`
- [ ] Tool access check for customer self-plan creation

### Section 14 — Workout Calendar & Day Status
- [ ] Create `domain/training/TrainingPlanDayStatusHistory.java`
- [ ] Create enum: `TrainingPlanDayStatus`
- [ ] Implement postpone/skip/reschedule logic in `TrainingPlanService`
- [ ] Create `controller/WorkoutCalendarController.java`
- [ ] Implement month/week/day calendar response shapes

### Section 15 — Workout Execution
- [ ] Create `domain/training/WorkoutSession.java`
- [ ] Create `domain/training/WorkoutSessionExerciseLog.java`
- [ ] Create `domain/training/WorkoutExecutionSet.java`
- [ ] Create enums: `WorkoutSessionStatus`, `ExecutionSetType`, `ExecutionEntrySource`
- [ ] Implement suggested values logic
- [ ] Implement extra-set logic with warning flag
- [ ] Create `service/WorkoutExecutionService.java`
- [ ] Create `controller/WorkoutController.java`
- [ ] PT contribution calculation

### Section 16 — Nutrition Plans
- [ ] Create `domain/nutrition/NutritionPlan.java`
- [ ] Create `domain/nutrition/NutritionPlanDay.java`
- [ ] Create `domain/nutrition/NutritionPlanMeal.java`
- [ ] Create `domain/nutrition/NutritionPlanMealItem.java`
- [ ] Create `domain/nutrition/MealAlternative.java`
- [ ] Create `domain/nutrition/Recipe.java`
- [ ] Create `service/NutritionPlanService.java`
- [ ] Create `controller/NutritionPlanController.java`

### Section 17 — Meal Logs, Water, Food Prefs, Training Profile, Daily Summary
- [ ] Create `domain/nutrition/MealLog.java`
- [ ] Create `domain/nutrition/WaterLog.java`
- [ ] Create `domain/customer/CustomerFoodPreference.java`
- [ ] Create `domain/customer/CustomerTrainingProfile.java`
- [ ] Create `service/DailySummaryService.java` (computed — no separate entity)
- [ ] Create controllers for each

### Section 18 — Supplements & Reminders
- [ ] Create `domain/health/SupplementSchedule.java`
- [ ] Create `domain/health/CustomerReminder.java`
- [ ] Create enums: `SupplementTimingType`, `ReminderType`, `ReminderStatus`
- [ ] Create `service/SupplementService.java`
- [ ] Implement meal-linked timing calculation

### Section 19 — Appointments & Availability
- [ ] Create `domain/appointment/ProviderAvailability.java`
- [ ] Create `domain/appointment/Appointment.java`
- [ ] Create enums: `AppointmentType`, `AppointmentStatus`
- [ ] Capacity internal/customer label mapping
- [ ] Create `service/AppointmentService.java`
- [ ] Create `controller/AppointmentController.java`

### Section 20 — Customer Tool Access
- [ ] Create `domain/access/CustomerToolAccess.java`
- [ ] Create `domain/access/PlatformToolConfiguration.java`
- [ ] Create enums: `ToolAccessState`, `ToolAccessSource`
- [ ] Create `service/CustomerToolAccessService.java`
- [ ] Create `controller/CustomerToolAccessController.java`

### Section 21 — Upgrade Pricing
- [ ] Create `domain/access/PlatformFeaturePricing.java`
- [ ] Create `domain/access/CustomerToolUpgradeSubscription.java`
- [ ] Implement upgrade price calculation (overlap days logic)
- [ ] Create `controller/ToolUpgradeController.java`
- [ ] Test: 20/10 day split example

### Section 22 — Body Measurements & Progress
- [ ] Create `domain/progress/BodyMeasurement.java`
- [ ] Create `service/MeasurementService.java`
- [ ] Create `controller/BodyMeasurementController.java`
- [ ] Implement chart-ready analytics responses

### Section 23 — Gym Equipment QR
- [ ] Create `domain/gym/GymEquipment.java`
- [ ] Create `domain/gym/EquipmentExerciseLink.java`
- [ ] Create enums: `EquipmentStatus`, `EquipmentCategory`
- [ ] Generate unique QR value (UUID-based)
- [ ] Create `service/GymEquipmentService.java`
- [ ] Create `controller/GymEquipmentController.java`
- [ ] Use `GymAccessService` + `MANAGE_EQUIPMENT`

### Section 24 — Workout Sharing
- [ ] Create `domain/training/WorkoutShare.java`
- [ ] Create enums: `WorkoutShareType`, `WorkoutShareVisibility`
- [ ] Implement public safe-data filter (no PII)
- [ ] Create `service/WorkoutShareService.java`
- [ ] Create `controller/WorkoutShareController.java`
- [ ] Add public share endpoint to `SecurityConfig`

### Section 25 — Ratings & Performance
- [ ] Create `domain/rating/RatingReview.java`
- [ ] Create enum: `RatingStatus`
- [ ] Implement last-6-months filter
- [ ] Implement commitment-weighted internal score
- [ ] Create `service/RatingService.java`
- [ ] Create `controller/RatingController.java`

### Section 26 — Customer Analytics
- [ ] Implement `service/AnalyticsService.java` (computed from existing logged data)
- [ ] Create `controller/AnalyticsController.java`
- [ ] Support date range filters + viewType

### Section 27 — Requests Center
- [ ] Create `domain/request/ProviderRequest.java`
- [ ] Create enums: `RequestType`, `RequestStatus`
- [ ] Create `service/RequestCenterService.java`
- [ ] Create `controller/RequestCenterController.java`
- [ ] Audit on approve/reject

### Section 28 — Complaints, Renewal, Cancellation, Freeze (Provider)
- [ ] Create `domain/request/ProviderServiceCancellation.java`
- [ ] Create `domain/request/ProviderServiceFreeze.java`
- [ ] Create `domain/request/Complaint.java`
- [ ] Create enums: `ComplaintType`, `ComplaintStatus`
- [ ] Create `service/ComplaintService.java`
- [ ] Ensure no interference with `SubscriptionFreeze` (Phase 1)

### Section 29 — Audit Log
- [ ] Create `domain/audit/AuditLog.java`
- [ ] Create `service/AuditLogService.java`
- [ ] Integrate into relationship, payment, data-sharing, measurement, session-edit, appointment, freeze, request flows

### Section 30 — Security Matrix Review
- [ ] Verify all customer endpoints resolve identity from JWT only
- [ ] Verify all specialist endpoints scope to own customers
- [ ] Verify all gym endpoints use `GymAccessService`
- [ ] Verify public endpoints expose no PII

### Section 31 — Tests
- [ ] Implement all 50 test cases listed in Section 13

### Section 32 — Documentation
- [ ] Create `PHASE_2_API_GUIDE.md`
- [ ] Update `README.md` compatibility note

---

*End of Plan — Implementation begins at Section 2.*
