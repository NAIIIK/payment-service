package com.example.paymentservice.infrastructure.persistence.mapper;

import com.example.paymentservice.domain.user.User;
import com.example.paymentservice.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserJpaEntity toJpa(User user) {
        UserJpaEntity entity = new UserJpaEntity();

        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setPassword(user.password());
        entity.setEmail(user.email());
        entity.setRole(user.role());

        return entity;
    }

    public User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getPassword(),
                entity.getEmail(),
                entity.getRole()
        );
    }
}
