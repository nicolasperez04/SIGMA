package com.SIGMA.USCO.Users.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleRequest {

    private Long id;
    @NotBlank(message = "El nombre del rol es obligatorio.")
    private String name;
    private Set<Long> permissionIds;

}
