package cafe.controller;

import cafe.dto.FullScheduleDto;
import cafe.dto.MyScheduleDto;
import cafe.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    // === Сотрудник: получить своё расписание ===
    @GetMapping("/my")
    public ResponseEntity<FullScheduleDto> getMySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ResponseEntity.ok(scheduleService.getMySchedule(month));
    }

    // === Сотрудник и менеджер: сохранить своё расписание ===
    @PostMapping("/my")
    public ResponseEntity<FullScheduleDto> saveMySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @RequestBody MyScheduleDto dto) {
        FullScheduleDto result = scheduleService.saveMySchedule(month, dto);
        return ResponseEntity.ok(result);
    }

    // === Менеджер и сотрудник: получить всё расписание ===
    @GetMapping("/all")
    public ResponseEntity<FullScheduleDto> getAllSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ResponseEntity.ok(scheduleService.getAllSchedule(month));
    }

    // === Менеджер: сохранить всё расписание ===
    @PostMapping("/all")
    public ResponseEntity<FullScheduleDto> saveAllSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @RequestBody FullScheduleDto dto) {
        FullScheduleDto result = scheduleService.saveAllSchedule(month, dto);
        return ResponseEntity.ok(result);
    }

    // === Менеджер: утвердить/отменить утверждение месяц ===
    @PostMapping("/approve")
    public ResponseEntity<FullScheduleDto> approveSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @RequestParam(defaultValue = "true") boolean approved) {
        return ResponseEntity.ok(scheduleService.approveSchedule(month, approved));
    }

    // === Статус утверждения (для всех) ===
    @GetMapping("/status")
    public ResponseEntity<Boolean> getApprovalStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ResponseEntity.ok(scheduleService.isApproved(month));
    }
}