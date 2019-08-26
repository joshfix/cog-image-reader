package it.geosolutions.imageioimpl.plugins.tiff;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.net.http.HttpClient.Version.HTTP_2;

/**
 * @author joshfix
 * Created on 2019-08-21
 */
public class HttpRangeReader implements RangeReader {

    protected URI uri;
    protected HttpClient client;
    protected ByteBuffer buffer;

    protected int timeout = 5;
    protected int fileSize = -1;
    protected int headerSize = 16384;

    public static final String CONTENT_RANGE_HEADER = "content-range";

    public HttpRangeReader(String url) {
        this(URI.create(url));
    }

    public HttpRangeReader(URL url) {
        this(URI.create(url.toString()));
    }

    public HttpRangeReader(URI uri) {
        this.uri= uri;
        client = HttpClient.newBuilder()
                .version(HTTP_2)
                .connectTimeout(Duration.ofSeconds(timeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // read the header
        writeValue(0, read(0, headerSize));
    }

    @Override
    public int getHeaderSize() {
        return headerSize;
    }

    @Override
    public int getFileSize() {
        return fileSize;
    }

    @Override
    public byte[] getBytes() {
        return buffer.array();
    }

    @Override
    public void readAsync(Collection<long[]> ranges) {
        readAsync(ranges.toArray(new long[][]{}));
    }

    @Override
    public void readAsync(long[]... ranges) {
        ranges = reconcileRanges(ranges);

        Instant start = Instant.now();
        Map<Long, CompletableFuture<byte[]>> futureResults = new HashMap<>(ranges.length);

        for (int i = 0; i < ranges.length; i++) {
            HttpRequest request = buildRequest(ranges[i]);
            futureResults.put(ranges[i][0], getAsync(request));
        }

        awaitCompletion(futureResults);
        Instant end = Instant.now();
        System.out.println("Time to read all ranges: " + Duration.between(start, end));
    }

    protected void writeValue(int position, byte[] bytes) {
        buffer.position(position);
        buffer.put(bytes);
    }

    /**
     * Blocks until all ranges have been read and written to the ByteBuffer
     * @param futureResults
     */
    protected void awaitCompletion(Map<Long, CompletableFuture<byte[]>> futureResults) {
        boolean stillWaiting = true;
        List<Long> completed = new ArrayList<>(futureResults.size());
        while (stillWaiting) {
            boolean allDone = true;
            for (Map.Entry<Long, CompletableFuture<byte[]>> entry : futureResults.entrySet()) {
                long key = entry.getKey();
                CompletableFuture<byte[]> value = entry.getValue();
                if (value.isDone()) {
                    if (!completed.contains(key)) {
                        try {
                            writeValue((int) key, value.get());
                            completed.add(key);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    allDone = false;
                }
            }
            stillWaiting = !allDone;
        }
    }

    /**
     * Prevents making new range requests for image data that overlap with the header range that has already been read
     *
     * @param ranges
     * @return
     */
    protected long[][] reconcileRanges(long[][] ranges) {
        boolean modified = false;
        List<long[]> newRanges = new ArrayList<>();
        for (int i = 0; i < ranges.length; i++) {
            if (ranges[i][0] < headerSize) {
                // this range starts inside of what we already read for the header
                modified = true;
                if (ranges[i][1] < headerSize) {
                    // this range is fully inside the header which was already read; discard this range
                    System.out.println("Removed range " + ranges[i][0] + "-" + ranges[i][1] + " as it lies fully within"
                            + " the data already read in the header request");
                } else {
                    // this range starts inside the header range, but ends outside of it.
                    // add a new range that starts at the end of the header range
                    newRanges.add(new long[]{headerSize + 1, ranges[i][1]});
                    System.out.println("Modified range " + ranges[i][0] + "-" + ranges[i][1]
                            + " to " + (headerSize + 1) + "-" + ranges[i][1] + " as it overlaps with data previously"
                            + " read in the header request");
                }
            } else {
                // fully outside the header area, keep the range
                newRanges.add(ranges[i]);
            }
        }

        if (modified) {
            return newRanges.toArray(new long[][]{});
        } else {
            System.out.println("No ranges modified.");
            return ranges;
        }
    }

    protected HttpRequest buildRequest(long[] range) {
        System.out.println("Building request for range " + range[0] + '-' + range[1] + " to " + uri.toString());
        return HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header("Accept", "*/*")
                .header("Range", "bytes=" + range[0] + "-" + range[1])
                .build();
    }

    protected CompletableFuture<byte[]> getAsync(HttpRequest request) {
        return client
                .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(HttpResponse::body);
    }

    protected byte[] read(long start, long end) {
        byte[] bytes = get(buildRequest(new long[]{start, end}));
        writeValue((int) start, bytes);
        return bytes;
    }

    /**
     * Blocking request used to read the header
     * @param request
     * @return
     */
    protected byte[] get(HttpRequest request) {
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            // if the fileSize variable has not been initialized, read it from the response
            if (fileSize == -1) {
                HttpHeaders headers = response.headers();
                String contentRange = headers.firstValue(CONTENT_RANGE_HEADER).get();
                if (contentRange.contains("/")) {
                    String length = contentRange.split("/")[1];
                    try {
                        fileSize = Integer.parseInt(length);
                        buffer = ByteBuffer.allocate(fileSize);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.body();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

}
