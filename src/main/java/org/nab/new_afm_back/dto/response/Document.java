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
    private List<Risk> risks;

    public Document() {
        this.predicates = new ArrayList<>();
        this.risks = new ArrayList<>();
    }
    public void setPredicate(String label, String subLabel, String text){
        predicates.add(new Predicate(label, subLabel, text));
    }
    public void setRisk(String label, String subLabel, String text){
        risks.add(new Risk(label, subLabel, text));
    }
}





