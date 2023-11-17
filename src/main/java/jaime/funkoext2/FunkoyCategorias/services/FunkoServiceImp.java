package jaime.funkoext2.FunkoyCategorias.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jaime.funkoext2.FunkoyCategorias.Config.WebSocket.WebSocketConfig;
import jaime.funkoext2.FunkoyCategorias.Config.WebSocket.WebSocketHandler;
import jaime.funkoext2.FunkoyCategorias.Exceptions.CategoriaNoEncontrada;
import jaime.funkoext2.FunkoyCategorias.Exceptions.FunkoNoEncontrado;
import jaime.funkoext2.FunkoyCategorias.mapper.mapeador;
import jaime.funkoext2.FunkoyCategorias.models.Categoria;
import jaime.funkoext2.FunkoyCategorias.models.Funko;
import jaime.funkoext2.FunkoyCategorias.repository.FunkoRepository;
import jaime.funkoext2.FunkoyCategorias.WebSocket.FunkoNotificacionMapper;
import jaime.funkoext2.FunkoyCategorias.WebSocket.FunkoNotificacionResponse;
import jaime.funkoext2.FunkoyCategorias.WebSocket.Notificacion;
import jaime.funkoext2.FunkoyCategorias.dto.Funkodto;
import jaime.funkoext2.FunkoyCategorias.dto.FunkodtoUpdated;
import jaime.funkoext2.FunkoyCategorias.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@CacheConfig(cacheNames = {"funkos"})
public class FunkoServiceImp implements FunkoService {
    FunkoRepository funkoRepository;
    CategoriaRepository categoriaRepository;
    mapeador map = new mapeador();

    private final WebSocketConfig webSocketConfig;
    private final ObjectMapper mapper;
    private final FunkoNotificacionMapper funkoNotificacionMapper;
    private WebSocketHandler webSocketService;
    @Autowired
    public FunkoServiceImp(FunkoRepository funkoRepository,CategoriaRepository categoriaRepository, WebSocketConfig webSocketConfig, FunkoNotificacionMapper funkoNotificacionMapper) {
        this.webSocketConfig = webSocketConfig;
        mapper = new ObjectMapper();
        this.funkoNotificacionMapper = funkoNotificacionMapper;
        this.funkoRepository = funkoRepository;
        this.categoriaRepository = categoriaRepository;
        webSocketService = webSocketConfig.webSocketFunkosHandler();
    }
    @Override
    public Page<Funko> findall(Optional<String> nombre, Optional <Double> preciomax, Optional<Double> preciomin, Optional<Integer> cantidadmax, Optional<Integer> cantidadmin, Optional<String> imagen, Pageable pageable) {
        Specification<Funko> specNombreFunko = (root, query, criteriaBuilder) ->
                nombre.map(m -> criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), "%" + m.toLowerCase() + "%"))
                        .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));
        Specification<Funko> specPrecioMaxFunko = (root, query, criteriaBuilder) ->
                preciomax.map(p -> criteriaBuilder.lessThanOrEqualTo(root.get("precio"), p))
                        .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));
        Specification<Funko> specPrecioMinFunko = (root, query, criteriaBuilder) ->
                preciomin.map(p -> criteriaBuilder.greaterThanOrEqualTo(root.get("precio"), p))
                        .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));
        Specification<Funko> specCantMaxFunko = (root, query, criteriaBuilder) ->
                cantidadmax.map(p -> criteriaBuilder.lessThanOrEqualTo(root.get("cantidad"), p))
                        .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));
        Specification<Funko> specCantMinFunko = (root, query, criteriaBuilder) ->
                cantidadmin.map(p -> criteriaBuilder.greaterThanOrEqualTo(root.get("cantidad"), p))
                        .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));
        Specification<Funko> specImagenFunko = (root, query, criteriaBuilder) ->
                imagen.map(m -> criteriaBuilder.like(criteriaBuilder.lower(root.get("imagen")), "%" + m.toLowerCase() + "%"))
                        .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));
        Specification<Funko> criterio = Specification.where(specNombreFunko)
                .and(specPrecioMaxFunko)
                .and(specPrecioMinFunko)
                .and(specCantMaxFunko)
                .and(specCantMinFunko)
                .and(specImagenFunko);
        return funkoRepository.findAll(criterio, pageable);
    }


    @Override
    @Cacheable
    public List<Funko> findByName(String name) {
        return funkoRepository.findByNombre(name);
    }

    @Override
    @Cacheable
    public Funko findById(Long id) {
        return funkoRepository.findById(id).orElseThrow(() -> new FunkoNoEncontrado(id));
    }

    @Override
    @CachePut
    public Funko save(Funkodto funkodto) {
        Categoria categoria= categoriaRepository.findByCategoria(funkodto.getCategoria().toUpperCase());
        if (categoria == null) {
            throw new CategoriaNoEncontrada(funkodto.getCategoria().toUpperCase());
        }
        Funko funko1 = map.toFunkoNew(funkodto,categoria);
        onChange(Notificacion.Tipo.CREATE, funko1);
        return funkoRepository.save(funko1);
    }

    @Override
    @CachePut
    public Funko update(Long id, FunkodtoUpdated funkoUpdated) {
        Optional<Funko> existingFunko = funkoRepository.findById(id);
        Categoria categoria = null;
        if (existingFunko.isPresent()) {
            if (funkoUpdated.getCategoria() != null && !funkoUpdated.getCategoria().isEmpty()) {
                categoria = categoriaRepository.findByCategoria(funkoUpdated.getCategoria().toUpperCase());
                if (categoria == null) {
                    throw new CategoriaNoEncontrada(funkoUpdated.getCategoria().toUpperCase());
                }
            } else {
                categoria = existingFunko.get().getCategoria();
            }
            Funko funko1 =map.toFunkoUpdated(funkoUpdated, existingFunko.get(),categoria);
            onChange(Notificacion.Tipo.UPDATE, funko1);
            return funkoRepository.save(funko1);
        } else {
            throw new FunkoNoEncontrado(id);
        }
    }

    @Override
    @CacheEvict
    public boolean DeleteById(Long id) {
        Optional<Funko> existingFunko = funkoRepository.findById(id);
        if (existingFunko.isPresent()) {
            onChange(Notificacion.Tipo.DELETE, existingFunko.get());
            funkoRepository.deleteById(id);
            return true;
        }else {
            throw new FunkoNoEncontrado(id);
        }
    }
    public void onChange(Notificacion.Tipo tipo, Funko data) {

        if (webSocketService == null) {
            webSocketService = this.webSocketConfig.webSocketFunkosHandler();
        }

        try {
            Notificacion<FunkoNotificacionResponse> notificacion = new Notificacion<>(
                    "PRODUCTOS",
                    tipo,
                    FunkoNotificacionMapper.toResponse(data),
                    LocalDateTime.now().toString()
            );

            String json = mapper.writeValueAsString((notificacion));


            // Enviamos el mensaje a los clientes ws con un hilo, si hay muchos clientes, puede tardar
            // no bloqueamos el hilo principal que atiende las peticiones http
            Thread senderThread = new Thread(() -> {
                try {
                    webSocketService.sendMessage(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            senderThread.start();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    // Para los test
    public void setWebSocketService(WebSocketHandler webSocketHandlerMock) {
        this.webSocketService = webSocketHandlerMock;
    }
}
