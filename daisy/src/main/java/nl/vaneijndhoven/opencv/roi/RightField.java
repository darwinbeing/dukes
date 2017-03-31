package nl.vaneijndhoven.opencv.roi;

public class RightField extends RegionOfInterest{

    public RightField(double fraction) {
        super(1 - fraction, 0, fraction, 1);
    }
}
