/*
 * GeoTools - The Open Source Java GIS Toolkit
 * http://geotools.org
 *
 * (C) 2017, Open Source Geospatial Foundation (OSGeo)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package it.geosolutions.imageioimpl.plugins.tiff.stream;

import com.google.common.annotations.VisibleForTesting;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.geotools.util.logging.Logging;

import java.util.logging.Logger;

/** Very basic EhCache handling */
public enum CacheManagement {

    DEFAULT;

    public static final String DEFAULT_CACHE = "default_cache";
    private CacheManager manager;
    private CacheConfig config;

    CacheManagement() {
        init(false);
    }

    final @VisibleForTesting void init(final boolean removeCacheIfExists) {
        this.config = CacheConfig.getDefaultConfig();
        this.manager = buildCache(config, removeCacheIfExists);
    }

    private static CacheManager buildCache(CacheConfig config, boolean removeCacheIfExists) {
        CacheManager manager = CacheManagerBuilder
                .newCacheManagerBuilder().build();
        manager.init();

        CacheConfiguration cacheConfiguration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(CacheEntryKey.class, byte[].class, ResourcePoolsBuilder.heap(1000))
                .build();

        manager.createCache(DEFAULT_CACHE, cacheConfiguration);

        /*
        Configuration cacheConfig = null;

        if (cacheConfig == null) {
            cacheConfig = new Configuration();
            cacheConfig.setMaxBytesLocalDisk((long) config.getDiskCacheSize());
            cacheConfig.setMaxBytesLocalHeap((long) config.getHeapSize());

            if (config.isUseDiskCache()) {
                DiskStoreConfiguration diskConfig = new DiskStoreConfiguration();
                diskConfig.setPath(config.getCacheDirectory().toAbsolutePath().toString());
                cacheConfig.diskStore(diskConfig);
            }
        }
        if (cacheConfig.getDefaultCacheConfiguration() == null) {
            CacheConfiguration defaultCacheConfiguration =
                    new CacheConfiguration()
                            .persistence(
                                    new PersistenceConfiguration()
                                            .strategy(
                                                    PersistenceConfiguration.Strategy
                                                            .LOCALTEMPSWAP))
                            .timeToIdleSeconds(config.getTimeToIdle())
                            .timeToLiveSeconds(config.getTimeToLive());

            defaultCacheConfiguration.setMaxBytesLocalDisk((long) config.getDiskCacheSize());
            defaultCacheConfiguration.setMaxBytesLocalHeap((long) config.getHeapSize());

            cacheConfig.defaultCache(defaultCacheConfiguration);
        }

        // Use CacheManager.create() instead of new CacheManager(config) to avoid
        // "Another unnamed cache manager already exists..." exception
        CacheManager manager = CacheManager.create(cacheConfig);
        if (removeCacheIfExists && manager.cacheExists(DEFAULT_CACHE)) {
            manager.removeCache(DEFAULT_CACHE);
            logger().info("Re-creating cache " + DEFAULT_CACHE);
        }
        if (!manager.cacheExists(DEFAULT_CACHE)) {
            manager.addCache(DEFAULT_CACHE);
        }
*/
        return manager;
    }


    /** Get the logger from this method because when needed the class hasn't been loaded yet */
    private static Logger logger() {
        return Logging.getLogger("org.geotools.s3.cache.CacheManagement");
    }

    private Cache<CacheEntryKey, byte[]> getCache() {
        return manager.getCache(DEFAULT_CACHE, CacheEntryKey.class, byte[].class);
    }

    public byte[] getTile(CacheEntryKey key) {
        return getCache().get(key);
    }

    public void cacheTile(CacheEntryKey key, byte[] tileBytes) {
        getCache().put(key, tileBytes);
    }

    public boolean keyExists(CacheEntryKey key) {
        return getCache().containsKey(key);
    }

    public CacheConfig getCacheConfig() {
        return this.config;
    }
}
