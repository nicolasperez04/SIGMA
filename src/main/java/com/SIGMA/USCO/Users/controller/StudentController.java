package com.SIGMA.USCO.Users.controller;

import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.service.CancellationService;
import com.SIGMA.USCO.Modalities.service.StudentModalityListingService;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.Users.service.StudentService;
import com.SIGMA.USCO.academic.dto.StudentProfileRequest;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.service.DocumentService;
import com.SIGMA.USCO.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@Tag(name = "Estudiantes", description = "Operaciones para estudiantes: perfil, documentos, modalidades y cancelaciones")
@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class StudentController {

    private final StudentService studentService;
    private final StudentModalityListingService modalityListingService;
    private final CancellationService cancellationService;
    private final DocumentService documentService;
    private final StudentDocumentRepository studentDocumentRepository;
    private final UserRepository userRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;

    @Operation(summary = "Actualizar perfil del estudiante", description = "Actualiza la información del perfil del estudiante autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateStudentProfile(@RequestBody StudentProfileRequest request){
        return studentService.updateStudentProfile(request);
    }

    @Operation(summary = "Crear perfil desde historial académico", description = "Carga un archivo de historial académico para crear/actualizar el perfil del estudiante")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Archivo inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/profile/from-academic-history")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateStudentProfileFromAcademicHistory(
            @Parameter(description = "Archivo PDF del historial académico") @RequestParam("file") MultipartFile file
    ) {
        return studentService.updateStudentProfileFromAcademicHistory(file);
    }

    @Operation(summary = "Obtener perfil del estudiante", description = "Retorna la información completa del perfil del estudiante autenticado")
    @ApiResponse(responseCode = "200", description = "Perfil obtenido exitosamente")
    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getStudentInfo() {
        return studentService.getStudentProfile();
    }

    @Operation(summary = "Obtener modalidad actual", description = "Retorna la modalidad de grado activa del estudiante autenticado")
    @ApiResponse(responseCode = "200", description = "Modalidad obtenida exitosamente")
    @GetMapping("/modality/current")
    public ResponseEntity<?> getCurrentStudentModality() {
        return modalityListingService.getCurrentStudentModality();
    }

    @Operation(summary = "Obtener historial de documento", description = "Retorna el historial de cambios y estados de un documento específico")
    @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente")
    @GetMapping("/documents/{studentDocumentId}/history")
    public ResponseEntity<?> getDocumentHistory(@Parameter(description = "ID del documento del estudiante") @PathVariable Long studentDocumentId) {
        return documentService.getDocumentHistory(studentDocumentId);
    }

    @Operation(summary = "Solicitar cancelación de modalidad", description = "El estudiante solicita cancelar su inscripción en una modalidad de grado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud de cancelación registrada"),
            @ApiResponse(responseCode = "400", description = "No se puede cancelar esta modalidad"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada")
    })
    @PostMapping("/{studentModalityId}/request-cancellation")
    public ResponseEntity<?> requestCancellation(@Parameter(description = "ID de la modalidad del estudiante") @PathVariable Long studentModalityId) {
        return cancellationService.requestCancellation(studentModalityId);
    }

    @Operation(summary = "Obtener mis documentos", description = "Retorna la lista de documentos requeridos y su estado para la modalidad actual del estudiante")
    @ApiResponse(responseCode = "200", description = "Lista de documentos obtenida")
    @GetMapping("/my-documents")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyDocuments() {
        return studentService.getMyDocuments();
    }

    @Operation(summary = "Cargar documento de cancelación", description = "Carga el documento de justificación para la solicitud de cancelación de una modalidad")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento cargado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Archivo inválido"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada")
    })
    @PostMapping("/cancellation-document/{studentModalityId}")
    public ResponseEntity<?> uploadCancellationDocument(
            @Parameter(description = "ID de la modalidad del estudiante") @PathVariable Long studentModalityId,
            @Parameter(description = "Archivo de justificación en PDF") @RequestParam("file") MultipartFile file) {
        documentService.uploadCancellationDocument(studentModalityId, file);
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Documento de justificación cargado correctamente"
                )
        );
    }

    @Operation(summary = "Ver documento", description = "Permite ver/descargar un documento específico de la modalidad del estudiante. Valida que el usuario sea miembro activo de la modalidad.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento retornado"),
            @ApiResponse(responseCode = "403", description = "Sin permiso para ver este documento"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    })
    @GetMapping("/documents/{studentDocumentId}/view")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> viewMyDocument(@Parameter(description = "ID del documento del estudiante") @PathVariable Long studentDocumentId) {
        // Obtener el usuario actual autenticado
        User currentUser = SecurityUtils.getCurrentUser();

        // Buscar el documento
        Optional<StudentDocument> docOpt = studentDocumentRepository.findById(studentDocumentId);
        if (docOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Documento no encontrado");
        }

        StudentDocument document = docOpt.get();

        // Verificar que el usuario sea un miembro activo de la modalidad
        Long studentModalityId = document.getStudentModality().getId();
        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(
                studentModalityId,
                currentUser.getId()
        );

        if (!isActiveMember) {
            return ResponseEntity.status(403).body("No tienes permiso para ver este documento");
        }

        // Leer el archivo desde el sistema de archivos
        try {
            Path filePath = Paths.get(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(404).body("Archivo no encontrado o no legible");
            }

            // Detectar tipo de contenido
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Retornar el archivo como blob
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al leer el archivo: " + e.getMessage());
        }
    }




}
