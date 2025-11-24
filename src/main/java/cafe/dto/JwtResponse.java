package cafe.dto;

import lombok.Getter;

import java.util.List;

@Getter

public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String position;
    private List<String> roles;

    public JwtResponse(String token, Long id, String username, String email,
                       String firstName, String lastName, String position,
                       List<String> roles) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.position = position;
        this.roles = roles;
    }

}
