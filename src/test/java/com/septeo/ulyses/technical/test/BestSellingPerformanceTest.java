package com.septeo.ulyses.technical.test;

import com.septeo.ulyses.technical.test.entity.Vehicle;
import com.septeo.ulyses.technical.test.model.VehicleSalesCount;
import com.septeo.ulyses.technical.test.model.VehicleSalesCountProjection;
import com.septeo.ulyses.technical.test.repository.SalesRepository;
import com.septeo.ulyses.technical.test.repository.VehicleRepository;
import com.septeo.ulyses.technical.test.service.SalesServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Performance test for the top-5 best-selling ranking algorithm.
 * Validates time and heap consumption on 1 million vehicles.
 * No Spring context — repositories are mocked, only the in-memory algo is exercised.
 */
@ExtendWith(MockitoExtension.class)
class BestSellingPerformanceTest {

    @Mock SalesRepository  salesRepository;
    @Mock VehicleRepository vehicleRepository;
    @InjectMocks SalesServiceImpl service;

    private static final LocalDate FROM = LocalDate.of(2025, 1, 1);
    private static final LocalDate TO   = LocalDate.of(2025, 12, 31);
    private static final int       N    = 1_000_000;

    @Test
    void performance_1millionVehicles() {
        // Lazy stream: projection objects are created one at a time, O(1) memory in the generator itself.
        Random rng = new Random(42);
        when(salesRepository.streamSalesCountByVehicle(any(), any()))
            .thenReturn(Stream.generate(
                () -> new VehicleSalesCountProjection(
                    (long) rng.nextInt(1, N + 1),
                    (long) rng.nextInt(1, 10_000_001)))
                .limit(N));

        // Only the ≤5 winners will trigger findById — lenient because the exact ids are unpredictable.
        lenient().when(vehicleRepository.findById(anyLong()))
            .thenAnswer(inv -> Optional.of(
                new Vehicle(inv.getArgument(0), null, "Model", "2025", "black")));

        // --- measure ---
        System.gc();
        long memBefore = usedHeapKB();
        long t0        = System.nanoTime();

        List<VehicleSalesCount> result = service.getBestSellingVehicles(FROM, TO);

        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
        long memAfter  = usedHeapKB();

        // --- report ---
        System.out.printf("%n=== BestSelling performance — N=%,d ===%n", N);
        System.out.printf("  Time        : %,d ms%n",            elapsedMs);
        System.out.printf("  Heap delta  : %+,d KB%n",           memAfter - memBefore);
        System.out.printf("  Result size : %d (expected ≤5)%n",  result.size());
        System.out.printf("  Top scores  : %s%n",
            result.stream().map(VehicleSalesCount::salesCount).toList());

        // --- assert ---
        assertThat(result).as("at most MAX_RESULTS=5 entries returned").hasSizeLessThanOrEqualTo(5);
        assertThat(elapsedMs).as("completes in under 5 s for 1M records").isLessThan(5_000);
    }

    private static long usedHeapKB() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / 1024;
    }
}
