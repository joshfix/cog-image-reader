package it.geosolutions.imageioimpl.plugins.tiff.stream;

import it.geosolutions.imageioimpl.plugins.tiff.CogTileInfo;
import it.geosolutions.imageioimpl.plugins.tiff.HttpRangeReader;
import it.geosolutions.imageioimpl.plugins.tiff.RangeBuilder;

import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

/**
 * @author joshfix
 * Created on 2019-08-28
 */
public class CachingHttpCogImageInputStream extends ImageInputStreamImpl implements CogImageInputStream {

    protected long length;
    protected URI uri;
    protected HttpRangeReader rangeReader;
    protected CogTileInfo cogTileInfo;
    protected int headerSize = 16384;
    private final static int HEADER_TILE_INDEX = -100;

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
        CacheEntryKey headerKey = new CacheEntryKey(uri.toString(), HEADER_TILE_INDEX);
        if (!CacheManagement.DEFAULT.keyExists(headerKey)) {
            CacheManagement.DEFAULT.cacheTile(headerKey, rangeReader.readHeader());
            cogTileInfo.addTileRange(HEADER_TILE_INDEX, 0, headerSize);
        } else {
            byte[] headerBytes = CacheManagement.DEFAULT.getTile(headerKey);
            cogTileInfo.addTileRange(HEADER_TILE_INDEX, 0, headerBytes.length);
        }


        this.length = rangeReader.getFileSize();
    }

    public CogTileInfo getCogTileInfo() {
        return cogTileInfo;
    }

    @Override
    public void readRanges(CogTileInfo cogTileInfo) {
        this.cogTileInfo = cogTileInfo;

        long firstTileOffset = cogTileInfo.getFirstTileOffset();
        CogTileInfo.TileRange firstTileRange = cogTileInfo.getTileRange(firstTileOffset);

        RangeBuilder rangeBuilder = new RangeBuilder(firstTileOffset, firstTileOffset + firstTileRange.getByteLength());

        cogTileInfo.getTileRanges().forEach((tileIndex, tileRange) -> {
            CacheEntryKey key = new CacheEntryKey(uri.toString(), tileIndex);
            if (!CacheManagement.DEFAULT.keyExists(key)) {
                rangeBuilder.compare(tileRange.getStart(), tileRange.getByteLength());
            }
        });

        cogTileInfo.setContiguousRanges(rangeBuilder.getRanges());
        // read data with the RangeReader and set the byte order and pointer on the new input stream
        long[][] ranges = rangeBuilder.getRanges().toArray(new long[][]{});
        if (ranges.length == 1) {
            return;
        }

        System.out.println("Submitting " + ranges.length + " range request(s)");
        rangeReader.readAsync(ranges);

        byte[] cogBytes = rangeReader.getBytes();

        cogTileInfo.getTileRanges().forEach((tileIndex, tileRange) -> {
            CacheEntryKey key = new CacheEntryKey(uri.toString(), tileIndex);
            if (!CacheManagement.DEFAULT.keyExists(key)) {
                byte[] tileBytes = Arrays.copyOfRange(cogBytes, (int) tileRange.getStart(), (int) (tileRange.getEnd()));
                CacheManagement.DEFAULT.cacheTile(key, tileBytes);
            }
        });

    }

    @Override
    public int read() throws IOException {
        // TODO: implement
        //byte rawValue = readRawValue();
        //return Byte.toUnsignedInt(rawValue);
        System.out.println("READ");
        return 0;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int readRemaining = len;
        CogTileInfo.TileRange tileRange = cogTileInfo.getTileRange(streamPos);
        CacheEntryKey key = new CacheEntryKey(uri.toString(), getTileIndex());
        byte[] tileBytes = CacheManagement.DEFAULT.getTile(key);

        int relativeStreamPos = (int) (streamPos - tileRange.getStart() + off);
        byte[] requestedBytes = Arrays.copyOfRange(tileBytes, relativeStreamPos, relativeStreamPos + len);

        for (long i = 0; i < len; i++) {
            b[(int)i] = requestedBytes[(int)i];
        }
        streamPos += len;
        return len;
    }

    private int getTileIndex() {
        return cogTileInfo.getTileIndex(streamPos);
    }

}
