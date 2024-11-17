package com.reliaquest.api.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.reliaquest.api.model.CreateEmployee;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private EmployeeService employeeService;

    @Test
    public void testGetAllEmployees() {
        String baseUrl = "http://localhost:" + port + "/api/v1/employee";
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee("1", "emp1", 25, 50000, "Vice President", "emp1@company.com"));
        employees.add(new Employee("2", "emp2", 35, 60000, "Manager", "emp2@company.com"));

        when(employeeService.getAllEmployees()).thenReturn(employees);

        ResponseEntity<Employee[]> response = restTemplate.getForEntity(baseUrl, Employee[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().length == 2);
    }

    @Test
    public void testGetEmployeesByNameSearch() {
        String searchString = "Vaibhav";
        String baseUrl = "http://localhost:" + port + "/api/v1/employee/search/" + searchString;
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee("1", "Vaibhav Dave", 25, 50000, "Vice President", "emp1@company.com"));

        when(employeeService.getEmployeesByNameSearch(searchString)).thenReturn(employees);

        ResponseEntity<Employee[]> response = restTemplate.getForEntity(baseUrl, Employee[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().length == 1);
    }

    @Test
    public void testGetEmployeeById() {
        String id = "1";
        String baseUrl = "http://localhost:" + port + "/api/v1/employee/" + id;
        Employee emp1 = new Employee("1", "Vaibhav Dave", 25, 50000, "Vice President", "emp1@company.com");
        when(employeeService.getEmployeeById(id)).thenReturn(emp1);

        ResponseEntity<Employee> response = restTemplate.getForEntity(baseUrl, Employee.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getId().equals(id));
    }

    @Test
    public void testGetHighestSalaryOfEmployees() {
        String baseUrl = "http://localhost:" + port + "/api/v1/employee/highestSalary";
        when(employeeService.getHighestSalaryOfEmployees()).thenReturn(Optional.of(100000));
        ResponseEntity<Integer> response = restTemplate.getForEntity(baseUrl, Integer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().equals(100000));
    }

    @Test
    public void testGetTopTenHighestEarningEmployeeNames() {
        String baseUrl = "http://localhost:" + port + "/api/v1/employee/topTenHighestEarningEmployeeNames";
        when(employeeService.getTopTenHighestEarningEmployeeNames())
                .thenReturn(List.of(
                        "emp1Name",
                        "emp2Name",
                        "emp3Name",
                        "emp4Name",
                        "emp5Name",
                        "emp6Name",
                        "emp7Name",
                        "emp8Name",
                        "emp9Name",
                        "emp10Name"));
        ResponseEntity<String[]> response = restTemplate.getForEntity(baseUrl, String[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().length == 10);
    }

    @Test
    public void testCreateEmployee() {
        String baseUrl = "http://localhost:" + port + "/api/v1/employee";

        CreateEmployee newEmployee = new CreateEmployee("Vaibhav", 30000, 27, "Vice President");
        Employee e = new Employee("1", "Vaibhav", 30000, 27, "Vice President", "vaibhav@company.com");
        when(employeeService.createEmployee(newEmployee)).thenReturn(e);
        ResponseEntity<Employee> response = restTemplate.postForEntity(baseUrl, newEmployee, Employee.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Vaibhav", response.getBody().getName());
        assertEquals(30000, response.getBody().getSalary());
    }

    @Test
    public void testDeleteEmployeeById() {
        String empId = "1";
        String baseUrl = "http://localhost:" + port + "/api/v1/employee";
        when(employeeService.deleteEmployeeById(empId)).thenReturn("Vaibhav");
        ResponseEntity<String> response =
                restTemplate.exchange(baseUrl + "/" + empId, HttpMethod.DELETE, null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Vaibhav", response.getBody());
    }
}
