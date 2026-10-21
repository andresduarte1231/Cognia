package com.cognia.catalogo.repository;

import com.cognia.catalogo.entity.ConfiguracionTienda;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionRepository extends JpaRepository<ConfiguracionTienda, Long> {
    Optional<ConfiguracionTienda> findByClave(String clave);
}