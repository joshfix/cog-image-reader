package it.geosolutions.imageioimpl.plugins.tiff;

/**
 * @author joshfix
 * Created on 2019-08-23
 */
public interface CogImageInputStream {

    /**
     *
     * @param ranges
     */
    void readRanges(long[][] ranges);
}
