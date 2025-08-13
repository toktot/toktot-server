package com.toktot.domain.folder.service;

import com.toktot.domain.folder.Folder;
import com.toktot.domain.folder.repository.FolderRepository;
import com.toktot.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FolderDefaultService {

    private final FolderRepository folderRepository;

    @Transactional
    public void ensureDefaultFolderExists(User user) {
        boolean hasDefaultFolder = folderRepository.existsByUserAndIsDefaultTrue(user);

        if (!hasDefaultFolder) {
            folderRepository.save(Folder.createDefaultFolder(user));
            log.info("Created default folder for user: {}", user.getId());
        }
    }
}
