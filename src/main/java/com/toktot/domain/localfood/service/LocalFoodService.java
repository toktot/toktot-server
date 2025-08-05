package com.toktot.domain.localfood.service;

import com.toktot.domain.localfood.repository.LocalFoodRepository;
import com.toktot.web.dto.localfood.LocalFoodResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LocalFoodService {

    private final LocalFoodRepository localFoodRepository;

    public List<LocalFoodResponse> getActiveLocalFoods() {
        return localFoodRepository
                .findByIsActiveOrderByDisplayOrderAsc(true)
                .stream()
                .map(LocalFoodResponse::from)
                .toList();
    }
}
