package com.example.alimentaacao.data.firebase;

import com.google.firebase.firestore.GeoPoint;

public class GeoUtil {
    public static double distanceKm(GeoPoint a, GeoPoint b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        double R = 6371.0;
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double lat1 = Math.toRadians(a.getLatitude());
        double lat2 = Math.toRadians(b.getLatitude());
        double sinDLat = Math.sin(dLat/2), sinDLon = Math.sin(dLon/2);
        double h = sinDLat*sinDLat + Math.cos(lat1)*Math.cos(lat2)*sinDLon*sinDLon;
        return 2*R*Math.asin(Math.sqrt(h));
    }
}
