package com.upitranzact.sdk;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.upitranzact.sdk.network.PaymentCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CheckoutActivity extends AppCompatActivity {

    private Context context;
    LinearLayout screen_loading;
    TextView amount_tv;
    private Handler handler;
    private Runnable pollingRunnable;
    private String orderId;
    private Button requestButton;
    private CountDownTimer countDownTimer;
    private BottomSheetDialog failedOrSuccessPaymentDialog;
    private BottomSheetDialog requestPaymentDialog;
    private BottomSheetDialog processingPaymentDialog;
    private BottomSheetDialog cancelPaymentDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.gray));

        context = CheckoutActivity.this;

        screen_loading = findViewById(R.id.screen_loading);
        amount_tv = findViewById(R.id.amount_tv);
        requestButton = findViewById(R.id.requestButton);

        RelativeLayout showDialogButton = findViewById(R.id.cancel_payment);
        showDialogButton.setOnClickListener(v -> cancelPaymentDialog(this));

        Intent intent = getIntent();
        String publicKey = Objects.requireNonNull(intent.getStringExtra("publicKey")).trim();
        String secretKey = Objects.requireNonNull(intent.getStringExtra("secretKey")).trim();
        String mid = Objects.requireNonNull(intent.getStringExtra("mid")).trim();
        String amount = Objects.requireNonNull(intent.getStringExtra("amount")).trim();
        orderId = Objects.requireNonNull(intent.getStringExtra("orderId")).trim();
        String customerName = Objects.requireNonNull(intent.getStringExtra("customerName")).trim();
        String customerEmail = Objects.requireNonNull(intent.getStringExtra("customerEmail")).trim();
        String customerMobile = Objects.requireNonNull(intent.getStringExtra("customerMobile")).trim();

        if (amount == null || Integer.parseInt(amount) <= 0) {
            Toast.makeText(context, "Please enter a valid amount!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isNullOrEmpty(publicKey) || isNullOrEmpty(secretKey) || isNullOrEmpty(mid) || isNullOrEmpty(amount)
                || isNullOrEmpty(orderId) || isNullOrEmpty(customerName)
                || isNullOrEmpty(customerEmail) || isNullOrEmpty(customerMobile)) {

            showErrorAndExit();
            return;
        }

        createOrder(publicKey, secretKey, mid, amount, orderId, customerName, customerEmail,
                customerMobile);

        checkPaymentStatusWithPolling(publicKey, secretKey, mid, orderId);

        startCountdownTimer(5, context);
    }

    public void createOrder(String publicKey, String secretKey, String mid, String amount, String orderId, String customerName, String customerEmail, String customerMobile) {

        screen_loading.setVisibility(View.VISIBLE);

        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "https://api.upitranzact.com/v1/payments/createPaymentIntent", response -> {
            Log.d("API Response", "Response: " + response);
            try {
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.getBoolean("status")) {
                    screen_loading.setVisibility(View.GONE);
                    String dynamicQR = jsonResponse.getJSONObject("data").getString("dynamicQR");
                    amount_tv.setText(String.format("INR%s", jsonResponse.getJSONObject("data").getString("amount")));
                    ImageView qrImageView = findViewById(R.id.qr_code);

                    if (dynamicQR.contains(",")) {
                        dynamicQR = dynamicQR.split(",")[1];
                    }

                    byte[] decodedBytes = Base64.decode(dynamicQR, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    qrImageView.setImageBitmap(bitmap);


                    RelativeLayout save_qr_code = findViewById(R.id.save_qr_code);
                    save_qr_code.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            saveImageToGallery(bitmap, orderId);
                        }
                    });

                    RelativeLayout gpay = findViewById(R.id.gpay);
                    gpay.setOnClickListener(v -> {
                        try {
                            processingPaymentDialog(context);
                            String upiLink = jsonResponse.getJSONObject("data").getString("gpay");
                            openUPIIntent(upiLink);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(CheckoutActivity.this, "Error parsing JSON: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                    RelativeLayout phonepe = findViewById(R.id.phonepe);
                    phonepe.setOnClickListener(v -> {
                        try {
                            processingPaymentDialog(context);
                            String upiLink = jsonResponse.getJSONObject("data").getString("phonepe");
                            openUPIIntent(upiLink);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(CheckoutActivity.this, "Error parsing JSON: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                    RelativeLayout paytm = findViewById(R.id.paytm);
                    paytm.setOnClickListener(v -> {
                        try {
                            processingPaymentDialog(context);
                            String upiLink = jsonResponse.getJSONObject("data").getString("paytm");
                            openUPIIntent(upiLink);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(CheckoutActivity.this, "Error parsing JSON: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                    RelativeLayout cred = findViewById(R.id.cred);
                    cred.setOnClickListener(v -> {
                        try {
                            processingPaymentDialog(context);
                            String upiLink = jsonResponse.getJSONObject("data").getString("cred");
                            openUPIIntent(upiLink);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(CheckoutActivity.this, "Error parsing JSON: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                    RelativeLayout bhim = findViewById(R.id.bhim);
                    bhim.setOnClickListener(v -> {
                        try {
                            processingPaymentDialog(context);
                            String upiLink = jsonResponse.getJSONObject("data").getString("intent");
                            openUPIIntent(upiLink);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(CheckoutActivity.this, "Error parsing JSON: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                    EditText upiEditText = findViewById(R.id.upiEditText);
                    requestButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestButton.setText("Requesting...");
                            String upiId = upiEditText.getText().toString().trim();
                            String upiPattern = "^[\\w.-]+@[\\w.-]+$";

                            if (upiId.isEmpty() || !upiId.matches(upiPattern)) {
                                requestButton.setText("Verify and Pay");
                                Toast.makeText(CheckoutActivity.this, "Please enter a valid UPI ID!!",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                createPaymentRequest(publicKey, secretKey, mid, amount, orderId, upiId,
                                        customerName, customerEmail, customerMobile);
                            }

                        }
                    });

                } else {
                    notifyPaymentResult(false, orderId, "Payment failed: " + jsonResponse.getString("msg"));
                }
            } catch (JSONException e) {
                notifyPaymentResult(false, orderId, "Response parse error: " + e.getMessage());
            }
        }, error -> {
            if (error.networkResponse != null) {
                try {
                    String errorData = new String(error.networkResponse.data);
                    JSONObject errorResponse = new JSONObject(errorData);

                    String errorMessage = errorResponse.getString("msg");
                    Log.e("API Error", "Message: " + errorMessage);
                    notifyPaymentResult(false, orderId, errorMessage);

                } catch (JSONException e) {
                    Log.e("API Error", "Response parse error: " + e.getMessage());
                    notifyPaymentResult(false, orderId, "Response parse error: " + e.getMessage());
                }
            } else {
                Log.e("API Error", "Network Error: " + error.getMessage());
                notifyPaymentResult(false, orderId, "Network error: " + error.getMessage());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mid", mid);
                params.put("amount", amount);
                params.put("order_id", orderId);
                params.put("redirect_url", "https://upitranzact.com");
                params.put("note", "Add money");
                params.put("customer_name", customerName);
                params.put("customer_email", customerEmail);
                params.put("customer_mobile", customerMobile);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String authHeader = "Basic "
                        + Base64.encodeToString((publicKey + ":" + secretKey).getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", authHeader);
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)");
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);
    }

    public void checkPaymentStatusWithPolling(final String publicKey, final String secretKey, final String mid,
                                              final String orderId) {
        handler = new Handler();
        final int pollingInterval = 10000;
        final int maxAttempts = 30;
        final int[] attemptCounter = {0};

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (attemptCounter[0] >= maxAttempts) {
                    Toast.makeText(context, "Max attempts reached. Payment status not confirmed.", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                attemptCounter[0]++;

                StringRequest stringRequest = new StringRequest(Request.Method.POST,
                        "https://api.upitranzact.com/v1/payments/checkPaymentStatus", response -> {
                    Log.d("API Response", "Response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String txnStatus = jsonResponse.getString("txnStatus");
                        if (jsonResponse.getBoolean("status")) {
                            switch (txnStatus) {
                                case "SUCCESS":
                                    failedOrSuccessPaymentDialog(context, "SUCCESS", orderId);
                                    break;
                                case "FAIL":
                                    failedOrSuccessPaymentDialog(context, "FAIL", orderId);
                                    break;
                                case "REJECT":
                                    failedOrSuccessPaymentDialog(context, "REJECT", orderId);
                                    break;
                                default:
                                    failedOrSuccessPaymentDialog(context, "FAILED", orderId);
                                    break;
                            }
                        } else {
                            switch (txnStatus) {
                                case "FAIL":
                                    failedOrSuccessPaymentDialog(context, "FAIL", orderId);
                                    break;
                                case "REJECT":
                                    failedOrSuccessPaymentDialog(context, "REJECT", orderId);
                                    break;
                                default:
                                    break;
                            }
                        }
                    } catch (JSONException e) {
                        Toast.makeText(context, "Response parse error: " + e.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                }, error -> {
                    if (error.networkResponse != null) {
                        try {
                            String errorData = new String(error.networkResponse.data);
                            JSONObject errorResponse = new JSONObject(errorData);
                            String errorMessage = errorResponse.getString("msg");
                            Log.e("API Error", "Message: " + errorMessage);
                            Toast.makeText(context, "Message: " + errorMessage, Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Log.e("API Error", "Response parse error: " + e.getMessage());
                            Toast.makeText(context, "Response parse error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("API Error", "Network Error: " + error.getMessage());
                        Toast.makeText(context, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<>();
                        params.put("mid", mid);
                        params.put("order_id", orderId);
                        return params;
                    }

                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        String authHeader = "Basic "
                                + Base64.encodeToString((publicKey + ":" + secretKey).getBytes(), Base64.NO_WRAP);
                        headers.put("Authorization", authHeader);
                        headers.put("Content-Type", "application/x-www-form-urlencoded");
                        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)");
                        return headers;
                    }
                };

                RequestQueue requestQueue = Volley.newRequestQueue(context);
                requestQueue.add(stringRequest);

                handler.postDelayed(this, pollingInterval);
            }
        };

        handler.post(pollingRunnable);
    }

    public void createPaymentRequest(String publicKey, String secretKey, String mid, String amount, String orderId,
                                     String vpa, String customerName, String customerEmail, String customerMobile) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "https://api.upitranzact.com/v1/payments/paymentRequestForOrder", response -> {
            Log.d("API Response", "Response: " + response);
            try {
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.getBoolean("status")) {
                    requestButton.setText("Verify and Pay");
                    requestPaymentDialog(context);
                }
            } catch (JSONException e) {
                requestButton.setText("Verify and Pay");
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, error -> {
            if (error.networkResponse != null) {
                try {
                    String errorData = new String(error.networkResponse.data);
                    JSONObject errorResponse = new JSONObject(errorData);

                    String errorMessage = errorResponse.getString("msg");
                    Log.e("API Error", "Message: " + errorMessage);
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                    requestButton.setText("Verify and Pay");
                } catch (JSONException e) {
                    Log.e("Network Error", "Response parse error: " + e.getMessage());
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    requestButton.setText("Verify and Pay");
                }
            } else {
                Log.e("Network Error", "Network error: " + error.getMessage());
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                requestButton.setText("Verify and Pay");
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mid", mid);
                params.put("amount", amount);
                params.put("order_id", orderId);
                params.put("vpa", vpa);
                params.put("note", "Add money");
                params.put("customer_name", customerName);
                params.put("customer_email", customerEmail);
                params.put("customer_mobile", customerMobile);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String authHeader = "Basic "
                        + Base64.encodeToString((publicKey + ":" + secretKey).getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", authHeader);
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)");
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);
    }

    public void failedOrSuccessPaymentDialog(Context context, String status, String orderId) {
        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.failed_or_success_bottom_sheet_layout,
                null);

        failedOrSuccessPaymentDialog = new BottomSheetDialog(context, R.style.CustomBottomSheetDialogTheme);
        failedOrSuccessPaymentDialog.setContentView(bottomSheetView);
        failedOrSuccessPaymentDialog.setCanceledOnTouchOutside(false);

        ViewGroup parent = (ViewGroup) bottomSheetView.getParent();
        parent.setBackgroundResource(android.R.color.transparent);

        LottieAnimationView lottieAnimationView = parent.findViewById(R.id.lottieAnimationView);
        TextView status_tv = parent.findViewById(R.id.status_tv);
        TextView order_id = parent.findViewById(R.id.order_id);
        TextView timer = parent.findViewById(R.id.timer);

        order_id.setText(orderId);

        if (Objects.equals(status, "SUCCESS")) {
            status_tv.setText(R.string.payment_successful);
            lottieAnimationView.setAnimation(R.raw.success_animation);
            lottieAnimationView.setRepeatCount(LottieDrawable.INFINITE);
            lottieAnimationView.playAnimation();
            new CountDownTimer(5000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsRemaining = (int) (millisUntilFinished / 1000);
                    timer.setText(String.valueOf(secondsRemaining));
                }

                @Override
                public void onFinish() {
                    failedOrSuccessPaymentDialog.dismiss();
                    notifyPaymentResult(true, orderId, "Payment Successful");
                }
            }.start();
        } else if (Objects.equals(status, "FAIL")) {
            status_tv.setText(R.string.payment_failed);
            lottieAnimationView.setAnimation(R.raw.failed_animation);
            lottieAnimationView.setRepeatCount(LottieDrawable.INFINITE);
            lottieAnimationView.playAnimation();
            new CountDownTimer(5000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsRemaining = (int) (millisUntilFinished / 1000);
                    timer.setText(String.valueOf(secondsRemaining));
                }

                @Override
                public void onFinish() {
                    failedOrSuccessPaymentDialog.dismiss();
                    notifyPaymentResult(false, orderId, "Payment Failed");
                }
            }.start();
        } else if (Objects.equals(status, "REJECT")) {
            status_tv.setText(R.string.payment_pending);
            lottieAnimationView.setAnimation(R.raw.failed_animation);
            lottieAnimationView.setRepeatCount(LottieDrawable.INFINITE);
            lottieAnimationView.playAnimation();
            new CountDownTimer(5000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsRemaining = (int) (millisUntilFinished / 1000);
                    timer.setText(String.valueOf(secondsRemaining));
                }

                @Override
                public void onFinish() {
                    failedOrSuccessPaymentDialog.dismiss();
                    notifyPaymentResult(false, orderId, "Payment Rejected");
                }
            }.start();
        } else {
            status_tv.setText(R.string.payment_failed);
            lottieAnimationView.setAnimation(R.raw.failed_animation);
            lottieAnimationView.setRepeatCount(LottieDrawable.INFINITE);
            lottieAnimationView.playAnimation();
            new CountDownTimer(5000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsRemaining = (int) (millisUntilFinished / 1000);
                    timer.setText(String.valueOf(secondsRemaining));
                }

                @Override
                public void onFinish() {
                    failedOrSuccessPaymentDialog.dismiss();
                    notifyPaymentResult(false, orderId, "Payment Failed");
                }
            }.start();
        }

        failedOrSuccessPaymentDialog.show();
    }

    public void requestPaymentDialog(Context context) {
        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.request_bottom_sheet_layout, null);

        requestPaymentDialog = new BottomSheetDialog(context, R.style.CustomBottomSheetDialogTheme);
        requestPaymentDialog.setContentView(bottomSheetView);
        requestPaymentDialog.setCanceledOnTouchOutside(false);

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setDraggable(false);

        ViewGroup parent = (ViewGroup) bottomSheetView.getParent();
        parent.setBackgroundResource(android.R.color.transparent);

        Button cancel_payment = parent.findViewById(R.id.cancel_payment);

        cancel_payment.setOnClickListener(v -> requestPaymentDialog.dismiss());

        requestPaymentDialog.show();
    }

    public void processingPaymentDialog(Context context) {
        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.processing_bottom_sheet_layout, null);

        processingPaymentDialog = new BottomSheetDialog(context, R.style.CustomBottomSheetDialogTheme);
        processingPaymentDialog.setContentView(bottomSheetView);
        processingPaymentDialog.setCanceledOnTouchOutside(false);

        ViewGroup parent = (ViewGroup) bottomSheetView.getParent();
        parent.setBackgroundResource(android.R.color.transparent);

        Button cancel_payment = parent.findViewById(R.id.cancel_payment);

        cancel_payment.setOnClickListener(v -> processingPaymentDialog.dismiss());

        processingPaymentDialog.show();
    }

    public void cancelPaymentDialog(Context context) {

        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.cancel_bottom_sheet_layout, null);

        cancelPaymentDialog = new BottomSheetDialog(context, R.style.CustomBottomSheetDialogTheme);
        cancelPaymentDialog.setContentView(bottomSheetView);

        ViewGroup parent = (ViewGroup) bottomSheetView.getParent();
        parent.setBackgroundResource(android.R.color.transparent);

        Button continueBtn = parent.findViewById(R.id.continueBtn);
        Button cancelBtn = parent.findViewById(R.id.cancelBtn);

        continueBtn.setOnClickListener(v -> cancelPaymentDialog.dismiss());

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelPaymentDialog.dismiss();
                notifyPaymentResult(false, orderId, "Payment cancelled by user");
            }
        });

        cancelPaymentDialog.show();
    }

    private void openUPIIntent(String upiLink) {
        try {
            Uri uri = Uri.parse(upiLink);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);

            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to open UPI intent: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveImageToGallery(Bitmap bitmap, String fileName) {
        try {
            OutputStream outputStream;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/YourAppName");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                if (imageUri != null) {
                    outputStream = resolver.openOutputStream(imageUri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    Toast.makeText(this, "Image saved to gallery!", Toast.LENGTH_SHORT).show();
                }
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        .toString() + "/YourAppName";
                File directory = new File(imagesDir);

                if (!directory.exists()) {
                    directory.mkdirs();
                }

                File file = new File(directory, fileName + ".png");
                outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();

                MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
                Toast.makeText(this, "Image saved to gallery!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public void startCountdownTimer(int minutes, Context context) {
        long durationInMillis = (long) minutes * 60 * 1000;

        countDownTimer = new CountDownTimer(durationInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;

                @SuppressLint("DefaultLocale")
                String time = String.format("%d:%02d", minutes, seconds);
                TextView timestamp = findViewById(R.id.timestamp);
                timestamp.setText(time);
            }

            @Override
            public void onFinish() {
                notifyPaymentResult(false, orderId, "Payment timeout");
            }
        }.start();
    }

    private void notifyPaymentResult(boolean isSuccess, String order_id, String message) {
        PaymentCallback callback = UpiTranzactSDK.getPaymentCallback(); // Retrieve the callback
        if (callback != null) {
            if (isSuccess) {
                callback.onPaymentSuccess(order_id, message);
            } else {
                callback.onPaymentFailed(order_id, message);
            }
        }
        finish();
    }

    private void showErrorAndExit() {
        Toast.makeText(context, "Missing required data", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void stopPolling() {
        if (handler != null && pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
            handler = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (failedOrSuccessPaymentDialog != null && failedOrSuccessPaymentDialog.isShowing()) {
            failedOrSuccessPaymentDialog.dismiss();
        }
        if (requestPaymentDialog != null && requestPaymentDialog.isShowing()) {
            requestPaymentDialog.dismiss();
        }
        if (processingPaymentDialog != null && processingPaymentDialog.isShowing()) {
            processingPaymentDialog.dismiss();
        }
        if (cancelPaymentDialog != null && cancelPaymentDialog.isShowing()) {
            cancelPaymentDialog.dismiss();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopPolling();
    }
}