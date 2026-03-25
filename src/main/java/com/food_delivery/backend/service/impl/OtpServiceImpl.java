package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.GenerateOtpRequest;
import com.food_delivery.backend.dto.GenerateOtpResponse;
import com.food_delivery.backend.entity.OtpCode;
import com.food_delivery.backend.entity.Role;
import com.food_delivery.backend.entity.User;
import com.food_delivery.backend.exception.BadRequestException;
import com.food_delivery.backend.repository.OtpCodeRepository;
import com.food_delivery.backend.repository.UserRepository;
import com.food_delivery.backend.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;

    @Override
    @Transactional
    public GenerateOtpResponse generateOtp(GenerateOtpRequest request) {
        Role role = validateSupportedRole(request.getRole());
        String identifier = resolveIdentifier(request);

        // Restaurant owners are stored in the same users table as regular users.
        // The OTP flow differentiates them by filtering on the requested role here.
        User user = validateUserForOtp(identifier, role, isEmailRequest(request))
                .filter(User::isActive)
                .orElseThrow(() -> new BadRequestException("No active account found for the provided identifier and role"));

        String otp = generateSecureOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        otpCodeRepository.deleteByIdentifierAndRole(identifier, role);
        otpCodeRepository.save(OtpCode.builder()
                .identifier(identifier)
                .role(role)
                .otp(otp)
                .expiresAt(expiresAt)
                .build());

        return GenerateOtpResponse.builder()
                .otp(otp)
                .message("OTP generated successfully")
                .identifier(identifier)
                .role(user.getRole())
                .expiresAt(expiresAt)
                .build();
    }

    public String generateSecureOtp() {
        return String.valueOf(SECURE_RANDOM.nextInt(900_000) + 100_000);
    }

    private Role validateSupportedRole(Role role) {
        if (role != Role.USER && role != Role.RESTAURANT_OWNER) {
            throw new BadRequestException("OTP is only supported for USER or RESTAURANT_OWNER");
        }
        return role;
    }

    private String resolveIdentifier(GenerateOtpRequest request) {
        if (isEmailRequest(request)) {
            return request.getEmail().trim();
        }
        return request.getPhone().trim();
    }

    private boolean isEmailRequest(GenerateOtpRequest request) {
        return request.getEmail() != null && !request.getEmail().isBlank();
    }

    // Validation happens against the existing users table. The same lookup is reused
    // for both flows, with role deciding whether we expect a USER or RESTAURANT_OWNER account.
    private Optional<User> validateUserForOtp(String identifier, Role role, boolean emailLookup) {
        if (emailLookup) {
            return userRepository.findByEmailAndRoleAndDeletedAtIsNull(identifier, role);
        }
        return userRepository.findByPhoneAndRoleAndDeletedAtIsNull(identifier, role);
    }
}
