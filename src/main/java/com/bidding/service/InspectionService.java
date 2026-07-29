package com.bidding.service;

import com.bidding.dto.request.InspectionDraftRequest;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.dto.responce.InspectionSummaryResponse;

import java.util.List;

public interface InspectionService {

    InspectionDetailsResponse saveDraft(InspectionDraftRequest request, Long inspectorId);

    InspectionDetailsResponse getInspection(Long id);

    void submitInspection(Long id, Long inspectorId);

    List<InspectionSummaryResponse> getAllInspections();

    void approveInspection(Long id);

    void rejectInspection(Long id, String reason);

    byte[] generatePdfReport(Long id);

    void uploadInspectionImage(Long id, String category, String originalName, String fileUrl, Long inspectorId);

    InspectionDetailsResponse updateInspection(Long id, InspectionDraftRequest request, Long inspectorId);

    void deleteInspection(Long id);
}
