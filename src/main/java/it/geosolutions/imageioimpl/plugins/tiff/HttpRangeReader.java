package it.geosolutions.imageioimpl.plugins.tiff;

import java.io.IOException;
import java.net.URI;
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

    protected HttpClient client;

    public HttpRangeReader() {
        client = HttpClient.newBuilder()
                .version(HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public int getFileSize(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpHeaders headers = client.send(request, HttpResponse.BodyHandlers.discarding())
                    .headers();

            Optional<String> length = headers.firstValue("content-length");
            if (length.isPresent()) {
                return Integer.parseInt(length.get());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public byte[] read(String url, long start, long end) {
        return get(buildRequest(url, new long[]{start, end}));
    }

    @Override
    public void read(ByteBuffer byteBuffer, String url, long start, long end) {
        byte[] bytes = get(buildRequest(url, new long[]{start, end}));
        writeValue(byteBuffer, (int)start, bytes);
    }

    @Override
    public void readAsync(ByteBuffer byteBuffer, String url, long[]... ranges) {
        Instant start = Instant.now();

        Map<Long, CompletableFuture<byte[]>> futureResults = new HashMap<>(ranges.length);

        for (int i = 0; i < ranges.length; i++) {
            HttpRequest request = buildRequest(url, ranges[i]);
            futureResults.put(ranges[i][0], getAsync(request));
        }

        awaitCompletion(futureResults, byteBuffer);
        Instant end = Instant.now();
        System.out.println("Time to read all ranges: " + Duration.between(start, end));
    }

    protected void writeValue(ByteBuffer byteBuffer, int position, byte[] bytes) {
        byteBuffer.position(position);
        byteBuffer.put(bytes);
    }


    protected void awaitCompletion(Map<Long, CompletableFuture<byte[]>> futureResults, ByteBuffer byteBuffer) {
        boolean stillWaiting = true;
        while (stillWaiting) {
            boolean allDone = true;
            for (Map.Entry<Long, CompletableFuture<byte[]>> entry : futureResults.entrySet()) {
                long key = entry.getKey();
                CompletableFuture<byte[]> value = entry.getValue();
                if (value.isDone()) {
                    try {
                        writeValue(byteBuffer, (int)key, value.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    allDone = false;
                }
            }
            stillWaiting = !allDone;
        }
    }

    protected HttpRequest buildRequest(String url, long[] range) {
        System.out.println("Building request for range " + range[0] + '-' + range[1] + " to " + url);
        return HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("Accept", "*/*")
                .header("Range", "bytes=" + range[0] + "-" + range[1])
                .build();
    }

    protected CompletableFuture<byte[]> getAsync(HttpRequest request) {
        return client
                .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(HttpResponse::body);
    }

    protected byte[] get(HttpRequest request) {
        try {
            return client
                    .send(request, HttpResponse.BodyHandlers.ofByteArray())
                    .body();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}
