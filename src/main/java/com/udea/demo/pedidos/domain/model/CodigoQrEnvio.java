package com.udea.demo.pedidos.domain.model;

import com.udea.demo.pedidos.domain.exception.CodigoQrInvalidoException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** Contenido del QR de la etiqueta de envío: {@code numeroTracking|checksum}. */
public record CodigoQrEnvio(String numeroTracking, String checksum) {
    private static final String SEPARADOR = "|";

    public static CodigoQrEnvio para(String numeroTracking) {
        return new CodigoQrEnvio(numeroTracking, checksumDe(numeroTracking));
    }

    /** Interpreta un QR escaneado; rechaza formatos desconocidos o checksums alterados. */
    public static CodigoQrEnvio parsear(String contenido) {
        if (contenido == null) throw new CodigoQrInvalidoException();
        String[] partes = contenido.trim().split(java.util.regex.Pattern.quote(SEPARADOR), -1);
        if (partes.length != 2 || partes[0].isBlank() || partes[1].isBlank()) throw new CodigoQrInvalidoException();
        String tracking = partes[0].trim();
        String checksum = partes[1].trim().toUpperCase(Locale.ROOT);
        if (!MessageDigest.isEqual(checksumDe(tracking).getBytes(StandardCharsets.UTF_8),
                checksum.getBytes(StandardCharsets.UTF_8))) {
            throw new CodigoQrInvalidoException();
        }
        return new CodigoQrEnvio(tracking, checksum);
    }

    public String contenido() {
        return numeroTracking + SEPARADOR + checksum;
    }

    private static String checksumDe(String tracking) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(tracking.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 8).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
