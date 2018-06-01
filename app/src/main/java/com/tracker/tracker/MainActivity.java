package com.tracker.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.CardView;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.thomashaertel.widget.MultiSpinner;
import com.tracker.tracker.Modelos.Contacto;
import com.tracker.tracker.Modelos.Usuario;
import com.tracker.tracker.tareas.ProfilePicture;

import java.util.ArrayList;

/**
 * Controlador de la actividad principal
 * Esta clase configura el menú, permite crear viaje, maneja el tema de la ubicación
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private static final int PLACE_PICKER_REQUEST = 2;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";

    private FirebaseUser user;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Datos de Ubicación
    private SettingsClient settingsClient;
    private FusedLocationProviderClient locationProviderClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private Boolean requestingLocationUpdate;

    // Botones
    private FloatingActionButton fabAdd, fabAddPerson, fabAddLocation;

    // Animaciones
    private Animation fabOpen, fabClose, fabRotateClockwise, fabRotateCounter;
    private boolean isOpen = false;

    //Opciones en Spinner
    private MultiSpinner spinner;
    private ArrayList<Contacto> contactos;
    private ArrayAdapter<String> adapter;

    // Viaje
    private Location currentLocation;
    private Location destination;
    private Contacto contacto;
    private Place placeDestionation;
    private CardView tripDescription;
    private boolean isViajando = false;

    private Usuario usuario;

    /**
     * Método onCreate:
     * @param savedInstanceState {Bundle}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.firebaseConfig();
        this.getUserData();

        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.navigationConfig();
        this.permissionConfig();

        updateValuesFromBundle(savedInstanceState);

        // Verificar GPS
        this.requestingLocationUpdate = true;
        this.settingsClient = LocationServices.getSettingsClient(this);
        this.locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        this.contactos = new ArrayList<>();
        this.fabConfig();
        this.createLocationCallback();
        this.createLocationRequest();
        this.buildLocationSettingsRequest();
        this.updateUI();
        this.startLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.spinnerConfig();
        this.adapter.notifyDataSetChanged();
    }

    @Override
    public void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Método: getUserData: Este método se encarga de obtener los datos del usuario de la DB y guardarlos en el modelo
     */
    private void getUserData() {
        this.usuario = Usuario.getUsuario(this.db, this.user.getUid());
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                this.requestingLocationUpdate = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                this.currentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }
        }
    }

    /**
     * Método firebaseConfig: Este método se encarga de iniciar la DB y obtener una instacia del servicio de Auth y se obtiene el usuario actual del sistema
     */
    private void firebaseConfig() {
        this.auth = FirebaseAuth.getInstance();
        this.user = this.auth.getCurrentUser();
        this.db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        this.db.setFirestoreSettings(settings);
    }
    /**
     * Método navigationConfig: Este método se encarga de crear el Toolbar y el menú de Hamburguesa
     */
    private void navigationConfig() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    /**
     *
     */
    private void spinnerConfig() {
        if(isViajando) {
            this.spinner.setVisibility(View.GONE);
            ((TextView) findViewById(R.id.txtContacto)).setVisibility(View.GONE);
            ((Button) findViewById(R.id.btnFindPlace)).setVisibility(View.GONE);
        } else {
            this.adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
            this.adapter.add("Seleccione a sus Contactos");
            if(usuario != null) {
                if(this.usuario.haveContactos()) {
                    for (Contacto c : this.usuario.getContactos()) {
                        adapter.add(c.getNombre());
                    }
                    this.spinner = (MultiSpinner) findViewById(R.id.spinnerMulti);
                    this.spinner.setAdapter(adapter, false, new MultiSpinner.MultiSpinnerListener() {
                        @Override
                        public void onItemsSelected(boolean[] selected) {
                            contactos.clear();
                            for (int i = 0; i < selected.length; i++) {
                                if(selected[i]) {
                                    contactos.add(usuario.getContacto(i-1));
                                }
                            }
                            Log.e("Select", contactos.toString());
                        }
                    });
                    boolean[] selectedItems = new boolean[adapter.getCount()];
                    selectedItems[1] = true; // select second item
                    spinner.setSelected(selectedItems);
                    findViewById(R.id.layoutCargando).setVisibility(View.GONE);
                    findViewById(R.id.layoutPrincipal).setVisibility(View.VISIBLE);
                } else {

                }
            } else {
                Log.e("USUARIO", "NULL");
            }
        }
    }

    /**
     * Método fabConfig: Este método se encarga de configurar el botón fab
     */
    private void fabConfig() {
        // Botones
        fabAdd = findViewById(R.id.fabAdd);
        fabAddPerson = findViewById(R.id.fabAddPerson);
        fabAddLocation = findViewById(R.id.fabAddLocation);

        // Animaciones
        fabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        fabRotateClockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_clockwise);
        fabRotateCounter = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_counterclockwise);

        fabAddPerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AddSerQuerido.class);
                startActivityForResult(intent, 0);
                onStop();
            }
        });

        fabAddLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Navegar a la actividad para agregar ubicacion
            }
        });

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isOpen) {
                    fabAdd.startAnimation(fabRotateCounter);
                    fabAddPerson.startAnimation(fabClose);
                    fabAddLocation.startAnimation(fabClose);
                    fabAddPerson.setClickable(false);
                    fabAddLocation.setClickable(false);
                    isOpen = false;
                } else {
                    fabAdd.startAnimation(fabRotateClockwise);
                    fabAddPerson.startAnimation(fabOpen);
                    fabAddLocation.startAnimation(fabOpen);
                    fabAddPerson.setClickable(true);
                    fabAddLocation.setClickable(true);
                    isOpen = true;
                }
            }
        });
    }

    /**
     *
     */
    private void updateUI() {
        final Activity activity = this;
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View header = navigationView.getHeaderView(0);
        this.tripDescription = findViewById(R.id.estadoViaje);
        ((Button) tripDescription.findViewById(R.id.btnCancelarViaje)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tripDescription.setVisibility(View.GONE);
                spinner.setVisibility(View.VISIBLE);
                ((Button) findViewById(R.id.btnFindPlace)).setVisibility(View.VISIBLE);
                isViajando = false;
                contacto = null;
                destination = null;
                placeDestionation = null;
            }
        });

        // Personalizar la UI
        ((TextView) header.findViewById(R.id.txtNombre)).setText(this.user.getDisplayName());
        ((TextView) header.findViewById(R.id.txtEmail)).setText(this.user.getEmail());

        // Tarea Especial para la foto
        new ProfilePicture((ImageView) header.findViewById(R.id.imgProfilePhoto)).execute(this.user.getPhotoUrl().toString());

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                // Construct an intent for the place picker
                try {
                    Intent intent = builder.build(activity);
                    startActivityForResult(intent, PLACE_PICKER_REQUEST);
                }  catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    Log.e("ERROR", e.getMessage(), e);
                }
            }
        };
        findViewById(R.id.btnFindPlace).setOnClickListener(listener);
    }

    /**
     *
     */
    private void createLocationCallback() {
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                if(destination != null) {
                    ((TextView) tripDescription.findViewById(R.id.txtDistance))
                            .setText(String.valueOf(currentLocation.distanceTo(destination)));
                    if(currentLocation.distanceTo(destination) <= 50.0) {
                        sendSMS();
                    }
                }
            }
        };
    }

    /**
     *
     */
    private void sendSMS() {
        if(contacto != null && destination != null && placeDestionation != null) {
            String SENT = "SMS_SENT";
            String DELIVERED = "SMS_DELIVERED";

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);

            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

            registerReceiver(new BroadcastReceiver(){
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode())
                    {
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), "SMS enviado", Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            Toast.makeText(getBaseContext(), "Sin señal", Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }, new IntentFilter(SENT));

            registerReceiver(new BroadcastReceiver(){
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode())
                    {
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), "SMS recibido", Toast.LENGTH_SHORT).show();
                            break;
                        case Activity.RESULT_CANCELED:
                            Toast.makeText(getBaseContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }, new IntentFilter(DELIVERED));

            //Envío del mensaje de texto
            boolean grado = (String.valueOf(placeDestionation.getName())).contains("°");
            String sms;
            if(grado) {
                sms = "Hola " + contacto.getNombre() + ", ya llegue al destino, " + placeDestionation.getAddress() + ". Mensaje enviado con Tracker App";
            } else {
                sms = "Hola " + contacto.getNombre() + ", ya llegue a " + placeDestionation.getName() + ". Mensaje enviado desde Tracker App";
            }
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(contacto.getTelf(), null, sms, sentPI, deliveredPI);

            this.tripDescription.setVisibility(View.GONE);
            this.isViajando = false;
            contacto = null;
            destination = null;
            placeDestionation = null;
            spinnerConfig();
            ((Button) findViewById(R.id.btnFindPlace)).setVisibility(View.VISIBLE);
        }
    }

    /**
     *
     */
    private void createLocationRequest() {
        this.locationRequest = new LocationRequest();
        this.locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        this.locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     *
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(this.locationRequest);
        this.locationSettingsRequest = builder.build();
    }

    /**
     *
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        this.settingsClient.checkLocationSettings(this.locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i("", "All location settings are satisfied.");
                        try {
                            locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        } catch (SecurityException e) {
                            Log.e("Main", "Security Exception", e);
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i("", "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e("", errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                requestingLocationUpdate = false;
                        }
                        updateUI();
                    }
                });
    }

    /**
     *
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdate);
        savedInstanceState.putParcelable(KEY_LOCATION, currentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("", "User agreed to make required location settings changes.");
                        break;
                    case Activity.RESULT_CANCELED:
                        this.requestingLocationUpdate = false;
                        break;
                }
                break;
            case PLACE_PICKER_REQUEST:
                if (resultCode == RESULT_OK) {
                    Place place = PlacePicker.getPlace(this, data);
                    this.placeDestionation = place;
                    this.destination = new Location("Google Place");
                    this.isViajando = true;
                    this.destination.setLatitude(place.getLatLng().latitude);
                    this.destination.setLongitude(place.getLatLng().longitude);
                    if(contacto != null) {
                        if(currentLocation == null) {
                            Log.e("CurrentLocation", "null");
                        } else {
                            tripDescription = findViewById(R.id.estadoViaje);
                            tripDescription.setVisibility(View.VISIBLE);
                            ((TextView) tripDescription.findViewById(R.id.txtContactoNombre)).setText(contacto.getNombre());
                            ((TextView) tripDescription.findViewById(R.id.txtDestino)).setText(placeDestionation.getName());
                            ((TextView) tripDescription.findViewById(R.id.txtDistance)).setText(String.valueOf(currentLocation.distanceTo(destination)));
                        }
                    } else {
                        Toast.makeText(this, "Debes escoger un ser querido primero", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    /**
     *
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     *
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = new Intent();
        switch (id) {
            case R.id.add_place :
                break;
            case R.id.add_seres:
                intent = new Intent(this, AddSerQuerido.class);
                intent.putExtra("user", usuario);
                findViewById(R.id.layoutCargando).setVisibility(View.VISIBLE);
                findViewById(R.id.layoutPrincipal).setVisibility(View.GONE);
                break;
            case R.id.seres:
                intent = new Intent(this, seresQueridos.class);
                intent.putExtra("user", usuario);
                findViewById(R.id.layoutCargando).setVisibility(View.VISIBLE);
                findViewById(R.id.layoutPrincipal).setVisibility(View.GONE);
                break;
            case R.id.logout:
                this.auth.signOut();
                intent = new Intent(this, Login.class);
                finish();
                break;
        }
        startActivity(intent);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     *
     */
    private void permissionConfig() {
        if(!gotPermissions()) {
            requestPermissions();
        }
    }

    /**
     *
     */
    public boolean gotPermissions() {
        boolean a = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean b = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean c = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        return a && b && c;
    }

    /**
     *
     */
    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            Log.i("", "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i("", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestingLocationUpdate) {
                    Log.i("", "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            }
        }
    }

    /**
     *
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }
}

