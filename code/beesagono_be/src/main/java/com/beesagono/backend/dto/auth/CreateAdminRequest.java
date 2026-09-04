package com.beesagono.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAdminRequest {

    @NotBlank(message = "Il nome utente è obbligatorio.")
    @Size(min = 3, max = 20, message = "Il nome utente deve contenere tra 3 e 20 caratteri.")
    private String username;

    @NotBlank(message = "L'email è obbligatoria.")
    @Email(message = "Formato email non valido.")
    private String email;

    @NotBlank(message = "La password è obbligatoria.")
    @Size(min = 6, message = "La password deve contenere almeno 6 caratteri.")
    private String password;
}