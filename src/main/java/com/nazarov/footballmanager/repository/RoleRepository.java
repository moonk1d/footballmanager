package com.nazarov.footballmanager.repository;

import com.nazarov.footballmanager.domain.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

  Optional<Role> findByName(String name);
}
