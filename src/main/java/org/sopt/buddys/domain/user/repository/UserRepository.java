package org.sopt.buddys.domain.user.repository;

import org.sopt.buddys.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
