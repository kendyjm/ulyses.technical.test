package com.septeo.ulyses.technical.test;

import com.septeo.ulyses.technical.test.entity.Brand;
import com.septeo.ulyses.technical.test.entity.Sales;
import com.septeo.ulyses.technical.test.entity.Vehicle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.sql.init.mode=never")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SalesControllerBestSellingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory emf;

    private Long brandId;
    private Long vehicleId1;
    private Long vehicleId2;
    private Long vehicleId3;
    private Long vehicleId4;
    private Long vehicleId5;
    private Long vehicleId6;

    /**
     * Fixture layout:
     *   Vehicle 1: 10 sales on 2025-01-01
     *   Vehicle 2:  8 sales on 2025-01-03
     *   Vehicle 3:  6 sales on 2025-01-05
     *   Vehicle 4:  4 sales on 2025-01-10
     *   Vehicle 5:  2 sales on 2025-01-20  (tie with V6; lower id → ranks first)
     *   Vehicle 6:  2 sales on 2025-01-20  (tie with V5; higher id → ranks second)
     *
     * No-filter top-5:  [V1(10), V2(8), V3(6), V4(4), V5(2)] — V6 excluded (6th place)
     */
    @BeforeAll
    void setUp() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // Clean slate — other test classes share the same H2 context
            em.createQuery("DELETE FROM Sales s").executeUpdate();
            em.flush();
            em.clear();

            Brand brand = new Brand(null, "BestSellingTestBrand", "Test brand for 2-3", new ArrayList<>());
            em.persist(brand);

            Vehicle v1 = new Vehicle(null, brand, "BS_Model_1", "2025", "Red");
            Vehicle v2 = new Vehicle(null, brand, "BS_Model_2", "2025", "Blue");
            Vehicle v3 = new Vehicle(null, brand, "BS_Model_3", "2025", "Green");
            Vehicle v4 = new Vehicle(null, brand, "BS_Model_4", "2025", "White");
            Vehicle v5 = new Vehicle(null, brand, "BS_Model_5", "2025", "Black");
            Vehicle v6 = new Vehicle(null, brand, "BS_Model_6", "2025", "Silver");
            em.persist(v1); em.persist(v2); em.persist(v3);
            em.persist(v4); em.persist(v5); em.persist(v6);

            // V1: 10 sales on Jan 1
            for (int i = 0; i < 10; i++) {
                em.persist(new Sales(null, brand, v1, LocalDate.of(2025, 1, 1), BigDecimal.valueOf(10000)));
            }
            // V2: 8 sales on Jan 3
            for (int i = 0; i < 8; i++) {
                em.persist(new Sales(null, brand, v2, LocalDate.of(2025, 1, 3), BigDecimal.valueOf(20000)));
            }
            // V3: 6 sales on Jan 5
            for (int i = 0; i < 6; i++) {
                em.persist(new Sales(null, brand, v3, LocalDate.of(2025, 1, 5), BigDecimal.valueOf(30000)));
            }
            // V4: 4 sales on Jan 10
            for (int i = 0; i < 4; i++) {
                em.persist(new Sales(null, brand, v4, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(40000)));
            }
            // V5: 2 sales on Jan 20
            for (int i = 0; i < 2; i++) {
                em.persist(new Sales(null, brand, v5, LocalDate.of(2025, 1, 20), BigDecimal.valueOf(50000)));
            }
            // V6: 2 sales on Jan 20 (tie with V5; higher id → ranks after V5)
            for (int i = 0; i < 2; i++) {
                em.persist(new Sales(null, brand, v6, LocalDate.of(2025, 1, 20), BigDecimal.valueOf(60000)));
            }

            tx.commit();

            brandId = brand.getId();
            vehicleId1 = v1.getId();
            vehicleId2 = v2.getId();
            vehicleId3 = v3.getId();
            vehicleId4 = v4.getId();
            vehicleId5 = v5.getId();
            vehicleId6 = v6.getId();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @AfterAll
    void tearDown() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM Sales s WHERE s.brand.id = :bId")
                    .setParameter("bId", brandId).executeUpdate();
            em.flush();
            em.clear();
            Brand b = em.find(Brand.class, brandId);
            if (b != null) em.remove(b); // cascade = ALL → removes vehicles too
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @Test
    void getBestSelling_noFilter_returns5Items() throws Exception {
        mockMvc.perform(get("/api/sales/vehicles/bestSelling"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void getBestSelling_noFilter_firstItemHasHighestCount() throws Exception {
        mockMvc.perform(get("/api/sales/vehicles/bestSelling"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].salesCount").value(10))
                .andExpect(jsonPath("$[0].vehicle.id").value(vehicleId1));
    }

    @Test
    void getBestSelling_noFilter_sortedDescending() throws Exception {
        mockMvc.perform(get("/api/sales/vehicles/bestSelling"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].salesCount").value(10))
                .andExpect(jsonPath("$[1].salesCount").value(8))
                .andExpect(jsonPath("$[2].salesCount").value(6))
                .andExpect(jsonPath("$[3].salesCount").value(4))
                .andExpect(jsonPath("$[4].salesCount").value(2));
    }

    @Test
    void getBestSelling_noFilter_tieBrokenByLowerVehicleId() throws Exception {
        // V5 and V6 both have salesCount=2; V5 has lower id so appears 5th, V6 is excluded
        mockMvc.perform(get("/api/sales/vehicles/bestSelling"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[4].vehicle.id").value(vehicleId5));
    }

    @Test
    void getBestSelling_startDateFilter_excludesEarlierSales() throws Exception {
        // startDate=Jan 2: V1 (Jan 1) excluded → [V2(8), V3(6), V4(4), V5(2), V6(2)]
        mockMvc.perform(get("/api/sales/vehicles/bestSelling").param("startDate", "2025-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].salesCount").value(8))
                .andExpect(jsonPath("$[0].vehicle.id").value(vehicleId2))
                .andExpect(jsonPath("$[3].vehicle.id").value(vehicleId5))  // tie: V5 before V6
                .andExpect(jsonPath("$[4].vehicle.id").value(vehicleId6));
    }

    @Test
    void getBestSelling_endDateFilter_excludesLaterSales() throws Exception {
        // endDate=Jan 5: only V1(10), V2(8), V3(6) have sales on or before Jan 5
        mockMvc.perform(get("/api/sales/vehicles/bestSelling").param("endDate", "2025-01-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].salesCount").value(10))
                .andExpect(jsonPath("$[1].salesCount").value(8))
                .andExpect(jsonPath("$[2].salesCount").value(6));
    }

    @Test
    void getBestSelling_bothDateParams_appliesBothBounds() throws Exception {
        // startDate=Jan 10, endDate=Jan 20: V4(4), V5(2), V6(2) only
        mockMvc.perform(get("/api/sales/vehicles/bestSelling")
                        .param("startDate", "2025-01-10")
                        .param("endDate", "2025-01-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].vehicle.id").value(vehicleId4))
                .andExpect(jsonPath("$[0].salesCount").value(4));
    }

    @Test
    void getBestSelling_narrowRange_fewerThan5() throws Exception {
        // startDate=Jan 20: only V5(2) and V6(2) — tie-break: V5 first
        mockMvc.perform(get("/api/sales/vehicles/bestSelling").param("startDate", "2025-01-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].vehicle.id").value(vehicleId5))
                .andExpect(jsonPath("$[1].vehicle.id").value(vehicleId6));
    }
}
