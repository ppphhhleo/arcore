package za.co.fredkobo.arcoreland.arcorelocation.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.util.Log;
import android.view.WindowManager;

import com.google.ar.sceneform.ArSceneView;

import static android.hardware.SensorManager.AXIS_MINUS_X;
import static android.hardware.SensorManager.AXIS_MINUS_Y;
import static android.hardware.SensorManager.AXIS_X;
import static android.hardware.SensorManager.AXIS_Y;
import static android.hardware.SensorManager.getRotationMatrixFromVector;
import static android.hardware.SensorManager.remapCoordinateSystem;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;

/**
 * Created by John on 02/03/2018.
 */

public class DeviceOrientation implements SensorEventListener {


    private WindowManager windowManager;
    private SensorManager mSensorManager;
    private float orientation = 0f;
    float[] projectionMatrix = new float[16];
    private final static float Z_NEAR = 0.5f;
    private final static float Z_FAR = 10000;
    float[] rotatedProjectionMatrix = new float[16];


    public DeviceOrientation(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }
    public void setProjectionMatrix(ArSceneView mArSceneView){
        mArSceneView.getArFrame().getCamera().getProjectionMatrix(projectionMatrix,0,Z_NEAR,Z_FAR);

    }
    public float[] getRotatedProjectionMatrix(){
        return rotatedProjectionMatrix;
    }

    /**
     * Gets the device orientation in degrees from the azimuth (clockwise)
     *
     * @return orientation [0-360] in degrees
     */
    public float getOrientation() {
        return orientation;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
            case Sensor.TYPE_ROTATION_VECTOR:
                processSensorOrientation(event.values);
                break;
            default:
                Log.e("DeviceOrientation", "Sensor event type not supported");
                break;
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void processSensorOrientation(float[] rotation) {
        /*
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotation);
        final int worldAxisX;
        final int worldAxisY;

        switch (windowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                worldAxisX = SensorManager.AXIS_Z;
                worldAxisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                worldAxisX = SensorManager.AXIS_MINUS_X;
                worldAxisY = SensorManager.AXIS_MINUS_Z;
                break;
            case Surface.ROTATION_270:
                worldAxisX = SensorManager.AXIS_MINUS_Z;
                worldAxisY = SensorManager.AXIS_X;
                break;
            case Surface.ROTATION_0:
            default:
                worldAxisX = SensorManager.AXIS_X;
                worldAxisY = SensorManager.AXIS_Z;
                break;
        }
        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX,
                worldAxisY, adjustedRotationMatrix);

        // azimuth/pitch/roll
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
        this.orientation = ((float) Math.toDegrees(orientation[0]) + 360f) % 360f;
        this.pitch=((float) Math.toDegrees(orientation[1]) + 360f) % 360f;
        */

        float[] rotationMatrixFromVector = new float[16];
        float[] rotationMatrix = new float[16];
        getRotationMatrixFromVector(rotationMatrixFromVector, rotation);
        final int screenRotation = windowManager.getDefaultDisplay().getRotation();

        switch (screenRotation) {
            case ROTATION_90:
                remapCoordinateSystem(rotationMatrixFromVector,
                        AXIS_Y,
                        AXIS_MINUS_X, rotationMatrix);
                break;
            case ROTATION_270:
                remapCoordinateSystem(rotationMatrixFromVector,
                        AXIS_MINUS_Y,
                        AXIS_X, rotationMatrix);
                break;
            case ROTATION_180:
                remapCoordinateSystem(rotationMatrixFromVector,
                        AXIS_MINUS_X, AXIS_MINUS_Y,
                        rotationMatrix);
                break;
            default:
                remapCoordinateSystem(rotationMatrixFromVector,
                        AXIS_X, AXIS_Y,
                        rotationMatrix);
                break;
        }
        Matrix.multiplyMM(rotatedProjectionMatrix, 0, projectionMatrix, 0, rotationMatrix, 0);
        //Heading
        float[] orientation = new float[3];
        mSensorManager.getOrientation(rotatedProjectionMatrix, orientation);
        this.orientation = ((float) Math.toDegrees(orientation[0]) + 360f) % 360f;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Log.w("DeviceOrientation", "Orientation compass unreliable");
        }
    }

    public void resume() {
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void pause() {
        mSensorManager.unregisterListener(this);
    }
}