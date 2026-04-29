package com.project.edusync.iam.model.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsDto {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String profileUrl;
    private Set<String> roles;
}