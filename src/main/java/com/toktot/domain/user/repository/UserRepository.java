package com.toktot.domain.user.repository;

import com.toktot.domain.user.User;
import com.toktot.domain.user.type.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    @Query("SELECT u FROM User u WHERE " +
            "(u.email = :identifier AND u.authProvider = 'EMAIL') OR " +
            "(u.oauthId = :identifier AND u.authProvider = :authProvider)")
    Optional<User> findByIdentifierAndAuthProvider(@Param("identifier") String identifier,
                                                   @Param("authProvider") AuthProvider authProvider);

    Optional<User> findByIdAndDeletedAtIsNull(Long userId);
}
