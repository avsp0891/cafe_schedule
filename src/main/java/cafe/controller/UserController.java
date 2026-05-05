package cafe.controller;

import cafe.dto.UserDto;
import cafe.model.User;
import cafe.service.UserService;
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

@Tag(name = "User Management", description = "Эндпоинты для управления пользователями (требует роль USER_ADMIN)")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(
            summary = "Создать нового пользователя",
            description = "Создаёт пользователя с указанными данными. Доступно только пользователям с ролью `USER_ADMIN`."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "409", description = "Username или Email уже заняты")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    public ResponseEntity<User> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные нового пользователя",
                    required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class)))
            @Valid @RequestBody UserDto userDto) {
        return ResponseEntity.status(201).body(userService.createUser(userDto));
    }

    @Operation(
            summary = "Обновить данные пользователя",
            description = "Частичное обновление полей пользователя. Нулевые значения игнорируются. Только `USER_ADMIN`."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь обновлён",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    public ResponseEntity<User> updateUser(
            @Parameter(description = "ID пользователя", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Поля для обновления (только ненулевые)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class)))
            @Valid @RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }

    @Operation(
            summary = "Удалить пользователя",
            description = "Безвозвратно удаляет пользователя по ID. Только `USER_ADMIN`."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь удалён"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя", required = true, example = "1", in = ParameterIn.PATH)
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Получить список всех пользователей",
            description = "Возвращает всех пользователей системы. Доступно `USER_ADMIN` и `CAFE_ADMIN`."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class, type = "array"))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('USER_ADMIN') or hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}