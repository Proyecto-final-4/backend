package com.backend.backend.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link User} entities. */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  /** Looks up a user by their unique email address. */
  Optional<User> findByEmail(String email);
}
