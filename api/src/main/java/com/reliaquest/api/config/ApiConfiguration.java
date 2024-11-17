package com.reliaquest.api.config;

import com.reliaquest.api.model.DeleteEmployee;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.MockEmployee;
import com.reliaquest.api.web.LoggingInterceptor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiConfiguration implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        TypeMap<MockEmployee, Employee> mockEmployeeToEmployeeType =
                modelMapper.createTypeMap(MockEmployee.class, Employee.class);
        mockEmployeeToEmployeeType.addMappings(mapper -> mapper.map(MockEmployee::getId, Employee::setId));
        mockEmployeeToEmployeeType.addMappings(mapper -> mapper.map(MockEmployee::getName, Employee::setName));
        mockEmployeeToEmployeeType.addMappings(mapper -> mapper.map(MockEmployee::getAge, Employee::setAge));
        mockEmployeeToEmployeeType.addMappings(mapper -> mapper.map(MockEmployee::getSalary, Employee::setSalary));
        mockEmployeeToEmployeeType.addMappings(mapper -> mapper.map(MockEmployee::getTitle, Employee::setTitle));
        mockEmployeeToEmployeeType.addMappings(mapper -> mapper.map(MockEmployee::getEmail, Employee::setEmail));

        TypeMap<Employee, DeleteEmployee> employeeToDeleteEmployee =
                modelMapper.createTypeMap(Employee.class, DeleteEmployee.class);
        employeeToDeleteEmployee.addMappings(mapper -> mapper.map(Employee::getName, DeleteEmployee::setName));

        return modelMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor());
    }
}
