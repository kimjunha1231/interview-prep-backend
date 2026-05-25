package com.junha.interview.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.util.List;

@Entity
@Table(name = "question", indexes = {
    @Index(name = "idx_question_category_subject", columnList = "category, subject")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    public Question(Long id, String category, String subject, String title, String perfectAnswer) {
        this.id = id;
        this.category = category;
        this.subject = subject;
        this.title = title;
        this.perfectAnswer = perfectAnswer;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 50)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "perfect_answer", nullable = false, columnDefinition = "TEXT")
    private String perfectAnswer;

    @Column(nullable = true)
    private Integer importance;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String caveats;

    @Column(name = "tail_questions", columnDefinition = "TEXT")
    @Convert(converter = TailQuestionsConverter.class)
    private List<TailQuestion> tailQuestions;

    @Column(name = "references_data", columnDefinition = "TEXT")
    @Convert(converter = ReferencesConverter.class)
    private List<ReferenceItem> references;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TailQuestion {
        private String question;
        private String answer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ReferenceItem {
        private String name;
        private String url;
    }

    @Converter
    public static class TailQuestionsConverter implements AttributeConverter<List<TailQuestion>, String> {
        private static final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<TailQuestion> attribute) {
            try {
                return attribute == null ? null : objectMapper.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error converting List<TailQuestion> to JSON string", e);
            }
        }

        @Override
        public List<TailQuestion> convertToEntityAttribute(String dbData) {
            try {
                return dbData == null ? null : objectMapper.readValue(dbData, new TypeReference<List<TailQuestion>>() {});
            } catch (IOException e) {
                throw new IllegalArgumentException("Error converting JSON string to List<TailQuestion>", e);
            }
        }
    }

    @Converter
    public static class ReferencesConverter implements AttributeConverter<List<ReferenceItem>, String> {
        private static final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<ReferenceItem> attribute) {
            try {
                return attribute == null ? null : objectMapper.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error converting List<ReferenceItem> to JSON string", e);
            }
        }

        @Override
        public List<ReferenceItem> convertToEntityAttribute(String dbData) {
            try {
                return dbData == null ? null : objectMapper.readValue(dbData, new TypeReference<List<ReferenceItem>>() {});
            } catch (IOException e) {
                throw new IllegalArgumentException("Error converting JSON string to List<ReferenceItem>", e);
            }
        }
    }
}
