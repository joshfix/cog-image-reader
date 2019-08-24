# Cloud Optimized GeoTIFF Reader 

Cloud Optimized GeoTIFF (COG) reader for imageio-ext.   

Introduces a 
[RangeReader](./src/main/java/it/geosolutions/imageioimpl/plugins/tiff/RangeReader.java) interface.  This interface can 
be implemented by any library to execute asynchronous block reads.  Currently an HTTP implementation is provided.  This 
is used by the 
[HttpCogImageInputStream](./src/main/java/it/geosolutions/imageioimpl/plugins/tiff/HttpCogImageInputStream.java)
ImageInputStream implementation.  This ImageInputStream implementation pre-fetches all of the bytes from the requested 
grid range asynchronoulsy and wraps the results in a MemoryCacheImageInputStream.  With the byte data in memory, legacy 
TIFFImageReader code is free to decode individual tiles without network performance penalties.
 

![COG](./images/sample.png "COG")