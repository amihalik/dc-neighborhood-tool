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

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class AppendGisIdWithGeocoding {

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

    private static double[] getLatLon(String address) throws Exception {
        HttpResponse<JsonNode> jsonResponse = Unirest.get("https://maps.googleapis.com/maps/api/geocode/json").header("accept", "application/json")
                .queryString("address", address).queryString("key", "xxxxxxxxxxxxxxxxxxxx").asJson();

        System.out.println(jsonResponse.getBody());
        double lat = 0.;
        double lon = 0.;
        
        try{
        lat = jsonResponse.getBody().getObject().getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
                .getDouble("lat");
        lon = jsonResponse.getBody().getObject().getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
                .getDouble("lng");
        } catch (Exception e){
            System.err.println("Error with Address :: " + address);
            e.printStackTrace();
        }
        System.out.format("lat %s, lon %s %n", lat, lon);
        return new double[] {lat, lon};
    }

    private static List<String> getValues(List<String> header, CSVRecord record) {
        List<String> values = new ArrayList<String>();
        for (String h : header) {
            if (!h.equals("gis_id") && !h.equals("lat") && !h.equals("lon")) {
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

    // public static void main(String[] args) throws Exception{
    // HttpResponse<JsonNode> jsonResponse = Unirest.get("http://maps.googleapis.com/maps/api/geocode/json")
    // .header("accept", "application/json")
    // .queryString("address", "1330 5th St NW, Washington, DC 20001, USA")
    // .queryString("sensor", "false")
    // .asJson();
    //
    // double lat =
    // jsonResponse.getBody().getObject().getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lat");
    // double lon =
    // jsonResponse.getBody().getObject().getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lng");
    //
    // System.out.format("lat %s, lon %s %n", lat, lon);
    // }

    public static void main(String[] args) throws Exception {
        // InputStream jsonStream = CreateGeoCSV.class.getClassLoader().getResourceAsStream("dcneighorhoodboundarieswapo.json");
        InputStream jsonStream = AppendGisIdWithGeocoding.class.getClassLoader().getResourceAsStream("neighborhood_boundaries.json");
        FeatureCollection<?,?> fc = new FeatureJSON().readFeatureCollection(jsonStream);

        File inputFile = FileUtils.getFile("src/main/resources/ABC/DistrictABCLicenseeList842016_1.csv");

        File outFile = new File(inputFile.getParentFile(), FilenameUtils.getBaseName(inputFile.getName()) + "_gis_id.csv");

        CSVParser csv = CSVParser.parse(inputFile, Charsets.UTF_8, CSVFormat.EXCEL.withFirstRecordAsHeader());

        Map<String,Integer> hm = csv.getHeaderMap();
        // find the addressIndex
        int addressIndex = -1;
        for (String k : hm.keySet()) {
            if (equalsIgnoreCase(k, "address") && addressIndex == -1) {
                System.out.println("Using field for addressv :: " + k);
                addressIndex = hm.get(k);
            }
        }

        Appendable out = new PrintStream(outFile);

        CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT);
        // Print the header with the new col
        List<String> header = new ArrayList<String>(csv.getHeaderMap().keySet());
        header.add("lat");
        header.add("lon");
        header.add("gis_id");
        printer.printRecord(header);

        
        for (CSVRecord r : csv) {
            String address = r.get(addressIndex);
            double[] latLon = getLatLon(address);

            double lat = latLon[0];
            double lon = latLon[1];
            String gis_id = getNbhGisId(fc, lat, lon);
            List<String> values = getValues(header, r);

            values.add(Double.toString(lat));
            values.add(Double.toString(lon));
            values.add(gis_id);
            printer.printRecord(values);
        }

        IOUtils.closeQuietly(printer);
    }

}
