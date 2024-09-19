package pe.com.yzm.webclient.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponse implements Serializable {

    static final long serialVersionUID = 1497966138172503544L;
    private String accessToken;
}
