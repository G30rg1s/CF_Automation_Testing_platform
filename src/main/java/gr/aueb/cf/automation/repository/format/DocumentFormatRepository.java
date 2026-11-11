package gr.aueb.cf.automation.repository.format;

import gr.aueb.cf.automation.entity.format.DocumentFormatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentFormatRepository extends JpaRepository<DocumentFormatEntity, Long> {

    List<DocumentFormatEntity> findByDocument_IdOrderByParagraphIndexAsc(Long documentId);

    Optional<DocumentFormatEntity> findByDocument_IdAndParagraphIndex(Long documentId, Integer paragraphIndex);

    boolean existsByDocument_Id(Long documentId);

    long countByDocument_Id(Long documentId);

    void deleteByDocument_IdAndParagraphIndexGreaterThanEqual(Long documentId, Integer paragraphIndex);

    void deleteByDocument_Id(Long documentId);
}