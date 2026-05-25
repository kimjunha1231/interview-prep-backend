package com.junha.interview.dto.interview;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionRequest {
    private Long memberId;
    
    @NotBlank(message = "카테고리는 필수 입력 값입니다.")
    private String category;
    
    private String subject;
    private List<String> subjects;
    private Integer count;
    private String portfolioText;
}
