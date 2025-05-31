package org.nab.new_afm_back.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;


import java.util.List;

@Getter
@Setter
@Schema(description = "Request object for uploading a new case")
public class UploadCaseRequest {

    @Schema(description = "Unique case number", example = "CASE-2024-001", required = true)
    private String number;

    @Schema(description = "Author of the case", example = "John Smith", required = true)
    private String author;

    @Schema(description = "Investigating officer", example = "Detective Brown")
    private String investigator;

    @Schema(description = "Police officer assigned", example = "Officer Johnson")
    private String policeman;

    @Schema(description = "List of article IDs associated with the case", example = "[1, 2, 3]")
    private List<Integer> articles;

    @Schema(description = "Object or subject of the case", example = "Theft investigation")
    private String object;
}