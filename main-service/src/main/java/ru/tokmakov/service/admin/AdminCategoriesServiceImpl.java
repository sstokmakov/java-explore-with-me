package ru.tokmakov.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.tokmakov.exception.NotFoundException;
import ru.tokmakov.exception.category.CategoryNameAlreadyExistsException;
import ru.tokmakov.dto.category.CategoryDto;
import ru.tokmakov.dto.category.CategoryMapper;
import ru.tokmakov.dto.category.NewCategoryDto;
import org.springframework.stereotype.Service;
import ru.tokmakov.exception.category.CategoryNotEmptyException;
import ru.tokmakov.model.Category;
import ru.tokmakov.repository.CategoriesRepository;
import ru.tokmakov.repository.EventRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCategoriesServiceImpl implements AdminCategoriesService {
    private final CategoriesRepository categoriesRepository;
    private final EventRepository eventRepository;

    @Override
    public CategoryDto saveCategory(NewCategoryDto newCategoryDto) {
        log.info("Attempting to save new category: {}", newCategoryDto);

        if (categoriesRepository.existsByName(newCategoryDto.getName())) {
            log.error("Category with name {} already exists", newCategoryDto.getName());
            throw new CategoryNameAlreadyExistsException("Category with name " + newCategoryDto.getName() + " already exists");
        }

        Category category = CategoryMapper.newCategoryDtoToCategory(newCategoryDto);

        Category savedCategory = categoriesRepository.save(category);

        log.info("Category saved successfully: {}", savedCategory);

        return CategoryMapper.categoryToCategoryDto(savedCategory);
    }

    @Override
    public void deleteCategory(long catId) {
        log.info("Attempting to delete category with ID: {}", catId);

        Category category = categoriesRepository.findById(catId)
                .orElseThrow(() -> {
                    log.error("Category with ID {} not found. Deletion failed.", catId);
                    return new NotFoundException("Category with ID " + catId + " not found");
                });

        if (eventRepository.existsByCategoryId(category.getId())) {
            throw new CategoryNotEmptyException("Cannot delete category with ID " + catId + " category is not empty");
        } else {
            categoriesRepository.delete(category);
            log.info("Category with ID {} successfully deleted.", catId);
        }
    }

    @Override
    public CategoryDto updateCategory(long catId, NewCategoryDto newCategoryDto) {
        log.info("Starting update for category with ID: {}. New data: {}", catId, newCategoryDto);

        Category category;
        try {
            category = categoriesRepository.findById(catId)
                    .orElseThrow(() -> new NotFoundException("Category with ID " + catId + " not found"));
        } catch (NotFoundException e) {
            log.error("Category with ID {} not found. Update failed.", catId, e);
            throw e;
        }

        if (categoriesRepository.existsByName(newCategoryDto.getName())
            && !category.getName().equals(newCategoryDto.getName())) {
            log.warn("Category name '{}' already exists. Update for ID {} cannot proceed.", newCategoryDto.getName(), catId);
            throw new CategoryNameAlreadyExistsException("Category with name '" + newCategoryDto.getName() + "' already exists");
        }

        category.setName(newCategoryDto.getName());
        log.info("Updating category name to '{}'", newCategoryDto.getName());

        Category updatedCategory = categoriesRepository.save(category);
        log.info("Category with ID {} successfully updated.", catId);

        CategoryDto updatedCategoryDto = CategoryMapper.categoryToCategoryDto(updatedCategory);
        log.info("Returning updated category DTO: {}", updatedCategoryDto);

        return updatedCategoryDto;
    }
}