package com.smartlib.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.smartlib.config.AppProperties;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service

public class QrService {
    private final AppProperties props;


    public QrService(AppProperties props) {
        this.props = props;
    }
    public String attendanceUrl(String token) { return props.baseUrl().replaceAll("/+$", "") + "/attendance/scan/" + token; }

    public byte[] png(String text, int size) {
        try {
            BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, Map.of(EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(m, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("QR generation failed", e); }
    }
}
