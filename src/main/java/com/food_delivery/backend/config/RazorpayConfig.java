package com.food_delivery.backend.config;

import com.food_delivery.backend.config.properties.RazorpayProperties;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RazorpayConfig {

    private final RazorpayProperties razorpayProperties;

    /**
     * Creates a singleton RazorpayClient bean that is injected into the
     * PaymentServiceImpl. The client is thread-safe and can be reused.
     */
    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        log.info("Initializing Razorpay client with key ID: {}", razorpayProperties.getKeyId());
        return new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());
    }
}