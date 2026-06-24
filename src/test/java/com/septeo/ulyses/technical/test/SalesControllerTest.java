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
class SalesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory emf;

    private Long brandId;
    private Long vehicleId;
    private Long emptyBrandId;
    private Long emptyVehicleId;

    @BeforeAll
    void setUp() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Brand brand = new Brand(null, "SalesTestBrand", "Desc", new ArrayList<>());
            em.persist(brand);
            Vehicle vehicle = new Vehicle(null, brand, "TestModel", "2024", "Blue");
            em.persist(vehicle);
            Sales sale = new Sales(null, brand, vehicle, LocalDate.of(2025, 1, 1), BigDecimal.valueOf(25000));
            em.persist(sale);
            Brand emptyBrand = new Brand(null, "SalesTestEmptyBrand", "No sales", new ArrayList<>());
            em.persist(emptyBrand);
            Vehicle emptyVehicle = new Vehicle(null, emptyBrand, "EmptyModel", "2024", "Red");
            em.persist(emptyVehicle);
            tx.commit();
            brandId = brand.getId();
            vehicleId = vehicle.getId();
            emptyBrandId = emptyBrand.getId();
            emptyVehicleId = emptyVehicle.getId();
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
            // Delete sales first (FK constraint: sales.brand_id and sales.vehicle_id)
            em.createQuery("DELETE FROM Sales s WHERE s.brand.id = :bId")
              .setParameter("bId", brandId)
              .executeUpdate();
            em.flush();
            em.clear();
            // Remove brand with vehicle (cascade = ALL removes vehicle automatically)
            Brand b = em.find(Brand.class, brandId);
            if (b != null) em.remove(b);
            // Remove empty brand with its (sales-less) vehicle (cascade = ALL removes vehicle automatically)
            Brand eb = em.find(Brand.class, emptyBrandId);
            if (eb != null) em.remove(eb);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @Test
    void getSalesByBrandId_brandNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/sales/brands/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSalesByBrandId_brandExists_returnsSales() throws Exception {
        mockMvc.perform(get("/api/sales/brands/" + brandId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getSalesByBrandId_brandExistsNoSales_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/sales/brands/" + emptyBrandId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getSalesByVehicleId_vehicleNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/sales/vehicles/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSalesByVehicleId_vehicleExists_returnsSales() throws Exception {
        mockMvc.perform(get("/api/sales/vehicles/" + vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getSalesByVehicleId_vehicleExistsNoSales_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/sales/vehicles/" + emptyVehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
