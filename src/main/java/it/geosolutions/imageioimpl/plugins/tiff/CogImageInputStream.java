package it.geosolutions.imageioimpl.plugins.tiff;

import java.util.Collection;

/**
 * @author joshfix
 * Created on 2019-08-23
 */
public interface CogImageInputStream {

    void readRanges(long[][] ranges);

    void readRanges(Collection<long[]> ranges);

}
