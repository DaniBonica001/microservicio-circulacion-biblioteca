package co.analisys.biblioteca.service;

import co.analisys.biblioteca.client.CatalogoClient;
import co.analisys.biblioteca.client.NotificacionClient;
import co.analisys.biblioteca.dto.NotificacionDTO;
import co.analisys.biblioteca.exception.LibroNoDisponibleException;
import co.analisys.biblioteca.exception.PrestamoNoEncontradoException;
import co.analisys.biblioteca.model.*;
import co.analisys.biblioteca.repository.PrestamoRepository;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class CirculacionService {
    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private CatalogoClient catalogoClient;

    @Autowired
    private NotificacionClient notificacionClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private KafkaTemplate<String, NotificacionDTO> kafkaTemplate;

    @Transactional
    public void prestarLibro(UsuarioId usuarioId, LibroId libroId) {
        Boolean libroDisponible = catalogoClient.isLibroDisponible(libroId.getLibroid_value());

        if (libroDisponible != null && libroDisponible) {
            Prestamo prestamo = new Prestamo(
                    new PrestamoId(java.util.UUID.randomUUID().toString()),
                    usuarioId,
                    libroId,
                    new FechaPrestamo(),
                    new FechaDevolucionPrevista(),
                    EstadoPrestamo.ACTIVO
            );
            prestamoRepository.save(prestamo);
            catalogoClient.actualizarDisponibilidad(libroId.getLibroid_value(), false);
//            notificacionClient.enviarNotificacion(new NotificacionDTO(usuarioId.getUsuarioid_value(), "Libro prestado: " + libroId.getLibroid_value()));
            // Enviar notificación asíncrona
            NotificacionDTO notificacion = new NotificacionDTO(usuarioId.getUsuarioid_value(), "Libro prestado: " + libroId.getLibroid_value());
            rabbitTemplate.convertAndSend("notificacion.exchange", "notificacion.routingkey", notificacion);
        } else {
            throw new LibroNoDisponibleException(libroId);
        }
    }

    @Transactional
    public void devolverLibro(PrestamoId prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new PrestamoNoEncontradoException(prestamoId));

        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamoRepository.save(prestamo);

        catalogoClient.actualizarDisponibilidad(prestamo.getLibroId().getLibroid_value(), true);

        NotificacionDTO notificacion = new NotificacionDTO(
                prestamo.getUsuarioId().getUsuarioid_value(),
                "Libro devuelto: " + prestamo.getLibroId().getLibroid_value()
        );
        kafkaTemplate.send("devolucion-libro", notificacion);
    }

    public List<Prestamo> obtenerTodosPrestamos() {
        return prestamoRepository.findAll();
    }
}
