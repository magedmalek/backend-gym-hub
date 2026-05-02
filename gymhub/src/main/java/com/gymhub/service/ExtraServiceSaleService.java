package com.gymhub.service;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.extraservice.ExtraServiceTransaction;
import com.gymhub.domain.extraservice.ServiceUsage;
import com.gymhub.domain.gymservice.GymService;
import com.gymhub.domain.subscription.Subscription;
import com.gymhub.dto.request.SellExtraServiceRequest;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.ExtraServiceTransactionRepository;
import com.gymhub.repository.ServiceUsageRepository;
import com.gymhub.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExtraServiceSaleService {

    private final ExtraServiceTransactionRepository transactionRepository;
    private final ServiceUsageRepository usageRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final CustomerService customerService;
    private final EmployeeManagementService employeeService;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Sells an extra paid service to a customer (independent financial transaction).
     * This must NOT be mixed with the subscription invoice.
     */
    @Transactional
    public ExtraServiceTransaction sellExtraService(Long gymId, SellExtraServiceRequest request,
                                                     Long employeeId) {
        Employee emp = employeeService.findOrThrow(employeeId);
        if (!emp.hasPermission(EmployeePermission.SELL_EXTRA_SERVICE)) {
            throw new UnauthorizedException("Employee does not have permission to sell extra services");
        }

        Customer customer = customerService.findOrThrow(request.getCustomerId());
        GymService service = serviceCatalogService.findOrThrow(request.getServiceId());

        if (!service.getGym().getId().equals(gymId)) {
            throw new BusinessException("Service does not belong to this gym");
        }
        if (!service.isCanBeSoldIndependently()) {
            throw new BusinessException("Service '" + service.getName() + "' cannot be sold independently");
        }

        ExtraServiceTransaction tx = ExtraServiceTransaction.builder()
                .customer(customer)
                .gym(service.getGym())
                .service(service)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "EGP")
                .soldBy(emp)
                .notes(request.getNotes())
                .build();

        return transactionRepository.save(tx);
    }

    /**
     * Records use of a service bundled inside a subscription (no financial transaction).
     */
    @Transactional
    public ServiceUsage recordIncludedServiceUsage(Long gymId, Long customerId,
                                                    Long serviceId, Long subscriptionId,
                                                    Long employeeId, String notes) {
        Employee emp = employeeService.findOrThrow(employeeId);
        Customer customer = customerService.findOrThrow(customerId);
        GymService service = serviceCatalogService.findOrThrow(serviceId);

        Subscription sub = subscriptionRepository.findByIdAndGymId(subscriptionId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));

        // Verify the service is in the package
        boolean included = sub.getGymPackage().getIncludedServices()
                .stream().anyMatch(s -> s.getId().equals(serviceId));
        if (!included) {
            throw new BusinessException("Service '" + service.getName() + "' is not included in this subscription");
        }

        ServiceUsage usage = ServiceUsage.builder()
                .customer(customer)
                .subscription(sub)
                .service(service)
                .recordedBy(emp)
                .notes(notes)
                .build();

        return usageRepository.save(usage);
    }

    @Transactional(readOnly = true)
    public Page<ExtraServiceTransaction> getCustomerExtraTransactions(Long customerId, Pageable pageable) {
        return transactionRepository.findByCustomerIdOrderBySoldAtDesc(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ExtraServiceTransaction> getGymExtraTransactions(Long gymId, Pageable pageable) {
        return transactionRepository.findByGymIdOrderBySoldAtDesc(gymId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ServiceUsage> getCustomerServiceUsages(Long customerId, Pageable pageable) {
        return usageRepository.findByCustomerIdOrderByUsedAtDesc(customerId, pageable);
    }
}
