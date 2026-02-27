package com.monsoon.seedflowplus.domain.deal.common;

import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static Pageable parsePageRequest(
            int page,
            int size,
            String sort,
            Sort defaultSort,
            Set<String> allowedSortProperties,
            int maxSize
    ) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page는 0 이상이어야 합니다.");
        }
        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1 이상이어야 합니다.");
        }
        if (size > maxSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 " + maxSize + " 이하여야 합니다.");
        }

        Sort parsedSort = parseSort(sort, defaultSort, allowedSortProperties);
        return PageRequest.of(page, size, parsedSort);
    }

    private static Sort parseSort(String sort, Sort defaultSort, Set<String> allowedSortProperties) {
        Objects.requireNonNull(defaultSort, "defaultSort는 null값이 될 수 없습니다.");
        Objects.requireNonNull(allowedSortProperties, "allowedSortProperties는 null값이 될 수 없습니다.");
        if (sort == null || sort.isBlank()) {
            return defaultSort;
        }

        String[] tokens = sort.split(",");
        if (tokens.length < 1 || tokens.length > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sort 형식이 올바르지 않습니다. 예: property,asc");
        }

        String property = tokens[0].trim();
        if (property.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sort property는 필수입니다.");
        }
        if (!allowedSortProperties.contains(property)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않은 sort property입니다: " + property);
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (tokens.length == 2) {
            String rawDirection = tokens[1].trim();
            if (rawDirection.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sort direction은 비어있을 수 없습니다.");
            }
            direction = Sort.Direction.fromOptionalString(rawDirection)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.BAD_REQUEST, "sort direction은 asc 또는 desc여야 합니다."));
        }

        return Sort.by(new Sort.Order(direction, property));
    }
}
