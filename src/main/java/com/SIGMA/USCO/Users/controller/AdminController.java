package com.SIGMA.USCO.Users.controller;

import com.SIGMA.USCO.Modalities.Entity.enums.ModalityStatus;
import com.SIGMA.USCO.Modalities.dto.ModalityDTO;
import com.SIGMA.USCO.Users.Entity.ProgramAuthority;
import com.SIGMA.USCO.Users.dto.request.AssignExaminerMultipleProgramsRequest;
import com.SIGMA.USCO.Users.dto.request.assignAuthorityProgram;
import com.SIGMA.USCO.Users.dto.request.PermissionDTO;
import com.SIGMA.USCO.Users.dto.request.RegisterUserByAdminRequest;
import com.SIGMA.USCO.Users.dto.request.RoleRequest;
import com.SIGMA.USCO.Users.dto.request.UpdateUserRequest;
import com.SIGMA.USCO.Users.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/createRole")
    @PreAuthorize("hasAuthority('PERM_CREATE_ROLE')")
    public ResponseEntity<?> createRole(@RequestBody RoleRequest request) {
        return adminService.createRole(request);
    }

    @PutMapping("/updateRole/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ROLE')")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        return adminService.updateRole(id, request);
    }

    @PostMapping("/assignRole")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_ROLE')")
    public ResponseEntity<?> assignRoleToUser(@RequestBody UpdateUserRequest request) {
        return adminService.assignRoleToUser(request);
    }

    @PostMapping("/changeUserStatus")
    @PreAuthorize("hasAuthority('PERM_ACTIVATE_OR_DEACTIVATE_USER')")
    public ResponseEntity<?> changeUserStatus(@RequestBody UpdateUserRequest request){
        return adminService.changeUserStatus(request);
    }

    @GetMapping("/getRoles")
    @PreAuthorize("hasAuthority('PERM_VIEW_ROLE')")
    public ResponseEntity<?> getRoles() {
        return adminService.getRoles();
    }

    @PostMapping("/createPermission")
    @PreAuthorize("hasAuthority('PERM_CREATE_PERMISSION')")
    public ResponseEntity<?> createPermission(@RequestBody PermissionDTO request) {
        return adminService.createPermission(request);
    }

    @GetMapping("/getPermissions")
    @PreAuthorize("hasAuthority('PERM_VIEW_PERMISSION')")
    public ResponseEntity<?> getPermissions() {
        return adminService.getPermissions();
    }

    @GetMapping("/getUsers")
    @PreAuthorize("hasAuthority('PERM_VIEW_USER')")
    public ResponseEntity<?> getUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long academicProgramId,
            @RequestParam(required = false) Long facultyId
    ) {
        return adminService.getUsers(status, role, academicProgramId, facultyId);
    }

    @PutMapping("/changeUserStatus/{userId}")
    @PreAuthorize("hasAuthority('PERM_ACTIVATE_OR_DEACTIVATE_USER')")
    public ResponseEntity<?> desactiveUser(@PathVariable Long userId) {
        return adminService.desactiveUser(userId);
    }

    @GetMapping("/modalities")
    @PreAuthorize("hasAuthority('PERM_VIEW_MODALITIES_ADMIN')")
    public ResponseEntity<List<ModalityDTO>> getModalities(@RequestParam(required = false) ModalityStatus status) {
        return adminService.getModalities(status);
    }

    @PostMapping("/assign-program-head")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_PROGRAM_HEAD')")
    public ResponseEntity<?> assignProgramHead(@RequestBody assignAuthorityProgram request){

        try {
            ProgramAuthority assigned = adminService.assignProgramHead(request);
            return ResponseEntity.ok(
                    Map.of(
                        "message", "Se ha asignado el jefe de programa correctamente"
                     )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @PostMapping("/assign-project-director")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_PROGRAM_HEAD')")
    public ResponseEntity<?> assignProjectDirector(@RequestBody assignAuthorityProgram request){

        try {
            ProgramAuthority assigned = adminService.assignProjectDirector(request);
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Se ha asignado el director de proyecto correctamente"
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @PostMapping("/assign-committee-member")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_PROGRAM_HEAD')")
    public ResponseEntity<?> assignCommittee(@RequestBody assignAuthorityProgram request){

        try {
            ProgramAuthority assigned = adminService.assignCommittee(request);
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Se ha asignado el miembro del comité correctamente"
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @PostMapping("/assign-examiner")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_EXAMINER')")
    public ResponseEntity<?> assignExaminer(@RequestBody assignAuthorityProgram request){

        try {
            ProgramAuthority assigned = adminService.assignExaminer(request);
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Se ha asignado el jurado/evaluador (examiner) correctamente"
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @PostMapping("/register-user")
    @PreAuthorize("hasAuthority('PERM_CREATE_USER')")
    public ResponseEntity<?> registerUserByAdmin(@RequestBody RegisterUserByAdminRequest request) {
        return adminService.registerUserByAdmin(request);
    }

    /**
     * Vincula un jurado a múltiples programas académicos en una sola operación.
     * Si el usuario no tiene el rol EXAMINER, se lo asigna automáticamente.
     * Body: { "userId": 1, "academicProgramIds": [1, 2, 3] }
     */
    @PostMapping("/examiner/assign-programs")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_EXAMINER')")
    public ResponseEntity<?> assignExaminerToMultiplePrograms(
            @RequestBody AssignExaminerMultipleProgramsRequest request) {
        return adminService.assignExaminerToMultiplePrograms(request);
    }

    /**
     * Vincula un jurado existente a un programa académico adicional.
     * Un jurado puede estar vinculado a múltiples programas académicos.
     */
    @PostMapping("/examiner/assign-program")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_EXAMINER')")
    public ResponseEntity<?> assignExaminerToAdditionalProgram(@RequestBody assignAuthorityProgram request) {
        return adminService.assignExaminerToAdditionalProgram(request);
    }

    /**
     * Desvincula un jurado de un programa académico específico.
     */
    @DeleteMapping("/examiner/{userId}/program/{academicProgramId}")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_EXAMINER')")
    public ResponseEntity<?> removeExaminerFromProgram(
            @PathVariable Long userId,
            @PathVariable Long academicProgramId) {
        return adminService.removeExaminerFromProgram(userId, academicProgramId);
    }

    /**
     * Retorna todos los programas académicos a los que está asociado un jurado.
     */
    @GetMapping("/examiner/{userId}/programs")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_EXAMINER')")
    public ResponseEntity<?> getExaminerPrograms(@PathVariable Long userId) {
        return adminService.getExaminerPrograms(userId);
    }

}
