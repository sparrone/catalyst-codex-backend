package com.separrone.awakeningbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeEmailRequestDTO {
    private String currentPassword;
    private String newEmail;
}
