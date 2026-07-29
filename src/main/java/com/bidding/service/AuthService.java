package com.bidding.service;

import com.bidding.dto.request.LoginRequest;
import com.bidding.dto.request.InspectorRegisterRequest;
import com.bidding.dto.request.DealerRegisterRequest;
import com.bidding.dto.responce.AuthResponse;
import com.bidding.dto.responce.InspectorResponseDTO;
import com.bidding.dto.responce.DealerResponseDTO;

public interface AuthService {

    InspectorResponseDTO registerInspector(InspectorRegisterRequest request);

    DealerResponseDTO registerDealer(DealerRegisterRequest request);

    AuthResponse login(LoginRequest request);
}


