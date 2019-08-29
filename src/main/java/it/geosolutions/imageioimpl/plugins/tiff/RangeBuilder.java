package it.geosolutions.imageioimpl.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * @author joshfix
 * Created on 2019-08-27
 */
public class RangeBuilder {

    protected long currentRangeStart;
    protected long currentRangeEnd;
    protected boolean tileAdded = false;
    protected List<long[]> ranges = new ArrayList<>();

    public RangeBuilder(long initialRangeStart, long initialRangeEnd) {
        currentRangeStart = initialRangeStart;
        currentRangeEnd = initialRangeEnd;
    }

    public void addTileRange(long offset, long tileOrStripByteCount) {
        tileAdded = true;
        if (offset == currentRangeEnd + 1) {
            // this tile starts where the last one left off
            currentRangeEnd = offset + tileOrStripByteCount - 1;
        } else {
            // this tile is in a new position.  add the current range and start a new one.
            ranges.add(new long[]{currentRangeStart, currentRangeEnd});
            currentRangeStart = offset;
            currentRangeEnd = currentRangeStart + tileOrStripByteCount - 1;
        }
    }

    public List<long[]> getRanges() {
        if (tileAdded) {
            ranges.add(new long[]{currentRangeStart, currentRangeEnd});
        }
        return ranges;
    }
}
