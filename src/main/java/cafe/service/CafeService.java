package cafe.service;

import cafe.dto.CafeDto;
import cafe.model.Cafe;
import cafe.repository.CafeRepository;
import cafe.repository.ScheduleEntryRepository;
import cafe.repository.ScheduleMonthRepository;
import cafe.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CafeService {

    @Autowired
    private CafeRepository cafeRepository;
    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;
    @Autowired
    private ScheduleMonthRepository scheduleMonthRepository;

    public List<CafeDto> getAllCafes() {
        return cafeRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CafeDto createCafe(CafeDto dto) {
        Cafe cafe = Cafe.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .build();
        cafe = cafeRepository.save(cafe);
        return toDto(cafe);
    }

    public CafeDto updateCafe(Long id, CafeDto dto) {
        Cafe cafe = cafeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cafe not found with id: " + id));
        cafe.setName(dto.getName());
        cafe.setAddress(dto.getAddress());
        cafe.setPhone(dto.getPhone());
        cafe = cafeRepository.save(cafe);
        return toDto(cafe);
    }

    @Transactional
    public void deleteCafe(Long id) {
        Cafe cafe = cafeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cafe not found with id: " + id));
        scheduleMonthRepository.findByCafeId(id).forEach(month -> {
            scheduleEntryRepository.deleteAllByScheduleMonthIdAndCafeId(month.getId(), id);
            scheduleMonthRepository.delete(month);
        });
        cafeRepository.delete(cafe);
    }

    private CafeDto toDto(Cafe cafe) {
        return CafeDto.builder()
                .id(cafe.getId())
                .name(cafe.getName())
                .address(cafe.getAddress())
                .phone(cafe.getPhone())
                .build();
    }
}
