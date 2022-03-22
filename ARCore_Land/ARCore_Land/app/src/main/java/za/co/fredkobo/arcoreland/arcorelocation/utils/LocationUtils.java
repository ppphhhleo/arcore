package za.co.fredkobo.arcoreland.arcorelocation.utils;

import android.util.Log;

import java.util.ArrayList;

import za.co.fredkobo.arcoreland.arcorelocation.LocationMarker;
import za.co.fredkobo.arcoreland.arcorelocation.sensor.DeviceLocation;

/**
 * Created by John on 02/03/2018.
 */

public class LocationUtils {

    /**
     * Bearing in degrees between two coordinates.
     * [0-360] Clockwise
     *
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */


    public static double bearing(double lat1, double lon1, double lat2, double lon2) {

        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff = Math.toRadians(lon2 - lon1);
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);
        double bearing=(Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
        Log.i("Test","lat1:"+lat1+"lon1:"+lon1+"  lat2:"+  lat2+"  lon2:"+lon2+"  bearing:"+bearing);
        return bearing;
    }
    public static int getNumberDecimalDigits(double number) {
        String moneyStr = String.valueOf(number);
        String[] num = moneyStr.split("\\.");
        if (num.length == 2) {
            for (;;){
                if (num[1].endsWith("0")) {
                    num[1] = num[1].substring(0, num[1].length() - 1);
                }else {
                    break;
                }
            }
            return num[1].length();
        }else {
            return 0;
        }
    }
    /**
     * Distance in metres between two coordinates
     *
     * @param lat1
     * @param lat2
     * @param lon1
     * @param lon2
     * @param el1  - Elevation 1
     * @param el2  - Elevation 2
     * @return
     */
    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
    public static int[] getClearestMarker(ArrayList<LocationMarker> mLocationMarkers, DeviceLocation deviceLocation){
        double distance=999999999;
        int index=-1;
        double lat= deviceLocation.currentBestLocation.getLatitude();
        double lon=deviceLocation.currentBestLocation.getLongitude();
        for (int i = 0; i < mLocationMarkers.size();i++) {
            try {
                LocationMarker marker = mLocationMarkers.get(i);
                double markerDistance = distance(
                        marker.latitude,
                        lat,
                        marker.longitude,
                        lon,
                        0,
                        0);

                if(markerDistance<distance){
                    distance=markerDistance;
                    index=i;
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new int[]{index,(int)Math.round(distance)};
    }
}
