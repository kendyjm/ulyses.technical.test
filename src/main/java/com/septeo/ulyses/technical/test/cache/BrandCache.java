package com.septeo.ulyses.technical.test.cache;

import com.septeo.ulyses.technical.test.entity.Brand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manual in-memory cache for Brand entities.
 * Uses ConcurrentHashMap for per-brand entries and an AtomicReference for the all-brands list.
 * TTL is configurable via {@code brand.cache.ttl-ms} (default 60 s).
 * Cache writes are intended to be registered via afterCommit() in TransactionSynchronizationManager
 * to prevent cache poisoning on transaction rollback.
 */
@Component
public class BrandCache {

    @Value("${brand.cache.ttl-ms:60000}")
    private long ttlMs;

    private final ConcurrentHashMap<Long, CacheEntry<Optional<Brand>>> byIdCache = new ConcurrentHashMap<>();
    private final AtomicReference<CacheEntry<List<Brand>>> allBrandsRef = new AtomicReference<>();

    private static class CacheEntry<T> {
        final T value;
        final long expiresAt;

        CacheEntry(T value, long ttlMs) {
            this.value = value;
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }

    /**
     * Returns null on cache miss or expiry; returns the cached Optional on hit.
     * The inner Optional may be empty if a "not found" result was cached.
     */
    @Nullable
    public Optional<Brand> getById(Long id) {
        CacheEntry<Optional<Brand>> entry = byIdCache.get(id);
        if (entry == null || entry.isExpired()) {
            byIdCache.remove(id);
            return null;
        }
        return entry.value;
    }

    /**
     * Stores an Optional<Brand> for the given id.
     */
    public void putById(Long id, Optional<Brand> brand) {
        byIdCache.put(id, new CacheEntry<>(brand, ttlMs));
    }

    /**
     * Returns null on cache miss or expiry; returns the cached list on hit.
     * CAS ensures a lagging reader cannot null out a fresh entry written by a concurrent thread.
     */
    @Nullable
    public List<Brand> getAllBrands() {
        CacheEntry<List<Brand>> entry = allBrandsRef.get();
        if (entry == null || entry.isExpired()) {
            allBrandsRef.compareAndSet(entry, null);
            return null;
        }
        return entry.value;
    }

    /**
     * Stores the full list of brands.
     */
    public void putAllBrands(List<Brand> brands) {
        allBrandsRef.set(new CacheEntry<>(brands, ttlMs));
    }

    /**
     * Evicts a specific brand by id AND invalidates the all-brands list.
     * Called after commit on save or delete operations.
     */
    public void evict(Long id) {
        byIdCache.remove(id);
        allBrandsRef.set(null);
    }

    /**
     * Clears all cache entries. Used in tests to reset cache state between test cases.
     */
    public void evictAll() {
        byIdCache.clear();
        allBrandsRef.set(null);
    }
}
