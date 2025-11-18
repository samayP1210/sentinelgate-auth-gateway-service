package com.sentinelgate.database.mysql.dao;

import com.sentinelgate.database.mysql.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDao extends JpaRepository<User, Long> {

    Optional<User> findFirstByUsername(String username);

}
