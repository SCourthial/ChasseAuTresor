package com.example.chassetresor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;

public class CameraActivity extends AppCompatActivity implements LocationListener {

    WebSocketService wsService;
    ServiceConnection onService;

    private ArFragment arFragment;
    private ArSceneView arSceneView;
    private LocationScene locationScene;
    private ModelRenderable redCubeRenderable, andyRenderable;
    private LocationMarker myHint;

    private double latitude, newLatitude;
    private double longitude, newLongitude;
    private double altitude, newAltitude;
    private boolean firstTime, catsCreated = false;
    private boolean hasFinishedLoading = false;

    private double[] hintList;
    private boolean[] hintTouched;

    //Request code associé à la demande de permission de localisation:
    private static final int PERMS_CALL_ID = 1234; //Pour chaque demande d'activation de permission il faut un identifiant qui est unique.
    //Service de la plateforme android.
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        onService = new ServiceConnection(){
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                wsService = ((WebSocketService.LocalBinder) service).getService();
                Log.i("on connection", "service connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                wsService = null;
                Log.i("on disconnection", "service disconnected");
            }

            @Override
            public void onBindingDied(ComponentName name) {
                wsService = null;
                Log.i("on binding death", "service binding dead");
            }

            @Override
            public void onNullBinding(ComponentName name) {
                wsService = null;
                Log.i("on binding null", "service binding null");
            }
        };

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("HINT_LIST")){ // vérifie qu'une valeur est associée à la clé “edittext”
                hintList = intent.getDoubleArrayExtra("HINT_LIST"); // on récupère la valeur associée à la clé
                hintTouched = new boolean[(int) hintList.length/2];
            }
        }

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arSceneView = arFragment.getArSceneView();

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material-> {
                            redCubeRenderable =
                                    ShapeFactory.makeCube(new Vector3(5f,5f,5f), new Vector3(0.0f,0.0f,0.0f), material);});

        CompletableFuture<ModelRenderable> andy = ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build();

        CompletableFuture.allOf(andy)
                .handle(
                        (notUsed, throwable) ->
                        {
                            if (throwable != null) {
                                return null;
                            }

                            try {
                                andyRenderable = andy.get();
                                hasFinishedLoading = true;
                            } catch (InterruptedException | ExecutionException ex) {
                            }
                            return null;
                        }
                );

        /*
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (redCubeRenderable == null) {
                        return;
                    }
                    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                        return;
                    }


                    //On créé le point d'encrage du modèle 3d
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    //On attache ensuite notre modèle au point d'encrage
                    TransformableNode Node = new TransformableNode(arFragment.getTransformationSystem());
                    Node.setParent(anchorNode);
                    Node.setRenderable(redCubeRenderable);
                    Node.select();
                }
        );

         */

        arSceneView
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {
                            if (!hasFinishedLoading) {
                                return;
                            }

                            if (locationScene == null) {
                                locationScene = new LocationScene(this, arSceneView);
                                locationScene.setRefreshAnchorsAsLocationChanges(true);

                                for (int i = 0; i < hintList.length / 2; i++) {
                                    LocationMarker hint = new LocationMarker(
                                            hintList[2*i],
                                            hintList[2*i+1],
                                            getHint(i));
                                    hint.setScaleModifier(0.1f);
                                    hint.setScalingMode(LocationMarker.ScalingMode.GRADUAL_TO_MAX_RENDER_DISTANCE);
                                    hint.setHeight(0f);

                                    locationScene.mLocationMarkers.add(hint);
                                }
                            }

                            if (locationScene != null) {
                                locationScene.processFrame(arSceneView.getArFrame());
                            }
                        });
    }

    private Node getHint(int i) {
        Node hint = new Node();
        hint.setRenderable(andyRenderable);
        Context c = this;
        hint.setOnTapListener((v, event) -> {
            Toast.makeText(
                    c, "Hint "+ i + " touched.", Toast.LENGTH_LONG)
                    .show();
            hintTouched[i] = true;
            wsService.addHint();
        });
        return hint;
    }

    public void cameraButtonClicked(View view) {
        Intent intent = new Intent(getBaseContext(), MapActivity.class);
        intent.putExtra("HINT_TOUCHED", hintTouched);
        startActivity(intent);
    }

    /**
     * On s'abonne au fournisseur de données de localisation
     */
    @Override
    protected void onResume() {//onResume : start ou reprend une activité
        super.onResume();
        checkPermission();//Il faut checker les permission et s'abonner

        if (locationScene != null) {
            locationScene.resume();
        }

        bindService(new Intent(this, WebSocketService.class), onService, Context.BIND_AUTO_CREATE);
    }

    private void checkPermission(){

        //Permet d'activer une permission, et donc d'afficher une pop-up qui permet de demander à l'utilisateur d'accepter ou non la permission de localisation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ //Si les permissions ne sont pas activées je lance une popup.
                    Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION //J'ai deux permission que je dois activer accepter (popup)
            }, PERMS_CALL_ID); //Demande de permission //Le request code est là parce qu'à différents endroits de mon code je peux demander  d'activer différentes permissions.
            //Dès qu'une permission est activée, on va être renvoyé vers une méthode bien précise, dans cette méthode là, qui réagira quelques soit le type et la permission qu'on a demandé à activer, je vais pouvoir récupérer le request code et tester si c'est bien le bon. on déclare une constante PERMS_CALL_ID = 1234; Pour chaque demande de permission il faut un request code, un identifiant unique.
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return; //appelle asynchrone continue
        }

        //Je demande à android de me récupérer un service de la plateforme android. Je récupère mon LOCATION_SERVICE
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { //Si sur ce location manager un fournisseur particulier est autorisé, LocationManger.GPS_Provider j'ai un capteur gps qui est activé
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this); //Je m'abonne au event si j'ai un capteur GPS qui est activé
        }

        //Je peux recevoir mais informations de localisation sur plusieurs provider.
        if(locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)){ //Si sur ce location manager un fournisseur particulier est autorisé, LocationManger.GPS_Provider j'ai un capteur gps qui est activé
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,1000,0,this); //Je m'abonne au event si j'ai un capteur GPS qui est activé, c'est mois qu'il faut notifier
        }
        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){ //Si sur ce location manager un fournisseur particulier est autorisé, LocationManger.GPS_Provider j'ai un capteur gps qui est activé
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this); //Je m'abonne au event si j'ai un capteur GPS qui est activé
        }
    }

    //Cette méthode sera lancée à chaque fois qu'une demande d'activation des permissions sera proposée             ActivityCompat.requestPermissions(this, new String[]{ //Si les permissions ne sont pas activées je lance une popup.
    //                    Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION //J'ai deux permission que je dois activer accepter (popup)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Le request code en parametre va nous permettre de savoir d'ou est ce que l'on vient ainsi on pourra dealer selon de la ou en vient.
        if(requestCode==PERMS_CALL_ID){
            checkPermission();//Je reemande un appelle à checkPermission pour que ça m'abonne
        }
    }

    /**
     * On se désabonne des fournisseurs de données de localisation pour ne pas consommer du cpu pour rien
     */
    @Override
    protected void onPause() {
        super.onPause();
        if(locationManager!=null){
            locationManager.removeUpdates(this); //on se désabonne, c'est this, l'écouteur que je dois retirer de l'ensemble des providers
        }

        unbindService(onService);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) //C'est appelé 10 fois toute les 30 secondes.
    {
        /*newLongitude=location.getLongitude();
        newLatitude=location.getLatitude();
        newAltitude=location.getAltitude();

        if (!catsCreated) {
            if (abs(longitude - newLongitude) < 0.00001 && abs(latitude - newLatitude) < 0.00001) {
                ArrayList<LocationMarker> catList = new ArrayList<LocationMarker>();
                for (int i = -5; i < 5; i++) {
                    catList.add(createHint(longitude + i * 0.01, latitude + i* 0.01, altitude - 10));
                }

                addHints(catList);

                Toast.makeText(this, "Hint créés!", Toast.LENGTH_LONG).show();

                catsCreated = true;
            } else {
                Toast.makeText(this, "Ne bougez pas trop", Toast.LENGTH_LONG).show();
            }
        }

        longitude=newLongitude;
        latitude=newLatitude;
        altitude=newAltitude;*/

        //Toast.makeText(this, "Altitude" + altitude + "Longitude" + longitude + "latitude" + latitude, Toast.LENGTH_LONG).show();
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    /*private void addHints(ArrayList<LocationMarker> hintLocationMarkers) {
        arSceneView
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {
                            if (!hasFinishedLoading) {
                                return;
                            }

                            if (locationScene == null) {
                                locationScene = new LocationScene(this, arSceneView);

                                Iterator<LocationMarker> it = hintLocationMarkers.iterator();
                                LocationMarker hintLocationMarker;
                                while(it.hasNext()){
                                    hintLocationMarker = it.next();
                                    locationScene.mLocationMarkers.add(hintLocationMarker);
                                }
                            }

                            if (locationScene != null) {
                                locationScene.processFrame(arSceneView.getArFrame());
                            }
                        });

    }*/

    /*private void addHint(LocationMarker hintLocationMarker) {
        arSceneView
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {

                            if (locationScene == null) {
                                locationScene = new LocationScene(this, arSceneView);

                                locationScene.mLocationMarkers.add(hintLocationMarker);
                            }

                            if (locationScene != null) {
                                locationScene.processFrame(arSceneView.getArFrame());
                            }
                        });
    }*/

    /*private LocationMarker createHint(int id, double longitude, double latitude, double altitude){
        LocationMarker hintLocationMarker = new LocationMarker(
                longitude,
                latitude,
                getHint(id));
        hintLocationMarker.setHeight((float) altitude);
        hintLocationMarker.setScaleModifier(1);

        return hintLocationMarker;
    }*/
}