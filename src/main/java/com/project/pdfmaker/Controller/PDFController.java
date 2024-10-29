package com.project.pdfmaker.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.pdfmaker.Service.PDFService;

import java.util.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/pdf")
public class PDFController {

    @Autowired
    private PDFService pdfService;

    @Value("${pdf.storage.path}")
    String path;

    // Endpoint to merge multiple PDFs using metadata keys from Redis
    @PostMapping(value = "/merge", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> mergePDFsFromVolume(@RequestBody Map<String, Object> requestBody) {
        try {
            // Get the Redis metadata key from the PDFService
            System.out.println(path);
        System.out.println(requestBody);
        System.out.println(requestBody.get("etaasfsgs"));
        System.out.println(requestBody.get("etags").getClass().getName());
            //String[] eTags = (String[]) requestBody.get("etags");
        ArrayList<String> eTags = (ArrayList<String>) requestBody.get("etags");
            System.out.println(eTags);
        System.out.println(eTags.size());
        System.out.println(eTags.get(0));
            List<String> outputeTag = pdfService.mergePDFsFromVolume(eTags);

            // Return the outputeTag as json array
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(outputeTag);

//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
//                    .body("Merged PDF metadata key: " + metadataKey);

        }
       catch (IOException | InterruptedException e) {
           return ResponseEntity.internalServerError().body(Collections.singletonList("Error merging PDFs: " + e.getMessage()));
      }
    }

    // Endpoint to split a PDF into single pages and return a list of metadata keys
    // as plain text
    @PostMapping(value = "/split", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> splitPDF(@RequestBody Map<String, Object> requestBody) {
        try {

            System.out.println(path);
            System.out.println(requestBody);
            System.out.println(requestBody.get("etaasfsgs"));
            System.out.println(requestBody.get("etags").getClass().getName());
            //String[] eTags = (String[]) requestBody.get("etags");
            ArrayList<String> eTags = (ArrayList<String>) requestBody.get("etags");
            System.out.println(eTags);
            System.out.println(eTags.size());
            System.out.println(eTags.get(0));


            List<String> splitMetadataKeys = pdfService.splitPDF(eTags);


            // Return the metadata keys in the HTTP response body
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(splitMetadataKeys);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Error splitting PDF: " + e.getMessage()));
        }
    }

    // Endpoint to compress a PDF using its metadata key from Redis
    @PostMapping(value = "/compress", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>>  compressPDF(@RequestBody Map<String, Object> requestBody) {
        try {
            //String[] eTags = (String[]) requestBody.get("etags");
            ArrayList<String> eTags = (ArrayList<String>) requestBody.get("etags");
            Float compressionQuality = (Float) requestBody.get("compressionQuality");
            // Get the metadata key of the compressed PDF
            List<String> compressedMetadataKey = pdfService.compressPDF(eTags,compressionQuality);

            // Return the metadata key for the compressed PDF
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(compressedMetadataKey);

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Error compressing PDF: " + e.getMessage()));
        }
    }


   

    // Endpoint to convert images to a PDF using metadata keys from Redis
    @GetMapping("/convert-images-to-pdf")
    public ResponseEntity<String> convertImagesToPDF(@RequestParam("imageMetadataKeys") String[] imageMetadataKeys) {
        try {
            // Get the metadata key for the generated PDF
            String pdfMetadataKey = pdfService.convertImagesToPDF(imageMetadataKeys);

            // Return the metadata key for the PDF in the HTTP response
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body("Generated PDF metadata key: " + pdfMetadataKey);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error converting images to PDF: " + e.getMessage());
        }
    }


    // Endpoint to convert PDF to images using metadata key from Redis
    @GetMapping("/convert-pdf-to-images")
    public ResponseEntity<List<String>> convertPDFToImages(@RequestParam("pdfMetadataKey") String pdfMetadataKey) {
        try {
            // Get the list of image metadata keys from the PDFService
            List<String> imageMetadataKeys = pdfService.convertPDFToImages(pdfMetadataKey);

            // Return the metadata keys in the HTTP response body
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(imageMetadataKeys);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    

    // Endpoint to add a watermark to a PDF and return the metadata key of the watermarked PDF
    @GetMapping("/add-watermark")
    public ResponseEntity<String> addWatermarkToPDF(
            @RequestParam("pdfMetadataKey") String pdfMetadataKey,
            @RequestParam("watermarkText") String watermarkText,
            @RequestParam("opacity") float opacity,
            @RequestParam("position") String position) {
        try {
            // Call the service method with additional parameters for opacity and position
            String watermarkedMetadataKey = pdfService.addWatermarkToPDF(pdfMetadataKey, watermarkText, opacity, position);

            // Return the metadata key in the HTTP response body
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body("Watermarked PDF metadata key: " + watermarkedMetadataKey);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error adding watermark to PDF: " + e.getMessage());
        }
    }
    

    // Endpoint to add a view-only password to a PDF and return the metadata key of the secured PDF
    @GetMapping("/secure")
    public ResponseEntity<String> securePDF(
            @RequestParam("pdfMetadataKey") String pdfMetadataKey,
            @RequestParam("userPassword") String userPassword) {
        try {
            // Call the service method to secure the PDF with view-only access
            String securedMetadataKey = pdfService.securePDF(pdfMetadataKey, userPassword);

            // Return the metadata key of the secured PDF
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body("Secured PDF metadata key: " + securedMetadataKey);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error securing PDF: " + e.getMessage());
        }
    }
}







