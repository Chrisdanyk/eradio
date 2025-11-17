package cg.xis.eradio.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    public String getUsername(){
        return this.username;
    }

    public  String getPassword(){
        return this.password;
    }
}

