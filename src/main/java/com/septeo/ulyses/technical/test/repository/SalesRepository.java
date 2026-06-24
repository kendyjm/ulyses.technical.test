package com.septeo.ulyses.technical.test.repository;

import com.septeo.ulyses.technical.test.entity.Brand;
import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.entity.Vehicle;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Sales entity.
 */
@Repository
public interface SalesRepository {
    /**
     * Find all sales.
     *
     * @return a list of all sales
     */
    List<Sales> findAll();

    /**
     * Find a sale by its ID.
     *
     * @param id the ID of the sale to find
     * @return an Optional containing the sale if found, or empty if not found
     */
    Optional<Sales> findById(Long id);

    /**
     * Find all sales for a specific brand.
     *
     * @param brandId the ID of the brand
     * @return a list of all sales records for that brand; empty list if the brand has no sales
     */
    List<Sales> findByBrandId(Long brandId);

    /**
     * Find all sales for a specific vehicle.
     *
     * @param vehicleId the ID of the vehicle
     * @return a list of all sales records for that vehicle; empty list if the vehicle has no sales
     */
    List<Sales> findByVehicleId(Long vehicleId);

}
