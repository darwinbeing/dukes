package nl.vaneijndhoven.opencv.roi;

public class FarField extends RegionOfInterest {

    public FarField(double fraction) {
        super(0, 0, 1, fraction);
    }

}
