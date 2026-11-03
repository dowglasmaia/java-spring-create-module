package {{BASE}}.{{feature}};

import jakarta.validation.constraints.NotBlank;

record {{Entity}}Request(@NotBlank String name) {
}
