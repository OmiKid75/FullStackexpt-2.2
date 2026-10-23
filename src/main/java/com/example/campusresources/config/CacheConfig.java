package com.example.campusresources.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Switches on Spring's cache proxies. The cache provider (Ehcache via JCache) and its settings
 * come from application.properties (spring.cache.*) and ehcache.xml.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
