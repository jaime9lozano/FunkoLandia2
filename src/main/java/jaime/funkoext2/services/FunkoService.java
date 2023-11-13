package jaime.funkoext2.services;

import jaime.funkoext2.dto.Funkodto;
import jaime.funkoext2.dto.FunkodtoUpdated;
import jaime.funkoext2.models.Funko;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface FunkoService {
    Page<Funko> findall(Optional<String> nombre, Optional <Double> preciomax,Optional<Double> preciomin, Optional<Integer> cantidadmax, Optional<Integer> cantidadmin, Optional<String> imagen,Pageable pageable); ;
    List<Funko> findByName(String name);
    Funko findById(Long id);
    Funko save(Funkodto funkodto);
    Funko update(Long id, FunkodtoUpdated funko);
    boolean DeleteById(Long id);
}
