package com.project.edusync.teacher.service;

import com.project.edusync.teacher.model.entity.ProxyRequest;
import com.project.edusync.teacher.repository.ProxyRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProxyRequestService {

    private final ProxyRequestRepository proxyRequestRepository;

    public List<ProxyRequest> getProxyRequests(Long teacherId) {
        return proxyRequestRepository.findByRequestedToId(teacherId);
    }

    public ProxyRequest acceptProxyRequest(Long requestId) {
        ProxyRequest proxyRequest = proxyRequestRepository.findById(requestId).orElseThrow();
        proxyRequest.setIsAccepted(true);
        return proxyRequestRepository.save(proxyRequest);
    }
}