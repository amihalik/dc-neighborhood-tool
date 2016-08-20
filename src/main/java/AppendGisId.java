import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.Feature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class AppendGisId {

    private static String getNbhGisId(FeatureCollection<?,?> fc, double lat, double lon) {
        FeatureIterator<?> iterator = fc.features();
        Point point = getPoint(lat, lon);
        while (iterator.hasNext()) {
            Feature f = iterator.next();
            String gis_id = f.getProperty("gis_id").getValue().toString();
            Polygon p = (Polygon) f.getDefaultGeometryProperty().getValue();

            // String gis_id = f.getProperty("id").getValue().toString();
            // MultiPolygon p = (MultiPolygon) f.getDefaultGeometryProperty().getValue();

            if (p.contains(point)) {
                return gis_id;
            }
        }

        return null;
    }

    private static List<String> getValues(List<String> header, CSVRecord record) {
        List<String> values = new ArrayList<String>();
        for (String h : header) {
            if (!h.equals("gis_id")) {
                values.add(record.get(h));
            }
        }
        return values;
    }

    private static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

    private static Point getPoint(double lat, double lon) {
        return geometryFactory.createPoint(new Coordinate(lon, lat));

    }

    public static boolean equalsIgnoreCase(String key, String... strings) {
        String klow = key.toLowerCase();
        boolean startswith = false;
        for (String v : strings) {
            startswith = startswith || klow.startsWith(v.toLowerCase());
        }
        return startswith;
    }

    public static void main(String[] args) throws Exception {
        // InputStream jsonStream = CreateGeoCSV.class.getClassLoader().getResourceAsStream("dcneighorhoodboundarieswapo.json");
        InputStream jsonStream = AppendGisId.class.getClassLoader().getResourceAsStream("neighborhood_boundaries.json");
        FeatureCollection<?,?> fc = new FeatureJSON().readFeatureCollection(jsonStream);

        File inputFile = FileUtils.getFile("src/main/resources/inputdata/restaurantlist_clean.csv");

        File outFile = new File(inputFile.getParentFile(), FilenameUtils.getBaseName(inputFile.getName()) + "_gis_id.csv");
        
        CSVParser csv = CSVParser.parse(inputFile, Charsets.UTF_8, CSVFormat.EXCEL.withFirstRecordAsHeader());

        Map<String,Integer> hm = csv.getHeaderMap();
        // find the lat
        int latIndex = -1;
        for (String k : hm.keySet()) {
            if (equalsIgnoreCase(k, "lat", "Latitude") && latIndex == -1) {
                System.out.println("Using field for lat :: " + k);
                latIndex = hm.get(k);
            }
        }
        // find the lon
        int lonIndex = -1;
        for (String k : hm.keySet()) {
            if (equalsIgnoreCase(k, "lon", "long", "Longitude") && lonIndex == -1) {
                System.out.println("Using field for lon :: " + k);
                lonIndex = hm.get(k);
            }
        }
        
        
        
        Appendable out = new PrintStream(outFile);

        CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT);
        // Print the header with the new col
        List<String> header = new ArrayList<String>(csv.getHeaderMap().keySet());
        header.add("gis_id");
        printer.printRecord(header);

        for (CSVRecord r : csv) {
            String latStr = r.get(latIndex);
            String lonStr = r.get(lonIndex);
            double lat = -1;
            double lon = -1;
            if (!latStr.isEmpty() && !lonStr.isEmpty()) {
                lat = Double.parseDouble(latStr);
                lon = Double.parseDouble(r.get(lonIndex));
            }
            String gis_id = getNbhGisId(fc, lat, lon);
            List<String> values = getValues(header, r);
            values.add(gis_id);
            printer.printRecord(values);
        }

        IOUtils.closeQuietly(printer);
    }

}
