package com.udea.demo.pedidos.domain.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.udea.demo.pedidos.domain.model.Pedido;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;

@Component
public class EtiquetaPdfGenerador implements GeneradorEtiqueta {
    @Override
    public String generar(Pedido pedido) {
        try {
            String checksum = checksum(pedido.getNumeroTracking());
            String qrPayload = pedido.getNumeroTracking() + "|" + checksum;
            var hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            var matrix = new QRCodeWriter().encode(qrPayload, BarcodeFormat.QR_CODE, 260, 260, hints);
            ByteArrayOutputStream qrBytes = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", qrBytes);

            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, pdf);
            document.open();
            document.add(new Paragraph("LogisTrack - Etiqueta de envío"));
            document.add(new Paragraph("Pedido: " + pedido.getNumeroPedido()));
            document.add(new Paragraph("Tracking: " + pedido.getNumeroTracking()));
            document.add(new Paragraph("Remitente: " + nullSafe(pedido.getRemitenteNombre())));
            document.add(new Paragraph("Telefono de origen: " + nullSafe(pedido.getRemitenteTelefono())));
            document.add(new Paragraph("Origen: " + pedido.getDireccionOrigen() + ", " + nullSafe(pedido.getCiudadOrigen())));
            document.add(new Paragraph("Destinatario: " + nullSafe(pedido.getDestinatarioNombre())));
            document.add(new Paragraph("Telefono destino: " + nullSafe(pedido.getDestinatarioTelefono())));
            document.add(new Paragraph("Destino: " + pedido.getDireccionDestino() + ", " + nullSafe(pedido.getCiudadDestino())));
            document.add(new Paragraph("Paquete: " + pedido.getDescripcionPaquete()));
            document.add(new Paragraph("Peso: " + pedido.getPesoKg() + " kg"));
            document.add(new Paragraph("Dimensiones: " + pedido.getLargoCm() + " x " + pedido.getAnchoCm() + " x " + pedido.getAltoCm() + " cm"));
            document.add(new Paragraph("Checksum: " + checksum));
            Image qr = Image.getInstance(qrBytes.toByteArray());
            qr.scaleToFit(180, 180);
            document.add(qr);
            document.close();
            return Base64.getEncoder().encodeToString(pdf.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible generar la etiqueta PDF", ex);
        }
    }

    private String nullSafe(String value) { return value == null ? "-" : value; }

    private String checksum(String tracking) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(tracking.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest).substring(0, 8).toUpperCase();
    }
}
