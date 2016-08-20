import java.io.InputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.Feature;

public class CreateGeoNamesCSV {

    public static void main(String[] args) throws Exception {
        InputStream jsonStream = CreateGeoNamesCSV.class.getClassLoader().getResourceAsStream("neighborhood_boundaries.json");
        // InputStream jsonStream = CreateGeoCSV.class.getClassLoader().getResourceAsStream("dcneighorhoodboundarieswapo.json");

        FeatureJSON reader = new FeatureJSON();

        FeatureCollection<?,?> fc = reader.readFeatureCollection(jsonStream);

        CSVPrinter printer = new CSVPrinter(System.out, CSVFormat.DEFAULT);
        printer.printRecord("gis_id", "NBH_NAMES");

        FeatureIterator<?> iterator = fc.features();
        while (iterator.hasNext()) {
            Feature f = iterator.next();

            // neighborhood_boundaries.json
            String gis_id = f.getProperty("gis_id").getValue().toString();
            String nbh_names = f.getProperty("NBH_NAMES").getValue().toString();

            // dcneighorhoodboundarieswapo.json
            // String objectId = f.getProperty("id").getValue().toString();
            // String gis_id = f.getProperty("id").getValue().toString();
            // String name = f.getProperty("subhood").getValue().toString();
            // String nbh_names = f.getProperty("subhood").getValue().toString();
            // MultiPolygon p = (MultiPolygon) f.getDefaultGeometryProperty().getValue();

            printer.printRecord(gis_id, nbh_names);

        }

        IOUtils.closeQuietly(printer);
        IOUtils.closeQuietly(iterator);
    }

}
