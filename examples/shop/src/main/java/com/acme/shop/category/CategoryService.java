package com.acme.shop.category;

import com.acme.shop.shared.error.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CategoryService {

    private final CategoryRepository categories;

    CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional
    Category create(String name) {
        return categories.save(new Category(name));
    }

    @Transactional
    Category rename(UUID id, String name) {
        Category category = get(id);
        category.rename(name);
        return category;
    }

    @Transactional(readOnly = true)
    List<Category> list() {
        return categories.findAll();
    }

    @Transactional(readOnly = true)
    Category get(UUID id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category not found."));
    }

    @Transactional
    void delete(UUID id) {
        categories.delete(get(id));
    }
}
