package com.SIGMA.USCO.Users.dto.response;

import com.SIGMA.USCO.Users.entity.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String lastname;
    private String email;
    private Status status;
    private Set<String> roles;
    private String faculty;
    private String academicProgram;
    private LocalDateTime createdDate;

}
