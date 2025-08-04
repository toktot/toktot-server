package com.toktot.domain.folder.repository;

import com.toktot.domain.folder.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, Long> {
}
