package cafe.controller;

import cafe.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Profile", description = "Эндпоинты для работы с профилем текущего пользователя")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    @Operation(
            summary = "Получить данные текущего пользователя",
            description = "Возвращает информацию о пользователе, чей JWT-токен передан в заголовке запроса."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные пользователя получены",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CurrentUserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    @GetMapping
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return ResponseEntity.ok(new CurrentUserResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getPosition(),
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
        ));
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "Ответ с данными текущего пользователя")
    public record CurrentUserResponse(
            @io.swagger.v3.oas.annotations.media.Schema(description = "ID пользователя", example = "1")
            Long id,
            @io.swagger.v3.oas.annotations.media.Schema(description = "Логин пользователя", example = "john_doe")
            String username,
            @io.swagger.v3.oas.annotations.media.Schema(description = "Email пользователя", example = "john@cafe.com")
            String email,
            @io.swagger.v3.oas.annotations.media.Schema(description = "Имя", example = "Иван")
            String firstName,
            @io.swagger.v3.oas.annotations.media.Schema(description = "Фамилия", example = "Петров")
            String lastName,
            @io.swagger.v3.oas.annotations.media.Schema(description = "Должность", example = "Бариста")
            String position,
            @io.swagger.v3.oas.annotations.media.Schema(description = "Список ролей пользователя")
            List<String> roles
    ) {
    }
}