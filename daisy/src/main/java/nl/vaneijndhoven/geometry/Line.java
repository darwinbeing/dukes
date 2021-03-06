package nl.vaneijndhoven.geometry;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Math.*;
import static java.util.Optional.of;

public class Line {

    private final Point2D point1;
    private final Point2D point2;

    public Line(double[] coordinates) {
        this(new Point(coordinates[0], coordinates[1]), 
                new Point(coordinates[2], coordinates[3]));
    }

    public Line(Point2D point1, Point2D point2) {
        Objects.requireNonNull(point1, "point1 can not be null.");
        Objects.requireNonNull(point2, "point2 can not be null.");
        this.point1 = point1;
        this.point2 = point2;
    }

    public double length() { return point1.distance(point2);  }

    public double angleRad() {
        return atan2(point2.getY() - point1.getY(), point2.getX() - point1.getX());
    }

    public double angleDeg() {
        return toDegrees(angleRad());
    }

    public Point2D getPoint1() {
        return point1;
    }

    public Point2D getPoint2() {
        return point2;
    }

    public static Line average(Collection<Line> lines) {
        double sumAngleRad = 0;
        double sumx = 0;
        double sumy = 0;
        double sumxlen = 0;
        double sumylen = 0;

        for (Line line : lines) {
            sumAngleRad += line.angleRad();
            sumx += line.point1.getX() + line.point2.getX();
            sumy += line.point1.getY() + line.point2.getY();
            sumxlen += line.point2.getX() - line.point1.getX();
            sumylen += line.point2.getY() - line.point1.getY();
        }

        double avgAngleRad = sumAngleRad / lines.size();
        double avgx = sumx / (lines.size() * 2);
        double avgy = sumy / (lines.size() * 2);
        double avgxlen = sumxlen / lines.size();
        double avgylen = sumylen / lines.size();

        Point2D origin = new Point(avgx - 0.5 * avgxlen, avgy - 0.5 * avgylen);
        Point2D end = new Vector(avgxlen, avgylen).calculate(origin, 1);

        Line avgLine = new Line(origin, end);

        // System.out.println("avg rad: " + avgAngleRad + " line rad: " + avgLine.angleRad());

        return avgLine;
    }

    /**
     * Tests if a point is Left|On|Right of this line it it were infinite.
     * (See: Algorithm 1 "Area of Triangles and Polygons")
     * @param point The point to determine the position relative to this line.
     * @return     >0 for P2 left of this line
     *            =0 for P2  on this line
     *            <0 for P2  right of this line
     */
    private int howLeft(Point2D point) {
        Point2D start = point1;
        Point2D end = point2;
        return (int)((end.getX() - start.getX()) * (point.getY() - start.getY())
                - (point.getX() -  start.getX()) * (end.getY() - start.getY()) );
    }

    public boolean isLeftOfLine(Point2D point) {
        return howLeft(point) > 0;
    }

    public boolean isRightOfLine(Point2D point) {
        return howLeft(point) < 0;
    }

    public boolean existsOnLine(Point2D point) {
        if (point.getY() > Math.max(point1.getY(), point2.getY())) {
            // Point above line
            return false;
        }

        if (point.getY() < Math.min(point1.getY(), point2.getY())) {
            // Point below line
            return false;
        }

        return howLeft(point) == 0;
    }

    public Point2D bottomMost() {
        return point1.getY() > point2.getY() ? point1 : point2;
    }

    public Point2D leftMost() {
        return point1.getX() < point2.getX() ? point1 : point2;
    }

    public Point2D rightMost() {
        return point1.getX() > point2.getX() ? point1 : point2;
    }

    public Point2D topMost() {
        return point1.getY() < point2.getY() ? point1 : point2;
    }

    // 0 ---> 10
    // width 10
    // 0 + 5 = 5

    // 0 <--- 10
    // width -10

    public Point2D pointAt(double fraction) {
        double x = leftMost().getX() + width() * fraction;
        double y = topMost().getY() + height() * fraction;

        return new Point(x, y);
    }

    /**
     * Distance between @code{point1.x} and @code{point2.x}.
     * @return
     */
    public double width() {
        return abs(point1.getX() - point2.getX());
    }

    /**
     * Distance between @code{point1.y} and @code{point2.y}.
     * @return
     */
    public double height() {
        return abs(point1.getY() - point2.getY());
    }

    private boolean isBelow(Point2D point, Point2D nextVertexPoint) {
        return nextVertexPoint.getY()  > point.getY();
    }

    private boolean isAboveOrEqual(Point2D point, Point2D reference) {
        return reference.getY() <= point.getY();
    }

    @Override
    public String toString() {
        return point1 + " - " + point2;
    }

    public Optional<Point2D> intersect(Line other) {
        double x1 = point1.getX();
        double x2 = point2.getX();
        double x3 = other.point1.getX();
        double x4 = other.point2.getX();

        double y1 = point1.getY();
        double y2 = point2.getY();
        double y3 = other.point1.getY();
        double y4 = other.point2.getY();

        double determinant = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);

        if (determinant == 0) {
            // edge case: lines are parallel, there is no intersection
            return Optional.empty();
        }

        double x = ((x1*y2 - y1*x2) * (x3 - x4) - (x1 - x2) * (x3*y4 - y3*x4)) / determinant;
        double y = ((x1*y2 - y1*x2) * (y3 - y4) - (y1 - y2) * (x3*y4 - y3*x4)) / determinant;

        return of(new Point(x, y));
    }

    public double distance(Point2D point) {
        double x0 = point.getX();
        double y0 = point.getY();
        double x1 = point1.getX();
        double y1 = point1.getY();
        double x2 = point2.getX();
        double y2 = point2.getY();

        double determinant = sqrt(pow(y2 - y1, 2) + pow(x2 - x1, 2));

        return abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1) / determinant;
    }

    public static class Vector {

        private double x;
        private double y;

        public Vector(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Point2D calculate(Point2D origin, double delta) {
            return new Point(origin.getX() + delta * x, origin.getY() + delta * y);
        }

        public double calculateX(double deltaY) {
            return (deltaY / y) * x;
        }

        public double calculateY(double deltaX) {
            return (deltaX / x) * y;
        }

        public static Vector forY(double y) {
            return new Vector(0, y);
        }

        public static Vector forX(double x) {
            return new Vector(x, 0);
        }
    }
}