package com.artha.domain;

/** Will/estate planning status. SPEC REF: FR-7.5 */
public enum WillStatus {
    INCLUDED,       // Asset is included in the will
    NOT_INCLUDED,   // Asset exists but not in will
    NO_WILL         // No will has been created
}
