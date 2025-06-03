package org.nab.new_afm_back.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;


@Getter
@Setter
public class UploadAdds {
    private MultipartFile additionalFile;
    private String category;
}
