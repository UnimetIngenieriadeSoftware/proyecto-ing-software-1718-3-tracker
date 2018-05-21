package com.tracker.tracker.tareas;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.tracker.tracker.Modelos.Contacto;

import static android.content.ContentValues.TAG;

public class SeresQueridosAsync extends AsyncTask<FirebaseUser, Void, DocumentSnapshot> {

    private FirebaseFirestore db;

    public SeresQueridosAsync(){
        this.db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        this.db.setFirestoreSettings(settings);
    }

    @Override
    protected DocumentSnapshot doInBackground(FirebaseUser... users) {
        FirebaseUser user = users[0];
        if (user != null) {
            CollectionReference contactos = db.collection("users/" + user.getUid() + "/contactos");
            contactos.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if(task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            Contacto c = new Contacto(document.getString("nombre"), document.getString("telf"));
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }
            });
        }
        return null;
    }
}