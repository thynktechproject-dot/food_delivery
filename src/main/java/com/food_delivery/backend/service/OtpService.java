package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.GenerateOtpRequest;
import com.food_delivery.backend.dto.GenerateOtpResponse;

public interface OtpService {

    GenerateOtpResponse generateOtp(GenerateOtpRequest request);
}
