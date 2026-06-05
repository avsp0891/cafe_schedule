package cafe.controller;

import cafe.dto.NewsDto;
import cafe.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "News", description = "Эндпоинты для управления новостями")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/news")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('USER_ADMIN')")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @Operation(
            summary = "Получить список новостей",
            description = "Возвращает все новости, отсортированные по дате публикации (новые сверху). Доступно администраторам."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список новостей",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NewsDto.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @GetMapping
    public ResponseEntity<List<NewsDto>> getAll() {
        return ResponseEntity.ok(newsService.getAll());
    }

    @Operation(
            summary = "Получить опубликованные новости",
            description = "Возвращает все новости для отображения на главной странице. Доступно всем аутентифицированным пользователям."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список новостей",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NewsDto.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    })
    @GetMapping("/published")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NewsDto>> getPublished() {
        return ResponseEntity.ok(newsService.getPublished());
    }

    @Operation(
            summary = "Получить новость по ID",
            description = "Возвращает одну новость. Доступно администраторам."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новость найдена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewsDto.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Новость не найдена")
    })
    @GetMapping("/{id}")
    public ResponseEntity<NewsDto> getById(
            @Parameter(description = "ID новости", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable Long id) {
        return ResponseEntity.ok(newsService.getById(id));
    }

    @Operation(
            summary = "Создать новость",
            description = "Создаёт новость с указанными заголовком и текстом. Дата публикации проставляется автоматически. Доступно только `USER_ADMIN`."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новость создана",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewsDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PostMapping
    public ResponseEntity<NewsDto> create(@Valid @RequestBody NewsDto dto) {
        return ResponseEntity.ok(newsService.create(dto));
    }

    @Operation(
            summary = "Обновить новость",
            description = "Обновляет заголовок и текст новости. Дата публикации сохраняется. Доступно только `USER_ADMIN`."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новость обновлена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewsDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Новость не найдена")
    })
    @PutMapping("/{id}")
    public ResponseEntity<NewsDto> update(
            @Parameter(description = "ID новости", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable Long id,
            @Valid @RequestBody NewsDto dto) {
        return ResponseEntity.ok(newsService.update(id, dto));
    }

    @Operation(
            summary = "Удалить новость",
            description = "Безвозвратно удаляет новость по ID. Доступно только `USER_ADMIN`."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Новость удалена"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Новость не найдена")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID новости", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable Long id) {
        newsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
