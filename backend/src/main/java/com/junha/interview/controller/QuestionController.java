package com.junha.interview.controller;

import com.junha.interview.common.ApiResponse;
import com.junha.interview.domain.Question;
import com.junha.interview.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/random")
    public ApiResponse<List<Question>> getRandomQuestions(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "count", required = false) Integer count
    ) {
        List<Question> questions = questionService.getRandomQuestions(category, subject, count);
        return ApiResponse.success(questions);
    }

    @GetMapping
    public ApiResponse<List<Question>> getQuestions(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subject", required = false) String subject
    ) {
        List<Question> questions = questionService.getQuestions(category, subject);
        return ApiResponse.success(questions);
    }

    @GetMapping("/{id}")
    public ApiResponse<Question> getQuestionById(@PathVariable("id") Long id) {
        Question question = questionService.getQuestionById(id);
        return ApiResponse.success(question);
    }
}
