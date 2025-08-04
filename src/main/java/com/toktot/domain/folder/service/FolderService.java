package com.toktot.domain.folder.service;

import com.toktot.domain.folder.Folder;
import com.toktot.domain.folder.repository.FolderRepository;
import com.toktot.domain.folder.repository.FolderReviewRepository;
import com.toktot.domain.user.User;
import com.toktot.web.dto.folder.response.FolderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final FolderReviewRepository folderReviewRepository;

    @Transactional
    public FolderResponse createFolder(User user, String folderName) {

        Folder folder = Folder.create(user, folderName);
        folderRepository.save(folder);

        return FolderResponse.from(folder);
    }
}
