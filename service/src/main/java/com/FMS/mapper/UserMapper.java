package com.FMS.mapper;

import com.FMS.dto.UserDto;
import com.FMS.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "username", source = "username")
    UserDto toDto(User user);
}
