package com.artha.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/** Document attachment (polymorphic link to any entity). SPEC REF: FR-7.6 */
@Entity
@Table(name = "document")
public class Document extends BaseEntity {

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "linked_entity_type", length = 50)
    private String linkedEntityType;

    @Column(name = "linked_entity_id")
    private UUID linkedEntityId;

    @Column(name = "document_date")
    private LocalDate documentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensitivity", nullable = false)
    private DocumentSensitivity sensitivity = DocumentSensitivity.NORMAL;

    @Column(name = "source", length = 50)
    private String source;

    // ─── Getters & Setters ───────────────────────────────────
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getLinkedEntityType() { return linkedEntityType; }
    public void setLinkedEntityType(String linkedEntityType) { this.linkedEntityType = linkedEntityType; }
    public UUID getLinkedEntityId() { return linkedEntityId; }
    public void setLinkedEntityId(UUID linkedEntityId) { this.linkedEntityId = linkedEntityId; }
    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }
    public DocumentSensitivity getSensitivity() { return sensitivity; }
    public void setSensitivity(DocumentSensitivity sensitivity) { this.sensitivity = sensitivity; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
