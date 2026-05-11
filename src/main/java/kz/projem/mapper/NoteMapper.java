package kz.projem.mapper;

import kz.projem.domain.model.Note;
import kz.projem.dto.request.NoteRequest;
import kz.projem.dto.response.NoteResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NoteMapper {

    NoteResponse toResponse(Note note);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "archived", ignore = true)
    Note toEntity(NoteRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "archived", ignore = true)
    void updateFromRequest(NoteRequest request, @MappingTarget Note note);
}
