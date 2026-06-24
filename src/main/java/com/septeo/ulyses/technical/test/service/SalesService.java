package com.septeo.ulyses.technical.test.service;

import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.model.VehicleSalesCount;

import java.time.LocalDate;
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

    /**
     * Get a paginated page of all sales.
     *
     * @param page zero-based page index; negative values must be rejected by the caller
     * @return up to 10 sales records for the given page; empty list if the page is out of range
     */
    List<Sales> getSalesPaginated(int page);

    /**
     * Get the top 5 best-selling vehicles with optional date-range filtering.
     * Ties in salesCount are broken by ascending vehicle id for deterministic ordering.
     *
     * @param startDate inclusive lower bound on sale date, or null for no lower bound
     * @param endDate   inclusive upper bound on sale date, or null for no upper bound
     * @return up to 5 VehicleSalesCount objects ranked highest-to-lowest by salesCount
     */
    List<VehicleSalesCount> getBestSellingVehicles(LocalDate startDate, LocalDate endDate);

}
