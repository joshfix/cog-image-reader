package it.geosolutions.imageioimpl.plugins.tiff;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageioimpl.plugins.tiff.stream.CogImageInputStream;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;

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
        // if the image input stream isn't a CogImageInputStream, skip all this nonsense and just use the original code
        if (!(stream instanceof CogImageInputStream)) {
            return super.read(imageIndex, param);
        }

        System.out.println("Reading pixels at offset (" + param.getSourceRegion().getX() + ", "
                + param.getSourceRegion().getY() + ") with a width of " + param.getSourceRegion().getWidth()
                + "px and height of " + param.getSourceRegion().getHeight() + "px");

        // TODO: would be very nice if prepareRead method in TIFFIMageReader was protected and not private
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

        System.out.println("Reading tiles (" + minTileX + "," + minTileY + ") - (" + maxTileX + "," + maxTileY + ")");

        int firstTileIndex = minTileY * tilesAcross + minTileX;
        long initialRangeStart = getTileOrStripOffset(firstTileIndex);
        long initialRangeEnd = initialRangeStart + getTileOrStripByteCount(firstTileIndex) - 1;

        RangeBuilder rangeBuilder = new RangeBuilder(initialRangeStart, initialRangeEnd);
        CogTileInfo cogTileInfo = ((CogImageInputStream)stream).getCogTileInfo();

        // loops through each requested tile and determine if they byte positions are consecutive
        // builds long[][] array of all ranges needed to be requested
        boolean isAbortRequested = false;
        if (planarConfiguration == BaselineTIFFTagSet.PLANAR_CONFIGURATION_PLANAR) {
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                    for (int band = 0; band < numBands; band++) {

                        int tileIndex = band * tilesAcross * tilesDown;
                        long offset = getTileOrStripOffset(tileIndex);
                        long byteLength = getTileOrStripByteCount(tileIndex);
                        cogTileInfo.addTileRange(tileIndex, offset, byteLength);

                        // skip the first tile as there is no previous tile to compare to
                        if (tileY == minTileY && tileX == minTileX) {
                            continue;
                        }

                        rangeBuilder.compare(offset, byteLength);
                    }

                    if (isAbortRequested) break;
                }

                if (isAbortRequested) break;
            }
        } else {
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                for (int tileX = minTileX; tileX <= maxTileX; tileX++) {

                    int tileIndex = tileY * tilesAcross + tileX;
                    long offset = getTileOrStripOffset(tileIndex);
                    long byteLength = getTileOrStripByteCount(tileIndex);
                    cogTileInfo.addTileRange(tileIndex, offset, byteLength);

                    // skip the first tile as there is no previous tile to compare to
                    if (tileY == minTileY && tileX == minTileX) {
                        continue;
                    }

                    rangeBuilder.compare(offset, byteLength);
                }

                if (isAbortRequested) break;
            }
        }

        if (isAbortRequested) {
            processReadAborted();
        } else {
            processImageComplete();
        }

        cogTileInfo.setContiguousRanges(rangeBuilder.getRanges());

        // read the ranges and cache them in the image input stream delegate
        ((CogImageInputStream) stream).readRanges(cogTileInfo);

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
