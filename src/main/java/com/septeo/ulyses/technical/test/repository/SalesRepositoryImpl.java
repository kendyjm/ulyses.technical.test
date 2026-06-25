package com.septeo.ulyses.technical.test.repository;

import com.septeo.ulyses.technical.test.entity.Brand;
import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.entity.Vehicle;
import com.septeo.ulyses.technical.test.model.VehicleSalesCountProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of the SalesRepository interface.
 * This class provides the implementation for all sales-related operations.
 */
@Repository
public class SalesRepositoryImpl implements SalesRepository {

    /** Fixed number of records per page (see project pagination contract). */
    private static final int PAGE_SIZE = 10;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Sales> findAll() {
        String stringQuery = "SELECT s FROM Sales s";
        Query query = entityManager.createQuery(stringQuery);
        return query.getResultList();
    }

    @Override
    public Optional<Sales> findById(Long id) {
        String stringQuery = "SELECT s FROM Sales s WHERE s.id = :id";
        Query query = entityManager.createQuery(stringQuery);
        query.setParameter("id", id);

        try {
            return Optional.of((Sales) query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Sales> findByBrandId(Long brandId) {
        String stringQuery = "SELECT s FROM Sales s WHERE s.brand.id = :brandId";
        Query query = entityManager.createQuery(stringQuery);
        query.setParameter("brandId", brandId);
        return query.getResultList();
    }

    @Override
    public List<Sales> findByVehicleId(Long vehicleId) {
        String stringQuery = "SELECT s FROM Sales s WHERE s.vehicle.id = :vehicleId";
        Query query = entityManager.createQuery(stringQuery);
        query.setParameter("vehicleId", vehicleId);
        return query.getResultList();
    }

    @Override
    public List<Sales> findAllPaginated(int page) {
        // Compute the offset in long arithmetic so a very large page cannot overflow
        // int and wrap to a negative value (which setFirstResult would reject). Any
        // offset beyond the addressable range is, by definition, out of range -> empty.
        long firstResult = (long) page * PAGE_SIZE;
        if (firstResult > Integer.MAX_VALUE) {
            return List.of();
        }
        String stringQuery = "SELECT s FROM Sales s ORDER BY s.id ASC";
        Query query = entityManager.createQuery(stringQuery);
        query.setFirstResult((int) firstResult);
        query.setMaxResults(PAGE_SIZE);
        return query.getResultList();
    }

    @Override
    public Stream<VehicleSalesCountProjection> streamSalesCountByVehicle(LocalDate startDate, LocalDate endDate) {
        boolean hasStart = startDate != null;
        boolean hasEnd = endDate != null;

        // Constructor expression projects straight into a typed record. Only s.vehicle.id
        // (the FK column on sales) is read — Hibernate resolves it without joining the
        // vehicles table, so no Vehicle entity is hydrated here.
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.septeo.ulyses.technical.test.model.VehicleSalesCountProjection("
                        + "s.vehicle.id, COUNT(s)) FROM Sales s");
        if (hasStart && hasEnd) {
            jpql.append(" WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate");
        } else if (hasStart) {
            jpql.append(" WHERE s.saleDate >= :startDate");
        } else if (hasEnd) {
            jpql.append(" WHERE s.saleDate <= :endDate");
        }
        jpql.append(" GROUP BY s.vehicle.id");

        TypedQuery<VehicleSalesCountProjection> query =
                entityManager.createQuery(jpql.toString(), VehicleSalesCountProjection.class);
        if (hasStart) query.setParameter("startDate", startDate);
        if (hasEnd) query.setParameter("endDate", endDate);

        return query.getResultStream();
    }
}
