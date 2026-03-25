package com.food_delivery.backend.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.razorpay")
@Getter
@Setter
public class RazorpayProperties {

    /** Razorpay Key ID — starts with rzp_test_ or rzp_live_ */
    private String keyId;

    /** Razorpay Key Secret */
    private String keySecret;

    /** Currency code e.g. INR */
    private String currency = "INR";

    /** Webhook secret configured in Razorpay Dashboard */
    private String webhookSecret;
}