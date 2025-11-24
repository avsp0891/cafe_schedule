package cafe.controller;

import cafe.dto.*;
import cafe.model.ScheduleEntry;
import cafe.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    // === Сотрудник: получить своё расписание ===
    @GetMapping("/my")
    public ResponseEntity<List<ScheduleEntry>> getMySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ResponseEntity.ok(scheduleService.getMySchedule(month));
    }

    // === Сотрудник: сохранить своё расписание ===
    @PostMapping("/my")
    public ResponseEntity<?> saveMySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @RequestBody MyScheduleDto dto) {
        scheduleService.saveMySchedule(month, dto);
        return ResponseEntity.ok("Saved");
    }

    // === Менеджер: получить всё расписание ===
    @GetMapping("/all")
    public ResponseEntity<FullScheduleDto> getAllSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ResponseEntity.ok(scheduleService.getAllSchedule(month));
    }

    // === Менеджер: сохранить всё расписание ===
    @PostMapping("/all")
    public ResponseEntity<?> saveAllSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @RequestBody FullScheduleDto dto) {
        scheduleService.saveAllSchedule(month, dto);
        return ResponseEntity.ok("Saved");
    }

    // === Менеджер: утвердить месяц ===
    @PostMapping("/approve")
    public ResponseEntity<?> approveSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        scheduleService.approveSchedule(month);
        return ResponseEntity.ok("Approved");
    }

    // === Статус утверждения (для всех) ===
    @GetMapping("/status")
    public ResponseEntity<Boolean> getApprovalStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ResponseEntity.ok(scheduleService.isApproved(month));
    }
}