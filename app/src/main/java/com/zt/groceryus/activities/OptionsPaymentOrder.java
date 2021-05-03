package com.zt.groceryus.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.zt.groceryus.Constants;
import com.zt.groceryus.R;
import com.zt.groceryus.adapters.AdapterCartItem;
import com.zt.groceryus.adapters.AdapterProductUser;
import com.zt.groceryus.models.ModelCartItem;
import com.zt.groceryus.models.ModelProduct;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import p32929.androideasysql_library.Column;
import p32929.androideasysql_library.EasyDB;

public class OptionsPaymentOrder extends AppCompatActivity {

    private ImageView shopIv;
    private TextView shopNameTv, phoneTv, emailTv, openCloseTv, deliveryFeeTv, addressTv, filteredProductsTv, cartCountTv;
    private ImageButton callBtn, mapBtn, cartBtn, backBtn, filterProductBtn, reviewsBtn;
    private EditText searchProductEt;
    private RecyclerView productsRv;
    private RatingBar ratingBar;
    private Button payPalButton, payCashBtn;

    private String shopUid;
    private String myLatitude, myLongitude, myPhone;
    private String shopName, shopEmail, shopPhone, shopAddress, shopLatitude, shopLongitude;
    public String deliveryFee;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private ArrayList<ModelProduct> productsList;
    private AdapterProductUser adapterProductUser;

    //cart
    private ArrayList<ModelCartItem> cartItemList;
    private AdapterCartItem adapterCartItem;

    private EasyDB easyDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options_payment_order);

//        //Start PayPal Service
//        Intent intent1 = new Intent(this, PayPalService.class);
//        intent1.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
//        startService(intent1);

        ShopDetailsActivity fact = new ShopDetailsActivity();

        shopIv = findViewById(R.id.shopIv);
        shopNameTv = findViewById(R.id.shopNameTv);
        phoneTv = findViewById(R.id.phoneTv);
        emailTv = findViewById(R.id.emailTv);
        openCloseTv = findViewById(R.id.openCloseTv);
        deliveryFeeTv = findViewById(R.id.deliveryFeeTv);
        addressTv = findViewById(R.id.addressTv);
        filteredProductsTv = findViewById(R.id.filteredProductsTv);
        callBtn = findViewById(R.id.callBtn);
        mapBtn = findViewById(R.id.mapBtn);
        cartBtn = findViewById(R.id.cartBtn);
        backBtn = findViewById(R.id.backBtn);
        filterProductBtn = findViewById(R.id.filterProductBtn);
        searchProductEt = findViewById(R.id.searchProductEt);
        productsRv = findViewById(R.id.productsRv);
        cartCountTv = findViewById(R.id.cartCountTv);
        reviewsBtn = findViewById(R.id.reviewsBtn);
        ratingBar = findViewById(R.id.ratingBar);
        payPalButton = findViewById(R.id.payPalBtn);
        payCashBtn = findViewById(R.id.payCashBtn);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        shopUid = getIntent().getStringExtra("shopUid");
        firebaseAuth = FirebaseAuth.getInstance();


        easyDB = EasyDB.init(this, "ITEMS_DB")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id", new String[]{"text", "unique"}))
                .addColumn(new Column("Item_PID", new String[]{"text", "not null"}))
                .addColumn(new Column("Item_Name", new String[]{"text", "not null"}))
                .addColumn(new Column("Item_Price_Each", new String[]{"text", "not null"}))
                .addColumn(new Column("Item_Price", new String[]{"text", "not null"}))
                .addColumn(new Column("Item_Quantity", new String[]{"text", "not null"}))
                .doneTableColumn();
        //cart will be deleted if different shops chosen by user


        backBtn.setOnClickListener(v -> onBackPressed());

        payCashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                submitOrder();

            }
        });

    }

    private float ratingSum = 0;


    public double allTotalPrice = 0.00;
    //need to access these views in adapter
    public TextView sTotalTv, dFeeTv, allTotalPriceTv, promoDescriptionTv, discountTv;
    public EditText promoCodeEt;
    public Button applyBtn;

    // place order dialog
//    private void showCartDialog() {
//
//        cartItemList = new ArrayList<>();
//
//        //inflate cart layout
//        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cart, null);
//        //init view
//        TextView shopNameTv = view.findViewById(R.id.shopNameTv);
//        promoCodeEt = view.findViewById(R.id.promoCodeEt);
//        promoDescriptionTv = view.findViewById(R.id.promoDescriptionTv);
//        discountTv = view.findViewById(R.id.discountTv);
//        FloatingActionButton validateBtn = view.findViewById(R.id.validateBtn);
//        applyBtn = view.findViewById(R.id.applyBtn);
//        RecyclerView cartItemsRv = view.findViewById(R.id.cartItemsRv);
//        sTotalTv = view.findViewById(R.id.sTotalTv);
//        dFeeTv = view.findViewById(R.id.dFeeTv);
//        allTotalPriceTv= view.findViewById(R.id.totalTv);
//        Button checkoutBtn = view.findViewById(R.id.checkoutBtn);
//
//        //whenever cart dailog shows, check if promo cocde is applied or not
//        if (isPromoCodeApplied){
//            promoDescriptionTv.setVisibility(View.VISIBLE);
//            applyBtn.setVisibility(View.VISIBLE);
//            applyBtn.setText("Applied");
//            promoCodeEt.setText(promoCode);
//            promoDescriptionTv.setText(promoDescription);
//        }else {
//            promoDescriptionTv.setVisibility(View.GONE);
//            applyBtn.setVisibility(View.GONE);
//            applyBtn.setText("Apply");
//        }
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setView(view);
//        shopNameTv.setText(shopName);
//
//        EasyDB easyDB = EasyDB.init(this, "ITEMS_DB")
//                .setTableName("ITEMS_TABLE")
//                .addColumn(new Column("Item_Id", new String[]{"text", "unique"}))
//                .addColumn(new Column("Item_PID", new String[]{"text", "not null"}))
//                .addColumn(new Column("Item_Name", new String[]{"text", "not null"}))
//                .addColumn(new Column("Item_Price_Each", new String[]{"text", "not null"}))
//                .addColumn(new Column("Item_Price", new String[]{"text", "not null"}))
//                .addColumn(new Column("Item_Quantity", new String[]{"text", "not null"}))
//                .doneTableColumn();
//
//        //get record from db
//        Cursor res = easyDB.getAllData();
//        while (res.moveToNext()){
//            String id = res.getString(1);
//            String pId = res.getString(2);
//            String name = res.getString(3);
//            String price = res.getString(4);
//            String cost = res.getString(5);
//            String quantity = res.getString(6);
//
//            allTotalPrice = allTotalPrice + Double.parseDouble(cost);
//
//            ModelCartItem modelCartItem = new ModelCartItem(
//                    ""+id,
//                    ""+pId,
//                    ""+name,
//                    ""+price,
//                    ""+cost,
//                    "["+quantity+"]"
//            );
//
//            cartItemList.add(modelCartItem);
//
//        }
//        adapterCartItem = new AdapterCartItem(this, cartItemList);
//        cartItemsRv.setAdapter(adapterCartItem);
//
//        if (isPromoCodeApplied){
//            priceWithDiscount();
//        }else{
//            priceWithoutDiscount();
//        }
//
//        //show dialog
//        AlertDialog dialog = builder.create();
//        dialog.show();
//
//        //reset total price on dialog dismiss
//        dialog.setOnCancelListener(dialog1 -> allTotalPrice = 0.00);
//
//
//
//        validateBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                /*
//                 * get code from et- if not empty promomaybe applied
//                 * check if code is valid
//                 * check if expired
//                 * check minimum order price minimumOrderPrice >= SubtotalPrice
//                 * */
//                String promotionCode = promoCodeEt.getText().toString().trim();
//                if (TextUtils.isEmpty(promotionCode)){
//                    Toast.makeText(OptionsPaymentOrder.this, "Please enter promo code", Toast.LENGTH_SHORT).show();
//                }else {
//                    checkCodeAvailability(promotionCode);
//                }
//            }
//        });
//
//        //will visible only if code is valid
//        applyBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                isPromoCodeApplied = true;
//                applyBtn.setText("Applied");
//
//                priceWithDiscount();
//            }
//        });
//
//
//    }
//
//    private void priceWithDiscount(){
//        discountTv.setText("$"+promoPrice);
//        dFeeTv.setText("$"+deliveryFee);
//        sTotalTv.setText("$"+String.format("%.2f", allTotalPrice));
//        allTotalPriceTv.setText("$"+(allTotalPrice+ Double.parseDouble(deliveryFee.replace("$","")) - Double.parseDouble(promoPrice)));
//    }
//
//    private void priceWithoutDiscount(){
//        discountTv.setText("$0");
//        dFeeTv.setText("$"+deliveryFee);
//        sTotalTv.setText("$"+String.format("%.2f", allTotalPrice));
//        allTotalPriceTv.setText("$"+(allTotalPrice+ Double.parseDouble(deliveryFee.replace("$",""))));
//    }
//
//    public boolean isPromoCodeApplied = false;
//    public String promoId, promoTimestamp, promoCode, promoDescription, promoExpDate, promoMinimumOrderPrice, promoPrice;
//    private void checkCodeAvailability(String promotionCode){
//        final ProgressDialog progressDialog = new ProgressDialog(this);
//        progressDialog.setTitle("Please wait");
//        progressDialog.setMessage("Checking Promotion Codes");
//        progressDialog.setCanceledOnTouchOutside(false);
//
//        //proomo not applied
//        isPromoCodeApplied = false;
//        applyBtn.setText("Apply");
//        priceWithoutDiscount();
//
//        //check promo code availability
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
//        ref.child(shopUid).child("Promotions").orderByChild("promoCode").equalTo(promotionCode)
//                .addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        //check ifpromo exist
//                        if (snapshot.exists()){
//                            progressDialog.dismiss();
//                            for (DataSnapshot ds: snapshot.getChildren()){
//                                promoId = ""+ds.child("id").getValue();
//                                promoTimestamp = ""+ds.child("timestamp").getValue();
//                                promoCode = ""+ds.child("promoCode").getValue();
//                                promoDescription = ""+ds.child("description").getValue();
//                                promoExpDate = ""+ds.child("expireDate").getValue();
//                                promoMinimumOrderPrice = ""+ds.child("minimumOrderPrice").getValue();
//                                promoPrice = ""+ds.child("promoPrice").getValue();
//
//                                //check code is expired
//                                checkCodeExpireDate();
//                            }
//                        }
//                        else{
//                            //promo code doesnt exist
//                            progressDialog.dismiss();
//                            Toast.makeText(OptionsPaymentOrder.this, "Invalid promo code", Toast.LENGTH_SHORT).show();
//                            applyBtn.setVisibility(View.GONE);
//                            promoDescriptionTv.setVisibility(View.GONE);
//                            promoDescriptionTv.setText("");
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//
//                    }
//                });
//    }
//
//    private void checkCodeExpireDate() {
//        Calendar calendar = Calendar.getInstance();
//        int year = calendar.get(Calendar.YEAR);
//        int month = calendar.get(Calendar.MONTH) + 1;
//        int day = calendar.get(Calendar.DAY_OF_MONTH);
//        String todayDate = day +"/"+ month+"/"+year;
//
//        try {
//            SimpleDateFormat sdformat = new SimpleDateFormat("dd/MM/yyyy");
//            Date currentDate = sdformat.parse(todayDate);
//            Date expireDate = sdformat.parse(promoExpDate);
//            if (expireDate.compareTo(currentDate) > 0){
//                checkMinimumOrderPrice();
//            }
//            else if (expireDate.compareTo(currentDate) < 0){
//                Toast.makeText(this, "The promotion code is expired on "+promoExpDate, Toast.LENGTH_SHORT).show();
//                applyBtn.setVisibility(View.GONE);
//                promoDescriptionTv.setVisibility(View.GONE);
//                promoDescriptionTv.setText("");
//            }
//            else if (expireDate.compareTo(currentDate) == 0){
//                checkMinimumOrderPrice();
//            }
//
//        }catch (Exception e){
//            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
//            applyBtn.setVisibility(View.GONE);
//            promoDescriptionTv.setVisibility(View.GONE);
//            promoDescriptionTv.setText("");
//        }
//
//    }
//
//    private void checkMinimumOrderPrice() {
//        if (Double.parseDouble(String.format("%.2f", allTotalPrice)) < Double.parseDouble(promoMinimumOrderPrice)){
//            //current order price doesnt meet the minimum order to apply promo code
//            Toast.makeText(this, "This code is valid for order with minimum amount $"+promoMinimumOrderPrice, Toast.LENGTH_SHORT).show();
//            applyBtn.setVisibility(View.GONE);
//            promoDescriptionTv.setVisibility(View.GONE);
//            promoDescriptionTv.setText("");
//        }else{
//            applyBtn.setVisibility(View.VISIBLE);
//            promoDescriptionTv.setVisibility(View.VISIBLE);
//            promoDescriptionTv.setText(promoDescription);
//        }
//
//    }

    private void submitOrder() {

        progressDialog.setMessage("Placing order. Please wait...");
        progressDialog.show();

        String timestamp = ""+System.currentTimeMillis();
        String cost = allTotalPriceTv.getText().toString().trim().replace("$", "");

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("orderId", ""+timestamp);
        hashMap.put("orderTime", ""+timestamp);
        hashMap.put("orderStatus", "In Progress");
        hashMap.put("orderCost", ""+cost);
        hashMap.put("orderBy", ""+firebaseAuth.getUid());
        hashMap.put("orderTo", ""+shopUid);
        hashMap.put("deliveryFee", " "+ deliveryFee);
        hashMap.put("latitude", ""+myLatitude);
        hashMap.put("longitude",""+myLongitude);
//        if (isPromoCodeApplied){
//            hashMap.put("discount", ""+promoPrice);
//        }else {
//            hashMap.put("discount","0");
//        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(shopUid).child("Orders");
        ref.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(aVoid -> {
                    for (int i=0; i <cartItemList.size();i++){
                        String pId = cartItemList.get(i).getpId();
                        String id = cartItemList.get(i).getId();
                        String cost1 = cartItemList.get(i).getCost();
                        String name = cartItemList.get(i).getName();
                        String price = cartItemList.get(i).getPrice();
                        String quantity = cartItemList.get(i).getQuantity();

                        HashMap<String, String> hashMap1 = new HashMap<>();
                        hashMap1.put("pId", pId);
                        hashMap1.put("name", name);
                        hashMap1.put("cost", cost1);
                        hashMap1.put("price", price);
                        hashMap1.put("quantity", quantity);

                        ref.child(timestamp).child("Items").child(pId).setValue(hashMap1);
                    }
                    progressDialog.dismiss();
                    Toast.makeText(OptionsPaymentOrder.this, "Order placed successfully", Toast.LENGTH_SHORT).show();

                    prepareNotificationMessage(timestamp);

                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(OptionsPaymentOrder.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void prepareNotificationMessage(String orderId){
        String NOTIFICATION_TOPIC = "/topics/" +Constants.FCM_TOPIC;
        String NOTIFICATION_TITLE = "New Order "+orderId;
        String NOTIFICATION_MESSAGE = "New Order Coming In!";
        String NOTIFICATION_TYPE = "NewOrder";

        JSONObject notificationJo = new JSONObject();
        JSONObject notificationBodyJo = new JSONObject();
        try {
            //what to send
            notificationBodyJo.put("notificationType", NOTIFICATION_TYPE);
            notificationBodyJo.put("buyerUid", firebaseAuth.getUid());
            notificationBodyJo.put("sellerId", shopUid);
            notificationBodyJo.put("orderId", orderId);
            notificationBodyJo.put("notificationTitle", NOTIFICATION_TITLE);
            notificationBodyJo.put("notificationMessage", NOTIFICATION_MESSAGE);
            // where to send
            notificationJo.put("to", NOTIFICATION_TOPIC);
            notificationJo.put("data", notificationBodyJo);
        }catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        sendFcmNotification(notificationJo, orderId);
    }

    private void sendFcmNotification(JSONObject notificationJo, String orderId) {
        //send Volley Request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJo, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Intent intent = new Intent(OptionsPaymentOrder.this, OrderDetailsUsersActivity.class);
                intent.putExtra("orderTo", shopUid);
                intent.putExtra("orderId", orderId);
                startActivity(intent);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Intent intent = new Intent(OptionsPaymentOrder.this, OrderDetailsUsersActivity.class);
                intent.putExtra("orderTo", shopUid);
                intent.putExtra("orderId", orderId);
                startActivity(intent);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authotization", "key="+ Constants.FCM_KEY);
                return super.getHeaders();
            }
        };

        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }


}