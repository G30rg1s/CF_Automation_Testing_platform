package gr.aueb.cf.automation.repository.documents;

import gr.aueb.cf.automation.entity.documents.DocumentEntity;
import gr.aueb.cf.automation.entity.documents.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    List<DocumentEntity> findByStatus(DocumentStatus status);

    Optional<DocumentEntity> findByTitle(String title);

    boolean existsByCategory_Id(Long categoryId);

    Page<DocumentEntity> findByCategory_IdOrderByTitleAsc(Long categoryId, Pageable pageable);
}