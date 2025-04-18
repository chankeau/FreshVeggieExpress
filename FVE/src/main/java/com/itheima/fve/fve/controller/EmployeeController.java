package com.itheima.fve.fve.controller

;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.fve.fve.common.BaseContext;
import com.itheima.fve.fve.common.R;
import com.itheima.fve.fve.entity.Employee;
import com.itheima.fve.fve.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;


@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登陆
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){//获取登录信息
        /**
         */
        //1、将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        //2、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername,employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);
        //3、如果没有查询（无人）到则返回登录失败结果
        if(emp==null)return R.error("登陆失败, 请检查用户是否注册");
        //4、密码比对，如果不一致则返回登录失败结果
        if(!emp.getPassword().equals(password))return R.error("登陆失败, 请检查用户名或密码是否正确");
        //5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if(emp.getStatus()==0)return R.error("账号已禁用");
        //6、登录成功，将员工id存入Session并返回登录成功结果
        log.info("登录成功，准备设置 Session 'employee' 为 ID: {}", emp.getId()); // 添加日志
        request.getSession().setAttribute("employee", emp.getId()); // 注意是用 emp.getId()
        Object sessionValueCheck = request.getSession().getAttribute("employee"); // 立即检查
        log.info("Session 'employee' 设置后立即读取的值: {}", sessionValueCheck); // 添加日志

        return R.success(emp);
    }

    /**
     * 员工退出
     * @param request
     * @return
     */

    @PostMapping("/logout")
    //清空存储
    public R<String> logout(HttpServletRequest request){
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");

    }

    /**
     * 新增员工
     * @param request
     * @param employee
     * @return
     */

    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee){
        log.info("新增员工，员工信息：{}",employee.toString());
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//
//        Long empId = (Long) request.getSession().getAttribute("employee");
//        employee.setCreateUser(empId);
//        employee.setUpdateUser(empId);

        employeeService.save(employee);
        return R.success("新增员工成功");
    }

    /**
     * 员工信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page={}, pageSize={}, name={}",page, pageSize, name);

        //构造分页构造器
        Page pageInfo = new Page<>(page, pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();

        //添加过滤条件
        queryWrapper.like(StringUtils.hasText(name),Employee::getName,name);

        //添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        //执行查询
        employeeService.page(pageInfo,queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 根据id修改员工信息
     * @param employee
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee){
        log.info("更新员工信息: {}", employee.toString());

        long threadId = Thread.currentThread().getId();
        log.info("线程id为：{}", threadId);

        employeeService.updateById(employee);

        return R.success("员工信息修改成功");
    }

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */


    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息...");
        Employee employee = employeeService.getById(id);
        if(employee != null){
            return R.success(employee);

        }
        return R.error("没有查询到对应员工信息");


    }

}
