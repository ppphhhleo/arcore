package za.co.fredkobo.arcoreland.arcorelocation;

import com.google.ar.sceneform.ux.TransformableNode;

import za.co.fredkobo.arcoreland.arcorelocation.rendering.LocationNode;

/**
 * Created by John on 02/03/2018.
 */

public class LocationMarker {

    // Location in real-world terms
    public double longitude;
    public double latitude;

    // Location in AR terms
    public LocationNode anchorNode;

    // Node to render
    public TransformableNode node;

    // boolean to keep static after the first rendering
    private boolean hasRendering=false;


    private float scaleModifier = 1F;
    private float height = 0F;
    private int onlyRenderWhenWithin = 70; //Integer.MAX_VALUE;
    private ScalingMode scalingMode = ScalingMode.GRADUAL_FIXED_SIZE;
    private float gradualScalingMinScale = 0.2F;
    private float gradualScalingMaxScale = 0.5F;

    public LocationMarker(double longitude, double latitude, TransformableNode node) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.node = node;
    }

    public float getGradualScalingMinScale() {
        return gradualScalingMinScale;
    }

    public void setGradualScalingMinScale(float gradualScalingMinScale) {
        this.gradualScalingMinScale = gradualScalingMinScale;
    }

    public boolean getHasRendering(){
        return hasRendering;
    }
    public void setHasRendering(boolean hasRendering){
        this.hasRendering=hasRendering;

    }
    public float getGradualScalingMaxScale() {
        return gradualScalingMaxScale;
    }

    public void setGradualScalingMaxScale(float gradualScalingMaxScale) {
        this.gradualScalingMaxScale = gradualScalingMaxScale;
    }


    /**
     * Only render this marker when within [onlyRenderWhenWithin] metres
     *
     * @return - metres or -1
     */
    public int getOnlyRenderWhenWithin() {
        return onlyRenderWhenWithin;
    }

    /**
     * Only render this marker when within [onlyRenderWhenWithin] metres
     *
     * @param onlyRenderWhenWithin - metres
     */
    public void setOnlyRenderWhenWithin(int onlyRenderWhenWithin) {
        this.onlyRenderWhenWithin = onlyRenderWhenWithin;
    }

    /**
     * Height based on camera height
     *
     * @return - height in metres
     */
    public float getHeight() {
        return height;
    }

    /**
     * Height based on camera height
     *
     * @param height - height in metres
     */
    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * How the markers should scale
     *
     * @return - ScalingMode
     */
    public ScalingMode getScalingMode() {
        return scalingMode;
    }

    /**
     * Whether the marker should scale, regardless of distance.
     *
     * @param scalingMode - ScalingMode.X
     */
    public void setScalingMode(ScalingMode scalingMode) {
        this.scalingMode = scalingMode;
    }

    /**
     * Scale multiplier
     *
     * @return - multiplier
     */
    public float getScaleModifier() {
        return scaleModifier;
    }

    /**
     * Scale multiplier
     *
     * @param scaleModifier - multiplier
     */
    public void setScaleModifier(float scaleModifier) {
        this.scaleModifier = scaleModifier;
    }

    /**
     * Called on each frame
     *
     * @return - LocationNodeRender (event)
     */

    public enum ScalingMode {
        FIXED_SIZE_ON_SCREEN,
        NO_SCALING,
        GRADUAL_TO_MAX_RENDER_DISTANCE,
        GRADUAL_FIXED_SIZE
    }

}
