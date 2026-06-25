package com.example.payment_service.infrastructure.persistence.repository;

import com.example.payment_service.domain.user.User;
import com.example.payment_service.domain.user.UserRepository;
import com.example.payment_service.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username)
                .map(mapper::toDomain);
    }
}
