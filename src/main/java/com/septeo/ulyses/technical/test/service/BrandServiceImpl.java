package com.septeo.ulyses.technical.test.service;

import com.septeo.ulyses.technical.test.cache.BrandCache;
import com.septeo.ulyses.technical.test.entity.Brand;
import com.septeo.ulyses.technical.test.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the BrandService interface.
 * This class provides the implementation for all brand-related operations.
 */
@Service
@Transactional(readOnly = false)
public class BrandServiceImpl implements BrandService {

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private BrandCache brandCache;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Brand> getAllBrands() {
        List<Brand> cached = brandCache.getAllBrands();
        if (cached != null) return cached;
        List<Brand> result = brandRepository.findAll();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                brandCache.putAllBrands(result);
            }
        });
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Brand> getBrandById(Long id) {
        Optional<Brand> cached = brandCache.getById(id);
        if (cached != null) return cached;
        Optional<Brand> result = brandRepository.findById(id);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                brandCache.putById(id, result);
            }
        });
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Brand saveBrand(Brand brand) {
        Brand saved = brandRepository.save(brand);
        Long savedId = saved.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                brandCache.evict(savedId);
            }
        });
        return saved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteBrand(Long id) {
        brandRepository.deleteById(id);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                brandCache.evict(id);
            }
        });
    }
}
