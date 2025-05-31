package org.nab.new_afm_back.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Case {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "surname")
    private String surname;

    @Column(name = "name")
    private String name;

    @Column(name = "fathername")
    private String fathername;

    @Column(name = "status")
    private boolean status;

    private int info;

    @Column(name = "uploadDate")
    private LocalDate uploadDate;

    @Column(name = "updateDate")
    private LocalDate updateDate;

    @Column(name = "qualificationDate")
    private LocalDate qualificationDate;

    @Column(name = "accusationDate")
    private LocalDate accusationDate;

    @Column(name = "author")
    private String author;

    @Column(name = "investigator")
    private String investigator;

    @Column(name = "policeman")
    private String policeman;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", referencedColumnName = "id")
    private Article article;

    @Column(name = "object")
    private String object;

    @ElementCollection
    @CollectionTable(name = "case_additional_files", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "file_name")
    private List<String> adds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (uploadDate == null) {
            uploadDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateDate = LocalDate.now();
    }
}
