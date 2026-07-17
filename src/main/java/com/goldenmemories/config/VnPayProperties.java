package com.goldenmemories.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.vnpay")
public class VnPayProperties {

    private String paymentUrl;
    private String tmnCode;
    private String hashSecret;
    private String publicBaseUrl;
    private String locale = "vn";
    private String orderType = "other";
    private String version = "2.1.0";
    private String currency = "VND";
    private int expireMinutes = 15;

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }

    public String getTmnCode() { return tmnCode; }
    public void setTmnCode(String tmnCode) { this.tmnCode = tmnCode; }

    public String getHashSecret() { return hashSecret; }
    public void setHashSecret(String hashSecret) { this.hashSecret = hashSecret; }

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public int getExpireMinutes() { return expireMinutes; }
    public void setExpireMinutes(int expireMinutes) { this.expireMinutes = expireMinutes; }

    public boolean isConfigured() {
        return hasText(paymentUrl) && hasText(tmnCode) && hasText(hashSecret) && hasText(publicBaseUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
