package it.geosolutions.imageioimpl.plugins.tiff;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFField;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author joshfix
 * Created on 2019-08-22
 */
public class CogImageReader extends TIFFImageReader {

    public CogImageReader(ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        // TODO: prepareRead method in TIFFIMageReader should be protected, not private?
        try {
            Method prepareRead = TIFFImageReader.class.getDeclaredMethod("prepareRead", int.class, ImageReadParam.class);
            prepareRead.setAccessible(true);
            prepareRead.invoke(this, imageIndex, param);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // prepare for reading
        this.theImage = getDestination(param, getImageTypes(imageIndex), width, height, noData);

        // This could probably be made more efficient...
        Rectangle srcRegion = new Rectangle(0, 0, 0, 0);
        Rectangle destRegion = new Rectangle(0, 0, 0, 0);

        computeRegions(imageReadParam, width, height, theImage, srcRegion, destRegion);
        tilesAcross = (width + tileOrStripWidth - 1) / tileOrStripWidth;
        tilesDown = (height + tileOrStripHeight - 1) / tileOrStripHeight;

        // Compute bounds on the tile indices for this source region.
        int minTileX = TIFFImageWriter.XToTileX(srcRegion.x, 0, tileOrStripWidth);
        int minTileY = TIFFImageWriter.YToTileY(srcRegion.y, 0, tileOrStripHeight);
        int maxTileX = TIFFImageWriter.XToTileX(srcRegion.x + srcRegion.width - 1, 0, tileOrStripWidth);
        int maxTileY = TIFFImageWriter.YToTileY(srcRegion.y + srcRegion.height - 1, 0, tileOrStripHeight);

        boolean isAbortRequested = false;

        List<long[]> ranges = new ArrayList<>();
        int firstTileIndex = minTileY * tilesAcross + minTileX;

        long rangeStart = getTileOrStripOffset(firstTileIndex);
        long rangeEnd = rangeStart + getTileOrStripByteCount(firstTileIndex) - 1;
        if (planarConfiguration == BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR) {
            //decompressor.setPlanar(true);
            int[] sb = new int[1];
            int[] db = new int[1];
            for (int tj = minTileY; tj <= maxTileY; tj++) {
                for (int ti = minTileX; ti <= maxTileX; ti++) {
                    for (int band = 0; band < numBands; band++) {
                        //sb[0] = sourceBands[band];
                        //decompressor.setSourceBands(sb);
                        //db[0] = destinationBands[band];
                        //decompressor.setDestinationBands(db);
                        //XXX decompressor.beginDecoding();

                        // The method abortRequested() is synchronized
                        // so check it only once per loop just before
                        // doing any actual decoding.
                        if (abortRequested()) {
                            isAbortRequested = true;
                            break;
                        }
                        //decodeTile(ti, tj, band);
                    }
                    if (isAbortRequested) break;
                }
                if (isAbortRequested) break;
            }
        } else {
            int band = -1;
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                    // The method abortRequested() is synchronized
                    // so check it only once per loop just before
                    // doing any actual decoding.
                    if (abortRequested()) {
                        isAbortRequested = true;
                        break;
                    }

                    if (tileY == minTileY && tileX == minTileX) {
                        continue;
                    }

                    int tileIndex = tileY * tilesAcross + tileX;

                    if (planarConfiguration == BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR) {
                        tileIndex += band * tilesAcross * tilesDown;
                    }

                    long offset = getTileOrStripOffset(tileIndex);

                    if (offset == rangeEnd + 1) {
                        // this tile starts where the last one left off
                        rangeEnd = offset + getTileOrStripByteCount(tileIndex) - 1;
                    } else {
                        // this tile is in a new position.  add the current range and start a new one.
                        ranges.add(new long[]{rangeStart, rangeEnd});
                        rangeStart = offset;
                        rangeEnd = rangeStart + getTileOrStripByteCount(tileIndex) - 1;
                    }
                }

                if (isAbortRequested) break;
            }
        }
        ranges.add(new long[]{rangeStart, rangeEnd});

        // read the ranges and cache them in the image input stream delegate
        if (stream instanceof CogImageInputStream) {
            ((CogImageInputStream)stream).readRanges(ranges.toArray(new long[][]{}));
        }

        // At this point, the CogImageInputStream has fetched and cached all of the bytes from the requested tiles.
        // Now we proceed with the legacy TIFFImageReader code.
        return super.read(imageIndex, param);
    }

    // TODO: this method should be protected in TIFFImageReader so it need not be reimplemented
    protected long getTileOrStripOffset(int tileIndex) throws IIOException {
        TIFFField f = this.imageMetadata.getTIFFField(324);
        if (f == null) {
            f = this.imageMetadata.getTIFFField(273);
        }

        if (f == null) {
            f = this.imageMetadata.getTIFFField(513);
        }

        if (f == null) {
            throw new IIOException("Missing required strip or tile offsets field.");
        } else {
            return f.getAsLong(tileIndex);
        }
    }

    // TODO: this method should be protected in TIFFImageReader so it need not be reimplemented
    protected long getTileOrStripByteCount(int tileIndex) throws IOException {
        TIFFField f = this.imageMetadata.getTIFFField(325);
        if (f == null) {
            f = this.imageMetadata.getTIFFField(279);
        }

        if (f == null) {
            f = this.imageMetadata.getTIFFField(514);
        }

        long tileOrStripByteCount;
        if (f != null) {
            tileOrStripByteCount = f.getAsLong(tileIndex);
        } else {
            this.processWarningOccurred("TIFF directory contains neither StripByteCounts nor TileByteCounts field: attempting to calculate from strip or tile width and height.");
            int bitsPerPixel = this.bitsPerSample[0];

            int bytesPerRow;
            for (bytesPerRow = 1; bytesPerRow < this.samplesPerPixel; ++bytesPerRow) {
                bitsPerPixel += this.bitsPerSample[bytesPerRow];
            }

            bytesPerRow = (this.tileOrStripWidth * bitsPerPixel + 7) / 8;
            tileOrStripByteCount = (long) (bytesPerRow * this.tileOrStripHeight);
            long streamLength = this.stream.length();
            if (streamLength != -1L) {
                tileOrStripByteCount = Math.min(tileOrStripByteCount, streamLength - this.getTileOrStripOffset(tileIndex));
            } else {
                this.processWarningOccurred("Stream length is unknown: cannot clamp estimated strip or tile byte count to EOF.");
            }
        }

        return tileOrStripByteCount;
    }

}
