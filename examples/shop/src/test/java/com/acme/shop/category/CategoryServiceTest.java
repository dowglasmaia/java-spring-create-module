package com.acme.shop.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.shop.shared.error.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categories;

    @InjectMocks
    private CategoryService service;

    @Test
    void createSavesTheCategory() {
        when(categories.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.create("Books").getName()).isEqualTo("Books");
    }

    @Test
    void anUnknownCategoryIsNotFound() {
        UUID id = UUID.randomUUID();
        when(categories.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class).hasMessage("Category not found.");
    }

    @Test
    void deleteRemovesAnExistingCategory() {
        Category category = new Category("Books");
        when(categories.findById(category.getId())).thenReturn(Optional.of(category));

        service.delete(category.getId());

        verify(categories).delete(category);
    }
}
