package cafe.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CafeDto {
    private Long id;
    private String name;
    private String address;
    private String phone;
}
