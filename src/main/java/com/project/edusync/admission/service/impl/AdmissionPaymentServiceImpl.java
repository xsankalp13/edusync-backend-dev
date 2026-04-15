package com.project.edusync.admission.service.impl;

import com.project.edusync.finance.dto.payment.InitiatePaymentResponseDTO;
import com.project.edusync.admission.model.dto.PaymentVerifyRequest;
import com.project.edusync.admission.model.entity.AdmissionApplication;
import com.project.edusync.admission.model.entity.AdmissionPayment;
import com.project.edusync.admission.model.enums.AdmissionStatus;
import com.project.edusync.admission.repository.AdmissionApplicationRepository;
import com.project.edusync.admission.repository.AdmissionPaymentRepository;
import com.project.edusync.admission.service.AdmissionPaymentService;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.finance.model.enums.PaymentMethod;
import com.project.edusync.finance.model.enums.PaymentStatus;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.finance.utils.NumberToWordsConverter;
import com.project.edusync.iam.model.entity.User;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionPaymentServiceImpl implements AdmissionPaymentService {

    private final RazorpayClient razorpayClient;
    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionPaymentRepository admissionPaymentRepository;
    private final PdfGenerationService pdfGenerationService;
    private final NumberToWordsConverter numberToWordsConverter;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Override
    @Transactional
    public InitiatePaymentResponseDTO createOrder(UUID applicationUuid, User user) {
        AdmissionApplication app = applicationRepository.findByUuid(applicationUuid)
                .orElseThrow(() -> new ResourceNotFoundException("AdmissionApplication", "uuid", applicationUuid));

        if (app.getStatus() != AdmissionStatus.APPROVED) {
            throw new EdusyncException("Application is not in APPROVED status. Current status: " + app.getStatus(), HttpStatus.BAD_REQUEST);
        }

        if (app.getFormFee() == null || app.getFormFee().compareTo(BigDecimal.ZERO) <= 0) {
            throw new EdusyncException("Admission fee has not been set by administrator.", HttpStatus.BAD_REQUEST);
        }

        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay expect amount in paise (1 INR = 100 paise)
            orderRequest.put("amount", app.getFormFee().multiply(new BigDecimal(100)).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "adm_" + app.getUuid().toString().substring(0, 8));

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            app.setRazorpayOrderId(orderId);
            applicationRepository.save(app);

            return new InitiatePaymentResponseDTO(razorpayKeyId, null, orderId);

        } catch (Exception e) {
            log.error("Failed to create Razorpay order for application: {}", applicationUuid, e);
            throw new EdusyncException("Failed to initiate payment gateway: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void verifyPayment(PaymentVerifyRequest request, User user) {
        AdmissionApplication app = applicationRepository.findByUuid(request.getApplicationUuid())
                .orElseThrow(() -> new ResourceNotFoundException("AdmissionApplication", "uuid", request.getApplicationUuid()));

        // Verification logic
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getOrderId());
            attributes.put("razorpay_payment_id", request.getGatewayTransactionId());
            attributes.put("razorpay_signature", request.getSignature());

            boolean isValid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);

            if (!isValid) {
                throw new EdusyncException("Payment signature verification failed", HttpStatus.BAD_REQUEST);
            }

            // Update application status
            app.setStatus(AdmissionStatus.FEE_PAID);
            app.setFeePaymentId(request.getGatewayTransactionId());
            app.setPaidAt(LocalDateTime.now());
            applicationRepository.save(app);

            // Create AdmissionPayment record
            AdmissionPayment payment = AdmissionPayment.builder()
                    .application(app)
                    .amount(app.getFormFee())
                    .currency("INR")
                    .status(PaymentStatus.SUCCESS)
                    .method(PaymentMethod.ONLINE)
                    .razorpayOrderId(request.getOrderId())
                    .razorpayPaymentId(request.getGatewayTransactionId())
                    .razorpaySignature(request.getSignature())
                    .paidAt(app.getPaidAt())
                    .build();

            admissionPaymentRepository.save(payment);

            log.info("Payment verified successfully for application: {}", request.getApplicationUuid());

        } catch (Exception e) {
            log.error("Payment verification failed for application: {}", request.getApplicationUuid(), e);
            throw new EdusyncException("Payment verification failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getAdmissionReceipt(UUID applicationUuid, User user) {
        log.info("Generating admission receipt for application: {}", applicationUuid);

        AdmissionApplication app = applicationRepository.findByUuid(applicationUuid)
                .orElseThrow(() -> new ResourceNotFoundException("AdmissionApplication", "uuid", applicationUuid));

        // Security Check: Only the applicant user (or admin) should download this.
        if (!app.getUser().getId().equals(user.getId())) {
             throw new EdusyncException("You are not authorized to download this receipt.", HttpStatus.FORBIDDEN);
        }

        if (app.getStatus() != AdmissionStatus.FEE_PAID) {
            throw new EdusyncException("Receipt not available. Current status: " + app.getStatus(), HttpStatus.BAD_REQUEST);
        }

        AdmissionPayment payment = admissionPaymentRepository.findByApplication(app).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("AdmissionPayment", "applicationUuid", applicationUuid));

        Map<String, Object> data = new java.util.HashMap<>();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        data.put("receiptNo", payment.getRazorpayPaymentId());
        data.put("applicationId", app.getUuid().toString().substring(0, 8).toUpperCase());
        data.put("applicantName", app.getStudentBasicDetails() != null ? app.getStudentBasicDetails().getFullName() : "Applicant");
        data.put("parentName", app.getParentGuardianDetails() != null ? app.getParentGuardianDetails().getFatherName() : "-");
        data.put("paymentDate", app.getPaidAt().format(dtf));
        data.put("session", app.getAdmissionDetails() != null ? app.getAdmissionDetails().getAcademicYear() : "-");
        data.put("className", app.getAdmissionDetails() != null ? app.getAdmissionDetails().getClassApplyingFor() : "-");
        data.put("payMode", payment.getMethod().toString());
        data.put("bankName", "-");
        data.put("paymentNumber", payment.getRazorpayPaymentId());
        data.put("note", "Successfully verified electronic payment.");

        // Line Item for Admission Fee
        List<Map<String, Object>> lineItems = new java.util.ArrayList<>();
        Map<String, Object> feeItem = new java.util.HashMap<>();
        feeItem.put("description", "Admission Application Fee");
        feeItem.put("amount", app.getFormFee());
        lineItems.add(feeItem);
        data.put("lineItems", lineItems);

        data.put("totalAmount", app.getFormFee());
        data.put("totalInWords", numberToWordsConverter.convertToWords(app.getFormFee().longValue()));

        return pdfGenerationService.generatePdfFromHtml("admission_receipt", data);
    }
}
