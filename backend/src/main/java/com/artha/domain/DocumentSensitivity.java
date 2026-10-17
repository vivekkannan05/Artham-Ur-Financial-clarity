package com.artha.domain;

/** Document sensitivity level. SPEC REF: FR-7.6, NFR-3.3 */
public enum DocumentSensitivity {
    NORMAL,         // General financial documents
    SENSITIVE,      // Passwords, credentials — excluded from exports
    CONFIDENTIAL    // Legal documents — restricted access
}
