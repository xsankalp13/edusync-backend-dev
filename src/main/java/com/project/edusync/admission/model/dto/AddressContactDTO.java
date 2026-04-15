package com.project.edusync.admission.model.dto;
import lombok.Data;
@Data
public class AddressContactDTO {
    private String residentialAddress;
    private String city;
    private String state;
    private String pinCode;
    private String permanentAddress;
    private String primaryMobile;
    private String alternateMobile;
    private String emailId;
}
