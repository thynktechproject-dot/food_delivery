package com.food_delivery.backend.entity;

public enum OrderStatus {
    CREATED,
    PAID,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    FAILED
}