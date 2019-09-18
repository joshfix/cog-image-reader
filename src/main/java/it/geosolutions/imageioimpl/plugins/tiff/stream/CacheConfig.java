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

import java.nio.file.InvalidPathException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic configuration properties for the S3 caching
 */
public class CacheConfig {

    private static final Logger LOGGER = Logger.getLogger(CacheConfig.class.getName());

    // whether disk caching should be disabled
    public static final String COG_CACHING_USE_DISK = "cog.caching.useDisk";

    // whether off heap should be used. currently not supported
    public static final String COG_CACHING_USE_OFF_HEAP = "cog.caching.useOffHeap";

    // the disk cache size.
    public static final String COG_CACHING_DISK_CACHE_SIZE = "cog.caching.diskCacheSize";

    // path for the disk cache
    public static final String COG_CACHING_DISK_PATH = "cog.caching.diskPath";

    // alternatively an EhCache 2.x XML config can be used to override all cache config
    public static final String COG_CACHING_EH_CACHE_CONFIG = "cog.caching.ehCacheConfig";

    // in heap cache size in bytes
    public static final String COG_CACHING_HEAP_SIZE = "cog.caching.heapSize";

    // time to idle in seconds
    public static final String COG_CACHING_TIME_TO_IDLE = "cog.caching.timeToIdle";

    // time to live in seconds
    public static final String COG_CACHING_TIME_TO_LIVE = "cog.caching.timeToLive";

    public static final int MEBIBYTE_IN_BYTES = 1048576;

    private static boolean useDiskCache;
    private static boolean useOffHeapCache;
    private static int diskCacheSize;
    private static int heapSize;
    private static String cacheDirectory;
    private static long timeToIdle;
    private static long timeToLive;

    public CacheConfig() {
        useDiskCache = Boolean.getBoolean(getPropertyValue(COG_CACHING_USE_DISK, "false"));
        useOffHeapCache = Boolean.getBoolean(getPropertyValue(COG_CACHING_USE_OFF_HEAP, "false"));
        diskCacheSize = Integer.parseInt(getPropertyValue(COG_CACHING_DISK_CACHE_SIZE, Integer.toString(500 * MEBIBYTE_IN_BYTES)));
        heapSize = Integer.parseInt(getPropertyValue(COG_CACHING_HEAP_SIZE, Integer.toString(50 * MEBIBYTE_IN_BYTES)));
        cacheDirectory = getPropertyValue(COG_CACHING_DISK_PATH, null);
        timeToIdle = Integer.parseInt(getPropertyValue(COG_CACHING_TIME_TO_IDLE, "0"));
        timeToLive = Integer.parseInt(getPropertyValue(COG_CACHING_TIME_TO_LIVE, "0"));
    }

    public static CacheConfig getDefaultConfig() {
        CacheConfig config = new CacheConfig();

        if (useDiskCache) {
            config.setUseDiskCache(false);
        }

        if (useOffHeapCache) {
            config.setUseOffHeapCache(true);
        }

        if (heapSize > 0) {
            config.setHeapSize(heapSize);
        }

        if (diskCacheSize > 0) {
            config.setDiskCacheSize(diskCacheSize);
        }

        if (cacheDirectory != null) {
            try {
                config.setCacheDirectory(cacheDirectory);
            } catch (InvalidPathException e) {
                LOGGER.log(Level.FINER, "Can't parse disk cache path", e);
            }
        } else {
            if (config.isUseDiskCache()) {
                config.setCacheDirectory("cogCachine");
            }
        }

        if (timeToIdle > 0) {
            config.setTimeToIdle(timeToIdle);
        }

        if (timeToLive > 0) {
            config.setTimeToLive(timeToLive);
        }

        return config;
    }

    public static String getPropertyValue(String key, String defaultValue) {
        String environmentKey = key.toUpperCase().replace(".", "_");
        String value = System.getenv(environmentKey);
        if (null != value) {
            return value;
        }
        value = System.getProperty(key);
        if (null != value) {
            return value;
        }

        return defaultValue;
    }

    public boolean isUseDiskCache() {
        return useDiskCache;
    }

    public void setUseDiskCache(boolean useDiskCache) {
        this.useDiskCache = useDiskCache;
    }

    public boolean isUseOffHeapCache() {
        return useOffHeapCache;
    }

    public void setUseOffHeapCache(boolean useOffHeapCache) {
        this.useOffHeapCache = useOffHeapCache;
    }

    public int getDiskCacheSize() {
        return diskCacheSize;
    }

    public void setDiskCacheSize(int diskCacheSize) {
        this.diskCacheSize = diskCacheSize;
    }

    public String getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public int getHeapSize() {
        return heapSize;
    }

    public void setHeapSize(int heapSize) {
        this.heapSize = heapSize;
    }

    public long getTimeToIdle() {
        return timeToIdle;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToIdle(long timeToIdle) {
        this.timeToIdle = timeToIdle;
    }

    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }
}
