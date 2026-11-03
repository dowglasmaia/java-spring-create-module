package com.acme.shop.category;

import jakarta.validation.constraints.NotBlank;

record CategoryRequest(@NotBlank String name) {
}
