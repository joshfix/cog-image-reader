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
public enum CacheManagement implements CogTileCacheProvider {

    DEFAULT;

    public static final String TILE_CACHE = "tile_cache";
    public static final String HEADER_CACHE = "header_cache";
    public static final String FILESIZE_CACHE = "filesize_cache";
    private CacheManager manager;
    private CacheConfig config;

    CacheManagement() {
        init(false);
    }

    final @VisibleForTesting void init(final boolean removeCacheIfExists) {
        config = CacheConfig.getDefaultConfig();
        manager = buildCache(config, removeCacheIfExists);
    }

    /**
     * Builds caches for tiles, headers, and filesizes.
     *
     * @param config
     * @param removeCacheIfExists
     * @return
     */
    private static CacheManager buildCache(CacheConfig config, boolean removeCacheIfExists) {
        CacheManager manager = CacheManagerBuilder
                .newCacheManagerBuilder().build();
        manager.init();

        CacheConfiguration cacheConfiguration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(TileCacheEntryKey.class, byte[].class, ResourcePoolsBuilder.heap(1000))
                .build();

        manager.createCache(TILE_CACHE, cacheConfiguration);

        manager.createCache(HEADER_CACHE, CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, byte[].class, ResourcePoolsBuilder.heap(1000))
                .build());

        manager.createCache(FILESIZE_CACHE, CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, Integer.class, ResourcePoolsBuilder.heap(1000))
                .build());

        return manager;
    }


    /** Get the logger from this method because when needed the class hasn't been loaded yet */
    private static Logger logger() {
        return Logging.getLogger("org.geotools.s3.cache.CacheManagement");
    }

    private Cache<TileCacheEntryKey, byte[]> getTileCache() {
        return manager.getCache(TILE_CACHE, TileCacheEntryKey.class, byte[].class);
    }

    private Cache<String, byte[]> getHeaderCache() {
        return manager.getCache(HEADER_CACHE, String.class, byte[].class);
    }

    private Cache<String, Integer> getFilesizeCache() {
        return manager.getCache(FILESIZE_CACHE, String.class, Integer.class);
    }

    @Override
    public byte[] getTile(TileCacheEntryKey key) {
        return getTileCache().get(key);
    }

    @Override
    public void cacheTile(TileCacheEntryKey key, byte[] tileBytes) {
        getTileCache().put(key, tileBytes);
    }

    @Override
    public boolean keyExists(TileCacheEntryKey key) {
        return getTileCache().containsKey(key);
    }

    @Override
    public void cacheHeader(String key, byte[] headerBytes) {
        getHeaderCache().put(key, headerBytes);
    }

    @Override
    public byte[] getHeader(String key) {
        return getHeaderCache().get(key);
    }

    @Override
    public boolean headerExists(String key) {
        return getHeaderCache().containsKey(key);
    }

    @Override
    public void cacheFilesize(String key, int size) {
        getFilesizeCache().put(key, size);
    }

    @Override
    public int getFilesize(String key) {
        return getFilesizeCache().get(key);
    }

    @Override
    public boolean filesizeExists(String key) {
        return getFilesizeCache().containsKey(key);
    }

    public CacheConfig getCacheConfig() {
        return this.config;
    }
}
