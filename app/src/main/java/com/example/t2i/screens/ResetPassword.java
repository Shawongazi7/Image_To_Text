package com.example.t2i.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.t2i.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPassword extends AppCompatActivity {
    private EditText v_forgotpassemail;
    private Button v_pass_recover_button;
    private TextView v_gobacktologin;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        //getSupportActionBar().hide();
        v_forgotpassemail = findViewById(R.id.forgotpassemail);
        v_pass_recover_button = findViewById(R.id.pass_recover_button);
        v_gobacktologin = findViewById(R.id.gobacktologin);
        firebaseAuth = FirebaseAuth.getInstance();

        v_gobacktologin.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ResetPassword.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        v_pass_recover_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mail = v_forgotpassemail.getText().toString().trim();
                if(mail.isEmpty())
                {
                    Toast.makeText(getApplicationContext(), "Enter Your Mail", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    firebaseAuth.sendPasswordResetEmail(mail).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                            {
                                Toast.makeText(getApplicationContext(), "Pass Recover Mail Sent", Toast.LENGTH_SHORT).show();
                                finish();
                                startActivity(new Intent(ResetPassword.this, LoginActivity.class));
                            }
                            else
                            {
                                Toast.makeText(getApplicationContext(), "Email Is not REGISTERED", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

    }
}