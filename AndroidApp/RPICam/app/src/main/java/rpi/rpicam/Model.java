package rpi.rpicam;

//another class to handle item's id and name
public class Model {

    public boolean cbShowing;
    public boolean cbChecked;
    public String filename;
    // constructor
    public Model(boolean cbShowing, boolean cbChecked, String filename) {
        this.cbShowing = cbShowing;
        this.cbChecked = cbChecked;
        this.filename = filename;
    }

}