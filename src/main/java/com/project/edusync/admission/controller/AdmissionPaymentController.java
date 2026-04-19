package com.project.edusync.admission.controller;

import com.project.edusync.admission.model.dto.PaymentVerifyRequest;
import com.project.edusync.finance.dto.payment.InitiatePaymentResponseDTO;
import com.project.edusync.admission.service.AdmissionPaymentService;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.url}/admission/payment")
public class AdmissionPaymentController {

    private final AdmissionPaymentService paymentService;
    private final UserRepository userRepository;

    /**
     * POST /api/v1/admission/payment/create-order/{applicationUuid}
     * Initiate Razorpay order for an approved application.
     */
    @PostMapping("/create-order/{applicationUuid}")
    public ResponseEntity<InitiatePaymentResponseDTO> createOrder(
            @PathVariable UUID applicationUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUser(userDetails);
        InitiatePaymentResponseDTO orderDTO = paymentService.createOrder(applicationUuid, user);
        return ResponseEntity.ok(orderDTO);
    }

    /**
     * POST /api/v1/admission/payment/verify
     * Verify Razorpay payment signature and record payment.
     */
    @PostMapping("/verify")
    public ResponseEntity<Void> verifyPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentVerifyRequest request) {
        
        User user = getUser(userDetails);
        paymentService.verifyPayment(request, user);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/v1/admission/payment/receipt/{applicationUuid}
     * Downloads a PDF receipt for a paid admission application.
     */
    @GetMapping("/receipt/{applicationUuid}")
    public ResponseEntity<byte[]> getAdmissionReceipt(
            @PathVariable UUID applicationUuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        byte[] pdfBytes = paymentService.getAdmissionReceipt(applicationUuid, user);

        String filename = "admission_receipt_" + applicationUuid.toString().substring(0, 8) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }
}
