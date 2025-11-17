package cg.xis.eradio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;

    public UserProfileResponse(Long id, String name, String email, String role) {
        this.id = id;
        this.fullName = name;
        this.email = email;
        this.role = role;
    }
}
