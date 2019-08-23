# Cloud Optimized GeoTIFF Reader 

Early stages of a Cloud Optimized GeoTIFF (COG) reader for imageio-ext.  Introduces a 
[RangeReader](./src/main/java/it/geosolutions/imageioimpl/plugins/tiff/RangeReader.java) interface.  This interface can 
be implemented by any library to execute asynchronous block reads.  Currently an HTTP implementation is provided.  This 
is used by the 
[HttpCogImageInputStream](./src/main/java/it/geosolutions/imageioimpl/plugins/tiff/HttpCogImageInputStream.java)
ImageInputStream implementation.  This ImageInputStream implementation pre-fetches all of the ranges from the 
RangeReader and stores the results in a delegate stream.  This stream can then be read a byte/short/int/etc at a time 
by the existing reader code without performance penalties.

Currently there is an issue decoding the image when the image does not start at tile 0,0.   
 


