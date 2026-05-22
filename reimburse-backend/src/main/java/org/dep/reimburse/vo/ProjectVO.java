package org.dep.reimburse.vo;

import lombok.Data;
import org.dep.reimburse.entity.Project;

@Data
public class ProjectVO {
    private String projectId;
    private String projectNo;
    private String projectName;

    public static ProjectVO from(Project e) {
        ProjectVO vo = new ProjectVO();
        vo.setProjectId(e.getProjectId());
        vo.setProjectNo(e.getProjectNo());
        vo.setProjectName(e.getProjectName());
        return vo;
    }
}
