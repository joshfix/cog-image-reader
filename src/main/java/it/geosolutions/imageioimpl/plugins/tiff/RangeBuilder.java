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
    protected boolean finalized = false;
    protected List<long[]> ranges = new ArrayList<>();

    public RangeBuilder(long currentRangeStart, long currentRangeEnd) {
        this.currentRangeStart = currentRangeStart;
        this.currentRangeEnd = currentRangeEnd;
    }

    public void compare(long offset, long tileOrStripByteCount) {
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
        if (!finalized) {
            ranges.add(new long[]{currentRangeStart, currentRangeEnd});
            finalized = true;
        }
        return ranges;
    }
}
