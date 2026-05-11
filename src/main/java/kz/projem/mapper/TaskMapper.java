package kz.projem.mapper;

import kz.projem.domain.model.Task;
import kz.projem.dto.request.TaskRequest;
import kz.projem.dto.response.TaskResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    TaskResponse toResponse(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "status", defaultValue = "TODO")
    @Mapping(target = "priority", defaultValue = "MEDIUM")
    Task toEntity(TaskRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    void updateFromRequest(TaskRequest request, @MappingTarget Task task);
}
