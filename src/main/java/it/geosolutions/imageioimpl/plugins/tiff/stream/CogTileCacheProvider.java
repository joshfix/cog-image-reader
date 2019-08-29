package it.geosolutions.imageioimpl.plugins.tiff.stream;

/**
 * @author joshfix
 * Created on 2019-08-29
 */
public interface CogTileCacheProvider {

    byte[] getTile(TileCacheEntryKey key);

    void cacheTile(TileCacheEntryKey key, byte[] tileBytes);

    boolean keyExists(TileCacheEntryKey key);
}
