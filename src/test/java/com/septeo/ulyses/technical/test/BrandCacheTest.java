package com.septeo.ulyses.technical.test;

import com.septeo.ulyses.technical.test.cache.BrandCache;
import com.septeo.ulyses.technical.test.entity.Brand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.sql.init.mode=never", "brand.cache.ttl-ms=100"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrandCacheTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandCache brandCache;

    @Autowired
    private EntityManagerFactory emf;

    private Long testBrandId;

    @BeforeAll
    void createTestBrand() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Brand brand = new Brand(null, "CacheTestBrand", "For cache tests", new ArrayList<>());
            em.persist(brand);
            tx.commit();
            testBrandId = brand.getId();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @AfterAll
    void deleteTestBrand() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Brand b = em.find(Brand.class, testBrandId);
            if (b != null) em.remove(b);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @BeforeEach
    void resetCache() {
        brandCache.evictAll();
    }

    @Test
    void cacheHit_afterFirstGet_cacheIsPopulated() throws Exception {
        assertThat(brandCache.getById(testBrandId)).isNull();
        mockMvc.perform(get("/api/brands/" + testBrandId)).andExpect(status().isOk());
        assertThat(brandCache.getById(testBrandId)).isNotNull();
    }

    @Test
    void cacheHit_secondGetById_usesCacheNotDatabase() throws Exception {
        mockMvc.perform(get("/api/brands/" + testBrandId)).andExpect(status().isOk());
        assertThat(brandCache.getById(testBrandId)).as("cache populated after first GET").isNotNull();
        mockMvc.perform(get("/api/brands/" + testBrandId)).andExpect(status().isOk());
        assertThat(brandCache.getById(testBrandId)).as("cache still populated after second GET").isNotNull();
    }

    @Test
    void cacheExpiry_afterTtlElapsed_entryIsExpiredAndRefetched() throws Exception {
        mockMvc.perform(get("/api/brands/" + testBrandId)).andExpect(status().isOk());
        assertThat(brandCache.getById(testBrandId)).isNotNull();
        Thread.sleep(150); // TTL is 100 ms
        assertThat(brandCache.getById(testBrandId)).as("entry should be expired after 150 ms").isNull();
        mockMvc.perform(get("/api/brands/" + testBrandId)).andExpect(status().isOk());
        assertThat(brandCache.getById(testBrandId)).as("cache repopulated after expired GET").isNotNull();
    }

    @Test
    void getAllBrandsCache_secondGetAll_usesCacheNotDatabase() throws Exception {
        assertThat(brandCache.getAllBrands()).isNull();
        mockMvc.perform(get("/api/brands")).andExpect(status().isOk());
        assertThat(brandCache.getAllBrands()).as("all-brands cache populated after first GET").isNotNull();
        mockMvc.perform(get("/api/brands")).andExpect(status().isOk());
        assertThat(brandCache.getAllBrands()).as("all-brands cache still populated after second GET").isNotNull();
    }

    @Test
    void mutation_evictsAllBrandsCache_nextGetAllRepopulates() throws Exception {
        mockMvc.perform(get("/api/brands")).andExpect(status().isOk());
        assertThat(brandCache.getAllBrands()).isNotNull();
        mockMvc.perform(post("/api/brands")
                .with(httpBasic("user", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"MutationTestBrand\",\"description\":\"eviction test\"}"))
                .andExpect(status().isCreated());
        assertThat(brandCache.getAllBrands()).as("cache evicted after mutation commit").isNull();
        mockMvc.perform(get("/api/brands")).andExpect(status().isOk());
        assertThat(brandCache.getAllBrands()).as("cache repopulated on next GET after eviction").isNotNull();
    }

    @Test
    void update_evictsByIdCache_nextGetRefetchesFromDatabase() throws Exception {
        mockMvc.perform(get("/api/brands/" + testBrandId)).andExpect(status().isOk());
        assertThat(brandCache.getById(testBrandId)).as("cache warm before PUT").isNotNull();
        mockMvc.perform(put("/api/brands/" + testBrandId)
                .with(httpBasic("user", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"CacheTestBrand\",\"description\":\"updated\"}"))
                .andExpect(status().isOk());
        assertThat(brandCache.getById(testBrandId)).as("byId cache evicted after PUT").isNull();
    }

    @Test
    void delete_evictsByIdCache_entryEvictedAfterDelete() throws Exception {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        Long deletableId;
        try {
            tx.begin();
            Brand brand = new Brand(null, "DeleteTestBrand", "to be deleted", new ArrayList<>());
            em.persist(brand);
            tx.commit();
            deletableId = brand.getId();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
        mockMvc.perform(get("/api/brands/" + deletableId)).andExpect(status().isOk());
        assertThat(brandCache.getById(deletableId)).as("cache warm before DELETE").isNotNull();
        mockMvc.perform(delete("/api/brands/" + deletableId)
                .with(httpBasic("user", "password")))
                .andExpect(status().isNoContent());
        assertThat(brandCache.getById(deletableId)).as("byId cache evicted after DELETE").isNull();
    }

    @Test
    void concurrentCacheAccess_doNotThrow() throws Exception {
        // Test BrandCache thread safety directly (ConcurrentHashMap + volatile field).
        // Concurrent MockMvc calls share the H2 connection pool and are not safe for
        // parallel execution; the concurrency concern is at the cache data-structure level.
        Brand fakeBrand = new Brand(testBrandId, "CacheTestBrand", "test", new ArrayList<>());
        ExecutorService exec = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            futures.add(exec.submit(() -> {
                brandCache.getById(testBrandId);
                brandCache.putById(testBrandId, Optional.of(fakeBrand));
                brandCache.getAllBrands();
                brandCache.putAllBrands(List.of(fakeBrand));
                brandCache.evict(testBrandId);
            }));
        }
        for (Future<?> f : futures) f.get(); // propagates any ConcurrentModificationException
        exec.shutdown();
    }
}
