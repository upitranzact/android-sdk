package com.upitranzact.sdk.network;

import com.upitranzact.sdk.model.ApiResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {

    @FormUrlEncoded
    @POST("v1/payments/createPaymentIntent")
    Call<ApiResponse> createOrder(
            @Field("mid") String mid,
            @Field("amount") String amount,
            @Field("order_id") String orderId,
            @Field("redirect_url") String redirectUrl,
            @Field("note") String note,
            @Field("customer_name") String customerName,
            @Field("customer_email") String customerEmail,
            @Field("customer_mobile") String customerMobile
    );

    @FormUrlEncoded
    @POST("v1/payments/checkPaymentStatus")
    Call<ApiResponse> checkPaymentStatus(
            @Field("mid") String mid,
            @Field("order_id") String orderId
    );

    @FormUrlEncoded
    @POST("v1/payments/paymentRequestForOrder")
    Call<ApiResponse> createPaymentRequest(
            @Field("mid") String mid,
            @Field("amount") String amount,
            @Field("order_id") String orderId,
            @Field("vpa") String vpa,
            @Field("note") String note,
            @Field("customer_name") String customerName,
            @Field("customer_email") String customerEmail,
            @Field("customer_mobile") String customerMobile
    );

}


