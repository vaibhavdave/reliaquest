package com.reliaquest.api.service;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.RateLimitExceededException;
import com.reliaquest.api.model.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class EmployeeService {

    private final RestTemplate restTemplate;

    @Value("${service.employee.max_backoff_time: 120}")
    private int MAX_BACKOFF_TIME_IN_SEC;

    @Value("${service.employee.initial_backoff_time: 10}")
    private int INITIAL_BACKOFF_TIME;

    @Value("${service.employee.baseurl: http://localhost:8112/api/v1/employee}")
    private String serverUrl;

    private final ModelMapper modelMapper;

    @Autowired
    public EmployeeService(RestTemplate restTemplate, ModelMapper modelMapper) {
        this.restTemplate = restTemplate;
        this.modelMapper = modelMapper;
    }

    public List<Employee> getAllEmployees() {
        ;
        List<Employee> employees;
        ParameterizedTypeReference<Response<List<MockEmployee>>> typeRef =
                new ParameterizedTypeReference<Response<List<MockEmployee>>>() {};
        List<MockEmployee> mockEmployees = executeWithRetry(
                        () -> restTemplate.exchange(serverUrl, HttpMethod.GET, null, typeRef))
                .data();
        employees = mockEmployees.stream()
                .map(mockEmployee -> modelMapper.map(mockEmployee, Employee.class))
                .collect(Collectors.toList());
        return employees;
    }

    public List<Employee> getEmployeesByNameSearch(String searchString) {
        log.debug("getting employees by search term:" + searchString);
        List<Employee> employees = getAllEmployees();
        employees = employees.stream()
                .filter(e -> e.getName().toLowerCase().contains(searchString.toLowerCase()))
                .collect(Collectors.toList());
        log.debug("found employees count:" + employees.size());
        return employees;
    }

    public Employee getEmployeeById(String id) {
        log.debug("getting employee by id:" + id);
        Employee emp = null;
        String baseUrl = new StringBuilder(serverUrl).append("/{id}").toString();
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .uriVariables(Map.of("id", id))
                .build()
                .toUriString();
        ParameterizedTypeReference<Response<MockEmployee>> typeRef =
                new ParameterizedTypeReference<Response<MockEmployee>>() {};
        try {
            MockEmployee mockEmployee = executeWithRetry(
                            () -> restTemplate.exchange(url, HttpMethod.GET, null, typeRef))
                    .data();
            emp = modelMapper.map(mockEmployee, Employee.class);
            log.debug(String.format("Employee found for id: %s", emp.getId()));
            return emp;

        } catch (HttpStatusCodeException e) {
            log.error(String.format("Error while invoking getEmployeeById:  %s", e));
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new EmployeeNotFoundException(String.format("Employee with id: %s not found", id), e);
            }
        }

        throw new RuntimeException(String.format("Employee with id: %s could not be found", id));
    }

    public Optional<Integer> getHighestSalaryOfEmployees() {
        log.trace("inside getHighestSalaryOfEmployees");
        List<Employee> employeeResults = getAllEmployees();
        employeeResults.sort(Comparator.comparing(Employee::getSalary).reversed());
        Optional<Integer> salary =
                employeeResults.stream().map(e -> e.getSalary()).findFirst();
        log.debug(String.format("highest salary found?: %s", salary.isPresent()));
        return salary;
    }

    public List<String> getTopTenHighestEarningEmployeeNames() {
        log.trace("inside getTopTenHighestEarningEmployeeNames");
        List<Employee> employeeResults = getAllEmployees();
        List<String> empl = employeeResults.stream()
                .sorted((e1, e2) -> Integer.compare(e2.getSalary(), e1.getSalary()))
                .limit(10)
                .map(e -> e.getName())
                .collect(Collectors.toList());
        log.debug(String.format("Size of employeelist: %s", empl.size()));
        return empl;
    }

    public Employee createEmployee(CreateEmployee input) {
        Employee emp = null;
        ParameterizedTypeReference<Response<MockEmployee>> typeRef =
                new ParameterizedTypeReference<Response<MockEmployee>>() {};
        try {
            MockEmployee mockEmployee = executeWithRetry(
                            () -> restTemplate.exchange(serverUrl, HttpMethod.POST, new HttpEntity<>(input), typeRef))
                    .data();
            emp = modelMapper.map(mockEmployee, Employee.class);
            log.info(String.format("Employee created with id: %s", emp.getId()));
            return emp;

        } catch (HttpStatusCodeException e) {
            log.error(String.format("Error while invoking createEmployee:  %s", e));
            throw new RuntimeException(String.format("Error while invoking createEmployee:  %s", e.getMessage()), e);
        }
    }

    public String deleteEmployeeById(String id) {
        Employee employeeById = getEmployeeById(id);

        DeleteEmployee deleteEmployeeInput = modelMapper.map(employeeById, DeleteEmployee.class);
        HttpEntity<DeleteEmployee> entity = new HttpEntity<DeleteEmployee>(deleteEmployeeInput);
        ParameterizedTypeReference<Response<Boolean>> typeRef = new ParameterizedTypeReference<Response<Boolean>>() {};
        try {
            Boolean isDeleted = executeWithRetry(
                            () -> restTemplate.exchange(serverUrl, HttpMethod.DELETE, entity, typeRef))
                    .data();
            if (!isDeleted) {
                throw new RuntimeException(String.format("Failed to delete employee with id: %s", id));
            }
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException(String.format("Failed to delete employee with id: %s", id), e);
        }
        log.info(String.format("Employee with id: %s deleted", id));
        return employeeById.getName();
    }

    public <T> T executeWithRetry(Supplier<ResponseEntity<T>> requestSupplier) {
        log.debug("Inside executeWithRetry");
        int attempt = 1;
        int backOffTimeInSec = INITIAL_BACKOFF_TIME;
        boolean isSuccess = false;
        T responseBody = null;

        while (backOffTimeInSec < MAX_BACKOFF_TIME_IN_SEC) {
            try {
                log.info(String.format("Proceeding with attempt: %d", attempt));
                ResponseEntity<T> response = requestSupplier.get();
                if (response.getStatusCode().is2xxSuccessful()) {
                    isSuccess = true;
                    responseBody = response.getBody();
                    break;
                }
            } catch (HttpStatusCodeException e) {
                if (!e.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                    log.error(String.format("Error while executing attempt: %s , %s", attempt, e.getMessage()));
                    throw e;
                }
            }

            attempt++;
            try {
                backOffTimeInSec *= 2;
                log.debug(String.format("Waiting for %s seconds before attempting retry", backOffTimeInSec));
                Thread.sleep(backOffTimeInSec * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!isSuccess) {
            log.error(String.format("%d attempts are exhausted. Please try after some time", attempt));
            throw new RateLimitExceededException(
                    String.format("%d attempts are exhausted. Please try after some time", attempt));
        }
        log.debug("Exiting executeWithRetry");
        return responseBody;
    }
}
