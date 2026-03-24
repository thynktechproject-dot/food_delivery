package com.food_delivery.backend.repository;

import com.food_delivery.backend.entity.Review;
import com.food_delivery.backend.entity.ReviewType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** All reviews for a specific target (e.g. all reviews for restaurant #5). */
    Page<Review> findByReviewTypeAndTargetId(ReviewType reviewType, Long targetId, Pageable pageable);

    /** All reviews written by a specific user. */
    Page<Review> findByUserId(Long userId, Pageable pageable);

    /** Check whether a user has already reviewed a target for a given order. */
    boolean existsByUserIdAndReviewTypeAndTargetIdAndOrderId(
            Long userId, ReviewType reviewType, Long targetId, Long orderId);

    /** Find a specific review by user + order + type + target (for update/delete guard). */
    Optional<Review> findByUserIdAndReviewTypeAndTargetIdAndOrderId(
            Long userId, ReviewType reviewType, Long targetId, Long orderId);

    /** Average rating for a target. */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewType = :type AND r.targetId = :targetId")
    Optional<Double> findAverageRating(@Param("type") ReviewType type, @Param("targetId") Long targetId);

    /** Total review count for a target. */
    long countByReviewTypeAndTargetId(ReviewType reviewType, Long targetId);
}