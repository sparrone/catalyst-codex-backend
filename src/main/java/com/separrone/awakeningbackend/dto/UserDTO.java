package com.separrone.awakeningbackend.dto;

import com.separrone.awakeningbackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDTO {
    private final Long id;
    private final String username;
    private final String email;

    public static UserDTO fromEntity(User user) {
        return new UserDTO(user.getId(), user.getUsername(), user.getEmail());
    }
}
