package com.reliaquest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("salary")
    private Integer salary;

    @JsonProperty("age")
    private Integer age;

    @JsonProperty("title")
    private String title;

    @JsonProperty("email")
    private String email;
}
