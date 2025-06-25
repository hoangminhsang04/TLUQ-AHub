package com.example.baitaplon;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {
    EditText edtEmail, edtPassword;
    Button btnLogin;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtEmail = findViewById(R.id.edtemail);
        edtPassword = findViewById(R.id.edtpassword);
        btnLogin = findViewById(R.id.btnlogin);

        dbRef = FirebaseDatabase.getInstance().getReference();

        btnLogin.setOnClickListener(view -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            checkLogin(email, password);
        });
    }

    private void checkLogin(String email, String password) {
        dbRef.child("users").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Lỗi kết nối Firebase", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean found = false;

            for (DataSnapshot userSnapshot : task.getResult().getChildren()) {
                String dbEmail = userSnapshot.child("email").getValue(String.class);
                String dbPassword = userSnapshot.child("passwordHash").getValue(String.class);
                String roleId = userSnapshot.child("roleId").getValue(String.class);

                if (email.equals(dbEmail) && password.equals(dbPassword)) {
                    found = true;
                    String userId = userSnapshot.getKey(); // Lấy userId
                    String avatarName = userSnapshot.child("avatar").getValue(String.class);
                    String fullName = userSnapshot.child("fullName").getValue(String.class);
                    Long repLong = userSnapshot.child("reputationScore").getValue(Long.class);
                    int reputationScore = (repLong != null) ? repLong.intValue() : 0;


                    // Lưu thông tin vào SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("userId", userId)
                            .putString("fullName", fullName)
                            .putString("avatarName", avatarName)
                            .putInt("reputationScore", reputationScore)
                            .apply();

                    if (roleId != null) {
                        dbRef.child("roles").child(roleId).child("roleName").get().addOnCompleteListener(roleTask -> {
                            if (!roleTask.isSuccessful() || roleTask.getResult() == null) {
                                Toast.makeText(this, "Không lấy được quyền người dùng", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String roleName = roleTask.getResult().getValue(String.class);
                            Intent intent;
                            if ("Admin".equalsIgnoreCase(roleName)) {
                                intent = new Intent(LoginActivity.this, AdminActivity.class);
                            } else {
                                intent = new Intent(LoginActivity.this, UserActivity.class);
                            }

                            startActivity(intent);
                            finish();
                        });
                    }
                    break;
                }
            }

            if (!found) {
                Toast.makeText(this, "Sai email hoặc mật khẩu", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
