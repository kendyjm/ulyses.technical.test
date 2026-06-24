package com.septeo.ulyses.technical.test.service;

import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.entity.Vehicle;
import com.septeo.ulyses.technical.test.model.VehicleSalesCount;
import com.septeo.ulyses.technical.test.model.VehicleSalesCountProjection;
import com.septeo.ulyses.technical.test.repository.BrandRepository;
import com.septeo.ulyses.technical.test.repository.SalesRepository;
import com.septeo.ulyses.technical.test.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

    /** Maximum number of best-selling vehicles returned. */
    private static final int MAX_RESULTS = 5;

    @Override
    public List<VehicleSalesCount> getBestSellingVehicles(LocalDate startDate, LocalDate endDate) {
        // Stream the (vehicleId, count) projection and keep only the running top-5, so
        // memory stays O(MAX_RESULTS) even with millions of vehicles. The buffer is held
        // in ranked order (best first) by manual insertion.
        List<VehicleSalesCountProjection> top = new ArrayList<>(MAX_RESULTS + 1);
        try (Stream<VehicleSalesCountProjection> rows =
                     salesRepository.streamSalesCountByVehicle(startDate, endDate)) {
            rows.forEach(candidate -> {
                int pos = top.size();
                for (int i = 0; i < top.size(); i++) {
                    if (ranksBefore(candidate, top.get(i))) {
                        pos = i;
                        break;
                    }
                }
                if (pos < MAX_RESULTS) {
                    top.add(pos, candidate);
                    if (top.size() > MAX_RESULTS) {
                        top.remove(MAX_RESULTS); // drop the new worst entry
                    }
                }
            });
        }

        // Hydrate only the (<= MAX_RESULTS) selected vehicles, preserving ranked order.
        List<VehicleSalesCount> result = new ArrayList<>(top.size());
        for (VehicleSalesCountProjection ranked : top) {
            Vehicle vehicle = vehicleRepository.findById(ranked.vehicleId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Vehicle " + ranked.vehicleId() + " referenced by sales no longer exists"));
            result.add(new VehicleSalesCount(vehicle, ranked.salesCount()));
        }
        return result;
    }

    /** A candidate ranks before another iff it has more sales, or equal sales and a lower id. */
    private static boolean ranksBefore(VehicleSalesCountProjection a, VehicleSalesCountProjection b) {
        return a.salesCount() > b.salesCount()
                || (a.salesCount().equals(b.salesCount()) && a.vehicleId() < b.vehicleId());
    }

}
