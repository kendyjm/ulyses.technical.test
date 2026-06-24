package com.septeo.ulyses.technical.test.service;

import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.repository.BrandRepository;
import com.septeo.ulyses.technical.test.repository.SalesRepository;
import com.septeo.ulyses.technical.test.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the SalesService interface.
 * This class provides the implementation for all sales-related operations.
 */
@Service
@Transactional(readOnly = false)
public class SalesServiceImpl implements SalesService {

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Sales> getAllSales() {
        return salesRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Sales> getSalesById(Long id) {
        return salesRepository.findById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<List<Sales>> getSalesByBrandId(Long brandId) {
        if (brandRepository.findById(brandId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(salesRepository.findByBrandId(brandId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<List<Sales>> getSalesByVehicleId(Long vehicleId) {
        if (vehicleRepository.findById(vehicleId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(salesRepository.findByVehicleId(vehicleId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Sales> getSalesPaginated(int page) {
        return salesRepository.findAllPaginated(page);
    }

}
