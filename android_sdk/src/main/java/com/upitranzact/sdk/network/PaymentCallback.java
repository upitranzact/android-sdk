package com.upitranzact.sdk.network;
public interface PaymentCallback {
    /**
     * Called when the payment is successful.
     *
     * @param message
     *            Success message
     */
    void onPaymentSuccess(String order_id, String message);

    /**
     * Called when the payment fails.
     *
     * @param message
     *            Failure message
     */
    void onPaymentFailed(String order_id, String message);
}
