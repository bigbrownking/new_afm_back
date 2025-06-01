package org.nab.new_afm_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Document {
    private String header;
    private List<Predicate> predicates;

    public Document() {
        this.predicates = new ArrayList<>();
    }
    public void setPredicate(String label, String text){
        predicates.add(new Predicate(label, text));
    }
}

@Getter
@AllArgsConstructor
class Predicate{
    private String label;
    private String text;
}
