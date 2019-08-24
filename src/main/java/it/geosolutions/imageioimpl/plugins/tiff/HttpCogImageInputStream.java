package it.geosolutions.imageioimpl.plugins.tiff;

import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author joshfix
 * Created on 2019-08-23
 */
public class HttpCogImageInputStream implements ImageInputStream, CogImageInputStream {

    protected int headerSize = 16384;
    protected String url;
    protected ByteBuffer byteBuffer;
    protected HttpRangeReader rangeReader = new HttpRangeReader();
    protected int fileSize;
    protected long[][] ranges;
    protected MemoryCacheImageInputStream delegate;

    public HttpCogImageInputStream(URL url) {
        this(url.toString());
    }

    public HttpCogImageInputStream(String url) {
        this.url = url;

        // get the file size with a HEAD request
        fileSize = rangeReader.getFileSize(url);
        byteBuffer = ByteBuffer.allocate(fileSize);

        // read the header
        System.out.println("Reading header with size " + headerSize);
        rangeReader.read(byteBuffer, url, 0, headerSize);

        // wrap the result in a MemoryCacheInputStream
        delegate = new MemoryCacheImageInputStream(new ByteArrayInputStream(byteBuffer.array()));
    }

    @Override
    public void readRanges(long[][] ranges) {
        System.out.println("Reading " + ranges.length + " ranges.");
        this.ranges = ranges;
        rangeReader.readAsync(byteBuffer, url, ranges);
        ByteOrder byteOrder = delegate.getByteOrder();
        long streamPos = 0;
        try {
            streamPos = delegate.getStreamPosition();
        } catch (IOException e) {
            e.printStackTrace();
        }
        delegate = new MemoryCacheImageInputStream(new ByteArrayInputStream(byteBuffer.array()));
        delegate.setByteOrder(byteOrder);
        try {
            delegate.seek(streamPos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUrl() {
        return url;
    }

    public ImageInputStream getDelegate() {
        return delegate;
    }

    @Override
    public void setByteOrder(ByteOrder byteOrder) {
        delegate.setByteOrder(byteOrder);
    }

    @Override
    public ByteOrder getByteOrder() {
        return delegate.getByteOrder();
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public long length() {
        return byteBuffer.array().length;
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return delegate.skipBytes(n);
    }

    @Override
    public long skipBytes(long n) throws IOException {
        return delegate.skipBytes(n);
    }

    @Override
    public void seek(long pos) throws IOException {
        delegate.seek(pos);
    }

    @Override
    public void mark() {
        delegate.mark();
    }

    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public void flushBefore(long pos) throws IOException {
        delegate.flushBefore(pos);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public long getFlushedPosition() {
        return delegate.getFlushedPosition();
    }

    @Override
    public boolean isCached() {
        return delegate.isCached();
    }

    @Override
    public boolean isCachedMemory() {
        return delegate.isCachedMemory();
    }

    @Override
    public boolean isCachedFile() {
        return delegate.isCachedFile();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void readBytes(IIOByteBuffer buf, int len) throws IOException {
        delegate.readBytes(buf, len);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return delegate.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return delegate.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return delegate.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return delegate.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return delegate.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return delegate.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return delegate.readInt();
    }

    @Override
    public long readUnsignedInt() throws IOException {
        return delegate.readUnsignedInt();
    }

    @Override
    public long readLong() throws IOException {
        return delegate.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return delegate.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return delegate.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return delegate.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return delegate.readUTF();
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        delegate.readFully(b, off, len);
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        delegate.readFully(b);
    }

    @Override
    public void readFully(short[] s, int off, int len) throws IOException {
        delegate.readFully(s, off, len);
    }

    @Override
    public void readFully(char[] c, int off, int len) throws IOException {
        delegate.readFully(c, off, len);
    }

    @Override
    public void readFully(int[] i, int off, int len) throws IOException {
        delegate.readFully(i, off, len);
    }

    @Override
    public void readFully(long[] l, int off, int len) throws IOException {
        delegate.readFully(l, off, len);
    }

    @Override
    public void readFully(float[] f, int off, int len) throws IOException {
        delegate.readFully(f, off, len);
    }

    @Override
    public void readFully(double[] d, int off, int len) throws IOException {
        delegate.readFully(d, off, len);
    }

    @Override
    public long getStreamPosition() throws IOException {
        return delegate.getStreamPosition();
    }

    @Override
    public int getBitOffset() throws IOException {
        return delegate.getBitOffset();
    }

    @Override
    public void setBitOffset(int bitOffset) throws IOException {
        delegate.setBitOffset(bitOffset);
    }

    @Override
    public int readBit() throws IOException {
        return delegate.readBit();
    }

    @Override
    public long readBits(int numBits) throws IOException {
        return delegate.readBits(numBits);
    }

}
