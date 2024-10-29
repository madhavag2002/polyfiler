package com.project.pdfmaker.Service;

import com.project.pdfmaker.Service.FileMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.utils.PdfMerger;
import com.project.pdfmaker.config.HttpClientConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;

import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PDFService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${pdf.storage.path}")
    String path;

    private ObjectMapper objectMapper = new ObjectMapper();

    // Method to merge multiple PDFs and save the merged PDF to the volume
    public List<String> mergePDFsFromVolume(ArrayList<String> etags ) throws IOException, InterruptedException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Initialize the merged PDF document
        PdfWriter pdfWriter = new PdfWriter(outputStream);
        PdfDocument mergedPdfDoc = new PdfDocument(pdfWriter);
        PdfMerger pdfMerger = new PdfMerger(mergedPdfDoc);

        String first_etag_name = null;
        String first_etag_owner = null;

        // Loop through metadata keys, fetch file paths from Redis, and merge the PDFs
        for (String etag : etags) {
            // Fetch file path from Redis using the metadata key
            String metadata_string = redisTemplate.opsForValue().get(etag);
            System.out.println(" metadata_string: " + metadata_string);

            FileMetadata fileMetaData = objectMapper.readValue(metadata_string, FileMetadata.class);
            System.out.println(" fileMetaData: " + fileMetaData);
            System.out.println(" fileMetaData.getPath(): " + fileMetaData.getPath());
            System.out.println(" fileMetaData.getName(): " + fileMetaData.getName());
            String filePath=path+fileMetaData.getPath();
            if (!filePath.contains(".pdf")) {
                throw new IOException("File metadata not found for key: " );
            }
            if (first_etag_name == null) {
                first_etag_name = fileMetaData.getName();
            }
            if (first_etag_owner == null) {
                first_etag_owner = fileMetaData.getOwner();
            }

            // Access the file from the volume using the file path
            File pdfFile = new File(filePath);
            if (!pdfFile.exists()) {
                throw new IOException("File not found on the volume: " + filePath);
            }

            // Open the PDF document from the file
            try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new FileInputStream(pdfFile)))) {
                // Merge the current PDF into the merged document
                pdfMerger.merge(pdfDoc, 1, pdfDoc.getNumberOfPages());
            }
        }

        assert first_etag_name != null;
        String first_etag_name_without_extension = first_etag_name.substring(0, first_etag_name.lastIndexOf('.'));

        mergedPdfDoc.close();

       String newUuid = UUID.randomUUID().toString();

        // Generate a unique file name for the merged PDF
        String mergedFileName = newUuid+ ".pdf";
        String filePath = path + "/" + mergedFileName;

        // Save the merged PDF to the specified path
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(outputStream.toByteArray());
        }

        UploadFileInternalRequest uploadFileInternalRequest = new UploadFileInternalRequest();
        uploadFileInternalRequest.setPath(filePath);
        uploadFileInternalRequest.setName(first_etag_name_without_extension + "_merged.pdf");
        uploadFileInternalRequest.setOwner(first_etag_owner);
        uploadFileInternalRequest.setUuid(newUuid);
        uploadFileInternalRequest.setHash("hash");
        uploadFileInternalRequest.setSize(outputStream.size());

        String uploadFileInternalRequestJson = objectMapper.writeValueAsString(uploadFileInternalRequest);
        System.out.println("uploadFileInternalRequestJson: " + uploadFileInternalRequestJson);
        //Send POST request to the internal API to upload the merged PDF
        //_, err = http.Post(os.Getenv("FILE_MICROSERVICE")+"/upload/internal", "application/json", bytes.NewReader(payloadBytes))


        String url = System.getenv("FILE_MICROSERVICE") + "/upload/internal";
        HttpClient httpclient = HttpClientConfig.getClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(uploadFileInternalRequestJson))
                .build();
        HttpResponse<String> response = httpclient.send(request, HttpResponse.BodyHandlers.ofString());


        System.out.println("Response code: " + response.statusCode());
        System.out.println("Response body: " + response.body());

        ArrayList<String> mergedMetadataKeys = new ArrayList<>();
        mergedMetadataKeys.add(newUuid);
        return mergedMetadataKeys;





        // Store the merged PDF file path in Redis (e.g., key: "merged_file:123", value:
//        // file path)
//        String metadataKey = "merged_file:" + System.currentTimeMillis();
//        redisTemplate.opsForValue().set(metadataKey, filePath);
    }

    // method to split pdfs
    public List<String> splitPDF(ArrayList<String> etags) throws IOException {

        if (etags.size()!=1) {
            throw new IOException("Only one PDF can be split at a time");
        }
        String metadata_string = redisTemplate.opsForValue().get(etags.get(0));
        System.out.println(" metadata_string: " + metadata_string);

        FileMetadata fileMetaData = objectMapper.readValue(metadata_string, FileMetadata.class);
        System.out.println(" fileMetaData: " + fileMetaData);
        System.out.println(" fileMetaData.getPath(): " + fileMetaData.getPath());
        System.out.println(" fileMetaData.getName(): " + fileMetaData.getName());
        String filePath=path+fileMetaData.getPath();
        if (!filePath.contains(".pdf")) {
            throw new IOException("File metadata not found for key, or file was not a PDF" );
        }
        // Retrieve the file path from Redis using the metadata key
        File pdfFile = new File(filePath);
        if (!pdfFile.exists()) {
            throw new IOException("File not found on the volume: " + filePath);
        }

        // Load the PDF document
        List<String> splitMetadataKeys = new ArrayList<>();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new FileInputStream(pdfFile)))) {
            int totalPages = pdfDoc.getNumberOfPages();

            // Define the upload path for saving split PDFs
//            Path uploadPath = Paths.get("/data/pdf-uploads/");
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }

            // Split each page into its own PDF
            for (int i = 1; i <= totalPages; i++) {
                String newUuid = UUID.randomUUID().toString();

                // Generate a unique file name for the merged PDF
                String mergedFileName = newUuid+ ".pdf";
                String splitFilePath = path + "/" + mergedFileName;

                try (PdfWriter writer = new PdfWriter(splitFilePath)) {
                    PdfDocument singlePagePdf = new PdfDocument(writer);
                    pdfDoc.copyPagesTo(i, i, singlePagePdf);
                    singlePagePdf.close();
                }

                UploadFileInternalRequest uploadFileInternalRequest = new UploadFileInternalRequest();
                uploadFileInternalRequest.setPath(splitFilePath);
                uploadFileInternalRequest.setName(fileMetaData.getName().substring(0, fileMetaData.getName().lastIndexOf('.')) + "_page_" + i + ".pdf");
                uploadFileInternalRequest.setOwner(fileMetaData.getOwner());
                uploadFileInternalRequest.setUuid(newUuid);
                uploadFileInternalRequest.setHash("hash");
                uploadFileInternalRequest.setSize(1234);//any non-zero value should work as download handler doesnt actually check the size

                String uploadFileInternalRequestJson = objectMapper.writeValueAsString(uploadFileInternalRequest);
                System.out.println("uploadFileInternalRequestJson: " + uploadFileInternalRequestJson);
                //Send POST request to the internal API to upload the merged PDF

                String url = System.getenv("FILE_MICROSERVICE") + "/upload/internal";
                HttpClient httpclient = HttpClientConfig.getClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(uploadFileInternalRequestJson))
                        .build();
                HttpResponse<String> response = httpclient.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Response code: " + response.statusCode());
                System.out.println("Response body: " + response.body());
                if (response.statusCode() != 200) {
                    throw new IOException("Error uploading split PDF to file microservice");
                }
                splitMetadataKeys.add(newUuid);


            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return splitMetadataKeys; // Return list of metadata keys for split PDFs
    }

    // Method to compress a PDF and save the compressed PDF to the volume
    public List<String> compressPDF(List<String> eTags , float compressionQuality) throws IOException, InterruptedException {
        // Retrieve file path from Redis using metadata key
        //String originalFilePath = redisTemplate.opsForValue().get(metadataKey);
        List<String> outputeTag = new ArrayList<>();
        for (String eTag:eTags){
            String metadata_string = redisTemplate.opsForValue().get(eTag);
            FileMetadata fileMetaData = objectMapper.readValue(metadata_string, FileMetadata.class);
            String filePath=path+fileMetaData.getPath();

            if (!filePath.contains(".pdf")) {
                throw new IOException("File metadata not found for key: " );
            }

            // Access the file from the volume using the file path
            File originalFile = new File(filePath);
            if (!originalFile.exists()) {
                throw new IOException("File not found on the volume: " + filePath);
            }

            // Define the path to save the compressed PDF
            String newUuid = UUID.randomUUID().toString();
            String compressedFileName = newUuid + ".pdf";
            String compressedFilePath = path + "/" + compressedFileName;

            try (PdfDocument originalPdf = new PdfDocument(new PdfReader(filePath));
                 PdfWriter writer = new PdfWriter(compressedFilePath, new WriterProperties().setCompressionLevel((int)(compressionQuality * 9)));
                 PdfDocument compressedPdf = new PdfDocument(writer)) {

                // Copy pages from the original PDF to the compressed PDF
                originalPdf.copyPagesTo(1, originalPdf.getNumberOfPages(), compressedPdf);
            }

            UploadFileInternalRequest uploadFileInternalRequest = new UploadFileInternalRequest();
            uploadFileInternalRequest.setPath(newUuid+ ".pdf");
            uploadFileInternalRequest.setName(fileMetaData.getName().substring(0, fileMetaData.getName().lastIndexOf('.')) + "_compressed.pdf");
            uploadFileInternalRequest.setOwner(fileMetaData.getOwner());
            uploadFileInternalRequest.setUuid(newUuid);
            uploadFileInternalRequest.setHash("hash");
            uploadFileInternalRequest.setSize(1234);//any non-zero value should work as download handler doesnt actually check the size

            String uploadFileInternalRequestJson = objectMapper.writeValueAsString(uploadFileInternalRequest);
            System.out.println("uploadFileInternalRequestJson: " + uploadFileInternalRequestJson);
            //Send POST request to the internal API to upload the merged PDF

            String url = System.getenv("FILE_MICROSERVICE") + "/upload/internal";

            HttpClient httpclient = HttpClientConfig.getClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(uploadFileInternalRequestJson))
                    .build();
            HttpResponse<String> response = httpclient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
            if (response.statusCode() != 200) {
                throw new IOException("Error uploading compressed PDF to file microservice");
            }
            outputeTag.add(newUuid);

        }


        return outputeTag;
    }





    
        // Method to convert images to PDF and save the PDF to the volume
        public String convertImagesToPDF(String[] imageMetadataKeys) throws IOException {
            // Define the upload path for saving the generated PDF
            Path uploadPath = Paths.get("/data/pdf-uploads/");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
    
            // Generate a unique name for the output PDF file
            String pdfFileName = "images_" + System.currentTimeMillis() + ".pdf";
            String pdfFilePath = uploadPath.toString() + "/" + pdfFileName;
    
            // Initialize the PDF document
            PdfWriter pdfWriter = new PdfWriter(pdfFilePath);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            try (Document document = new Document(pdfDoc)) {
                // Loop through metadata keys, fetch image file paths from Redis, and add images to the PDF
                for (String metadataKey : imageMetadataKeys) {
                    String imagePath = redisTemplate.opsForValue().get(metadataKey);
   
                    if (imagePath == null || imagePath.isEmpty()) {
                        throw new IOException("Image metadata not found for key: " + metadataKey);
                    }
   
                    File imageFile = new File(imagePath);
                    if (!imageFile.exists()) {
                        throw new IOException("Image file not found on the volume: " + imagePath);
                    }
   
                    // Add image to PDF
                    Image img = new Image(ImageDataFactory.create(imagePath));
                    document.add(img);
                }
   
                document.close();
            }
    
            // Store the generated PDF file path in Redis (e.g., key: "pdf_from_images:timestamp")
            String pdfMetadataKey = "pdf_from_images:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(pdfMetadataKey, pdfFilePath);
    
            return pdfMetadataKey;
        }

            // Method to convert PDF pages to images and save the images to the volume
            public List<String> convertPDFToImages(String pdfMetadataKey) throws IOException {
                // Fetch file path from Redis using the metadata key
                String pdfFilePath = redisTemplate.opsForValue().get(pdfMetadataKey);
        
                if (pdfFilePath == null || pdfFilePath.isEmpty()) {
                    throw new IOException("File metadata not found for key: " + pdfMetadataKey);
                }
        
                File pdfFile = new File(pdfFilePath);
                if (!pdfFile.exists()) {
                    throw new IOException("PDF file not found on the volume: " + pdfFilePath);
                }
        
                // Load PDF document
                PDDocument pdfDocument = PDDocument.load(pdfFile);
                PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);
        
                // Define the path for saving images
                Path uploadPath = Paths.get("/data/image-uploads/");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
        
                // List to store metadata keys for the generated images
                List<String> imageMetadataKeys = new ArrayList<>();
        
                // Loop through each page in the PDF and convert to image
                for (int page = 0; page < pdfDocument.getNumberOfPages(); page++) {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
        
                    // Generate a unique name for each image
                    String imageFileName = "page_" + (page + 1) + "_" + System.currentTimeMillis() + ".png";
                    String imageFilePath = uploadPath.toString() + "/" + imageFileName;
        
                    // Save image to the specified path
                    ImageIO.write(image, "PNG", new File(imageFilePath));
        
                    // Store image file path in Redis
                    String imageMetadataKey = "image_from_pdf:" + System.currentTimeMillis() + "_" + page;
                    redisTemplate.opsForValue().set(imageMetadataKey, imageFilePath);
                    imageMetadataKeys.add(imageMetadataKey);
                }
        
                pdfDocument.close();
                return imageMetadataKeys;
            }

           






    // Method to add a watermark to a PDF with customizable opacity and position, saving the edited PDF to the volume
    public String addWatermarkToPDF(String pdfMetadataKey, String watermarkText, float opacity, String position) throws IOException {
        // Retrieve the file path of the original PDF from Redis using the metadata key
        String originalPdfPath = redisTemplate.opsForValue().get(pdfMetadataKey);

        if (originalPdfPath == null || originalPdfPath.isEmpty()) {
            throw new IOException("File metadata not found for key: " + pdfMetadataKey);
        }

        File originalPdfFile = new File(originalPdfPath);
        if (!originalPdfFile.exists()) {
            throw new IOException("File not found on the volume: " + originalPdfPath);
        }

        // Define the path for saving the watermarked PDF
        Path uploadPath = Paths.get("/data/pdf-uploads/");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate a unique name for the watermarked PDF
        String watermarkedFileName = "watermarked_" + System.currentTimeMillis() + ".pdf";
        String watermarkedFilePath = uploadPath.toString() + "/" + watermarkedFileName;

        // Open the original PDF document
        try (PdfDocument originalPdf = new PdfDocument(new PdfReader(originalPdfFile));
             PdfDocument watermarkedPdf = new PdfDocument(new PdfWriter(watermarkedFilePath))) {

            try (Document document = new Document(watermarkedPdf)) {
                // Loop through each page in the PDF
                for (int i = 1; i <= originalPdf.getNumberOfPages(); i++) {
                    PdfPage page = originalPdf.getPage(i).copyTo(watermarkedPdf);
                    watermarkedPdf.addPage(page);

                    // Create the watermark paragraph with given opacity
                    Paragraph watermark = new Paragraph(watermarkText)
                            .setFontSize(60)
                            .setOpacity(opacity) // Set the custom opacity
                            .setRotationAngle(Math.toRadians(45)) // Rotate if desired
                            .setTextAlignment(TextAlignment.CENTER);

                    // Position the watermark based on the specified location
                    PageSize pageSize = (PageSize) page.getPageSize();
                    float x = pageSize.getWidth() / 2;
                    float y = pageSize.getHeight() / 2;

                    switch (position.toLowerCase()) {
                        case "top-left":
                            x = pageSize.getWidth() * 0.25f;
                            y = pageSize.getHeight() * 0.75f;
                            break;
                        case "top-right":
                            x = pageSize.getWidth() * 0.75f;
                            y = pageSize.getHeight() * 0.75f;
                            break;
                        case "bottom-left":
                            x = pageSize.getWidth() * 0.25f;
                            y = pageSize.getHeight() * 0.25f;
                            break;
                        case "bottom-right":
                            x = pageSize.getWidth() * 0.75f;
                            y = pageSize.getHeight() * 0.25f;
                            break;
                        case "center":
                        default:
                            // Center position already set by default
                            break;
                    }

                    // Show the watermark text aligned on the page at specified coordinates
                    document.showTextAligned(watermark, x, y, i, TextAlignment.CENTER, VerticalAlignment.MIDDLE, 0);
                }
            }
        }

        // Store the path of the watermarked PDF in Redis
        String watermarkedMetadataKey = "watermarked_file:" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(watermarkedMetadataKey, watermarkedFilePath);

        return watermarkedMetadataKey;
    }





 

    // Method to add a view-only password to a PDF and save it to the volume
    public String securePDF(String pdfMetadataKey, String userPassword) throws IOException {
        // Retrieve the file path of the original PDF from Redis using the metadata key
        String originalPdfPath = redisTemplate.opsForValue().get(pdfMetadataKey);

        if (originalPdfPath == null || originalPdfPath.isEmpty()) {
            throw new IOException("File metadata not found for key: " + pdfMetadataKey);
        }

        File originalPdfFile = new File(originalPdfPath);
        if (!originalPdfFile.exists()) {
            throw new IOException("File not found on the volume: " + originalPdfPath);
        }

        // Define the upload path for saving the secured PDF
        Path uploadPath = Paths.get("/data/pdf-uploads/");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate a unique file name for the secured PDF
        String securedFileName = "secured_" + System.currentTimeMillis() + ".pdf";
        String securedFilePath = uploadPath.toString() + "/" + securedFileName;

        // Open the original PDF document, apply view-only encryption, and save it as a secured PDF
        try (PdfReader reader = new PdfReader(originalPdfFile);
             PdfWriter writer = new PdfWriter(securedFilePath,
                    new WriterProperties()
                        .setStandardEncryption(
                            userPassword.getBytes(),
                            null, // No owner password, only user password for view access
                            EncryptionConstants.ALLOW_PRINTING, // Allow only viewing without additional permissions
                            EncryptionConstants.ENCRYPTION_AES_256))) {
            PdfDocument pdfDoc = new PdfDocument(reader, writer);
            pdfDoc.close();
        }

        // Store the secured PDF's path in Redis (e.g., key: "secured_file:123", value: file path)
        String securedMetadataKey = "secured_file:" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(securedMetadataKey, securedFilePath);

        return securedMetadataKey;
    }
}








//type UploadFileInternalRequest struct {
//    Size  int64  `json:"size"`
//    Name  string `json:"name"`
//    HASH  string `json:"hash"`
//    Owner string `json:"owner"`
//    UUID  string `json:"uuid"`
//    Path  string `json:"path"`
//}

@Getter
@Setter
class UploadFileInternalRequest {
    private long size;
    private String name;
    private String hash;
    private String owner;
    private String uuid;
    private String path;

}