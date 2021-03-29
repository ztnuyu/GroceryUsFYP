package com.zt.groceryus.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.zt.groceryus.R;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;

public class AddPromotionCodeActivity extends AppCompatActivity {

    private ImageButton backBtn;
    private EditText promoCodeEt, promoDescriptionEt, promoPriceEt, minimumOrderPriceEt;
    private TextView expiredDateTv, titleTv;
    private Button addBtn;

    FirebaseAuth firebaseAuth;
    ProgressDialog progressDialog;

    private String promoId;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_promotion_code);

        backBtn = findViewById(R.id.backBtn);
        promoCodeEt = findViewById(R.id.promoCodeEt);
        promoDescriptionEt = findViewById(R.id.promoDescriptionEt);
        promoPriceEt = findViewById(R.id.promoPriceEt);
        minimumOrderPriceEt = findViewById(R.id.minimumOrderPriceEt);
        expiredDateTv = findViewById(R.id.expiredDateTv);
        titleTv = findViewById(R.id.titleTv);
        addBtn = findViewById(R.id.addBtn);

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //get promo id from intent
        Intent intent = getIntent();
        if (intent.getStringExtra("promoId") != null){
            promoId = intent.getStringExtra("promoId");

            titleTv.setText("Update Promotion Code");
            addBtn.setText("UPDATE");

            isUpdating = true;

            loadPromoInfo();
        }else{
            titleTv.setText("Add Promotion Code");
            addBtn.setText("ADD");

            isUpdating = false;

        }

        backBtn.setOnClickListener(v -> onBackPressed());

        expiredDateTv.setOnClickListener(v -> datePickDialog());

        addBtn.setOnClickListener(v -> {
            inputData();
        });
    }

    private void loadPromoInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Promotions").child(promoId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String id = ""+snapshot.child("id").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String promoCode = ""+snapshot.child("promoCode").getValue();
                        String promoPrice = ""+snapshot.child("promoPrice").getValue();
                        String minimumOrderPrice = ""+snapshot.child("minimumOrderPrice").getValue();
                        String expireDate = ""+snapshot.child("expireDate").getValue();

                        promoCodeEt.setText(promoCode);
                        promoDescriptionEt.setText(description);
                        promoPriceEt.setText(promoPrice);
                        minimumOrderPriceEt.setText(minimumOrderPrice);
                        expiredDateTv.setText(expireDate);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }


    private void datePickDialog() {
        Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                DecimalFormat mFormat = new DecimalFormat("00");
                String pDay = mFormat.format(dayOfMonth);
                String pMonth = mFormat.format(monthOfYear);
                String pYear = ""+year;
                String pDate = pDay +"/" + pMonth + "/" + pYear;

                expiredDateTv.setText(pDate);
            }
        },mYear, mMonth, mDay);

        datePickerDialog.show();
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis()-1000);
    }

    private String description, promoCode, promoPrice, minimumOrderPrice, expireDate;
    private void inputData() {
        promoCode = promoCodeEt.getText().toString().trim();
        description = promoDescriptionEt.getText().toString().trim();
        promoPrice = promoPriceEt.getText().toString().trim();
        minimumOrderPrice = minimumOrderPriceEt.getText().toString().trim();
        expireDate = expiredDateTv.getText().toString().trim();

        if (TextUtils.isEmpty(promoCode)){
            Toast.makeText(this, "Enter discount code", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(description)){
            Toast.makeText(this, "Enter description", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(promoPrice)){
            Toast.makeText(this, "Enter Promotion Price", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(minimumOrderPrice)){
            Toast.makeText(this, "Enter minimum order price", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(expireDate)){
            Toast.makeText(this, "Choose expiry date", Toast.LENGTH_SHORT).show();
            return;
        }

        //all fields entered, add/update data to db
        if (isUpdating){
            updateDataToDb();
        }else {
            addDataToDb();
        }

    }

    private void updateDataToDb() {
        progressDialog.setMessage("Updating Promotion Codes");
        progressDialog.show();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("description", ""+description);
        hashMap.put("promoCode", ""+promoCode);
        hashMap.put("promoPrice", ""+promoPrice);
        hashMap.put("minimumOrderPrice", ""+minimumOrderPrice);
        hashMap.put("expireDate", ""+expireDate);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Promotions").child(promoId)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Toast.makeText(AddPromotionCodeActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(AddPromotionCodeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addDataToDb() {
        progressDialog.setMessage("Adding Promotion Codes");
        progressDialog.show();

        String timestamp = ""+System.currentTimeMillis();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", ""+timestamp);
        hashMap.put("timestamp", ""+timestamp);
        hashMap.put("description", ""+description);
        hashMap.put("promoCode", ""+promoCode);
        hashMap.put("promoPrice", ""+promoPrice);
        hashMap.put("minimumOrderPrice", ""+minimumOrderPrice);
        hashMap.put("expireDate", ""+expireDate);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Promotions").child(timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Toast.makeText(AddPromotionCodeActivity.this, "Promotion Code Succesfully added", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(AddPromotionCodeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


}