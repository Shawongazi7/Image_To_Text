package com.example.t2i.screens;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.t2i.R;
import com.example.t2i.screens.GoogleSignInManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";

    private EditText v_loginemail, v_loginpassword;
    private AppCompatButton v_login, v_createAcc, v_guestLogin;
    private TextView v_gotof_pass;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        v_loginemail = findViewById(R.id.loginemail);
        v_loginpassword = findViewById(R.id.loginpassword);
        v_login = findViewById(R.id.login);
        v_gotof_pass = findViewById(R.id.v_gotof_pass);
        v_createAcc = findViewById(R.id.createAcc);
        v_guestLogin = findViewById(R.id.guestLogin);
        SignInButton googleSignInButton = findViewById(R.id.googleSignInButton);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            finish();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }

        // Get GoogleSignInClient from GoogleSignInManager
        googleSignInClient = GoogleSignInManager.getInstance().getGoogleSignInClient(this);

        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        v_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mail = v_loginemail.getText().toString().trim();
                String pass = v_loginpassword.getText().toString().trim();

                if (mail.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Fill All the Fields", Toast.LENGTH_SHORT).show();
                } else {
                    firebaseAuth.signInWithEmailAndPassword(mail, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                checkMailVerification();
                            } else {
                                Toast.makeText(getApplicationContext(), "Account Does Not Exist", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        v_gotof_pass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, ResetPassword.class));
            }
        });

        v_createAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        v_guestLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                guestLogin();
            }
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(LoginActivity.this, "Google sign in failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Google Sign-In successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkMailVerification() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null && firebaseUser.isEmailVerified()) {
            Toast.makeText(getApplicationContext(), "Logged In", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        } else {
            Toast.makeText(getApplicationContext(), "Verify Your Email First", Toast.LENGTH_SHORT).show();
            firebaseAuth.signOut();
        }
    }

    private void guestLogin() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public static void logout(Context context, Runnable onCompleteCallback) {
        FirebaseAuth.getInstance().signOut();
        GoogleSignInManager.getInstance().signOut(context, onCompleteCallback);
    }
}