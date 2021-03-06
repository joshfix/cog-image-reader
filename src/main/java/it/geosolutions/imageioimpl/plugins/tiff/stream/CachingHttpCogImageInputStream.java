package it.geosolutions.imageioimpl.plugins.tiff.stream;

import it.geosolutions.imageioimpl.plugins.tiff.CogTileInfo;
import it.geosolutions.imageioimpl.plugins.tiff.HttpRangeReader;
import it.geosolutions.imageioimpl.plugins.tiff.RangeBuilder;
import it.geosolutions.imageioimpl.plugins.tiff.RangeReader;

import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author joshfix
 * Created on 2019-08-28
 */
public class CachingHttpCogImageInputStream extends ImageInputStreamImpl implements CogImageInputStream {

    protected int headerByteLength = 16384;

    protected URI uri;
    protected RangeReader rangeReader;
    protected CogTileInfo cogTileInfo;
    private static final int HEADER_TILE_INDEX = -100;
    private final static Logger LOGGER = Logger.getLogger(CachingHttpCogImageInputStream.class.getName());

    public CachingHttpCogImageInputStream(String url) {
        this(URI.create(url));
    }

    public CachingHttpCogImageInputStream(URL url) {
        this(URI.create(url.toString()));
    }

    public CachingHttpCogImageInputStream(URI uri) {
        this.uri = uri;

        cogTileInfo = new CogTileInfo();
        rangeReader = new HttpRangeReader(uri);

        // determine if the header has already been cached
        if (!CacheManagement.DEFAULT.headerExists(uri.toString())) {
            CacheManagement.DEFAULT.cacheHeader(uri.toString(), rangeReader.readHeader(headerByteLength));
            CacheManagement.DEFAULT.cacheFilesize(uri.toString(), rangeReader.getFilesize());
            cogTileInfo.addTileRange(HEADER_TILE_INDEX, 0, headerByteLength);
        } else {
            byte[] headerBytes = CacheManagement.DEFAULT.getHeader(uri.toString());
            headerByteLength = headerBytes.length;
            rangeReader.setFilesize(CacheManagement.DEFAULT.getFilesize(uri.toString()));
            cogTileInfo.addTileRange(HEADER_TILE_INDEX, 0, headerByteLength);
        }
    }

    public CogTileInfo getCogTileInfo() {
        return cogTileInfo;
    }

    @Override
    public void setHeaderByteLength(int headerByteLength) {
        this.headerByteLength = headerByteLength;
    }

    /**
     * TIFFImageReader will read and decode the requested region of the GeoTIFF tile by tile.  Because of this, we will
     * not arbitrarily store fixed-length byte chunks in cache, but instead create a cache entry for all the bytes for
     * each tile.
     * <p>
     * The first step is to loop through the tile ranges from CogTileInfo and determine which tiles are already cached.
     * Tile ranges that are not in cache are submitted to RangeBuilder to build contiguous ranges to be read via HTTP.
     * <p>
     * Once the contiguous ranges have been read, we obtain the full image-length byte array from the RangeReader.  Then
     * loop through each of the requested tile ranges from CogTileInfo and cache the bytes.
     * <p>
     * There are likely lots of optimizations to be made in here.
     */
    @Override
    public void readRanges() {
        long firstTileOffset = cogTileInfo.getFirstTileOffset();
        long firstTileByteLength = cogTileInfo.getFirstTileByteLength();

        // TODO: is this worth it?  or should we just leave the header alone?
        if (firstTileOffset < headerByteLength) {
            byte[] headerBytes = CacheManagement.DEFAULT.getHeader(uri.toString());
            byte[] newHeaderBytes = Arrays.copyOf(headerBytes, (int) (firstTileOffset - 1));
            CacheManagement.DEFAULT.cacheHeader(uri.toString(), newHeaderBytes);
        }

        // instantiate the range builder
        RangeBuilder rangeBuilder = new RangeBuilder(firstTileOffset, firstTileOffset + firstTileByteLength);

        // determine which requested tiles are not in cache and build the required ranges that need to be read (if any)
        cogTileInfo.getTileRanges().forEach((tileIndex, tileRange) -> {
            TileCacheEntryKey key = new TileCacheEntryKey(uri.toString(), tileIndex);
            if (!CacheManagement.DEFAULT.keyExists(key)) {
                rangeBuilder.addTileRange(tileRange.getStart(), tileRange.getByteLength());
            }
        });

        // get the ranges for the tiles that are not already cached.  if there are none, simply return
        List<long[]> ranges = rangeBuilder.getRanges();
        if (ranges.size() == 0) {
            return;
        }

        // read all they byte ranges for tiles that are not in cache
        LOGGER.fine("Submitting " + ranges.size() + " range request(s)");
        rangeReader.readAsync(ranges);

        // cache the bytes for each tile
        cogTileInfo.getTileRanges().forEach((tileIndex, tileRange) -> {
            TileCacheEntryKey key = new TileCacheEntryKey(uri.toString(), tileIndex);
            byte[] tileBytes =
                    Arrays.copyOfRange(rangeReader.getBytes(), (int) tileRange.getStart(), (int) (tileRange.getEnd()));
            CacheManagement.DEFAULT.cacheTile(key, tileBytes);
        });

    }

    @Override
    public int read() throws IOException {
        // TODO: implement, even though this never seems to get called by TIFFImageReader
        return 0;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        // based on the stream position, determine which tile we are in and fetch the corresponding TileRange
        CogTileInfo.TileRange tileRange = cogTileInfo.getTileRange(streamPos);

        // get the bytes from cache for the tile. need to determine if we're reading from the header or a tile.
        byte[] bytes;
        switch (tileRange.getIndex()) {
            case HEADER_TILE_INDEX:
                bytes = CacheManagement.DEFAULT.getHeader(uri.toString());
                break;
            default:
                TileCacheEntryKey key = new TileCacheEntryKey(uri.toString(), tileRange.getIndex());
                bytes = CacheManagement.DEFAULT.getTile(key);
        }

        // translate the overall stream position to the stream position of the fetched tile
        int relativeStreamPos = (int) (streamPos - tileRange.getStart() + off);

        // copy the bytes from the fetched tile into the destination byte array
        for (long i = 0; i < len; i++) {
            try {
                b[(int) i] = bytes[(int) (relativeStreamPos + i)];
            } catch (Exception e) {
                LOGGER.fine("Error copying bytes. requested offset: " + off
                        + " - requested length: " + len
                        + " - relativeStreamPos: " + relativeStreamPos
                        + " - streamPos: " + streamPos
                        + " - tile range start: " + tileRange.getStart()
                        + " - tile range length: " + tileRange.getByteLength());
            }
        }

        streamPos += len;
        return len;
    }

}
