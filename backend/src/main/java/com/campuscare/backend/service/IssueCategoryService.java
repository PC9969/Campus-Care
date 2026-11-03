package com.campuscare.backend.service;

import com.campuscare.backend.dto.CategoryResponse;
import com.campuscare.backend.entity.IssueCategory;
import com.campuscare.backend.entity.IssueStatus;
import com.campuscare.backend.exception.ResourceNotFoundException;
import com.campuscare.backend.repository.IssueCategoryRepository;
import com.campuscare.backend.repository.IssueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class IssueCategoryService {

    private final IssueCategoryRepository issueCategoryRepository;
    private final IssueRepository issueRepository;

    public IssueCategoryService(
            IssueCategoryRepository issueCategoryRepository,
            IssueRepository issueRepository
    ) {
        this.issueCategoryRepository = issueCategoryRepository;
        this.issueRepository = issueRepository;
    }

    public List<CategoryResponse> getAllCategories() {
        List<IssueCategory> categories = issueCategoryRepository.findAll();
        List<CategoryResponse> result = new ArrayList<>();

        for (IssueCategory cat : categories) {
            long activeCount = issueRepository.findAll().stream()
                    .filter(i -> i.getCategory() != null && i.getCategory().getId().equals(cat.getId()))
                    .filter(i -> i.getStatus() != IssueStatus.RESOLVED && i.getStatus() != IssueStatus.CLOSED)
                    .count();
            result.add(new CategoryResponse(cat.getId(), cat.getName(), cat.getDescription(), activeCount));
        }

        return result;
    }

    public IssueCategory getCategoryById(Long id) {
        return issueCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    public Optional<IssueCategory> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return issueCategoryRepository.findByNameIgnoreCase(name.trim());
    }

    @Transactional
    public IssueCategory findOrCreateCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "General Maintenance";
        }
        String cleanName = name.trim();
        return issueCategoryRepository.findByNameIgnoreCase(cleanName)
                .orElseGet(() -> {
                    IssueCategory newCat = new IssueCategory();
                    newCat.setName(cleanName);
                    newCat.setDescription("Maintenance category for " + cleanName);
                    return issueCategoryRepository.save(newCat);
                });
    }
}
