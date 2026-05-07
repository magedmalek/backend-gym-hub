package com.gymhub.service;

import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.gympackage.GymPackage;
import com.gymhub.domain.gymservice.GymService;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.CreatePackageRequest;
import com.gymhub.dto.response.PackageResponse;
import com.gymhub.dto.response.ServiceResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.repository.GymPackageRepository;
import com.gymhub.repository.GymServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final GymPackageRepository packageRepository;
    private final GymServiceRepository serviceRepository;
    private final GymManagementService gymManagementService;
    private final GymAccessService gymAccessService;

    @Transactional
    public PackageResponse createPackage(Long gymId, CreatePackageRequest request, User currentUser) {
        gymAccessService.resolveActingEmployee(currentUser, gymId, EmployeePermission.MANAGE_PACKAGES);
        Gym gym = gymManagementService.findGymOrThrow(gymId);

        Set<GymService> services = resolveServices(gymId, request.getIncludedServiceIds());

        if (request.isFamilyPackage() && request.getMaxSubUsers() <= 0) {
            throw new BusinessException("maxSubUsers must be > 0 for a family package");
        }

        GymPackage pkg = GymPackage.builder()
                .gym(gym)
                .name(request.getName())
                .description(request.getDescription())
                .durationDays(request.getDurationDays())
                .bonusDays(request.getBonusDays())
                .price(request.getPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "EGP")
                .freezeAllowanceDays(request.getFreezeAllowanceDays())
                .maxInvitations(request.getMaxInvitations())
                .allowGuestRepeatVisit(request.isAllowGuestRepeatVisit())
                .allowPartialPayment(request.isAllowPartialPayment())
                .isFamilyPackage(request.isFamilyPackage())
                .maxSubUsers(request.isFamilyPackage() ? request.getMaxSubUsers() : 0)
                .includedServices(services)
                .active(true)
                .build();

        return toResponse(packageRepository.save(pkg));
    }

    @Transactional(readOnly = true)
    public Page<PackageResponse> getPackages(Long gymId, User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return packageRepository.findByGymId(gymId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PackageResponse getPackage(Long gymId, Long packageId, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        GymPackage pkg = findOrThrow(packageId);
        assertBelongsToGym(pkg, gymId);
        return toResponse(pkg);
    }

    @Transactional
    public PackageResponse updatePackage(Long gymId, Long packageId,
                                         CreatePackageRequest request, User currentUser) {
        gymAccessService.resolveActingEmployee(currentUser, gymId, EmployeePermission.MANAGE_PACKAGES);
        GymPackage pkg = findOrThrow(packageId);
        assertBelongsToGym(pkg, gymId);

        if (request.isFamilyPackage() && request.getMaxSubUsers() <= 0) {
            throw new BusinessException("maxSubUsers must be > 0 for a family package");
        }

        pkg.setName(request.getName());
        pkg.setDescription(request.getDescription());
        pkg.setDurationDays(request.getDurationDays());
        pkg.setBonusDays(request.getBonusDays());
        pkg.setPrice(request.getPrice());
        pkg.setFreezeAllowanceDays(request.getFreezeAllowanceDays());
        pkg.setMaxInvitations(request.getMaxInvitations());
        pkg.setAllowGuestRepeatVisit(request.isAllowGuestRepeatVisit());
        pkg.setAllowPartialPayment(request.isAllowPartialPayment());
        pkg.setFamilyPackage(request.isFamilyPackage());
        pkg.setMaxSubUsers(request.isFamilyPackage() ? request.getMaxSubUsers() : 0);

        if (request.getIncludedServiceIds() != null) {
            pkg.setIncludedServices(resolveServices(gymId, request.getIncludedServiceIds()));
        }

        return toResponse(packageRepository.save(pkg));
    }

    @Transactional
    public void toggleStatus(Long gymId, Long packageId, boolean active, User currentUser) {
        gymAccessService.resolveActingEmployee(currentUser, gymId, EmployeePermission.MANAGE_PACKAGES);
        GymPackage pkg = findOrThrow(packageId);
        assertBelongsToGym(pkg, gymId);
        pkg.setActive(active);
        packageRepository.save(pkg);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public GymPackage findOrThrow(Long packageId) {
        return packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package", packageId));
    }

    private void assertBelongsToGym(GymPackage pkg, Long gymId) {
        if (!pkg.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("Package not found in this gym");
        }
    }

    private Set<GymService> resolveServices(Long gymId, Set<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) return new HashSet<>();
        Set<GymService> services = new HashSet<>();
        for (Long id : serviceIds) {
            GymService svc = serviceRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Service", id));
            if (!svc.getGym().getId().equals(gymId)) {
                throw new BusinessException("Service " + id + " does not belong to this gym");
            }
            if (!svc.isCanBeIncludedInPackage()) {
                throw new BusinessException("Service '" + svc.getName() + "' cannot be included in packages");
            }
            services.add(svc);
        }
        return services;
    }

    private PackageResponse toResponse(GymPackage pkg) {
        List<ServiceResponse> services = pkg.getIncludedServices().stream()
                .map(s -> ServiceResponse.builder()
                        .id(s.getId())
                        .gymId(s.getGym().getId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .canBeIncludedInPackage(s.isCanBeIncludedInPackage())
                        .canBeSoldIndependently(s.isCanBeSoldIndependently())
                        .status(s.getStatus())
                        .createdAt(s.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PackageResponse.builder()
                .id(pkg.getId())
                .gymId(pkg.getGym().getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .durationDays(pkg.getDurationDays())
                .bonusDays(pkg.getBonusDays())
                .price(pkg.getPrice())
                .currency(pkg.getCurrency())
                .freezeAllowanceDays(pkg.getFreezeAllowanceDays())
                .maxInvitations(pkg.getMaxInvitations())
                .allowGuestRepeatVisit(pkg.isAllowGuestRepeatVisit())
                .allowPartialPayment(pkg.isAllowPartialPayment())
                .isFamilyPackage(pkg.isFamilyPackage())
                .maxSubUsers(pkg.getMaxSubUsers())
                .active(pkg.isActive())
                .includedServices(services)
                .createdAt(pkg.getCreatedAt())
                .build();
    }
}
