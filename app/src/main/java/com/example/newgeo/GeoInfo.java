package com.example.newgeo;

public class GeoInfo {

    String info;
    int radius;
    double lat,longt;

    GeoInfo(){

    }

    GeoInfo(String info,int radius,double lat, double longt){
        this.info = info;
        this.radius = radius;
        this.lat = lat;
        this.longt = longt;
    }

    public String getInfo() {
        return info;
    }

    public int getRadius() {
        return radius;
    }

    public double getLat() {
        return lat;
    }

    public double getLongt() {
        return longt;
    }
}
