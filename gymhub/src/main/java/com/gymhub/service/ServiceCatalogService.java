package com.gymhub.service;

import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.gymservice.GymService;
import com.gymhub.domain.gymservice.ServiceStatus;
import com.gymhub.dto.request.CreateServiceRequest;
import com.gymhub.dto.response.ServiceResponse;
import com.gymhub.exception.DuplicateResourceException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.repository.GymServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final GymServiceRepository serviceRepository;
    private final GymManagementService gymManagementService;

    @Transactional
    public ServiceResponse createService(Long gymId, CreateServiceRequest request) {
        Gym gym = gymManagementService.findGymOrThrow(gymId);

        if (serviceRepository.existsByNameIgnoreCaseAndGymId(request.getName(), gymId)) {
            throw new DuplicateResourceException("Service already exists: " + request.getName());
        }

        GymService service = GymService.builder()
                .gym(gym)
                .name(request.getName())
                .description(request.getDescription())
                .canBeIncludedInPackage(request.isCanBeIncludedInPackage())
                .canBeSoldIndependently(request.isCanBeSoldIndependently())
                .status(ServiceStatus.ACTIVE)
                .build();

        return toResponse(serviceRepository.save(service));
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getServices(Long gymId, Pageable pageable) {
        return serviceRepository.findByGymId(gymId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getService(Long gymId, Long serviceId) {
        GymService svc = findOrThrow(serviceId);
        assertBelongsToGym(svc, gymId);
        return toResponse(svc);
    }

    @Transactional
    public ServiceResponse updateService(Long gymId, Long serviceId, CreateServiceRequest request) {
        GymService svc = findOrThrow(serviceId);
        assertBelongsToGym(svc, gymId);

        svc.setName(request.getName());
        svc.setDescription(request.getDescription());
        svc.setCanBeIncludedInPackage(request.isCanBeIncludedInPackage());
        svc.setCanBeSoldIndependently(request.isCanBeSoldIndependently());

        return toResponse(serviceRepository.save(svc));
    }

    @Transactional
    public void toggleStatus(Long gymId, Long serviceId, ServiceStatus status) {
        GymService svc = findOrThrow(serviceId);
        assertBelongsToGym(svc, gymId);
        svc.setStatus(status);
        serviceRepository.save(svc);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public GymService findOrThrow(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));
    }

    private void assertBelongsToGym(GymService svc, Long gymId) {
        if (!svc.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("Service not found in this gym");
        }
    }

    private ServiceResponse toResponse(GymService svc) {
        return ServiceResponse.builder()
                .id(svc.getId())
                .gymId(svc.getGym().getId())
                .name(svc.getName())
                .description(svc.getDescription())
                .canBeIncludedInPackage(svc.isCanBeIncludedInPackage())
                .canBeSoldIndependently(svc.isCanBeSoldIndependently())
                .status(svc.getStatus())
                .createdAt(svc.getCreatedAt())
                .build();
    }
}
