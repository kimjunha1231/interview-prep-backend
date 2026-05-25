package com.junha.interview.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    @NotNull(message = "질문 ID는 필수 입력 값입니다.")
    private Long questionId;

    @NotBlank(message = "답변 내용은 필수 입력 값입니다.")
    private String userAnswer;
}
