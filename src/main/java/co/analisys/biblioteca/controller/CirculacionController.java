package co.analisys.biblioteca.controller;

import co.analisys.biblioteca.model.LibroId;
import co.analisys.biblioteca.model.Prestamo;
import co.analisys.biblioteca.model.PrestamoId;
import co.analisys.biblioteca.model.UsuarioId;
import co.analisys.biblioteca.service.CirculacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/circulacion")
public class CirculacionController {
    @Autowired
    private CirculacionService circulacionService;
    @Operation(
            summary = "Prestar un libro a un usuario",
            description = "Este endpoint permite prestar un libro a un usuario específico, si el libro está disponible. " +
                    "Se debe proporcionar el ID del usuario y el ID del libro como parámetros."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El libro ha sido prestado exitosamente"),
            @ApiResponse(responseCode = "400", description = "El libro no está disponible"),
            @ApiResponse(responseCode = "404", description = "Usuario o libro no encontrado")
    })
    @PostMapping("/prestar")
    @PreAuthorize("hasRole('ROLE_LIBRARIAN')")
    public void prestarLibro(@RequestParam String usuarioId, @RequestParam String libroId) {
        circulacionService.prestarLibro(new UsuarioId(usuarioId), new LibroId(libroId));
    }

    @Operation(
            summary = "Devolver un libro prestado",
            description = "Este endpoint permite devolver un libro que ha sido previamente prestado. " +
                    "Se debe proporcionar el ID del préstamo como parámetro."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El libro ha sido devuelto exitosamente"),
            @ApiResponse(responseCode = "404", description = "Préstamo no encontrado")
    })
    @PostMapping("/devolver")
    @PreAuthorize("hasRole('ROLE_LIBRARIAN')")
    public void devolverLibro(@RequestParam String prestamoId) {
        circulacionService.devolverLibro(new PrestamoId(prestamoId));
    }
    @Operation(
            summary = "Consultar todos los préstamos",
            description = "Este endpoint permite obtener una lista de todos los préstamos registrados en el sistema. " +
            "Es importante que el cliente esté registrado previamente en la base de datos, " +
            "de lo contrario no podrá acceder a esta información."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de todos los préstamos devuelta exitosamente")
    })
    @GetMapping("/prestamos")
    @PreAuthorize("hasAnyRole('ROLE_LIBRARIAN', 'ROLE_USER')")
    public List<Prestamo> obtenerTodosPrestamos() {
        return circulacionService.obtenerTodosPrestamos();
    }

    @GetMapping("/public/status")
    public String getPublicStatus() {
        return "El servicio de circulación está funcionando correctamente";
    }
}
