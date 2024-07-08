package com.example.t2i.screens;

import android.content.Context;

import com.example.t2i.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class GoogleSignInManager {
    private static GoogleSignInManager instance;
    private GoogleSignInClient googleSignInClient;

    private GoogleSignInManager() {
        // Private constructor to prevent instantiation
    }

    public static synchronized GoogleSignInManager getInstance() {
        if (instance == null) {
            instance = new GoogleSignInManager();
        }
        return instance;
    }

    public GoogleSignInClient getGoogleSignInClient(Context context) {
        if (googleSignInClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(context, gso);
        }
        return googleSignInClient;
    }

    public void signOut(Context context, Runnable onCompleteCallback) {
        if (googleSignInClient != null) {
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                googleSignInClient.revokeAccess().addOnCompleteListener(revokeTask -> {
                    // Reset the client to force a new account selection next time
                    googleSignInClient = null;
                    if (onCompleteCallback != null) {
                        onCompleteCallback.run();
                    }
                });
            });
        } else if (onCompleteCallback != null) {
            onCompleteCallback.run();
        }
    }
}