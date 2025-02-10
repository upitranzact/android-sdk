package com.upitranzact.sdk.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    @SerializedName("status")
    private boolean status;

    @SerializedName("msg")
    private String msg;

    @SerializedName("txnStatus")
    private String txnStatus;

    @SerializedName("data")
    private Data data;

    public boolean getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    public String getTxnStatus() {
        return txnStatus;
    }

    public Data getData() {
        return data;
    }

    public class Data {

        @SerializedName("intent")
        private String intent;

        @SerializedName("phonepe")
        private String phonepe;

        @SerializedName("paytm")
        private String paytm;

        @SerializedName("cred")
        private String cred;

        @SerializedName("gpay")
        private String gpay;
        @SerializedName("dynamicQR")
        private String dynamicQR;

        @SerializedName("merchantName")
        private String merchantName;

        @SerializedName("amount")
        private String amount;


        public String getIntent() {
            return intent;
        }

        public String getPhonepe() {
            return phonepe;
        }

        public String getPaytm() {
            return paytm;
        }

        public String getCred() {
            return cred;
        }

        public String getGpay() {
            return gpay;
        }

        public String getDynamicQR() {
            return dynamicQR;
        }

        public String getMerchantName() {
            return merchantName;
        }

        public String getAmount() {
            return amount;
        }
    }
}


