package com.toktot.domain.user.repository;

import com.toktot.domain.user.User;
import com.toktot.domain.user.type.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    @Query("""
    SELECT COUNT(r) FROM Review r
    WHERE r.user.id = :userId
    """)
    Integer countReviewsByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT AVG(t.rating) FROM Review r
    JOIN r.images i
    JOIN i.tooltips t
    WHERE r.user.id = :userId
    AND t.rating IS NOT NULL
    """)
    BigDecimal calculateAverageRatingByUserId(@Param("userId") Long userId);
}
