import weka.classifiers.Evaluation;
import weka.classifiers.meta.Bagging;
import weka.classifiers.meta.Vote;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.M5Rules;
import weka.classifiers.trees.RandomTree;
import weka.classifiers.lazy.LWL;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            // Load ARFF file
            Instances arffData;
            File myArff = new File("./suinGeneratedData.arff");
            ArffLoader arffLoader = new ArffLoader();
            arffLoader.setFile(myArff);
            arffData = arffLoader.getDataSet();
            arffData.setClassIndex(arffData.numAttributes() - 1); // Assume the last attribute is the class

            // Hardcoded user location (latitude, longitude)
            double userLat = 46.0569;
            double userLon = 14.5058;

            // GUI Window
            JFrame frame = new JFrame("SUIN Recommendation System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            frame.getContentPane().add(panel);

            JLabel label = new JLabel("Filter Options:");
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);

            JCheckBox accessibilityCheck = new JCheckBox("Accessible for disabled");
            accessibilityCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(accessibilityCheck);

            JCheckBox parkingCheck = new JCheckBox("Parking available");
            parkingCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(parkingCheck);

            JComboBox<String> packageTypeDropdown = new JComboBox<>(new String[]{"<Any>", "navadni", "hlajen", "suhi"});
            packageTypeDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(new JLabel("Package Locker Type:"));
            panel.add(packageTypeDropdown);

            JComboBox<String> classifierDropdown = new JComboBox<>(new String[]{
                    "<Select Classifier>", "DecisionTable", "LWL (takes long time - maybe too long for this demo)", "Bagging", "Vote", "RandomTree"
            });
            classifierDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(new JLabel("Classifier:"));
            panel.add(classifierDropdown);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            panel.add(splitPane);

            JTextArea resultsTxt = new JTextArea(15, 30);
            resultsTxt.setEditable(false);
            JScrollPane resultsScrollPane = new JScrollPane(resultsTxt);
            splitPane.setLeftComponent(resultsScrollPane);

            JTextArea statsTxt = new JTextArea(15, 30);
            statsTxt.setEditable(false);
            JScrollPane statsScrollPane = new JScrollPane(statsTxt);
            splitPane.setRightComponent(statsScrollPane);

            splitPane.setResizeWeight(0.5);

            JButton classifyAndRunBtn = new JButton("Classify and Recommend");
            classifyAndRunBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            classifyAndRunBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String selectedClassifier = (String) classifierDropdown.getSelectedItem();
                    Evaluation eval;

                    if (selectedClassifier == null || selectedClassifier.equals("<Select Classifier>")) {
                        statsTxt.append("Please select a classifier.\n");
                        return;
                    }

                    try {
                        eval = new Evaluation(arffData);
                        weka.classifiers.Classifier classifier;

                        switch (selectedClassifier) {
                            case "DecisionTable":
                                classifier = new DecisionTable();
                                break;
                            case "LWL (takes long time - maybe too long for this demo)":
                                classifier = new LWL();
                                break;
                            case "Bagging":
                                classifier = new Bagging();
                                break;
                            case "Vote":
                                classifier = new Vote();
                                break;
                            case "RandomTree":
                                classifier = new RandomTree();
                                break;
                            default:
                                statsTxt.append("Invalid classifier selected.\n");
                                return;
                        }

                        // Clear text field before running classification/recommendation
                        resultsTxt.setText("Top Recommendations:\n");
                        statsTxt.setText("Classifier: " + selectedClassifier + "\n");

                        long startTime = System.currentTimeMillis();
                        classifier.buildClassifier(arffData);
                        long endTime = System.currentTimeMillis();
                        eval.crossValidateModel(classifier, arffData, 10, new Random(0));
                        statsTxt.append(eval.toSummaryString() + "\n");
                        statsTxt.append("Model build time: " + (endTime - startTime) + " ms\n");


                        PriorityQueue<Recommendation> recommendationQueue = new PriorityQueue<>(Comparator.comparingDouble(Recommendation::getScore).reversed());

                        for (int i = 0; i < arffData.numInstances(); i++) {
                            double[] distribution = classifier.distributionForInstance(arffData.instance(i));
                            double suitabilityScore = distribution[0];

                            double lat = arffData.instance(i).value(arffData.attribute("Geolokacija_lat"));
                            double lon = arffData.instance(i).value(arffData.attribute("Geolokacija_lon"));
                            double distance = calculateDistance(userLat, userLon, lat, lon);

                            // Adjust score to consider both suitability and distance
                            double combinedScore = suitabilityScore - (distance * 0.1); // Penalize by 0.1 per km

                            String type = arffData.instance(i).stringValue(arffData.attribute("Vrsta_paketnika"));
                            boolean accessible = arffData.instance(i).stringValue(arffData.attribute("Dostopnost_za_invalide")).equals("da");
                            boolean parking = arffData.instance(i).stringValue(arffData.attribute("Dostopnost_parkirisca")).equals("da");

                            if (accessibilityCheck.isSelected() && !accessible) continue;
                            if (parkingCheck.isSelected() && !parking) continue;
                            if (!packageTypeDropdown.getSelectedItem().equals("<Any>") && !packageTypeDropdown.getSelectedItem().equals(type)) continue;

                            recommendationQueue.add(new Recommendation(i, combinedScore, distance, type));
                        }

                        int count = 0;
                        while (!recommendationQueue.isEmpty() && count < 5) {
                            Recommendation r = recommendationQueue.poll();
                            resultsTxt.append(String.format("ID: %d, Score: %.2f, Distance: %.2f km, Type: %s\n",
                                    r.getId(), r.getScore(), r.getDistance(), r.getType()));
                            count++;
                        }

                        if (count == 0) {
                            resultsTxt.append("No recommendations found based on the selected filters.\n");
                        }

                    } catch (Exception ex) {
                        statsTxt.append("Error during classification: " + ex.getMessage() + "\n");
                    }
                }
            });
            panel.add(classifyAndRunBtn);

            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the Earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Convert to km
    }

    private static class Recommendation {
        private final int id;
        private final double score;
        private final double distance;
        private final String type;

        public Recommendation(int id, double score, double distance, String type) {
            this.id = id;
            this.score = score;
            this.distance = distance;
            this.type = type;
        }

        public int getId() {
            return id;
        }

        public double getScore() {
            return score;
        }

        public double getDistance() {
            return distance;
        }

        public String getType() {
            return type;
        }
    }
}
