 package com.zt.groceryus.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.zt.groceryus.Constants;
import com.zt.groceryus.R;

import java.util.HashMap;

public class EditProductActivity extends AppCompatActivity {

    private ImageButton backBtn;
    private ImageView productIconIv;
    private EditText titleEt, descriptionEt, quantityEt, priceEt, discountedPriceEt, discountedNoteEt;
    private TextView categoryTv;
    private SwitchCompat discountSwitch;
    private Button updateProductBtn;

    private String productId;

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;

    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;

    private String[] cameraPermissions;
    private String[] storagePermissions;


    private Uri image_uri;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        backBtn = findViewById(R.id.backBtn);
        productIconIv = findViewById(R.id.productIconIv);
        titleEt = findViewById(R.id.titleEt);
        descriptionEt = findViewById(R.id.descriptionEt);
        quantityEt = findViewById(R.id.quantityEt);
        priceEt = findViewById(R.id.priceEt);
        discountedPriceEt = findViewById(R.id.discountedPriceEt);
        discountedNoteEt = findViewById(R.id.discountedNoteEt);
        categoryTv = findViewById(R.id.categoryTv);
        discountSwitch = findViewById(R.id.discountSwitch);
        updateProductBtn = findViewById(R.id.updateProductBtn);

        productId = getIntent().getStringExtra("productId");

        //on start is unchecked so hide both
        discountedPriceEt.setVisibility(View.GONE);
        discountedNoteEt.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        loadProductDetails(); // to set on views

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("please wait");
        progressDialog.setCanceledOnTouchOutside(false);


        //init permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //if discountSwitch is checked: show discountPriceEt and discountNoteEt
        //if unchecked: hide discountPriceEt and discountNoteEt
        discountSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    discountedPriceEt.setVisibility(View.VISIBLE);
                    discountedNoteEt.setVisibility(View.VISIBLE);
                }else{
                    discountedPriceEt.setVisibility(View.GONE);
                    discountedNoteEt.setVisibility(View.GONE);
                }
            }
        });

        productIconIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickDialog();
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoryDialog();
            }
        });

        updateProductBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Flow:
                //1.Input Data
                //2. Validate data
                //3. Update data to db
                inputData();
            }
        });
    }

    private void loadProductDetails() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Products").child(productId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //get data
                        String productId = ""+dataSnapshot.child("productId").getValue();
                        String productTitle = ""+dataSnapshot.child("productTitle").getValue();
                        String productDescription = ""+dataSnapshot.child("productDescription").getValue();
                        String productCategory = ""+dataSnapshot.child("productCategory").getValue();
                        String productQuantity = ""+dataSnapshot.child("productQuantity").getValue();
                        String productIcon = ""+dataSnapshot.child("productId").getValue();
                        String originalPrice = ""+dataSnapshot.child("originalPrice").getValue();
                        String discountPrice = ""+dataSnapshot.child("discountPrice").getValue();
                        String discountNote = ""+dataSnapshot.child("discountNote").getValue();
                        String discountAvailable = ""+dataSnapshot.child("discountAvailable").getValue();
                        String timestamp = ""+dataSnapshot.child("timestamp").getValue();
                        String uid = ""+dataSnapshot.child("uid").getValue();

                        if (discountAvailable.equals("true")){
                            discountSwitch.setChecked(true);

                            discountedPriceEt.setVisibility(View.VISIBLE);
                            discountedNoteEt.setVisibility(View.VISIBLE);
                        }else{
                            discountSwitch.setChecked(false);

                            discountedPriceEt.setVisibility(View.GONE);
                            discountedNoteEt.setVisibility(View.GONE);
                        }

                        titleEt.setText(productTitle);
                        descriptionEt.setText(productDescription);
                        categoryTv.setText(productCategory);
                        discountedNoteEt.setText(discountNote);
                        quantityEt.setText(productQuantity);
                        priceEt.setText(originalPrice);
                        discountedPriceEt.setText(discountPrice);

                        try {
                            Picasso.get().load(productIcon).placeholder(R.drawable.ic_add_shopping_white).into(productIconIv);
                        }catch (Exception e){
                            productIconIv.setImageResource(R.drawable.ic_add_shopping_white);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private String productTitle, productDescription, productCategory, productQuantity, originalPrice, discountPrice, discountNote;
    private boolean discountAvailable = false;

    private void inputData() {
        //1. Input data
        productTitle = titleEt.getText().toString().trim();
        productDescription = descriptionEt.getText().toString().trim();
        productCategory = categoryTv.getText().toString().trim();
        productQuantity = quantityEt.getText().toString().trim();
        originalPrice = priceEt.getText().toString().trim();
        discountAvailable = discountSwitch.isChecked();
        //2. Validate Data
        if (TextUtils.isEmpty(productTitle)){
            Toast.makeText(this, "Title is required...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(productCategory)){
            Toast.makeText(this, "Category is required...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(originalPrice)){
            Toast.makeText(this, "Price is required...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (discountAvailable){
            discountPrice = discountedPriceEt.getText().toString().trim();
            discountNote = discountedNoteEt.getText().toString().trim();
            if (TextUtils.isEmpty(originalPrice)){
                Toast.makeText(this, "Discounted Price is required...", Toast.LENGTH_SHORT).show();
                return;
            }
        }else{
            discountPrice ="0";
            discountNote = "";
        }
        updateProduct();

    }

    private void updateProduct() {
        progressDialog.setMessage("Updating product...");
        progressDialog.show();

        if (image_uri == null){
            //update w/o image

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("productTitle", ""+productTitle);
            hashMap.put("productDescription", ""+productDescription);
            hashMap.put("productCategory", ""+productCategory);
            hashMap.put("productQuantity", ""+productQuantity);
            hashMap.put("originalPrice", ""+originalPrice);
            hashMap.put("discountPrice", ""+discountPrice);
            hashMap.put("discountNote", ""+discountNote);
            hashMap.put("discountAvailable", ""+discountAvailable);

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
            reference.child(firebaseAuth.getUid()).child("Products").child(productId)
                    .updateChildren(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            progressDialog.dismiss();
                            Toast.makeText(EditProductActivity.this, "Product Successfully updated", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(EditProductActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }else {
            String filePathAndName = "product_images/"+""+productId;
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            Task<Uri> uriTask =taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());
                            Uri downloadImageUri = uriTask.getResult();

                            if (uriTask.isSuccessful()){
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("productTitle", ""+productTitle);
                                hashMap.put("productDescription", ""+productDescription);
                                hashMap.put("productCategory", ""+productCategory);
                                hashMap.put("productIcon", "" + downloadImageUri);
                                hashMap.put("productQuantity", ""+productQuantity);
                                hashMap.put("originalPrice", ""+originalPrice);
                                hashMap.put("discountPrice", ""+discountPrice);
                                hashMap.put("discountNote", ""+discountNote);
                                hashMap.put("discountAvailable", ""+discountAvailable);

                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                                reference.child(firebaseAuth.getUid()).child("Products").child(productId)
                                        .updateChildren(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                progressDialog.dismiss();
                                                Toast.makeText(EditProductActivity.this, "Product Successfully updated", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(EditProductActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(EditProductActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void categoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Product Category")
                .setItems(Constants.productCategories, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String category = Constants.productCategories[which];
                        categoryTv.setText(category);
                    }
                })
                .show();
    }

    private void showImagePickDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0){

                            if (checkCameraPermission()){
                                pickFromCamera();
                            }else {
                                requestCameraPermission();
                            }
                        }else{

                            if (checkStoragePermission()){
                                pickFromGallery();
                            }else {
                                requestStoragePermission();
                            }
                        }
                    }
                })
                .show();
    }

    private void pickFromGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera(){
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) ==
                (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted){
                        //permission allowed
                        pickFromCamera();

                    }else {
                        Toast.makeText(this, "Camera permissions are neccessary....", Toast.LENGTH_SHORT).show();

                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length>0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted){
                        //permission allowed
                        pickFromGallery();

                    }else {
                        Toast.makeText(this, "Storage permissions are neccessary....", Toast.LENGTH_SHORT).show();

                    }
                }
            }
            break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                image_uri = data.getData();
                productIconIv.setImageURI(image_uri);
            }else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                productIconIv.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}