package com.tracker.tracker.tareas;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

public class UserData extends AsyncTask<FirebaseUser, Integer, DocumentSnapshot> {

    private FirebaseUser user;
    private FirebaseFirestore db;

    public UserData() {
        this.db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        this.db.setFirestoreSettings(settings);
    }

    @Override
    protected DocumentSnapshot doInBackground(FirebaseUser... users) {
        this.user = users[0];

        DocumentReference dbUser = db.collection("user").document(user.getUid());
        final DocumentSnapshot[] document = new DocumentSnapshot[1];
        dbUser.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    document[0] = task.getResult();
                } else {
                    Log.e("Getting User DB:", task.getException().getMessage());
                }
            }
        });
        return document[0];
    }

    @Override
    protected void onPostExecute(DocumentSnapshot user) {
        if (user == null) {
            CollectionReference usersRef = this.db.collection("users");
            Map<String, Object> u = new HashMap<>();
            u.put("nombre",this.user.getDisplayName());
            u.put("email", this.user.getEmail());
            u.put("photo", this.user.getPhotoUrl().toString());
            usersRef.document(this.user.getUid()).set(u);
        }
    }

    @Override
    protected void onPreExecute() { }

    @Override
    protected void onProgressUpdate(Integer... values) { }

}
