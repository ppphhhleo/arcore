
package za.co.fredkobo.arcoreland.arcorelocation;

import android.app.Activity;
import android.location.Location;
import android.os.Handler;
import android.util.Log;


import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.math.Vector3;


import java.util.ArrayList;
import java.util.List;


import za.co.fredkobo.arcoreland.arcorelocation.rendering.LocationNode;
import za.co.fredkobo.arcoreland.arcorelocation.sensor.DeviceLocation;
import za.co.fredkobo.arcoreland.arcorelocation.sensor.DeviceLocationChanged;
import za.co.fredkobo.arcoreland.arcorelocation.sensor.DeviceOrientation;
import za.co.fredkobo.arcoreland.arcorelocation.utils.LocationUtils;


/**
 * Created by John on 02/03/2018.
 */

public class LocationScene {

    private float RENDER_DISTANCE = 70f;
    public ArSceneView mArSceneView;
    public DeviceLocation deviceLocation;
    public DeviceOrientation deviceOrientation;
    public Activity context;
    private int LocationNeedRefresh=50;
    public ArrayList<LocationMarker> mLocationMarkers = new ArrayList<>();
    // Anchors are currently re-drawn on an interval. There are likely better
    // ways of doing this, however it's sufficient for now.
    private int anchorRefreshInterval = 1000 * 3; // 3 seconds
    // Limit of where to draw markers within AR scene.
    // They will auto scale, but this helps prevents rendering issues
    private int distanceLimit = 70;
    private boolean offsetOverlapping = false;
    private boolean removeOverlapping = false;
    // Bearing adjustment. Can be set to calibrate with true north
    private int bearingAdjustment = 0;
    private String TAG = "LocationScene";
    private boolean anchorsNeedRefresh = true;
    private boolean minimalRefreshing = false;
    private boolean refreshAnchorsAsLocationChanges = false;
    private Location lastLocation;
    private boolean lastLocationInit=false;


    private static  int times=0;

    private Handler mHandler = new Handler();

    Runnable anchorRefreshTask = new Runnable() {
        @Override
        public void run() {
            anchorsNeedRefresh = true;
            mHandler.postDelayed(anchorRefreshTask, anchorRefreshInterval);
        }
    };
    private boolean debugEnabled = false;
    private Session mSession;
    private DeviceLocationChanged locationChangedEvent;

    public LocationScene(Activity context,ArSceneView mArSceneView) {
        this.context = context;
        this.mSession = mArSceneView.getSession();
        this.mArSceneView = mArSceneView;
        startCalculationTask();
        deviceLocation = new DeviceLocation(context, this);
        lastLocation=new Location("");
        deviceOrientation = new DeviceOrientation(context);
        deviceOrientation.setProjectionMatrix(mArSceneView);
        deviceOrientation.resume();
        //test();
    }
    public double getCurrentLat(){
        return deviceLocation.currentBestLocation.getLatitude();
    }
    public double getCurrentLon(){
        return deviceLocation.currentBestLocation.getLongitude();
    }

    public void setLastLocation(){
        if(deviceLocation!=null&&deviceLocation.currentBestLocation!=null) {
            lastLocation.setLongitude(deviceLocation.currentBestLocation.getLongitude());
            lastLocation.setLatitude(deviceLocation.currentBestLocation.getLatitude());
            //System.out.println("lat"+lastLocation.getLatitude()+" lon:"+lastLocation.getLongitude());
        }

    }



    public boolean isDebugEnabled() {
        return debugEnabled;
    }


    public boolean minimalRefreshing() {
        return minimalRefreshing;
    }

    public void setMinimalRefreshing(boolean minimalRefreshing) {
        this.minimalRefreshing = minimalRefreshing;
    }

    public boolean refreshAnchorsAsLocationChanges() {
        return refreshAnchorsAsLocationChanges;
    }

    public void setRefreshAnchorsAsLocationChanges(boolean refreshAnchorsAsLocationChanges) {
        if (refreshAnchorsAsLocationChanges) {
            stopCalculationTask();
        } else {
            startCalculationTask();
        }
        refreshAnchors();
        this.refreshAnchorsAsLocationChanges = refreshAnchorsAsLocationChanges;
    }

    /**
     * Get additional event to run as device location changes.
     * Save creating extra sensor classes
     *
     * @return
     */
    public DeviceLocationChanged getLocationChangedEvent() {
        return locationChangedEvent;
    }

    /**
     * Set additional event to run as device location changes.
     * Save creating extra sensor classes
     */
    public void setLocationChangedEvent(DeviceLocationChanged locationChangedEvent) {
        this.locationChangedEvent = locationChangedEvent;
    }

    public int getAnchorRefreshInterval() {
        return anchorRefreshInterval;
    }

    /**
     * Set the interval at which anchors should be automatically re-calculated.
     *
     * @param anchorRefreshInterval
     */
    public void setAnchorRefreshInterval(int anchorRefreshInterval) {
        this.anchorRefreshInterval = anchorRefreshInterval;
        stopCalculationTask();
        startCalculationTask();
    }

    public void clearMarkers() {
        for (LocationMarker lm : mLocationMarkers) {
            if (lm.anchorNode != null) {
                lm.anchorNode.getAnchor().detach();
                lm.anchorNode.setEnabled(false);
                lm.anchorNode = null;
            }

        }
        mLocationMarkers = new ArrayList<>();
    }

    /**
     * The distance cap for distant markers.
     * ARCore doesn't like markers that are 2000km away :/
     *
     * @return
     */
    public int getDistanceLimit() {
        return distanceLimit;
    }

    /**
     * The distance cap for distant markers.
     * Render distance limit is 30 meters, impossible to change that for now
     * https://github.com/google-ar/sceneform-android-sdk/issues/498
     */
    public void setDistanceLimit(int distanceLimit) {
        this.distanceLimit = distanceLimit;
    }

    public boolean shouldOffsetOverlapping() {
        return offsetOverlapping;
    }

    public boolean shouldRemoveOverlapping() {
        return removeOverlapping;
    }

    /**
     * Attempts to raise markers vertically when they overlap.
     * Needs work!
     *
     * @param offsetOverlapping
     */
    public void setOffsetOverlapping(boolean offsetOverlapping) {
        this.offsetOverlapping = offsetOverlapping;
    }


    /**
     * Remove farthest markers when they overlap
     *
     * @param removeOverlapping
     */
    public void setRemoveOverlapping(boolean removeOverlapping) {
        this.removeOverlapping = removeOverlapping;

//        for (LocationMarker mLocationMarker : mLocationMarkers) {
//            LocationNode anchorNode = mLocationMarker.anchorNode;
//            if (anchorNode != null) {
//                anchorNode.setEnabled(true);
//            }
//        }
    }

    public void processFrame(Frame frame) {
        refreshAnchorsIfRequired(frame);
    }

    /**
     * Force anchors to be re-calculated
     */
    public void refreshAnchors() {
        anchorsNeedRefresh = true;
    }

    private void refreshAnchorsIfRequired(Frame frame) {
        if (!anchorsNeedRefresh) {
            return;
        }

        //Log.i(TAG, "Refreshing anchors...");

        if (deviceLocation == null || deviceLocation.currentBestLocation == null || LocationUtils.getNumberDecimalDigits(deviceLocation.currentBestLocation.getLatitude())<6) {
            Log.i(TAG, "Location not yet established.has markers:"+mLocationMarkers.size());
            return;
        }
        anchorsNeedRefresh = false;
        try{
                int[] MinMarker=LocationUtils.getClearestMarker(mLocationMarkers,deviceLocation);
                int markerDistance=MinMarker[1];
                int index=MinMarker[0];

                if(index==-1)
                    return;
                final LocationMarker marker=mLocationMarkers.get(index);
                //Log.i(TAG,"current latitude:"+deviceLocation.currentBestLocation.getLatitude()+"  current longitude:"+deviceLocation.currentBestLocation.getLongitude());

                if (markerDistance > marker.getOnlyRenderWhenWithin()) {
                    // Don't render if this has been set and we are too far away.
                    Log.i(TAG, "Not rendering. Marker distance: " + markerDistance
                            + " Max render distance: " + marker.getOnlyRenderWhenWithin());
                    System.out.println("mLocationMarker size:"+mLocationMarkers.size()+" index:"+index+" distance big");
                    return;
                }
                if(markerDistance==0){
                    Log.i(TAG,"Not rendering.Marker distance is zero");
                    return;
                }

                if(marker.getHasRendering())
                    return;

                float bearing = (float) LocationUtils.bearing(
                        deviceLocation.currentBestLocation.getLatitude(),
                        deviceLocation.currentBestLocation.getLongitude(),
                        marker.latitude,
                        marker.longitude
               );
                float markerBearing = bearing - deviceOrientation.getOrientation();

                // Bearing adjustment can be set if you are trying to
                // correct the heading of north - setBearingAdjustment(10)
                markerBearing = markerBearing + bearingAdjustment + 360;
                markerBearing = markerBearing % 360;



                double rotation = Math.floor(markerBearing);


                // When pointing device upwards (camera towards sky)
                // the compass bearing can flip.
                // In experiments this seems to happen at pitch~=-25
                //if (deviceOrientation.pitch > -25)
                //rotation = rotation * Math.PI / 180;

                int renderDistance = markerDistance;

                // Limit the distance of the Anchor within the scene.
                // Prevents rendering issues.


                if (renderDistance > distanceLimit)
                    renderDistance = distanceLimit;



                // Adjustment to add markers on horizon, instead of just directly in front of camera
                double heightAdjustment = 0;
                // Math.round(renderDistance * (Math.tan(Math.toRadians(deviceOrientation.pitch)))) - 1.5F;

                // Raise distant markers for better illusion of distance
                // Hacky - but it works as a temporary measure
                int cappedRealDistance = markerDistance > 500 ? 500 : markerDistance;
                if (renderDistance != markerDistance)
                    heightAdjustment += 0.005F * (cappedRealDistance - renderDistance);
                float z = -Math.min(renderDistance, RENDER_DISTANCE);
                //float z=-renderDistance;

                double rotationRadian = Math.toRadians(rotation);
                //rotationRadian=0f;

                float zRotated = (float) (z * Math.cos(rotationRadian));
                float xRotated = (float) -(z * Math.sin(rotationRadian));

                //ty() returns the y axis of the translation,units are meter
                float y = frame.getCamera().getDisplayOrientedPose().ty() + (float) heightAdjustment;

                if (marker.anchorNode != null && marker.anchorNode.getAnchor() != null) {
                    marker.anchorNode.getAnchor().detach();
                    marker.anchorNode.setAnchor(null);
                    marker.anchorNode.setEnabled(false);
                    marker.anchorNode = null;
                }

                Pose translation = Pose.makeTranslation(xRotated, y, zRotated);
                Pose newp=frame.getCamera()
                        .getDisplayOrientedPose()
                        .compose(translation)
                        .extractTranslation();
                Vector3 worldcoord=new Vector3(newp.tx(),newp.ty(),newp.tz());
                Vector3 pt=mArSceneView.getScene().getCamera().worldToScreenPoint(worldcoord);
                List<HitResult> hits;
                hits = frame.hitTest(pt.x,pt.y);

                for (HitResult hit : hits) {
                    Trackable trackable = hit.getTrackable();
                    if (trackable instanceof Plane &&
                            ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {

                        Anchor newAnchor=hit.createAnchor();
                        Log.i(TAG,"x:"+pt.x+" y:"+pt.y+" hits:"+hits.size()+" times:"+(times++)+" index:"+index);
                        marker.anchorNode = new LocationNode(newAnchor, marker, this);
                        marker.anchorNode.setParent(mArSceneView.getScene());
                       // marker.anchorNode.getRenderable().setShadowCaster(false);
                       // marker.anchorNode.getRenderable().setShadowReceiver(false);
                        marker.node.setParent(marker.anchorNode);
                        marker.node.getScaleController().setMinScale(0.5f);
                        marker.node.getScaleController().setMaxScale(1.0f);
                        marker.node.select();
                        marker.setHasRendering(true);
                        break;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

         //this is bad, you should feel bad
         //Log.i(TAG,"have finished once computation.");

         System.gc();
     }
     /*
     * Adjustment for compass bearing.
     *
     * @return
     */
    public int getBearingAdjustment() {
        return bearingAdjustment;
    }

    /**
     * Adjustment for compass bearing.
     * You may use this for a custom method of improving precision.
     *
     * @param i
     */
    public void setBearingAdjustment(int i) {
        bearingAdjustment = i;
        anchorsNeedRefresh = true;
    }

    /**
     * Resume sensor services. Important!
     */
    public void resume() {
        deviceOrientation.resume();
        deviceLocation.resume();
    }

    /**
     * Pause sensor services. Important!
     */
    public void pause() {
        deviceOrientation.pause();
        deviceLocation.pause();
    }

    void startCalculationTask() {
        anchorRefreshTask.run();
    }

    void stopCalculationTask() {
        mHandler.removeCallbacks(anchorRefreshTask);
    }

    public boolean markersNeedRefresh(){
        if(deviceLocation==null||deviceLocation.currentBestLocation==null)
            return false;
        if(!lastLocationInit){
            lastLocationInit=true;
            return true;
        }
        if(lastLocation.getLongitude()==0&&lastLocation.getLatitude()==0)
            return false;
        int dis=(int)LocationUtils.distance(lastLocation.getLatitude(),deviceLocation.currentBestLocation.getLatitude(),lastLocation.getLongitude(),deviceLocation.currentBestLocation.getLongitude(),0,0);
        if(dis>=LocationNeedRefresh) {
            //System.out.println("dis:"+dis);
            return true;
        }
        return false;
    }

}
