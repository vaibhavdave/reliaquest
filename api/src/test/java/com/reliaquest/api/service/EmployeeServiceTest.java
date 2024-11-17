package com.reliaquest.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.RateLimitExceededException;
import com.reliaquest.api.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

public class EmployeeServiceTest {

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(employeeService, "MAX_BACKOFF_TIME_IN_SEC", 60);
        ReflectionTestUtils.setField(employeeService, "INITIAL_BACKOFF_TIME", 5);
        ReflectionTestUtils.setField(employeeService, "serverUrl", "http://localhost:8000/api/v1/employee");
    }

    @Test
    void testGetAllEmployees() {

        MockEmployee mockEmp1 = new MockEmployee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        MockEmployee mockEmp2 = new MockEmployee("2", "Satish Dhawan", 60000, 25, "Manager", "emp2@company.com");

        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp2 = new Employee("2", "Satish Dhawan", 60000, 25, "Manager", "emp2@company.com");

        Response<List<MockEmployee>> mockResponse = Response.handledWith(List.of(mockEmp1, mockEmp2));
        EmployeeService spyService = spy(employeeService);
        doAnswer(new Answer<Response<List<MockEmployee>>>() {
                    @Override
                    public Response<List<MockEmployee>> answer(InvocationOnMock invocation) {
                        return mockResponse;
                    }
                })
                .when(spyService)
                .executeWithRetry(any(Supplier.class));

        when(modelMapper.map(mockEmp1, Employee.class)).thenReturn(emp1);
        when(modelMapper.map(mockEmp2, Employee.class)).thenReturn(emp2);

        List<Employee> employees = spyService.getAllEmployees();

        assertEquals(2, employees.size());

        assertEquals(emp1.getName(), employees.get(0).getName());
        assertEquals(emp1.getAge(), employees.get(0).getAge());
        assertEquals(emp1.getSalary(), employees.get(0).getSalary());
        assertEquals(emp1.getTitle(), employees.get(0).getTitle());
        assertEquals(emp1.getId(), employees.get(0).getId());
        assertEquals(emp1.getEmail(), employees.get(0).getEmail());

        assertEquals(emp2.getName(), employees.get(1).getName());
        assertEquals(emp2.getAge(), employees.get(1).getAge());
        assertEquals(emp2.getSalary(), employees.get(1).getSalary());
        assertEquals(emp2.getTitle(), employees.get(1).getTitle());
        assertEquals(emp2.getId(), employees.get(1).getId());
        assertEquals(emp2.getEmail(), employees.get(1).getEmail());
    }

    @Test
    void testGetEmployeesByNameSearch_should_return_searched_employees_with_exact_match() {

        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp2 = new Employee("2", "Satish Dhawan", 60000, 25, "Manager", "emp2@company.com");

        List<Employee> employees = List.of(emp1, emp2);
        EmployeeService spyService = spy(employeeService);
        doAnswer(new Answer<List<Employee>>() {
                    @Override
                    public List<Employee> answer(InvocationOnMock invocation) {
                        return employees;
                    }
                })
                .when(spyService)
                .getAllEmployees();

        List<Employee> searchedEmployees = spyService.getEmployeesByNameSearch("Vaibhav Dave");

        assertEquals(1, searchedEmployees.size());

        assertEquals(emp1.getName(), employees.get(0).getName());
        assertEquals(emp1.getAge(), employees.get(0).getAge());
        assertEquals(emp1.getSalary(), employees.get(0).getSalary());
        assertEquals(emp1.getTitle(), employees.get(0).getTitle());
        assertEquals(emp1.getId(), employees.get(0).getId());
        assertEquals(emp1.getEmail(), employees.get(0).getEmail());
    }

    @Test
    void testGetEmployeesByNameSearch_should_return_searched_employees_with_partial_match() {

        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp2 = new Employee("2", "Satish Dhawan", 60000, 25, "Manager", "emp2@company.com");

        List<Employee> employees = List.of(emp1, emp2);
        EmployeeService spyService = spy(employeeService);
        doAnswer(new Answer<List<Employee>>() {
                    @Override
                    public List<Employee> answer(InvocationOnMock invocation) {
                        return employees;
                    }
                })
                .when(spyService)
                .getAllEmployees();

        List<Employee> searchedEmployees = spyService.getEmployeesByNameSearch("Vaibhav");

        assertEquals(1, searchedEmployees.size());

        assertEquals(emp1.getName(), employees.get(0).getName());
        assertEquals(emp1.getAge(), employees.get(0).getAge());
        assertEquals(emp1.getSalary(), employees.get(0).getSalary());
        assertEquals(emp1.getTitle(), employees.get(0).getTitle());
        assertEquals(emp1.getId(), employees.get(0).getId());
        assertEquals(emp1.getEmail(), employees.get(0).getEmail());
    }

    @Test
    void testGetEmployeesByNameSearch_should_return_searched_employees_with_case_insensitive_match() {

        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp2 = new Employee("2", "Satish Dhawan", 60000, 25, "Manager", "emp2@company.com");

        List<Employee> employees = List.of(emp1, emp2);
        EmployeeService spyService = spy(employeeService);
        doAnswer(new Answer<List<Employee>>() {
                    @Override
                    public List<Employee> answer(InvocationOnMock invocation) {
                        return employees;
                    }
                })
                .when(spyService)
                .getAllEmployees();

        List<Employee> searchedEmployees = spyService.getEmployeesByNameSearch("vaibhav");

        assertEquals(1, searchedEmployees.size());

        assertEquals(emp1.getName(), employees.get(0).getName());
        assertEquals(emp1.getAge(), employees.get(0).getAge());
        assertEquals(emp1.getSalary(), employees.get(0).getSalary());
        assertEquals(emp1.getTitle(), employees.get(0).getTitle());
        assertEquals(emp1.getId(), employees.get(0).getId());
        assertEquals(emp1.getEmail(), employees.get(0).getEmail());
    }

    @Test
    void testGetEmployeesByNameSearch_should_return_empty_list_when_searched_employees_not_found() {

        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp2 = new Employee("2", "Satish Dhawan", 60000, 25, "Manager", "emp2@company.com");

        List<Employee> employees = List.of(emp1, emp2);
        EmployeeService spyService = spy(employeeService);
        doAnswer(new Answer<List<Employee>>() {
                    @Override
                    public List<Employee> answer(InvocationOnMock invocation) {
                        return employees;
                    }
                })
                .when(spyService)
                .getAllEmployees();

        List<Employee> searchedEmployees = spyService.getEmployeesByNameSearch("akash");
        assertEquals(0, searchedEmployees.size());
    }

    @Test
    void testGetEmployeeById_should_return_employee_when_found_by_id() {
        MockEmployee mockEmp1 = new MockEmployee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");

        Response<MockEmployee> mockResponse = Response.handledWith(mockEmp1);
        EmployeeService spyService = spy(employeeService);
        doAnswer(new Answer<Response<MockEmployee>>() {
                    @Override
                    public Response<MockEmployee> answer(InvocationOnMock invocation) {
                        return mockResponse;
                    }
                })
                .when(spyService)
                .executeWithRetry(any(Supplier.class));

        when(modelMapper.map(mockEmp1, Employee.class)).thenReturn(emp1);
        Employee employee = spyService.getEmployeeById("1");

        assertEquals(emp1.getName(), employee.getName());
        assertEquals(emp1.getAge(), employee.getAge());
        assertEquals(emp1.getSalary(), employee.getSalary());
        assertEquals(emp1.getTitle(), employee.getTitle());
        assertEquals(emp1.getId(), employee.getId());
        assertEquals(emp1.getEmail(), employee.getEmail());
    }

    @Test
    void
            testGetEmployeeById_should_throw_employee_not_found_exception_when_remote_service_invocation_returns_status_404() {
        String idToSearch = "1";
        EmployeeService spyService = spy(employeeService);
        doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(404)))
                .when(spyService)
                .executeWithRetry(any(Supplier.class));
        EmployeeNotFoundException employeeNotFoundException =
                assertThrows(EmployeeNotFoundException.class, () -> spyService.getEmployeeById("1"));

        assertEquals(
                String.format("Employee with id: %s not found", idToSearch), employeeNotFoundException.getMessage());
    }

    @Test
    void
            testGetEmployeeById_should_throw_runtime_exception_when_remote_service_invocation_returns_status_other_than_2xx_and_404() {
        String idToSearch = "1";
        EmployeeService spyService = spy(employeeService);
        doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(403)))
                .when(spyService)
                .executeWithRetry(any(Supplier.class));
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> spyService.getEmployeeById("1"));

        assertEquals(
                String.format("Employee with id: %s could not be found", idToSearch), runtimeException.getMessage());
    }

    @Test
    void testGetHighestSalaryOfEmployees_should_return_highest_salary() {
        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp2 = new Employee("2", "Satish Dhawan", 60000, 25, "Manager", "emp2@company.com");

        List<Employee> employees = new ArrayList<>();
        employees.add(emp1);
        employees.add(emp2);

        EmployeeService spyService = spy(employeeService);
        doAnswer(new Answer<List<Employee>>() {
                    @Override
                    public List<Employee> answer(InvocationOnMock invocation) {
                        return employees;
                    }
                })
                .when(spyService)
                .getAllEmployees();

        Optional<Integer> highestSalaryOfEmployees = spyService.getHighestSalaryOfEmployees();
        assertEquals(emp2.getSalary(), highestSalaryOfEmployees.get());
    }

    @Test
    void testGetTopTenHighestEarningEmployeeNames_should_return_top_10_salaries_in_descending_order() {
        Employee emp1 = new Employee("1", "emp1", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp2 = new Employee("2", "emp2", 60000, 25, "Manager", "emp2@company.com");
        Employee emp3 = new Employee("3", "emp3", 70000, 24, "Vice President", "emp3@company.com");
        Employee emp4 = new Employee("4", "emp4", 80000, 25, "Manager", "emp4@company.com");
        Employee emp5 = new Employee("5", "emp5", 90000, 24, "Vice President", "emp5@company.com");
        Employee emp6 = new Employee("6", "emp6", 100000, 25, "Manager", "emp6@company.com");
        Employee emp7 = new Employee("7", "emp7", 25000, 24, "Vice President", "emp7@company.com");
        Employee emp8 = new Employee("8", "emp8", 35000, 25, "Manager", "emp8@company.com");
        Employee emp9 = new Employee("9", "emp9", 45000, 24, "Vice President", "emp9@company.com");
        Employee emp10 = new Employee("10", "emp10", 55000, 25, "Manager", "emp10@company.com");
        Employee emp11 = new Employee("11", "emp11", 75000, 24, "Vice President", "emp11@company.com");
        Employee emp12 = new Employee("12", "emp12", 95000, 25, "Manager", "emp12@company.com");
        Employee emp13 = new Employee("13", "emp13", 105000, 24, "Vice President", "emp13@company.com");
        Employee emp14 = new Employee("14", "emp14", 110000, 25, "Manager", "emp14@company.com");

        List<Employee> employees = new ArrayList<>();
        employees.add(emp1);
        employees.add(emp2);
        employees.add(emp3);
        employees.add(emp4);
        employees.add(emp5);
        employees.add(emp6);
        employees.add(emp7);
        employees.add(emp8);
        employees.add(emp9);
        employees.add(emp10);
        employees.add(emp11);
        employees.add(emp12);
        employees.add(emp13);
        employees.add(emp14);

        EmployeeService spyService = spy(employeeService);
        doAnswer(new Answer<List<Employee>>() {
                    @Override
                    public List<Employee> answer(InvocationOnMock invocation) {
                        return employees;
                    }
                })
                .when(spyService)
                .getAllEmployees();

        List<String> highestSalaryEmployeeNames = spyService.getTopTenHighestEarningEmployeeNames();
        assertEquals(10, highestSalaryEmployeeNames.size());
        assertEquals(emp14.getName(), highestSalaryEmployeeNames.get(0));
        assertEquals(emp13.getName(), highestSalaryEmployeeNames.get(1));
        assertEquals(emp6.getName(), highestSalaryEmployeeNames.get(2));
        assertEquals(emp12.getName(), highestSalaryEmployeeNames.get(3));
        assertEquals(emp5.getName(), highestSalaryEmployeeNames.get(4));
        assertEquals(emp4.getName(), highestSalaryEmployeeNames.get(5));
        assertEquals(emp11.getName(), highestSalaryEmployeeNames.get(6));
        assertEquals(emp3.getName(), highestSalaryEmployeeNames.get(7));
        assertEquals(emp2.getName(), highestSalaryEmployeeNames.get(8));
        assertEquals(emp10.getName(), highestSalaryEmployeeNames.get(9));
    }

    @Test
    void testCreateEmployee_should_return_successfully_created_employee() {
        MockEmployee mockEmp1 = new MockEmployee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");

        Response<MockEmployee> mockResponse = Response.handledWith(mockEmp1);
        EmployeeService spyService = spy(employeeService);
        doAnswer(new Answer<Response<MockEmployee>>() {
                    @Override
                    public Response<MockEmployee> answer(InvocationOnMock invocation) {
                        return mockResponse;
                    }
                })
                .when(spyService)
                .executeWithRetry(any(Supplier.class));

        when(modelMapper.map(mockEmp1, Employee.class)).thenReturn(emp1);

        CreateEmployee createRequest = new CreateEmployee();
        createRequest.setName("Vaibhav Dave");
        createRequest.setAge(24);
        createRequest.setSalary(50000);
        createRequest.setTitle("Vice President");

        Employee employee = spyService.createEmployee(createRequest);
        assertEquals(emp1.getName(), employee.getName());
        assertEquals(emp1.getEmail(), employee.getEmail());
        assertEquals(emp1.getId(), employee.getId());
        assertEquals(emp1.getAge(), employee.getAge());
        assertEquals(emp1.getTitle(), employee.getTitle());
        assertEquals(emp1.getSalary(), employee.getSalary());
    }

    @Test
    void testCreateEmployee_should_throw_runtime_exception_when_remote_service_invocation_returns_status_4xx_or_5xx() {
        CreateEmployee createRequest = new CreateEmployee();
        createRequest.setName("Vaibhav Dave");
        createRequest.setAge(24);
        createRequest.setSalary(50000);
        createRequest.setTitle("Vice President");

        EmployeeService spyService = spy(employeeService);
        doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(403)))
                .when(spyService)
                .executeWithRetry(any(Supplier.class));
        RuntimeException runtimeException =
                assertThrows(RuntimeException.class, () -> spyService.createEmployee(createRequest));
        assertNotNull(runtimeException.getMessage());
        assertNotNull(runtimeException.getCause());
    }

    @Test
    void testDeleteEmployeeById_should_return_name_of_deleted_employee_when_remote_invocation_succeeds() {
        MockEmployee mockEmp1 = new MockEmployee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        DeleteEmployee delEmployeeRequest = new DeleteEmployee("Vaibhav Dave");

        Response<Boolean> mockResponse = Response.handledWith(true);
        EmployeeService spyService = spy(employeeService);

        doAnswer(new Answer<Employee>() {
                    @Override
                    public Employee answer(InvocationOnMock invocation) {
                        return emp1;
                    }
                })
                .when(spyService)
                .getEmployeeById(anyString());

        doAnswer(new Answer<Response<Boolean>>() {
                    @Override
                    public Response<Boolean> answer(InvocationOnMock invocation) {
                        return mockResponse;
                    }
                })
                .when(spyService)
                .executeWithRetry(any(Supplier.class));

        when(modelMapper.map(emp1, DeleteEmployee.class)).thenReturn(delEmployeeRequest);

        String deleteEmployeeName = spyService.deleteEmployeeById("1");
        assertEquals(emp1.getName(), deleteEmployeeName);
    }

    @Test
    void testDeleteEmployeeById_should_throw_exception_when_remote_invocation_returns_false() {
        MockEmployee mockEmp1 = new MockEmployee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        DeleteEmployee delEmployeeRequest = new DeleteEmployee("Vaibhav Dave");

        Response<Boolean> mockResponse = Response.handledWith(false);
        EmployeeService spyService = spy(employeeService);

        doAnswer(new Answer<Employee>() {
                    @Override
                    public Employee answer(InvocationOnMock invocation) {
                        return emp1;
                    }
                })
                .when(spyService)
                .getEmployeeById(anyString());

        doAnswer(new Answer<Response<Boolean>>() {
                    @Override
                    public Response<Boolean> answer(InvocationOnMock invocation) {
                        return mockResponse;
                    }
                })
                .when(spyService)
                .executeWithRetry(any(Supplier.class));

        when(modelMapper.map(emp1, DeleteEmployee.class)).thenReturn(delEmployeeRequest);

        String employeeIdToSearch = "2";
        RuntimeException runtimeException =
                assertThrows(RuntimeException.class, () -> spyService.deleteEmployeeById(employeeIdToSearch));
        assertNotNull(runtimeException.getMessage());
        assertNull(runtimeException.getCause());
        assertEquals(
                String.format("Failed to delete employee with id: %s", employeeIdToSearch),
                runtimeException.getMessage());
    }

    @Test
    void testDeleteEmployeeById_should_throw_exception_when_remote_invocation_returns_statuscode_other_than_2xx() {
        MockEmployee mockEmp1 = new MockEmployee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        Employee emp1 = new Employee("1", "Vaibhav Dave", 50000, 24, "Vice President", "emp1@company.com");
        DeleteEmployee delEmployeeRequest = new DeleteEmployee("Vaibhav Dave");

        EmployeeService spyService = spy(employeeService);

        doAnswer(new Answer<Employee>() {
                    @Override
                    public Employee answer(InvocationOnMock invocation) {
                        return emp1;
                    }
                })
                .when(spyService)
                .getEmployeeById(anyString());

        doThrow(new HttpClientErrorException(HttpStatusCode.valueOf(403)))
                .when(spyService)
                .executeWithRetry(any(Supplier.class));

        when(modelMapper.map(emp1, DeleteEmployee.class)).thenReturn(delEmployeeRequest);

        String employeeIdToSearch = "2";
        RuntimeException runtimeException =
                assertThrows(RuntimeException.class, () -> spyService.deleteEmployeeById(employeeIdToSearch));
        assertNotNull(runtimeException.getMessage());
        assertNotNull(runtimeException.getCause());
        assertEquals(
                String.format("Failed to delete employee with id: %s", employeeIdToSearch),
                runtimeException.getMessage());
    }

    @Test
    void testExecuteWithRetry_should_return_responsebody_when_remote_service_invocation_returns_2xx() {
        Supplier<ResponseEntity<Response<Boolean>>> requestSupplier =
                () -> ResponseEntity.ok(Response.handledWith(true));
        Response<Boolean> response = employeeService.executeWithRetry(requestSupplier);
        assertEquals(true, response.data());
    }

    @Test
    void
            testExecuteWithRetry_should_throw_ratelimit_exceeded_exception_when_remote_service_invocation_returns_429_after_multiple_retries() {
        Supplier<ResponseEntity<Response<Boolean>>> requestSupplier =
                () -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Response.handledWith(true));
        RateLimitExceededException rateLimitExceededException =
                assertThrows(RateLimitExceededException.class, () -> employeeService.executeWithRetry(requestSupplier));
        assertEquals(
                String.format("%d attempts are exhausted. Please try after some time", 5),
                rateLimitExceededException.getMessage());
    }

    @Test
    void testExecuteWithRetry_should_rethrow_exception_when_remote_service_invocation_throws_other_than_429() {
        Supplier<ResponseEntity<Response<Boolean>>> requestSupplier =
                () -> ResponseEntity.ok(Response.handledWith(true));
        Supplier<ResponseEntity<Response<Boolean>>> mockSupplier = mock(Supplier.class);
        doThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN))
                .when(mockSupplier)
                .get();

        HttpClientErrorException httpClientErrorException =
                assertThrows(HttpClientErrorException.class, () -> employeeService.executeWithRetry(mockSupplier));
        assertEquals(HttpStatus.FORBIDDEN, httpClientErrorException.getStatusCode());
    }
}
