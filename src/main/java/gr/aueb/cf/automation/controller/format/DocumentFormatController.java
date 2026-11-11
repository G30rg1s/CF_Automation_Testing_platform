package gr.aueb.cf.automation.controller.format;

import gr.aueb.cf.automation.dto.format.DocumentFormatDto;
import gr.aueb.cf.automation.dto.format.UpdateDocumentFormatDto;
import gr.aueb.cf.automation.service.format.DocumentFormatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cf_automation")
@RequiredArgsConstructor
public class DocumentFormatController {

    private final DocumentFormatService documentFormatService;

    /* =========================================
       GET DOCUMENT FORMATS
       ========================================= */

    @GetMapping("/documents/{documentId}/formats")
    public ResponseEntity<List<DocumentFormatDto>> getByDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentFormatService.getByDocument(documentId));
    }

    /* ========================================
       UPDATE DOCUMENT FORMATS
       ======================================== */

    @PutMapping("/documents/{documentId}/formats")
    public ResponseEntity<Void> updateFormats(@PathVariable Long documentId, @RequestBody List<UpdateDocumentFormatDto> updates) {
        documentFormatService.updateFormats(documentId, updates);
        return ResponseEntity.ok().build();
    }

    /* ========================================
       DELETE DOCUMENT FORMATS
       ======================================== */

    @DeleteMapping("/documents/{documentId}/formats")
    public ResponseEntity<Void> deleteByDocument(@PathVariable Long documentId) {
        documentFormatService.deleteByDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}