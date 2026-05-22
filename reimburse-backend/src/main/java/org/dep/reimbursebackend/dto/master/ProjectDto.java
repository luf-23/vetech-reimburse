package org.dep.reimbursebackend.dto.master;

import lombok.Data;
import org.dep.reimbursebackend.entity.Project;

@Data
public class ProjectDto {
    private String projectId;
    private String projectNo;
    private String projectName;

    public static ProjectDto from(Project e) {
        ProjectDto dto = new ProjectDto();
        dto.setProjectId(e.getProjectId());
        dto.setProjectNo(e.getProjectNo());
        dto.setProjectName(e.getProjectName());
        return dto;
    }
}
