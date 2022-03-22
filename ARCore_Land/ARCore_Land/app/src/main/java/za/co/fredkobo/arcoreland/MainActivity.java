package za.co.fredkobo.arcoreland;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.gson.Gson;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import za.co.fredkobo.arcoreland.arcorelocation.LocationMarker;
import za.co.fredkobo.arcoreland.arcorelocation.LocationScene;
import za.co.fredkobo.arcoreland.arcorelocation.utils.ARLocationPermissionHelper;
import za.co.fredkobo.arcoreland.arcorelocation.utils.MapDialogFragment;


public class MainActivity extends AppCompatActivity {
    private boolean installRequested;
    private boolean hasFinishedLoading = false;
    private TextView show_log;
    private ArSceneView arSceneView;
    private ArFragment arFragment;
    private boolean threadStart=false;
    private boolean canDownload=false;
    private static List<Poi> poiList;

    private String prefix="@drawable/";
    private static String[] Crops=new String[]{"yumi","shuidao","xiaomai","gaoliang","baicai","qingcai","jiucai","huluobo","luobo","yangcong",
            "cong","hongshu","dadou","candou","xiangrikui","xihongshi","youcaizi","tudou","shanyao","huanggua"};
    private static List<String> CropsList=Arrays.asList(Crops);



    //variable for counting two successive up-down events;
    private int clickCount=0;
    //variable for storing the time of first click
    private long startTime;
    //variable for calculating the total time
    private long duration;
    //constant for defining the time duration between the click that can be considered as double-tap
    static final int MAX_DURATION = 100;

    // Renderables for this example
    private final List<ViewRenderable> layoutRenderables = new ArrayList<>();

    // Our ARCore-Location scene
    private LocationScene locationScene;

    private final List<CompletableFuture<ViewRenderable>> viewRenderables = new ArrayList<>();
    public void BuildView() {
        if(poiList==null||poiList.size()==0) {
            Log.i("DATA","size:0");
            return;
        }
        for (int i = 0; i < poiList.size(); i++) {
            Poi poi=poiList.get(i);
            String image=getImage(poi.getShortName());
            LayoutInflater inflater = LayoutInflater.from(this);

            View render = inflater.inflate(R.layout.renderable_layout, null);
            LinearLayout totallinearLayout=(LinearLayout)render.findViewById(R.id.render);
            LinearLayout imglinearLayout=totallinearLayout.findViewById(R.id.image) ;
            image=prefix+image;
            LinearLayout.LayoutParams imParams =
                    new LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            int imageResource = getResources().getIdentifier(image, null, getPackageName());
            ImageView imageview= new ImageView(this);
            TextView tv_master=totallinearLayout.findViewById(R.id.info).findViewById(R.id.tv_master);
            tv_master.setText("姓名： "+poi.getName()+" ");
            TextView tv_area=totallinearLayout.findViewById(R.id.info).findViewById(R.id.tv_area);
            tv_area.setText("面积： "+String.valueOf(poi.getArea())+"亩");
            TextView tv_target=totallinearLayout.findViewById(R.id.info).findViewById(R.id.tv_target);
            tv_target.setText("标的： "+poi.getTarget()+" ");
            imageview.setImageResource(imageResource);
            imglinearLayout.addView(imageview,imParams);

            CompletableFuture<ViewRenderable> exampleLayout =
                    ViewRenderable.builder()
                            .setView(this, totallinearLayout)
                            .build();
            viewRenderables.add(exampleLayout);

        }

        for (CompletableFuture<ViewRenderable> renderableCompletableFuture : viewRenderables) {
            CompletableFuture.allOf(
                    renderableCompletableFuture)
                    .handle(
                            (notUsed, throwable) -> {
                                // When you build a Renderable, Sceneform loads its resources in the background while
                                // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                                // before calling get().

                                if (throwable != null) {
                                    DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                    return null;
                                }

                                try {
                                    layoutRenderables.add(renderableCompletableFuture.get());
                                    canDownload = true;

                                } catch (InterruptedException | ExecutionException ex) {
                                    DemoUtils.displayError(this, "Unable to load renderables", ex);
                                }

                                return null;
                            });
        }


    }
    private Handler mHandler;
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arSceneView=arFragment.getArSceneView();
        show_log=findViewById(R.id.tv_log);

        //show the log info to debug
        mHandler = new Handler() {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0x11) {
                    Bundle bundle = msg.getData();
                    String answer = bundle.getString("info");

                    if (answer != null) {
                        //更新UI
                        show_log.setText(answer);
                    } else {
                        show_log.setText("there is no log info.");

                    }
                }
            }
        };

        //handle the resources from the server.
        Handler handler = new Handler(){
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle data = msg.getData();
                String val = data.getString("value");
                Gson gson = new Gson();
                Poi[] pois = gson.fromJson(val, Poi[].class);
                poiList = Arrays.asList(pois);
                Log.i("DATA","poi has:"+poiList.size());
                BuildView();
                //poiList.clear();
            }
        };





        //get poi from the server.
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String url = "http://192.168.43.219:8090/?"; //url address
                double latitude = locationScene.getCurrentLat();
                double longitude = locationScene.getCurrentLon();
                int radius = 50;
                url = url + "lat=" + String.valueOf(latitude) + "&lon=" + String.valueOf(longitude) + "&radius=" + String.valueOf(radius);
                String result = "";
                BufferedReader in = null;

                try {
                    URL realUrl = new URL(url);
                    // 打开和URL之间的连接
                    HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(6000);
                    // 设置通用的请求属性
                    connection.setRequestProperty("accept", "*/*");
                    connection.setRequestProperty("connection", "Keep-Alive");
                    connection.setRequestProperty("user-agent",
                            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
                    // 建立实际的连接
                    connection.connect();
                    // 获取所有响应头字段
                    in = new BufferedReader(new InputStreamReader(
                            connection.getInputStream()));
                    String line;

                    while ((line = in.readLine()) != null) {
                        result += line;
                    }

                    Message msg = new Message();
                    Bundle data = new Bundle();
                    data.putString("value",result);
                    msg.setData(data);
                    handler.sendMessage(msg);

                } catch (Exception e) {
                    System.out.println("发送GET请求出现异常！" + e);
                    e.printStackTrace();
                }
                // 使用finally块来关闭输入流
                finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }

            }
        };

        arSceneView
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {
                            Frame frame = arSceneView.getArFrame();
                            if (frame == null) {
                                return;
                            }

                            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                                return;
                            }

                            if (locationScene == null) {

                                //步骤一：添加一个FragmentTransaction的实例
                                FragmentManager fragmentManager = getSupportFragmentManager();
                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                //步骤二：用add()方法加上Fragment的对象rightFragment
                                MapDialogFragment rightFragment = new MapDialogFragment();
                                transaction.add(R.id.right, rightFragment);
                                //步骤三：调用commit()方法使得FragmentTransaction实例的改变生效
                                transaction.commit();
                                locationScene = new LocationScene(this, arSceneView);
                                //locationScene.setAnchorRefreshInterval(120);
                            }

                            if(locationScene.markersNeedRefresh()&&!threadStart){
                                locationScene.clearMarkers();
                                threadStart=true;
                                hasFinishedLoading=false;
                                new Thread(runnable).start();

                            }
                            if(canDownload){
                                int i=0;
                                for (Poi poi : poiList) {
                                    if(i>=poiList.size())
                                        break;
                                    TransformableNode base=new TransformableNode(arFragment.getTransformationSystem());
                                    base.setRenderable(layoutRenderables.get(i));
                                    Context c = this;
                                    // Add  listeners etc here
                                    View eView = layoutRenderables.get(i).getView();
                                    View.OnTouchListener myOnTouchListener=new View.OnTouchListener(){
                                        @Override
                                        public boolean onTouch (View v, MotionEvent event)
                                        {
                                            switch(event.getAction() & MotionEvent.ACTION_MASK)
                                            {
                                                case MotionEvent.ACTION_DOWN:
                                                    startTime = System.currentTimeMillis();
                                                    clickCount++;
                                                    break;
                                                case MotionEvent.ACTION_UP:
                                                    long time = System.currentTimeMillis() - startTime;
                                                    duration=  duration + time;
                                                    if(clickCount == 2)
                                                    {
                                                        if(duration<= MAX_DURATION)
                                                        {
                                                            AlertDialog.Builder builder = new AlertDialog.Builder(c);
                                                            Context dialogContext = builder.getContext();
                                                            LayoutInflater inflater = LayoutInflater.from(dialogContext);
                                                            View alertView = inflater.inflate(R.layout.table, null);
                                                            builder.setView(alertView);
                                                            TableLayout tableLayout = (TableLayout)alertView.findViewById(R.id.table_dialog);

                                                            TableRow frow=tableLayout.findViewById(R.id.first);
                                                            TextView firstView=frow.findViewById(R.id.master);
                                                            firstView.setText(poi.getName());

                                                            TableRow srow=tableLayout.findViewById(R.id.second);
                                                            TextView secondView=srow.findViewById(R.id.area);
                                                            secondView.setText(String.valueOf(poi.getArea())+"亩");

                                                            TableRow trow=tableLayout.findViewById(R.id.third);
                                                            TextView thirdView=trow.findViewById(R.id.target);
                                                            thirdView.setText(poi.getTarget());

                                                            TableRow forthrow=tableLayout.findViewById(R.id.forth);
                                                            TextView forthView=forthrow.findViewById(R.id.address);
                                                            forthView.setText(poi.getAddress());

                                                            builder.setCancelable(true);
                                                            AlertDialog alertDialog = builder.create();
                                                            Window window=alertDialog.getWindow();
                                                            //这一句消除白块
                                                            window.setBackgroundDrawable(new BitmapDrawable());
                                                            Button button1 = (Button) alertView.findViewById(R.id.confirm);
                                                            button1.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View v) {
                                                                    alertDialog.dismiss();
                                                                }
                                                            });

                                                            alertDialog.setCanceledOnTouchOutside(true);
                                                            alertDialog.show();
                                                        }
                                                        clickCount = 0;
                                                        duration = 0;
                                                        break;
                                                    }
                                            }
                                            return true;
                                        }
                                    };
                                    eView.setOnTouchListener(myOnTouchListener);

                                    LocationMarker layoutLocationMarker = new LocationMarker(
                                            poi.getLongitude(), poi.getLatitude(),
                                            base
                                    );
                                    // Adding the marker
                                    locationScene.mLocationMarkers.add(layoutLocationMarker);
                                    i++;
                                }
                                System.out.println("marker size:"+locationScene.mLocationMarkers.size());
                                canDownload=false;
                                hasFinishedLoading=true;
                                locationScene.setLastLocation();
                                threadStart=false;
                                for(Poi poi : poiList){
                                    poi=null;
                                }
                                poiList=new ArrayList<>();
                            }
                            if (locationScene != null&&hasFinishedLoading) {
                                locationScene.processFrame(frame);
                            }

                        });
        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.
        ARLocationPermissionHelper.requestPermission(this);
        new Thread() {  // 2. 开启线程来捕获日志
            @Override
            public void run() {
                Process mLogcatProc = null;
                BufferedReader reader = null;
                try {
                    //获取logcat日志信息
                    mLogcatProc = Runtime.getRuntime().exec(new String[]{"logcat", "LocationScene:I *:S"});
                    //mLogcatProc = Runtime.getRuntime().exec(new String[]{"logcat", "Test:I *:S"});
                    reader = new BufferedReader(new InputStreamReader(mLogcatProc.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        //logcat打印信息在这里可以监听到
                        Message msg3 = new Message();
                        Bundle bundle = new Bundle();
                        msg3.what = 0x11;
                        // 使用looper 把给界面一个显示
                        bundle.putString("info", line);
                        msg3.setData(bundle);

                        // 3. 发送日志通知主线程更新UI
                        mHandler.sendMessage(msg3);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }


    /**
     * Make sure we call locationScene.resume();
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                DemoUtils.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            DemoUtils.displayError(this, "Unable to get camera", ex);
            finish();
            return;
        }
    }

    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    public void onPause() {
        super.onPause();

        if (locationScene != null) {
            locationScene.pause();
        }

        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private String getImage(String name){
        if(CropsList.contains(name))
            return name;
        return "nongzuowu";

    }


}
