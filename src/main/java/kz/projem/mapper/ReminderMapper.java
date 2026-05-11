package kz.projem.mapper;

import kz.projem.domain.model.Reminder;
import kz.projem.dto.request.ReminderRequest;
import kz.projem.dto.response.ReminderResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ReminderMapper {

    ReminderResponse toResponse(Reminder reminder);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    Reminder toEntity(ReminderRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateFromRequest(ReminderRequest request, @MappingTarget Reminder reminder);
}
