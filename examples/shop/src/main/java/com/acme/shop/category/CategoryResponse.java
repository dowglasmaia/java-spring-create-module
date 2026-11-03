package com.acme.shop.category;

import java.util.UUID;

record CategoryResponse(UUID id, String name) {

    static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
