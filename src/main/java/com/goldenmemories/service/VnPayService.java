package com.goldenmemories.service;

import com.goldenmemories.config.VnPayProperties;
import com.goldenmemories.model.Project;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VnPayService {

    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VnPayProperties properties;

    public VnPayService(VnPayProperties properties) {
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public String buildPaymentUrl(Project project,
                                  long amountVnd,
                                  String bankCode,
                                  String orderInfo,
                                  String returnUrl,
                                  String ipnUrl,
                                  String clientIp) {
        ensureConfigured();

        Map<String, String> params = new TreeMap<>(Comparator.naturalOrder());
        params.put("vnp_Amount", String.valueOf(amountVnd * 100));
        params.put("vnp_Command", "pay");
        params.put("vnp_CreateDate", currentVnTime());
        params.put("vnp_CurrCode", properties.getCurrency());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_Locale", properties.getLocale());
        params.put("vnp_OrderInfo", normalizeOrderInfo(orderInfo));
        params.put("vnp_OrderType", properties.getOrderType());
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_TxnRef", buildTxnRef(project));
        params.put("vnp_Version", properties.getVersion());
        params.put("vnp_ExpireDate", VNPAY_DATE.format(ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
            .plusMinutes(properties.getExpireMinutes())));

        if (hasText(bankCode)) {
            params.put("vnp_BankCode", bankCode.trim());
        }

        if (hasText(ipnUrl)) {
            params.put("vnp_IpnUrl", ipnUrl);
        }

        String query = buildQueryString(params);
        String secureHash = hmacSha512(properties.getHashSecret(), query);
        return properties.getPaymentUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public Map<String, String> extractParameters(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0 && values[0] != null) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    public boolean verifySignature(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (!hasText(secureHash)) {
            return false;
        }

        Map<String, String> signingParams = new TreeMap<>(Comparator.naturalOrder());
        params.forEach((key, value) -> {
            if (!"vnp_SecureHash".equalsIgnoreCase(key) && !"vnp_SecureHashType".equalsIgnoreCase(key) && hasText(value)) {
                signingParams.put(key, value);
            }
        });

        String query = buildQueryString(signingParams);
        String signed = hmacSha512(properties.getHashSecret(), query);
        return MessageDigest.isEqual(signed.getBytes(StandardCharsets.UTF_8), secureHash.getBytes(StandardCharsets.UTF_8));
    }

    public String buildPublicUrl(String path) {
        String base = properties.getPublicBaseUrl();
        return UriComponentsBuilder.fromHttpUrl(base)
            .path(path.startsWith("/") ? path : "/" + path)
            .build()
            .toUriString();
    }

    public String buildReturnUrl(Long projectId) {
        return buildPublicUrl("/project/" + projectId + "/publish/payment/return");
    }

    public String buildIpnUrl(Long projectId) {
        return buildPublicUrl("/project/" + projectId + "/publish/payment/ipn");
    }

    public String buildTxnRef(Project project) {
        return project.getId() + "-" + System.currentTimeMillis() + "-" + randomSuffix();
    }

    public String normalizeOrderInfo(String orderInfo) {
        String normalized = Normalizer.normalize(orderInfo, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replace('đ', 'd').replace('Đ', 'D');
        normalized = normalized.replaceAll("[^A-Za-z0-9 _\\-:.]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private String currentVnTime() {
        return VNPAY_DATE.format(ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!hasText(entry.getValue())) {
                continue;
            }
            if (!first) {
                query.append('&');
            }
            first = false;
            query.append(urlEncode(entry.getKey()));
            query.append('=');
            query.append(urlEncode(entry.getValue()));
        }
        return query.toString();
    }

    private String hmacSha512(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(keySpec);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign VNPay payload", ex);
        }
    }

    private String randomSuffix() {
        return Integer.toHexString(RANDOM.nextInt(0xFFFFF));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("VNPay is not configured");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
