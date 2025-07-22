package com.toktot.domain.user.type;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AccountStatus {
    ACTIVE("활성"),
    INACTIVE("비활성"),
    SUSPENDED("정지");

    private final String description;

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
