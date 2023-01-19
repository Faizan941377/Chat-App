package com.nextsuntech.chatapp.activities;

import  androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.nextsuntech.chatapp.databinding.ActivitySignUpBinding;
import com.nextsuntech.chatapp.utils.Constants;
import com.nextsuntech.chatapp.utils.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private String encodedImage;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void setListeners() {
        binding.tvSignUpAlreadyAccount.setOnClickListener(v -> onBackPressed());
        //register user button
        binding.btSignUpSignUp.setOnClickListener(v -> {
            if (isValidSingleUpDetail()) {
                signUp();
            }
        });

        //select profile button
        binding.ivSignUpAddProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            pickImage.launch(intent);
        });

    }


    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.etSignUpName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.etSignUpEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.etSignUpPassword.getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImage);
        database.collection(Constants.KEY_COLLECTION_USER)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.etSignUpName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }).addOnFailureListener(exception -> {
                    loading(false);
                    toast(exception.getMessage());
                });
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.ivSignUpProfileImg.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private Boolean isValidSingleUpDetail() {
        if (encodedImage == null) {
            toast("Select profile image");
            return false;
        } else if (binding.etSignUpName.getText().toString().isEmpty()) {
            toast("Enter your name");
            return false;
        } else if (binding.etSignUpEmail.getText().toString().isEmpty()) {
            toast("Enter you email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.etSignUpEmail.getText().toString()).matches()) {
            toast("Enter valid email address");
            return false;
        } else if (binding.etSignUpPassword.getText().toString().isEmpty()) {
            toast("Enter your password");
            return false;
        } else if (binding.etSignUpConfirmPassword.getText().toString().isEmpty()) {
            toast("Confirm your password");
            return false;
        } else if (!binding.etSignUpPassword.getText().toString().equals(binding.etSignUpConfirmPassword
                .getText().toString())) {
            toast("Password & Confirm password must be same");
            return false;
        } else {
            return true;
        }
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.btSignUpSignUp.setVisibility(View.INVISIBLE);
            binding.pbSignUpProgress.setVisibility(View.VISIBLE);
        } else {
            binding.pbSignUpProgress.setVisibility(View.INVISIBLE);
            binding.btSignUpSignUp.setVisibility(View.VISIBLE);
        }
    }
}