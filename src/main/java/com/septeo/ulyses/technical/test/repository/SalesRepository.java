package com.septeo.ulyses.technical.test.repository;

import com.septeo.ulyses.technical.test.entity.Brand;
import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.entity.Vehicle;
import com.septeo.ulyses.technical.test.model.VehicleSalesCountProjection;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

    /**
     * Find a paginated page of sales, ordered by ID ascending.
     *
     * @param page zero-based page index
     * @return up to 10 sales records for the given page; empty list if the page is out of range
     */
    List<Sales> findAllPaginated(int page);

    /**
     * Stream the {@code (vehicleId, salesCount)} aggregate for each vehicle, with
     * optional date-range filtering. Projects only the vehicle id — no entity
     * hydration — and returns a lazy {@link Stream} so callers can select a top-N
     * with O(N) memory instead of materialising one row per vehicle.
     *
     * <p>The returned stream is backed by an open JDBC {@code ResultSet} and MUST be
     * consumed and closed within an active transaction (use try-with-resources).
     *
     * @param startDate inclusive lower bound on sale date, or null for no lower bound
     * @param endDate   inclusive upper bound on sale date, or null for no upper bound
     * @return lazy stream of {@link VehicleSalesCountProjection}, unordered
     */
    Stream<VehicleSalesCountProjection> streamSalesCountByVehicle(LocalDate startDate, LocalDate endDate);

}
