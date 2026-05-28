package cafe.controller;

import cafe.dto.FullScheduleDto;
import cafe.dto.MyScheduleDto;
import cafe.service.ScheduleService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Schedule", description = "Эндпоинты для управления расписанием сотрудников кафе")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/schedule")
@SecurityRequirement(name = "bearerAuth")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Operation(
            summary = "Получить моё расписание",
            description = "Возвращает расписание текущего пользователя для указанного месяца и кафе."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Расписание получено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FullScheduleDto.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Кафе не найдено")
    })
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('STAFF') or hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<FullScheduleDto> getMySchedule(
            @Parameter(description = "Месяц в формате YYYY-MM-DD (например, 2024-05-01)",
                    required = true, example = "2024-05-01", in = ParameterIn.QUERY)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @Parameter(description = "ID кафе", required = true, example = "1", in = ParameterIn.QUERY)
            @RequestParam Long cafeId) {
        return ResponseEntity.ok(scheduleService.getMySchedule(month, cafeId));
    }

    @Operation(
            summary = "Сохранить моё расписание",
            description = "Сохраняет или обновляет расписание текущего пользователя. " +
                    "Принимает список смен с датами и временными интервалами."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Расписание сохранено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FullScheduleDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные (пересечение смен, неверный месяц)"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав или нет доступа к кафе"),
            @ApiResponse(responseCode = "409", description = "Расписание уже утверждено и заблокировано")
    })
    @PostMapping("/my")
    @PreAuthorize("hasAuthority('STAFF') or hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<FullScheduleDto> saveMySchedule(
            @Parameter(description = "Месяц в формате YYYY-MM-DD", required = true, example = "2024-05-01", in = ParameterIn.QUERY)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Список смен для сохранения",
                    required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MyScheduleDto.class)))
            @Valid @RequestBody MyScheduleDto dto) {
        return ResponseEntity.ok(scheduleService.saveMySchedule(month, dto));
    }

    @Operation(
            summary = "Получить полное расписание кафе",
            description = "Возвращает расписание всех сотрудников указанного кафе за месяц. Только `CAFE_ADMIN` или `USER_ADMIN`."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Расписание получено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FullScheduleDto.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Кафе не найдено")
    })
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('CAFE_ADMIN') or hasAuthority('USER_ADMIN') or hasAuthority('STAFF')")
    public ResponseEntity<FullScheduleDto> getAllSchedule(
            @Parameter(description = "Месяц в формате YYYY-MM-DD", required = true, example = "2024-05-01", in = ParameterIn.QUERY)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @Parameter(description = "ID кафе", required = true, example = "1", in = ParameterIn.QUERY)
            @RequestParam Long cafeId) {
        return ResponseEntity.ok(scheduleService.getAllSchedule(month, cafeId));
    }

    @Operation(
            summary = "Сохранить полное расписание кафе",
            description = "Массовое сохранение расписания для всех сотрудников кафе. Только `CAFE_ADMIN`."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Расписание сохранено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FullScheduleDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "409", description = "Расписание уже утверждено")
    })
    @PostMapping("/all")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<FullScheduleDto> saveAllSchedule(
            @Parameter(description = "Месяц в формате YYYY-MM-DD", required = true, example = "2024-05-01", in = ParameterIn.QUERY)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Полное расписание кафе со сменами всех сотрудников",
                    required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FullScheduleDto.class)))
            @Valid @RequestBody FullScheduleDto dto) {
        return ResponseEntity.ok(scheduleService.saveAllSchedule(month, dto));
    }

    @Operation(
            summary = "Утвердить или отменить утверждение расписания",
            description = "Блокирует или разблокирует расписание месяца для редактирования. Только `CAFE_ADMIN`."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус изменён",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FullScheduleDto.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Кафе или месяц не найдены")
    })
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<FullScheduleDto> approveSchedule(
            @Parameter(description = "Месяц в формате YYYY-MM-DD", required = true, example = "2024-05-01", in = ParameterIn.QUERY)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @Parameter(description = "ID кафе", required = true, example = "1", in = ParameterIn.QUERY)
            @RequestParam Long cafeId,
            @Parameter(description = "true — утвердить, false — отменить", example = "true", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "true") boolean approved) {
        return ResponseEntity.ok(scheduleService.approveSchedule(month, cafeId, approved));
    }

    @Operation(
            summary = "Проверить статус утверждения расписания",
            description = "Возвращает `true`, если расписание месяца утверждено и заблокировано для изменений."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "boolean"))),
            @ApiResponse(responseCode = "404", description = "Кафе или месяц не найдены")
    })
    @GetMapping("/status")
    public ResponseEntity<Boolean> getApprovalStatus(
            @Parameter(description = "Месяц в формате YYYY-MM-DD", required = true, example = "2024-05-01", in = ParameterIn.QUERY)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @Parameter(description = "ID кафе", required = true, example = "1", in = ParameterIn.QUERY)
            @RequestParam Long cafeId) {
        return ResponseEntity.ok(scheduleService.isApproved(month, cafeId));
    }
}