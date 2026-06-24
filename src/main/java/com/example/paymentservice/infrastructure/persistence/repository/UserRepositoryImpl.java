package com.example.paymentservice.infrastructure.persistence.repository;

import com.example.paymentservice.domain.user.User;
import com.example.paymentservice.domain.user.UserRepository;
import com.example.paymentservice.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final SpringDataUserRepository repository;
    private final UserMapper mapper;

    @Override
    public void save(User user) {
        repository.save(mapper.toJpa(user));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username)
                .map(mapper::toDomain);
    }
}
