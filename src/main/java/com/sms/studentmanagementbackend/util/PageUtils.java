package com.sms.studentmanagementbackend.util;

import com.sms.studentmanagementbackend.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageUtils {

    private PageUtils() {
    }

    public static Sort createSort(String sortBy, String sortDir) {
        return Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    }

    public static Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        return PageRequest.of(page, size, createSort(sortBy, sortDir));
    }

    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
