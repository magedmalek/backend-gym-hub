package com.gymhub.service;

import com.gymhub.domain.request.Complaint;
import com.gymhub.domain.request.ComplaintStatus;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.ComplaintRequest;
import com.gymhub.dto.request.ComplaintStatusUpdateRequest;
import com.gymhub.dto.response.ComplaintResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * FND-14 Request Center & Complaint Standards (Phase 2 plan — Section 28).
 */
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final GymAccessService gymAccessService;

    // ── Customer ──────────────────────────────────────────────────────────────

    @Transactional
    public ComplaintResponse fileComplaint(ComplaintRequest request, User currentUser) {
        if (request.getGymId() == null && request.getAgainstProviderId() == null) {
            throw new BusinessException(
                    "A complaint must reference a gym and/or a provider for routing");
        }
        Complaint complaint = Complaint.builder()
                .complainant(currentUser)
                .type(request.getType())
                .subject(request.getSubject())
                .body(request.getBody())
                .gymId(request.getGymId())
                .againstProviderId(request.getAgainstProviderId())
                .status(ComplaintStatus.NEW)
                .build();
        return toResponse(complaintRepository.save(complaint));
    }

    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getMyComplaints(User currentUser, Pageable pageable) {
        return complaintRepository
                .findByComplainantIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ComplaintResponse cancelMyComplaint(Long complaintId, User currentUser) {
        Complaint complaint = complaintRepository
                .findByIdAndComplainantId(complaintId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", complaintId));
        if (complaint.getStatus() == ComplaintStatus.RESOLVED
                || complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new BusinessException("Resolved or closed complaints cannot be cancelled");
        }
        complaint.setStatus(ComplaintStatus.CLOSED);
        complaint.setUpdatedAt(LocalDateTime.now());
        return toResponse(complaintRepository.save(complaint));
    }

    // ── Dashboard staff ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getGymComplaints(Long gymId, User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return complaintRepository.findByGymIdOrderByCreatedAtDesc(gymId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ComplaintResponse updateStatus(Long gymId, Long complaintId,
                                          ComplaintStatusUpdateRequest request, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        Complaint complaint = complaintRepository.findByIdAndGymId(complaintId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", complaintId));

        complaint.setStatus(request.getStatus());
        if (request.getResolutionNote() != null) {
            complaint.setResolutionNote(request.getResolutionNote());
        }
        complaint.setUpdatedAt(LocalDateTime.now());
        if (request.getStatus() == ComplaintStatus.RESOLVED
                || request.getStatus() == ComplaintStatus.CLOSED
                || request.getStatus() == ComplaintStatus.REJECTED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }
        return toResponse(complaintRepository.save(complaint));
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private ComplaintResponse toResponse(Complaint c) {
        return ComplaintResponse.builder()
                .id(c.getId())
                .complainantName(c.getComplainant() != null ? c.getComplainant().getFullName() : null)
                .type(c.getType())
                .subject(c.getSubject())
                .body(c.getBody())
                .gymId(c.getGymId())
                .againstProviderId(c.getAgainstProviderId())
                .status(c.getStatus())
                .resolutionNote(c.getResolutionNote())
                .createdAt(c.getCreatedAt())
                .resolvedAt(c.getResolvedAt())
                .build();
    }
}
