package nl.vaneijndhoven.opencv.lanedetection;

import nl.vaneijndhoven.geometry.Line;
import nl.vaneijndhoven.geometry.Point;
import nl.vaneijndhoven.navigation.plot.LaneOrientation;
import nl.vaneijndhoven.navigation.plot.StoppingZoneOrientation;
import nl.vaneijndhoven.objects.*;
import nl.vaneijndhoven.opencv.edgedectection.CannyEdgeDetector;
import nl.vaneijndhoven.opencv.linedetection.ProbabilisticHoughLinesLineDetector;
import nl.vaneijndhoven.opencv.objectdetection.LineExtractor;
import nl.vaneijndhoven.opencv.roi.RegionOfInterest;
import nl.vaneijndhoven.opencv.stopzonedetection.DefaultStoppingZoneDetector;
import nl.vaneijndhoven.opencv.tools.ImageCollector;
import nl.vaneijndhoven.opencv.tools.MemoryManagement;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

import static java.util.Arrays.asList;
import static nl.vaneijndhoven.opencv.mapper.PointMapper.toPoint;
import static nl.vaneijndhoven.opencv.tools.MemoryManagement.closable;

public class ImageLaneDetection {

    private final CannyEdgeDetector.Config cannyConfig;
    private final ProbabilisticHoughLinesLineDetector.Config lineDetectorConfig;

    public ImageLaneDetection(CannyEdgeDetector.Config cannyConfig, ProbabilisticHoughLinesLineDetector.Config lineDetectorConfig) {
        this.cannyConfig = cannyConfig;
        this.lineDetectorConfig = lineDetectorConfig;
    }

    public Map<String, Object> detectLane(Mat original, ImageCollector imageCollector) {
        try (MemoryManagement.ClosableMat<Mat> clsImage = closable(new RegionOfInterest(0, 0.45, 1, 0.55).region(original))) {
            Mat image = clsImage.get();
            Size imageSize = image.size();
            ViewPort viewPort = new ViewPort(new Point(0, 0), imageSize.width, imageSize.height);

            LineExtractor lineExtractor = new LineExtractor(
                    new CannyEdgeDetector(cannyConfig).withImageCollector(imageCollector),
                    new ProbabilisticHoughLinesLineDetector(lineDetectorConfig).withImageCollector(imageCollector)
            );

            Collection<Line> lines = lineExtractor.extract(image);

            Lane lane = new DefaultLaneDetector().detect(lines, viewPort);
            StoppingZone stoppingZone = new DefaultStoppingZoneDetector().detect(lines);

            LaneOrientation laneOrientation = new LaneOrientation(lane, viewPort);
            StoppingZoneOrientation stoppingZoneOrientation = new StoppingZoneOrientation(stoppingZone, lane, viewPort);

            Optional<Line> middle = laneOrientation.determineLaneMiddle();

            lane.getLeftBoundary().ifPresent(boundary -> drawLinesToImage(image, asList(boundary), new Scalar(0, 255, 0)));
            lane.getRightBoundary().ifPresent(boundary -> drawLinesToImage(image, asList(boundary), new Scalar(255, 128, 0)));
            middle.ifPresent(line -> drawLinesToImage(image, asList(line), new Scalar(0, 0, 255)));

            double distanceToStoppingZone = -1;
            double distanceToStoppingZoneEnd = -1;
            if (stoppingZone.getEntrance() != null) {
                stoppingZone.getEntrance().ifPresent(entrance -> drawLinesToImage(image, asList(entrance), new Scalar(255, 255, 0)));
                distanceToStoppingZone = stoppingZoneOrientation.determineDistanceToStoppingZone();
            }

            if (stoppingZone.getExit() != null) {
                stoppingZone.getExit().ifPresent(exit -> drawLinesToImage(image, asList(exit), new Scalar(0, 255, 255)));
                distanceToStoppingZoneEnd = stoppingZoneOrientation.determineDistanceToStoppingZoneEnd();
            }

            imageCollector.lines(image);

            double angle = laneOrientation.determineCurrentAngle();

            double distanceMiddle = laneOrientation.determineDistanceToMiddle();
            double distanceLeft = laneOrientation.distanceFromLeftBoundary();
            double distanceRight = laneOrientation.distanceFromRightBoundary();

            Map result = new HashMap<>();
            result.put("lane", lane);
            result.put("angle", angle);
            result.put("distanceMiddle", distanceMiddle);
            result.put("distanceLeft", distanceLeft);
            result.put("distanceRight", distanceRight);
            result.put("distanceToStoppingZone", distanceToStoppingZone);
            result.put("distanceToStoppingZoneEnd", distanceToStoppingZoneEnd);

            return result;
        }
    }

    private void drawLinesToImage(Mat image, Collection<Line> lines, Scalar color) {
        lines.stream().filter(Objects::nonNull).forEach(line -> Imgproc.line(image, toPoint(line.getPoint1()), toPoint(line.getPoint2()), color, 4));
    }

}