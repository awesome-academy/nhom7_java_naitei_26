package com.nhom7.coworkingspace.specification;

import com.nhom7.coworkingspace.dto.request.UserSearchRequest;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecification {

    private UserSpecification() {
        // Private constructor for utility class
    }

    public static Specification<User> buildSearchSpecification(UserSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null) {
                query.distinct(true);
            }

            if (request != null) {
                // Keyword search in name, email, or phone
                if (StringUtils.hasText(request.getKeyword())) {
                    String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                    Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                    Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                    Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), pattern);
                    predicates.add(cb.or(nameMatch, emailMatch, phoneMatch));
                }

                // Filter by UserStatus (ACTIVE, INACTIVE, BLOCKED)
                if (request.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), request.getStatus()));
                }

                // Filter by Role name (USER, HOST, MODERATOR, ADMIN)
                if (StringUtils.hasText(request.getRole())) {
                    Join<User, Role> roleJoin = root.join("roles", JoinType.INNER);
                    predicates.add(cb.equal(cb.upper(roleJoin.get("name")), request.getRole().trim().toUpperCase()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
