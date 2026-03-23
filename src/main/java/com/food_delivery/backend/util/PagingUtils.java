package com.food_delivery.backend.util;

import com.food_delivery.backend.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PagingUtils {

    private static final int MAX_PAGE_SIZE = 100;

    private PagingUtils() {
    }

    public static Pageable pageRequest(int page, int size, Sort sort) {
        if (page < 0) {
            throw new BadRequestException("Page index must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }
}
