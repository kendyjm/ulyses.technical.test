package com.septeo.ulyses.technical.test.model;

/**
 * Lightweight aggregate projection: a vehicle id and its total sales count.
 *
 * <p>Used as the streaming intermediate when ranking best-selling vehicles, so the
 * query reads only the vehicle foreign-key column (no {@code Vehicle} entity hydration).
 * The full {@link VehicleSalesCount} is built only for the selected top results.
 */
public record VehicleSalesCountProjection(Long vehicleId, Long salesCount) {}
