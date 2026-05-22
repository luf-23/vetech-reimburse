package org.dep.reimburse.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateResultVO {
    private boolean valid;
    private String message;

    public static ValidateResultVO ok() {
        return new ValidateResultVO(true, "");
    }

    public static ValidateResultVO fail(String message) {
        return new ValidateResultVO(false, message);
    }
}
