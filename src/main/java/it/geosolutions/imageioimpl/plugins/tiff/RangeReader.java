package it.geosolutions.imageioimpl.plugins.tiff;

import java.util.Collection;

/**
 * @author joshfix
 * Created on 2019-08-21
 */
public interface RangeReader {

    byte[] getBytes();

    int getFileSize();

    void readAsync(long[]... ranges);

    void readAsync(Collection<long[]> ranges);

}
