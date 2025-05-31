package org.nab.new_afm_back.controller;

import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.service.impl.CaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CaseController {
    private CaseService caseService;

    @GetMapping("/case")
    private ResponseEntity<Case> getCaseByNumber(){

    }
}
