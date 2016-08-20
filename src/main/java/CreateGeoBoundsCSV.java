import java.io.InputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.Feature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class CreateGeoBoundsCSV {

    public static void main(String[] args) throws Exception {
        InputStream jsonStream = CreateGeoBoundsCSV.class.getClassLoader().getResourceAsStream("neighborhood_boundaries.json");
        // InputStream jsonStream = CreateGeoCSV.class.getClassLoader().getResourceAsStream("dcneighorhoodboundarieswapo.json");

        FeatureJSON reader = new FeatureJSON();

        FeatureCollection<?,?> fc = reader.readFeatureCollection(jsonStream);

        CSVPrinter printer = new CSVPrinter(System.out, CSVFormat.DEFAULT);
        printer.printRecord("gis_id", "Longitude", "Latitude", "Point Order");

        FeatureIterator<?> iterator = fc.features();
        while (iterator.hasNext()) {
            Feature f = iterator.next();

            // neighborhood_boundaries.json
            String gis_id = f.getProperty("gis_id").getValue().toString();
            Polygon p = (Polygon) f.getDefaultGeometryProperty().getValue();

            // dcneighorhoodboundarieswapo.json
            // String objectId = f.getProperty("id").getValue().toString();
            // String gis_id = f.getProperty("id").getValue().toString();
            // String name = f.getProperty("subhood").getValue().toString();
            // String nbh_names = f.getProperty("subhood").getValue().toString();
            // MultiPolygon p = (MultiPolygon) f.getDefaultGeometryProperty().getValue();

            int pointOrder = 1;
            for (Coordinate c : p.getCoordinates()) {
                printer.printRecord(gis_id, c.x, c.y, pointOrder++);
            }

        }

        IOUtils.closeQuietly(printer);
        IOUtils.closeQuietly(iterator);
    }

}
