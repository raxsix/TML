package eu.raxsix.tml.pojo;


import com.google.android.gms.maps.model.Marker;

import java.io.Serializable;

public class User implements Serializable {

    boolean web;
    private String name;
    private String distance;
    private Marker locationMarker;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isWeb() {
        return web;
    }

    public void setWeb(boolean web) {
        this.web = web;
    }

    public Marker getLocationMarker() {
        return locationMarker;
    }

    public Marker setLocationMarker(Marker locationMarker) {
        this.locationMarker = locationMarker;
        return locationMarker;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {

        if (distance != null) {

            return getName() + " - " + distance;
        }

        return getName();
    }
}
