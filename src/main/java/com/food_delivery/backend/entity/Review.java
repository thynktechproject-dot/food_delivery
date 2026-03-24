package com.food_delivery.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                // One review per user per target per order — prevents duplicate reviews
                @UniqueConstraint(
                        name = "uq_review_user_type_target_order",
                        columnNames = {"user_id", "review_type", "target_id", "order_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The customer who wrote the review. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** The completed order that entitles this user to leave a review. */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** What kind of entity is being reviewed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false)
    private ReviewType reviewType;

    /**
     * ID of the reviewed entity — restaurant ID, menu item ID, or delivery agent user ID,
     * depending on reviewType.
     */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 1 – 5 star rating. */
    @Column(nullable = false)
    private int rating;

    /** Optional text feedback. */
    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}