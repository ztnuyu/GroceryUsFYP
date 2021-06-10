package com.zt.groceryus.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

public class ShopDetailsActivity extends AppCompatActivity {

//    //paypal sdk integration
//    public static final int PAYPAL_REQUEST_CODE = 7171;
//
//    private static PayPalConfiguration config = new PayPalConfiguration()
//            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
//            .clientId(Config.PAYPAL_CLIENT_ID);

    private ImageView shopIv;
    private TextView shopNameTv, phoneTv, emailTv, openCloseTv, deliveryFeeTv, addressTv, filteredProductsTv, cartCountTv;
    private ImageButton callBtn, mapBtn, cartBtn, backBtn, filterProductBtn, reviewsBtn;
    private EditText searchProductEt;
    private RecyclerView productsRv;
    private RatingBar ratingBar;

    public String shopUid, orderId;
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

    public String productQuantity, tQuantity, quantity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_details);

//        //Start PayPal Service
//        Intent intent1 = new Intent(this, PayPalService.class);
//        intent1.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
//        startService(intent1);



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


        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        shopUid = getIntent().getStringExtra("shopUid");
        firebaseAuth = FirebaseAuth.getInstance();
        loadMyInfo();
        loadShopDetails();
        loadShopProducts();
        loadReviews();

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
        deleteCartData();
        cartCount();

        //search
        searchProductEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    adapterProductUser.getFilter().filter(s);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        backBtn.setOnClickListener(v -> onBackPressed());

        cartBtn.setOnClickListener(v -> showCartDialog());

        callBtn.setOnClickListener(v -> dialPhone());

        mapBtn.setOnClickListener(v -> openMap());

        filterProductBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ShopDetailsActivity.this);
            builder.setTitle("Choose Category:")
                    .setItems(Constants.productCategories1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //get selected item
                            String selected = Constants.productCategories1[which];
                            filteredProductsTv.setText(selected);
                            if (selected.equals("All")){
                                loadShopProducts();
                            }else {
                                adapterProductUser.getFilter().filter(selected);
                            }
                        }
                    })
                    .show();
        });

        reviewsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ShopDetailsActivity.this, ShopReviewsActivity.class);
            intent.putExtra("shopUid", shopUid);
            startActivity(intent);
        });


    }

    private float ratingSum = 0;
    private void loadReviews() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).child("Ratings")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        ratingSum = 0;
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            float rating = Float.parseFloat(""+ds.child("ratings").getValue());
                            ratingSum = ratingSum + rating;
                        }

                        long numberOfReviews = dataSnapshot.getChildrenCount();
                        float avgRating = ratingSum/numberOfReviews;
                        ratingBar.setRating(avgRating);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void deleteCartData() {
        easyDB.deleteAllDataFromTable();
    }

    public void cartCount(){
        int count = easyDB.getAllData().getCount();
        if (count <=0){
            cartCountTv.setVisibility(View.GONE);
        }else{
            cartCountTv.setVisibility(View.VISIBLE);
            cartCountTv.setText(""+count);
        }
    }

    public double allTotalPrice = 0.00;
    //need to access these views in adapter
    public TextView sTotalTv, dFeeTv, allTotalPriceTv, promoDescriptionTv, discountTv;
    public EditText promoCodeEt;
    public Button applyBtn;
    public boolean promoCodeApplied = false;


    //nak order! place order dialog
    private void showCartDialog() {

        cartItemList = new ArrayList<>();

        //inflate cart layout
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cart, null);
        //init view
        TextView shopNameTv = view.findViewById(R.id.shopNameTv);
        promoCodeEt = view.findViewById(R.id.promoCodeEt);
        promoDescriptionTv = view.findViewById(R.id.promoDescriptionTv);
        discountTv = view.findViewById(R.id.discountTv);
        FloatingActionButton validateBtn = view.findViewById(R.id.validateBtn);
        applyBtn = view.findViewById(R.id.applyBtn);
        RecyclerView cartItemsRv = view.findViewById(R.id.cartItemsRv);
        sTotalTv = view.findViewById(R.id.sTotalTv);
        dFeeTv = view.findViewById(R.id.dFeeTv);
        allTotalPriceTv= view.findViewById(R.id.totalTv);

        Button checkoutBtn = view.findViewById(R.id.checkoutBtn);
        Button checkoutBtnStripe = view.findViewById(R.id.checkoutBtnStripe);


        //whenever cart dailog shows, check if promo cocde is applied or not
        if (isPromoCodeApplied){
            promoDescriptionTv.setVisibility(View.VISIBLE);
            applyBtn.setVisibility(View.VISIBLE);
            applyBtn.setText("Applied");
            promoCodeEt.setText(promoCode);
            promoDescriptionTv.setText(promoDescription);
        }else {
            promoDescriptionTv.setVisibility(View.GONE);
            applyBtn.setVisibility(View.GONE);
            applyBtn.setText("Apply");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        shopNameTv.setText(shopName);

        EasyDB easyDB = EasyDB.init(this, "ITEMS_DB")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id", new String[]{"text", "unique"}))
                .addColumn(new Column("Item_PID", new String[]{"text", "not null"}))
                .addColumn(new Column("Item_Name", new String[]{"text", "not null"}))
                .addColumn(new Column("Item_Price_Each", new String[]{"text", "not null"}))
                .addColumn(new Column("Item_Price", new String[]{"text", "not null"}))
                .addColumn(new Column("Item_Quantity", new String[]{"text", "not null"}))
                .doneTableColumn();

        //get record from db
        Cursor res = easyDB.getAllData();
        if(res!=null && res.getCount() > 0) {
            while (res.moveToNext()){
                String id = res.getString(1);
                String pId = res.getString(2);
                String name = res.getString(3);
                String price = res.getString(4);
                String cost = res.getString(5);
                String quantity = res.getString(6);

                allTotalPrice = allTotalPrice + Double.parseDouble(cost);

                ModelCartItem modelCartItem = new ModelCartItem(
                        ""+id,
                        ""+pId,
                        ""+name,
                        ""+price,
                        ""+cost,
                        ""+quantity
                );

                cartItemList.add(modelCartItem);

            }
        }


        adapterCartItem = new AdapterCartItem(this, cartItemList);
        cartItemsRv.setAdapter(adapterCartItem);

        if (isPromoCodeApplied){
            priceWithDiscount();
        }else{
            priceWithoutDiscount();
        }

        //show dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        //reset total price on dialog dismiss
        dialog.setOnCancelListener(dialog1 -> allTotalPrice = 0.00);

        checkoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //validate delivery address
                if (myLatitude.equals("")|| myLatitude.equals("null")|| myLongitude.equals("")|| myLongitude.equals("null")){
                    //user didint enter address
                    Toast.makeText(ShopDetailsActivity.this, "Please insert your address before placing orders. Thank you", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (myPhone.equals("")|| myPhone.equals("null")){
                    //user dont have phone num
                    Toast.makeText(ShopDetailsActivity.this, "Please insert your phone number before placing orders. Thank you", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (cartItemList.size() == 0){
                    //cart is empty
                    Toast.makeText(ShopDetailsActivity.this, "No items in the cart! Cant proceed order", Toast.LENGTH_SHORT).show();
                    return;
                }

                submitOrder();
            }
        });


        checkoutBtnStripe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //validate delivery address
                if (myLatitude.equals("")|| myLatitude.equals("null")|| myLongitude.equals("")|| myLongitude.equals("null")){
                    //user didint enter address
                    Toast.makeText(ShopDetailsActivity.this, "Please insert your address before placing orders. Thank you", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (myPhone.equals("")|| myPhone.equals("null")){
                    //user dont have phone num
                    Toast.makeText(ShopDetailsActivity.this, "Please insert your phone number before placing orders. Thank you", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (cartItemList.size() == 0){
                    //cart is empty
                    Toast.makeText(ShopDetailsActivity.this, "No items in the cart! Cant proceed order", Toast.LENGTH_SHORT).show();
                    return;
                }

                submitOrderStripe();

            }
        });





        validateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * get code from et- if not empty promomaybe applied
                 * check if code is valid
                 * check if expired
                 * check minimum order price minimumOrderPrice >= SubtotalPrice
                 * */
                String promotionCode = promoCodeEt.getText().toString().trim();
                if (TextUtils.isEmpty(promotionCode)){
                    Toast.makeText(ShopDetailsActivity.this, "Please enter promo code", Toast.LENGTH_SHORT).show();
                }else {
                    checkCodeAvailability(promotionCode);
                }
            }
        });

        //will visible only if code is valid
        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPromoCodeApplied = true;
                applyBtn.setText("Applied");

                priceWithDiscount();
            }
        });

    }

    private void submitOrderStripe() {


        progressDialog.setMessage("Placing order. Please wait...");
        progressDialog.show();


        String timestamp = ""+System.currentTimeMillis();
        String cost = allTotalPriceTv.getText().toString().trim().replace("$", "");

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("orderId", ""+timestamp);
        hashMap.put("orderTime", ""+timestamp);
        hashMap.put("orderStatus", "To Pay");
        hashMap.put("orderCost", ""+cost);
        hashMap.put("orderBy", ""+firebaseAuth.getUid());
        hashMap.put("orderTo", ""+shopUid);
        hashMap.put("deliveryFee", " "+ deliveryFee);
        hashMap.put("latitude", ""+myLatitude);
        hashMap.put("longitude",""+myLongitude);
        if (isPromoCodeApplied){
            hashMap.put("discount", ""+promoPrice);
        }else {
            hashMap.put("discount","0");
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(shopUid).child("Orders");
        ref.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(aVoid -> {
                    for (int i=0; i <cartItemList.size();i++){
                        String pId = cartItemList.get(i).getpId();
                        String id = cartItemList.get(i).getId();
                        String cost1 = cartItemList.get(i).getCost();
                        String name = cartItemList.get(i).getName();
                        String price = cartItemList.get(i).getPrice();

                        quantity = cartItemList.get(i).getQuantity();
                        productQuantity = productsList.get(i).getProductQuantity();
                        tQuantity = String.valueOf(Integer.valueOf(productQuantity) - Integer.valueOf(quantity));

                        HashMap<String, String> hashMap1 = new HashMap<>();
                        hashMap1.put("pId", pId);
                        hashMap1.put("name", name);
                        hashMap1.put("cost", cost1);
                        hashMap1.put("price", price);
                        hashMap1.put("quantity", quantity);

                        DatabaseReference ref1 =FirebaseDatabase.getInstance().getReference("Users").child(shopUid).child("Products").child(pId);
                        Map<String, Object> updates = new HashMap<String,Object>();
                        updates.put("productQuantity", tQuantity);

                        ref1.updateChildren(updates);
                        ref.child(timestamp).child("Items").child(pId).setValue(hashMap1);
                    }

                    String test;
                    if(isPromoCodeApplied) {
                        test = Double.toString(Double.valueOf(allTotalPrice + Double.valueOf(deliveryFee))- Double.valueOf(promoPrice));
                    }else{
                        test = Double.toString(Double.valueOf(allTotalPrice + Double.valueOf(deliveryFee)));
                    }
                    Intent intent = new Intent(ShopDetailsActivity.this, CheckoutActivityJava.class);
                    intent.putExtra("tp", test);
                    intent.putExtra("orderTo", shopUid);
                    intent.putExtra("orderId", timestamp);
                    startActivity(intent);

                    progressDialog.dismiss();
                    Toast.makeText(ShopDetailsActivity.this, "Order placed successfully", Toast.LENGTH_SHORT).show();
                    prepareNotificationMessage(timestamp);

                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(ShopDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }


    private void priceWithDiscount(){
        discountTv.setText("$"+promoPrice);
        dFeeTv.setText("$"+deliveryFee);
        sTotalTv.setText("$"+String.format("%.2f", allTotalPrice));
        allTotalPriceTv.setText("$"+(allTotalPrice+ Double.parseDouble(deliveryFee.replace("$","")) - Double.parseDouble(promoPrice)));
    }

    private void priceWithoutDiscount(){
        discountTv.setText("$0");
        dFeeTv.setText("$"+deliveryFee);
        sTotalTv.setText("$"+String.format("%.2f", allTotalPrice));
        allTotalPriceTv.setText("$"+(allTotalPrice+ Double.parseDouble(deliveryFee.replace("$",""))));
    }

    public boolean isPromoCodeApplied = false;
    public String promoId, promoTimestamp, promoCode, promoDescription, promoExpDate, promoMinimumOrderPrice, promoPrice;
    private void checkCodeAvailability(String promotionCode){
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Checking Promotion Codes");
        progressDialog.setCanceledOnTouchOutside(false);

        //proomo not applied
        isPromoCodeApplied = false;
        applyBtn.setText("Apply");
        priceWithoutDiscount();

        //check promo code availability
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).child("Promotions").orderByChild("promoCode").equalTo(promotionCode)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //check ifpromo exist
                        if (snapshot.exists()){
                            progressDialog.dismiss();
                            for (DataSnapshot ds: snapshot.getChildren()){
                                promoId = ""+ds.child("id").getValue();
                                promoTimestamp = ""+ds.child("timestamp").getValue();
                                promoCode = ""+ds.child("promoCode").getValue();
                                promoDescription = ""+ds.child("description").getValue();
                                promoExpDate = ""+ds.child("expireDate").getValue();
                                promoMinimumOrderPrice = ""+ds.child("minimumOrderPrice").getValue();
                                promoPrice = ""+ds.child("promoPrice").getValue();

                                //check code is expired
                                checkCodeExpireDate();
                            }
                        }
                        else{
                            //promo code doesnt exist
                            progressDialog.dismiss();
                            Toast.makeText(ShopDetailsActivity.this, "Invalid promo code", Toast.LENGTH_SHORT).show();
                            applyBtn.setVisibility(View.GONE);
                            promoDescriptionTv.setVisibility(View.GONE);
                            promoDescriptionTv.setText("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void checkCodeExpireDate() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String todayDate = day +"/"+ month+"/"+year;

        try {
            SimpleDateFormat sdformat = new SimpleDateFormat("dd/MM/yyyy");
            Date currentDate = sdformat.parse(todayDate);
            Date expireDate = sdformat.parse(promoExpDate);
            if (expireDate.compareTo(currentDate) > 0){
                checkMinimumOrderPrice();
            }
            else if (expireDate.compareTo(currentDate) < 0){
                Toast.makeText(this, "The promotion code is expired on "+promoExpDate, Toast.LENGTH_SHORT).show();
                applyBtn.setVisibility(View.GONE);
                promoDescriptionTv.setVisibility(View.GONE);
                promoDescriptionTv.setText("");
            }
            else if (expireDate.compareTo(currentDate) == 0){
                checkMinimumOrderPrice();
            }

        }catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            applyBtn.setVisibility(View.GONE);
            promoDescriptionTv.setVisibility(View.GONE);
            promoDescriptionTv.setText("");
        }

    }

    private void checkMinimumOrderPrice() {
        if (Double.parseDouble(String.format("%.2f", allTotalPrice)) < Double.parseDouble(promoMinimumOrderPrice)){
            //current order price doesnt meet the minimum order to apply promo code
            Toast.makeText(this, "This code is valid for order with minimum amount $"+promoMinimumOrderPrice, Toast.LENGTH_SHORT).show();
            applyBtn.setVisibility(View.GONE);
            promoDescriptionTv.setVisibility(View.GONE);
            promoDescriptionTv.setText("");
        }else{
            applyBtn.setVisibility(View.VISIBLE);
            promoDescriptionTv.setVisibility(View.VISIBLE);
            promoDescriptionTv.setText(promoDescription);
        }

    }


    public void submitOrder() {

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
        if (isPromoCodeApplied){
            hashMap.put("discount", ""+promoPrice);
        }else {
            hashMap.put("discount","0");
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(shopUid).child("Orders");
        ref.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(aVoid -> {
                    for (int i=0; i <cartItemList.size();i++){
                        String pId = cartItemList.get(i).getpId();
                        String id = cartItemList.get(i).getId();
                        String cost1 = cartItemList.get(i).getCost();
                        String name = cartItemList.get(i).getName();
                        String price = cartItemList.get(i).getPrice();

                        quantity = cartItemList.get(i).getQuantity();
                        productQuantity = productsList.get(i).getProductQuantity();
                        tQuantity = String.valueOf(Integer.valueOf(productQuantity) - Integer.valueOf(quantity));

                        HashMap<String, String> hashMap1 = new HashMap<>();
                        hashMap1.put("pId", pId);
                        hashMap1.put("name", name);
                        hashMap1.put("cost", cost1);
                        hashMap1.put("price", price);
                        hashMap1.put("quantity", quantity);

                        DatabaseReference ref1 =FirebaseDatabase.getInstance().getReference("Users").child(shopUid).child("Products").child(pId);
                        Map<String, Object> updates = new HashMap<String,Object>();
                        updates.put("productQuantity", tQuantity);

                        ref1.updateChildren(updates);
                        ref.child(timestamp).child("Items").child(pId).setValue(hashMap1);
                    }
                    progressDialog.dismiss();
                    Toast.makeText(ShopDetailsActivity.this, "Order placed successfully", Toast.LENGTH_SHORT).show();

                    prepareNotificationMessage(timestamp);

                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(ShopDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    private void openMap() {
        String address = "https://maps.goolge.com/maps?saddr="+ myLatitude + "," +myLongitude + "&daddr=" + shopLatitude + "," + shopLongitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(address));
        startActivity(intent);
    }

    private void dialPhone() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+Uri.encode(shopPhone))));
        Toast.makeText(this, ""+shopPhone, Toast.LENGTH_SHORT).show();

    }

    private void loadMyInfo() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            String name = ""+ds.child("name").getValue();
                            String email = ""+ds.child("email").getValue();
                            myPhone = ""+ds.child("phone").getValue();
                            String profileImage = ""+ds.child("profileImage").getValue();
                            String accountType = ""+ds.child("accountType").getValue();
                            String city = ""+ds.child("city").getValue();
                            myLatitude = ""+ds.child("latitude").getValue();
                            myLongitude = ""+ds.child("longitude").getValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadShopDetails() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = ""+dataSnapshot.child("name").getValue();
                shopName = ""+dataSnapshot.child("shopName").getValue();
                shopEmail = ""+dataSnapshot.child("email").getValue();
                shopPhone = ""+dataSnapshot.child("phone").getValue();
                shopLatitude = ""+dataSnapshot.child("latitude").getValue();
                shopAddress = ""+dataSnapshot.child("address").getValue();
                shopLongitude= ""+dataSnapshot.child("longitude").getValue();
                deliveryFee = ""+dataSnapshot.child("deliveryFee").getValue();
                String profileImage = ""+dataSnapshot.child("profileImage").getValue();
                String shopOpen = ""+dataSnapshot.child("shopOpen").getValue();

                shopNameTv.setText(shopName);
                emailTv.setText(shopEmail);
                deliveryFeeTv.setText("Delivery Fee: $"+deliveryFee);
                addressTv.setText(shopAddress);
                phoneTv.setText(shopPhone);
                if (shopOpen.equals("true")){
                    openCloseTv.setText("Open");
                }else {
                    openCloseTv.setText("Close");
                }

                try {
                    Picasso.get().load(profileImage).into(shopIv);
                }catch (Exception e){

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadShopProducts() {
        productsList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(shopUid).child("Products")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        productsList.clear();
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            ModelProduct modelProduct = ds.getValue(ModelProduct.class);
                            productsList.add(modelProduct);
                        }
                        adapterProductUser = new AdapterProductUser(ShopDetailsActivity.this, productsList);
                        productsRv.setAdapter(adapterProductUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
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
        // send Volley Request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJo, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Intent intent = new Intent(ShopDetailsActivity.this, OrderDetailsUsersActivity.class);
                intent.putExtra("orderTo", shopUid);
                intent.putExtra("orderId", orderId);
                startActivity(intent);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Intent intent = new Intent(ShopDetailsActivity.this, OrderDetailsUsersActivity.class);
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
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(jsonObjectRequest);

    }

}