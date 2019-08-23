package it.geosolutions.imageioimpl.plugins.tiff;

import java.nio.ByteBuffer;

/**
 * @author joshfix
 * Created on 2019-08-21
 */
public interface RangeReader {

    int getFileSize(String url);

    byte[] read(String url, long start, long end);

    void read(ByteBuffer byteBuffer, String url, long start, long end);

    void readAsync(ByteBuffer byteBuffer, String url, long[]... range);

}
