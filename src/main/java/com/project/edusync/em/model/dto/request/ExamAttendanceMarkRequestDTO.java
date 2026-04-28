package com.project.edusync.em.model.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamAttendanceMarkRequestDTO {

    @NotNull
    private Long examScheduleId;

    @NotNull
    private Long roomId;

    @Valid
    @NotEmpty
    @JsonProperty("attendances")
    @JsonAlias("entries")
    private List<ExamAttendanceMarkEntryDTO> attendances;

    @JsonIgnore
    public List<ExamAttendanceMarkEntryDTO> getEntries() {
        return attendances;
    }

    @JsonIgnore
    public void setEntries(List<ExamAttendanceMarkEntryDTO> entries) {
        this.attendances = entries;
    }
}

