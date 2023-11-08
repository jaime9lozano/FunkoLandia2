package jaime.funkoext2.storage.Controller;

import jaime.funkoext2.dto.FunkodtoUpdated;
import jaime.funkoext2.models.Funko;
import jaime.funkoext2.services.FunkoService;
import jaime.funkoext2.storage.Services.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/storage")
public class StorageController {
    private final StorageService storageService;
    private final FunkoService funkoService;

    @Autowired
    public StorageController(StorageService storageService, FunkoService funkoService) {
        this.storageService = storageService;
        this.funkoService = funkoService;
    }
    @GetMapping(value = "{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename, HttpServletRequest request) {
        Resource file = storageService.loadAsResource(filename);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(file.getFile().getAbsolutePath());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede determinar el tipo de fichero");
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(file);
    }
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestPart("file") MultipartFile file) {

        // Almacenamos el fichero y obtenemos su URL
        String urlImagen = null;

        if (!file.isEmpty()) {
            String imagen = storageService.store(file);
            urlImagen = storageService.getUrl(imagen);
            Map<String, Object> response = Map.of("url", urlImagen);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede subir un fichero vacío");
        }
    }
    @PatchMapping(value = "/imagen/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Funko> nuevoProducto(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {

        if (!file.isEmpty()) {
            String imagen = storageService.store(file);
            String urlImagen = storageService.getUrl(imagen);

            Funko funko = funkoService.findById(id);
            funko.setImagen(urlImagen);
            FunkodtoUpdated dto = new FunkodtoUpdated(funko.getNombre(), funko.getPrecio(), funko.getCantidad(), funko.getImagen(), funko.getCategoria().getCategoria());
            funkoService.update(id,dto);

            return ResponseEntity.ok(funko);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha enviado la imagen");
        }
    }
}
