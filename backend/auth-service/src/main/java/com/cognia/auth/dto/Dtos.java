package com.cognia.auth.dto;

import com.cognia.auth.entity.Usuario;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public static final String PASSWORD_RE = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MSG = "La contrasena debe tener entre 8 y 72 caracteres, con letras y numeros";

    public record RegistroRequest(@NotBlank @Size(max = 120) String nombre, @NotBlank @Email @Size(max = 190) String correo,
                                  @NotBlank @Pattern(regexp = PASSWORD_RE, message = PASSWORD_MSG) String password) {}

    public record LoginRequest(@NotBlank @Size(max = 190) String correo, @NotBlank @Size(max = 72) String password) {}

    public record UsuarioDto(Long id, String nombre, String correo, Usuario.Rol rol, Usuario.Estado estado, Instant creadoEn) {}

    public record TokenResponse(String token, String tipo, long expiraEnSegundos, UsuarioDto usuario) {}

    public record PerfilRequest(@NotBlank @Size(max = 120) String nombre) {}

    public record CambioPasswordRequest(@NotBlank String actual, @NotBlank @Pattern(regexp = PASSWORD_RE, message = PASSWORD_MSG) String nueva) {}

    public record UsuarioAdminRequest(@NotBlank @Size(max = 120) String nombre, @NotBlank @Email @Size(max = 190) String correo,
                                      @NotNull Usuario.Rol rol, @Pattern(regexp = PASSWORD_RE, message = PASSWORD_MSG) String password) {}

    public record EditarUsuarioRequest(@NotBlank @Size(max = 120) String nombre, @NotNull Usuario.Rol rol) {}

    public record EstadoUsuarioRequest(@NotNull Usuario.Estado estado) {}

    public record PasswordTemporalResponse(String passwordTemporal) {}

    public record InicializarPropietarioRequest(@NotBlank @Size(max = 120) String nombre, @NotBlank @Email String correo,
                                                @NotBlank @Pattern(regexp = PASSWORD_RE, message = PASSWORD_MSG) String password) {}

    public record AuditoriaDto(Long id, Long usuarioId, String accion, String recurso, String recursoId, String resultado, String detalle,
                               String correlacionId, Instant creadoEn) {}

    public record PageDto<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {}
}