package cafe.controller;

import cafe.dto.CafeDto;
import cafe.service.CafeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cafe", description = "Эндпоинты для управления кафе")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/cafes")
@SecurityRequirement(name = "bearerAuth")
public class CafeController {

    @Autowired
    private CafeService cafeService;

    @Operation(
            summary = "Получить список всех кафе",
            description = "Возвращает список всех кафе. Доступно всем аутентифицированным пользователям."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список кафе получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CafeDto.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    })
    @GetMapping
    public ResponseEntity<List<CafeDto>> getAllCafes() {
        return ResponseEntity.ok(cafeService.getAllCafes());
    }

    @Operation(
            summary = "Создать новое кафе",
            description = "Создаёт новое кафе. Доступно только администраторам."
    )
    @PostMapping
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    public ResponseEntity<CafeDto> createCafe(@RequestBody CafeDto dto) {
        return ResponseEntity.ok(cafeService.createCafe(dto));
    }

    @Operation(
            summary = "Обновить кафе",
            description = "Обновляет существующее кафе по ID. Доступно только администраторам."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    public ResponseEntity<CafeDto> updateCafe(@PathVariable Long id, @RequestBody CafeDto dto) {
        return ResponseEntity.ok(cafeService.updateCafe(id, dto));
    }

    @Operation(
            summary = "Удалить кафе",
            description = "Удаляет кафе по ID. Доступно только администраторам."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    public ResponseEntity<Void> deleteCafe(@PathVariable Long id) {
        cafeService.deleteCafe(id);
        return ResponseEntity.noContent().build();
    }
}
