package it.geosolutions.imageioimpl.plugins.tiff.stream;

import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * @author joshfix
 * Created on 2019-08-23
 */
public class HttpCogImageInputStreamSpi extends ImageInputStreamSpi {

    private static final String vendorName = "Josh Fix";
    private static final String version = "1.0";
    private static final Class<String> inputClass = String.class;
    private final static Logger LOGGER = Logger.getLogger(HttpCogImageInputStreamSpi.class.getName());

    public HttpCogImageInputStreamSpi() {
        super(vendorName, version, inputClass);
    }

    @Override
    public ImageInputStream createInputStreamInstance(Object input, boolean useCache, File cacheDir) throws IOException {
        if (input instanceof String || input instanceof URL) {
            return new HttpCogImageInputStream(input.toString());
        }
        throw new IOException("Invalid input.");
    }

    @Override
    public String getDescription(Locale locale) {
        return "Cloud Optimized GeoTIFF reader";
    }

    @Override
    public void onRegistration(ServiceRegistry registry, Class<?> category) {
        super.onRegistration(registry, category);
        Class<ImageInputStreamSpi> targetClass = ImageInputStreamSpi.class;
        for (Iterator<? extends ImageInputStreamSpi> i = registry.getServiceProviders(targetClass, true); i.hasNext();) {
            ImageInputStreamSpi other = i.next();

            if (this != other)
                registry.setOrdering(targetClass, this, other);

        }
    }
}
