package com.bidding.service;

import com.bidding.dto.request.InspectionDraftRequest;
import com.bidding.dto.responce.InspectionDetailsResponse;
import com.bidding.dto.responce.InspectionSummaryResponse;
import com.bidding.dto.responce.InspectorStatsResponse;
import com.bidding.dto.responce.InspectorResponseDTO;
import com.bidding.dto.responce.DealerResponseDTO;

import java.util.List;

public interface InspectionService {

    InspectionDetailsResponse saveDraft(InspectionDraftRequest request, Long inspectorId);

    InspectionDetailsResponse getInspection(Long id);

    void submitInspection(Long id, Long inspectorId);

    void submitFreelancerInspection(Long id, Long inspectorId);

    List<InspectionSummaryResponse> getAllInspections();

    List<InspectionSummaryResponse> getInspectionsByInspector(Long inspectorId);

    void approveInspection(Long id);

    void rejectInspection(Long id, String reason);

    byte[] generatePdfReport(Long id);

    byte[] generateDealerPdfReport(Long id);

    void uploadInspectionImage(Long id, String category, String originalName, String fileUrl, Long inspectorId);

    InspectionDetailsResponse updateInspection(Long id, InspectionDraftRequest request, Long inspectorId);

    void deleteInspection(Long id);

    InspectorStatsResponse getInspectorStats(Long inspectorId);

    List<InspectorResponseDTO> getAllInspectors();

    List<InspectorResponseDTO> getAllFreelancers();

    void deleteInspector(Long id);

    List<DealerResponseDTO> getAllDealers();

    DealerResponseDTO updateDealer(Long id, DealerResponseDTO dto);

    void deleteDealer(Long id);

    void goLive(Long id);

    void stopAuction(Long id);

    void importDealers(org.springframework.web.multipart.MultipartFile file);

    void submitSellerResponse(Long id, Boolean agreed, Double counterPrice, String message);

    void submitAdminDealerMessage(Long id, String message);

    void submitDealerReply(Long id, String reply);

    void updateVehicleStatus(Long id, String vehicleStatus);
}