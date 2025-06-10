package org.nab.new_afm_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Predicate{
    private String label;
    private String subLabel;
    private String text;

    public Predicate() {

    }
}