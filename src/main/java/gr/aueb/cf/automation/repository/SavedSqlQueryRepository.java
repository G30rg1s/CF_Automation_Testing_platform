
package gr.aueb.cf.automation.repository;

import gr.aueb.cf.automation.entity.SavedSqlQuery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedSqlQueryRepository
        extends JpaRepository<SavedSqlQuery, Long> {

    // ========================================
    // GET ALL QUERIES BY OWNER
    // ========================================

    List<SavedSqlQuery> findByOwnerOrderByUpdatedAtDesc(
            String owner
    );

    // ========================================
    // SEARCH BY TITLE
    // ========================================

    List<SavedSqlQuery>
    findByOwnerAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
            String owner,
            String title
    );

    // ========================================
    // FIND BY LOCAL ID AND OWNER
    // ========================================

    Optional<SavedSqlQuery> findByIdAndOwner(
            Long id,
            String owner
    );

    // ========================================
    // FIND BY SHARED UUID AND OWNER
    // ========================================

    Optional<SavedSqlQuery> findByQueryUuidAndOwner(
            String queryUuid,
            String owner
    );

    // ========================================
    // CHECK IF UUID EXISTS
    // ========================================

    boolean existsByQueryUuid(
            String queryUuid
    );

    // ========================================
    // DELETE BY SHARED UUID AND OWNER
    // ========================================

    long deleteByQueryUuidAndOwner(
            String queryUuid,
            String owner
    );
}
