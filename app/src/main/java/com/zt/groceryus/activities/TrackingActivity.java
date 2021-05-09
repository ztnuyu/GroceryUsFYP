package com.zt.groceryus.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.zt.groceryus.R;

public class TrackingActivity extends AppCompatActivity {

    private TextView textEstimatedTimeTv, textOrderNumberTv,textTimeTv, textOrderTv, textOrderPlacedTv
            , textOrderConfirmedTv, textOrderProcessedTv, textOrderReadyTv, textOrderCancelledTv;
    private View viewOrderPlaced, viewOne, viewOrderConfirmed, viewTwo, viewOrderProcessed, viewThree, viewOrderReady, viewFour, viewOrderCancelled;
    private ImageView imageOrderPlaced, imageOrderConfirmed, imageOrderProcessed, imageOrderReady, imageOrderCancelled;
    private ImageButton backBtn;

    String orderId, orderBy;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        textEstimatedTimeTv = findViewById(R.id.textEstimatedTimeTv);
        textOrderNumberTv = findViewById(R.id.textOrderNumberTv);
        textTimeTv = findViewById(R.id.textTimeTv);
        textOrderTv = findViewById(R.id.textOrderTv);
        textOrderPlacedTv = findViewById(R.id.textOrderPlacedTv);
        textOrderConfirmedTv = findViewById(R.id.textOrderConfirmedTv);
        textOrderProcessedTv = findViewById(R.id.textOrderProcessedTv);
        textOrderReadyTv = findViewById(R.id.textOrderReadyTv);
        textOrderCancelledTv = findViewById(R.id.textOrderCancelledTv);
        viewOrderPlaced = findViewById(R.id.viewOrderPlaced);
        viewOne = findViewById(R.id.viewOne);
        viewOrderConfirmed = findViewById(R.id.viewOrderConfirmed);
        viewTwo = findViewById(R.id.viewTwo);
        viewOrderProcessed = findViewById(R.id.viewOrderProcessed);
        viewThree = findViewById(R.id.viewThree);
        viewFour = findViewById(R.id.viewFour);
        viewOrderCancelled = findViewById(R.id.viewOrderCancelled);
        viewOrderReady = findViewById(R.id.viewOrderReady);
        imageOrderPlaced = findViewById(R.id.imageOrderPlaced);
        imageOrderConfirmed = findViewById(R.id.imageOrderConfirmed);
        imageOrderProcessed = findViewById(R.id.imageOrderProcessed);
        imageOrderReady = findViewById(R.id.imageOrderReady);
        imageOrderCancelled = findViewById(R.id.imageOrderCancelled);
        backBtn = findViewById(R.id.backBtn);

        orderId = getIntent().getStringExtra("orderId");
        orderBy = getIntent().getStringExtra("orderBy");

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        firebaseAuth = FirebaseAuth.getInstance();
        trackStatus();

    }

    private void trackStatus(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Orders").child(orderId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String orderId = ""+dataSnapshot.child("orderId").getValue();
                        String orderStatus = ""+dataSnapshot.child("orderStatus").getValue();

                        textOrderTv.setText(orderId);

                        if (orderStatus.equals("In Progress")){
                            viewOrderPlaced.setBackgroundResource(R.drawable.shape_status_current);
                            viewOrderConfirmed.setBackgroundResource(R.drawable.shape_status_remaining);
                            viewOrderProcessed.setBackgroundResource(R.drawable.shape_status_remaining);
                            viewOrderReady.setBackgroundResource(R.drawable.shape_status_remaining);
                            viewOne.setBackgroundResource(R.color.colorRemaining);
                            viewTwo.setBackgroundResource(R.color.colorRemaining);
                            viewThree.setBackgroundResource(R.color.colorRemaining);
                            viewFour.setBackgroundResource(R.color.colorRemaining);
                            viewOrderCancelled.setBackgroundResource(R.drawable.shape_status_remaining);
                            imageOrderCancelled.setImageAlpha(128);
                            textOrderCancelledTv.setAlpha(1);
                            imageOrderConfirmed.setImageAlpha(128);
                            textOrderConfirmedTv.setAlpha(1);
                            imageOrderProcessed.setImageAlpha(128);
                            textOrderProcessedTv.setAlpha(1);
                            imageOrderReady.setImageAlpha(128);
                            textOrderReadyTv.setAlpha(1);
                        }else if (orderStatus.equals("Order Confirmed")){
                            viewOrderPlaced.setBackgroundResource(R.drawable.shape_status_completed);
                            viewOrderConfirmed.setBackgroundResource(R.drawable.shape_status_current);
                            viewOrderProcessed.setBackgroundResource(R.drawable.shape_status_remaining);
                            viewOrderReady.setBackgroundResource(R.drawable.shape_status_remaining);
                            viewOne.setBackgroundResource(R.color.colorGreen);
                            viewTwo.setBackgroundResource(R.color.colorRemaining);
                            viewThree.setBackgroundResource(R.color.colorRemaining);
                            viewFour.setBackgroundResource(R.color.colorRemaining);
                            viewOrderCancelled.setBackgroundResource(R.drawable.shape_status_remaining);
                            imageOrderCancelled.setImageAlpha(128);
                            textOrderCancelledTv.setAlpha(1);
                            imageOrderProcessed.setImageAlpha(128);
                            textOrderProcessedTv.setAlpha(1);
                            imageOrderReady.setImageAlpha(128);
                            textOrderReadyTv.setAlpha(1);
                        }else if (orderStatus.equals("Order Processed")){
                            viewOrderPlaced.setBackgroundResource(R.drawable.shape_status_completed);
                            viewOrderConfirmed.setBackgroundResource(R.drawable.shape_status_completed);
                            viewOrderProcessed.setBackgroundResource(R.drawable.shape_status_current);
                            viewOrderReady.setBackgroundResource(R.drawable.shape_status_remaining);
                            viewOne.setBackgroundResource(R.color.colorGreen);
                            viewTwo.setBackgroundResource(R.color.colorGreen);
                            viewThree.setBackgroundResource(R.color.colorRemaining);
                            viewFour.setBackgroundResource(R.color.colorRemaining);
                            viewOrderCancelled.setBackgroundResource(R.drawable.shape_status_remaining);
                            imageOrderCancelled.setImageAlpha(128);
                            textOrderCancelledTv.setAlpha(1);
                            textOrderReadyTv.setAlpha(1);
                            textOrderCancelledTv.setAlpha(1);
                            imageOrderReady.setImageAlpha(128);
                            textOrderReadyTv.setAlpha(1);
                        }else if (orderStatus.equals("Order Ready")){
                            viewOrderPlaced.setBackgroundResource(R.drawable.shape_status_completed);
                            viewOrderConfirmed.setBackgroundResource(R.drawable.shape_status_completed);
                            viewOrderProcessed.setBackgroundResource(R.drawable.shape_status_completed);
                            viewOrderReady.setBackgroundResource(R.drawable.shape_status_current);
                            viewOne.setBackgroundResource(R.color.colorGreen);
                            viewTwo.setBackgroundResource(R.color.colorGreen);
                            viewThree.setBackgroundResource(R.color.colorGreen);
                            viewFour.setBackgroundResource(R.color.colorRemaining);
                            viewOrderCancelled.setBackgroundResource(R.drawable.shape_status_remaining);
                            imageOrderCancelled.setImageAlpha(128);
                            textOrderCancelledTv.setAlpha(1);
                        }else if (orderStatus.equals("Cancelled")){
                            viewOrderPlaced.setBackgroundResource(R.drawable.shape_status_remaining);
                            viewOrderConfirmed.setBackgroundResource(R.drawable.shape_status_remaining);
                            viewOrderProcessed.setBackgroundResource(R.drawable.shape_status_remaining);
                            viewOrderReady.setBackgroundResource(R.drawable.shape_status_remaining);
                            viewOrderCancelled.setBackgroundResource(R.drawable.shape_status_current);
                            viewOne.setBackgroundResource(R.color.colorRemaining);
                            viewTwo.setBackgroundResource(R.color.colorRemaining);
                            viewThree.setBackgroundResource(R.color.colorRemaining);
                            viewFour.setBackgroundResource(R.color.colorRemaining);
                            imageOrderConfirmed.setImageAlpha(128);
                            imageOrderPlaced.setImageAlpha(128);
                            imageOrderProcessed.setImageAlpha(128);
                            imageOrderReady.setImageAlpha(128);
                            textOrderReadyTv.setAlpha(1);
                            textOrderConfirmedTv.setAlpha(1);
                            textOrderPlacedTv.setAlpha(1);
                            textOrderProcessedTv.setAlpha(1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });



    }
}