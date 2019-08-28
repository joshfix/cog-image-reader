package it.geosolutions.imageioimpl.plugins.tiff.stream;

import it.geosolutions.imageioimpl.plugins.tiff.CogTileInfo;

/**
 * @author joshfix
 * Created on 2019-08-23
 */
public interface CogImageInputStream {

    void readRanges(CogTileInfo cogTileInfo);
    CogTileInfo getCogTileInfo();

}
