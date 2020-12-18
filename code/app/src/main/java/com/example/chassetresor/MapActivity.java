package com.example.chassetresor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MapActivity extends AppCompatActivity implements LocationListener {
    private double latitude;
    private double longitude;
    private boolean firstTime=true;
    private boolean needMapUpdate=false;

    //Request code associé à la demande de permission de localisation:
    private static final int PERMS_CALL_ID = 1234; //Pour chaque demande d'activation de permission il faut un identifiant qui est unique.
    //Service de la plateforme android.
    private LocationManager locationManager;

    //On récupére le composant graphique le fragment
    private MapFragment mapFragment;
    private GoogleMap googleMap;
    private List<LatLng> indicesArray;

    private double[] hintList;
    private boolean[] hintTouched;

    private final static double MAX_RADIUS = 100; //En mètre

    private Button dashboardButton;


    WebSocketService wsService;
    ServiceConnection onService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        FragmentManager fragmentManager = getFragmentManager();
        mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.map);

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

        dashboardButton = findViewById(R.id.goToDashboard);

        indicesArray = new ArrayList<LatLng>();
        indicesArray.add(new LatLng(45.005031, 4.918539));

        hintList = new double[2];
        hintList[0] = 4.918539;
        hintList[1] = 45.005031;

        hintTouched = new boolean[(int) hintList.length/2];
    }

    public void mapButtonClicked(View view) {
        Intent intent = new Intent(getBaseContext(), CameraActivity.class);
        intent.putExtra("HINT_LIST", hintList);
        startActivity(intent);
    }

    /**
     * On s'abonne au fournisseur de données de localisation
     */
    @Override
    protected void onResume() {//onResume : start ou reprend une activité
        super.onResume();
        checkPermission();//Il faut checker es permission et s'abonner

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("HINT_TOUCHED")){ // vérifie qu'une valeur est associée à la clé “edittext”
                hintTouched = intent.getBooleanArrayExtra("HINT_TOUCHED"); // on récupère la valeur associée à la clé
            }
        }

        needMapUpdate = true;

        bindService(new Intent(this, WebSocketService.class), onService, Context.BIND_AUTO_CREATE);
    }

    private void checkPermission() {

        //Permet d'activer une permission, et donc d'afficher une pop-up qui permet de demander à l'utilisateur d'accepter ou non la permission de localisation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ //Si les permissions ne sont pas activées je lance une popup.
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION //J'ai deux permission que je dois activer accepter (popup)
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
        if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) { //Si sur ce location manager un fournisseur particulier est autorisé, LocationManger.GPS_Provider j'ai un capteur gps qui est activé
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000, 0, this); //Je m'abonne au event si j'ai un capteur GPS qui est activé, c'est mois qu'il faut notifier
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) { //Si sur ce location manager un fournisseur particulier est autorisé, LocationManger.GPS_Provider j'ai un capteur gps qui est activé
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this); //Je m'abonne au event si j'ai un capteur GPS qui est activé
        }

        //Je load la map que si les permissions sont données
        loadMap();
    }

    //Cette méthode sera lancée à chaque fois qu'une demande d'activation des permissions sera proposée             ActivityCompat.requestPermissions(this, new String[]{ //Si les permissions ne sont pas activées je lance une popup.
    //                    Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION //J'ai deux permission que je dois activer accepter (popup)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Le request code en parametre va nous permettre de savoir d'ou est ce que l'on vient ainsi on pourra dealer selon de la ou en vient.
        if (requestCode == PERMS_CALL_ID) {
            checkPermission();//Je reemande un appelle à checkPermission pour que ça m'abonne
        }
    }

    /**
     * On se désabonne des fournisseurs de données de localisation pour ne pas consommer du cpu pour rien
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this); //on se désabonne, c'est this, l'écouteur que je dois retirer de l'ensemble des providers
        }

        unbindService(onService);
    }

    @SuppressWarnings("MissingPermission")
    private void loadMap() {//Attention asynchrone on peut recevoir une première localisation sans que al map ne soirt prete d'ou le if dans onLocationChanged
        mapFragment.getMapAsync(new OnMapReadyCallback() {//je serais norifier quans tout sera pret
            @Override
            public void onMapReady(GoogleMap googleMap) {
                MapActivity.this.googleMap = googleMap;
                MapActivity.this.googleMap.setMyLocationEnabled(true);
            }
        });
    }

    @Override
    public void onLocationChanged(@NonNull Location location) //C'est appelé 10 fois toute les 30 secondes.
    {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        LatLng googleLocation = new LatLng(latitude, longitude);

        if (googleMap != null) {
            if (firstTime) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(googleLocation));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(googleLocation, 15));
                firstTime = false;
            }

            if (needMapUpdate) {
                for (int i = 0; i < indicesArray.size(); i++) {
                    if (hintTouched[i])
                        googleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(indicesArray.get(i).latitude, indicesArray.get(i).longitude))
                                .title("L'indice "+ i +" est ici"));
                }

                needMapUpdate = false;
            }

            LatLng nearestPoint = getNearestLocation(googleLocation, indicesArray);
            makeItVibrate(nearestPoint, googleLocation);
        }
        //googleMap.moveCamera(CameraUpdateFactory.newLatLng(googleLocation));//Centrer la caméra la position actuelle
        //Toast.makeText(this,"altitude " + latitude + "//" +"longitude " + longitude,Toast.LENGTH_LONG ).show();
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

    private double distanceBetween(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {
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

    private boolean arePointsNear(Location checkPoint, Location centerPoint,  double km) {
        double ky = 40000 / 360;
        double kx = Math.cos(Math.PI * centerPoint.getLatitude() / 180.0) * ky;
        double dx = Math.abs(centerPoint.getLongitude() - checkPoint.getLongitude()) * kx;
        double dy = Math.abs(centerPoint.getLatitude() - checkPoint.getLatitude()) * ky;
        return Math.sqrt(dx * dx + dy * dy) <= km;
    }

    @SuppressLint("MissingPermission")
    public void makeItVibrate(LatLng nearestPoint, LatLng googleLocation){
        if (nearestPoint != null) {
            if (distanceBetween(nearestPoint.latitude, googleLocation.latitude, nearestPoint.longitude, googleLocation.longitude, 0, 0) < 20) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(1000);
                }
            }
        }
    }

    public List<LatLng> getRandomLocation(LatLng point, int radius) {

        List<LatLng> randomPoints = new ArrayList<>();
        List<Float> randomDistances = new ArrayList<>();
        Location myLocation = new Location("");
        myLocation.setLatitude(point.latitude);
        myLocation.setLongitude(point.longitude);



        //This is to generate 10 random points
        for(int i = 0; i<10; i++) {
            double x0 = point.latitude;
            double y0 = point.longitude;

            Random random = new Random();

            // Convert radius from meters to degrees
            double radiusInDegrees = radius / 111000f;

            double u = random.nextDouble();
            double v = random.nextDouble();
            double w = radiusInDegrees * Math.sqrt(u);
            double t = 2 * Math.PI * v;
            double x = w * Math.cos(t);
            double y = w * Math.sin(t);

            // Adjust the x-coordinate for the shrinking of the east-west distances
            double new_x = x / Math.cos(y0);

            double foundLatitude = new_x + x0;
            double foundLongitude = y + y0;
            LatLng randomLatLng = new LatLng(foundLatitude, foundLongitude);
            randomPoints.add(randomLatLng);
            Location l1 = new Location("");
            l1.setLatitude(randomLatLng.latitude);
            l1.setLongitude(randomLatLng.longitude);
            randomDistances.add(l1.distanceTo(myLocation));
        }
        //Get nearest point to the centre
        //  int indexOfNearestPointToCentre = randomDistances.indexOf(Collections.min(randomDistances));
        //return randomPoints.get(indexOfNearestPointToCentre);
        return randomPoints;
    }

    public LatLng getNearestLocation(LatLng point, List<LatLng> pointList ) {

        //List<LatLng> pointList = new ArrayList<>();
        float[] minDist = new float[2];
        minDist[0] = -1;
        minDist[1] = Float.MAX_VALUE;
        Location myLocation = new Location("");
        myLocation.setLatitude(point.latitude);
        myLocation.setLongitude(point.longitude);

        //This is to generate 10 random points
        for (int i = 0; i < pointList.size(); i++) {
            if (!hintTouched[i]) {
                double x0 = point.latitude;
                double y0 = point.longitude;

                //Random random = new Random();

                // Convert radius from meters to degrees
                //double radiusInDegrees = radius / 111000f;

                // double u = random.nextDouble();
                //  double v = random.nextDouble();
                //  double w = radiusInDegrees * Math.sqrt(u);
                //double t = 2 * Math.PI * v;
                //   double x = w * Math.cos(t);
                //  double y = w * Math.sin(t);

                // Adjust the x-coordinate for the shrinking of the east-west distances
                //   double new_x = x / Math.cos(y0);

                // double foundLatitude = new_x + x0;
                // double foundLongitude = y + y0;
                // LatLng randomLatLng = new LatLng(foundLatitude, foundLongitude);
                //pointList.add(randomLatLng);
                Location l1 = new Location("");
                l1.setLatitude(pointList.get(i).latitude);
                l1.setLongitude(pointList.get(i).longitude);
                float dist = l1.distanceTo(myLocation);
                if (dist < minDist[1]) {
                    minDist[0] = i;
                    minDist[1] = dist;
                }
            }
        }
        //Get nearest point to the centre
        if (minDist[0] != -1){
            int indexOfNearestPointToCentre = (int) minDist[0];
            return pointList.get(indexOfNearestPointToCentre);
        }
        return null;
    }

    public void clickOnDashboardButton(View view) {
        Intent myIntent = new Intent(MapActivity.this, DashBoardActivity.class);
        myIntent.putExtra("HINT_TOUCHED", hintTouched);
        MapActivity.this.startActivity(myIntent);
    }
}