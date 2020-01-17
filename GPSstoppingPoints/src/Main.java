import weka.clusterers.*;
import weka.core.Instances;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static java.lang.StrictMath.sqrt;


public class Main {

    public static Map<String, String> clustersInstances = new HashMap<>();
    public static Map<String, String> clustersTimestamps = new HashMap<>();
    public static Map<String, StringBuilder> clusterValues = new HashMap<>();
    public static ArrayList<Integer> trueClusters = new ArrayList<>();


    /*LOAD ARFF CONTENT*/
    public static Instances openFile() throws IOException {
        BufferedReader reader = new BufferedReader( new FileReader("../coords.arff"));
        Instances data = new Instances(reader);
        reader.close();


        return data;
    }


    public static boolean calculatePCT(int cluster, double DCCap, double PCTapp) throws IOException {
        int abnormals = 0;
        String[] parts, aux1, aux2, aux3;
        double x1, y1, x2, y2, C, DCC, pct = 0;
        double ponto1x, ponto1y, ponto2x, ponto2y, ponto3x, ponto3y;
        StringBuilder mystring;

        C = Integer.parseInt(clustersInstances.get(Integer.toString(cluster)));

        mystring = clusterValues.get(Integer.toString(cluster));
        parts = mystring.toString().split("\n");

        for (int i = 3; i <= C; i++){//first two points are considered automatically normal

            //TODO: confirm that the direction change cosine angle is not diferent if we consider longitude and latitude
            aux1 = parts[i-2].split(",");
            aux2 = parts[i-1].split(",");
            aux3 = parts[i].split(",");

            ponto1x = Double.parseDouble(aux1[0]);
            ponto1y = Double.parseDouble(aux1[1]);
            ponto2x = Double.parseDouble(aux2[0]);
            ponto2y = Double.parseDouble(aux2[1]);
            ponto3x = Double.parseDouble(aux3[0]);
            ponto3y = Double.parseDouble(aux3[1]);

            x1 = ponto2x - ponto1x;
            y1 = ponto2y - ponto1y;
            x2 = ponto3x - ponto2x;
            y2 = ponto3y - ponto2y;

            DCC = abs((x1 * x2 + y1 * y2) / ( sqrt(x1*x1 + y1*y1) * sqrt(x2*x2 + y2*y2) ));

            if (DCC >= DCCap){
                abnormals++;
            }
        }

        pct = abnormals/C;

        if(pct >= PCTapp){
            return false;
        } else {
            return true;
        }

    }

    public static boolean checkTimeSequence(){


        return true;

    }

    public static void extractTimestamps(){
        long instance = 0;
        String csvFile = "C:\\Users\\Joel Pires\\Desktop\\new\\traces.csv";
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ";";

        try {

            br = new BufferedReader(new FileReader(csvFile));
            instance = 0;
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] country = line.split(cvsSplitBy);

                System.out.println(country[1]);

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void main(String[] args) throws Exception {
        int i,j, tmp, numberClusters = 0;
        long numberInstances = 0;
        String[] parts, options, aux;
        String clustersDescriptions, currentCLuster, pathname;
        DBSCAN clusterer;
        Instances mydata;
        Matcher m;
        StringBuilder sb, tmp1;

        extractTimestamps();
        mydata = openFile();

        options = weka.core.Utils.splitOptions("-E 9.354974507694466532578698723046e-7 -M 6 -A \"weka.core.EuclideanDistance -R first-last\"");

        clusterer = new DBSCAN();   // new instance of clusterer
        clusterer.setOptions(options);     // set the options
        clusterer.buildClusterer(mydata);    // build the clusterer

        clustersDescriptions = clusterer.toString();
        parts = (clustersDescriptions).split("\n");

        m = Pattern.compile("\\d+").matcher(parts[8]);
        while (m.find()) {
            numberClusters = Integer.parseInt(m.group(0));
        }

        m = Pattern.compile("\\d+").matcher(parts[3]);
        while (m.find()) {
            numberInstances = Integer.parseInt(m.group(0));
        }

        for (i = 11; i < numberInstances; i++) {

            aux = (parts[i]).split(">");
            currentCLuster = aux[1].trim();

            if (!clustersInstances.containsKey(currentCLuster)){

                clustersInstances.put(currentCLuster, "0");
                sb = new StringBuilder();
                sb.append("x");
                sb.append(',');
                sb.append("y");
                sb.append('\n');
                clusterValues.put(currentCLuster, sb);

            }


            //increment number of instances of each cluster
            tmp = Integer.parseInt(clustersInstances.get(currentCLuster));
            tmp++;
            clustersInstances.put(currentCLuster, Integer.toString(tmp));

            //Add latitude and longitude to the values
            m = Pattern.compile("(\\-)?\\d+\\.\\d+").matcher(parts[i]);
            j = 0;
            tmp1 = clusterValues.get(currentCLuster);
            while (m.find()) {
                tmp1.append(m.group(0));
                if (j == 0) {
                    tmp1.append(',');
                }
                j++;
            }
            tmp1.append('\n');
            clusterValues.put(currentCLuster, tmp1);

        }

        //SECOND CONSTRAINT
        for (i = 0; i< numberClusters; i++){
            if(calculatePCT(i, 0.8, 0.6)){
                trueClusters.add(i);
            }
        }


        //TODO: FIRST CONSTRAINT

        //TODO: escrever para CSV apenas os clusters que passaram as duas constraints
        //WRITE TO A CSV FILE
        Iterator it = clusterValues.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            pathname = "C:\\Users\\Joel Pires\\Desktop\\new\\csv\\cluster" + pair.getKey() + ".csv";
            PrintWriter pw = new PrintWriter(new File(pathname));
            pw.write(pair.getValue().toString());
            pw.close();
            it.remove(); // avoids a ConcurrentModificationException
        }


    }

}
