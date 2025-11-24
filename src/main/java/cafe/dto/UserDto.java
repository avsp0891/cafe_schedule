package cafe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserDto {
    @Size(min = 3, max = 20)
    private String username;
    @Email
    private String email;
    @Size(min = 6, max = 40)
    private String password;
    private String firstName;
    private String lastName;
    private String position;
    private Set<String> roles;


}
