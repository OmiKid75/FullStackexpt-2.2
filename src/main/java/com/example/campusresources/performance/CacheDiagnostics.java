package com.example.campusresources.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Helps to SEE what the cache is doing:
 *  - a counter of real database loads for GET /api/resources/{id} (only increases on a cache MISS)
 *  - Ehcache hit/miss statistics read from JMX
 *  - a way to clear all caches (for a "cold cache" benchmark)
 */
@Component
public class CacheDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(CacheDiagnostics.class);

    private final CacheManager cacheManager;
    private final AtomicLong resourceDbLoads = new AtomicLong();

    public CacheDiagnostics(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void recordResourceDbLoad() {
        resourceDbLoads.incrementAndGet();
    }

    public long getResourceDbLoads() {
        return resourceDbLoads.get();
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cacheNames", cacheManager.getCacheNames());
        result.put("resourceByIdDatabaseLoads", resourceDbLoads.get());
        result.put("ehcacheStatistics", readJmxStatistics());
        result.put("note", "resourceByIdDatabaseLoads only increases when GET /api/resources/{id} is a cache MISS.");
        return result;
    }

    public void clearAll() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
        log.info("All caches cleared");
    }

    private List<Map<String, Object>> readJmxStatistics() {
        List<Map<String, Object>> stats = new ArrayList<>();
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> names = server.queryNames(new ObjectName("javax.cache:type=CacheStatistics,*"), null);
            for (ObjectName name : names) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("cache", name.getKeyProperty("Cache"));
                row.put("hits", server.getAttribute(name, "CacheHits"));
                row.put("misses", server.getAttribute(name, "CacheMisses"));
                row.put("puts", server.getAttribute(name, "CachePuts"));
                row.put("evictions", server.getAttribute(name, "CacheEvictions"));
                stats.add(row);
            }
            stats.sort(Comparator.comparing(m -> String.valueOf(m.get("cache"))));
        } catch (Exception e) {
            log.debug("Could not read cache statistics from JMX: {}", e.getMessage());
        }
        return stats;
    }
}
