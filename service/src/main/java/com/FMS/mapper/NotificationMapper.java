package com.FMS.mapper;

import com.FMS.dto.NotificationDto;
import com.FMS.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationDto toDto(Notification notification);
}
