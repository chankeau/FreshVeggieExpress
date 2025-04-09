package com.itheima.fve.fve.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.fve.fve.entity.Employee;
import com.itheima.fve.fve.mapper.EmployeeMapper;
import com.itheima.fve.fve.service.EmployeeService;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {


}
