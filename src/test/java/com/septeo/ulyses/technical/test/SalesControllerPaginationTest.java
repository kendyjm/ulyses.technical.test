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
class SalesControllerPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory emf;

    private Long brandId;

    @BeforeAll
    void setUp() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Ensure a clean slate regardless of test class execution order
            em.createQuery("DELETE FROM Sales s").executeUpdate();
            em.flush();
            em.clear();
            Brand brand = new Brand(null, "PaginationTestBrand", "Desc", new ArrayList<>());
            em.persist(brand);
            Vehicle vehicle = new Vehicle(null, brand, "PaginationModel", "2024", "Black");
            em.persist(vehicle);
            for (int i = 0; i < 25; i++) {
                em.persist(new Sales(null, brand, vehicle,
                        LocalDate.of(2025, 1, 1), BigDecimal.valueOf(10000 + i)));
            }
            tx.commit();
            brandId = brand.getId();
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
              .setParameter("bId", brandId)
              .executeUpdate();
            em.flush();
            em.clear();
            Brand b = em.find(Brand.class, brandId);
            if (b != null) em.remove(b);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @Test
    void getSales_noPageParam_returnsFirstTen() throws Exception {
        // Fixture persists prices 10000..10024 in id-ascending order; asserting the
        // boundary prices verifies ORDER BY s.id ASC, not just the record count.
        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].price").value(10000))
                .andExpect(jsonPath("$[9].price").value(10009));
    }

    @Test
    void getSales_pageZero_returnsFirstTen() throws Exception {
        mockMvc.perform(get("/api/sales").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].price").value(10000))
                .andExpect(jsonPath("$[9].price").value(10009));
    }

    @Test
    void getSales_pageOne_returnsTen() throws Exception {
        // Page 1 must begin exactly one page-size offset after page 0 (prices 10010..10019),
        // proving setFirstResult(page * PAGE_SIZE) offset math and a disjoint, ordered slice.
        mockMvc.perform(get("/api/sales").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].price").value(10010))
                .andExpect(jsonPath("$[9].price").value(10019));
    }

    @Test
    void getSales_pageTwo_returnsFiveRemaining() throws Exception {
        mockMvc.perform(get("/api/sales").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].price").value(10020))
                .andExpect(jsonPath("$[4].price").value(10024));
    }

    @Test
    void getSales_outOfRangePage_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/sales").param("page", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getSales_negativePageParam_returns400() throws Exception {
        mockMvc.perform(get("/api/sales").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSales_nonNumericPageParam_returns400() throws Exception {
        mockMvc.perform(get("/api/sales").param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSales_emptyPageParam_defaultsToPageZero() throws Exception {
        // An empty value falls back to defaultValue "0" (Spring applies the default for an
        // empty param), so it is treated as page 0 rather than erroring.
        mockMvc.perform(get("/api/sales").param("page", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].price").value(10000));
    }

    @Test
    void getSales_veryLargePageOverflow_returnsEmptyArray() throws Exception {
        // page * 10 overflows int for Integer.MAX_VALUE; the offset is computed in long
        // and treated as out-of-range, returning an empty 200 rather than a 500.
        mockMvc.perform(get("/api/sales").param("page", String.valueOf(Integer.MAX_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
