package com.itheima.fve.fve.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.fve.fve.entity.Employee;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}
