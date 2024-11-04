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
@CrossOrigin(
        origins = {"http://localhost:5173, https://polyfile.manojshivagange.tech, http://polyfile.manojshivagange.tech"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowedHeaders = "*", // Allow all headers
        exposedHeaders = "*", // Expose all headers
        allowCredentials = "true" // Set to true if you want to allow credentials
)
@RequestMapping("/")
public class PDFController {

    @GetMapping("/ping")
    public String ping() {
        return "Pong";
    }

    @Autowired
    private PDFService pdfService;

    @Value("${pdf.storage.path}")
    String path;

    // Endpoint to merge multiple PDFs using metadata keys from Redis
    @PostMapping(value = "/merge", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> mergePDFsFromVolume(@RequestBody Map<String, Object> requestBody) {
        try {
            // Get the Redis metadata key from the PDFService

            if (!requestBody.containsKey("etags")) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags"));
            }
            if (!(requestBody.get("etags") instanceof ArrayList)) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags what we recieved was "+ requestBody.get("etags").getClass().getName()));
            }
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

            if (!requestBody.containsKey("etags")) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags"));
            }
            if (!(requestBody.get("etags") instanceof ArrayList)) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags what we recieved was "+ requestBody.get("etags").getClass().getName()));
            }

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
            if (!requestBody.containsKey("etags")) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags"));
            }
            if (!(requestBody.get("etags") instanceof ArrayList)) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags what we recieved was "+ requestBody.get("etags").getClass().getName()));
            }
            //String[] eTags = (String[]) requestBody.get("etags");
            ArrayList<String> eTags = (ArrayList<String>) requestBody.get("etags");
            Double compressionQuality = (Double) requestBody.get("compressionQuality");
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
    @PostMapping(value = "/convert-images-to-pdf", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> convertImagesToPDF(@RequestBody Map<String, Object> requestBody) {
        try {
            if (!requestBody.containsKey("etags")) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags"));
            }
            if (!(requestBody.get("etags") instanceof ArrayList)) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags what we recieved was "+ requestBody.get("etags").getClass().getName()));
            }
                ArrayList<String> eTags = (ArrayList<String>) requestBody.get("etags");



            // Get the metadata key for the generated PDF
            List<String> pdfMetadataKey = pdfService.convertImagesToPDF(eTags);

            // Return the metadata key for the PDF in the HTTP response
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(pdfMetadataKey);

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Error converting images to PDF: " + e.getMessage()));
        }
    }


    // Endpoint to convert PDF to images using metadata key from Redis
    @PostMapping(value = "/convert-pdf-to-images", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> convertPDFToImages(@RequestBody Map<String, Object> requestBody) {
        try {

            if (!requestBody.containsKey("etags")) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags"));
            }
            if (!(requestBody.get("etags") instanceof ArrayList)) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags what we recieved was "+ requestBody.get("etags").getClass().getName()));
            }
            ArrayList<String> eTags = (ArrayList<String>) requestBody.get("etags");
            if (eTags.size() != 1) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide only one eTag"));
            }

            // Get the list of image metadata keys from the PDFService
            List<String> imageMetadataKeys = pdfService.convertPDFToImages(eTags);

            // Return the list of image metadata keys in the HTTP response
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(imageMetadataKeys);

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Error converting PDF to images: " + e.getMessage()));
        }
    }

    

    // Endpoint to add a watermark to a PDF and return the metadata key of the watermarked PDF
    @PostMapping(value = "/add-watermark", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> addWatermarkToPDF(@RequestBody Map<String, Object> requestBody) {
        try {
            if (!requestBody.containsKey("etags")) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags"));
            }
            if (!(requestBody.get("etags") instanceof ArrayList)) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags what we recieved was "+ requestBody.get("etags").getClass().getName()));
            }
            ArrayList<String> eTags = (ArrayList<String>) requestBody.get("etags");
            String watermarkText = (String) requestBody.get("watermarkText");
            Integer opacity = (Integer) requestBody.get("opacity");
            String position = (String) requestBody.get("position");
            float fontSize = 0.0f;
            float angle = 0.0f;


            // Call the service method with additional parameters for opacity and position
            List<String> watermarkedMetadataKey = pdfService.addWatermarkToPDF(eTags, watermarkText, opacity, position, fontSize, angle);

            // Return the metadata key in the HTTP response body
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(watermarkedMetadataKey);

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Error adding watermark to PDF: " + e.getMessage()));
        }
    }
    

    // Endpoint to add a view-only password to a PDF and return the metadata key of the secured PDF
    @PostMapping(value = "/secure", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> securePDF(@RequestBody Map<String, Object> requestBody) {
        try {
            if (!requestBody.containsKey("etags")) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags"));
            }
            if (!(requestBody.get("etags") instanceof ArrayList)) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Please provide a list of eTags what we recieved was "+ requestBody.get("etags").getClass().getName()));
            }
            ArrayList<String> eTags = (ArrayList<String>) requestBody.get("etags");
            String userPassword = (String) requestBody.get("userPassword");

            // Call the service method to secure the PDF with view-only access
            List<String> securedMetadataKey = pdfService.securePDF(eTags, userPassword);

            // Return the metadata key of the secured PDF
            return ResponseEntity.ok()
                    .body(securedMetadataKey);

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Error securing PDF: " + e.getMessage()));
        }
    }
}







