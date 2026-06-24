package com.septeo.ulyses.technical.test.service;

import com.septeo.ulyses.technical.test.entity.Sales;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Sales operations.
 */
public interface SalesService {

    /**
     * Get all sales.
     *
     * @return a list of all sales
     */
    List<Sales> getAllSales();

    /**
     * Get a sales by its ID.
     *
     * @param id the ID of the sales to find
     * @return an Optional containing the sales if found, or empty if not found
     */
    Optional<Sales> getSalesById(Long id);

    /**
     * Get all sales for a specific brand.
     *
     * @param brandId the ID of the brand to look up
     * @return an Optional containing the list of sales if the brand exists (may be empty list),
     *         or Optional.empty() if the brand does not exist
     */
    Optional<List<Sales>> getSalesByBrandId(Long brandId);

    /**
     * Get all sales for a specific vehicle.
     *
     * @param vehicleId the ID of the vehicle to look up
     * @return an Optional containing the list of sales if the vehicle exists (may be empty list),
     *         or Optional.empty() if the vehicle does not exist
     */
    Optional<List<Sales>> getSalesByVehicleId(Long vehicleId);

}
