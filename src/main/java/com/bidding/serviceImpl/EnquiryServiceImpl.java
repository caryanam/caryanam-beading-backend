package com.bidding.serviceImpl;

import com.bidding.dto.request.EnquiryRequest;
import com.bidding.dto.responce.EnquiryResponse;
import com.bidding.entity.Enquiry;
import com.bidding.repo.EnquiryRepository;
import com.bidding.service.EnquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnquiryServiceImpl implements EnquiryService {

    private final EnquiryRepository enquiryRepository;

    @Override
    public void submitEnquiry(EnquiryRequest request) {
        Enquiry enquiry = Enquiry.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .message(request.getMessage())
                .build();
        enquiryRepository.save(enquiry);
    }

    @Override
    public List<EnquiryResponse> getAllEnquiries() {
        return enquiryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(e -> EnquiryResponse.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .email(e.getEmail())
                        .phone(e.getPhone())
                        .message(e.getMessage())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
