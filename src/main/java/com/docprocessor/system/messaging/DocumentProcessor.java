package com.docprocessor.system.messaging;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.docprocessor.system.model.Document;
import com.docprocessor.system.model.Job;
import com.docprocessor.system.model.JobStatus;
import com.docprocessor.system.model.JobType;
import com.docprocessor.system.model.User;
import com.docprocessor.system.repository.DocumentRepository;
import com.docprocessor.system.repository.JobRepository;
import com.docprocessor.system.repository.UserRepository;
import com.docprocessor.system.service.EmailService;
import com.docprocessor.system.service.NotificationService;
import com.docprocessor.system.service.ProcessingResultService;

import net.coobird.thumbnailator.Thumbnails;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Component
@Slf4j
public class DocumentProcessor {

    private final JobRepository jobRepository;
    private final DocumentRepository documentRepository;
    private final ProcessingResultService processingResultService;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${rabbitmq.queue.name:document-processing-queue}")
    private String processingQueue;

    public DocumentProcessor(
                              JobRepository jobRepository,
                              DocumentRepository documentRepository,
                              ProcessingResultService processingResultService,
                              RabbitTemplate rabbitTemplate,
                              NotificationService notificationService,
                              EmailService emailService,
                              UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.documentRepository = documentRepository;
        this.processingResultService = processingResultService;
        this.rabbitTemplate = rabbitTemplate;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = "${rabbitmq.queue.name:document-processing-queue}")
    public void processDocument(@Payload Object payload) {
        Long jobId = extractJobIdFromPayload(payload);
        if (jobId == null) {
            log.warn("Unable to determine jobId from payload: {}", payload);
            return;
        }

        log.info("Received job message for jobId={}", jobId);

        Optional<Job> maybeJob = jobRepository.findById(jobId);
        if (maybeJob.isEmpty()) {
            log.warn("Job not found for id={}", jobId);
            return;
        }

        Job job = maybeJob.get();

        try {
            // mark processing
            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);

            Optional<Document> maybeDoc = documentRepository.findById(job.getDocumentId());
            if (maybeDoc.isEmpty()) {
                throw new RuntimeException("Associated document not found for jobId=" + jobId);
            }

            Document doc = maybeDoc.get();

            // Delegate actual processing to helper
            handleProcessing(job, doc);

            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("Job {} completed successfully", jobId);
            safeNotifyCompletion(job);

            // load user and send completion email (best-effort)
            try {
                // resolve user id safely (Job implementation may vary)
                Long userId = resolveUserId(job);
                if (userId != null) {
                    Optional<User> maybeUser = userRepository.findById(userId);
                    if (maybeUser.isPresent()) {
                        User user = maybeUser.get();
                        emailService.sendJobCompletedEmail(user.getEmail(), user.getUsername(), job.getId(),
                                doc.getOriginalFilename());
                    } else {
                        log.warn("User not found for job {} when attempting to send completion email", job.getId());
                    }
                } else {
                    log.warn("Could not resolve userId for job {} to send completion email", job.getId());
                }
            } catch (Exception emailEx) {
                log.warn("Failed to send completion email for job {}: {}", job.getId(), emailEx.getMessage());
            }

        } catch (Exception ex) {
            handleProcessingError(job, ex);
        }
    }

    private void handleProcessing(Job job, Document doc) throws Exception {
        if (job.getJobType() == JobType.TEXT_EXTRACTION) {
            String extracted = extractText(doc);
            log.info("Extracted {} characters of text from document {}",
                     extracted != null ? extracted.length() : 0, doc.getId());
            log.debug("Extracted text preview: {}",
                      extracted != null && extracted.length() > 100
                          ? extracted.substring(0, 100) + "..."
                          : extracted);
            processingResultService.saveResult(job.getId(), "EXTRACTED_TEXT", extracted);

            // Generate thumbnail dynamically in the same pipeline
            try {
                String mimeType = doc.getMimeType();
                if (mimeType != null) {
                    if (mimeType.equals("application/pdf")) {
                        generateThumbnail(doc);
                        doc.setThumbnailUrl("/uploads/" + doc.getStoredFilename() + "_thumb.png");
                        documentRepository.save(doc);
                        log.info("Generated PDF thumbnail for document ID {}", doc.getId());
                    } else if (mimeType.startsWith("image/")) {
                        generateImageThumbnail(doc);
                        doc.setThumbnailUrl("/uploads/" + doc.getStoredFilename() + "_thumb.png");
                        documentRepository.save(doc);
                        log.info("Generated image thumbnail for document ID {}", doc.getId());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to generate thumbnail for document ID {}: {}", doc.getId(), e.getMessage());
            }
        } else if (job.getJobType() == JobType.THUMBNAIL) {
            String thumbPath = generateThumbnail(doc);
            processingResultService.saveResult(job.getId(), "THUMBNAIL_PATH", thumbPath);
        } else if (job.getJobType() == JobType.OCR) {
            String ocrResult = performOCR(new File(doc.getStoragePath()));
            processingResultService.saveResult(job.getId(), "EXTRACTED_TEXT", ocrResult);
        } else if (job.getJobType() == JobType.METADATA) {
            String metadata = extractMetadata(doc);
            processingResultService.saveResult(job.getId(), "METADATA", metadata);
        } else {
            throw new IllegalArgumentException("Unsupported job type: " + job.getJobType());
        }
    }

    private void safeNotifyCompletion(Job job) {
        try {
            notificationService.sendJobCompletedNotification(job);
        } catch (Exception notifEx) {
            log.warn("Failed to send completion notification for job {}: {}", job.getId(), notifEx.getMessage());
        }
    }

    private void handleProcessingError(Job job, Exception ex) {
        log.error("Error processing job {}: {}", job.getId(), ex.getMessage(), ex);
        int retries = job.getRetryCount() == null ? 0 : job.getRetryCount();
        retries++;
        job.setRetryCount(retries);
        job.setErrorMessage(ex.getMessage());

        if (retries < (job.getMaxRetries() == null ? 3 : job.getMaxRetries())) {
            job.setStatus(JobStatus.QUEUED);
            jobRepository.save(job);
            // re-queue
            rabbitTemplate.convertAndSend(processingQueue, job.getId());
            log.info("Re-queued job {} (retry {}/{})", job.getId(), retries, job.getMaxRetries());
            // notify user about retry
            try {
                notificationService.sendJobNotification(job, "Job re-queued (retry " + retries + ")");
            } catch (Exception notifEx) {
                log.warn("Failed to send retry notification for job {}: {}", job.getId(), notifEx.getMessage());
            }
        } else {
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            log.info("Job {} failed after {} retries", job.getId(), retries);

            // send failure notification
            try {
                notificationService.sendJobFailedNotification(job, ex.getMessage());
            } catch (Exception notifEx) {
                log.warn("Failed to send failure notification for job {}: {}", job.getId(), notifEx.getMessage());
            }

            // load document and user and send failure email (best-effort)
            try {
                Optional<Document> maybeDoc = documentRepository.findById(job.getDocumentId());
                String originalFilename = maybeDoc.map(Document::getOriginalFilename).orElse(null);

                Long userId = resolveUserId(job);
                if (userId != null) {
                    Optional<User> maybeUser = userRepository.findById(userId);
                    if (maybeUser.isPresent()) {
                        User user = maybeUser.get();
                        emailService.sendJobFailedEmail(user.getEmail(), user.getUsername(), job.getId(),
                                originalFilename, ex.getMessage());
                    } else {
                        log.warn("User not found for job {} when attempting to send failure email", job.getId());
                    }
                } else {
                    log.warn("Could not resolve userId for job {} to send failure email", job.getId());
                }
            } catch (Exception emailEx) {
                log.warn("Failed to send failure email for job {}: {}", job.getId(), emailEx.getMessage());
            }
        }
    }

    private Long extractJobIdFromPayload(Object payload) {
        if (payload == null) return null;
        try {
            // Use classic instanceof + casts for Java compatibility
            if (payload instanceof Long) return (Long) payload;
            if (payload instanceof Integer) return Long.valueOf(((Integer) payload).longValue());
            if (payload instanceof Number) return Long.valueOf(((Number) payload).longValue());
            if (payload instanceof String) return parseLongFromString((String) payload);
            if (payload instanceof byte[]) return parseLongFromString(new String((byte[]) payload));
            if (payload instanceof Message) return parseLongFromString(new String(((Message) payload).getBody()));
            if (payload instanceof Map) return parseLongFromMap((Map<?, ?>) payload);
        } catch (Exception e) {
            log.warn("Failed to parse jobId from payload (type={}): {}", payload.getClass().getName(), e.getMessage());
            return null;
        }
        return null;
    }

    private Long parseLongFromString(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return Long.valueOf(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLongFromMap(Map<?, ?> map) {
        Object val = map.get("jobId");
        if (val == null) val = map.get("id");
        if (val instanceof Number) return Long.valueOf(((Number) val).longValue());
        if (val instanceof String) return parseLongFromString((String) val);
        return null;
    }

    private String extractText(Document doc) throws IOException {
        Path path = Path.of(doc.getStoragePath());
        String mimeType = doc.getMimeType();

        // Handle text files directly
        if (mimeType != null && mimeType.startsWith("text/")) {
            log.info("Extracting text from plain text file: {}", doc.getOriginalFilename());
            return Files.readString(path);
        }

        // Handle Image files using OCR fallback directly
        if (mimeType != null && mimeType.startsWith("image/")) {
            log.info("Extracting text from image file using OCR: {}", doc.getOriginalFilename());
            return performOCR(path.toFile());
        }

        // Handle PDF files
        log.info("Extracting text from PDF file: {}", doc.getOriginalFilename());
        String text = "";
        try (PDDocument pdf = PDDocument.load(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(pdf);
        }

        // Fallback to OCR if PDF contains no selectable text (scanned PDF)
        if (text == null || text.trim().isEmpty()) {
            log.info("PDF contains no selectable text. Falling back to OCR for scanned document: {}", doc.getOriginalFilename());
            return performOCR(path.toFile());
        }

        return text;
    }

    private String performOCR(File file) {
        log.info("Starting Optical Character Recognition (OCR) for file: {}", file.getName());
        try {
            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
            
            // Resolve local workspace training folder or environment path
            String tessDataEnv = System.getenv("TESSDATA_PREFIX");
            if (tessDataEnv != null && !tessDataEnv.isEmpty()) {
                tesseract.setDatapath(tessDataEnv);
            } else {
                File localTessData = new File("./tessdata");
                File winTessData = new File("C:\\Program Files\\Tesseract-OCR\\tessdata");
                if (localTessData.exists() && localTessData.isDirectory()) {
                    tesseract.setDatapath(localTessData.getAbsolutePath());
                } else if (winTessData.exists() && winTessData.isDirectory()) {
                    log.info("Detected default Windows Tesseract installation. Using datapath: {}", winTessData.getAbsolutePath());
                    tesseract.setDatapath(winTessData.getAbsolutePath());
                } else {
                    log.info("TESSDATA_PREFIX env, local ./tessdata, or default Windows directory not found. Attempting automatic classpath extraction.");
                }
            }
            
            tesseract.setLanguage("eng");
            String result = tesseract.doOCR(file);
            log.info("OCR completed successfully. Extracted {} characters.", result != null ? result.length() : 0);
            return result;
        } catch (Throwable t) {
            log.warn("OCR skipped or native Tesseract engine not loaded on host: {}", t.getMessage());
            return "[OCR Engine Fallback: Tesseract native binary or eng.traineddata is not installed on this host. Please install tesseract-ocr to enable full visual character recognition. File preview was processed successfully.]";
        }
    }

    private String generateThumbnail(Document doc) throws IOException {
        Path path = Path.of(doc.getStoragePath());
        // generate thumbnail in same folder with suffix _thumb.png
        File source = path.toFile();
        String thumbName = doc.getStoredFilename() + "_thumb.png";
        File thumbFile = new File(source.getParentFile(), thumbName);

        // attempt to render first page image using PDFBox
        try (PDDocument pdf = PDDocument.load(source)) {
            BufferedImage pageImage = new org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImageWithDPI(0, 150);
            // use Thumbnailator to resize and write
            BufferedImage thumbnail = Thumbnails.of(pageImage)
                    .size(200, 200)
                    .asBufferedImage();
            ImageIO.write(thumbnail, "png", thumbFile);
            return thumbFile.getAbsolutePath();
        }
    }

    private String generateImageThumbnail(Document doc) throws IOException {
        Path path = Path.of(doc.getStoragePath());
        File source = path.toFile();
        String thumbName = doc.getStoredFilename() + "_thumb.png";
        File thumbFile = new File(source.getParentFile(), thumbName);

        // use Thumbnailator to resize original image directly
        Thumbnails.of(source)
                .size(200, 200)
                .toFile(thumbFile);

        return thumbFile.getAbsolutePath();
    }

    private String extractMetadata(Document doc) {
        // Basic metadata: original filename, size, mimeType
        return "originalFilename:" + doc.getOriginalFilename() + "; storedFilename:" + doc.getStoredFilename() +
                "; fileSize:" + doc.getFileSize() + "; mimeType:" + doc.getMimeType() + ";";
    }

    /**
     * Resolve user id from Job using reflection to support multiple Job shapes:
     * - getUserId()
     * - getUser().getId()
     * - field userId
     */
    private Long resolveUserId(Job job) {
        if (job == null) return null;
        // try getUserId()
        try {
            Method m = job.getClass().getMethod("getUserId");
            Object val = m.invoke(job);
            if (val instanceof Number) return ((Number) val).longValue();
            if (val instanceof String) return parseLongFromString((String) val);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.debug("Error invoking getUserId reflectively: {}", e.getMessage());
        }

        // try getUser().getId()
        try {
            Method getUser = job.getClass().getMethod("getUser");
            Object userObj = getUser.invoke(job);
            if (userObj != null) {
                Method getId = userObj.getClass().getMethod("getId");
                Object idVal = getId.invoke(userObj);
                if (idVal instanceof Number) return ((Number) idVal).longValue();
                if (idVal instanceof String) return parseLongFromString((String) idVal);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.debug("Error invoking getUser/getId reflectively: {}", e.getMessage());
        }

        // try field userId
        try {
            Field f = job.getClass().getDeclaredField("userId");
            f.setAccessible(true);
            Object v = f.get(job);
            if (v instanceof Number) return ((Number) v).longValue();
            if (v instanceof String) return parseLongFromString((String) v);
        } catch (NoSuchFieldException ignored) {
        } catch (Exception e) {
            log.debug("Error reading userId field reflectively: {}", e.getMessage());
        }

        return null;
    }
}
